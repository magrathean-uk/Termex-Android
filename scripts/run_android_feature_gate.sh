#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source /Users/bolyki/dev/source/build-env.sh

cd "$ROOT"

run_stage() {
  local name="$1"
  shift
  printf '=== %s ===\n' "$name"
  "$@"
}

run_stage "JVM unit tests" ./gradlew :app:testDevUnitTest
run_stage "release compile proof" ./gradlew \
  :app:minifyReleaseWithR8 \
  :app:optimizeReleaseResources \
  :app:compileReleaseArtProfile
run_stage "release surface proof lane" "$ROOT/scripts/run_android_release_surface_proof.sh"
run_stage "backup and transfer lane" ./gradlew :app:testDevUnitTest \
  --tests com.termex.app.core.transfer.TermexArchiveTransferTest \
  --tests com.termex.app.core.transfer.TermexArchiveTransferIntegrationTest
run_stage "repair routing lane" ./gradlew :app:testDevUnitTest \
  --tests com.termex.app.ui.navigation.RoutesTest
run_stage "extra keys lane" ./gradlew :app:testDevUnitTest \
  --tests com.termex.app.data.prefs.TerminalExtraKeyBehaviorTest \
  --tests com.termex.app.data.prefs.UserPreferencesRepositoryTest
run_stage "terminal tools lane" ./gradlew :app:testDevUnitTest \
  --tests com.termex.app.ui.components.TerminalTranscriptToolsTest
run_stage "selector lane" ./gradlew :app:pixel8api35ReleaseProofAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AutomationSelectorsTest
run_stage "lock matrix api29" ./gradlew :app:pixel2api29ReleaseProofAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AppLockFlowTest
run_stage "lock matrix api35" ./gradlew :app:pixel8api35ReleaseProofAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AppLockFlowTest
run_stage "live VM matrix" "$ROOT/scripts/run_android_vm_matrix_ui_tests.sh"
