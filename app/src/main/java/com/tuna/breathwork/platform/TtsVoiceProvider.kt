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
 * Android TTS behind the VoiceProvider seam. Slow rate + low pitch (tuned calm voice).
 * Streams on USAGE_MEDIA so it mixes with the binaural bed; [stop] flushes the queue
 * so a slow utterance never bleeds into the next phase.
 *
 * IMPORTANT: readiness uses a suspend gate ([CompletableDeferred.await]), never a
 * blocking latch — TTS's onInit is dispatched on the main thread, so blocking there
 * would deadlock (and ANR) exactly like a CountDownLatch does. If the engine isn't
 * ready within 5 s, speech is skipped and the session continues visual-only.
 */
class TtsVoiceProvider(context: Context, voiceRate: Float, voicePitch: Float) : VoiceProvider {

    private val ready = CompletableDeferred<Unit>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(voiceRate)
            tts.setPitch(voicePitch)
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        ready.complete(Unit)
    }
    private val idGen = AtomicInteger(0)

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
