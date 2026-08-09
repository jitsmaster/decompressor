package com.tuna.breathwork.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TechniqueConfigTest {

    private fun boxFour() = TechniqueConfig(
        id = "box",
        name = "Box Breathing",
        zhName = "方盒呼吸",
        description = "4-4-4-4",
        phases = listOf(
            Phase(PhaseType.INHALE, 4000, voicePhrase = "Breathe in"),
            Phase(PhaseType.HOLD, 4000, voicePhrase = "Hold"),
            Phase(PhaseType.EXHALE, 4000, voicePhrase = "Breathe out"),
            Phase(PhaseType.HOLD, 4000, voicePhrase = "Hold"),
        ),
        cycles = 4,
        soundMode = SoundMode.ALPHA,
    )

    @Test
    fun `total duration is cycles times sum of phase durations - known literal`() {
        // One box cycle = 4 × 4 s = 16 s; 4 cycles = 64 s = 64000 ms
        assertEquals(64_000L, boxFour().totalDurationMs)
    }

    @Test
    fun `single cycle of sigh is 10 seconds - known literal`() {
        val sigh = TechniqueConfig(
            id = "sigh",
            name = "Physiological Sigh",
            zhName = "生理叹息",
            description = "double inhale, long exhale",
            phases = listOf(
                Phase(PhaseType.INHALE, 1500, voicePhrase = "Breathe in"),
                Phase(PhaseType.INHALE, 1500, voicePhrase = "and in"),
                Phase(PhaseType.EXHALE, 7000, voicePhrase = "Let it all out"),
            ),
            cycles = 1,
            soundMode = SoundMode.THETA,
        )
        assertEquals(10_000L, sigh.totalDurationMs)
    }

    @Test
    fun `config with zero cycles is rejected`() {
        val bad = boxFour().copy(cycles = 0)
        assertThrows(IllegalArgumentException::class.java) { bad.validate() }
    }

    @Test
    fun `config with negative phase duration is rejected`() {
        val bad = boxFour().copy(phases = listOf(Phase(PhaseType.INHALE, -1)))
        assertThrows(IllegalArgumentException::class.java) { bad.validate() }
    }

    @Test
    fun `config with empty phases is rejected`() {
        val bad = boxFour().copy(phases = emptyList())
        assertThrows(IllegalArgumentException::class.java) { bad.validate() }
    }

    @Test
    fun `voice phrase longer than its phase is rejected`() {
        val bad = boxFour().copy(
            phases = listOf(Phase(PhaseType.INHALE, 500, voicePhrase = "Breathe in very slowly and deeply"))
        )
        assertThrows(IllegalArgumentException::class.java) { bad.validate() }
    }

    @Test
    fun `voice phrase exactly at phase duration is rejected - needs margin`() {
        val bad = boxFour().copy(
            phases = listOf(Phase(PhaseType.INHALE, 1000, voicePhrase = "four syllables"))
        )
        assertThrows(IllegalArgumentException::class.java) { bad.validate() }
    }
}
