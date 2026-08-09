package com.tuna.breathwork.session

import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.domain.PostureCueScheduler
import com.tuna.breathwork.domain.SessionResult
import com.tuna.breathwork.domain.TechniqueConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Voice output boundary. `speak` enqueues and returns (non-blocking in real TTS). */
interface VoiceProvider {
    suspend fun speak(phrase: String)
    suspend fun stop()

    /** Speak and wait until playback finishes — default: fire-and-forget. */
    suspend fun speakAndAwait(phrase: String) {
        speak(phrase)
    }
}

/** Vibration output boundary. [start] begins a pattern for the given phase duration; [stop] silences it. */
interface HapticDriver {
    fun start(kind: HapticKind, durationMs: Long)
    fun stop()
}

/** What the engine reports to its UI. */
interface SessionSink {
    fun onPhase(phase: Phase)
    fun onCycle(cycle: Int, total: Int)
    fun onComplete(result: SessionResult)
    fun onAbort()

    /** Fired ~1 s before a phase ends, so the UI can cue the transition (tick sound, visual flash). */
    fun onPhaseEnding(phase: Phase)
}

/**
 * Timer-driven session state machine. One coroutine per session; phases advance on
 * real elapsed time ([delay]), never on TTS completion. The voice queue is flushed
 * (stop) at every phase boundary so a slow utterance can never bleed into the next
 * phase. Posture cues slot in on every 4th cycle (suppressed in Calm Now).
 */
class SessionEngine(
    private val config: TechniqueConfig,
    private val voice: VoiceProvider,
    private val haptics: HapticDriver,
    private val sink: SessionSink,
    private val calmNow: Boolean = false,
    private val postureEnabled: Boolean = true,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    /** Session-start script; spoken before the first phase. */
    var sessionIntro: String? = "Settle in. Long spine, soft shoulders, relaxed jaw."

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runSession() }
    }

    fun abort() {
        job?.cancel()
    }

    private suspend fun runSession() {
        config.validate()
        try {
            voice.stop()
            sessionIntro?.let { voice.speak(it) }
            val scheduler = PostureCueScheduler(POSTURE_TEMPLATES, calmNow = calmNow)
            var cycle = 0
            while (cycle < config.cycles) {
                val cue = if (postureEnabled) scheduler.cueForCycle(cycle) else null
                val cycleSound = config.cycleSounds?.getOrNull(cycle % config.cycleSounds.size)
                // Cue + cycle sound ride the cycle's longest phase (prefer exhale): there the
                // queued phrases have room alongside the phase's own phrase, so recorded clips
                // never get cut by the phase-boundary flush.
                val cuePhaseIndex = cuePhaseIndex(config)
                config.phases.forEachIndexed { index, phase ->
                    voice.stop()
                    haptics.start(phase.haptic, phase.durationMs)
                    phase.voicePhrase?.let { voice.speak(it) }
                    if (index == cuePhaseIndex) {
                        cue?.let { voice.speak(it) }
                        cycleSound?.let { voice.speak(it) }
                    }
                    sink.onPhase(phase)
                    // Pre-tick ~1 s before the phase ends: the "get ready to switch" cue.
                    if (phase.durationMs > 1_500) {
                        delay(phase.durationMs - 1_000)
                        haptics.start(HapticKind.PRETICK, 80)
                        sink.onPhaseEnding(phase)
                        delay(1_000)
                    } else {
                        delay(phase.durationMs)
                    }
                }
                cycle++
                sink.onCycle(cycle, config.cycles)
            }
            sink.onComplete(
                SessionResult(
                    techniqueId = config.id,
                    plannedDurationMs = config.totalDurationMs,
                    completed = true,
                    cyclesCompleted = cycle,
                )
            )
        } catch (e: CancellationException) {
            // Session aborted (engine.abort or scope teardown): surface as abort, not completion.
            sink.onAbort()
        }
    }

    private fun cuePhaseIndex(config: TechniqueConfig): Int {
        val maxDur = config.phases.maxOf { it.durationMs }
        val exhaleIdx = config.phases.indexOfFirst {
            it.durationMs == maxDur && (it.type == PhaseType.EXHALE || it.type == PhaseType.SOUND_EXHALE)
        }
        return if (exhaleIdx >= 0) exhaleIdx else config.phases.indexOfFirst { it.durationMs == maxDur }
    }

    companion object {
        // Short, calm posture prompts — texts match the bundled recorded clips exactly.
        val POSTURE_TEMPLATES = listOf(
            "Long spine",
            "Drop your shoulders",
            "Relax your jaw",
            "Lengthen your neck",
        )
    }
}
