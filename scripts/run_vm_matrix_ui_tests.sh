#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=live_vm_harness.sh
source "$SCRIPT_DIR/live_vm_harness.sh"

FIXTURE_ROOT="$(live_vm_fixture_root)"
VM_LIST="${TERMEX_VM_LIST:-debian fedora alma opensuse}"
TEST_PLAN="${TERMEX_UI_TEST_PLAN:-${TERMEX_UI_TEST_SET:-full}}"
GRADLE_TASK="${TERMEX_ANDROID_TEST_TASK:-connectedDevAndroidTest}"

if [[ -n "${TERMEX_UI_TEST_CLASS:-}" && "$TEST_PLAN" == "full" ]]; then
  TEST_CLASS_ARG="$TERMEX_UI_TEST_CLASS"
else
  mapfile -t TESTS < <(live_vm_test_list_for_plan "$TEST_PLAN")
  TEST_CLASS_ARG="$(IFS=,; printf '%s' "${TESTS[*]}")"
fi

for vm in $VM_LIST; do
  echo "=== Termex Android VM matrix: $vm ==="
  eval "$(
    TERMEX_VM_FIXTURE_ROOT="$FIXTURE_ROOT" TERMEX_VM_NAME="$vm" \
      "$ROOT/scripts/prepare_live_ssh_ui_fixture.sh"
  )"

  PRIVATE_KEY_B64="$(python3 - <<'PY'
import base64
import os

print(base64.b64encode(os.environ["TERMEX_UI_TEST_KEY_TEXT"].encode("utf-8")).decode("ascii"))
PY
)"

  CERT_KEY_B64="$(python3 - <<'PY'
import base64
import os

print(base64.b64encode(os.environ["TERMEX_UI_TEST_CERT_KEY_TEXT"].encode("utf-8")).decode("ascii"))
PY
)"

  CERT_TEXT_B64="$(python3 - <<'PY'
import base64
import os

print(base64.b64encode(os.environ["TERMEX_UI_TEST_CERT_TEXT"].encode("utf-8")).decode("ascii"))
PY
)"

  ./gradlew "$GRADLE_TASK" \
    -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS_ARG" \
    -Pandroid.testInstrumentationRunnerArguments.termexHost="$TERMEX_UI_TEST_LIVE_HOST" \
    -Pandroid.testInstrumentationRunnerArguments.termexPort="$TERMEX_UI_TEST_LIVE_PORT" \
    -Pandroid.testInstrumentationRunnerArguments.termexUser="$TERMEX_UI_TEST_LIVE_USERNAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexServerName="$TERMEX_UI_TEST_LIVE_NAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexKeyName="$TERMEX_UI_TEST_KEY_NAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexPrivateKeyBase64="$PRIVATE_KEY_B64" \
    -Pandroid.testInstrumentationRunnerArguments.termexPasswordHost="$TERMEX_UI_TEST_PASSWORD_HOST" \
    -Pandroid.testInstrumentationRunnerArguments.termexPasswordPort="$TERMEX_UI_TEST_PASSWORD_PORT" \
    -Pandroid.testInstrumentationRunnerArguments.termexPasswordUsername="$TERMEX_UI_TEST_PASSWORD_USERNAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexPassword="$TERMEX_UI_TEST_PASSWORD" \
    -Pandroid.testInstrumentationRunnerArguments.termexPasswordName="$TERMEX_UI_TEST_PASSWORD_NAME_FOR_SERVER" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertHost="$TERMEX_UI_TEST_CERT_HOST" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertPort="$TERMEX_UI_TEST_CERT_PORT" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertUsername="$TERMEX_UI_TEST_CERT_USERNAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertName="$TERMEX_UI_TEST_CERT_NAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertNameForServer="$TERMEX_UI_TEST_CERT_NAME_FOR_SERVER" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertKeyName="$TERMEX_UI_TEST_CERT_KEY_NAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertKeyBase64="$CERT_KEY_B64" \
    -Pandroid.testInstrumentationRunnerArguments.termexCertTextBase64="$CERT_TEXT_B64" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpHost="$TERMEX_UI_TEST_JUMP_HOST" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpPort="$TERMEX_UI_TEST_JUMP_PORT" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpUsername="$TERMEX_UI_TEST_JUMP_USERNAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpName="$TERMEX_UI_TEST_JUMP_NAME" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpTargetHost="$TERMEX_UI_TEST_JUMP_TARGET_HOST" \
    -Pandroid.testInstrumentationRunnerArguments.termexJumpTargetPort="$TERMEX_UI_TEST_JUMP_TARGET_PORT"
done
