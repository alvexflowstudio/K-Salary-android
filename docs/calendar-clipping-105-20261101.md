# K-Salary 1.0.5 calendar-cell clipping correction — 2026-11-01

## Result and scope

Calendar-only layout correction on `codex/calendar-editor-payroll`, based on the existing uncommitted 1.0.4 milestone. The populated date frame is removed; the outer frame, seven-column geometry, dimensions, inter-cell gaps, date colors, backgrounds, manual status classification and click path are retained. Ordinary populated cells use the existing blue `LightAppPalette.accent` in both themes. The previous dark-theme outer border inherited a beige accent; this one border color is corrected to meet the explicit blue-border requirement. Sunday/manual-paid-holiday borders remain the existing red. Empty days keep no frame and no generated badge.

Populated content is top-aligned. Removing the inner date border/padding and populated column padding recovers vertical space. Badges use 1 dp horizontal padding, no extra vertical padding, fixed **9 sp** bold text, 10 sp line height, up to two centered lines, and phrase-aware line breaking. Their text width is the full available badge width, so paragraph centering cannot paint its last glyph outside a narrower wrap-content text node. The 12 sp date stays visible. No autoshrinking `FittingText` remains in calendar cells; that shared component is unchanged elsewhere.

Only three existing files changed in this task:

- `app/src/main/java/com/koreasalary/calculator/presentation/screens/calculator/CalculatorScreen.kt`: populated cell internals and explicit ordinary blue border.
- `app/src/androidTest/java/com/koreasalary/calculator/DayEditDialogTest.kt`: three focused tests; existing border assertions now target the outer frame; removal of inner frames is checked; installed-version assertion reads package metadata instead of pinning 1.0.4.
- `app/build.gradle.kts`: `1.0.5` / `6`.

This report and `docs/verification/calendar-clipping-105/` are new. Existing documentation and historical evidence are not edited.

## Direct diagnosing-bugs application

Read and directly applied `/Users/wrt/.agents/skills/diagnosing-bugs/SKILL.md`; the operator supplied the same skill. This is **direct file-based skill application**, with actual Android instrumentation execution. It does **not** establish a separate native Desktop `$diagnosing-bugs` dispatch/resolution receipt; native invocation remains UNVERIFIED. No project/ancestor AGENTS.md, GLOSSARY.md or relevant ADR was found; the supplied global instructions were followed. The shared skill inventory was inspected. No suitable K-Dense skill is installed for this project; no installation or cross-project skill reuse occurred. Jev's installed workflow was read/considered; no hosted call was useful for this deterministic layout correction with unchanged terminology, so none was dispatched.

| Phase | Disposition and evidence |
| --- | --- |
| 1 — Feedback loop | COMPLETED. Real `CalendarCell`, production 16+14 dp horizontal page/card padding, seven weighted columns and 4 dp gaps, on 320×568 dp Android. Added `calendarContentStaysInsideCellsAtReadableSize` before changing production code. `before.log` fails on fonts below 8.5 sp; exact baseline minimum 5.4991455 sp in Korean. Captures text and cell bounds, paragraph flags and line extents. The smaller single-cell loop runs in seconds. |
| 2 — Reproduce/minimize | COMPLETED for the baseline unreadable-size/low-placement defect. `calendarCellMinimumRepro` leaves one 48.25-hour Korean cell and six width-preserving spacers; both `minimum-red-1.log` and `minimum-red-2.log` fail at 5.4991455 sp. Short values in the same baseline matrix pass. A full production `CalculatorScreen` baseline was also captured from the exact retained 1.0.4 APK, with real Room/ViewModel fixture data and working date selection (`screen-before.log`). |
| 3 — Hypothesize | COMPLETED. Ranked predictions were shown before probes: (1) date frame/padding plus SpaceBetween pushes badges down; remove them to move badges upward; (2) single-line width/padding causes autoshrink; allow two lines at 9 sp; (3) line metrics can clip Thai/Korean glyphs; test the line interval/word breaking. No bisection was needed because the affected cell seam and baseline were directly available. |
| 4 — Instrument | COMPLETED. Targeted test-only JSON/screenshot probes. `probe1` moves the long Korean badge upward by 20 px / 10 dp without changing the cell rectangle, but keeps the font at 5.499 sp. The first fixed-9-sp two-line probe (`probe2`) genuinely clips its second line, demonstrating why simply enabling wrap is insufficient. Reducing badge padding (`probe3`), then line interval (`probe4`) isolates the height budget. Phrase-aware wrapping keeps Korean `시간` together. Centered-paragraph measurement caught a further concrete edge case: a 48 px Text had a line from x=4 to x=52; `centered-paragraph-width.json` records it. Giving Text its actual available width resolves this paint overflow. |
| 5 — Fix/regression | COMPLETED. Correct seam is the production cell and complete calculator screen, not a text-only mock. Test existed and failed before the fix; `fixed.log` passes the original 12-language matrix plus the minimum repro in 3.784 seconds. Final APK verification is recorded below. |
| 6 — Cleanup | COMPLETED. No production debug logging or throwaway production code was added. Measurements remain intentionally in Android tests and the clearly named evidence directory. Temporary probes live only in `/private/tmp/ksalary-calendar-105`. Original loops re-run successfully. No commit/PR was authorized; the confirmed cause and correction are recorded here instead. |

