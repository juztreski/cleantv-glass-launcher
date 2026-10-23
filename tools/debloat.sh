#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
DEVICE_SERIAL="${DEVICE_SERIAL:-192.168.100.17:5555}"
CANDIDATES="$ROOT/tools/debloat-candidates.txt"
DISABLED="$ROOT/backup/disabled-packages.txt"

if [[ ! -s "$ROOT/backup/default-home.txt" ]]; then
    echo "Pre-change backup missing; refusing to disable packages." >&2
    exit 1
fi
if ! "$ADB" -s "$DEVICE_SERIAL" shell dumpsys package preferred-activities \
    | grep -q 'io.tvhome.launcher/.MainActivity'; then
    echo "Glass TV is not the saved HOME preference. Install/select it before debloating." >&2
    exit 1
fi

touch "$DISABLED"
while IFS= read -r package; do
    [[ -z "$package" || "$package" == \#* ]] && continue
    if ! "$ADB" -s "$DEVICE_SERIAL" shell pm list packages "$package" </dev/null | grep -Fxq "package:$package"; then
        echo "Skipping absent package: $package"
        continue
    fi
    if "$ADB" -s "$DEVICE_SERIAL" shell pm list packages -e "$package" </dev/null | grep -Fxq "package:$package"; then
        "$ADB" -s "$DEVICE_SERIAL" shell pm disable-user --user 0 "$package" </dev/null
        if ! grep -Fxq "$package" "$DISABLED"; then
            printf '%s\n' "$package" >> "$DISABLED"
        fi
    else
        echo "Already disabled; no restore entry added: $package"
    fi
done < "$CANDIDATES"

echo "Packages disabled by this script are recorded in backup/disabled-packages.txt."
