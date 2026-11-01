#!/usr/bin/env bash
# Use an already installed SDK/AVD and cached dependencies; never download packages.
set -euo pipefail
cd "$(dirname "$0")/.."
serial="${1:-emulator-5554}"
adb="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}/platform-tools/adb"
report_dir="$PWD/app/build/reports/calendar-editor"
mkdir -p "$report_dir"
if [[ "$("$adb" -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
    echo "An already running Android emulator is required: $serial" >&2
    exit 1
fi
previous_size=$("$adb" -s "$serial" shell wm size | awk '/Override size:/ {print $3}' | tr -d '\r')
previous_density=$("$adb" -s "$serial" shell wm density | awk '/Override density:/ {print $3}' | tr -d '\r')
previous_font=$("$adb" -s "$serial" shell settings get system font_scale | tr -d '\r')
previous_ime=$("$adb" -s "$serial" shell settings get secure show_ime_with_hard_keyboard | tr -d '\r')
restore_settings() {
    "$adb" -s "$serial" shell wm size "${previous_size:-reset}"
    "$adb" -s "$serial" shell wm density "${previous_density:-reset}"
    if [[ "$previous_font" == "null" ]]; then
        "$adb" -s "$serial" shell settings delete system font_scale
    else
        "$adb" -s "$serial" shell settings put system font_scale "$previous_font"
    fi
    if [[ "$previous_ime" == "null" ]]; then
        "$adb" -s "$serial" shell settings delete secure show_ime_with_hard_keyboard
    else
        "$adb" -s "$serial" shell settings put secure show_ime_with_hard_keyboard "$previous_ime"
    fi
}
trap restore_settings EXIT
"$adb" -s "$serial" shell wm size 640x1136
"$adb" -s "$serial" shell wm density 320
"$adb" -s "$serial" shell settings put system font_scale 1.0
"$adb" -s "$serial" shell settings put secure show_ime_with_hard_keyboard 1
./gradlew --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest | tee "$report_dir/build.log"
"$adb" -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s "$serial" shell am instrument -w -r -e notClass com.koreasalary.calculator.ApkUpgradeProbe com.koreasalary.calculator.test/androidx.test.runner.AndroidJUnitRunner | tee "$report_dir/instrumentation.log"
# am instrument may exit 0 even when JUnit failed. Check the runner's result.
rg -q '^OK \([1-9][0-9]* tests?\)' "$report_dir/instrumentation.log"
for name in sunday-base-keyboard actions-keyboard; do
    "$adb" -s "$serial" pull "/sdcard/Android/data/com.koreasalary.calculator/files/$name.png" "$report_dir/$name.png"
done
