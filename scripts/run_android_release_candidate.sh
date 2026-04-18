#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh
# shellcheck source=check_release_signing.sh
source "$ROOT/scripts/check_release_signing.sh"

resolve_bundletool_cmd() {
  if command -v bundletool >/dev/null 2>&1; then
    BUNDLETOOL_CMD=(bundletool)
    return 0
  fi

  if [[ -n "${BUNDLETOOL_JAR:-}" && -f "${BUNDLETOOL_JAR:-}" ]]; then
    BUNDLETOOL_CMD=(java -jar "$BUNDLETOOL_JAR")
    return 0
  fi

  echo "bundletool is required. Install it or set BUNDLETOOL_JAR." >&2
  return 1
}

select_release_device() {
  local serial
  serial="$("$ANDROID_HOME/platform-tools/adb" devices | awk 'NR>1 && $2 == "device" { print $1; exit }')"
  if [[ -z "$serial" ]]; then
    echo "No connected adb device/emulator for bundletool install." >&2
    return 1
  fi
  printf '%s\n' "$serial"
}

check_release_signing
resolve_bundletool_cmd

STORE_FILE="$(resolve_release_signing_prop RELEASE_STORE_FILE)"
STORE_PASSWORD="$(resolve_release_signing_prop RELEASE_STORE_PASSWORD)"
KEY_ALIAS="$(resolve_release_signing_prop RELEASE_KEY_ALIAS)"
KEY_PASSWORD="$(resolve_release_signing_prop RELEASE_KEY_PASSWORD)"

cd "$ROOT"

"$ROOT/scripts/run_android_feature_gate.sh"
./gradlew :app:bundleRelease

AAB_PATH="$ROOT/app/build/outputs/bundle/release/app-release.aab"
APKS_PATH="$ROOT/app/build/outputs/bundle/release/app-release.apks"

if [[ ! -f "$AAB_PATH" ]]; then
  echo "Missing release bundle: $AAB_PATH" >&2
  exit 1
fi

jarsigner -verify "$AAB_PATH"

"${BUNDLETOOL_CMD[@]}" build-apks \
  --bundle="$AAB_PATH" \
  --output="$APKS_PATH" \
  --ks="$STORE_FILE" \
  --ks-pass="pass:$STORE_PASSWORD" \
  --ks-key-alias="$KEY_ALIAS" \
  --key-pass="pass:$KEY_PASSWORD" \
  --overwrite

DEVICE_SERIAL="${TERMEX_RELEASE_DEVICE_SERIAL:-$(select_release_device)}"

"${BUNDLETOOL_CMD[@]}" install-apks \
  --apks="$APKS_PATH" \
  --device-id="$DEVICE_SERIAL"
