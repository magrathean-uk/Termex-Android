#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=live_vm_harness.sh
source "$SCRIPT_DIR/live_vm_harness.sh"

FIXTURE_ROOT="$(live_vm_fixture_root)"
LOGIN_JSON="$(live_vm_login_json "$FIXTURE_ROOT")"
SUMMARY_MD="$(live_vm_summary_md "$FIXTURE_ROOT")"
VM_NAME="${TERMEX_VM_NAME:?TERMEX_VM_NAME is required}"
TEST_CLASS_ARG="${TERMEX_UI_TEST_CLASS:-com.termex.app.RealSshFlowTest}"
TEST_PLAN="${TERMEX_UI_TEST_PLAN:-${TERMEX_UI_TEST_SET:-full}}"
TEST_VARIANT="${TERMEX_ANDROID_TEST_VARIANT:-releaseProof}"
TEST_VARIANT_PASCAL="$(capitalize_ascii "$TEST_VARIANT")"
PREPARE_PLAIN_KEYS="${TERMEX_VM_PREPARE_PLAIN_KEYS:-1}"
PREPARE_TMUX="${TERMEX_VM_PREPARE_TMUX:-1}"
RESET_SSH_BASELINE="${TERMEX_VM_RESET_SSH_BASELINE:-1}"
ROTATE_HOST_KEYS="${TERMEX_VM_ROTATE_HOST_KEYS:-0}"
RESTART_SSHD="${TERMEX_VM_RESTART_SSHD:-1}"
HOST_TRANSPORT="${TERMEX_ANDROID_MANAGED_HOST_TRANSPORT:-adb_reverse}"
EMULATOR_PORT="${TERMEX_ANDROID_EMULATOR_PORT:-5654}"
SERIAL="emulator-${EMULATOR_PORT}"
APP_APK="${TERMEX_ANDROID_APP_APK:-$ROOT/app/build/outputs/apk/$TEST_VARIANT/app-$TEST_VARIANT.apk}"
TEST_APK="${TERMEX_ANDROID_TEST_APK:-$ROOT/app/build/outputs/apk/androidTest/$TEST_VARIANT/app-$TEST_VARIANT-androidTest.apk}"
WORK_ROOT="${TERMEX_ANDROID_WORK_ROOT:-/tmp/termex-android-vm-workers}"
WORK_DIR="$WORK_ROOT/$VM_NAME"
RESULT_DIR="$WORK_DIR/result_bundle"
WRAPPER_LOG="$WORK_DIR/wrapper.log"
EMULATOR_LOG="$WORK_DIR/emulator.log"
INSTRUMENTATION_LOG="$WORK_DIR/instrumentation.log"
EXIT_STATUS_FILE="$RESULT_DIR/exit_status.txt"
SUMMARY_FILE="$RESULT_DIR/summary.md"
FAILURE_SCREENSHOT="$WORK_DIR/failure_screenshot.png"
FAILURE_LOGCAT="$WORK_DIR/failure_logcat.txt"
KNOWN_HOSTS_FILE="$WORK_DIR/known_hosts"
GRADLE_MANAGED_AVD_HOME="${ANDROID_AVD_HOME}/gradle-managed"
AVD_NAME="$(android_resolve_managed_avd_name "$GRADLE_MANAGED_AVD_HOME" "${TERMEX_ANDROID_AVD_NAME:-}")"
APP_ID_SUFFIX="$(android_variant_app_id_suffix "$TEST_VARIANT")"
APP_ID="${TERMEX_ANDROID_APP_ID:-com.termex.app${APP_ID_SUFFIX}}"
TEST_APP_ID="${TERMEX_ANDROID_TEST_APP_ID:-${APP_ID}.test}"
RUNNER_ID="${TERMEX_ANDROID_TEST_RUNNER:-com.termex.app.testing.TermexHiltTestRunner}"
DETAILS_FILE="$WORK_DIR/details.md"
REMOTE_FORWARD_HELPER_LOG="$WORK_DIR/remote_forward_host.log"
REMOTE_FORWARD_HELPER_PORT_FILE="$WORK_DIR/remote_forward_host_port.txt"
REMOTE_FORWARD_TARGET_HOST=""
REMOTE_FORWARD_TARGET_PORT=""
REMOTE_FORWARD_HELPER_PID=""
REMOTE_FORWARD_REVERSE_PORT=""
NEEDS_SSHD_RESTART=0
PRESERVE_APP_DATA=0
ADB_TIMEOUT_SECONDS="${TERMEX_ADB_TIMEOUT_SECONDS:-60}"
ADB_CLEANUP_TIMEOUT_SECONDS="${TERMEX_ADB_CLEANUP_TIMEOUT_SECONDS:-10}"
INSTRUMENT_TIMEOUT_SECONDS="${TERMEX_INSTRUMENT_TIMEOUT_SECONDS:-420}"
TIMEOUT_BIN="$(command -v timeout || true)"

