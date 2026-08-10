package com.tuna.breathwork.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.logDataStore by preferencesDataStore(name = "session_log")

/**
 * DataStore-backed session log. The JSON codec + log logic are pure and unit-tested
 * (SessionLogCodecTest); this class is the thin persistence wrapper.
 */
class SessionLogStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val log: Flow<SessionLog> = context.logDataStore.data.map { prefs ->
        val raw = prefs[Keys.LOG] ?: "[]"
        SessionLog(runCatching { json.decodeFromString<List<SessionRecord>>(raw) }.getOrDefault(emptyList()))
    }

    suspend fun append(record: SessionRecord) {
        val next = log.first().append(record)
        context.logDataStore.edit { prefs ->
            prefs[Keys.LOG] = json.encodeToString(next.records)
        }
    }

    /** Set/clear the mood on an existing record (the completion record is written immediately). */
    suspend fun updateMood(timestampEpochMs: Long, mood: com.tuna.breathwork.domain.MoodTag?) {
        val current = log.first()
        val updated = current.records.map {
            if (it.timestampEpochMs == timestampEpochMs) it.copy(moodTag = mood) else it
        }
        context.logDataStore.edit { prefs ->
            prefs[Keys.LOG] = json.encodeToString(updated)
        }
    }

    private object Keys {
        val LOG = stringPreferencesKey("records_json")
    }
}
