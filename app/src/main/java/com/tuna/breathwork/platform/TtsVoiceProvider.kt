package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.tuna.breathwork.session.VoiceProvider
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android TTS behind the VoiceProvider seam. Slow rate + low pitch (tuned calm voice).
 * Streams on USAGE_MEDIA so it mixes with the binaural bed; [stop] flushes the queue
 * so a slow utterance never bleeds into the next phase.
 */
class TtsVoiceProvider(context: Context, voiceRate: Float, voicePitch: Float) : VoiceProvider {

    private val ready = java.util.concurrent.CountDownLatch(1)
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
        ready.countDown()
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
        ready.await()
        val id = "tuna_${idGen.incrementAndGet()}"
        tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, id)
    }

    override suspend fun stop() {
        if (ready.count == 0L) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}