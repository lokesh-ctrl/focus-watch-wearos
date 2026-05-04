# Implementation Plan — Phase 1 Completion + Phase 2 (Watch-Side)

## Status: ALL STEPS COMPLETE

## Overview

Complete the remaining Phase 1 feature (Weekly Insights) and implement
watch-side Phase 2 features that don't require a companion phone app:
Wear OS Tiles, Health Connect integration, and data export via share intent.

---

## Step 1: Weekly Insights Activity [Pro] ✓

**Goal**: A scrollable canvas-based stats screen showing 7-day trends.

**Data Source**: `DaySummaryDao.getRecentSummaries(7)` already returns the data.

**Implementation**:
1. Add `DaySummaryDao.getRecentSummaries(dateAfter: String)` query for flexible lookups
2. Create `InsightsActivity` (canvas-based, not Compose — matches existing pattern)
   - Bar chart: daily focus minutes for past 7 days (Mon-Sun)
   - Best day highlight (highest totalFocusMillis)
   - Weekly total, average score, sessions completed
   - Streak display
   - Trend arrow (up/down/flat vs previous week)
3. Add navigation from SettingsActivity ("Weekly Insights" button, Pro-gated)
4. Register in AndroidManifest

**Files**:
- `InsightsActivity.kt` — new
- `DaySummaryDao.kt` — add query
- `HistoryRepository.kt` — add `getWeeklySummaries()` and `getPreviousWeekTotalMillis()`
- `SettingsActivity.kt` — add navigation button
- `AndroidManifest.xml` — register activity

---

## Step 2: Wear OS Tile ✓

**Goal**: A Tile that shows current focus status + daily progress at a glance
without entering the watch face. Users can tap to start/stop.

**Implementation**:
1. Add Tiles dependency (`androidx.wear.tiles:tiles:1.4.0`, `tiles-material:1.4.0`)
2. Create `FocusTileService` extending `TileService`
   - Layout: status text (IDLE/FOCUS/BREAK), progress arc, daily total, tap action
   - Refreshes on timeline request
   - Tap launches `ToggleSessionActivity`
3. Register service in manifest with BIND_TILE_PROVIDER permission
4. Add tile preview resource

**Files**:
- `FocusTileService.kt` — new
- `gradle/libs.versions.toml` — add tiles dependency
- `focus-dial/build.gradle.kts` — add tiles dependency
- `AndroidManifest.xml` — register tile service
- `res/drawable/tile_preview.xml` — placeholder

---

## Step 3: Health Connect Integration [Pro] ✓

**Goal**: Log completed focus sessions as "Mindfulness" sessions in Health Connect,
allowing users to see focus time alongside other health data.

**Implementation**:
1. Add Health Connect dependency (`androidx.health.connect:connect-client:1.1.0-alpha07`)
2. Create `HealthConnectManager` utility class
   - `isAvailable()`: checks if Health Connect is installed
   - `requestPermission()`: launches permission request
   - `logMindfulnessSession(startMillis, endMillis)`: writes MindfulnessSessionRecord
3. Hook into `FocusSessionManager.onFocusTimerExpired()` to auto-log completed sessions
4. Add Health Connect toggle in SettingsActivity (Pro-gated)
5. Add permission declaration to manifest

**Files**:
- `data/HealthConnectManager.kt` — new
- `FocusSessionManager.kt` — call health connect on session complete
- `FocusPreferences.kt` — add HEALTH_CONNECT_ENABLED key
- `SettingsActivity.kt` — add toggle
- `gradle/libs.versions.toml` — add health connect dep
- `focus-dial/build.gradle.kts` — add health connect dep
- `AndroidManifest.xml` — permission + intent filter

---

## Step 4: Data Export (Share Intent) ✓

**Goal**: Let users export their session history as JSON via share intent
(e.g., to Bluetooth, email, or cloud storage). No phone app required.

**Implementation**:
1. Add `SessionDao.getAllSessions()` query
2. Create export method in `HistoryRepository` that serializes sessions to JSON
3. Add "Export Data" button in SettingsActivity (Pro-gated)
4. Use `FileProvider` + share intent to send the JSON file

**Files**:
- `SessionDao.kt` — add query
- `HistoryRepository.kt` — add `exportToJson()` method
- `SettingsActivity.kt` — add export button
- `res/xml/file_paths.xml` — new (FileProvider paths)
- `AndroidManifest.xml` — register FileProvider

---

## Step 5: SCHEDULE_EXACT_ALARM API 34+ Handling ✓

**Goal**: On API 34+, `SCHEDULE_EXACT_ALARM` requires user opt-in. Add graceful
handling so the app doesn't crash and guides users to the settings.

**Implementation**:
1. Check `AlarmManager.canScheduleExactAlarms()` before scheduling
2. If not granted, show a settings prompt or fall back to `setAndAllowWhileIdle()`
3. Add `USE_EXACT_ALARM` permission as an alternative for manifest-declared usage

**Files**:
- `FocusSessionManager.kt` — add alarm permission check
- `AndroidManifest.xml` — add USE_EXACT_ALARM permission

---

## Build & Commit Strategy

Each step is an independent commit:
1. `feat: add weekly insights activity with bar chart and trends`
2. `feat: add Wear OS Tile for quick focus status and tap-to-start`
3. `feat: integrate Health Connect for mindfulness session logging`
4. `feat: add JSON data export via share intent`
5. `fix: handle SCHEDULE_EXACT_ALARM permission on API 34+`

Verify build passes (`compileDebugKotlin`) after each step before committing.
Update `ROADMAP.md` after each successful commit.
