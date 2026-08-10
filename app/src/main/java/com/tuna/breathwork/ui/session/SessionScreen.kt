package com.tuna.breathwork.ui.session

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuna.breathwork.data.VoiceLanguage
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.PhaseType
import com.tuna.breathwork.session.HeadphoneStatus
import com.tuna.breathwork.ui.theme.BgDeep
import com.tuna.breathwork.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlin.math.ceil

private fun phaseWord(type: PhaseType?, language: VoiceLanguage): String = when (language) {
    VoiceLanguage.ZH -> when (type) {
        PhaseType.INHALE -> "吸气"
        PhaseType.HOLD -> "屏住"
        PhaseType.EXHALE, PhaseType.SOUND_EXHALE -> "呼气"
        null -> "开始"
    }
    VoiceLanguage.EN -> when (type) {
        PhaseType.INHALE -> "Breathe in"
        PhaseType.HOLD -> "Hold"
        PhaseType.EXHALE, PhaseType.SOUND_EXHALE -> "Breathe out"
        null -> "Begin"
    }
}

@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onFinished: () -> Unit,
    onAborted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Screen policy: allowScreenOff=true → user may switch the display off (wakelock keeps
    // the rhythm); false → keep the screen on for the whole session.
    val view = LocalView.current
    DisposableEffect(viewModel.allowScreenOff) {
        if (!viewModel.allowScreenOff) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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
                val phase = state.phase
                val phaseStartedAt = state.phaseStartedAtMs
                val voiceCue = state.voiceCue
                var remainingMs by remember { mutableLongStateOf(0L) }
                LaunchedEffect(phase, phaseStartedAt, state.completed, voiceCue) {
                    if (voiceCue) {
                        remainingMs = 0 // the breath hasn't started; the ring waits
                        return@LaunchedEffect
                    }
                    while (phase != null && state.completed == null && !state.voiceCue) {
                        val elapsed = System.currentTimeMillis() - phaseStartedAt
                        remainingMs = (phase.durationMs - elapsed).coerceAtLeast(0)
                        delay(100)
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    BreathingGlyph(
                        phase = phase,
                        phaseDurationMs = phase?.durationMs ?: 4000L,
                        reduceMotion = viewModel.reduceMotion,
                        accent = Color(viewModel.config.accentColor),
                        modifier = Modifier.size(260.dp),
                    )
                    CountdownRing(
                        fractionRemaining = if (phase != null && phase.durationMs > 0) {
                            remainingMs.toFloat() / phase.durationMs
                        } else 0f,
                        accent = Color(viewModel.config.accentColor),
                        modifier = Modifier.size(300.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (voiceCue) {
                        // Heads-up: the voice is speaking — get ready; the breath starts when it ends.
                        Text(
                            text = "${phaseWord(phase?.type, viewModel.language)}…",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            color = TextMuted,
                        )
                        Text(
                            text = "listen…",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    } else {
                        Text(
                            text = phaseWord(phase?.type, viewModel.language),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = if (phase != null && remainingMs > 0) {
                                "${ceil(remainingMs / 1000.0).toInt()}"
                            } else "",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        text = viewModel.config.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
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
private fun CountdownRing(fractionRemaining: Float, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 5.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = accent.copy(alpha = 0.12f),
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 360f * fractionRemaining.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
        )
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
