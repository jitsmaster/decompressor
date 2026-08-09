# Changelog

## 0.2.0 — 2026-08-09

Voice, rhythm, and ambient upgrades.

### Recorded voice (no runtime TTS)
- Phrase library: 20 bundled clips generated from Microsoft Edge neural voices —
  `en-US-JennyNeural` (calm female, matching the UCLA guided-track voice) for all
  English phrases, `zh-CN-XiaoxiaoNeural` (Mandarin female) for the six Liu Zi Jue
  sounds. `RecordedVoiceProvider` plays clips in a strict sequential queue behind the
  `VoiceProvider` seam; TTS remains only as a fallback for unmapped phrases.
- Fixed a prepare/start race where the headphone reminder could overlap the session
  intro (generation token invalidates in-flight playback on `stop()`).
- Headphone reminder now plays to completion (`speakAndAwait`) before the session
  starts; beats begin after it.

### Rhythm guidance (eyes-open and eyes-closed)
- Depleting countdown ring around the breathing glyph + big seconds countdown per
  phase (driven by phase-start wall clock).
- Soft tick sound in the final second of each phase (bundled `sfx/tick.mp3`, toggle
  in Settings → Countdown ticks).
- Engine pre-tick: haptics cue ~1 s before every phase end (short phases skip it).
- Haptic language upgraded: inhale = double-pulse, exhale = gentle continuous buzz
  for the phase duration, hold = silence; Calm Now haptics now default ON.
- Posture cues moved to the cycle's longest phase (prefer exhale) so queued clips
  never get cut by the phase-boundary flush; cues shortened to fit 4s box phases.
- Technique timing adjusted for recorded clips: sigh inhales 2.2 s, Liu Zi Jue
  inhale 4 s; Calm Now preset is always ~2 min for any technique (fixed 55-cycle bug
  when switching the Calm Now exercise).

### Ambient (recorded sessions from the web)
- Two UCLA Mindful tracks bundled (Breathing Meditation EN + 呼吸冥想 ZH,
  CC BY-NC-ND 4.0, personal use) with a dedicated Ambient screen.
- Speech/silence envelopes generated at build time (16 kHz energy, 100 ms frames);
  while a track plays, the breathing glyph follows the actual voice — expand on
  pauses (inhale), ease out on speech (exhale).
- Switched the exercise in this phase to coherent breathing during testing —
  confirms the Calm Now technique switch works end to end.

### Misc
- Palette: replaced teal/green accents with a soothing lavender family (no green/cyan).
- 43 unit tests green (added pre-tick, phase-ending, haptics patterns, envelope,
  duration-fit, and reminder sequencing coverage).

## 0.1.0 — 2026-08-09

Initial build (SPARC: Spec → Plan → Architecture → Review → Code, TDD).

- Scaffold: Kotlin 2.0.21, AGP 8.7.3, Gradle wrapper 8.11.1, Compose (BOM 2024.10), min SDK 26,
  compile/target SDK 35, dark muted theme (deep charcoal/indigo + teal), vector launcher glyph.
- Domain: `Phase`, `TechniqueConfig` (data-driven, validated), `BinauralSpec` (theta 200/206 Hz,
  alpha 200/210 Hz), `PostureCueScheduler` (every 4th cycle, varied phrasing, Calm Now
  suppression), preset duration math (`cyclesForDuration`).
- Session engine: timer-driven coroutine state machine; phase-accurate (virtual-time tested);
  TTS queue flush per phase boundary; per-cycle sound rotation (Liu Zi Jue); abort semantics;
  haptics scheduling.
- Voice: Android TTS behind `VoiceProvider` — slow rate 0.75, pitch 0.85, USAGE_MEDIA mixing;
  suspend readiness gate (fixes main-thread deadlock) + 5 s visual-only fallback.
- Sound: procedural stereo binaural beats via `AudioTrack` (200 ms PCM chunks, ring buffered,
  fade-in, audio-focus aware); mono amplitude-modulated fallback; synchronous headphone
  detection (wired/USB/BT-A2DP, no permissions).
- UX: Home (Calm Now hero + library), Library (5 techniques + use-case tags), technique detail
  (alpha/theta label, 5/10/15 presets), full-screen session (breathing glyph + phase word +
  cycle progress + headphone status), Settings (haptics ×2, posture, reduce-motion), History
  (session list + mood trend per technique), completion mood panel (😌/😐/😣, skippable).
- Calm Now: dedicated single-task activity (no back stack), Glance widget + quick-settings
  tile → one tap to a 2-minute physiological sigh with theta beats.
- Persistence: DataStore settings + session log (kotlinx.serialization JSON), local-only.
- Fixed during device testing: TTS init main-thread deadlock (ANR), missing VIBRATE
  permission (practice haptics crash), BLUETOOTH_CONNECT crash (async profile callback),
  nav sessions not auto-starting.

### Verified on device (Galaxy S23 Ultra, Android 16)

Calm Now 0→12 cycles → completion → mood tag → log persisted; box 5-min session (19 cycles)
with haptics; abort → logged as ended; history trend; widget + tile registration.
