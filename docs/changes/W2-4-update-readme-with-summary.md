# W2-4: Update README with summary

## Summary
Added a concise project summary to the README describing Focus Dial as a Wear OS focus timer and watch face.

## What changed
- Rewrote the introduction under `# Focus Dial` as a short project summary covering on-watch Pomodoro sessions, progress and streak tracking, and break management.
- Preserved the existing feature list and the rest of the README.

## Decisions
- Kept the summary to one sentence and placed it immediately under the project heading so readers can quickly understand the app.
- This is a documentation-only change. A focused README assertion validates the summary text; no application test source is appropriate.

## How each criterion is met
- **Update the README with a project summary:** `READMEHasFocusDialSummary` verifies the expected summary sentence appears exactly in `README.md`.

## How to verify
Run from the repository root:

```sh
summary='Focus Dial is a productivity-focused Wear OS watch face and focus timer. Start Pomodoro-style sessions from the watch dial, track progress and streaks, and manage breaks without opening an app or reaching for a phone.'
grep -Fqx "$summary" README.md
```

## Tests
- **Added:** No repository test source; the focused `READMEHasFocusDialSummary` shell assertion was used for this documentation-only change.
- `READMEHasFocusDialSummary` — passed.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — unit-test task completed successfully (`testDebugUnitTest` was `NO-SOURCE`), and `compileDebugKotlin` passed (up to date). The overall command failed at `lintDebug` due to two existing lint errors unrelated to this documentation change: `CalendarHelper.kt:61` uses `LocalTime.ofInstant` (requires API 31; minSdk is 30), and `AndroidManifest.xml:19` is missing the `com.google.android.wearable.standalone` metadata element.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-4-update-readme-with-summary.md` (this change note)

## Open questions
None.
