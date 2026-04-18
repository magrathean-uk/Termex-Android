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
VM_LIST="${TERMEX_VM_LIST:-debian fedora alma opensuse}"
TEST_PLAN="${TERMEX_UI_TEST_PLAN:-${TERMEX_UI_TEST_SET:-full}}"
TEST_VARIANT="${TERMEX_ANDROID_TEST_VARIANT:-releaseProof}"
TEST_VARIANT_PASCAL="$(capitalize_ascii "$TEST_VARIANT")"
EMULATOR_PORT_BASE="${TERMEX_ANDROID_EMULATOR_PORT_BASE:-5654}"
RUN_MODE="${TERMEX_ANDROID_VM_MODE:-serial}"
APP_APK="${TERMEX_ANDROID_APP_APK:-$ROOT/app/build/outputs/apk/$TEST_VARIANT/app-$TEST_VARIANT.apk}"
TEST_APK="${TERMEX_ANDROID_TEST_APK:-$ROOT/app/build/outputs/apk/androidTest/$TEST_VARIANT/app-$TEST_VARIANT-androidTest.apk}"
WORK_ROOT="${TERMEX_ANDROID_WORK_ROOT:-/tmp/termex-android-vm-workers}"

if [[ ! -f "$LOGIN_JSON" ]]; then
  echo "missing fixture file: $LOGIN_JSON" >&2
  exit 2
fi

if [[ ! -f "$SUMMARY_MD" ]]; then
  echo "missing fixture file: $SUMMARY_MD" >&2
  exit 2
fi

if [[ -n "${TERMEX_UI_TEST_CLASS:-}" && "$TEST_PLAN" == "full" ]]; then
  TEST_CLASS_ARG="$TERMEX_UI_TEST_CLASS"
else
  TESTS=()
  while IFS= read -r line; do
    [[ -n "$line" ]] && TESTS+=("$line")
  done < <(live_vm_test_list_for_plan "$TEST_PLAN")
  TEST_CLASS_ARG="$(IFS=,; printf '%s' "${TESTS[*]}")"
fi

cd "$ROOT"

./gradlew ":app:assemble${TEST_VARIANT_PASCAL}" ":app:assemble${TEST_VARIANT_PASCAL}AndroidTest"

case "$RUN_MODE" in
  serial|parallel)
    ;;
  *)
    echo "unknown TERMEX_ANDROID_VM_MODE=$RUN_MODE" >&2
    exit 2
    ;;
esac

run_vm() {
  local vm="$1"
  local emulator_port="$2"
  local vm_dir="$WORK_ROOT/$vm"
  local runner_log="$vm_dir/matrix.log"
  local worker_status=0

  mkdir -p "$vm_dir"
  : > "$runner_log"

  printf '=== Termex Android VM matrix: %s (port %s) ===\n' "$vm" "$emulator_port" | tee -a "$runner_log"

  if [[ "$RUN_MODE" == "parallel" ]]; then
    TERMEX_VM_FIXTURE_ROOT="$FIXTURE_ROOT" \
      TERMEX_VM_NAME="$vm" \
      TERMEX_UI_TEST_CLASS="$TEST_CLASS_ARG" \
      TERMEX_ANDROID_APP_APK="$APP_APK" \
      TERMEX_ANDROID_TEST_APK="$TEST_APK" \
      TERMEX_ANDROID_EMULATOR_PORT="$emulator_port" \
      TERMEX_ANDROID_WORK_ROOT="$WORK_ROOT" \
      "$ROOT/scripts/run_android_vm_ui_worker.sh" >>"$runner_log" 2>&1 || worker_status=$?
  else
    TERMEX_VM_FIXTURE_ROOT="$FIXTURE_ROOT" \
      TERMEX_VM_NAME="$vm" \
      TERMEX_UI_TEST_CLASS="$TEST_CLASS_ARG" \
      TERMEX_ANDROID_APP_APK="$APP_APK" \
      TERMEX_ANDROID_TEST_APK="$TEST_APK" \
      TERMEX_ANDROID_EMULATOR_PORT="$emulator_port" \
      TERMEX_ANDROID_WORK_ROOT="$WORK_ROOT" \
      "$ROOT/scripts/run_android_vm_ui_worker.sh" 2>&1 | tee -a "$runner_log" || worker_status=$?
  fi

  printf 'result bundle: %s\n' "$vm_dir/result_bundle" | tee -a "$runner_log"
  return "$worker_status"
}

failures=()
jobs=()
job_pids=()
statuses=()
index=0
for vm in $VM_LIST; do
  emulator_port=$((EMULATOR_PORT_BASE + (index * 2)))
  if [[ "$RUN_MODE" == "parallel" ]]; then
    run_vm "$vm" "$emulator_port" &
    jobs+=("$vm")
    job_pids+=("$!")
  else
    if run_vm "$vm" "$emulator_port"; then
      statuses+=("$vm=0")
    else
      statuses+=("$vm=1")
      failures+=("$vm")
    fi
  fi
  index=$((index + 1))
done

if [[ "$RUN_MODE" == "parallel" ]]; then
  for idx in "${!job_pids[@]}"; do
    if wait "${job_pids[$idx]}"; then
      statuses+=("${jobs[$idx]}=0")
      printf 'vm completed: %s\n' "${jobs[$idx]}"
      printf 'result bundle: %s\n' "$WORK_ROOT/${jobs[$idx]}/result_bundle"
    else
      statuses+=("${jobs[$idx]}=1")
      failures+=("${jobs[$idx]}")
      printf 'vm failed: %s\n' "${jobs[$idx]}" >&2
      printf 'result bundle: %s\n' "$WORK_ROOT/${jobs[$idx]}/result_bundle" >&2
    fi
  done
fi

if (( ${#statuses[@]} > 0 )); then
  printf 'vm statuses: %s\n' "${statuses[*]}"
fi

if (( ${#failures[@]} > 0 )); then
  printf 'failed vm runs: %s\n' "${failures[*]}" >&2
  exit 1
fi
