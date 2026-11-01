# W2-2: Add how are you to README

## Summary
Added `how are you` to the opening greeting in the README.

## What changed
- Added `how are you` as the third line of `README.md`, after `hi` and `**hello**`.
- Left the project heading and remaining documentation unchanged.

## Decisions
- Kept the greeting on its own line to follow the existing opening format; used plain text markup.
- This is a documentation-only change. The focused `READMEHasHowAreYou` shell assertion validates the README text, so no application test source is appropriate.

## How each criterion is met
- **Add how are you to README:** `READMEHasHowAreYou` verifies the exact third line of `README.md` is `how are you`.

## How to verify
Run from the repository root:

```sh
test "$(sed -n '3p' README.md)" = 'how are you'
```

## Tests
- **Added:** No repository test source; `READMEHasHowAreYou` is the focused shell assertion documented above.
- `READMEHasHowAreYou` — passed; the third line is exactly `how are you`.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — unit-test task passed (`testDebugUnitTest` was `NO-SOURCE`), Kotlin compilation passed (`compileDebugKotlin` was up to date), but the command failed at `lintDebug` because of existing `NewApi` errors in `CalendarHelper.kt` (`LocalTime.ofInstant` requires API 31 while minSdk is 30). No application code was changed for this documentation-only ticket.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-2-add-how-are-you-to-readme.md` (this change note)

## Open questions
None.
