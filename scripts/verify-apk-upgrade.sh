#!/usr/bin/env bash
# A real package-replacement check on a disposable, already running emulator only.
set -euo pipefail
if [[ $# -lt 3 ]]; then
    echo 'Usage: verify-apk-upgrade.sh OLD.apk NEW.apk TEST.apk [emulator-5554]' >&2
    exit 2
fi
old_apk="$1"
new_apk="$2"
test_apk="$3"
serial="${4:-emulator-5554}"
sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
adb="$sdk/platform-tools/adb"
build_tools="${ANDROID_BUILD_TOOLS:-$sdk/build-tools/36.0.0}"
if [[ "$("$adb" -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" != 1 ]]; then
    echo 'Refusing to seed fixtures on a physical device.' >&2
    exit 1
fi
for apk in "$old_apk" "$new_apk"; do
    "$build_tools/apksigner" verify --print-certs "$apk"
    "$build_tools/aapt" dump badging "$apk" | sed -n '1p'
done
old_cert=$("$build_tools/apksigner" verify --print-certs "$old_apk" | sed -n 's/Signer #1 certificate SHA-256 digest: //p')
new_cert=$("$build_tools/apksigner" verify --print-certs "$new_apk" | sed -n 's/Signer #1 certificate SHA-256 digest: //p')
[[ -n "$old_cert" && "$old_cert" == "$new_cert" ]] || { echo 'Signing mismatch; stop without modifying installed data.' >&2; exit 1; }
python3 - "$build_tools/aapt" "$old_apk" "$new_apk" <<'PY'
import re, subprocess, sys
metadata=[]
for apk in sys.argv[2:]:
    line=subprocess.check_output([sys.argv[1], 'dump', 'badging', apk],text=True).splitlines()[0]
    metadata.append(dict(re.findall(r"(\w+)='([^']*)'",line)))
a,b=metadata
assert a['name']==b['name']=='com.koreasalary.calculator', metadata
assert int(b['versionCode'])>int(a['versionCode']), metadata
PY
"$adb" -s "$serial" install -r "$old_apk"
"$adb" -s "$serial" install -r "$test_apk"
run_probe() {
    local result
    result=$("$adb" -s "$serial" shell am instrument -w -r -e class com.koreasalary.calculator.ApkUpgradeProbe -e upgradeProbe "$1" com.koreasalary.calculator.test/androidx.test.runner.AndroidJUnitRunner)
    printf '%s\n' "$result"
    [[ "$result" == *'OK (1 test)'* ]]
}
run_probe seed
"$adb" -s "$serial" shell dumpsys package com.koreasalary.calculator | rg 'versionCode=|versionName='
"$adb" -s "$serial" install -r "$new_apk"
"$adb" -s "$serial" shell dumpsys package com.koreasalary.calculator | rg 'versionCode=|versionName='
run_probe verify
