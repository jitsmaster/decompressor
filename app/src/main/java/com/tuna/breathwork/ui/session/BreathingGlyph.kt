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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import kotlin.math.min

/**
 * The taiji yin-yang breathing glyph — the visual anchor of every session.
 * Grows on inhale, holds, and shrinks on a long exhale, synced to the phase
 * duration. Reduce-motion mode replaces phase-following motion with a very slow,
 * gentle pulse.
 *
 * Construction (classic layering): dark disc → light right half → light top lobe →
 * dark bottom lobe → opposing dots. The S-curve boundary emerges from the two
 * half-radius lobes.
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

    // Yin (dark) uses the accent; Yang (light) is a soft off-white — reads clearly on the
    // deep background. A faint outer glow keeps it gentle.
    val yin = accent
    val yang = Color(0xFFE6EAF2)

    Canvas(modifier = modifier) {
        val r = min(size.width, size.height) / 2 * scale
        val c = Offset(size.width / 2, size.height / 2)

        // Soft radial glow behind the symbol
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(yin.copy(alpha = 0.22f), yin.copy(alpha = 0.06f), Color.Transparent),
                center = c,
                radius = r * 1.7f,
            ),
            radius = r * 1.7f,
            center = c,
        )

        // Outer ring
        drawCircle(color = yin.copy(alpha = 0.25f), radius = r * 1.18f, center = c, style = Stroke(1.5.dp.toPx()))

        // 1. Dark disc
        drawCircle(color = yin, radius = r, center = c)
        // 2. Light right half
        drawArc(
            color = yang,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(2 * r, 2 * r),
        )
        // 3. Top lobe (light) — left of center, in the dark half
        drawCircle(color = yang, radius = r / 2, center = Offset(c.x, c.y - r / 2))
        // 4. Bottom lobe (dark) — right of center, in the light half
        drawCircle(color = yin, radius = r / 2, center = Offset(c.x, c.y + r / 2))
        // 5. Opposing dots
        drawCircle(color = yin, radius = r / 6, center = Offset(c.x, c.y - r / 2))
        drawCircle(color = yang, radius = r / 6, center = Offset(c.x, c.y + r / 2))
    }
}
