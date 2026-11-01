# W2-1: Add hi in README

## Summary
Added the requested `hi` greeting at the beginning of the project README and incorporated the review request to add `hello` as well.

## What changed
- Added `hello` immediately after the existing `hi` at the beginning of `README.md`.
- Preserved the existing README title and content.

## Decisions
- Kept both greetings as plain-text opening lines before the original project heading.
- This is a documentation-only change. The focused `READMEStartsWithHiAndHello` shell assertion verifies the opening lines; no application test source or test framework changes are appropriate.

## How each criterion is met
- **Add hi in README at start:** `READMEStartsWithHiAndHello` checks that the first line of `README.md` is exactly `hi`.
- **Reviewer request — add hello as well:** `READMEStartsWithHiAndHello` also checks that the second line is exactly `hello`.

## How to verify
Run from the repository root:

```sh
actual="$(head -n 2 README.md)"
test "$actual" = "$(printf 'hi\nhello')"
```

## Tests
- **Added:** No repository test source; the focused verification is `READMEStartsWithHiAndHello` (the shell assertion above).
- `READMEStartsWithHiAndHello` — passed; the first two README lines are `hi` and `hello`.
- `./gradlew test :focus-dial:compileDebugKotlin lint` — test and Kotlin compile tasks passed (`testDebugUnitTest` was `NO-SOURCE`, and compile was up to date); the overall Gradle command failed at lint due to 2 existing `NewApi` errors in `CalendarHelper.kt` (including `LocalTime.ofInstant`, which requires API 31 while minSdk is 30). No application code was changed for this documentation-only ticket.
- `git diff --check` — passed.

## Docs updated
- `README.md`
- `docs/changes/W2-1-add-hi-in-readme.md` (this change note)

## Open questions
None.
