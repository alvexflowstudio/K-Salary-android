# Empty calendar days — 2026-11-01

Branch: `codex/calendar-editor-payroll`. Follow-up to the calendar/editor changes, preserving the existing uncommitted work.

## Behavior and scope

Both border modifiers now depend on the existing `hasShift` predicate: positive hours in any existing work/premium column, a manually enabled paid-day flag, or a nonblank note. No weekday makes a cell populated. A null record or blank stored row with no meaningful entries is visually empty. Paid rest/holiday records remain populated even when all work hours are zero.

Empty cells show only their date number, with no outer border or inner rectangular outline and no automatically generated badge. Weekday text retains the theme's normal color; empty Saturdays use the established blue `LightAppPalette.accent` in both themes, as explicitly requested; Sundays retain `palette.danger`. Populated Saturday colors retain their previous theme role.

Populated cells retain the previous outer border width/color rules, inner date outline, backgrounds, dimensions, aspect ratio, padding, font sizes and weights, click handling, paid-status text colors and actual-work badge priority. Filled Sundays and manually marked holidays remain red. No badge/hour calculation, payroll logic, entry behavior, Room model, migration, preferences, translation or data format was changed. A pre/post SHA-256 comparison confirmed all other application source files remained byte-identical.

## Verification

- **46 JVM tests passed**.
- **10 Android instrumentation tests passed** on the installed Pixel_8 Android 37.1 emulator at **320×568 dp**, font scale 1.0.
- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`: successful offline builds using the existing dependencies.
- `git diff --check`: passed.

The new regression initially failed on the visible border of empty October 7. [Before-fix log](verification/calendar-empty-days-20261101/regression-before-ui.log).

The final empty-day regression checks both palettes for:

| October 2026 fixture | Expected appearance |
| --- | --- |
| 7, empty Wednesday | Normal date text only; no frames/badge |
| 17, empty Saturday | Blue date text only; no frames/badge |
| 18, empty Sunday | Red date text only; no frames/badge |
| 19, blank stored row | Normal date text only; no frames/badge |
| 6, 8 base hours | Existing filled borders and `8 ч` badge |
| 11, weekly paid rest with zero work | Red borders and red rest status text |
| 9, manually paid holiday with zero work | Red borders and red holiday status text |

Assertions include straight-edge pixels matching the background on empty cells, border pixels on populated cells, date colors, font weights, absence of empty-day badges, equal cell dimensions and the unchanged 0.85 aspect ratio. The existing mixed-hours, paid-day text, October dates/all-12-language, Room/backup and keyboard regressions remain included. Earlier assertions expecting outlines on empty cells were updated for the new requirement.

Visual QA also identified the dark palette's beige accent on empty Saturdays; the empty-only blue override was added without altering filled-day colors. An older pixel fixture rendered under system bars and failed intermittently; its scene now uses the actual app theme and status-bar padding. Application code was not changed for that harness fix. The final complete run passed. [Intermediate harness failure](verification/calendar-empty-days-20261101/ui-before-fixture-fix.log), [final Android log](verification/calendar-empty-days-20261101/instrumentation.log).

Screenshots: [light](verification/calendar-empty-days-20261101/empty-light.png), [dark](verification/calendar-empty-days-20261101/empty-dark.png).

## APK and update verification

- Version: **1.0.3 / versionCode 4**, previously **1.0.2 / versionCode 3**.
- Package: `com.koreasalary.calculator` (unchanged).
- APK: `/Users/wrt/Desktop/Project/Salary calculator/app/build/outputs/apk/debug/app-debug.apk`.
- APK SHA-256: `c9ec6ed9ab0037cdd207f5ff49dabb7a0b2d18c2ede14422f01d6b46f1749c09`.
- Old v3 APK SHA-256: `6e89eb653295750888330638caeec3a1df46f5ce5c852b24f57bfa90fc04ad5e`.
- Matching old/new signing certificate SHA-256: `49800792c9b817ce87f43899affe286e83e5cad951600b107d6271d0655532cd` (existing local Android Debug signer).

A fresh read-only emulator was used to verify the final v3→v4 replacement without uninstalling or downgrading an installed package. The real retained v3 APK seeded a day record, saved template, monthly snapshot/rate/deductions/allowances and language/theme preferences. After `adb install -r` of the final v4 APK, a new instrumentation process verified equality of those values and gross salary ₩217,500. [Upgrade log](verification/calendar-empty-days-20261101/upgrade-v3-v4.log).

Commands used with the existing installed JBR/SDK:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-apk-upgrade.sh \
/private/tmp/ksalary-empty-days-20261101/previous-v3.apk \
app/build/outputs/apk/debug/app-debug.apk \
app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk emulator-5556

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-calendar-editor.sh emulator-5556
```

No physical-device run is claimed; Samsung was not connected or modified. No new signing credentials, dependencies or skills were introduced. Jev and K-Dense did not offer material value for this small deterministic drawing change.

## Changed files in this follow-up

- `app/src/main/java/com/koreasalary/calculator/presentation/screens/calculator/CalculatorScreen.kt`: conditional borders and empty-only Saturday blue.
- `app/build.gradle.kts`: version progression only.
- `app/src/androidTest/java/com/koreasalary/calculator/DayEditDialogTest.kt`: focused regression, revised empty-day expectations, test-scene positioning.
- This report and `docs/verification/calendar-empty-days-20261101/`: logs, screenshots and [verification receipt](verification/calendar-empty-days-20261101/verification.json).
