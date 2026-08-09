package com.tuna.breathwork.data

import com.tuna.breathwork.domain.MoodTag
import kotlinx.serialization.Serializable

@Serializable
data class SessionRecord(
    val techniqueId: String,
    val durationMs: Long,
    val completed: Boolean,
    val moodTag: MoodTag? = null,
    val timestampEpochMs: Long,
)

/**
 * Immutable in-memory session log. Production persistence: DataStore-backed
 * `SessionLogStore` (thin wrapper encoding [SessionLog] to JSON).
 */
data class SessionLog(val records: List<SessionRecord>) {

    fun append(record: SessionRecord): SessionLog = SessionLog(records + record)

    /** Count of mood tags per technique; every tag present (0 if never used) so the UI needs no null-checks. */
    fun moodTrend(techniqueId: String): Map<MoodTag, Int> {
        val counts = records.filter { it.techniqueId == techniqueId && it.moodTag != null }
            .groupingBy { it.moodTag!! }
            .eachCount()
        return MoodTag.entries.associateWith { counts[it] ?: 0 }
    }
}
