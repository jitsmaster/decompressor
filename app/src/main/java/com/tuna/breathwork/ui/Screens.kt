package com.tuna.breathwork.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.tuna.breathwork.data.Preset
import com.tuna.breathwork.data.VoiceLanguage
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.data.SettingsStore
import com.tuna.breathwork.data.UsageAnalytics
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.SoundMode
import com.tuna.breathwork.domain.TechniqueConfig
import com.tuna.breathwork.domain.UseCase
import com.tuna.breathwork.platform.VoiceSampler
import com.tuna.breathwork.ui.theme.Accent
import com.tuna.breathwork.ui.theme.NightGradient
import com.tuna.breathwork.ui.theme.AccentDim
import com.tuna.breathwork.ui.theme.BgDeep
import com.tuna.breathwork.ui.theme.BgElevated
import com.tuna.breathwork.ui.theme.BgSurface
import com.tuna.breathwork.ui.theme.SoftCard
import com.tuna.breathwork.ui.theme.TextMuted
import com.tuna.breathwork.ui.theme.TextPrimary

// ---------- Home ----------

@Composable
fun HomeScreen(
    onCalmNow: () -> Unit,
    onLibrary: () -> Unit,
    onAmbient: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(NightGradient).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "吐纳",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
            )
            Text(
                "Tuna · guided breathwork · binaural beats",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(34.dp))

            // Calm Now hero: gradient panel + taiji watermark + glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF9A88E8), Color(0xFF6C5BB8), Color(0xFF4A3F86))
                        )
                    )
                    .clickable(onClick = onCalmNow),
            ) {
                MiniTaiji(
                    modifier = Modifier.align(Alignment.CenterEnd).size(210.dp).padding(end = 6.dp),
                    color = Color.White,
                    alpha = 0.10f,
                )
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 26.dp),
                ) {
                    Text(
                        "Calm Now",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "One tap · ~2 minutes · theta beats\nNothing to choose. Just breathe.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 21.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            SoftCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLibrary),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🧘", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Practice Library", style = MaterialTheme.typography.titleMedium)
                        Text("5 techniques · 5 / 10 / 15 min", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("›", color = TextMuted, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            SoftCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAmbient),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE2C79E).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🎧", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ambient", style = MaterialTheme.typography.titleMedium)
                        Text("Guided sessions · EN + 普通话", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("›", color = TextMuted, fontSize = 22.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomePill("◷  History", onHistory, Modifier.weight(1f))
            HomePill("⚙  Settings", onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomePill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = BgSurface.copy(alpha = 0.7f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = TextMuted, letterSpacing = 0.5.sp)
        }
    }
}

// ---------- Library ----------

@Composable
fun LibraryScreen(onBack: () -> Unit, onTechnique: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NightGradient).padding(24.dp)) {
        Header("Practice Library", onBack)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(TechniquesRepository.all, key = { it.id }) { technique ->
                TechniqueCard(technique) { onTechnique(technique.id) }
            }
        }
    }
}

@Composable
private fun TechniqueCard(technique: TechniqueConfig, onClick: () -> Unit) {
    SoftCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // accent edge
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(technique.accentColor))
            )
            Row(
                modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${technique.zhName} · ${technique.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(technique.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(Modifier.width(12.dp))
                UseCaseChip(useCaseLabel(technique.useCase), Color(technique.accentColor))
            }
        }
    }
}

// ---------- Technique detail ----------

