# Everest Focus

A productivity-focused watch face for Wear OS that combines a Pomodoro timer with an always-on display. Tap the dial to start a focus session — no app to open, no phone required.

## Features

- **Watch face timer** — Focus state, progress arc, and stats rendered directly on the dial
- **Customizable sessions** — Focus (15–120 min), break (3–30 min), daily goal (1–8 sessions)
- **Adaptive breaks** — Automatically extends rest after consecutive sessions or late-night use
- **Calendar awareness** — Shows upcoming meetings, suggests shorter focus when one is near
- **Focus score** — Rates each session (0–100) based on completion and interruptions
- **Streak tracking** — Build daily consistency with streak counter
- **Auto Do Not Disturb** — Silences notifications during focus (Pro)
- **4 themes** — Minimal, Ember, Ocean, Monochrome
- **6 complications** — Focus Duration, Break Countdown, Session Count, Daily Total, Interruptions, Status
- **Haptic feedback** — Distinct vibration patterns for start, break, and end
- **Ambient mode** — Low-power display with time + focus state

## Architecture

```
everest-focus/src/main/kotlin/com/everest/focus/
├── FocusWatchFaceService.kt    # Canvas-rendered watch face
├── FocusSessionManager.kt      # Timer state machine (singleton)
├── FocusService.kt             # Foreground service + notifications
├── SettingsActivity.kt         # Configuration UI
├── OnboardingActivity.kt       # First-run flow
├── calendar/                   # Calendar integration
├── complication/               # 6 complication data sources
├── data/
│   ├── BillingManager.kt       # Google Play Billing (Pro unlock)
│   ├── FocusPreferences.kt     # DataStore preferences
│   ├── HistoryRepository.kt    # Session history + stats
│   ├── AdaptiveBreakCalculator.kt
│   └── db/                     # Room database (sessions, day summaries)
└── theme/                      # Theme definitions
```

## Build

```bash
./gradlew :everest-focus:assembleDebug
```

Requires Android SDK with Wear OS system images. Min SDK 30, target SDK 34.

## Monetization

Freemium model with a one-time Pro unlock ($4.99):

| Free | Pro |
|------|-----|
| 1 theme | All themes |
| 7-day history | 30-day history |
| Basic timer | Adaptive breaks |
| — | Auto-DND |
| 2 complications | All 6 |

## Docs

- [Product Roadmap](docs/ROADMAP.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Play Store Listing](docs/PLAY_STORE_LISTING.md)
- [Privacy Policy](docs/PRIVACY_POLICY.md)

## License

Proprietary. All rights reserved.
