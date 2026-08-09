package com.tuna.breathwork.ui.session

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import kotlin.math.min

/**
 * The vector-art breathing glyph — the visual anchor of every session (SPEC D10).
 * A soft circle (plus two glow rings) that grows on inhale, holds, and shrinks on
 * a long exhale, synced to the phase duration. Reduce-motion mode replaces the
 * phase-following motion with a very slow, gentle pulse.
 */
@Composable
fun BreathingGlyph(
    phase: Phase?,
    phaseDurationMs: Long,
    reduceMotion: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val targetScale = when (phase?.type) {
        PhaseType.INHALE -> 1.35f
        PhaseType.HOLD -> 1.2f
        PhaseType.EXHALE, PhaseType.SOUND_EXHALE -> 0.7f
        null -> 1f
    }
    val animationSpec: androidx.compose.animation.core.AnimationSpec<Float> = if (reduceMotion) {
        tween(durationMillis = 4000, easing = LinearEasing) // slow, undirected pulse
    } else {
        tween(durationMillis = phaseDurationMs.toInt().coerceAtLeast(200), easing = FastOutSlowInEasing)
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = animationSpec,
        label = "glyphScale",
    )

    Canvas(modifier = modifier) {
        val r = min(size.width, size.height) / 2 * scale
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(color = accent.copy(alpha = 0.10f), radius = r * 1.35f, center = center)
        drawCircle(color = accent.copy(alpha = 0.22f), radius = r * 1.12f, center = center)
        drawCircle(color = accent, radius = r, center = center)
    }
}
