package com.tuna.breathwork.session

import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.domain.SessionResult
import com.tuna.breathwork.domain.SoundMode
import com.tuna.breathwork.domain.TechniqueConfig
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    private val box = TechniqueConfig(
        id = "box",
        name = "Box Breathing",
        zhName = "方盒呼吸",
        description = "4-4-4-4",
        phases = listOf(
            Phase(PhaseType.INHALE, 4000, voicePhrase = "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.HOLD, 4000),
            Phase(PhaseType.EXHALE, 4000, voicePhrase = "Breathe out", haptic = HapticKind.SOFT),
            Phase(PhaseType.HOLD, 4000),
        ),
        cycles = 4,
        soundMode = SoundMode.ALPHA,
    )

    private class FakeVoice(val durationFor: (String) -> Long? = { null }) : VoiceProvider {
        val ops = mutableListOf<String>()
        override suspend fun speak(phrase: String) { ops += "speak:$phrase" }
        override suspend fun stop() { ops += "stop" }
        override suspend fun clipDurationMs(phrase: String): Long? = durationFor(phrase)
    }

    private class FakeHaptics : HapticDriver {
        val starts = mutableListOf<Pair<HapticKind, Long>>()
        override fun start(kind: HapticKind, durationMs: Long) { starts += kind to durationMs }
        override fun stop() {}
    }

    private class RecordingSink : SessionSink {
        val phases = mutableListOf<Phase>()
        val cycles = mutableListOf<Pair<Int, Int>>()
        val endings = mutableListOf<Phase>()
        val cues = mutableListOf<Phase>()
        var completed: SessionResult? = null
        var aborted = false
        override fun onPhase(phase: Phase) { phases += phase }
        override fun onCycle(cycle: Int, total: Int) { cycles += cycle to total }
        override fun onComplete(result: SessionResult) { completed = result }
        override fun onAbort() { aborted = true }
        override fun onPhaseEnding(phase: Phase) { endings += phase }
        override fun onVoiceCue(phase: Phase) { cues += phase }
    }

    private fun engine(scope: kotlinx.coroutines.CoroutineScope, voice: FakeVoice = FakeVoice(),
                       sink: RecordingSink = RecordingSink(), haptics: FakeHaptics = FakeHaptics()) =
        SessionEngine(box, voice, haptics, sink, scope = scope)

    @Test
    fun `emits phases in order with exact timing`() = runTest {
        val voice = FakeVoice()
        val sink = RecordingSink()
        engine(this, voice, sink).start()
        runCurrent()
        assertEquals(1, sink.phases.size)
        assertEquals(PhaseType.INHALE, sink.phases[0].type)

        advanceTimeBy(3999); runCurrent()
        assertEquals("no phase change before 4s", 1, sink.phases.size)
        advanceTimeBy(1); runCurrent()
        assertEquals(2, sink.phases.size)
        assertEquals(PhaseType.HOLD, sink.phases[1].type)

        advanceTimeBy(4000); runCurrent()
        assertEquals(3, sink.phases.size)
        assertEquals(PhaseType.EXHALE, sink.phases[2].type)

        advanceTimeBy(4000); runCurrent()
        assertEquals(4, sink.phases.size)
        assertEquals(PhaseType.HOLD, sink.phases[3].type)
        assertNull("not complete after one cycle of four", sink.completed)
    }

    @Test
    fun `completes after all cycles with result`() = runTest {
        val sink = RecordingSink()
        engine(this, sink = sink).start()
        advanceTimeBy(64_000)
        advanceUntilIdle()
        assertEquals(4, sink.cycles.size)
        assertEquals(4 to 4, sink.cycles.last())
        val result = sink.completed
        assertTrue(result != null)
        assertEquals("box", result!!.techniqueId)
        assertEquals(64_000L, result.plannedDurationMs)
        assertEquals(4, result.cyclesCompleted)
        assertTrue(result.completed)
        assertFalse(sink.aborted)
    }

    @Test
    fun `abort emits onAbort and never completes`() = runTest {
        val sink = RecordingSink()
        val engine = engine(this, sink = sink)
        engine.start()
        advanceTimeBy(5_000); runCurrent()
        engine.abort()
        advanceUntilIdle()
        assertTrue(sink.aborted)
        assertNull(sink.completed)
        assertEquals("aborted mid-first-cycle, zero completed cycles", 0, sink.cycles.size)
    }

    @Test
    fun `voice queue flushed at every phase boundary`() = runTest {
        val voice = FakeVoice()
        engine(this, voice = voice).start()
        advanceTimeBy(16_000); advanceUntilIdle()
        // After the intro, every phase transition = stop() then speak(). A speak that
        // directly follows another speak is a queued continuation (e.g. a cycle sound
        // chained after the phase phrase) — that's intentional, no flush needed.
        val ops = voice.ops
        val speakIdx = ops.mapIndexedNotNull { i, o -> i.takeIf { o.startsWith("speak:") } }
        for (i in 1 until speakIdx.size) {
            if (ops[speakIdx[i] - 1].startsWith("speak:")) continue // queued continuation
            assertEquals("stop before speak #$i", "stop", ops[speakIdx[i] - 1])
        }
        assertTrue("intro spoken first", ops[1].startsWith("speak:Settle in."))
    }

    @Test
    fun `start twice does not double-run the session`() = runTest {
        val sink = RecordingSink()
        val engine = engine(this, sink = sink)
        engine.start()
        engine.start()
        advanceTimeBy(64_000); advanceUntilIdle()
        assertEquals(1, sink.completed?.let { 1 } ?: 0)
    }

    @Test
    fun `haptics start per phase with kind and duration`() = runTest {
        val haptics = FakeHaptics()
        engine(this, haptics = haptics).start()
        advanceTimeBy(8_000); runCurrent()
        assertEquals(
            listOf(HapticKind.PULSE to 4000L, HapticKind.NONE to 4000L),
            haptics.starts.filter { it.first != HapticKind.PRETICK }.take(2),
        )
        advanceTimeBy(8_000); runCurrent()
        assertEquals(
            listOf(HapticKind.PULSE to 4000L, HapticKind.NONE to 4000L, HapticKind.NONE to 4000L, HapticKind.SOFT to 4000L),
            haptics.starts.filter { it.first != HapticKind.PRETICK }.take(4),
        )
    }

    @Test
    fun `breath phase starts exactly when the voice phrase finishes`() = runTest {
        val voice = FakeVoice(durationFor = { 1_000L })
        val sink = RecordingSink()
        engine(this, voice = voice, sink = sink).start()
        runCurrent()
        assertEquals("heads-up fires at phase start", 1, sink.cues.size)
        assertEquals(PhaseType.INHALE, sink.cues[0].type)
        assertEquals("breath not started while the clip plays", 0, sink.phases.size)

        advanceTimeBy(999); runCurrent()
        assertEquals(0, sink.phases.size)
        advanceTimeBy(1); runCurrent()
        assertEquals("breath starts at the clip end", 1, sink.phases.size)
        assertEquals(PhaseType.INHALE, sink.phases[0].type)
    }

    @Test
    fun `planned duration includes the voice lead-ins`() = runTest {
        val voice = FakeVoice(durationFor = { 1_000L }) // box: 4 voiced? only inhale+exhale phrase = 2 s/cycle
        val sink = RecordingSink()
        engine(this, voice = voice, sink = sink).start()
        advanceTimeBy(4 * 18_000); advanceUntilIdle() // 16 s breath + 2 s lead per cycle
        val result = sink.completed
        assertEquals(4, result?.cyclesCompleted)
        assertEquals(4L * (16_000L + 2_000L), result?.plannedDurationMs)
    }

    @Test
    fun `pre-tick fires one second before each haptic phase end, holds stay silent`() = runTest {
        val haptics = FakeHaptics()
        val sink = RecordingSink()
        engine(this, haptics = haptics, sink = sink).start()

        advanceTimeBy(2_999); runCurrent()
        assertEquals("no ending before the final second", 0, sink.endings.size)
        advanceTimeBy(1); runCurrent()
        assertEquals(1, sink.endings.size)
        assertEquals(PhaseType.INHALE, sink.endings[0].type)
        assertEquals(HapticKind.PRETICK, haptics.starts.last().first)

        advanceTimeBy(4_000); runCurrent() // through HOLD (4000→8000) — no pre-tick, no ending
        assertEquals("holds never fire a pre-tick", 1, sink.endings.size)

        advanceTimeBy(4_000); runCurrent() // EXHALE pre-tick at 8000+3000 = 11000
        assertEquals(2, sink.endings.size)
        assertEquals(PhaseType.EXHALE, sink.endings[1].type)
    }

    @Test
    fun `short phases skip the pre-tick`() = runTest {
        val short = TechniqueConfig(
            id = "short", name = "Short", zhName = "短", description = "",
            phases = listOf(Phase(PhaseType.INHALE, 1_200)), // no voice, no haptic
            cycles = 2,
            soundMode = SoundMode.THETA,
        )
        val sink = RecordingSink()
        val haptics = FakeHaptics()
        SessionEngine(short, FakeVoice(), haptics, sink, scope = this).start()
        advanceTimeBy(2_400); advanceUntilIdle()
        assertEquals(0, sink.endings.size)
        assertEquals(2, sink.cycles.size)
        assertEquals("no pre-tick haptics on short phases", 0, haptics.starts.count { it.first == HapticKind.PRETICK })
    }

    @Test
    fun `cycle sound rotation speaks each sound in order across cycles`() = runTest {
        val liuzijue = TechniqueConfig(
            id = "liuzijue", name = "Liu Zi Jue", zhName = "六字诀", description = "six healing sounds",
            phases = listOf(
                Phase(PhaseType.INHALE, 3000, voicePhrase = "Breathe in"),
                Phase(PhaseType.SOUND_EXHALE, 7000, voicePhrase = "Exhale"),
            ),
            cycles = 6,
            soundMode = SoundMode.THETA,
            cycleSounds = listOf("Xu", "He", "Hu", "Si", "Chui", "Xi"),
        )
        val voice = FakeVoice()
        val sink = RecordingSink()
        val engine = SessionEngine(liuzijue, voice, FakeHaptics(), sink, scope = this)
        engine.start()
        advanceTimeBy(6 * 10_000); advanceUntilIdle()

        val rotationSpeaks = voice.ops.filter { it.startsWith("speak:") }
            .map { it.removePrefix("speak:") }
            .filter { it in listOf("Xu", "He", "Hu", "Si", "Chui", "Xi") }
        assertEquals(listOf("Xu", "He", "Hu", "Si", "Chui", "Xi"), rotationSpeaks)
        assertEquals(6, sink.cycles.size)
    }
}
