package com.tuna.breathwork.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuna.breathwork.TunaApp
import com.tuna.breathwork.container
import com.tuna.breathwork.data.Preset
import com.tuna.breathwork.data.Settings
import com.tuna.breathwork.data.VoiceLanguage
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.TechniqueConfig
import com.tuna.breathwork.session.SessionService
import com.tuna.breathwork.session.SessionUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin UI façade over [SessionService]. The service owns the session (engine, voice,
 * haptics, beats, wakelock, log) so it survives the activity being destroyed or the
 * screen turning off; this view model only resolves config for the UI and forwards
 * commands to the service.
 */
class SessionViewModel(
    application: Application,
    val config: TechniqueConfig,
    private val calmNow: Boolean,
    private val initialSettings: Settings,
) : AndroidViewModel(application) {

    private val app = application

    val reduceMotion: Boolean = initialSettings.reduceMotion

    /** Active guidance language — drives the on-screen phase words. */
    val language: VoiceLanguage = VoiceLanguage.fromKey(initialSettings.voiceLanguage)

    /** Screen may turn off; the service's wakelock keeps the rhythm exact while off. */
    val allowScreenOff: Boolean = initialSettings.allowScreenOff

    val state: StateFlow<SessionUiState> = SessionService.state

    fun start() {
        val s = SessionService.state.value
        // Actively running → continue it (spec: handoff/ignore). Completed, aborted, or never
        // started → tear down and start fresh (the service handles the old engine).
        if (s.sessionStarted && s.completed == null && !s.aborted) return
        SessionService.resetForNewSession()
        SessionService.start(
            app,
            techniqueId = if (calmNow) null else config.id,
            preset = Preset.MEDIUM,
            calmNow = calmNow,
        )
    }

    fun abort() {
        SessionService.end(app)
    }

    fun tagMood(tag: MoodTag?) {
        SessionService.tagMood(tag)
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
