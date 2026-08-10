package com.tuna.breathwork.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---------- Palette tokens (switched per theme) ----------

data class TunaColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val elevated: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentDim: Color,
    val gradient: Brush,
)

val DarkTunaColors = TunaColors(
    background = Color(0xFF10131A),
    surface = Color(0xFF1A1F2A),
    surfaceVariant = Color(0xFF222834),
    elevated = Color(0xFF2A3140),
    textPrimary = Color(0xFFE6EAF2),
    textMuted = Color(0xFF8A93A6),
    accent = Color(0xFFB4A0E8),
    accentDim = Color(0xFF7C6BC0),
    gradient = Brush.verticalGradient(listOf(Color(0xFF1B2232), Color(0xFF10131A), Color(0xFF0C0F15))),
)

/** Calm light lavender palette — same mood, day-bright. */
val LightTunaColors = TunaColors(
    background = Color(0xFFF5F2EC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECE7F4),
    elevated = Color(0xFFE2DBF0),
    textPrimary = Color(0xFF2A2636),
    textMuted = Color(0xFF8A8496),
    accent = Color(0xFF7C6BC0),
    accentDim = Color(0xFFE4DDF5),
    gradient = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF5F2EC), Color(0xFFEFEBF6))),
)

val LocalTunaColors = staticCompositionLocalOf { DarkTunaColors }

@Composable
@ReadOnlyComposable
fun tunaColors(): TunaColors = LocalTunaColors.current

@Composable
fun TunaBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(tunaColors().gradient), content = content)
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    color: Color = tunaColors().surface.copy(alpha = 0.86f),
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

// ---------- Material schemes ----------

private val TunaDarkScheme = darkColorScheme(
    primary = DarkTunaColors.accent,
    onPrimary = Color(0xFF10131A),
    primaryContainer = DarkTunaColors.accentDim,
    onPrimaryContainer = DarkTunaColors.textPrimary,
    background = DarkTunaColors.background,
    onBackground = DarkTunaColors.textPrimary,
    surface = DarkTunaColors.surface,
    onSurface = DarkTunaColors.textPrimary,
    surfaceVariant = DarkTunaColors.surfaceVariant,
    onSurfaceVariant = DarkTunaColors.textMuted,
    outline = DarkTunaColors.textMuted,
)

private val TunaLightScheme = lightColorScheme(
    primary = LightTunaColors.accent,
    onPrimary = Color.White,
    primaryContainer = LightTunaColors.accentDim,
    onPrimaryContainer = LightTunaColors.textPrimary,
    background = LightTunaColors.background,
    onBackground = LightTunaColors.textPrimary,
    surface = LightTunaColors.surface,
    onSurface = LightTunaColors.textPrimary,
    surfaceVariant = LightTunaColors.surfaceVariant,
    onSurfaceVariant = LightTunaColors.textMuted,
    outline = LightTunaColors.textMuted,
)

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Follow system"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

@Composable
fun TunaTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    android.util.Log.i("TunaTheme", "themeMode=${themeMode.key} dark=$dark")
    val colors = if (dark) DarkTunaColors else LightTunaColors
    CompositionLocalProvider(LocalTunaColors provides colors) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            MaterialTheme(
                colorScheme = if (dark) TunaDarkScheme else TunaLightScheme,
                typography = MaterialTheme.typography,
                content = content,
            )
        }
    }
}

// ---------- Legacy tokens: theme-aware @Composable getters ----------
// All existing call sites keep working unchanged and follow the active theme.

val BgDeep: Color @Composable @ReadOnlyComposable get() = tunaColors().background
val BgSurface: Color @Composable @ReadOnlyComposable get() = tunaColors().surface
val BgElevated: Color @Composable @ReadOnlyComposable get() = tunaColors().elevated
val TextPrimary: Color @Composable @ReadOnlyComposable get() = tunaColors().textPrimary
val TextMuted: Color @Composable @ReadOnlyComposable get() = tunaColors().textMuted
val Accent: Color @Composable @ReadOnlyComposable get() = tunaColors().accent
val AccentDim: Color @Composable @ReadOnlyComposable get() = tunaColors().accentDim
val NightGradient: Brush @Composable @ReadOnlyComposable get() = tunaColors().gradient