if [[ -z "$TIMEOUT_BIN" ]]; then
  echo "timeout command is required for bounded emulator smoke runs" >&2
  exit 1
fi

mkdir -p "$WORK_DIR"
rm -rf "$RESULT_DIR"
mkdir -p "$RESULT_DIR"
: > "$WRAPPER_LOG"
: > "$EMULATOR_LOG"
: > "$INSTRUMENTATION_LOG"
: > "$KNOWN_HOSTS_FILE"
rm -f "$REMOTE_FORWARD_HELPER_PORT_FILE"

exec > >(tee -a "$WRAPPER_LOG") 2>&1

adb_cmd() {
  "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

adb_timeout() {
  "$TIMEOUT_BIN" "$ADB_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

adb_cleanup_timeout() {
  "$TIMEOUT_BIN" "$ADB_CLEANUP_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" "$@"
}

bundle_artifacts() {
  local path
  for path in "$WRAPPER_LOG" "$EMULATOR_LOG" "$INSTRUMENTATION_LOG" "$DETAILS_FILE" "$FAILURE_SCREENSHOT" "$FAILURE_LOGCAT" "$REMOTE_FORWARD_HELPER_LOG" "$REMOTE_FORWARD_HELPER_PORT_FILE"; do
    if [[ -f "$path" ]]; then
      cp -f "$path" "$RESULT_DIR/$(basename "$path")"
    fi
  done
}

write_summary() {
  local exit_code="$1"
  local outcome="passed"
  if [[ "$exit_code" != "0" ]]; then
    outcome="failed"
  fi

  cat >"$SUMMARY_FILE" <<EOF
# Termex Android VM Run

- VM: $VM_NAME
- Distro: ${TERMEX_VM_DISTRO:-unknown}
- Outcome: $outcome
- Exit status: $exit_code
- Test plan: $TEST_PLAN
- Test class arg: $TEST_CLASS_ARG
- Emulator port: $EMULATOR_PORT
- Serial: $SERIAL
- Host transport: $HOST_TRANSPORT
- Fixture summary: $SUMMARY_MD

## Artifacts
- wrapper.log
- emulator.log
- instrumentation.log
- exit_status.txt
- details.md
EOF

  if [[ "$exit_code" != "0" ]]; then
    cat >>"$SUMMARY_FILE" <<EOF
- failure_screenshot.png
- failure_logcat.txt
EOF
  fi
}

start_remote_forward_helper() {
  : > "$REMOTE_FORWARD_HELPER_LOG"
  python3 - "$REMOTE_FORWARD_HELPER_LOG" "$REMOTE_FORWARD_HELPER_PORT_FILE" <<'PY' &
import pathlib
import socket
import sys

log_path = pathlib.Path(sys.argv[1])
port_path = pathlib.Path(sys.argv[2])

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", 0))
    server.listen(16)
    port = server.getsockname()[1]
    port_path.write_text(str(port), encoding="utf-8")
    with log_path.open("a", encoding="utf-8") as log:
        log.write(f"listening {port}\n")
        log.flush()
        while True:
            client, addr = server.accept()
            with client:
                log.write(f"client {addr[0]}:{addr[1]}\n")
                log.flush()
                client.sendall(b"REMOTE-FORWARD-OK\n")
PY
  REMOTE_FORWARD_HELPER_PID=$!

  for _ in $(seq 1 50); do
    if [[ -s "$REMOTE_FORWARD_HELPER_PORT_FILE" ]]; then
      REMOTE_FORWARD_TARGET_PORT="$(cat "$REMOTE_FORWARD_HELPER_PORT_FILE")"
      if [[ "$HOST_TRANSPORT" == "adb_reverse" ]]; then
        adb_timeout reverse \
          "tcp:$REMOTE_FORWARD_TARGET_PORT" \
          "tcp:$REMOTE_FORWARD_TARGET_PORT" >/dev/null
        REMOTE_FORWARD_REVERSE_PORT="$REMOTE_FORWARD_TARGET_PORT"
        REMOTE_FORWARD_TARGET_HOST="127.0.0.1"
      else
        REMOTE_FORWARD_TARGET_HOST="10.0.2.2"
      fi
      return 0
    fi
    sleep 0.1
  done

  echo "remote forward helper failed to start" >&2
  exit 1
}

capture_failure_artifacts() {
  local exit_code="$1"

  if [[ "$exit_code" == "0" ]]; then
    return 0
  fi

  if adb_cleanup_timeout get-state >/dev/null 2>&1; then
    adb_timeout exec-out screencap -p >"$FAILURE_SCREENSHOT" 2>/dev/null || true
    adb_timeout logcat -d -v time 2>/dev/null | tail -n 600 >"$FAILURE_LOGCAT" || true
  fi

  if [[ ! -s "$FAILURE_SCREENSHOT" ]]; then
    rm -f "$FAILURE_SCREENSHOT"
  fi

  if [[ ! -s "$FAILURE_LOGCAT" ]]; then
    rm -f "$FAILURE_LOGCAT"
  fi
}

on_exit() {
  local exit_code="$?"
  set +e
  printf '%s\n' "$exit_code" >"$EXIT_STATUS_FILE"
  if [[ "$exit_code" != "0" ]]; then
    capture_failure_artifacts "$exit_code"
  fi
  bundle_artifacts
  write_summary "$exit_code"
  cleanup || true
  return "$exit_code"
}

trap on_exit EXIT

if [[ ! -f "$LOGIN_JSON" ]]; then
  echo "missing fixture file: $LOGIN_JSON" >&2
  exit 2
fi

if [[ ! -f "$SUMMARY_MD" ]]; then
  echo "missing fixture file: $SUMMARY_MD" >&2
  exit 2
fi

if [[ ! -f "$APP_APK" ]]; then
  echo "missing app apk: $APP_APK" >&2
  exit 2
fi

if [[ ! -f "$TEST_APK" ]]; then
  echo "missing test apk: $TEST_APK" >&2
  exit 2
fi

if [[ ! -f "$GRADLE_MANAGED_AVD_HOME/${AVD_NAME}.ini" ]]; then
  echo "missing avd ini: $GRADLE_MANAGED_AVD_HOME/${AVD_NAME}.ini" >&2
  exit 2
fi

if [[ ! -d "$GRADLE_MANAGED_AVD_HOME/${AVD_NAME}.avd" ]]; then
  echo "missing avd dir: $GRADLE_MANAGED_AVD_HOME/${AVD_NAME}.avd" >&2
  exit 2
fi

eval "$(
  python3 - "$LOGIN_JSON" "$VM_NAME" "$HOST_TRANSPORT" <<'PY'
import base64
import json
import pathlib
import shlex
import sys

login_json = pathlib.Path(sys.argv[1])
vm_name = sys.argv[2]
host_transport = sys.argv[3]

with login_json.open(encoding="utf-8") as f:
    matrix = json.load(f)

if vm_name not in matrix:
    raise SystemExit(f"missing VM in fixture bundle: {vm_name}")

item = matrix[vm_name]
password = item["password_logins"][1]
root = item["password_logins"][0]
cert = item["cert_logins"][0]
key = item["cert_logins"][1]

def read(path: str) -> str:
    return pathlib.Path(path).read_text(encoding="utf-8")

def device_host(host: str) -> str:
    if host_transport == "adb_reverse" and host in {"127.0.0.1", "localhost"}:
        return "127.0.0.1"
    return "10.0.2.2" if host in {"127.0.0.1", "localhost"} else host

target_host_mode = "adb_reverse_loopback" if host_transport == "adb_reverse" else "emulator_host_loopback"

fixture = {
    "vmName": vm_name,
    "distro": item["distro"],
    "targetHostMode": target_host_mode,
    "liveKey": {
        "host": device_host(item["host"]),
        "port": item["port"],
        "username": key["username"],
        "serverName": f"{item['name']} key",
        "keyName": f"{vm_name}-user3",
        "keyText": read(key["key"]),
    },
    "password": {
        "host": device_host(item["host"]),
        "port": item["port"],
        "username": password["username"],
        "password": password["password"],
        "serverName": f"{item['name']} password",
    },
    "certificate": {
        "host": device_host(item["host"]),
        "port": item["port"],
        "username": cert["username"],
        "serverName": f"{item['name']} cert",
        "keyName": f"{vm_name}-user2",
        "keyText": read(cert["key"]),
        "certificateName": f"{vm_name}-user2-cert",
        "certificateText": read(cert["cert"]),
    },
    "jump": {
        "host": device_host(item["host"]),
        "port": item["port"],
        "username": key["username"],
        "serverName": f"{item['name']} jump",
        "keyName": f"{vm_name}-user3",
        "targetHost": "127.0.0.1",
        "targetPort": item["guest_ssh_port"],
    },
}

fixture_b64 = base64.b64encode(json.dumps(fixture).encode("utf-8")).decode("ascii")
values = {
    "TERMEX_FIXTURE_BASE64": fixture_b64,
    "TERMEX_VM_ROOT_PASSWORD": root["password"],
    "TERMEX_VM_KEY_PATH": key["key"],
    "TERMEX_VM_KEY_USERNAME": key["username"],
    "TERMEX_VM_HOST": item["host"],
    "TERMEX_VM_DISTRO": item["distro"],
    "TERMEX_VM_PORT": str(item["port"]),
}

for name, value in values.items():
    print(f"export {name}={shlex.quote(value)}")
PY
)"

reset_ssh_baseline() {
  : > "$KNOWN_HOSTS_FILE"
}

restart_sshd() {
  root_ssh 'set -eu
    systemctl daemon-reload || true
    systemctl restart ssh.service 2>/dev/null || true
    systemctl restart sshd.service 2>/dev/null || true
    service ssh restart 2>/dev/null || true
    service sshd restart 2>/dev/null || true
  '
}

ensure_sshd_forwarding_features() {
  root_ssh 'set -eu
    main_config=/etc/ssh/sshd_config
    conf_dir=/etc/ssh/sshd_config.d
    conf_file="$conf_dir/termex-forwarding.conf"

    install -d -m 755 "$conf_dir"
    if ! grep -Eq "^[[:space:]]*Include[[:space:]]+/etc/ssh/sshd_config.d/\\*\\.conf([[:space:]]|$)" "$main_config"; then
      printf "\nInclude /etc/ssh/sshd_config.d/*.conf\n" >> "$main_config"
    fi

    cat >"$conf_file" <<EOF
AllowTcpForwarding yes
AllowAgentForwarding yes
GatewayPorts yes
EOF
  '
  NEEDS_SSHD_RESTART=1
}

rotate_host_keys() {
  root_ssh 'set -eu
    backup_dir=/var/lib/termex-ssh-host-key-backup
    install -d -m 700 "$backup_dir"
    for path in /etc/ssh/ssh_host_*_key /etc/ssh/ssh_host_*_key.pub; do
      [ -e "$path" ] || continue
      cp -a "$path" "$backup_dir"/
      rm -f "$path"
    done
    ssh-keygen -A
  '
  NEEDS_SSHD_RESTART=1
}

write_result_bundle_details() {
  local test
  cat >"$DETAILS_FILE" <<EOF
# Termex Android VM Worker Details

- VM: $VM_NAME
- Fixture root: $FIXTURE_ROOT
- Login JSON: $LOGIN_JSON
- Summary source: $SUMMARY_MD
- Test plan: $TEST_PLAN
- Test variant: $TEST_VARIANT
- Host transport: $HOST_TRANSPORT
- Emulator port: $EMULATOR_PORT
- Serial: $SERIAL
- App APK: $APP_APK
- Test APK: $TEST_APK
- Reset SSH baseline: $RESET_SSH_BASELINE
- Rotate host keys: $ROTATE_HOST_KEYS
- Restart sshd: $RESTART_SSHD

## Tests
EOF
  for test in "${tests[@]}"; do
    printf -- '- %s\n' "$test" >>"$DETAILS_FILE"
  done
}

root_ssh() {
  sshpass -p "$TERMEX_VM_ROOT_PASSWORD" ssh \
    -o PubkeyAuthentication=no \
    -o PreferredAuthentications=password \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile="$KNOWN_HOSTS_FILE" \
    -p "$TERMEX_VM_PORT" \
    "root@$TERMEX_VM_HOST" \
    "$@"
}

if [[ "$PREPARE_PLAIN_KEYS" == "1" ]]; then
  public_key="$(ssh-keygen -y -f "$TERMEX_VM_KEY_PATH")"
  public_key_b64="$(printf '%s' "$public_key" | base64)"
  root_ssh \
    "user='$TERMEX_VM_KEY_USERNAME'; key=\"\$(printf '%s' '$public_key_b64' | base64 -d)\"; home=\"\$(getent passwd \"\$user\" | cut -d: -f6)\"; install -d -m 700 -o \"\$user\" -g \"\$user\" \"\$home/.ssh\"; touch \"\$home/.ssh/authorized_keys\"; chown \"\$user:\$user\" \"\$home/.ssh/authorized_keys\"; chmod 600 \"\$home/.ssh/authorized_keys\"; grep -qxF \"\$key\" \"\$home/.ssh/authorized_keys\" || printf '%s\n' \"\$key\" >> \"\$home/.ssh/authorized_keys\""
fi

if [[ "$PREPARE_TMUX" == "1" ]]; then
  root_ssh 'set -e
    valid_tmux() { command -v tmux >/dev/null 2>&1 && tmux -V 2>/dev/null | grep -Eq "^tmux[[:space:]]+"; }
    if command -v tmux >/dev/null 2>&1 && ! tmux -V 2>/dev/null | grep -Eq "^tmux[[:space:]]+"; then
      rm -f "$(command -v tmux)"
    fi
    if ! valid_tmux; then
      . /etc/os-release
      case "$ID" in
        debian|ubuntu)
          apt-get update -qq
          DEBIAN_FRONTEND=noninteractive apt-get install -y tmux
          ;;
        fedora|almalinux|rocky|rhel|centos)
          dnf install -y tmux
          ;;
        opensuse*|suse|sles)
          zypper --non-interactive refresh
          zypper --non-interactive install tmux
          ;;
        *)
          echo "unsupported distro for tmux install: $ID" >&2
          exit 2
          ;;
      esac
    fi
    valid_tmux'
