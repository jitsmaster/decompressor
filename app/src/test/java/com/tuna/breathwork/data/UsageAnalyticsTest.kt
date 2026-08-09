package com.tuna.breathwork.data

import com.tuna.breathwork.domain.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAnalyticsTest {

    private fun rec(
        technique: String,
        mood: MoodTag?,
        ts: Long,
        durationMs: Long = 120_000,
        completed: Boolean = true,
    ) = SessionRecord(technique, durationMs, completed, mood, ts)

    // "now" pinned 10 days out, day-aligned, so time-window math is deterministic
    private val now = 10 * DAY

    private val sample = listOf(
        rec("sigh", MoodTag.CALM, now - 3 * DAY),
        rec("sigh", MoodTag.CALM, now - 2 * DAY),
        rec("sigh", MoodTag.NEUTRAL, now - DAY),
        rec("box", MoodTag.STILL_STRESSED, now - DAY),
        rec("box", MoodTag.CALM, now - 2 * HOUR),
        rec("box", null, now - HOUR), // untagged
        rec("coherent", MoodTag.CALM, now - HOUR, durationMs = 600_000),
    )

    @Test
    fun `mood distribution counts every tagged session`() {
        val dist = UsageAnalytics.moodDistribution(sample)
        assertEquals(4, dist[MoodTag.CALM])      // sigh×2 + box + coherent
        assertEquals(1, dist[MoodTag.NEUTRAL])
        assertEquals(1, dist[MoodTag.STILL_STRESSED])
    }

    @Test
    fun `calm rate is calm over tagged sessions only`() {
        val rate = UsageAnalytics.calmRatePerTechnique(sample)
        // sigh: 2 calm / 3 tagged = 67% (untagged box ignored, untagged isn't in sigh anyway)
        assertEquals(0.67, rate["sigh"]!!, 0.01)
        // box: 1 calm / 2 tagged = 50%
        assertEquals(0.50, rate["box"]!!, 0.01)
    }

    @Test
    fun `sessions per day covers the last 7 days with zero-filled gaps`() {
        val perDay = UsageAnalytics.sessionsPerDay(sample, now, days = 7)
        assertEquals(7, perDay.size)
        // days: [0,0,0, sigh@-3d=1, sigh@-2d=1, sigh@-1d+box@-1d=2, box×2+coherent today=3]
        assertEquals(0, perDay[0])
        assertEquals(1, perDay[3])
        assertEquals(2, perDay[5])
        assertEquals(3, perDay[6])
    }

    @Test
    fun `totals report session count practice minutes and most used`() {
        val totals = UsageAnalytics.totals(sample)
        assertEquals(7, totals.sessionCount)
        // completed durations: 6×120s + 600s = 1320 s = 22 min
        assertEquals(22, totals.practiceMinutes)
        assertEquals("sigh", totals.mostUsedTechnique)
    }

    @Test
    fun `empty log yields zeros and no trend`() {
        val totals = UsageAnalytics.totals(emptyList())
        assertEquals(0, totals.sessionCount)
        assertEquals(0, totals.practiceMinutes)
        assertEquals(null, totals.mostUsedTechnique)
        assertEquals(emptyMap<String, Double>(), UsageAnalytics.calmRatePerTechnique(emptyList()))
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
