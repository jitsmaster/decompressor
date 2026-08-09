package com.tuna.breathwork.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.ui.theme.BgDeep
import com.tuna.breathwork.ui.theme.TextMuted

private fun phaseWord(type: PhaseType?): String = when (type) {
    PhaseType.INHALE -> "Breathe in"
    PhaseType.HOLD -> "Hold"
    PhaseType.EXHALE -> "Breathe out"
    PhaseType.SOUND_EXHALE -> "Exhale"
    null -> "Begin"
}

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onFinished: () -> Unit,
    onAborted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(state.completed, state.aborted) {
        // nothing to do; UI renders completion/abort states
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: technique + cycle progress + headphone status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.config.zhName,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
                HeadphoneChip(state.headphoneStatus)
                Text(
                    text = "${state.cycle} / ${state.totalCycles}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.completed != null) {
                CompletionPanel(
                    onTag = { tag -> viewModel.tagMood(tag); onFinished() },
                    onSkip = { viewModel.tagMood(null); onFinished() },
                )
            } else {
                BreathingGlyph(
                    phase = state.phase,
                    phaseDurationMs = state.phase?.durationMs ?: 4000L,
                    reduceMotion = viewModel.reduceMotion,
                    accent = Color(viewModel.config.accentColor),
                    modifier = Modifier.size(260.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phaseWord(state.phase?.type),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = viewModel.config.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Button(
                    onClick = { viewModel.abort(); onAborted() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Text("End session")
                }
            }
        }
    }
}

@Composable
private fun HeadphoneChip(status: HeadphoneStatus) {
    val label = when (status) {
        HeadphoneStatus.CHECKING -> "checking audio…"
        HeadphoneStatus.STEREO -> "🎧 binaural beats on"
        HeadphoneStatus.MONO_FALLBACK -> "pulsing tone (headphones off)"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun CompletionPanel(onTag: (MoodTag) -> Unit, onSkip: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Complete", style = MaterialTheme.typography.headlineMedium)
        Text(
            "How do you feel now?",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MoodButton("😌", "Calm") { onTag(MoodTag.CALM) }
            MoodButton("😐", "Neutral") { onTag(MoodTag.NEUTRAL) }
            MoodButton("😣", "Stressed") { onTag(MoodTag.STILL_STRESSED) }
        }
        Surface(
            onClick = onSkip,
            color = Color.Transparent,
            shape = CircleShape,
        ) {
            Text("skip", color = TextMuted, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun MoodButton(emoji: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.size(width = 88.dp, height = 96.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, fontSize = 28.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}
