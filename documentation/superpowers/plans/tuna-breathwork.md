# Plan — 吐纳 Tuna breathwork app

Source: `docs/SPEC.md`. Stack: Kotlin 2.0.21, AGP 8.7.3, Gradle wrapper 8.11.1, JDK 17
(`/opt/homebrew/opt/openjdk@17`), compileSdk 35, minSdk 26, Jetpack Compose (BOM), Material3,
Glance (widget), DataStore, kotlinx.serialization. Package `com.tuna.breathwork`.
No network permission. Build via CLI, install to connected SM-S918U.

## Milestones & tasks

### M0 — Scaffold (buildable on device)
- [ ] 0.1 `settings.gradle.kts`, root `build.gradle.kts`, Gradle wrapper 8.11.1, `gradle.properties`
      (AndroidX, JVM args), `local.properties` → SDK at `/opt/homebrew/share/android-commandlinetools`,
      JDK 17 via `org.gradle.java.home`
- [ ] 0.2 `:app` module: manifest (no INTERNET permission), theme, launcher icon (vector),
      MainActivity showing placeholder Compose screen
- [ ] 0.3 `./gradlew :app:assembleDebug` + `adb install` on device — verify launch
- [ ] 0.4 CI-less sanity: clean build from scratch works

### M1 — Design system & app skeleton
- [ ] 1.1 Theme: dark muted palette (deep charcoal/indigo `#10131A`-family), teal accent, rounded
      sans typography, `≥56dp` touch targets, `reduceMotion` flag read from settings
- [ ] 1.2 Navigation scaffold (navigation-compose): Home · Library · Technique detail · Session ·
      Settings · History
- [ ] 1.3 Settings store (DataStore): voice pitch/rate, haptics on/off, posture prompts on/off,
      reduce-motion, calm-now haptics off
- [ ] 1.4 Home screen: two big cards — Calm Now (hero) + Practice Library

### M2 — Session engine + techniques + voice + haptics + glyph
- [ ] 2.1 Domain model: `Phase` (INHALE/HOLD/EXHALE/SOUND_EXHALE + durationMs + voice phrase +
      optional posture cue), `TechniqueConfig` (id, name, zhName, description, phases list,
      cycles, soundMode theta/alpha, color, use-case tag)
- [ ] 2.2 `TechniquesRepository`: the 5 techniques as data (sigh, box 4-4-4-4, 4-7-8, coherent,
      Liu Zi Jue with xu/he/hu/si/chui/xi sound-exhales + Dantian belly cues)
- [ ] 2.3 `SessionEngine` (coroutine state machine): phase timing, cycle counting, completion/
      abandon events, drives listeners (voice, haptics, glyph, beats)
- [ ] 2.4 `VoiceProvider` interface + `TtsVoiceProvider` (Android TTS, slow rate ~0.75, pitch
      ~0.85, calm phrasing, posture prompt every 4th breath w/ varied templates, headphone
      reminder at session start)
- [ ] 2.5 `Haptics` (Vibrator): short pulse at inhale start, long soft pulse at exhale start;
      off in Calm Now by default
- [ ] 2.6 `BreathingGlyph`: Compose Canvas vector shape (circle/blob morph) driven by phase
      progress with eased scale/alpha; respects reduce-motion (gentle static pulsing)
- [ ] 2.7 Session screen: glyph + phase word + progress arc + quiet stop button; keep screen on

### M3 — Sound engine (binaural beats)
- [ ] 3.1 `BinauralEngine`: procedural stereo PCM via `AudioTrack` — theta (e.g. 200+206 Hz) and
      alpha (e.g. 200+210 Hz) carriers; low-latency chunked generation on background thread;
      volume fades in/out; independent of voice volume
- [ ] 3.2 Headphone detection (`AudioManager` wired/Bluetooth A2DP); state exposed to UI
- [ ] 3.3 Mono fallback: single carrier amplitude-modulated at beat frequency (pulsing tone) when
      no headphones; UI/voice notes which mode is active
- [ ] 3.4 Voice reminder "put on your headphones" at session start when beats selected & no
      headphones; honest status line in session screen
- [ ] 3.5 Audio focus handling (AUDIOFOCUS_GAIN, duck others, pause on focus loss); TTS stream
      mixing so voice sits on top of beats

### M4 — Calm Now (emergency path)
- [ ] 4.1 `CalmNowActivity` (dedicated task, no back stack) → straight into 2-min physiological
      sigh session, theta beats, minimal words, no posture nagging
- [ ] 4.2 Glance widget: "吐纳 Calm Now" button → intent to CalmNowActivity
- [ ] 4.3 Quick-settings tile: same intent
- [ ] 4.4 Edge cases: launching while a session runs (handoff/ignore), screen-off → session
      continues audio-only, tapping Calm Now twice

### M5 — History + mood
- [ ] 5.1 `SessionLogStore` (DataStore, local JSON via kotlinx.serialization): technique, duration,
      completed, timestamp, mood tag
- [ ] 5.2 Post-session sheet: one-tap mood (calm / neutral / still-stressed), skippable
- [ ] 5.3 History screen: list + tiny mood trend per technique ("does 4-7-8 help me?")

### M6 — Polish & QA
- [ ] 6.1 Edge cases: TTS unavailable/voice data missing, audio focus interruption, widget
      tap while app running, orientation, screen timeout
- [ ] 6.2 Accessibility: content descriptions, touch targets, reduce-motion
- [ ] 6.3 Manual test matrix on device across all 5 techniques × modes; timing verification
      (phases within ±150 ms); headphone on/off paths
- [ ] 6.4 Final build, install, walkthrough with user; CHANGELOG entry

## Definition of done
All acceptance criteria in SPEC.md pass; app installed and demoed on the connected device.
