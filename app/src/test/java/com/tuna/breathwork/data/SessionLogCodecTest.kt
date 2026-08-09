package com.tuna.breathwork.data

import com.tuna.breathwork.domain.MoodTag
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLogCodecTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `record with mood round-trips unchanged`() {
        val record = SessionRecord(
            techniqueId = "sigh",
            durationMs = 120_000,
            completed = true,
            moodTag = MoodTag.CALM,
            timestampEpochMs = 1_752_000_000_000,
        )
        val decoded = json.decodeFromString<SessionRecord>(json.encodeToString(record))
        assertEquals(record, decoded)
    }

    @Test
    fun `record without mood round-trips with null mood`() {
        val record = SessionRecord(
            techniqueId = "box",
            durationMs = 64_000,
            completed = false,
            moodTag = null,
            timestampEpochMs = 1,
        )
        val decoded = json.decodeFromString<SessionRecord>(json.encodeToString(record))
        assertEquals(record, decoded)
        assertEquals(null, decoded.moodTag)
    }

    @Test
    fun `golden json pins the schema`() {
        val record = SessionRecord("box", 64_000, true, MoodTag.NEUTRAL, 1234)
        val encoded = json.encodeToString(record)
        assertEquals(
            """{"techniqueId":"box","durationMs":64000,"completed":true,"moodTag":"NEUTRAL","timestampEpochMs":1234}""",
            encoded,
        )
    }

    @Test
    fun `log append then read back through interface`() {
        val log = SessionLog(emptyList())
        val record = SessionRecord("coherent", 300_000, true, MoodTag.CALM, 99)
        val updated = log.append(record)
        assertEquals(1, updated.records.size)
        assertEquals(record, updated.records.first())
        assertEquals(0, log.records.size) // original unchanged (immutable)
    }

    @Test
    fun `mood trend counts tags per technique`() {
        val log = SessionLog(
            listOf(
                SessionRecord("sigh", 1, true, MoodTag.CALM, 1),
                SessionRecord("sigh", 1, true, MoodTag.CALM, 2),
                SessionRecord("sigh", 1, true, MoodTag.NEUTRAL, 3),
                SessionRecord("box", 1, true, MoodTag.STILL_STRESSED, 4),
            )
        )
        val trend = log.moodTrend("sigh")
        assertEquals(2, trend[MoodTag.CALM])
        assertEquals(1, trend[MoodTag.NEUTRAL])
        assertEquals(0, trend[MoodTag.STILL_STRESSED])
    }
}
