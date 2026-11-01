# W2-6: Update README

## Summary
Refined the README introduction into a concise summary of Focus Dial's low-friction Wear OS Pomodoro experience.

## What changed
- Updated the project summary immediately below the `Focus Dial` heading to describe the watch dial as a Pomodoro workspace and highlight one-tap sessions, at-a-glance progress and streaks, and break management without opening an app.
- Preserved the existing README structure and feature documentation.

## Decisions
- Built on the existing Focus Dial summary instead of replacing the README or introducing unrelated product details.
- This is a documentation-only change. A focused README assertion verifies the summary text; an application test source is not appropriate for this change.

## How each criterion is met
- **README updated with a summary:** `READMEHasFocusDialSummary` checks that the complete updated summary paragraph appears as a line in `README.md`.

## How to verify
Run from the repository root:

```sh
summary='Focus Dial is a productivity-focused Wear OS watch face and focus timer that turns the watch dial into a low-friction Pomodoro workspace. Start a focus session with a tap, see progress and streaks at a glance, and manage breaks without opening an app or reaching for your phone.'
grep -Fqx "$summary" README.md
```

## Tests
- **Added:** No repository test source; `READMEHasFocusDialSummary` is the focused shell assertion documented above.
- `READMEHasFocusDialSummary` — passed.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — the unit-test task (`testDebugUnitTest`) and Kotlin compilation (`compileDebugKotlin`) completed successfully (both were up to date). The overall command failed at `lintDebug` due to existing lint errors in application code; the report includes a `NewApi` error at `CalendarHelper.kt:61` because `LocalTime.ofInstant` requires API 31 while the app's minimum SDK is 30. No application code was changed.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-6-update-readme.md` (this change note)

## Open questions
None.
