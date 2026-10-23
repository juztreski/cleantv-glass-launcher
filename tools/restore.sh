#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
DEVICE_SERIAL="${DEVICE_SERIAL:-192.168.100.17:5555}"
DISABLED="$ROOT/backup/disabled-packages.txt"
HOME_FILE="$ROOT/backup/default-home.txt"

if [[ ! -s "$HOME_FILE" ]]; then
    echo "Original HOME backup missing: backup/default-home.txt" >&2
    exit 1
fi

if [[ -s "$DISABLED" ]]; then
    while IFS= read -r package; do
        [[ -z "$package" || "$package" == \#* ]] && continue
        "$ADB" -s "$DEVICE_SERIAL" shell pm enable --user 0 "$package" </dev/null
    done < "$DISABLED"
fi

for permission in android.permission.READ_TV_LISTINGS android.permission.ACCESS_COARSE_LOCATION \
    android.permission.ACCESS_FINE_LOCATION; do
    if "$ADB" -s "$DEVICE_SERIAL" shell pm revoke io.tvhome.launcher "$permission"; then
        echo "Revoked $permission from Glass TV"
    else
        echo "WARNING: Could not revoke $permission from Glass TV." >&2
    fi
done

original_home="$(grep -E '^[[:alnum:]_.]+/[[:alnum:]_.$]+' "$HOME_FILE" | tail -n 1)"
if [[ -z "$original_home" ]]; then
    echo "Could not parse original HOME component from backup/default-home.txt" >&2
    exit 1
fi
"$ADB" -s "$DEVICE_SERIAL" shell cmd package set-home-activity --user 0 "$original_home"
original_package="${original_home%%/*}"
"$ADB" -s "$DEVICE_SERIAL" shell cmd role add-role-holder --user 0 android.app.role.HOME "$original_package"
"$ADB" -s "$DEVICE_SERIAL" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
echo "Restored packages and HOME to $original_home."
