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
        // 1. Try candidates in order, honoring the engine's return code — setVoice does
        //    NOT throw on rejection; it returns ERROR and silently falls back to the
        //    engine default (often the female voice). So we must check every attempt.
        val candidates = pickDeepMaleAmericanVoiceCandidates()
        var applied = false
        for (voice in candidates) {
            val result = runCatching { tts.setVoice(voice) }.getOrDefault(TextToSpeech.ERROR)
            if (result == TextToSpeech.SUCCESS) {
                android.util.Log.i("TunaVoice", "voice applied: ${voice.name} (${voice.locale})")
                applied = true
                break
            }
            android.util.Log.w("TunaVoice", "voice rejected by engine: ${voice.name} (code $result)")
        }
        if (!applied) {
            runCatching { tts.setLanguage(Locale.US) }
            android.util.Log.w(
                "TunaVoice",
                "no candidate voice accepted; engine default = ${runCatching { tts.defaultVoice?.name }.getOrNull() ?: "?"}"
            )
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
        dumpAvailableVoices()
    }

    /** Ordered candidates: offline en-US male-ish first, then any en-US, then any male. */
    private fun pickDeepMaleAmericanVoiceCandidates(): List<android.speech.tts.Voice> {
        val voices = runCatching { tts.voices }.getOrNull() ?: return emptyList()
        val enUs = voices.filter { v ->
            val l = v.locale
            l.language.equals("en", true) && (l.country.equals("US", true) || l.variant.contains("us", true))
        }
        val maleHints = listOf("male", "tpf", "usm", "guy", "daniel")
        val femaleHints = listOf("female", "sfg", "x-fem")
        fun isMaleish(v: android.speech.tts.Voice) = maleHints.any { v.name.lowercase().contains(it) }
        fun isFemaleish(v: android.speech.tts.Voice) = femaleHints.any { v.name.lowercase().contains(it) }

        val ranked = enUs.sortedWith(
            compareBy(
                { it.isNetworkConnectionRequired },                    // offline first
                { !isMaleish(it) },                                    // male first
                { isFemaleish(it) },                                   // female last
                { -it.quality },                                       // higher quality first
            )
        )
        val anyMale = voices.filter(::isMaleish).sortedBy { it.isNetworkConnectionRequired }
        return (ranked + anyMale).distinctBy { it.name }
    }

    private fun dumpAvailableVoices() {
        runCatching {
            tts.voices?.forEach { v ->
                android.util.Log.i(
                    "TunaVoice",
                    "available: ${v.name} | ${v.locale} | net=${v.isNetworkConnectionRequired} | q=${v.quality} | ${v.features}"
                )
            }
        }
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
