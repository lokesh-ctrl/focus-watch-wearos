# Implementation Plan — Phase 0 (Launch-Ready)

## Completed

### Step 1: Billing Infrastructure [DONE]
- [x] Added `BillingManager` class wrapping Google Play Billing Library v7.0.0
- [x] Added `IS_PRO` key to `FocusPreferences` DataStore
- [x] Added billing dependency to `build.gradle.kts` and `libs.versions.toml`
- [x] Added `com.android.vending.BILLING` permission to manifest

### Step 2: Feature Gating [DONE]
- [x] Themes (Ember, Ocean, Monochrome) gated — locked appearance + tap triggers purchase
- [x] Adaptive breaks gated — disabled toggle with [PRO] badge
- [x] Free history retention reduced from 30 → 7 days in `HistoryRepository.pruneOldData()`
- [x] Upgrade button shown to free users in Settings
- [ ] Gate complications: free users get Status + FocusDuration only (TODO)

### Step 3: Fix ANR Risk [DONE]
- [x] Replaced `runBlocking` in `SettingsActivity.onCreate` with `scope.launch` + `buildUi()`

### Step 4: Onboarding [DONE]
- [x] Created `OnboardingActivity` — 3 pages with dot indicator
- [x] Created `LauncherActivity` routing to onboarding vs settings
- [x] First-launch flag via `ONBOARDING_COMPLETE` in DataStore
- [x] Manifest updated: LauncherActivity is now the LAUNCHER entry point

### Step 5: Auto-DND [DONE]
- [x] `enableDndIfConfigured()` saves previous filter, sets INTERRUPTION_FILTER_NONE
- [x] `restoreDnd()` restores previous filter on stop/break-end/toggle
- [x] Gated behind Pro + DND preference + notification policy access
- [x] DND toggle added to SettingsActivity with [PRO] badge
- [x] Hooked into: startFocusInternal, stopSession, onBreakTimerExpired, toggleSession

## Remaining (Next Session)

### Step 6: Play Store Prep
- [ ] Privacy policy markdown (PRIVACY_POLICY.md + hosted URL)
- [ ] Play Store listing copy (short + full description)
- [ ] Screenshots from emulator (round 384x384)
- [ ] Feature graphic (1024x500)
- [ ] Data safety declaration notes

### Step 7: Complication Gating
- [ ] Gate BreakCountdown, SessionCount, DailyTotal, Interruption sources behind Pro
- [ ] Free users get: Status + FocusDuration only

### Step 8: Polish
- [ ] Add ProGuard rules for Room + Billing
- [ ] Add crash reporting setup (Firebase Crashlytics)
- [ ] Test SCHEDULE_EXACT_ALARM on API 34+ (user opt-in needed)
- [ ] Physical watch testing

## Build Status

Last compile: **SUCCESS** (compileDebugKotlin passed, 1 harmless warning)

## Files Modified This Session

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added billing v7.0.0 |
| `focus-dial/build.gradle.kts` | Added billing dependency |
| `data/BillingManager.kt` | **New** — Play Billing wrapper (singleton) |
| `data/FocusPreferences.kt` | Added IS_PRO, ONBOARDING_COMPLETE, DND keys + methods |
| `data/HistoryRepository.kt` | Free tier 7-day retention |
| `SettingsActivity.kt` | Removed runBlocking, added Pro gating UI, DND toggle, upgrade button |
| `OnboardingActivity.kt` | **New** — 3-page first-run flow |
| `LauncherActivity.kt` | **New** — routes onboarding vs settings |
| `FocusSessionManager.kt` | Added DND enable/restore methods, hooked into all transitions |
| `AndroidManifest.xml` | Added LauncherActivity, OnboardingActivity, BILLING permission |
| `ROADMAP.md` | **New** — full product roadmap |
| `IMPLEMENTATION_PLAN.md` | **New** — this file |
