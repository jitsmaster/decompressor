# 吐纳 Tuna — Guided Breathwork

Personal Android app for reducing stress with voice-guided breathing exercises and binaural
beats. Built for panic attacks, obsessive breath-holding, rising anger, and daily baseline
stress. Offline-first, no accounts, no data leaves the device.

## The four moments

| Moment | Technique | Beat mode |
|---|---|---|
| Panic attack | Physiological Sigh (2-min Calm Now) | Theta 6 Hz — deep inward calm |
| Anger rising | Box Breathing 4-4-4-4 | Alpha 10 Hz — calm-but-alert |
| Obsessive breath-holding | Long-exhale patterns (sigh, 4-7-8) | Theta |
| General stress | Coherent Breathing, 4-7-8, Liu Zi Jue | Theta |

## Features

- **Calm Now** — one-tap emergency mode from a home-screen widget or quick-settings tile.
  Zero choices, zero menus: 2 minutes of physiological sigh + theta beats + voice guidance.
- **5 data-driven techniques** — Physiological Sigh, Box 4-4-4-4, 4-7-8, Coherent Breathing,
  and Liu Zi Jue 六字诀 (six healing sounds, rotating per cycle). Each is a config, not code.
- **Binaural beats only** (no noise colors) — theta 200/206 Hz and alpha 200/210 Hz,
  generated procedurally in-app. Headphone detection: with headphones → true stereo
  entrainment; without → honest status line + mono pulsing tone that also paces the breath.
- **Voice-driven** — Android TTS, slowed and lowered, behind a `VoiceProvider` seam
  (pre-recorded/self-recorded voice can drop in later without touching the engine).
  Reminds you to put on headphones when beats start without them.
- **Posture prompts** — session start + every 4th breath with varied phrasing; suppressed in
  Calm Now; toggleable.
- **Haptics** — breath-synced pulses in practice mode (off in Calm Now by default).
- **Vector-art breathing glyph** — the visual anchor; reduce-motion toggle.
- **Zero gamification** — local session log + optional one-tap mood tag, mood trends per
  technique. No streaks, no notifications, no nagging.
- **No live HR during sessions** — real-time heart-rate display is a documented panic trap;
  post-session camera HRV is a v2 idea.

## Build & install

Requirements: JDK 17 (`/opt/homebrew/opt/openjdk@17`), Android SDK (compileSdk 35,
build-tools 34), Gradle wrapper 8.11.1.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Add the "吐纳 Calm Now" widget to your home screen, or add the tile to quick settings.

## Architecture

```
UI (Compose)                     ← state, no logic
  Home / Library / Detail / Session / Settings / History
  BreathingGlyph (Canvas vector animation)
Session                          ← one source of truth (pure, unit-tested)
  SessionEngine (coroutine state machine, virtual-time tested)
  VoiceProvider · HapticDriver · SessionSink seams
Domain (pure data, no Android)
  Phase · TechniqueConfig · BinauralSpec · PostureCueScheduler · SessionResult
Data
  TechniquesRepository · SettingsStore (DataStore) · SessionLogStore (DataStore + JSON)
Platform (Android-only)
  TtsVoiceProvider · AndroidHaptics · BinauralEngine (AudioTrack PCM) ·
  HeadphoneDetector · CalmNowActivity · GlanceWidget · QuickSettingsTile
```

See `docs/SPEC.md` and `docs/ARCHITECTURE.md` for the full design, and
`documentation/superpowers/plans/tuna-breathwork.md` for the task plan.

## Testing

31 JVM unit tests (red → green, seams pre-agreed): domain/config validity, posture-cue
rotation, beat math, session-engine timing/cycles/abort/voice-flush/rotation (virtual time),
log codec + mood trends. Device verification was done manually on a Galaxy S23 Ultra
(Android 16): full Calm Now loop, practice sessions, abort, history, widget/tile registration.

## Safety note

This is a self-help tool, not medical care. It cannot replace a clinician for panic
disorder, anxiety, or anger issues. If symptoms are severe or persistent, seek professional
support.