@Composable
fun TechniqueDetailScreen(
    techniqueId: String,
    onBack: () -> Unit,
    onStart: (String, Preset) -> Unit,
) {
    val technique = TechniquesRepository.byId(techniqueId)
    Column(modifier = Modifier.fillMaxSize().background(NightGradient).padding(24.dp)) {
        Header(technique.name, onBack)
        Spacer(Modifier.height(12.dp))
        Text(technique.zhName, style = MaterialTheme.typography.headlineMedium, color = Color(technique.accentColor))
        Spacer(Modifier.height(8.dp))
        Text(technique.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        SoundModeRow(technique.soundMode)
        Spacer(Modifier.height(32.dp))
        Text("Session length", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Preset.SHORT, Preset.MEDIUM, Preset.LONG).forEach { preset ->
                Button(
                    onClick = { onStart(techniqueId, preset) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(technique.accentColor)),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Text(preset.label, color = Color(0xFF10131A), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SoundModeRow(mode: SoundMode) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (mode == SoundMode.THETA) "Theta beats · 6 Hz" else "Alpha beats · 10 Hz",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (mode == SoundMode.THETA) "deep inward calm" else "calm-but-alert",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

// ---------- Settings ----------

@Composable
fun SettingsScreen(onBack: () -> Unit, settingsStore: SettingsStore) {
    val settings by settingsStore.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sampler by remember { mutableStateOf<VoiceSampler?>(null) }
    DisposableEffect(Unit) {
        onDispose { sampler?.stop() }
    }
    Column(modifier = Modifier.fillMaxSize().background(NightGradient).padding(24.dp)) {
        Header("Settings", onBack)
        Spacer(Modifier.height(16.dp))
        val s = settings ?: return@Column

        Text("Voice", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        VoiceLanguage.entries.forEach { lang ->
            val selected = s.voiceLanguage == lang.key
            Surface(
                onClick = {
                    scope.launch { settingsStore.update { it.copy(voiceLanguage = lang.key) } }
                    sampler?.stop()
                    sampler = VoiceSampler(context, lang)
                    sampler?.play()
                },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) AccentDim else BgSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(if (selected) "✓" else "", color = Accent)
                }
            }
        }
        Surface(
            onClick = {
                val lang = VoiceLanguage.fromKey(s.voiceLanguage)
                sampler?.stop()
                sampler = VoiceSampler(context, lang)
                sampler?.play()
            },
            shape = RoundedCornerShape(16.dp),
            color = AccentDim,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text("▶ Test voice", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Calm Now exercise", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        TechniquesRepository.all.forEach { technique ->
            val selected = s.calmNowTechniqueId == technique.id
            Surface(
                onClick = { scope.launch { settingsStore.update { it.copy(calmNowTechniqueId = technique.id) } } },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) AccentDim else BgSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(12.dp).background(Color(technique.accentColor), CircleShape)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("${technique.zhName} · ${technique.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(if (selected) "✓" else "", color = Accent)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        ToggleRow("Haptic pulse (practice)", s.hapticsEnabled) {
            scope.launch { settingsStore.update { it.copy(hapticsEnabled = !it.hapticsEnabled) } }
        }
        ToggleRow("Haptic pulse (Calm Now)", s.calmNowHaptics) {
            scope.launch { settingsStore.update { it.copy(calmNowHaptics = !it.calmNowHaptics) } }
        }
        ToggleRow("Countdown ticks", s.countdownTicks) {
            scope.launch { settingsStore.update { it.copy(countdownTicks = !it.countdownTicks) } }
        }
        ToggleRow("Allow screen off during sessions", s.allowScreenOff) {
            scope.launch { settingsStore.update { it.copy(allowScreenOff = !it.allowScreenOff) } }
        }
        ToggleRow("Reduce motion", s.reduceMotion) {
            scope.launch { settingsStore.update { it.copy(reduceMotion = !it.reduceMotion) } }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = BgSurface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (checked) "on" else "off", color = if (checked) Accent else TextMuted)
        }
    }
}

// ---------- History ----------

@Composable
fun HistoryScreen(onBack: () -> Unit, logStore: com.tuna.breathwork.data.SessionLogStore) {
    val log by logStore.log.collectAsState(initial = null)
    Column(modifier = Modifier.fillMaxSize().background(NightGradient).padding(24.dp)) {
        Header("History", onBack)
        Spacer(Modifier.height(16.dp))
        val current = log ?: return@Column

        // One LazyColumn owns the whole scroll: the sessions list can grow unbounded, so a
        // fixed header + nested LazyColumn (the old layout) overflowed and couldn't scroll.
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current.records.isEmpty()) {
                item {
                    Text("No sessions yet. Your first breath is the start.", color = TextMuted)
                }
                return@LazyColumn
            }

            // ---- Insights: usage + mood analysis ----
            val totals = UsageAnalytics.totals(current.records)
            val distribution = UsageAnalytics.moodDistribution(current.records)
            val calmRates = UsageAnalytics.calmRatePerTechnique(current.records)
            val perDay = UsageAnalytics.sessionsPerDay(current.records, System.currentTimeMillis(), days = 7)

            item {
                Text("Mood trend — does it work?", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(8.dp))
            }
            TechniquesRepository.all.forEach { technique ->
                val trend = current.moodTrend(technique.id)
                if (trend.values.any { it > 0 }) {
                    item(key = "trend_${technique.id}") { TrendRow(technique, trend) }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Insights", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(8.dp))
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = BgSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${totals.sessionCount} sessions · ${totals.practiceMinutes} min practice" +
                                (totals.mostUsedTechnique?.let { " · most: ${TechniquesRepository.byId(it).zhName}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            MoodTagText(MoodTag.CALM, distribution[MoodTag.CALM] ?: 0)
                            MoodTagText(MoodTag.NEUTRAL, distribution[MoodTag.NEUTRAL] ?: 0)
                            MoodTagText(MoodTag.STILL_STRESSED, distribution[MoodTag.STILL_STRESSED] ?: 0)
                        }
                        Text("Calm rate by technique", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        calmRates.entries.sortedByDescending { it.value }.forEach { (id, rate) ->
                            CalmRateBar(id, rate)
                        }
                        Text("Sessions · last 7 days", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            perDay.forEach { count ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (count > 0) AccentDim else BgElevated,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((14 + 10 * count).dp),
                                ) {}
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Sessions", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(8.dp))
            }
            items(current.records.reversed(), key = { it.timestampEpochMs }) { record ->
                val name = runCatching { TechniquesRepository.byId(record.techniqueId).name }.getOrDefault(record.techniqueId)
                Surface(shape = RoundedCornerShape(14.dp), color = BgSurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            record.moodTag?.let { moodLabel(it) } ?: (if (record.completed) "done" else "ended"),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendRow(technique: TechniqueConfig, trend: Map<MoodTag, Int>) {    Surface(shape = RoundedCornerShape(14.dp), color = BgSurface, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(technique.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("😌${trend[MoodTag.CALM]}  😐${trend[MoodTag.NEUTRAL]}  😣${trend[MoodTag.STILL_STRESSED]}",
                style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}

// ---------- Shared ----------

@Composable
fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onBack, shape = CircleShape, color = BgSurface, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("←", color = TextMuted) }
        }
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

private fun useCaseLabel(useCase: UseCase): String = when (useCase) {
    UseCase.PANIC -> "panic"
    UseCase.ANGER -> "anger"
    UseCase.STRESS -> "stress"
    UseCase.BASELINE -> "daily"
    UseCase.SOUND -> "sound"
}

private fun moodLabel(tag: MoodTag): String = when (tag) {
    MoodTag.CALM -> "😌 calm"
    MoodTag.NEUTRAL -> "😐 neutral"
    MoodTag.STILL_STRESSED -> "😣 stressed"
}

@Composable
private fun MoodTagText(tag: MoodTag, count: Int) {
    Text("${moodEmoji(tag)} $count", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
}

@Composable
private fun CalmRateBar(techniqueId: String, rate: Double) {
    val technique = TechniquesRepository.byId(techniqueId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(technique.name, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(130.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(BgElevated, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rate.toFloat())
                    .height(8.dp)
                    .background(Color(technique.accentColor), RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

private fun moodEmoji(tag: MoodTag): String = when (tag) {
    MoodTag.CALM -> "😌"
    MoodTag.NEUTRAL -> "😐"
    MoodTag.STILL_STRESSED -> "😣"
}
