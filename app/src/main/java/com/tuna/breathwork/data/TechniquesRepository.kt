package com.tuna.breathwork.data

import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.domain.SoundMode
import com.tuna.breathwork.domain.TechniqueConfig
import com.tuna.breathwork.domain.UseCase
import com.tuna.breathwork.domain.cyclesForDuration

enum class Preset(val label: String, val durationMs: Long) {
    CALM_NOW("2 min", 120_000),
    SHORT("5 min", 300_000),
    MEDIUM("10 min", 600_000),
    LONG("15 min", 900_000),
}

/**
 * The five data-driven techniques (SPEC D3). Every timing lives here as data;
 * adding a technique is a new config, not a code change.
 */
object TechniquesRepository {

    private val sigh = TechniqueConfig(
        id = "sigh",
        name = "Physiological Sigh",
        zhName = "生理叹息",
        description = "Double inhale, long exhale — the fastest acute calm (Stanford 2023).",
        phases = listOf(
            Phase(PhaseType.INHALE, 4_600, "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.INHALE, 4_600, "and in"),
            Phase(PhaseType.EXHALE, 7_000, "Let it all out", haptic = HapticKind.SOFT),
        ),
        cycles = 7, // 16.2 s × 7 ≈ 1:53 — the Calm Now preset
        soundMode = SoundMode.THETA,
        accentColor = 0xFFB4A0E8,
        useCase = UseCase.PANIC,
    )

    private val box = TechniqueConfig(
        id = "box",
        name = "Box Breathing",
        zhName = "方盒呼吸",
        description = "Equal four-phase rhythm (5-5-5-5) that interrupts rising anger (SEAL technique).",
        phases = listOf(
            Phase(PhaseType.INHALE, 5_000, "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.HOLD, 5_000, haptic = HapticKind.NONE), // silent — the countdown + pre-tick carry the timing
            Phase(PhaseType.EXHALE, 5_000, "Breathe out", haptic = HapticKind.SOFT),
            Phase(PhaseType.HOLD, 5_000, haptic = HapticKind.NONE),
        ),
        cycles = cyclesForDuration(Preset.MEDIUM.durationMs, 20_000),
        soundMode = SoundMode.ALPHA,
        accentColor = 0xFFD9A58B,
        useCase = UseCase.ANGER,
    )

    private val fourSevenEight = TechniqueConfig(
        id = "478",
        name = "4-7-8",
        zhName = "四七八呼吸",
        description = "The natural tranquilizer — for stress and winding down (Dr. Weil).",
        phases = listOf(
            Phase(PhaseType.INHALE, 5_000, "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.HOLD, 7_000, "Hold"),
            Phase(PhaseType.EXHALE, 8_000, "Let it go", haptic = HapticKind.SOFT),
        ),
        cycles = cyclesForDuration(Preset.MEDIUM.durationMs, 20_000),
        soundMode = SoundMode.THETA,
        accentColor = 0xFF9DB8E8,
        useCase = UseCase.STRESS,
    )

    private val coherent = TechniqueConfig(
        id = "coherent",
        name = "Coherent Breathing",
        zhName = "谐振呼吸",
        description = "~5.5 breaths per minute — resonance breathing for your daily baseline.",
        phases = listOf(
            Phase(PhaseType.INHALE, 5_000, "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.EXHALE, 7_000, "Breathe out", haptic = HapticKind.SOFT),
        ),
        cycles = cyclesForDuration(Preset.MEDIUM.durationMs, 12_000),
        soundMode = SoundMode.THETA,
        accentColor = 0xFFE2C79E,
        useCase = UseCase.BASELINE,
    )

    private val liuzijue = TechniqueConfig(
        id = "liuzijue",
        name = "Liu Zi Jue",
        zhName = "六字诀",
        description = "Six healing sounds on the exhale — the ancient Chinese release practice.",
        phases = listOf(
            Phase(PhaseType.INHALE, 5_000, "Breathe in", haptic = HapticKind.PULSE),
            Phase(PhaseType.SOUND_EXHALE, 9_000, "Exhale", haptic = HapticKind.SOFT),
        ),
        cycles = 18, // six sounds × 3 rounds ≈ 4.2 min
        soundMode = SoundMode.THETA,
        accentColor = 0xFFC79ED2,
        useCase = UseCase.SOUND,
        cycleSounds = listOf("Xu", "He", "Hu", "Si", "Chui", "Xi"),
    )

    val all: List<TechniqueConfig> = listOf(sigh, box, fourSevenEight, coherent, liuzijue)

    fun byId(id: String): TechniqueConfig = all.first { it.id == id }

    /** Calm Now is always the 2-minute physiological sigh (SPEC D2). */
    val calmNow: TechniqueConfig = sigh

    /** Same technique at a different session length. Calm Now is always ~2 minutes. */
    fun withPreset(technique: TechniqueConfig, preset: Preset): TechniqueConfig {
        val cycleMs = technique.phases.sumOf { it.durationMs }
        return technique.copy(cycles = cyclesForDuration(preset.durationMs, cycleMs))
    }
}
