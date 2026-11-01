# W2-1: Add hi in README

## Summary
Kept `hi` as the first README line, added `hello` as the second line, and made `hello` bold per reviewer feedback.

## What changed
- Updated the opening of `README.md` to show `hi` followed by bold `hello`.
- Preserved the existing README title and content.

## Decisions
- Kept the greetings as separate lines before the original project heading; applied Markdown bold to `hello` only.
- This is a documentation-only change. The focused `READMEStartsWithHiAndBoldHello` shell assertion checks the rendered source markup, so no application test source or test framework changes are appropriate.

## How each criterion is met
- **Add hi in README at start:** `READMEStartsWithHiAndBoldHello` checks that the first line of `README.md` is exactly `hi`.
- **Reviewer request — add hello as well:** `READMEStartsWithHiAndBoldHello` checks that the second line is `**hello**`.
- **Reviewer request — make hello bolder:** the `**hello**` Markdown markup renders the greeting in bold.

## How to verify
Run from the repository root:

```sh
actual="$(head -n 2 README.md)"
test "$actual" = "$(printf 'hi\n**hello**')"
```

## Tests
- **Added:** No repository test source; `READMEStartsWithHiAndBoldHello` is the focused shell assertion documented above.
- `READMEStartsWithHiAndBoldHello` — passed; first two lines are `hi` and `**hello**`.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — test and Kotlin compile tasks passed (`testDebugUnitTest` was `NO-SOURCE`; compilation was up to date); the overall Gradle command failed at `lintDebug` because of existing `NewApi` errors in `CalendarHelper.kt` (`LocalTime.ofInstant` requires API 31 while minSdk is 30). No application code was changed for this documentation-only ticket.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-1-add-hi-in-readme.md` (this change note)

## Open questions
None.
