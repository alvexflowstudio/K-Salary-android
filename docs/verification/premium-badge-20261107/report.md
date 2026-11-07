# Calendar worked-hours badge color correction — 2026-11-07

## Problem

`CalendarCell` colored the worked-hours badge with `palette.success` (green) whenever `isPaidHoliday` or `isSundayPay` was set, checked **before** the actual work categories. Paid-holiday/weekly-rest flags therefore hid real premium work: e.g. 10 ч of 1.5× work on Sunday 2026-10-25 showed green, while 12 ч of 1.5× work on Saturday 2026-10-24 showed the premium color only because the day had no flag.

## Change (only `CalculatorScreen.kt`, badge color selection)

New category resolution before any flag check — `calendarWorkKind(dayOfWeek, record)`:

1. **Premium (ruby `danger`):** `holiday`/`holidayOvertime` hours from explicit categories; legacy `workHoursVersion = 1` records decode into these categories via `WorkHours.from`; and combined 1.5× `overtime` hours entered on Saturday, Sunday, a manually marked paid holiday, or a weekly-rest (paid sunday pay) day.
2. **Night (`purple`):** night premiums when no premium work exists (unchanged styling, +0.5× never implies holiday work).
3. **Overtime (`warning` orange):** 1.5× overtime on an ordinary weekday without holiday/paid-rest flags.
4. **Base (`success` green):** remaining base work, on any weekday, Saturday, Sunday, or manually flagged day.

A `null` kind (no physical work) keeps the existing paid-holiday status styling (`success` background with red text via the existing `badgeTextColor` rule). Date-number colors, red date borders, cell backgrounds, hour totals, label text and all 12 localizations are unchanged; no pay calculations, input validation or eight-hour base limit touched.

## Verification

- `./gradlew --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` — BUILD SUCCESSFUL (includes full unit suite).
- Focused unit tests added to `CalendarIndicatorTest.kt` (`calendarWorkKind` / `calendarBadgeColor`):
  - ordinary weekday base → green; 1.5× overtime → orange; night-only → purple;
  - base-only on **every** weekday incl. Sat/Sun → green; night premium alone on a paid day → purple;
  - Sat/Sun 1.5× → ruby; manually marked holiday with 1.5×/2.0× hours → ruby; paid-rest flag + premium hours → ruby;
  - premium beats base/night/flags; ordinary weekday overtime stays orange;
  - legacy `workHoursVersion = 1` holiday-work records decode to ruby; no-work paid days keep status styling; palette colors for light and dark themes;
  - October 22–25 2026 example: 12 × 1.5 on Sat/Sun ruby (Sunday 25 with the paid-rest flag no longer green), 8 ч Thursday base green, Friday overtime orange, clamped 12-base entry stays green.
- On the emulator (Pixel_8, Android 37.1, 320×568 dp): the two pre-existing `DayEditDialogTest` calendar-cell tests exercising badge colors/borders (`octoberOuterBordersAndStatusColorsInBothThemesAndAllLanguages`, `calendarBordersAndWorkedLabelsRenderInBothThemes`) pass **before** and **after** the change.

## Evidence (`before/` + `after/` screenshots, 320×568 dp)

- `october-cell-light/dark`: before — October 25 "10 ч" (1.5× + weekly-rest flag) green; after — ruby. Status cells 5/9/11 and dates/borders unchanged.
- `calendar-light/dark`: before — cell 1 (Sunday 8 ч + night 8 ч, paid-rest) green and cell 2 (8 holiday hour + 2) green; after — cell 1 purple (night premium visible), cell 2 ruby; cell 3 ordinary Saturday overtime stays orange; non-working "Празд." cell styling unchanged.

## Scope

`CalculatorScreen.kt`, `unit CalendarIndicatorTest.kt` and this evidence folder only. Pre-existing uncommitted `build.gradle.kts` / `gradle-daemon-jvm.properties` edits preserved. No push, merge, release, or versionCode change.