fi

ensure_sshd_forwarding_features

if [[ "$RESET_SSH_BASELINE" == "1" ]]; then
  reset_ssh_baseline
fi

if [[ "$ROTATE_HOST_KEYS" == "1" ]]; then
  rotate_host_keys
fi

if [[ "$RESTART_SSHD" == "1" || "$NEEDS_SSHD_RESTART" == "1" ]]; then
  restart_sshd
fi

cleanup() {
  if [[ -n "${REMOTE_FORWARD_REVERSE_PORT:-}" ]]; then
    adb_cleanup_timeout reverse --remove "tcp:$REMOTE_FORWARD_REVERSE_PORT" >/dev/null 2>&1 || true
  fi
  if [[ -n "${REMOTE_FORWARD_HELPER_PID:-}" ]] && kill -0 "$REMOTE_FORWARD_HELPER_PID" >/dev/null 2>&1; then
    kill "$REMOTE_FORWARD_HELPER_PID" >/dev/null 2>&1 || true
    wait "$REMOTE_FORWARD_HELPER_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "${EMU_PID:-}" ]] && kill -0 "$EMU_PID" >/dev/null 2>&1; then
    adb_cleanup_timeout emu kill >/dev/null 2>&1 || true
    for _ in $(seq 1 15); do
      if ! kill -0 "$EMU_PID" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
    if kill -0 "$EMU_PID" >/dev/null 2>&1; then
      kill "$EMU_PID" >/dev/null 2>&1 || true
      sleep 1
    fi
    if kill -0 "$EMU_PID" >/dev/null 2>&1; then
      kill -9 "$EMU_PID" >/dev/null 2>&1 || true
    fi
    wait "$EMU_PID" >/dev/null 2>&1 || true
  fi
}

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
    echo "emulator exited before adb detected it: $SERIAL ($AVD_NAME)" >&2
    if [[ -s "$EMULATOR_LOG" ]]; then
      tail -n 120 "$EMULATOR_LOG" >&2 || true
    fi
    exit 1
  fi
  if adb_cmd get-state >/dev/null 2>&1; then
    device_ready="1"
    break
  fi
  sleep 2
