# Calendar outer borders and paid-day text correction — 2026-11-01

Follow-up on `codex/calendar-editor-payroll`. All prior uncommitted work is preserved.

## Result

The outer border now uses the same `palette.danger` role as the existing red date number and inner outline whenever the date is Sunday or `isPaidHoliday` is manually enabled. This branch precedes the normal occupied-cell border branch, including when work is entered. Other border choices retain their existing theme colors and widths.

Paid-holiday and weekly-paid-rest **status text** uses `palette.danger`. Classification uses saved flags and the absence of physical work, never translated text. The existing badge background is deliberately preserved, as are work-badge colors. Actual work still takes priority: base + overtime + holiday + holiday overtime, through the existing compatibility decoder; no night-premium or paid-allowance hours are added.

An untouched Sunday no longer creates a Rest badge automatically. A Rest badge requires `isSundayPay`; a Holiday badge requires `isPaidHoliday`. Date number/outline/border styling is independent of those badges.

Cell aspect ratio, border thickness, corner shapes, spacing, backgrounds, typography, click handling and occupied-cell indication are unchanged. No payroll, Room, migration, persistence, preference or translation source was modified in this correction; a pre/post SHA-256 comparison confirmed those files remained byte-identical.

## October 2026 verification

The Android regression renders these exact dates using the actual application themes:

| Date | Fixture | Result |
| --- | --- | --- |
| October 5, Monday | Paid holiday, no work | Red outer border, date/inner outline and holiday status text |
| October 9, Friday | Paid holiday, no work | Red outer border, date/inner outline and holiday status text |
| October 11, Sunday | Weekly paid rest, no work | Red outer border, date/inner outline and rest status text |
| October 25, Sunday | 8 base + 2 overtime + 4 night premium, paid-rest flag | Red outer border/date, `10 ч` work badge in Russian, no Rest badge |
| October 6, Tuesday | 8 base hours | Existing normal occupied-cell border, `8 ч` |
| October 10, Saturday | 4 base hours | Existing normal occupied-cell border, `4 ч` |
| October 18, Sunday | No record | Red borders/date, no badge |

Both **light and dark themes** and **all 12 supported languages** passed. Outer/inner border pixels, date text style colors, semantic paid-day text colors, work labels and absence of an untouched-Sunday badge are asserted. Existing theme-specific normal border colors remain unchanged; in particular, this task does not recolor the application's dark-theme accent palette.

Screenshots: [light](verification/calendar-cell-style-20261101/october-light.png), [dark](verification/calendar-cell-style-20261101/october-dark.png). The fixture places seven real calendar cells side by side at 320 dp width, below system bars.

## Tests and build

- New regression failed before the fix: an untouched Sunday generated a Rest badge; the Android regression also failed on the **outer** border of October 5. [Unit/build red log](verification/calendar-cell-style-20261101/regression-before-unit.log), [UI red log](verification/calendar-cell-style-20261101/regression-before-ui.log). The first test-APK compilation also exposed a missing test-only import, corrected before the Android red run.
- **46 JVM tests passed**, including existing payroll tests and the updated/new calendar regressions.
- **9 Android tests passed**, including Room/backup checks, keyboard/focus checks and the new October styling/language regression. [Log](verification/calendar-cell-style-20261101/instrumentation.log).
- The October regression passed again after setting the test fixture's background to the real theme surface for accurate dark screenshots. This changed only the test scene, not application code. [Final focused UI log](verification/calendar-cell-style-20261101/final-october-ui.log).
- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`: successful offline builds using existing dependencies.
- `git diff --check`: passed.

Reproduction uses the existing `scripts/verify-calendar-editor.sh emulator-5554` runner with `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'` and `ANDROID_HOME="$HOME/Library/Android/sdk"`. No dependencies or skills were installed.

## Upgrade and APK

Version progresses from **1.0.1 / versionCode 2** to **1.0.2 / versionCode 3**. The package remains `com.koreasalary.calculator`; Room remains version 6. The existing local debug signing configuration is retained.

A real replacement test used the retained v2 APK, seeded persisted fixtures under v2, executed `adb install -r` with v3, and verified the fixtures in a new process. Saved day record, hours/flags/note/template reference, shift template, monthly snapshot/payroll settings, language, theme and the ₩217,500 gross-pay result all survived. No uninstall or application-data clearing was used. [Upgrade log](verification/calendar-cell-style-20261101/upgrade-v2-v3.log).

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-apk-upgrade.sh \
/private/tmp/ksalary-cell-style-20261101/previous-v2.apk \
app/build/outputs/apk/debug/app-debug.apk \
app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk emulator-5554
```

- Old v2 APK SHA-256: `74a0e834e57fd79e18484783feb6f550bdf849b45a707999c72803d1a7fc0cc4`.
- New v3 APK: `/Users/wrt/Desktop/Project/Salary calculator/app/build/outputs/apk/debug/app-debug.apk`.
- New v3 APK SHA-256: `6e89eb653295750888330638caeec3a1df46f5ce5c852b24f57bfa90fc04ad5e`.
- Matching old/new signing certificate SHA-256: `49800792c9b817ce87f43899affe286e83e5cad951600b107d6271d0655532cd` (existing Android Debug certificate).

Verification used a read-only Pixel_8 Android 37.1 emulator at 320×568 dp, font scale 1.0. Samsung was not connected or modified. Compatibility is verified against the retained local v2 APK; no physical-device update success is claimed.

## Files changed in this follow-up

- `app/src/main/java/com/koreasalary/calculator/presentation/screens/calculator/CalculatorScreen.kt`: outer border precedence, semantic status text color, explicit-only paid-day badges, nonvisual badge test tags.
- `app/build.gradle.kts`: version progression only.
- `app/src/test/java/com/koreasalary/calculator/presentation/screens/calculator/CalendarIndicatorTest.kt`: updated no-automatic-rest expectation and focused regression.
- `app/src/androidTest/java/com/koreasalary/calculator/DayEditDialogTest.kt`: October fixtures, both themes and all-language assertions.
- This report and `docs/verification/calendar-cell-style-20261101/`: evidence and hashes.

Jev and K-Dense capabilities were considered; deterministic flag/color checks and installation assertions were sufficient. No semantic advisory call or unrelated review stage was needed.
