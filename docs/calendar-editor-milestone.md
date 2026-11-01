# Calendar editor and payroll milestone

Implemented 2026-10-31 against baseline `0b643a1` on `codex/calendar-editor-payroll`.

## Behavior

- Calendar days and shift templates share four hour inputs: base 1.0× (maximum 8), combined 1.5×, night premium +0.5×, and holiday/rest work beyond 8 hours at 2.0×.
- Every weekday permits base hours. Only focusing the base field on an actual Sunday displays the localized advisory warning. Paid-day flags never disable, recolor, or move entered work hours.
- Each selected paid-day flag adds a separate allowance of exactly 8 hours at 1.0×. Two flags on one date produce one allowance, reported as a paid holiday. Both stored flags remain available. These allowances are excluded from actual work hours and worked-day counts.
- Work categories remain manual. Night hours only add the 0.5× premium. No weekday, holiday service, calendar integration, network capability, or permission chooses a work category.
- Inputs accept non-negative finite numbers and decimal commas. Empty editing fields mean zero; base hours above 8 and invalid numbers disable Save. The combined 1.5× field can exceed 8 because it includes regular overtime and existing combined records. Its retained internal holiday component remains capped at 8; users enter holiday hours beyond 8 in the 2.0× field.
- Reports retain historical holiday classifications; the general 1.5× report label now includes overtime, rest-day and holiday work. Paid-day report counts follow the same overlap precedence as payroll.
- IME Next moves between numeric fields without clearing focus. The entire dialog, including notes, checkboxes and actions, scrolls within a viewport bounded by the host activity's keyboard and system-bar insets. The native dialog uses adjustResize. Host insets avoid Compose 1.7 dialog windows reporting zero IME insets on the tested Android release.

## Compatibility

Room schema **5 → 6** adds only `workHoursVersion` to days and templates. Migration defaults existing rows to 1; new Kotlin records use 2. No original hour, flag, note, template ID, ordering, or monthly-snapshot column is rewritten.

`WorkHours.from` reproduces the old work-category interpretation for version 0/1 records: base work on a flagged legacy date moves into the remaining first-eight holiday capacity, then into holiday overtime. This is a read-time compatibility view. Version 2 retains explicit categories even when flags change. Saving a decoded edit or applying a legacy template writes explicit categories once. Existing 1.5× work therefore does not become 1.0× work merely because the app was upgraded.

The combined field displays `overtimeHours + holidayHours`. An unchanged value preserves both original doubles exactly. On a changed value, the existing holiday portion is retained up to the new total; the remainder goes into the general 1.5× bucket. New combined entries use that general bucket. Neither the date nor paid-day flags silently changes this split. Clearing it explicitly clears both components.

Backup export is **version 4** and includes the per-record semantics version. Imports of versions 1–3 remain supported: absent version fields deserialize as 0 and follow the legacy decoder. Version 4 round trips both legacy and explicit records. Older app versions intentionally reject version 4 backups rather than misinterpreting new manual-category records.

The approved fixed-eight allowance and single-allowance overlap rules apply when recalculating all saved months. Consequently a historical month that previously used a shorter scheduled allowance or two overlapping allowances can change its total. This is the requested payroll correction; historical worked hours and their previous compensation categories remain intact. `scheduledBaseHours` is retained as compatibility metadata and no longer determines allowance size.

## Verified payroll examples

Synthetic hourly rate: **₩10,000**, before deductions.

| Case | Gross pay |
| --- | ---: |
| 8 base hours on any weekday, including Sunday | ₩80,000 |
| 4/2 schedule over 12 dates: eight 8-hour shifts | ₩640,000 |
| Weekly paid rest or paid holiday, no work | ₩80,000 |
| Weekly paid rest plus 8 manually selected base hours | ₩160,000 |
| Paid rest/holiday plus 8 hours at 1.5× | ₩200,000 |
| Paid holiday plus 8 hours at 1.5× and 2 at 2.0× | ₩240,000 |
| 8 base + 2 at 1.5× + 1 at 2.0× + 4 night-premium hours | ₩150,000 |
| Both paid-day flags, no work | ₩80,000 total allowance |
| Both flags plus 8 hours at 1.5× | ₩200,000 |

