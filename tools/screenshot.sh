#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
DEVICE_SERIAL="${DEVICE_SERIAL:-192.168.100.17:5555}"
OUTPUT="${1:-$ROOT/screenshots/home-$(date +%Y%m%d-%H%M%S).png}"
REMOTE="/sdcard/glass-tv-screenshot-$$.png"

mkdir -p "$(dirname "$OUTPUT")"
"$ADB" -s "$DEVICE_SERIAL" shell screencap -p "$REMOTE" >/dev/null
"$ADB" -s "$DEVICE_SERIAL" pull "$REMOTE" "$OUTPUT" >/dev/null
"$ADB" -s "$DEVICE_SERIAL" shell rm -f "$REMOTE"
if [[ ! -s "$OUTPUT" ]] || ! file "$OUTPUT" | grep -q 'PNG image data'; then
    rm -f "$OUTPUT"
    echo "ADB did not return a valid PNG. Disable tuner/preview (debug_preview=false) and retry." >&2
    exit 1
fi
file "$OUTPUT"
echo "Saved screenshot: $OUTPUT"