done

if [[ "$device_ready" != "1" ]]; then
  echo "adb failed to detect emulator: $SERIAL" >&2
  exit 1
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
  echo "emulator failed to boot: $SERIAL" >&2
  exit 1
fi

adb_timeout shell settings put global window_animation_scale 0
adb_timeout shell settings put global transition_animation_scale 0
adb_timeout shell settings put global animator_duration_scale 0

adb_timeout install -r "$APP_APK" >/dev/null
adb_timeout install -r "$TEST_APK" >/dev/null

if [[ "$HOST_TRANSPORT" == "adb_reverse" ]]; then
  adb_timeout reverse "tcp:$TERMEX_VM_PORT" "tcp:$TERMEX_VM_PORT"
fi

: > "$INSTRUMENTATION_LOG"
failures=()
tests=()
PRESERVE_APP_DATA=0
while IFS= read -r line; do
  [[ -n "$line" ]] && tests+=("$line")
done < <(
  if [[ "$TEST_CLASS_ARG" == "com.termex.app.RealSshFlowTest" ]]; then
    live_vm_test_list_for_plan "$TEST_PLAN"
  else
    IFS=',' read -r -a split_tests <<< "$TEST_CLASS_ARG"
    printf '%s\n' "${split_tests[@]}"
  fi
)

