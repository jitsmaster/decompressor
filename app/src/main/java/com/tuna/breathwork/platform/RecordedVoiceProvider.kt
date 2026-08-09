package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.tuna.breathwork.session.VoiceProvider
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Plays the bundled recorded phrase clips (assets/phrases — generated from
 * Microsoft Edge neural voices, deep male US + Mandarin male for the six sounds)
 * in a strict sequential queue, exactly like a voice track. Unmapped phrases fall
 * back to [fallback] (TTS). [stop] flushes the queue instantly so a phrase never
 * bleeds into the next phase.
 */
class RecordedVoiceProvider(
    private val context: Context,
    private val fallback: VoiceProvider,
) : VoiceProvider {

    @Serializable
    private data class PhraseManifest(val id: String, val text: String)

    private val lock = Any()
    private val queue = ArrayDeque<String>() // asset paths
    private var player: MediaPlayer? = null
    /** Completes when the currently-awaited clip finishes (headphone reminder). */
    private var currentDone: CompletableDeferred<Unit>? = null
    /** Bumped by [stop] to invalidate any in-flight prepare/start — closes the overlap race. */
    private var generation = 0
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** normalized phrase text → asset path */
    private val clipByText: Map<String, String> = runCatching {
        val json = context.assets.open("phrases/manifest.json").bufferedReader().use { it.readText() }
        Json.decodeFromString<List<PhraseManifest>>(json)
            .associate { normalize(it.text) to "phrases/${it.id}.mp3" }
    }.getOrDefault(emptyMap())

    override suspend fun speak(phrase: String) {
        val path = clipByText[normalize(phrase)]
        if (path == null) {
            android.util.Log.w("TunaVoice", "no recorded clip for phrase: \"$phrase\" — falling back to TTS")
            fallback.speak(phrase)
            return
        }
        synchronized(lock) { queue.addLast(path) }
        pump()
    }

    /** Speak and don't return until the clip has finished playing (used for the headphone reminder). */
    override suspend fun speakAndAwait(phrase: String) {
        val path = clipByText[normalize(phrase)]
        if (path == null) {
            android.util.Log.w("TunaVoice", "no recorded clip for phrase: \"$phrase\" — falling back to TTS")
            fallback.speak(phrase)
            return
        }
        val done = CompletableDeferred<Unit>()
        synchronized(lock) {
            queue.addLast(path)
            currentDone = done
        }
        pump()
        withTimeoutOrNull(8_000) { done.await() }
    }

    override suspend fun stop() {
        synchronized(lock) {
            generation++ // invalidate any in-flight pump
            queue.clear()
            player?.let { mp ->
                runCatching { mp.stop() }
                runCatching { mp.release() }
            }
            player = null
            currentDone?.complete(Unit) // a stopped clip still counts as "done" for awaiters
            currentDone = null
        }
        fallback.stop()
    }

    /** Starts the next queued clip if nothing is playing. Runs on the IO dispatcher. */
    private fun pump() {
        val gen: Int
        val next: String
        synchronized(lock) {
            if (player?.isPlaying == true) return
            next = queue.pollFirst() ?: return
            gen = ++generation
        }
        io.launch {
            val mp = MediaPlayer()
            val ok = runCatching {
                val afd = context.assets.openFd(next)
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mp.setOnCompletionListener {
                    runCatching { mp.release() }
                    synchronized(lock) {
                        player = null
                        currentDone?.complete(Unit)
                        currentDone = null
                    }
                    pump()
                }
                mp.prepare()
                synchronized(lock) {
                    // A stop() may have raced our prepare — if so, do not start this player.
                    if (gen != generation) {
                        runCatching { mp.release() }
                        return@runCatching false
                    }
                    player = mp
                }
                mp.start()
                true
            }.getOrDefault(false)
            if (!ok) {
                synchronized(lock) { player = null }
                pump()
            }
        }
    }

    private fun normalize(text: String): String =
        text.trim().lowercase().replace(Regex("\\s+"), " ")
}
