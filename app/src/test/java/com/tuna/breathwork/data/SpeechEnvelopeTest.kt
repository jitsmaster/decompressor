package com.tuna.breathwork.data

import com.tuna.breathwork.domain.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechEnvelopeTest {

    // Hand-built envelope: 100 ms frames, silence, speech, silence, speech, silence
    private fun envelope(vararg flags: Int, frameMs: Int = 100) =
        SpeechEnvelope(frameMs, BooleanArray(flags.size) { flags[it] == 1 })

    @Test
    fun `silence means inhale everywhere`() {
        val env = envelope(0, 0, 0, 0)
        assertEquals(SpeechPhase(PhaseType.INHALE, 400), env.phaseAt(0))
        assertEquals(SpeechPhase(PhaseType.INHALE, 100), env.phaseAt(300))
    }

    @Test
    fun `speech means exhale everywhere`() {
        val env = envelope(1, 1, 1)
        assertEquals(SpeechPhase(PhaseType.EXHALE, 300), env.phaseAt(0))
    }

    @Test
    fun `alternating flags switch phase at frame boundaries with remaining duration`() {
        // frames: [silence, silence, speech, speech, speech, silence]
        val env = envelope(0, 0, 1, 1, 1, 0)
        // at 0 ms → silence (inhale), 200 ms of silence left
        assertEquals(SpeechPhase(PhaseType.INHALE, 200), env.phaseAt(0))
        // at 200 ms → speech (exhale), 300 ms left
        assertEquals(SpeechPhase(PhaseType.EXHALE, 300), env.phaseAt(200))
        // at 490 ms → still exhale, 10 ms left
        assertEquals(SpeechPhase(PhaseType.EXHALE, 10), env.phaseAt(490))
        // at 500 ms → silence (inhale)
        assertEquals(SpeechPhase(PhaseType.INHALE, 100), env.phaseAt(500))
    }

    @Test
    fun `position beyond the end clamps to the last frame`() {
        val env = envelope(0, 0, 1, 1)
        assertEquals(SpeechPhase(PhaseType.EXHALE, 1), env.phaseAt(399))
        assertEquals(SpeechPhase(PhaseType.EXHALE, 1), env.phaseAt(10_000))
    }

    @Test
    fun `negative position clamps to the first frame`() {
        val env = envelope(1, 0)
        assertEquals(SpeechPhase(PhaseType.EXHALE, 100), env.phaseAt(-50))
    }
}
