# Architecture — 吐纳 Tuna

Single Gradle module `:app`. Package `com.tuna.breathwork`. All state in-memory during a session;
persistence only for settings + session log (DataStore). No network.

## Layer overview

```
UI (Compose)                     ← state, no logic
  Home / Library / Technique / Session / Settings / History
  BreathingGlyph (Canvas vector animation)
Session (engine + drivers)       ← one source of truth
  SessionEngine (coroutine state machine)
  VoiceProvider (TTS) · Haptics · BinauralEngine · GlyphState
Domain (pure data, no Android)
  Phase · TechniqueConfig · SessionResult · MoodTag
Data (persistence)
  SettingsStore (DataStore) · SessionLogStore (DataStore + kotlinx.serialization)
Platform (Android-only)
  CalmNowActivity · GlanceWidget · QuickSettingsTile · HeadphoneDetector
```

## Core interfaces

```kotlin
enum class PhaseType { INHALE, HOLD, EXHALE, SOUND_EXHALE }

data class Phase(
    val type: PhaseType,
    val durationMs: Long,
    val voicePhrase: String? = null,      // enqueued at phase start
    val postureCue: String? = null,       // "Long spine, soft belly…" (varied)
    val haptic: HapticKind = NONE,        // PULSE at inhale, SOFT at exhale
)

data class TechniqueConfig(
    val id: String,                       // "sigh", "box", "478", "coherent", "liuzijue"
    val name: String, val zhName: String, val description: String,
    val phases: List<Phase>,              // one cycle; engine repeats
    val cycles: Int,
    val soundMode: SoundMode,             // THETA (Calm Now default) | ALPHA
    val accentColor: Long,
    val useCase: UseCase,                 // PANIC | ANGER | STRESS | BASELINE | SOUND
)

interface VoiceProvider {
    suspend fun speak(phrase: String)     // non-blocking; engine controls timing
    suspend fun stop()
    fun warmUp()
}

interface SessionSink {                   // what the engine drives
    fun onPhase(phase: Phase, progress: Float)
    fun onCycle(cycle: Int, total: Int)
    fun onComplete(result: SessionResult)
    fun onAbort()
}
```

`SessionEngine` owns a `Channel`/flow of ticks; phases advance on elapsed real time, not on TTS
completion (TTS phrases are kept shorter than phase duration; engine flushes the queue at phase
boundary to prevent bleed). Posture cue slots into the phrase queue every 4th cycle from rotating
templates; suppressed in Calm Now mode.

## Binaural generation

- `BinauralEngine` synthesizes 16-bit stereo PCM at 44.1 kHz on a background thread, in
  ~200 ms chunks reused via ring buffer, streamed to a low-latency `AudioTrack`
  (`MODE_STREAM`, `USAGE_MEDIA`, `CONTENT_TYPE_MUSIC`).
- Theta: L 200 Hz / R 206 Hz (6 Hz beat). Alpha: L 200 Hz / R 210 Hz (10 Hz beat).
- No headphones → mono fallback: `sin(2π·200t) · (0.5 + 0.5·sin(2π·fBeat·t))` pulsing tone.
- Headphones detected via `AudioManager.isWiredHeadsetOn` / Bluetooth A2DP profile.
- Audio focus: `AUDIOFOCUS_GAIN`; on transient loss, pause engine, resume on regain.
- Voice (TTS, stream MUSIC) rides above beats; beats volume fades to ~-18 dB under speech.

## Calm Now path

Widget (Glance) and Quick-Settings tile fire a PendingIntent to `CalmNowActivity`
(`FLAG_ACTIVITY_NEW_TASK | CLEAR_TOP`, dedicated launchMode, no visible back stack). Activity
auto-starts a 2-minute physiological-sigh session with THETA beats, minimal voice, no posture
nagging. Reachable in 1 tap from lock/home.

## Data

- `SettingsStore`: voiceRate, voicePitch, hapticsEnabled (default on for practice, off for Calm
  Now), posturePromptsEnabled (default on), reduceMotion, calmNowHaptics.
- `SessionLogStore`: `List<SessionRecord>` JSON (techniqueId, durationMs, completed, moodTag?,
  timestamp). Mood tag post-session: CALM / NEUTRAL / STILL_STRESSED (skippable).

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| TTS voice missing/unavailable on device | warm-up + `TextToSpeech.OnInitListener`; fallback to visual-only with status line |
| Phase/TTS drift | timer-driven engine; phrase < phase; queue flush per boundary |
| Binaural ineffective on speaker | honest status line + headphone reminder (SPEC D5) |
| Audio focus steal (calls/notifications) | pause/resume engine, keep glyph alive |
| Battery during long sessions | chunked PCM reuse; engine sleeps between ticks; no wake lock beyond keep-screen-on in session |
| Device Android 16 (target 35) quirks | test on connected SM-S918U first; minSdk 26 sanity via emulator if available |

## Testing strategy (SPARC R + C)

- Unit: phase timing math, technique config validity (durations > voice phrase length, cycles ≥ 1),
  serialization round-trip, posture cue rotation, beat-frequency math.
- Device: manual matrix (M6.3) + `adb shell dumpsys audio` sanity for focus, TTS visibility.
- No UI test framework in v1 (personal app); engine is the logic core and is unit-tested.
