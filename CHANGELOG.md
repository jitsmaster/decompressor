# Changelog

## 0.5.0 — 2026-08-09

- Bilingual guidance: voice language switcher (English / 中文) in Settings with an
  in-app "Test voice" sampler; ZH clips (Xiaoxiao, warm female — matching the UCLA
  呼吸冥想 ambient voice) for every phrase; on-screen phase words follow the language.
- Chinese voice pitch lowered 10% (build-time asetrate/aresample on the real file
  sample rate — duration preserved, manifest refreshed).
- UI v2 (researched against Calm/Headspace/breathwork patterns):
  - Night-sky vertical gradient backgrounds on every screen
  - Calm Now hero: lavender gradient panel + taiji watermark + soft type
  - Elevated rounded cards (SoftCard: hairline edge + soft shadow)
  - Technique cards: accent edge bars + use-case chips
  - Home pills for History/Settings; breathing glyph gains a radial glow + outer ring
  - Airy section labels, letter-spaced titles, consistent spacing

## 0.4.0 — 2026-08-09

- Voice lead-in model: each breath phase starts EXACTLY when its recorded phrase ends
  (clip durations baked into the manifest at build time; the engine delays the breath
  by the clip length). Heads-up: a light haptic tap + "…listen" UI when the voice
  starts speaking; phase timings account for the lead-ins in preset math and logs.
- Holds are fully silent: no haptic pattern, no pre-tick (vibration only on
  inhale/exhale phases).
- Haptic language: inhale = rapid high-frequency buzz (~12.5 Hz), exhale = slow
  low-frequency rumble (~1.7 Hz), all gentler amplitudes.
- Launcher icon: black & white taiji (yin-yang) with a thin white ring border on the
  dark navy background.
- History screen: whole screen is now a single LazyColumn — the session list scrolls
  properly instead of overflowing below the Insights section.
- Fix: a completed-but-untagged session no longer blocks the next Calm Now start
  (VM guard + service teardown on new-session intents).
- 39 unit tests green (added voice-lead timing + duration-with-leads coverage).

## 0.3.0 — 2026-08-09

- Speech pace: all phrase clips regenerated at 0.5625× (edge -25% + build-time
  atempo 0.75 — slowness baked into the mp3 files, playback untouched). Phase
  timings redesigned to fit: sigh 4.6/4.6/7 s (Calm Now ≈ 1:53), box 5-5-5-5,
  4-7-8 5/7/8, coherent 5/7, Liu Zi Jue 5/9 s.
- Recurring posture cues removed (no phase can host them at this pace); posture
  coaching lives in the session intro; "Posture reminders" toggle retired.
- Sessions run in a foreground SessionService (survives screen-off and activity
  teardown) with a live notification; completed sessions are logged immediately
  (durable) and the mood tag patches the record in place.
- Fix: session total-cycles now published by the service (was 0/0).
- 37 unit tests green.

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