write_result_bundle_details
if [[ "${TERMEX_REMOTE_FORWARD_HELPER:-1}" == "1" ]] && printf '%s\n' "${tests[@]}" | rg -q 'remoteForwardExposesServiceOnHost'; then
  start_remote_forward_helper
  cat >>"$DETAILS_FILE" <<EOF
- Remote forward helper host: $REMOTE_FORWARD_TARGET_HOST
- Remote forward helper port: $REMOTE_FORWARD_TARGET_PORT
EOF
fi

for test_name in "${tests[@]}"; do
  if [[ "$PRESERVE_APP_DATA" == "0" ]]; then
    adb_timeout shell pm clear "$APP_ID" >/dev/null
    adb_timeout shell pm clear "$TEST_APP_ID" >/dev/null || true
  elif [[ "${TERMEX_VM_PRESERVE_APP_PROCESS:-0}" != "1" ]]; then
    adb_timeout shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
    adb_timeout shell am force-stop "$TEST_APP_ID" >/dev/null 2>&1 || true
  fi
  printf '=== %s ===\n' "$test_name" | tee -a "$INSTRUMENTATION_LOG"
  test_log="$(mktemp)"
  clear_package_data_arg=true
  if [[ "$PRESERVE_APP_DATA" == "1" ]]; then
    clear_package_data_arg=false
  fi
  instrument_args=(
    -e clearPackageData "$clear_package_data_arg"
    -e class "$test_name"
    -e termexFixtureBase64 "$TERMEX_FIXTURE_BASE64"
  )
  if [[ -n "$REMOTE_FORWARD_TARGET_HOST" && -n "$REMOTE_FORWARD_TARGET_PORT" ]]; then
    instrument_args+=(
      -e termexRemoteForwardTargetHost "$REMOTE_FORWARD_TARGET_HOST"
      -e termexRemoteForwardTargetPort "$REMOTE_FORWARD_TARGET_PORT"
    )
  fi
  if ! "$TIMEOUT_BIN" "$INSTRUMENT_TIMEOUT_SECONDS" "$ANDROID_HOME/platform-tools/adb" -s "$SERIAL" shell am instrument -w -r \
    "${instrument_args[@]}" \
    "${TEST_APP_ID}/${RUNNER_ID}" | tee "$test_log" | tee -a "$INSTRUMENTATION_LOG"; then
    failures+=("$test_name")
  elif rg -q 'FAILURES!!!|INSTRUMENTATION_STATUS_CODE: -2|INSTRUMENTATION_CODE: 0' "$test_log"; then
    failures+=("$test_name")
  fi
  rm -f "$test_log"

  if [[ "$TEST_PLAN" == "changed-host" && "$test_name" == "com.termex.app.RealSshFlowTest#trustsLiveHostKeyBeforeRotation" ]]; then
    rotate_host_keys
    restart_sshd
    PRESERVE_APP_DATA=1
    TERMEX_VM_PRESERVE_APP_PROCESS=1
  fi
done

if (( ${#failures[@]} > 0 )); then
  printf 'failed tests: %s\n' "${failures[*]}" >&2
  exit 1
fi
