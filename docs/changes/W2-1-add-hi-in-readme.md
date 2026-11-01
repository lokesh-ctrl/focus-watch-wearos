# W2-1: Add hi in README

## Summary
Added the requested greeting at the beginning of the project README.

## What changed
- Added `hi` as the first line of `README.md`, followed by a blank line before the existing title.

## Decisions
- Kept the existing README content and heading intact; only inserted the greeting.
- This is a documentation-only change, and the project has no existing unit tests. A focused shell assertion verifies the acceptance criterion instead of adding a new test framework or test source set.

## How each criterion is met
- **Add hi in README at start:** `READMEStartsWithHi` checks that the first line of `README.md` is exactly `hi`.

## How to verify
Run from the repository root:

```sh
test "$(head -n 1 README.md)" = hi
```

## Tests
- **Added:** No repository test was added; the repository has no existing unit test sources. The verification check is `READMEStartsWithHi` (the shell assertion above).
- `test "$(head -n 1 README.md)" = hi` — passed (`README starts with hi`).
- `./gradlew test` — passed. Gradle reported unit-test tasks as `NO-SOURCE`.
- Initial concurrent Gradle commands timed out while Gradle was downloading/initializing; rerunning the test task after initialization passed.
- `./gradlew :focus-dial:compileDebugKotlin` — passed (Kotlin compilation/typecheck).
- `./gradlew lint` — failed due to existing `NewApi` lint errors in `focus-dial/src/main/kotlin/com/focusdial/app/calendar/CalendarHelper.kt` (including `LocalTime.ofInstant` requiring API 31 while minSdk is 30). No source-code changes were made because these findings are unrelated to this documentation ticket.

## Docs updated
- `README.md`
- `docs/changes/W2-1-add-hi-in-readme.md` (this change note)

## Open questions
None.
