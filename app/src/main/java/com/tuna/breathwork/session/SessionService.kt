package com.tuna.breathwork.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tuna.breathwork.CalmNowActivity
import com.tuna.breathwork.R
import com.tuna.breathwork.TunaApp
import com.tuna.breathwork.data.Preset
import com.tuna.breathwork.data.SessionLogStore
import com.tuna.breathwork.data.SessionRecord
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.data.VoiceLanguage
import com.tuna.breathwork.domain.BinauralSpec
import com.tuna.breathwork.domain.MoodTag
import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.SessionResult
import com.tuna.breathwork.domain.SoundMode
import com.tuna.breathwork.domain.TechniqueConfig
import com.tuna.breathwork.platform.AndroidHaptics
import com.tuna.breathwork.platform.BinauralEngine
import com.tuna.breathwork.platform.HeadphoneDetector
import com.tuna.breathwork.platform.RecordedVoiceProvider
import com.tuna.breathwork.platform.TtsVoiceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Foreground service owning the running session — the session survives the activity
 * (and the screen) being turned off. The UI observes [state]; when it reconnects
 * mid-session it picks up exactly where things are.
 */
class SessionService : Service(), SessionSink {

    private lateinit var config: TechniqueConfig
    private lateinit var voice: RecordedVoiceProvider
    private lateinit var haptics: AndroidHaptics
    private lateinit var beats: BinauralEngine
    private lateinit var engine: SessionEngine
    private lateinit var logStore: SessionLogStore
    private var wakeLock: PowerManager.WakeLock? = null
    private var tickPool: SoundPool? = null
    private var tickId = 0
    private var pendingRecord: SessionRecord? = null
    private var playTicks = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val app: TunaApp get() = applicationContext as TunaApp

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        logStore = SessionLogStore(this)
        running = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            endSession()
            return START_NOT_STICKY
        }
        if (intent == null) return START_NOT_STICKY
        // A new session may arrive while an old one is still held (e.g. the user left the
        // completion panel without tagging): tear the old one down first so the new start
        // is never swallowed by a live engine.
        if (::engine.isInitialized) {
            teardownSession()
        }
        startSession(intent)
        return START_NOT_STICKY
    }

    /** Releases the previous session. If it was mid-run it's logged as abandoned; a completed one is already logged. */
    private fun teardownSession() {
        runCatching { engine.abort() } // no-op if the engine already completed
        scope.launch { voice.stop() }
        haptics.stop()
        beats.stop()
        tickPool?.release()
        tickPool = null
        releaseWakeLock()
        pendingRecord = null
        _state.value = SessionUiState()
    }

    private fun startSession(intent: Intent) {
        val calmNow = intent.getBooleanExtra(EXTRA_CALM_NOW, false)
        val techniqueId = intent.getStringExtra(EXTRA_TECHNIQUE)
        val preset = runCatching {
            Preset.valueOf(intent.getStringExtra(EXTRA_PRESET) ?: "")
        }.getOrDefault(Preset.MEDIUM)

        val settings = runBlocking { app.container.currentSettings() }
        config = if (calmNow) {
            TechniquesRepository.withPreset(TechniquesRepository.byId(settings.calmNowTechniqueId), Preset.CALM_NOW)
        } else {
            TechniquesRepository.withPreset(TechniquesRepository.byId(techniqueId!!), preset)
        }

        val language = VoiceLanguage.fromKey(settings.voiceLanguage)
        android.util.Log.i("TunaVoice", "session language: ${language.key}")
        voice = RecordedVoiceProvider(
            this,
            TtsVoiceProvider(this, settings.voiceRate, settings.voicePitch),
            language = language,
        )
        haptics = AndroidHaptics(this, enabled = if (calmNow) settings.calmNowHaptics else settings.hapticsEnabled)
        beats = BinauralEngine(this)
        engine = SessionEngine(
            config = config,
            voice = voice,
            haptics = haptics,
            sink = this,
            scope = scope,
        ).apply { if (calmNow) sessionIntro = "Begin. Breathe with me." }

        tickPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
        tickId = runCatching { tickPool!!.load(assets.openFd("sfx/tick.mp3"), 1) }.getOrDefault(0)
        playTicks = settings.countdownTicks

        startForeground(
            NOTIF_ID,
            buildNotification("Preparing…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (settings.allowScreenOff) acquireWakeLock()
        _state.update { it.copy(totalCycles = config.cycles) }

        scope.launch {
            _state.update { it.copy(headphoneStatus = HeadphoneStatus.CHECKING) }
            val stereo = withContext(Dispatchers.IO) { HeadphoneDetector(this@SessionService).hasHeadphones() }
            _state.update { it.copy(headphoneStatus = if (stereo) HeadphoneStatus.STEREO else HeadphoneStatus.MONO_FALLBACK) }
            if (!stereo) {
                voice.speakAndAwait("Put on headphones for the full effect.")
            }
            beats.start(spec = specFor(config.soundMode), stereo = stereo, onFocusLost = { endSession() })
            engine.start()
            _state.update { it.copy(sessionStarted = true) }
            updateNotification()
        }
    }

    private fun endSession() {
        haptics.stop()
        runCatching { engine.abort() }
        stopSelf()
    }

    private fun tagMood(tag: MoodTag?) {
        val record = pendingRecord ?: return
        scope.launch { logStore.updateMood(record.timestampEpochMs, tag) }
        pendingRecord = null
        stopSelf()
    }

    // --- SessionSink ---

    override fun onPhase(phase: Phase) {
        _state.update { it.copy(phase = phase, phaseStartedAtMs = System.currentTimeMillis(), voiceCue = false) }
        updateNotification()
    }

    override fun onVoiceCue(phase: Phase) {
        _state.update { it.copy(voiceCue = true) }
    }

    override fun onCycle(cycle: Int, total: Int) {
        _state.update { it.copy(cycle = cycle) }
        updateNotification()
    }

    override fun onComplete(result: SessionResult) {
        releaseWakeLock()
        haptics.stop()
        beats.stop()
        val record = SessionRecord(
            techniqueId = config.id,
            durationMs = result.plannedDurationMs,
            completed = true,
            moodTag = null,
            timestampEpochMs = System.currentTimeMillis(),
        )
        // Persist immediately so a completed session is never lost (screen off, crash);
        // the mood tag, if given, patches this record in place.
        pendingRecord = record
        scope.launch { logStore.append(record) }
        _state.update { it.copy(completed = result) }
        updateNotification()
    }

    override fun onAbort() {
        releaseWakeLock()
        haptics.stop()
        beats.stop()
        val record = SessionRecord(
            techniqueId = config.id,
            durationMs = 0,
            completed = false,
            timestampEpochMs = System.currentTimeMillis(),
        )
        scope.launch { logStore.append(record) }
        _state.update { it.copy(aborted = true) }
    }

    override fun onPhaseEnding(phase: Phase) {
        if (playTicks) tickPool?.play(tickId, 0.5f, 0.5f, 1, 0, 1f)
    }

    override fun onDestroy() {
        pendingRecord = null // already persisted without mood on completion; abort path persists itself
        if (::engine.isInitialized) runCatching { engine.abort() }
        haptics.stop()
        beats.stop()
        tickPool?.release()
        releaseWakeLock()
        if (running === this) running = null
        super.onDestroy()
    }

    // --- helpers ---

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tuna:session")
            .apply { acquire(20 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        wakeLock = null
    }

    private fun specFor(mode: SoundMode): BinauralSpec =
        when (mode) {
            SoundMode.THETA -> BinauralSpec.THETA
            SoundMode.ALPHA -> BinauralSpec.ALPHA
        }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Breath session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Ongoing breathing session"
            setSound(null, null)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tuna)
            .setContentTitle("吐纳 · ${config.name}")
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, CalmNowActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .addAction(
                0,
                "End",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, SessionService::class.java).setAction(ACTION_END),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setSilent(true)
            .build()

    private fun updateNotification() {
        val s = _state.value
        val content = when {
            s.completed != null -> "Session complete"
            s.aborted -> "Session ended"
            s.phase != null -> "· ${s.phase.type.name.lowercase()}  ${s.cycle}/${s.totalCycles}"
            else -> "…"
        }
        runCatching {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, buildNotification(content))
        }
    }

    companion object {
        private const val CHANNEL_ID = "session"
        private const val NOTIF_ID = 1
        const val ACTION_END = "tuna.session.end"
        const val EXTRA_TECHNIQUE = "technique"
        const val EXTRA_PRESET = "preset"
        const val EXTRA_CALM_NOW = "calm_now"

        private val _state = MutableStateFlow(SessionUiState())
        val state: StateFlow<SessionUiState> = _state.asStateFlow()

        @Volatile
        var running: SessionService? = null
            private set

        fun start(context: Context, techniqueId: String? = null, preset: Preset = Preset.MEDIUM, calmNow: Boolean = false) {
            context.startForegroundService(
                Intent(context, SessionService::class.java)
                    .putExtra(EXTRA_TECHNIQUE, techniqueId)
                    .putExtra(EXTRA_PRESET, preset.name)
                    .putExtra(EXTRA_CALM_NOW, calmNow)
            )
        }

        fun end(context: Context) {
            context.startService(Intent(context, SessionService::class.java).setAction(ACTION_END))
        }

        fun tagMood(tag: MoodTag?) {
            running?.tagMood(tag)
        }

        /** Fresh state for a new session (old sessions' completion UI should have been consumed). */
        fun resetForNewSession() {
            _state.value = SessionUiState()
        }
    }
}
