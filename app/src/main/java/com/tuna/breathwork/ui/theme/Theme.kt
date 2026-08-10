package com.tuna.breathwork.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark, muted, low-stimulation palette (SPEC D10) — soothing lavender family, no green/cyan
val BgDeep = Color(0xFF10131A)
val BgSurface = Color(0xFF1A1F2A)
val BgElevated = Color(0xFF222834)
val Accent = Color(0xFFB4A0E8)      // soft lavender
val AccentDim = Color(0xFF7C6BC0)   // deeper lavender for containers
val TextPrimary = Color(0xFFE6EAF2)
val TextMuted = Color(0xFF8A93A6)
val DangerSoft = Color(0xFFB0564F)

/** Soft night-sky gradient behind every screen — depth without stimulation. */
val NightGradient = Brush.verticalGradient(
    listOf(Color(0xFF1B2232), Color(0xFF10131A), Color(0xFF0C0F15))
)

/** Sits on top of [NightGradient] for every screen. */
@Composable
fun TunaBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(NightGradient), content = content)
}

/** Elevated rounded card with a hairline edge — soft depth without harsh shadows. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    color: Color = BgSurface.copy(alpha = 0.86f),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.25f)),
        shape = shape,
        color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
    ) {
        Box(content = content)
    }
}

private val TunaDarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = BgDeep,
    primaryContainer = AccentDim,
    onPrimaryContainer = TextPrimary,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextMuted,
    outline = TextMuted,
)

@Composable
fun TunaTheme(content: @Composable () -> Unit) {
    // App is dark-first by design; system light theme still maps to the dark palette
    MaterialTheme(
        colorScheme = TunaDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
