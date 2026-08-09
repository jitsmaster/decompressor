package com.tuna.breathwork.ui.ambient

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuna.breathwork.data.AmbientTrack
import com.tuna.breathwork.data.SpeechEnvelope
import com.tuna.breathwork.data.SpeechPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AmbientUiState(
    val playingTrackId: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
)

/**
 * Plays the bundled UCLA Mindful tracks (offline assets). One player, audio-focus
 * aware; pauses on focus loss, releases when the view model is cleared. While a
 * track plays, the bundled speech envelope drives a breath phase flow so the UI
 * glyph follows the actual voice (silence → inhale, speech → exhale).
 */
class AmbientViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Application.AUDIO_SERVICE) as AudioManager
    private val _state = MutableStateFlow(AmbientUiState())
    val state: StateFlow<AmbientUiState> = _state.asStateFlow()

    /** Breath phase matched to the playing track's speech — null when paused/stopped. */
    private val _speechPhase = MutableStateFlow<SpeechPhase?>(null)
    val speechPhase: StateFlow<SpeechPhase?> = _speechPhase.asStateFlow()

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var envelope: SpeechEnvelope? = null
    private var ticker: Job? = null

    fun toggle(track: AmbientTrack) {
        val current = _state.value
        if (current.playingTrackId == track.id && current.isPlaying) {
            pause()
            return
        }
        if (current.playingTrackId == track.id) {
            resume()
            return
        }
        play(track)
    }

    private fun play(track: AmbientTrack) {
        release()
        requestFocus()
        val newPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(getApplication<Application>().assets.openFd(track.assetPath))
            setOnPreparedListener { mp ->
                _state.update {
                    it.copy(playingTrackId = track.id, isPlaying = true, durationMs = mp.duration.toLong())
                }
                mp.start()
                startSpeechTicker(track)
            }
            setOnCompletionListener { pause() }
            setOnErrorListener { _, _, _ ->
                _state.update { it.copy(playingTrackId = null, isPlaying = false) }
                true
            }
            prepareAsync()
        }
        player = newPlayer
    }

    private fun resume() {
        player?.start()
        _state.update { it.copy(isPlaying = true) }
        startSpeechTicker()
    }

    private fun pause() {
        player?.pause()
        _state.update { it.copy(isPlaying = false) }
        stopSpeechTicker()
    }

    /** Ticks the envelope by playback position every 100 ms → speech-synced breath phase. */
    private fun startSpeechTicker(track: AmbientTrack? = null) {
        track?.let {
            envelope = runCatching {
                val raw = getApplication<Application>().assets.open(it.envelopeAsset).bufferedReader().use { r -> r.readText() }
                SpeechEnvelope.fromJson(raw)
            }.getOrNull()
        }
        stopSpeechTicker()
        val env = envelope ?: return
        ticker = viewModelScope.launch {
            while (isActive) {
                val pos = player?.currentPosition?.toLong() ?: 0L
                _speechPhase.value = env.phaseAt(pos)
                delay(100)
            }
        }
    }

    private fun stopSpeechTicker() {
        ticker?.cancel()
        ticker = null
        _speechPhase.value = null
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS) pause()
            }
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }

    private fun release() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        abandonFocus()
        stopSpeechTicker()
        envelope = null
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}
