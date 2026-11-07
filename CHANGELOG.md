# Changelog

All notable changes to K-Salary will be documented in this file.

The project follows Semantic Versioning where practical.

## [Unreleased]

## [1.0.6] - 2026-11-07

### Fixed
- Day-edit dialog action row: "Сохранить" keeps one complete centered line at any width, "Очистить день" wraps into two centered lines, balanced two-action/three-action layouts.
- Calendar worked-hours badges now follow the saved work categories (premium 1.5×/2.0×, night, ordinary overtime, base work) instead of paid-holiday/weekly-rest flags, so premium weekend and holiday work is no longer colored green. Status styling for non-working paid days is unchanged.

### Release scope
- **1.0.6 / versionCode 7**, attaches the production-signed release APK (`app-release.apk`, user key `AlvexFlow Studio`) with its SHA-256 identity; debug builds remain local.
- No pay-rate rules, input validation or storage schema changes.


## [1.0.5] - 2026-11-01

### Added
- Shared four-category calendar and shift-template editor with validation, decimal commas, keyboard navigation and scrolling actions.
- Versioned legacy work-hours decoding, additive Room 5→6 migration and backup format 4 with imports from formats 1–3.
- Payroll, localization, layout, storage and update regression tests with verification reports and screenshots.

### Changed
- Base work remains available on any scheduled weekday, including Sunday; paid-day flags preserve manual work categories.
- Paid holiday/weekly rest pays a separate fixed eight-hour allowance; overlapping flags on one date pay once. Historical recalculated totals can change under these corrected allowance rules.
- Calendar badges prioritize physical worked hours, excluding night premiums and paid allowances. Populated cells use red/blue outer borders, no inner date frame, and fixed 9 sp two-line labels; empty cells have no border or automatic status.
- Updated all 12 interface languages, localized date headers, fitted pay summaries and installed-version display.

### Release scope
- **1.0.5 / versionCode 6**, personally tested and approved by the user on Samsung.
- Source and documentation release only: the exact tested APK is debug-signed and is not published as a production build.
- See [release notes](docs/releases/v1.0.5.md) for verification, artifact identity and data-compatibility limits.

## [1.0.0] - 2026-09-06

### Added
- Salary calculation for regular working hours
- Weekly paid holiday allowance (주휴수당)
- Overtime pay calculation
- Night work allowance
- Holiday and holiday overtime calculation
- Custom bonuses and allowances
- 3.3% withholding mode
- National Pension calculation
- Health Insurance calculation
- Long-Term Care Insurance calculation
- Employment Insurance calculation
- Estimated income tax
- Custom deductions
- Severance pay calculator (퇴직금)
- Shift calendar and shift templates
- Local salary data storage
- Multilingual interface with 12 supported languages
- Light and dark themes
- Salary guidebook
