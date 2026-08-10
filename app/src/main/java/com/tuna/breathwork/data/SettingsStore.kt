package com.tuna.breathwork.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class Settings(
    val voiceRate: Float = 0.70f,
    val voicePitch: Float = 0.55f,
    val hapticsEnabled: Boolean = true,      // practice mode
    val calmNowHaptics: Boolean = true,      // eyes-closed guidance default on
    val reduceMotion: Boolean = false,
    /** Soft tick in the final second of each phase. */
    val countdownTicks: Boolean = true,
    /** Screen may turn off during a session; a partial wakelock keeps the rhythm exact. */
    val allowScreenOff: Boolean = true,
    /** Which technique Calm Now runs (SPEC D2 extension: switchable). */
    val calmNowTechniqueId: String = "sigh",
    /** Session guidance language: "en" or "zh". */
    val voiceLanguage: String = "en",
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            voiceRate = prefs[Keys.VOICE_RATE] ?: 0.70f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 0.55f,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            calmNowHaptics = prefs[Keys.CALM_NOW_HAPTICS] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            countdownTicks = prefs[Keys.COUNTDOWN_TICKS] ?: true,
            allowScreenOff = prefs[Keys.ALLOW_SCREEN_OFF] ?: true,
            calmNowTechniqueId = prefs[Keys.CALM_NOW_TECHNIQUE] ?: "sigh",
            voiceLanguage = prefs[Keys.VOICE_LANGUAGE] ?: "en",
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        val next = transform(current())
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VOICE_RATE] = next.voiceRate
            prefs[Keys.VOICE_PITCH] = next.voicePitch
            prefs[Keys.HAPTICS] = next.hapticsEnabled
            prefs[Keys.CALM_NOW_HAPTICS] = next.calmNowHaptics
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
            prefs[Keys.COUNTDOWN_TICKS] = next.countdownTicks
            prefs[Keys.ALLOW_SCREEN_OFF] = next.allowScreenOff
            prefs[Keys.CALM_NOW_TECHNIQUE] = next.calmNowTechniqueId
            prefs[Keys.VOICE_LANGUAGE] = next.voiceLanguage
        }
    }

    private object Keys {
        val VOICE_RATE = floatPreferencesKey("voice_rate")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CALM_NOW_HAPTICS = booleanPreferencesKey("calm_now_haptics")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val COUNTDOWN_TICKS = booleanPreferencesKey("countdown_ticks")
        val ALLOW_SCREEN_OFF = booleanPreferencesKey("allow_screen_off")
        val CALM_NOW_TECHNIQUE = stringPreferencesKey("calm_now_technique")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
    }
}