**Baseline evidence limitation:** unmodified 1.0.4 at emulator font scale 1.0 did not show a Text rectangle escaping its cell: autoshrinking masked the height/width pressure by making long labels about 5.5–6.5 sp. Do not describe the controlled `probe2` screenshots as unmodified 1.0.4. Baseline screenshots demonstrate the original small, low-set labels and date frame; the controlled probes demonstrate real clipping and its measured correction. The operator's separate device-specific glyph-clipping observation was not independently certified on that physical device.

Already-executed tight red command (with test APK installed on the emulator):

```sh
/Users/wrt/Library/Android/sdk/platform-tools/adb -s emulator-5556 shell am instrument -w -r -e class 'com.koreasalary.calculator.DayEditDialogTest#calendarCellMinimumRepro' com.koreasalary.calculator.test/androidx.test.runner.AndroidJUnitRunner
```

Red output: `AssertionError: 48.25 hours at 34dp: font=5.4991455.sp`; `Tests run: 1, Failures: 1` twice. Fixed command: `OK (1 test)` as part of the final complete suite. The wider loop is the same command with `#calendarContentStaysInsideCellsAtReadableSize`; it records all locales/themes and asserts containment, no text overflow, no overlap, font >=8.5 sp and unbroken Korean hours units.

## Localization disposition

**Exact translation changes: none.** No `old string → replacement string` pair exists in this task: every localization definition, resource and format placeholder is byte-identical to task start. The layout now accommodates the existing words. No coefficient, unit meaning, paid-day/work-hour distinction or Sunday warning changed. Native-language uncertainties previously recorded in `docs/localization-audit-20261101.md` remain there (notably compact KO `유급`, EN `Paid`/`Off`, UZ `Dam`, Filipino register and Malay holiday specificity). None was guessed or silently shortened.

| Locale | Hours unit | Paid-holiday badge | Paid-rest badge | Change |
| --- | --- | --- | --- | --- |
| RU | `ч` | `Празд` | `Вых` | Unchanged |
| KO | `시간` | `유급` | `휴무` | Unchanged |
| EN | `h` | `Paid` | `Off` | Unchanged |
| KY | `саат` | `Майрам` | `Дем` | Unchanged |
| UZ | `soat` | `Bayram` | `Dam` | Unchanged |
| KK | `сағ` | `Мереке` | `Демалыс` | Unchanged |
| VI | `giờ` | `Lễ` | `Nghỉ` | Unchanged |
| TH | `ชม.` | `หยุด` | `พัก` | Unchanged |
| FIL | `oras` | `Holiday` | `Pahinga` | Unchanged |
| ID | `jam` | `Libur` | `Rehat` | Unchanged |
| MS | `jam` | `Cuti` | `Rehat` | Unchanged |
| MY | `နာရီ` | `ပိတ်` | `နား` | Unchanged |

The actual Android glyph rendering, not a browser substitute, is captured for every locale and theme. Calendar fixtures include 8, 0.5, 10.5, 23.75 and 48.25 hours; paid holiday, paid rest, overlapping flags, Sunday work, ordinary populated days, a saved note and empty weekdays/Saturday/Sunday. 48.25 is a legacy/large-value rendering stress fixture, not a claim about a physically possible single-day shift. Night premiums are present in work fixtures and remain excluded from worked-hour badges; paid-day allowances remain excluded as before.

## Final verification

