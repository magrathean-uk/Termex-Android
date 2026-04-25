#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=live_vm_harness.sh
source "$SCRIPT_DIR/live_vm_harness.sh"

FIXTURE_ROOT="$(live_vm_fixture_root)"
LOGIN_JSON="$(live_vm_login_json "$FIXTURE_ROOT")"
VM_NAME="${TERMEX_VM_NAME:-debian}"
TEST_VARIANT="${TERMEX_ANDROID_TEST_VARIANT:-releaseProof}"
TEST_VARIANT_PASCAL="$(capitalize_ascii "$TEST_VARIANT")"
APP_APK="$ROOT/app/build/outputs/apk/$TEST_VARIANT/app-$TEST_VARIANT.apk"
TEST_APK="$ROOT/app/build/outputs/apk/androidTest/$TEST_VARIANT/app-$TEST_VARIANT-androidTest.apk"
EMULATOR_PORT="${TERMEX_ANDROID_EMULATOR_PORT:-5554}"
SERIAL="emulator-${EMULATOR_PORT}"
GRADLE_MANAGED_AVD_HOME="${ANDROID_AVD_HOME}/gradle-managed"
AVD_NAME="$(android_resolve_managed_avd_name "$GRADLE_MANAGED_AVD_HOME" "${TERMEX_ANDROID_AVD_NAME:-}")"
APP_ID_SUFFIX="$(android_variant_app_id_suffix "$TEST_VARIANT")"
APP_ID="com.termex.app${APP_ID_SUFFIX}"
TEST_APP_ID="${APP_ID}.test"
RUNNER_ID="com.termex.app.testing.TermexHiltTestRunner"
OUTPUT_VIDEO="${TERMEX_OUTPUT_VIDEO:-/Users/bolyki/Desktop/Termex-a/videos/foreground-service-special-use.mp4}"
KNOWN_HOSTS_FILE="${TMPDIR:-/tmp}/termex-fgs-demo-known-hosts"
EMULATOR_LOG="${TMPDIR:-/tmp}/termex-fgs-demo-emulator.log"
INSTRUMENTATION_LOG="${TMPDIR:-/tmp}/termex-fgs-demo-instrumentation.log"
SCREEN_DIR="${TMPDIR:-/tmp}/termex-fgs-demo-screens"
CONCAT_LIST="${TMPDIR:-/tmp}/termex-fgs-demo-concat.txt"
ADB_TIMEOUT_SECONDS="${TERMEX_ADB_TIMEOUT_SECONDS:-60}"
ADB_CLEANUP_TIMEOUT_SECONDS="${TERMEX_ADB_CLEANUP_TIMEOUT_SECONDS:-10}"
INSTRUMENT_TIMEOUT_SECONDS="${TERMEX_INSTRUMENT_TIMEOUT_SECONDS:-420}"

TIMEOUT_BIN="$(command -v timeout || true)"
if [[ -z "$TIMEOUT_BIN" ]]; then
  echo "timeout command is required for bounded emulator smoke runs" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT_VIDEO")"
: > "$KNOWN_HOSTS_FILE"
: > "$INSTRUMENTATION_LOG"
rm -rf "$SCREEN_DIR"
mkdir -p "$SCREEN_DIR"
rm -f "$CONCAT_LIST"

eval "$(
  python3 - "$LOGIN_JSON" "$VM_NAME" <<'PY'
import base64
import json
import pathlib
import shlex
import sys

login_json = pathlib.Path(sys.argv[1])
vm_name = sys.argv[2]

with login_json.open(encoding="utf-8") as f:
    matrix = json.load(f)

item = matrix[vm_name]
root = item["password_logins"][0]
key = item["cert_logins"][1]

fixture = {
    "vmName": vm_name,
    "distro": item["distro"],
    "targetHostMode": "adb_reverse_loopback",
    "liveKey": {
        "host": "127.0.0.1" if item["host"] in {"127.0.0.1", "localhost"} else item["host"],
        "port": item["port"],
        "username": key["username"],
        "serverName": f"{item['name']} key",
        "connectionLabel": f"{key['username']}@{('127.0.0.1' if item['host'] in {'127.0.0.1', 'localhost'} else item['host'])}:{item['port']}",
        "keyName": f"{vm_name}-user3",
        "keyText": pathlib.Path(key["key"]).read_text(encoding="utf-8"),
    },
    "password": {
        "host": "127.0.0.1" if item["host"] in {"127.0.0.1", "localhost"} else item["host"],
        "port": item["port"],
        "username": item["password_logins"][1]["username"],
        "password": item["password_logins"][1]["password"],
        "serverName": f"{item['name']} password",
    },
}

