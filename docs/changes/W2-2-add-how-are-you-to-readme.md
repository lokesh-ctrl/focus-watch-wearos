# W2-2: Add how are you to README

## Summary
Added `how are you` to the opening greeting in the README and made it bold per reviewer feedback.

## What changed
- Added `**how are you**` as the third line of `README.md`, after `hi` and `**hello**`.
- Left the project heading and remaining documentation unchanged.

## Decisions
- Kept the greeting on its own line to follow the existing opening format and used Markdown bold to satisfy the reviewer request.
- This is a documentation-only change. The focused `READMEHasBoldHowAreYou` shell assertion validates the README markup, so no application test source is appropriate.

## How each criterion is met
- **Add how are you to README:** `READMEHasBoldHowAreYou` verifies the exact third line of `README.md` is `**how are you**`, which renders in bold.

## How to verify
Run from the repository root:

```sh
test "$(sed -n '3p' README.md)" = '**how are you**'
```

## Tests
- **Added:** No repository test source; the focused `READMEHasBoldHowAreYou` shell assertion is documented above. A source-code test is not appropriate for this documentation-only change.
- `READMEHasBoldHowAreYou` — passed; the third line is exactly `**how are you**`.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — unit-test task passed (`testDebugUnitTest` was `NO-SOURCE`) and Kotlin compilation/typecheck passed (`compileDebugKotlin` was up to date). The overall command failed at `lintDebug` on two existing issues: `CalendarHelper.kt:61` calls `LocalTime.ofInstant` (API 31, minSdk 30), and `AndroidManifest.xml:19` is missing `com.google.android.wearable.standalone` metadata. No application code was changed.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-2-add-how-are-you-to-readme.md` (this change note)

## Open questions
None.
