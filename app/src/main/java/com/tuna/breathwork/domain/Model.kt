package com.tuna.breathwork.domain

import kotlin.math.roundToInt

enum class PhaseType { INHALE, HOLD, EXHALE, SOUND_EXHALE }

enum class HapticKind { NONE, PULSE, SOFT }

enum class SoundMode { THETA, ALPHA }

enum class UseCase { PANIC, ANGER, STRESS, BASELINE, SOUND }

/**
 * Outcome of a finished session. `completed=false` is reserved for later use
 * (e.g. timed-out sessions); aborted sessions produce no result at all.
 */
data class SessionResult(
    val techniqueId: String,
    val plannedDurationMs: Long,
    val completed: Boolean,
    val cyclesCompleted: Int,
)
data class BinauralSpec(val leftHz: Double, val rightHz: Double) {
    val beatHz: Double get() = rightHz - leftHz

    companion object {
        /** 6 Hz beat — drowsy, inward, meditative → Calm Now default. */
        val THETA = BinauralSpec(200.0, 206.0)

        /** 10 Hz beat — calm-but-alert → anger path (box breathing). */
        val ALPHA = BinauralSpec(200.0, 210.0)
    }
}

/**
 * A single breath phase. Voice phrase must fit inside the phase duration with margin
 * (engine flushes the TTS queue at phase boundaries, so overshooting would cut words).
 */
data class Phase(
    val type: PhaseType,
    val durationMs: Long,
    val voicePhrase: String? = null,
    val postureCue: String? = null,
    val haptic: HapticKind = HapticKind.NONE,
)

/**
 * One cycle of a technique; the engine repeats it [cycles] times.
 */
data class TechniqueConfig(
    val id: String,
    val name: String,
    val zhName: String,
    val description: String,
    val phases: List<Phase>,
    val cycles: Int,
    val soundMode: SoundMode,
    val accentColor: Long = 0xFF4FD1C5,
    val useCase: UseCase = UseCase.STRESS,
    /** Rotating per-cycle phrases (e.g. Liu Zi Jue's six sounds), spoken at cycle start. */
    val cycleSounds: List<String>? = null,
) {
    val totalDurationMs: Long
        get() = cycles * phases.sumOf { it.durationMs }

    /** Voice phrases must leave headroom in their phase (default: phrase ≤ 80% of phase). */
    fun validate() {
        require(cycles >= 1) { "cycles must be >= 1" }
        require(phases.isNotEmpty()) { "phases must not be empty" }
        phases.forEachIndexed { i, phase ->
            require(phase.durationMs > 0) { "phase $i duration must be > 0" }
            phase.voicePhrase?.let { phrase ->
                require(phrase.length <= phase.durationMs / 80) {
                    "phase $i voice phrase too long for its ${phase.durationMs}ms duration: \"$phrase\""
                }
            }
        }
    }
}

/** Round the number of cycles that best fills [targetMs] given one cycle of [cycleMs] (min 1). */
fun cyclesForDuration(targetMs: Long, cycleMs: Long): Int =
    ((targetMs.toDouble() / cycleMs).roundToInt()).coerceAtLeast(1)
