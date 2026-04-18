#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh >/dev/null 2>&1

SERIAL="${ANDROID_SERIAL:-emulator-5554}"
TEST_CLASS="com.termex.app.PlayStoreScreenshotCaptureTest"
OUT_DIR="${1:-$ROOT_DIR/playstore/v1/assets/phone}"

mkdir -p "$OUT_DIR"
rm -f "$OUT_DIR"/*.png

cleanup() {
  "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
}
trap cleanup EXIT

"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" wait-for-device
for _ in {1..120}; do
  if "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | grep -q 1; then
    break
  fi
  sleep 2
done
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell settings put global window_animation_scale 0 >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell settings put global transition_animation_scale 0 >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell settings put global animator_duration_scale 0 >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell settings put global sysui_demo_allowed 1 >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900 >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide >/dev/null
"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell rm -rf "/sdcard/Download/termex-playstore" >/dev/null 2>&1 || true

ANDROID_SERIAL="$SERIAL" ./gradlew :app:connectedReleaseProofAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS"

"$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" pull \
  "/sdcard/Download/termex-playstore/." \
  "$OUT_DIR" >/dev/null
