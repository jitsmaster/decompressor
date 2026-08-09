package com.tuna.breathwork.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class Settings(
    val voiceRate: Float = 0.75f,
    val voicePitch: Float = 0.85f,
    val hapticsEnabled: Boolean = true,      // practice mode
    val calmNowHaptics: Boolean = false,     // Calm Now default off (SPEC D11)
    val posturePromptsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            voiceRate = prefs[Keys.VOICE_RATE] ?: 0.75f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 0.85f,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            calmNowHaptics = prefs[Keys.CALM_NOW_HAPTICS] ?: false,
            posturePromptsEnabled = prefs[Keys.POSTURE] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
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
            prefs[Keys.POSTURE] = next.posturePromptsEnabled
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
        }
    }

    private object Keys {
        val VOICE_RATE = floatPreferencesKey("voice_rate")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CALM_NOW_HAPTICS = booleanPreferencesKey("calm_now_haptics")
        val POSTURE = booleanPreferencesKey("posture_prompts")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }
}
