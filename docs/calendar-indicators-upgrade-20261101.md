# Calendar indicators and APK replacement — 2026-11-01

Branch: `codex/calendar-editor-payroll`. This follow-up preserves the uncommitted calendar-editor/payroll milestone.

## Changes

- `presentation/screens/calculator/CalculatorScreen.kt`: red outline around the date number on every Sunday and manually flagged public holiday, including worked/resting dates. Other date numbers use the theme's blue accent outline. Existing outer cell/shift indication and click handling remain in place. Saturdays do not become holidays automatically.
- The work badge now shows the sum of decoded base + overtime + holiday + holiday overtime hours. The existing `WorkHours` compatibility decoder is reused; the separated legacy 1.5× columns each contribute exactly once. Night premiums and paid-day allowances contribute no physical hours. A worked date shows hours before any paid-day label; a nonworking date retains its localized holiday/rest label. Neither premium-work category implies holiday status.
- `app/build.gradle.kts`: `versionCode=2`, `versionName=1.0.1`. Package remains `com.koreasalary.calculator`. The existing debug signing configuration was retained. No production signing credentials were created or replaced.
- `CalendarIndicatorTest.kt`: six unit cases for borders, labels, mixed/fractional/legacy hours, nonworking paid days and rotating/Saturday work.
- `DayEditDialogTest.kt`: adds real rendered date-outline pixel checks and work-label assertions in both palettes.
- `ApkUpgradeProbe.kt`, `scripts/verify-apk-upgrade.sh`: emulator-only two-process persisted-state probe around actual package replacement. The standard UI runner excludes the explicit upgrade probe.

Payroll arithmetic, Room schema (6), migrations, backup format (4), application permissions and saved data formats are unchanged by this follow-up.

## Verification

Two new unit regressions failed on the old badge code, showing paid-day labels instead of `8 ч` / `10 ч`; the same tests pass after the fix. See [before-fix log](verification/calendar-upgrade-20261101/regression-before-fix.log).

- **45 JVM tests passed**, including all earlier payroll tests and six new calendar cases.
- **8 Android instrumentation tests passed** on the installed Pixel_8 Android 37.1 emulator at 320×568 dp, font scale 1.0. The earlier Room migration, backup and keyboard checks remain included.
- `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`: successful offline builds using existing dependencies.
- `git diff --check` and shell-script syntax checks passed.
- Upgrade seed and verification probes each passed under separate instrumentation processes, on either side of `adb install -r`.

Commands used:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-calendar-editor.sh emulator-5554

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME="$HOME/Library/Android/sdk" \
scripts/verify-apk-upgrade.sh \
/private/tmp/ksalary-upgrade-20261101/previous-v1.apk \
app/build/outputs/apk/debug/app-debug.apk \
app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk emulator-5554
```

The retained previous APK is the actual October 31 v1 artifact, SHA-256 `dfada807ce1280c875613c61da0015a60ba0c08093772c962c4fe3f83afe6c41`, not a freshly reconstructed old build. It is retained in the task's temporary evidence directory; the script accepts another explicitly supplied earlier APK.

The probe installed that v1 APK, seeded the real application database and settings under v1, replaced the package with v2, and verified equality of the day record (all hour columns, flags, note, template reference), saved template, monthly snapshot (rate, deductions, allowances, timestamp), language and theme. The retained gross-pay result was ₩217,500. No uninstall or application-data clearing was used. This is a replacement test, not a fresh-install substitute. The existing Room 5→6 migration remains covered by the separate storage test.

[Upgrade log](verification/calendar-upgrade-20261101/upgrade.log), [Android test log](verification/calendar-upgrade-20261101/instrumentation.log), [hashes and result receipt](verification/calendar-upgrade-20261101/verification.json), [light palette](verification/calendar-upgrade-20261101/calendar-light.png), [dark palette](verification/calendar-upgrade-20261101/calendar-dark.png).

## APK and signing evidence

APK: `/Users/wrt/Desktop/Project/Salary calculator/app/build/outputs/apk/debug/app-debug.apk`

- Version: **1.0.1 / 2** (previous **1.0.0 / 1**).
- APK SHA-256: `74a0e834e57fd79e18484783feb6f550bdf849b45a707999c72803d1a7fc0cc4`.
- Both old and new APK signer certificate SHA-256: `49800792c9b817ce87f43899affe286e83e5cad951600b107d6271d0655532cd`.
- Certificate subject: `C=US, O=Android, CN=Android Debug`.

Local v1→v2 compatibility is **VERIFIED**. Compatibility with a differently signed APK is not established. Android requires compatible package identity and signing certificates (or an authorized signing lineage); increasing versionCode cannot repair a signature mismatch. See [Android app signing](https://developer.android.com/studio/publish/app-signing) and [Android update requirements](https://developer.android.com/google/play/app-updates).

The repository uses default local debug signing and CI also builds debug APKs without a configured persistent distribution key. Debug artifacts from different environments may have different certificates. This is a compatibility risk, not evidence that it caused the Samsung failure. Continue using the existing compatible key for this installation; a permanent production-signing policy requires separate authorization and knowledge of the installed signing identity.

## Samsung limitation and safe verification procedure

The Samsung was not connected to adb at investigation time; only the task's emulator was connected. No Samsung package, restored data, settings or files were modified. Its original installer status code and pre-uninstall APK/certificate are **UNKNOWN**. The cause of that earlier rejection cannot be determined from the local APKs alone; neither certificate mismatch nor versionCode is claimed as the confirmed cause.

For a safe physical-device follow-up:

1. Connect and authorize the Samsung over USB. Codex can perform the technical checks; no terminal work by the user is needed.
2. Read the installed package's version and APK path using `dumpsys package` and `pm path`; copy only the installed APK for certificate inspection. This does not modify the application or its data. Retain the user's existing JSON backup.
3. Compare installed package ID, installed versionCode, signer fingerprint/lineage and candidate APK certificate. If signatures are incompatible, stop before installation. Obtain the original compatible signing path or an authorized lineage; a version bump alone is insufficient. Do not uninstall, clear data, change package ID or bypass signature verification.
4. Only after compatibility is established, perform the explicitly authorized update with the installer or `adb install -r` and retain its exact result. On rejection, preserve data and record the installer error instead of uninstalling. Verify records, templates, monthly history and settings after success.

No device-specific update success is claimed. The tested v2 APK is compatible with the retained local v1 artifact; whether Samsung currently contains that exact signing identity requires the read-only comparison above.

## Skills

`diagnosing-bugs` supplied the failing-test/verification loop. Available skill names and the shared Workbench inventory were checked. No applicable K-Dense scientific skill is installed for this project; nothing was installed or copied. Jev was considered but not dispatched: certificate/package/version comparison and persisted-data assertions were deterministic, and no semantic architecture uncertainty remained. Jev cannot establish the missing Samsung installer facts.
