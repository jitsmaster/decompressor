package com.tuna.breathwork.ui.ambient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuna.breathwork.data.AmbientTracks
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.ui.Header
import com.tuna.breathwork.ui.session.BreathingGlyph
import com.tuna.breathwork.ui.theme.Accent
import com.tuna.breathwork.ui.theme.BgDeep
import com.tuna.breathwork.ui.theme.NightGradient
import com.tuna.breathwork.ui.theme.BgSurface
import com.tuna.breathwork.ui.theme.TextMuted

/**
 * Ambient listening: full-session guided tracks (UCLA Mindful, CC BY-NC-ND 4.0)
 * that play as-is rather than through the sync engine. Offline, bundled assets.
 */
@Composable
fun AmbientScreen(onBack: () -> Unit, viewModel: AmbientViewModel) {
    val state by viewModel.state.collectAsState()
    val speechPhase by viewModel.speechPhase.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(NightGradient).padding(24.dp)) {
        Header("Ambient", onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            "Full guided sessions for when you want to be led without the timer.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(20.dp))
        AmbientTracks.all.forEach { track ->
            AmbientTrackRow(
                track = track,
                isPlaying = state.playingTrackId == track.id && state.isPlaying,
                isCurrent = state.playingTrackId == track.id,
                onClick = { viewModel.toggle(track) },
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(16.dp))

        // Breathing glyph following the actual voice: expand on pauses, ease out on speech.
        val phase = speechPhase
        if (phase != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BreathingGlyph(
                        phase = Phase(phase.type, phase.msUntilChange),
                        phaseDurationMs = phase.msUntilChange,
                        reduceMotion = false,
                        accent = Accent,
                        modifier = Modifier.size(150.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (phase.type == PhaseType.INHALE) "inhale with the pause" else "exhale with the voice",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Audio: UCLA Mindful (uclahealth.org/uclamindful) · CC BY-NC-ND 4.0 — personal use.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}

@Composable
private fun AmbientTrackRow(
    track: com.tuna.breathwork.data.AmbientTrack,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else BgSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Accent.copy(alpha = if (isPlaying) 1f else 0.25f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isPlaying) "❚❚" else "▶", color = BgDeep, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(track.attribution, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
