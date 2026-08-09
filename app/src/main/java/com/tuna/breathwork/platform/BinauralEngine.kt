package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import com.tuna.breathwork.domain.BinauralSpec
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural binaural beats. Generates stereo PCM in ~200 ms chunks on a background
 * thread, streamed through a low-latency AudioTrack. Stereo mode produces true
 * binaural beats (requires headphones); mono mode falls back to a single carrier
 * amplitude-modulated at the beat frequency (the "pulsing tone" that works on a
 * phone speaker and doubles as a breath-pace cue). Fades in/out to avoid clicks,
 * and pauses when audio focus is lost.
 */
class BinauralEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var playing = false
    private var focusRequest: AudioFocusRequest? = null

    /** @param volume 0..1 amplitude of the bed (beats ride under the voice). */
    fun start(spec: BinauralSpec, stereo: Boolean, volume: Float = 0.25f, onFocusLost: () -> Unit) {
        stop()
        requestFocus(onFocusLost)
        val sampleRate = 44_100
        val channelMask = if (stereo) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val bufferBytes = maxOf(minBuf, sampleRate * 4 * 2) // ~4 s ring

        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track = newTrack
        playing = true
        newTrack.play()

        thread = Thread {
            val chunkSamples = sampleRate / 5 // 200 ms
            val lPhase = DoubleArray(1)
            val rPhase = DoubleArray(1)
            val t = DoubleArray(1) // elapsed seconds for the modulation envelope
            var elapsedMs = 0L
            val fadeMs = 800
            while (playing) {
                val samples = ShortArray(chunkSamples * (if (stereo) 2 else 1))
                val amplitude = volume * Short.MAX_VALUE
                for (i in 0 until chunkSamples) {
                    val env = fadeInOut(envelopeMs = elapsedMs + i * 1000.0 / sampleRate, fadeMs = fadeMs)
                    if (stereo) {
                        lPhase[0] += 2.0 * PI * spec.leftHz / sampleRate
                        rPhase[0] += 2.0 * PI * spec.rightHz / sampleRate
                        samples[i * 2] = (sin(lPhase[0]) * amplitude * env).toInt().toShort()
                        samples[i * 2 + 1] = (sin(rPhase[0]) * amplitude * env).toInt().toShort()
                    } else {
                        t[0] += 1.0 / sampleRate
                        val carrier = sin(2.0 * PI * spec.leftHz * t[0])
                        val beat = 0.5 + 0.5 * sin(2.0 * PI * spec.beatHz * t[0])
                        samples[i] = (carrier * beat * amplitude * env).toInt().toShort()
                    }
                }
                elapsedMs += chunkSamples * 1000 / sampleRate
                if (!playing) break
                newTrack.write(samples, 0, samples.size)
            }
        }
        thread!!.isDaemon = true
        thread!!.start()
    }

    /** Linear fade in/out around the session start/stop. */
    private fun fadeInOut(envelopeMs: Double, fadeMs: Int): Double {
        if (envelopeMs < fadeMs) return envelopeMs / fadeMs
        return 1.0
    }

    fun stop() {
        playing = false
        thread?.join(300)
        thread = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.release() }
        track = null
        abandonFocus()
    }

    private fun requestFocus(onFocusLost: () -> Unit) {
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (change != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        stop()
                        onFocusLost()
                    }
                }
            }
        }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }
}
