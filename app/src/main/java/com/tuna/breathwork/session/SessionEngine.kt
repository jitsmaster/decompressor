package com.tuna.breathwork.session

import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.domain.Phase
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
}

/** Vibration output boundary. */
interface HapticDriver {
    fun fire(kind: HapticKind)
}

/** What the engine reports to its UI. */
interface SessionSink {
    fun onPhase(phase: Phase)
    fun onCycle(cycle: Int, total: Int)
    fun onComplete(result: SessionResult)
    fun onAbort()
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
                config.phases.forEachIndexed { index, phase ->
                    voice.stop()
                    phase.voicePhrase?.let { voice.speak(it) }
                    if (index == 0) {
                        cue?.let { voice.speak(it) }
                        cycleSound?.let { voice.speak(it) }
                    }
                    haptics.fire(phase.haptic)
                    sink.onPhase(phase)
                    delay(phase.durationMs)
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

    companion object {
        val POSTURE_TEMPLATES = listOf(
            "Long spine, soft belly",
            "Let your shoulders drop",
            "Unclench your jaw",
            "Lengthen through your neck",
        )
    }
}