## Verification and reproduction

- **39 JVM tests passed**: existing salary, severance, info and monthly-summary tests plus 13 calendar payroll cases and 3 entry/localization cases.
- **7 Android instrumentation tests passed**: real Room 5→6 migration, JSON 1–4 round trips through Room, conditional Sunday warning, all other weekdays including manually paid holidays, invalid base validation, unchanged legacy 1.5× split, focus/Next, physical taps on checkboxes and Save, notes and all actions above the keyboard, and all 48 hour labels across 12 languages.
- `testDebugUnitTest`, `assembleDebug`, and `assembleDebugAndroidTest` succeeded offline using existing dependencies.
- The long-lived checkout initially had stale Kotlin incremental outputs (unresolved existing theme symbols). A full `--rerun-tasks -Pkotlin.incremental=false` build passed, followed by a successful normal runner invocation; project dependencies and compiler settings were unchanged.
- Android emulator: installed Pixel_8 AVD, Android 37.1, 640×1136 pixels at density 320 (**320×568 dp**), font scale 1.0, real Gboard numeric and text keyboards. No physical-device or other Android-version run is claimed.
- The cached Gradle UTP runner `com.android.tools.utp:gradle-work-action:32.4.0` is unavailable offline, so `connectedDebugAndroidTest` could not launch. The already built test APK was run directly with `adb shell am instrument`; all seven tests passed. No dependency was downloaded or added.
- Installed Espresso 3.6.1 uses an InputManager API removed on this emulator. UI tests instead use existing public Compose test semantics and Android's native input command through UiAutomation. Checks include actual callback results and toggle/focus state, not just visibility or injected actions.

Run against an already running disposable emulator:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-calendar-editor.sh emulator-5554
```

The runner restores screen/font/keyboard settings and writes logs/screenshots under `app/build/reports/calendar-editor`. It checks JUnit output because `am instrument` can return shell status 0 for failed tests. The application APK is `app/build/outputs/apk/debug/app-debug.apk`.

Machine-readable result and source/artifact hashes: [verification.json](verification/calendar-editor/verification.json).

Review screenshots: [Sunday base input and numeric IME](verification/calendar-editor/sunday-base-keyboard.png), [flags, notes and all actions with text IME](verification/calendar-editor/actions-keyboard.png). Implementation reference: [Android window-insets guidance](https://developer.android.com/develop/ui/compose/system/insets-ui).

## Changed components

- `data/model/CalendarHours.kt`: compatibility decoder, stable combined-field mapping, date-level payroll aggregation, template application.
- `data/local/AppDatabase.kt`, `data/local/entity/{DayRecordEntity,ShiftTemplateEntity}.kt`: additive semantics-version migration.
- `data/model/SalaryModels.kt`, `data/repository/SalaryRepository.kt`: backup version and category documentation.
- `presentation/viewmodel/SalaryViewModel.kt`: shared aggregation and manual-category saving.
- `presentation/components/{HourEntryForm,DayEditDialog,ShiftTemplateSection,CommonComponents,MonthlySummaryWidget}.kt`: shared editor, scrolling/focus, summaries.
- `domain/i18n/{AppStrings,UiTranslations}.kt`: all 12 warnings/labels and matching in-app calculation explanations.
- JVM tests `CalendarPayrollTest`, `HourEntryStateTest`; Android tests `CalendarStorageTest`, `DayEditDialogTest`; `scripts/verify-calendar-editor.sh`.

## Advisory tools

One sanitized Jev call, `jev-1.13.0`: versioned compatibility strategy selected with reported probability 1.0; retaining legacy split while editing the combined field received Noul probability 0.83. Latency 1009.912 ms; usage 660 input / 67 output tokens; public-tariff estimated cost $0.00002772; verified provider charge unavailable. This was design advice, not calculation or acceptance evidence.

The shared inventory and offline K-Dense index at revision `a1043ef544e3a100c28e70a736b6cf32554d0677` were checked. `scientific-critical-thinking` and `experimental-design` are approved discovery records without an applicable verified installation; the two installed scientific bundles belong to Kaggle only. No K-Dense skill was installed, copied or invoked for this task. Deterministic tests supplied the verification evidence.
