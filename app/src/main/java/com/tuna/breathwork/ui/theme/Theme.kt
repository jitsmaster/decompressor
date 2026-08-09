package com.tuna.breathwork.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark, muted, low-stimulation palette (SPEC D10)
val BgDeep = Color(0xFF10131A)
val BgSurface = Color(0xFF1A1F2A)
val BgElevated = Color(0xFF222834)
val TealAccent = Color(0xFF4FD1C5)
val TealDim = Color(0xFF2E8F88)
val TextPrimary = Color(0xFFE6EAF2)
val TextMuted = Color(0xFF8A93A6)
val DangerSoft = Color(0xFFB0564F)

private val TunaDarkScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = BgDeep,
    primaryContainer = TealDim,
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
