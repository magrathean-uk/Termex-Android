#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source /Users/bolyki/dev/source/build-env.sh
# shellcheck source=live_vm_harness.sh
source "$SCRIPT_DIR/live_vm_harness.sh"

cd "$ROOT"

TEST_CLASS="${TERMEX_RELEASE_PROOF_TEST_CLASS:-com.termex.app.ReleaseSurfaceProofTest}"
TEST_VARIANT="${TERMEX_RELEASE_PROOF_VARIANT:-releaseProof}"
TEST_VARIANT_PASCAL="$(capitalize_ascii "$TEST_VARIANT")"
MANAGED_DEVICE="${TERMEX_RELEASE_PROOF_DEVICE:-pixel8api35}"
MANAGED_TASK=":app:${MANAGED_DEVICE}${TEST_VARIANT_PASCAL}AndroidTest"

printf '=== release compile proof ===\n'
./gradlew :app:minifyReleaseWithR8 :app:optimizeReleaseResources :app:compileReleaseArtProfile

printf '=== assemble%s ===\n' "$TEST_VARIANT_PASCAL"
./gradlew ":app:assemble${TEST_VARIANT_PASCAL}"

printf '=== release surface proof ===\n'
./gradlew "$MANAGED_TASK" \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS"
