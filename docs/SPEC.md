# 吐纳 Tuna — Guided Breathwork App · SPEC

> Personal Android app for reducing stress via voice-guided breathing exercises + binaural beats.
> Status: approved via interview (Grill Me). This is the source of truth for the build.

## Problem Statement

During panic attacks, obsessive breath-holding, or rising anger, the user needs an immediate,
zero-thinking intervention that slows heart rate and stabilizes the mind. When stressed out in
general, they need a daily practice. Existing breathing apps require navigation, choices, and
accounts at exactly the moment the brain cannot handle them.

## Solution

An offline-first Android app with two modes:

1. **Calm Now** — one-tap emergency mode (home-screen widget, quick-settings tile). Fixed 2-minute
   guided session: voice + theta binaural beats + breathing glyph. No menus, no choices.
2. **Practice library** — everyday mode: 5 techniques, 5/10/15-minute presets, theta or alpha
   beats, full settings.

Voice guidance (TTS) reminds the user to put on headphones for the binaural effect, and the app
detects headphones, falling back to a mono pulsing tone when none are present.

## User Stories

1. As a user mid-panic-attack, I want to reach a calm-down session in under 3 seconds from the
   home screen, so that I can intervene before the panic escalates.
2. As a user mid-panic-attack, I want the session to start with zero choices, so that my panicked
   brain doesn't have to navigate anything.
3. As a user mid-panic-attack, I want the voice to guide my breathing slowly and simply, so that I
   can follow without thinking.
4. As a user who holds my breath when obsessing, I want long-exhale techniques available in Calm
   Now, so that I can break the hold reflex.
5. As an angry user about to say something regrettable, I want a breathing session that keeps me
   calm-but-alert, so that I de-escalate without losing awareness.
6. As a stressed user, I want daily practice sessions with selectable techniques and durations, so
   that I can lower my baseline stress.
7. As a user, I want the app to remind me to put on headphones for binaural beats, so that I get
   the intended brainwave effect.
8. As a user without headphones, I want the session to still work via a pulsing tone + voice +
   visual glyph, so that I can use the app anywhere.
9. As a user, I want the breathing animation to be vector-art based and soothing, so that the
   visual channel guides my breath without stimulation.
10. As a user, I want gentle posture reminders during sessions, so that my breathing is
    mechanically effective (slouching restricts the diaphragm).
11. As a user, I want my session history and mood tags stored only on my device, so that my health
    data stays private.
12. As a user, I want zero gamification (no streaks, no notifications, no nagging), so that the
    app never adds stress.
13. As a user, I want to see whether each technique actually helps me (mood trend over sessions),
    so that I can pick what works.
14. As a user with light sensitivity, I want a dark, muted, low-stimulation UI with a
    reduce-motion option.
15. As a user, I want the app fully offline with no account, so that it works anywhere without
    connectivity or signup.
16. As a user practicing Liu Zi Jue, I want the six healing sounds guided by voice, so that I get
    the Chinese sound-based technique.
17. As a user, I want haptic pulses synced to the breath (in practice mode), so that I can follow
    the rhythm with my eyes closed.
18. As a user who records nothing, I want the voice layer to work out of the box (TTS), so that
    the app is usable immediately.

## Non-Goals (v1)

- No live heart-rate display during sessions (documented anxiety risk for panic-prone users).
- No camera HRV (deferred to v2, post-session only).
- No accounts, cloud, or backend of any kind.
- No gamification, streaks, or push notifications.
- No Play Store submission (v1 is a locally-built sideload APK).
- No pre-recorded voice assets (VoiceProvider interface allows later drop-in).
- No brown noise / white noise (user decision: binaural beats only).

## Key Decisions (from interview)

| # | Decision |
|---|---|
| D1 | Personal app, offline-only, no accounts; modular code for possible future sharing |
| D2 | Two modes: Calm Now (widget/tile, fixed 2 min) + practice library (5/10/15 presets) |
| D3 | 5 data-driven techniques: physiological sigh, box 4-4-4-4, 4-7-8, coherent, Liu Zi Jue |
| D4 | TTS voice v1 (slow, low pitch) behind VoiceProvider interface |
| D5 | Binaural beats only: theta (4–8 Hz) → Calm Now, alpha (8–12 Hz) → anger path; procedural generation; headphone detection + voice reminder; mono pulsing-tone fallback |
| D6 | Native Kotlin + Jetpack Compose; vector-art breathing glyph; Glance widget; AudioTrack PCM; min SDK 26, target/compile SDK 35 |
| D7 | No sensors v1; post-session camera HRV deferred to v2; never live HR during session |
| D8 | Local session log + optional mood tag; zero gamification |
| D9 | Posture prompts: session start + every 4th breath, varied phrasing; minimal in Calm Now; toggle |
| D10 | Dark muted UI, teal accent, large targets (≥56dp), reduce-motion toggle |
| D11 | Local build + sideload APK |
| D12 | Name: 吐纳 Tuna (always displayed with characters) |

## Technique Set (D3 detail)

| Technique | Phases | Use case | Sound mode |
|---|---|---|---|
| Physiological Sigh | inhale-inhale-exhale (double inhale, long exhale) | panic / acute | theta |
| Box 4-4-4-4 | inhale 4 / hold 4 / exhale 4 / hold 4 | anger / focus | alpha |
| 4-7-8 | inhale 4 / hold 7 / exhale 8 | stress / sleep | theta |
| Coherent | inhale ~5.5s / exhale ~5.5s (~5.5 breaths/min) | baseline practice | theta (user may choose alpha) |
| Liu Zi Jue 六字诀 | six sound-exhales: xu, he, hu, si, chui, xi | sound-based release | theta; sounds lead the exhale |

Dantian belly-breathing coaching embedded in voice scripts of every technique.

## Acceptance Criteria

- [ ] APK builds from CLI (`./gradlew assembleDebug`) with JDK 17 + local SDK, installs on the
      connected device.
- [ ] Calm Now reachable in ≤3 taps from lock/home via widget and quick-settings tile.
- [ ] Session engine runs all 5 techniques with correct phase durations; voice, glyph, haptics,
      and beats stay in sync; cycle counts and session end honored.
- [ ] Voice reminds to put on headphones when a binaural session starts without them.
- [ ] Binaural generation verified (stereo L/R carrier tones at theta/alpha difference); mono
      fallback works on speaker.
- [ ] Session log records technique/duration/completed/mood; mood trend visible; data local only.
- [ ] Posture prompt at start + every ~4th breath with varied phrasing; toggle honored.
- [ ] Reduce-motion toggle honored; dark theme; large touch targets.
- [ ] No network permission in manifest; no accounts.