values = {
    "TERMEX_FIXTURE_BASE64": base64.b64encode(json.dumps(fixture).encode("utf-8")).decode("ascii"),
    "TERMEX_VM_ROOT_PASSWORD": root["password"],
    "TERMEX_VM_KEY_PATH": key["key"],
    "TERMEX_VM_KEY_USERNAME": key["username"],
    "TERMEX_VM_HOST": item["host"],
    "TERMEX_VM_PORT": str(item["port"]),
}

for name, value in values.items():
    print(f"export {name}={shlex.quote(value)}")
PY
)"

stage() {
  printf '\n=== %s ===\n' "$*"
}

fail_with_emulator_log() {
  echo "$*" >&2
  if [[ -s "$EMULATOR_LOG" ]]; then
    echo "emulator log tail:" >&2
    tail -n 120 "$EMULATOR_LOG" >&2 || true
  fi
  exit 1
}

adb_cmd() {
  "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

adb_timeout() {
  "$TIMEOUT_BIN" "$ADB_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

adb_cleanup_timeout() {
  "$TIMEOUT_BIN" "$ADB_CLEANUP_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

root_ssh() {
  sshpass -p "$TERMEX_VM_ROOT_PASSWORD" ssh \
    -o ConnectTimeout=20 \
    -o ConnectionAttempts=1 \
    -o PubkeyAuthentication=no \
    -o PreferredAuthentications=password \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile="$KNOWN_HOSTS_FILE" \
    -p "$TERMEX_VM_PORT" \
    "root@$TERMEX_VM_HOST" \
    "$@"
}

cleanup() {
  set +e
  adb_cleanup_timeout reverse --remove "tcp:$TERMEX_VM_PORT" >/dev/null 2>&1 || true
  adb_cleanup_timeout emu kill >/dev/null 2>&1 || true
  if [[ -n "${EMU_PID:-}" ]] && kill -0 "$EMU_PID" >/dev/null 2>&1; then
    wait "$EMU_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

cd "$ROOT"
stage "build release-proof app and tests"
./gradlew ":app:assemble${TEST_VARIANT_PASCAL}" ":app:assemble${TEST_VARIANT_PASCAL}AndroidTest"

stage "prepare VM SSH key"
public_key="$(ssh-keygen -y -f "$TERMEX_VM_KEY_PATH")"
public_key_b64="$(printf '%s' "$public_key" | base64)"
root_ssh \
  "user='$TERMEX_VM_KEY_USERNAME'; key=\"\$(printf '%s' '$public_key_b64' | base64 -d)\"; home=\"\$(getent passwd \"\$user\" | cut -d: -f6)\"; install -d -m 700 -o \"\$user\" -g \"\$user\" \"\$home/.ssh\"; touch \"\$home/.ssh/authorized_keys\"; chown \"\$user:\$user\" \"\$home/.ssh/authorized_keys\"; chmod 600 \"\$home/.ssh/authorized_keys\"; grep -qxF \"\$key\" \"\$home/.ssh/authorized_keys\" || printf '%s\n' \"\$key\" >> \"\$home/.ssh/authorized_keys\""

stage "start emulator $SERIAL"
adb_cleanup_timeout emu kill >/dev/null 2>&1 || true
ANDROID_AVD_HOME="$GRADLE_MANAGED_AVD_HOME" \
"$ANDROID_HOME/emulator/emulator" \
  @"$AVD_NAME" \
  -port "$EMULATOR_PORT" \
  -read-only \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -gpu swiftshader_indirect \
  -no-snapshot-save \
  >"$EMULATOR_LOG" 2>&1 &
EMU_PID=$!

device_ready="0"
for _ in $(seq 1 180); do
  if ! kill -0 "$EMU_PID" >/dev/null 2>&1; then
    wait "$EMU_PID" >/dev/null 2>&1 || true
    fail_with_emulator_log "emulator exited before adb detected it: $SERIAL ($AVD_NAME)"
  fi
  if adb_cmd get-state >/dev/null 2>&1; then
    device_ready="1"
    break
  fi
  sleep 2
done

if [[ "$device_ready" != "1" ]]; then
  fail_with_emulator_log "adb failed to detect emulator: $SERIAL"
fi

boot_ok="0"
for _ in $(seq 1 180); do
  booted="$(adb_cmd shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [[ "$booted" == "1" ]]; then
    boot_ok="1"
    break
  fi
  sleep 2
done

if [[ "$boot_ok" != "1" ]]; then
  fail_with_emulator_log "emulator failed to boot: $SERIAL"
fi

stage "install app"
adb_timeout shell settings put global window_animation_scale 0
adb_timeout shell settings put global transition_animation_scale 0
adb_timeout shell settings put global animator_duration_scale 0
adb_timeout install -r "$APP_APK" >/dev/null
adb_timeout install -r "$TEST_APK" >/dev/null
adb_timeout reverse "tcp:$TERMEX_VM_PORT" "tcp:$TERMEX_VM_PORT"
adb_timeout shell pm clear "$APP_ID" >/dev/null
adb_timeout shell pm clear "$TEST_APP_ID" >/dev/null || true
adb_timeout shell rm -rf "/sdcard/Android/data/$APP_ID/files/fgs-demo-screens" >/dev/null 2>&1 || true

stage "run foreground-service flow"
"$TIMEOUT_BIN" "$INSTRUMENT_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am instrument -w -r \
  -e clearPackageData true \
  -e class com.termex.app.ForegroundServiceDemoTest#recordsSpecialUseForegroundServiceFlow \
  -e termexFixtureBase64 "$TERMEX_FIXTURE_BASE64" \
  "${TEST_APP_ID}/${RUNNER_ID}" | tee "$INSTRUMENTATION_LOG"

if grep -Eq "INSTRUMENTATION_STATUS_CODE: -2|INSTRUMENTATION_RESULT: shortMsg=|FAILURES!!!|Process crashed" "$INSTRUMENTATION_LOG"; then
  echo "foreground-service instrumentation failed; see $INSTRUMENTATION_LOG" >&2
  exit 1
fi

if ! grep -Eq "OK \\([0-9]+ tests?\\)" "$INSTRUMENTATION_LOG"; then
  echo "foreground-service instrumentation did not report success; see $INSTRUMENTATION_LOG" >&2
  exit 1
fi

stage "pull screenshots"
adb_timeout pull "/sdcard/Android/data/$APP_ID/files/fgs-demo-screens" "$SCREEN_DIR" >/dev/null

python3 - "$SCREEN_DIR/fgs-demo-screens/01-connected.png" "$SCREEN_DIR/fgs-demo-screens/02-notification.png" "$SCREEN_DIR/fgs-demo-screens/03-home-notification.png" <<'PY'
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import sys

source = Path(sys.argv[1])
slide_two = Path(sys.argv[2])
slide_three = Path(sys.argv[3])

img = Image.open(source).convert("RGB")
canvas_size = img.size
font_title = ImageFont.load_default()
font_body = ImageFont.load_default()

def make_slide(title: str, body: str, out_path: Path) -> None:
    canvas = Image.new("RGB", canvas_size, "#0a1220")
    draw = ImageDraw.Draw(canvas)
    inset = img.resize((int(canvas_size[0] * 0.72), int(canvas_size[1] * 0.42)))
    inset_x = (canvas_size[0] - inset.size[0]) // 2
    inset_y = 220
    canvas.paste(inset, (inset_x, inset_y))
    draw.rounded_rectangle(
        (70, 1180, canvas_size[0] - 70, canvas_size[1] - 180),
        radius=32,
        fill="#111c2d"
    )
    draw.text((110, 1230), title, fill="white", font=font_title)
    draw.multiline_text((110, 1290), body, fill="#d7e3f5", font=font_body, spacing=16)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out_path, format="PNG")

make_slide(
    "Foreground notification stays visible",
    "Termex starts the foreground service only while the user has an active SSH session or port forward.\nThe ongoing notification shows the active connection count.",
    slide_two,
)

make_slide(
    "Session stays alive in background",
    "When the user backgrounds Termex, the active remote shell stays alive until the user returns and disconnects.\nThis protects long-running SSH work the user started.",
    slide_three,
)
PY

cat >"$CONCAT_LIST" <<EOF
file '$SCREEN_DIR/fgs-demo-screens/01-connected.png'
duration 3
file '$SCREEN_DIR/fgs-demo-screens/02-notification.png'
duration 3
file '$SCREEN_DIR/fgs-demo-screens/03-home-notification.png'
duration 4
file '$SCREEN_DIR/fgs-demo-screens/03-home-notification.png'
EOF

stage "render video"
ffmpeg -y -f concat -safe 0 -i "$CONCAT_LIST" -vf "format=yuv420p" -c:v libx264 -pix_fmt yuv420p "$OUTPUT_VIDEO" >/dev/null 2>&1
stage "wrote $OUTPUT_VIDEO"
