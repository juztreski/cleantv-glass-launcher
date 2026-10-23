#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
DEVICE_SERIAL="${DEVICE_SERIAL:-192.168.100.17:5555}"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
DISABLED="$ROOT/backup/disabled-packages.txt"

for file in getprop.txt packages.txt settings-global.txt settings-system.txt settings-secure.txt dumpsys-package.txt default-home.txt; do
    if [[ ! -s "$ROOT/backup/$file" ]]; then
        echo "Required pre-change backup missing: backup/$file" >&2
        exit 1
    fi
done

"$ROOT/gradlew" -p "$ROOT" assembleDebug
"$ADB" -s "$DEVICE_SERIAL" wait-for-device
"$ADB" -s "$DEVICE_SERIAL" install -r "$APK"

for permission in android.permission.READ_TV_LISTINGS android.permission.ACCESS_COARSE_LOCATION \
    android.permission.ACCESS_FINE_LOCATION; do
    if "$ADB" -s "$DEVICE_SERIAL" shell pm grant io.tvhome.launcher "$permission"; then
        echo "Granted $permission"
    else
        echo "WARNING: TV firmware refused $permission; related features may be unavailable." >&2
    fi
done

"$ADB" -s "$DEVICE_SERIAL" shell cmd package set-home-activity --user 0 io.tvhome.launcher/.MainActivity
"$ADB" -s "$DEVICE_SERIAL" shell cmd role add-role-holder --user 0 android.app.role.HOME io.tvhome.launcher
original_home="$(grep -E '^[[:alnum:]_.]+/[[:alnum:]_.$]+' "$ROOT/backup/default-home.txt" | tail -n 1)"
original_package="${original_home%%/*}"
if [[ -n "$original_package" && "$original_package" != "io.tvhome.launcher" ]] \
    && ! "$ADB" -s "$DEVICE_SERIAL" shell pm list packages -d "$original_package" </dev/null \
        | grep -Fxq "package:$original_package"; then
    "$ADB" -s "$DEVICE_SERIAL" shell pm disable-user --user 0 "$original_package" </dev/null
    if ! grep -Fxq "$original_package" "$DISABLED"; then
        printf '%s\n' "$original_package" >> "$DISABLED"
    fi
fi
if ! "$ADB" -s "$DEVICE_SERIAL" shell cmd package resolve-activity --user 0 --brief \
    -a android.intent.action.MAIN -c android.intent.category.HOME | grep -q 'io.tvhome.launcher/'; then
    echo "TV did not select Glass TV as the HOME activity." >&2
    exit 1
fi
"$ADB" -s "$DEVICE_SERIAL" shell am start -n io.tvhome.launcher/.MainActivity
echo "Installed and selected Glass TV as HOME on $DEVICE_SERIAL."
