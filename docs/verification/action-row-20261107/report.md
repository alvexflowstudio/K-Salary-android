# Day-edit dialog action row correction — 2026-11-07

## Problem

In the day-edit dialog footer, three equal-weight buttons with two-line text fitting produced bad wraps in Russian: "Сохранить" broke before its final character ("Сохранит" / "ь"), and "Очистить день" wrapped with the second line left-aligned instead of centered under "Очистить".

## Change (single layout adjustment in `DayEditDialog.kt`)

- Save keeps `FittingText` but with `maxLines = 1` and `textAlign = Center`, so it can never split a word; with the width below it never shrinks at 320 dp (kept ≥ 12 sp, renders at 14 sp).
- Width allocation: three actions → Clear 1.1 / Cancel 0.9 / Save 1.2 weights (Save widest); two actions → 1 / 1 as before. No fixed widths, no smaller default padding (`PaddingValues(4.dp)` unchanged).
- Clear and Cancel labels center every line (`textAlign = Center`, text node fills the action area), so "день" centers beneath "Очистить" in every language.
- Conditional visibility (`existingRecord != null`), test tags, enabled state, scroll behaviour, dialog sizing and all other behavior unchanged; no localization strings touched.

## Verification

- Emulator: Pixel_8 AVD (Android 37.1), overridden to 320×568 dp (`wm size 640x1136` + `wm density 320`), font scale 1.0 — the compact-screen setup from `scripts/verify-calendar-editor.sh`.
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — BUILD SUCCESSFUL.
- `./gradlew --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest` — BUILD SUCCESSFUL.
- `am instrument -e class com.koreasalary.calculator.DayEditDialogTest` at 320×568 dp — OK (16 tests), including the two new focused tests:
  - `actionRowKeepsSaveOneLineAndCentersLabelsAllLocalesBothThemes`: all 12 languages × light/dark; asserts Save is exactly one centered complete line (font ≥ 12 sp), Cancel centered with no overflow, Clear ≤ 2 lines with every line horizontally centered, Save wider than Cancel, and Russian Clear wraps to exactly 2 lines at 320 dp.
  - `twoActionRowStaysBalancedWithOneLineSave`: no record → clear action hidden, equal Cancel/Save widths, both labels one centered line.
- The pre-existing keyboard checks (`compactKeyboardAllowsNextScrollingFlagsNotesAndAllActions`, `existingDayFooterValidationWarningAndInstalledVersionAllLocalesBothThemes`) passed; one transient scroll-timing failure of the latter on the first run passed on two consecutive re-runs.

## Evidence (this folder, `screenshots/`)

- `day-actions-3-ru-light.png` / `day-actions-3-ru-dark.png` — RU, three actions, both themes: "Очистить / день" two centered lines, "Отмена" one line, "Сохранить" one complete line.
- `day-actions-2-ru-light.png` — two-action balanced state.
- `day-actions-3-ID-light.png` / `day-actions-3-ID-dark.png` — longest translated clear label, two centered lines, no clipping.
- `day-actions-3-MY-light.png` — Burmese: two-line labels stay centered inside their buttons, Save one line.
- `actions-keyboard.png` — all three actions visible and tappable with the numeric keyboard open.

## Scope

Only `DayEditDialog.kt` and `DayEditDialogTest.kt` changed; existing unrelated local edits to `build.gradle.kts` and `gradle/gradle-daemon-jvm.properties` were left untouched. No push, merge, release, or version bump.
