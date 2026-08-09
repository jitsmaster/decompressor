package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.tuna.breathwork.session.VoiceProvider
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Android TTS behind the VoiceProvider seam. Voice identity: deep male American English.
 * Selection is heuristic (the public API exposes no gender): prefer an offline, en-US
 * voice whose name doesn't look female, fall back to Locale.US, then to any male voice.
 * Streams on USAGE_MEDIA so it mixes with the binaural bed; [stop] flushes the queue
 * so a slow utterance never bleeds into the next phase.
 *
 * IMPORTANT: readiness uses a suspend gate ([CompletableDeferred.await]), never a
 * blocking latch — TTS's onInit is dispatched on the main thread, so blocking there
 * would deadlock (and ANR) exactly like a CountDownLatch does. If the engine isn't
 * ready within 5 s, speech is skipped and the session continues visual-only.
 */
class TtsVoiceProvider(context: Context, private val voiceRate: Float, private val voicePitch: Float) : VoiceProvider {

    private val ready = CompletableDeferred<Unit>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureVoice()
        }
        ready.complete(Unit)
    }
    private val idGen = AtomicInteger(0)

    private fun configureVoice() {
        // 1. Pick the voice first (setLanguage would reset it; pitch/rate applied after).
        val chosen = pickDeepMaleAmericanVoice()
        if (chosen != null) {
            android.util.Log.i("TunaVoice", "using voice: ${chosen.name} (${chosen.locale})")
            runCatching { tts.setVoice(chosen) }.onFailure {
                android.util.Log.w("TunaVoice", "setVoice failed, falling back to en_US", it)
                runCatching { tts.setLanguage(Locale.US) }
            }
        } else {
            android.util.Log.w("TunaVoice", "no en-US male voice found; using en_US locale default")
            runCatching { tts.setLanguage(Locale.US) }
        }
        // 2. Tone AFTER voice selection so nothing resets it.
        tts.setSpeechRate(voiceRate)
        tts.setPitch(voicePitch)
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    private fun pickDeepMaleAmericanVoice(): android.speech.tts.Voice? {
        val voices = runCatching { tts.voices }.getOrNull() ?: return null
        val enUs = voices.filter { v ->
            val l = v.locale
            l.language.equals("en", true) && (l.country.equals("US", true) || l.variant.contains("us", true))
        }
        if (enUs.isEmpty()) return null
        val offline = enUs.filter { !it.isNetworkConnectionRequired }
        val pool = offline.ifEmpty { enUs }
        // Male heuristic on engine name codes (Google: x-tpf = US male, x-sfg = US female).
        val maleish = pool.filter {
            val n = it.name.lowercase()
            n.contains("male") || n.contains("tpf") || n.contains("usm") || n.contains("guy") || n.contains("daniel")
        }
        val noFemale = pool.filter {
            val n = it.name.lowercase()
            !n.contains("female") && !n.contains("sfg") && !n.contains("x-fem")
        }
        return maleish.firstOrNull() ?: noFemale.firstOrNull() ?: pool.firstOrNull()
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    override suspend fun speak(phrase: String) {
        if (withTimeoutOrNull(5_000) { ready.await() } == null) return // engine unavailable → visual-only
        val id = "tuna_${idGen.incrementAndGet()}"
        tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, id)
    }

    override suspend fun stop() {
        if (ready.isCompleted) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
