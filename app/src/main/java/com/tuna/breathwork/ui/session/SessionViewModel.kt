package com.tuna.breathwork.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuna.breathwork.TunaApp
import com.tuna.breathwork.container
import com.tuna.breathwork.data.Preset
import com.tuna.breathwork.data.SessionLogStore
import com.tuna.breathwork.data.SessionRecord
import com.tuna.breathwork.data.Settings
import com.tuna.breathwork.data.SettingsStore
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.domain.BinauralSpec
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.SessionResult
import com.tuna.breathwork.domain.SoundMode
import com.tuna.breathwork.domain.TechniqueConfig
import com.tuna.breathwork.platform.AndroidHaptics
import com.tuna.breathwork.platform.BinauralEngine
import com.tuna.breathwork.platform.HeadphoneDetector
import com.tuna.breathwork.platform.RecordedVoiceProvider
import com.tuna.breathwork.platform.TtsVoiceProvider
import com.tuna.breathwork.session.SessionEngine
import com.tuna.breathwork.session.SessionSink
import com.tuna.breathwork.session.VoiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HeadphoneStatus { CHECKING, STEREO, MONO_FALLBACK }

data class SessionUiState(
    val phase: Phase? = null,
    val cycle: Int = 0,
    val totalCycles: Int,
    val completed: SessionResult? = null,
    val aborted: Boolean = false,
    val headphoneStatus: HeadphoneStatus = HeadphoneStatus.CHECKING,
    val sessionStarted: Boolean = false,
    /** Wall-clock time the current phase started — drives the countdown ring/seconds. */
    val phaseStartedAtMs: Long = 0,
)

/**
 * Wires the session engine to the platform: TTS voice, haptics, binaural beats and
 * the session log. Also owns the headphone check + reminder and the mood tagging flow.
 */
class SessionViewModel(
    application: Application,
    val config: TechniqueConfig,
    private val calmNow: Boolean,
    private val initialSettings: Settings,
) : AndroidViewModel(application), SessionSink {

    val reduceMotion: Boolean = initialSettings.reduceMotion

    private val app = application
    private val logStore = SessionLogStore(app)

    private val _state = MutableStateFlow(SessionUiState(totalCycles = config.cycles))
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val voice: VoiceProvider = RecordedVoiceProvider(
        app,
        TtsVoiceProvider(app, initialSettings.voiceRate, initialSettings.voicePitch),
    )
    private val haptics: AndroidHaptics = AndroidHaptics(
        app,
        enabled = if (calmNow) initialSettings.calmNowHaptics else initialSettings.hapticsEnabled,
    )
    private val beats = BinauralEngine(app)

    private val engine: SessionEngine = SessionEngine(
        config = config,
        voice = voice,
        haptics = haptics,
        sink = this,
        calmNow = calmNow,
        postureEnabled = initialSettings.posturePromptsEnabled,
        scope = viewModelScope,
    ).apply {
        if (calmNow) sessionIntro = "Begin. Breathe with me."
    }

    /** Completed session awaiting a mood tag; appended to the log once tagged or skipped. */
    private var pendingRecord: SessionRecord? = null

    /** Tick sound for the phase-end countdown (bundled sfx/tick.mp3). */
    private val tickPool = android.media.SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val tickId = runCatching {
        tickPool.load(app.assets.openFd("sfx/tick.mp3"), 1)
    }.getOrDefault(0)
    private val playTicks = initialSettings.countdownTicks

    fun start() {
        if (_state.value.sessionStarted) return
        viewModelScope.launch {
            _state.update { it.copy(headphoneStatus = HeadphoneStatus.CHECKING) }
            val stereo = withContext(Dispatchers.IO) { HeadphoneDetector(app).hasHeadphones() }
            _state.update {
                it.copy(headphoneStatus = if (stereo) HeadphoneStatus.STEREO else HeadphoneStatus.MONO_FALLBACK)
            }
            if (!stereo) {
                voice.speakAndAwait("Put on headphones for the full effect. We'll continue either way.")
            }
            beats.start(spec = specFor(config.soundMode), stereo = stereo, onFocusLost = { engine.abort() })
            engine.start()
            _state.update { it.copy(sessionStarted = true) }
        }
    }

    fun abort() {
        engine.abort()
        beats.stop()
    }

    /** Called by the UI after completion: tag the session, then finish. */
    fun tagMood(tag: MoodTag?) {
        val record = pendingRecord ?: return
        viewModelScope.launch {
            logStore.append(record.copy(moodTag = tag))
            pendingRecord = null
        }
    }

    // --- SessionSink ---

    override fun onPhase(phase: Phase) {
        _state.update { it.copy(phase = phase, phaseStartedAtMs = System.currentTimeMillis()) }
    }

    override fun onCycle(cycle: Int, total: Int) {
        _state.update { it.copy(cycle = cycle) }
    }

    override fun onComplete(result: SessionResult) {
        beats.stop()
        pendingRecord = SessionRecord(
            techniqueId = config.id,
            durationMs = result.plannedDurationMs,
            completed = true,
            timestampEpochMs = System.currentTimeMillis(),
        )
        _state.update { it.copy(completed = result) }
    }

    override fun onPhaseEnding(phase: Phase) {
        if (playTicks) {
            tickPool.play(tickId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    override fun onAbort() {
        beats.stop()
        val record = SessionRecord(
            techniqueId = config.id,
            durationMs = 0,
            completed = false,
            timestampEpochMs = System.currentTimeMillis(),
        )
        viewModelScope.launch { logStore.append(record) }
        _state.update { it.copy(aborted = true) }
    }

    /** Persist without mood if the user leaves before tagging. */
    override fun onCleared() {
        pendingRecord?.let { record ->
            viewModelScope.launch { logStore.append(record.copy(moodTag = null)) }
            pendingRecord = null
        }
        beats.stop()
        tickPool.release()
        super.onCleared()
    }

    private fun specFor(mode: SoundMode): BinauralSpec =
        when (mode) {
            SoundMode.THETA -> BinauralSpec.THETA
            SoundMode.ALPHA -> BinauralSpec.ALPHA
        }

    class Factory(
        private val calmNow: Boolean,
        private val techniqueId: String? = null,
        private val preset: Preset = Preset.MEDIUM,
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(
            modelClass: Class<T>,
            extras: androidx.lifecycle.viewmodel.CreationExtras,
        ): T {
            val app = extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                ?: error("SessionViewModel requires an Application")
            val settings = kotlinx.coroutines.runBlocking { (app as TunaApp).container.currentSettings() }
            val config = if (calmNow) {
                // Calm Now reads its technique from settings — switchable in the Settings screen.
                TechniquesRepository.withPreset(
                    TechniquesRepository.byId(settings.calmNowTechniqueId),
                    Preset.CALM_NOW,
                )
            } else {
                TechniquesRepository.withPreset(TechniquesRepository.byId(techniqueId!!), preset)
            }
            return SessionViewModel(app, config, calmNow, settings) as T
        }
    }
}
