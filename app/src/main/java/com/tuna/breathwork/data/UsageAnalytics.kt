package com.tuna.breathwork.data

import com.tuna.breathwork.domain.MoodTag

/**
 * Pure usage analytics over the session log — powers the History "Insights" section.
 * No Android deps, fully unit-tested (UsageAnalyticsTest). All time math takes an
 * explicit "now" so tests are deterministic.
 */
object UsageAnalytics {

    data class Totals(
        val sessionCount: Int,
        val practiceMinutes: Long,
        val mostUsedTechnique: String?,
    )

    /** Count of each mood tag across tagged sessions (untagged ignored). */
    fun moodDistribution(records: List<SessionRecord>): Map<MoodTag, Int> {
        val counts = records.filter { it.moodTag != null }.groupingBy { it.moodTag!! }.eachCount()
        return MoodTag.entries.associateWith { counts[it] ?: 0 }
    }

    /**
     * Calm rate per technique: CALM / tagged-session count, 0..1.
     * Untagged sessions are excluded from the denominator (no mood data).
     */
    fun calmRatePerTechnique(records: List<SessionRecord>): Map<String, Double> {
        val tagged = records.filter { it.moodTag != null }
        return tagged.groupBy { it.techniqueId }.mapValues { (_, group) ->
            val calm = group.count { it.moodTag == MoodTag.CALM }
            calm.toDouble() / group.size
        }
    }

    /**
     * Session count per day over the trailing [days] days (index 0 = oldest,
     * index days-1 = the day containing [nowEpochMs]), zero-filled.
     */
    fun sessionsPerDay(records: List<SessionRecord>, nowEpochMs: Long, days: Int): List<Int> {
        val dayMs = 24 * 3_600_000L
        val buckets = IntArray(days)
        val todayStart = nowEpochMs - (nowEpochMs % dayMs)
        records.forEach { r ->
            val ageDays = ((todayStart - r.timestampEpochMs) / dayMs).toInt()
            if (ageDays in 0 until days) buckets[days - 1 - ageDays]++
        }
        return buckets.toList()
    }

    fun totals(records: List<SessionRecord>): Totals {
        val completed = records.filter { it.completed }
        val practiceMinutes = completed.sumOf { it.durationMs } / 60_000
        val mostUsed = records.groupingBy { it.techniqueId }.eachCount().maxByOrNull { it.value }?.key
        return Totals(records.size, practiceMinutes, mostUsed)
    }
}