- Final offline build: `testDebugUnitTest assembleDebug assembleDebugAndroidTest` — PASS. 49 JVM tests, 0 failures/errors/skips. This includes 13 payroll/calendar compatibility tests, 16 salary-engine tests and the localization/format-argument regressions.
- Final exact APK on 320×568 dp: **16 Android tests PASS**, 110.353 s (`final-full-verification.log`). Includes storage/migration/backup tests, original entry/focus/IME regressions, new cell-content/minimum/actual-screen regressions, outer colors and absence of inner/empty frames. No Android test was skipped in this suite; the special two-stage upgrade probe was intentionally run separately.
- Final 360×800 dp: **2 calendar tests PASS**, 7.690 s. Final 411×891 dp: **2 calendar tests PASS**, 7.723 s. Each covers all 12 locales/two themes in cells plus TH/KO/EN on the actual full calculator screen and real date selection.
- **72 native cell-layout cases, 1,800 Text measurements, minimum badge font 9 sp, zero clipping/overlap/overflow/unreadable-size failures.** All six 320 dp baseline/current case pairs have exactly identical cell rectangles (width, height and placement), recorded in `layout-summary.json`. Aspect ratio remains 0.85; grid/page/card spacing is unchanged in source.
- Final screenshots: 72 cell-matrix images plus 18 full production-screen images. Baseline: 6 matrix + 6 production-screen images; 18 clearly separated intermediate probe screenshots. Priority TH/KO/EN final full-screen images were visually inspected in both themes; Burmese native glyph rendering was also inspected. Automated native layout assertions cover every locale/theme/size.
- Real `adb install -r` 1.0.4/code 5 → 1.0.5/code 6: **PASS**, no uninstall/clear/downgrade. The saved day/note/template link, saved template, monthly snapshot (rate, deductions, allowances, timestamp), Korean locale/dark theme and control gross salary **₩217,500** survive the replacement (`upgrade-v5-v6.log`). Earlier JVM tests still give ordinary Sunday 8 h = ₩80,000, paid Sunday base 8 h = ₩160,000, paid holiday 8 h at 1.5× + 2 h at 2× + one allowance = ₩240,000, mixed work/night case = ₩150,000, at ₩10,000/h with deductions disabled.
- No translation/source-resource/placeholder changes. Full i18n directory hashes are included in `preservation.json`; focused localization tests pass. No coefficients or salary formulas changed.
- `git diff --check` — PASS. HEAD/branch unchanged. All 578 other pre-existing files (including old audit/evidence) are byte-identical to task start.

Artifact: `/Users/wrt/Desktop/Project/Salary calculator/app/build/outputs/apk/debug/app-debug.apk`, **debug**, `com.koreasalary.calculator`, **1.0.5 / code 6**.

APK SHA-256: `2475d4bb10f4985dc4a8bd07f3acff64e33c4554762999a1d0fd600d1fa851ba`.

Existing signer SHA-256: `49800792c9b817ce87f43899affe286e83e5cad951600b107d6271d0655532cd` (C=US, O=Android, CN=Android Debug). `apksigner verify --print-certs` succeeds for both artifacts, with identical signer identity. Retained previously delivered 1.0.4 APK SHA-256: `a40f919fcc79100c5effcc25ffe59292b150bd275875a1a7cf92330d07bf0ecb`. See `apk-identity.json`; this verifies compatibility against that exact previously delivered artifact, not an unspecified external release channel.

Skill statuses, APK identity, source preservation and evidence hashes are bound by `verification.json` and its linked JSON files.

## Screenshot comparisons

Full production-screen baseline and after images at 320×568 dp (PNG, 640×1136 px; no rescaling). Both use October 2026 fixtures and the actual calculator screen. The scroll anchor is the first date, so removal of its internal frame can move the screen capture by approximately 2 dp; the grid itself is unchanged.

| Language/theme | Before: exact 1.0.4 APK | After: final 1.0.5 APK |
| --- | --- | --- |
| TH / light | [Before](verification/calendar-clipping-105/before/calendar-screen-before-TH-false-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-TH-false-320dp.png) |
| TH / dark | [Before](verification/calendar-clipping-105/before/calendar-screen-before-TH-true-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-TH-true-320dp.png) |
| KO / light | [Before](verification/calendar-clipping-105/before/calendar-screen-before-KO-false-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-KO-false-320dp.png) |
| KO / dark | [Before](verification/calendar-clipping-105/before/calendar-screen-before-KO-true-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-KO-true-320dp.png) |
| EN / light | [Before](verification/calendar-clipping-105/before/calendar-screen-before-EN-false-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-EN-false-320dp.png) |
| EN / dark | [Before](verification/calendar-clipping-105/before/calendar-screen-before-EN-true-320dp.png) | [After](verification/calendar-clipping-105/screenshots/calendar-screen-after-EN-true-320dp.png) |

The `screenshots/` directory also contains the complete 12-language cell matrix in both themes at each tested width, plus production-screen captures for TH/KO/EN at larger widths. Diagnostic-probe images are separate and never labeled final.

## Compatibility and limits

The applicationId, signing configuration, manifest/permissions, Room schema, migrations, raw entities, payroll engine, ViewModel, day-entry handling, templates and backup code are byte-identical to task start. The pre-existing uncommitted milestone changes are preserved. `preservation.json` binds those checks to the original file hashes/HEAD/branch and lists the only permitted existing-file changes. The current source still excludes night premiums and paid allowances from physical hours and prioritizes actual worked hours above status badges.

UI validation is on the installed Pixel_8 Android 37.1 emulator, density 320, font scale 1.0. Native Thai/Korean rendering is verified there. Physical devices, Android API 26–32 phrase-break fallback behavior and enlarged accessibility font scales were not executed/certified in this task. Fixed grid geometry limits very large accessibility text; do not infer its validation from the tested font scale. The artifact uses the same debug signing identity as the previously delivered APK; no separate production keystore was accessed or created. Nothing was committed, pushed, merged, published or distributed.
