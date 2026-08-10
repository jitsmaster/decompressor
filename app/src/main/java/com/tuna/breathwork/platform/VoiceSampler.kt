package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.tuna.breathwork.data.VoiceLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays a short sample of the selected guidance voice — used by the Settings
 * "Test voice" button so the user can compare English and 中文 before committing.
 */
class VoiceSampler(private val context: Context, private val language: VoiceLanguage) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var player: MediaPlayer? = null

    fun play() {
        stop()
        val samples = listOf("Breathe in", "Breathe out")
        scope.launch {
            for (text in samples) {
                if (!isActive) break
                val file = sampleFile(text) ?: continue
                val afd = context.assets.openFd(file)
                val mp = MediaPlayer()
                runCatching {
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    mp.prepare()
                    mp.start()
                    player = mp
                    delay(mp.duration + 300L)
                }
                runCatching { mp.release() }
                player = null
            }
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    /** The bundled sample clips for this language ("Breathe in"/"Breathe out" or their ZH equivalents). */
    private fun sampleFile(text: String): String? {
        val phrase = when (text) {
            "Breathe in" -> "phrase_breathe_in"
            "Breathe out" -> "phrase_breathe_out"
            else -> return null
        }
        return if (language == VoiceLanguage.ZH) {
            if (phrase == "phrase_breathe_in") "phrases/phrase_breathe_in_zh.mp3" else "phrases/phrase_breathe_out_zh.mp3"
        } else {
            if (phrase == "phrase_breathe_in") "phrases/phrase_breathe_in_en.mp3" else "phrases/phrase_breathe_out_en.mp3"
        }
    }
}
