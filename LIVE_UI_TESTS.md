# Live UI Tests

Termex-Android has a live SSH UI matrix for manual emulator workers.

## Fixture Root

- Default root: `/Users/bolyki/dev/test/termex-vms`
- Override with `TERMEX_VM_FIXTURE_ROOT`
- Required inputs from that bundle: `login.json` or legacy `raw/login.json`, plus `README.md` or legacy `SUMMARY.md`

## Script Entry Points

- `scripts/run_android_vm_ui_worker.sh`: runs one VM on one emulator, installs the app and test APKs, applies `adb reverse`, and runs direct instrumentation.
- `scripts/run_android_vm_matrix_ui_tests.sh`: builds the non-dev `releaseProof` APKs once, then runs the worker for each VM in `TERMEX_VM_LIST`. The worker gets a unique emulator port per VM. Default mode is serial. Set `TERMEX_ANDROID_VM_MODE=parallel` to fan out one worker per VM.
- `scripts/run_android_feature_gate.sh`: gate wrapper that runs JVM tests, release compile proof, the non-`dev` release-surface proof lane, the managed-emulator selector lane, the managed-emulator app-lock matrix on API 29 and API 35, then the live VM matrix.
- `scripts/run_android_release_candidate.sh`: full release-candidate lane. It runs the feature gate, builds the signed `release` bundle, verifies the bundle signature, then installs split APKs via `bundletool`.
- Each worker writes a result bundle under `TERMEX_ANDROID_WORK_ROOT/<vm>/result_bundle/` with `wrapper.log`, `emulator.log`, `instrumentation.log`, `exit_status.txt`, and `summary.md`. Failed runs also add a screenshot and logcat slice.
- Each worker also writes `details.md` into the bundle with the exact plan, VM prep flags, and fixture paths.

## Emulator Lane

- Default managed-emulator transport uses `adb reverse`.
- Live workers launch the shared Gradle-managed Pixel 8 AVD template in read-only mode and talk to it through direct `adb` plus `am instrument`.
- Default emulator port base is `5654`. Each VM gets the next even port.
- Set `TERMEX_ANDROID_EMULATOR_PORT_BASE` if you need a different port range.
- When the VM bundle host is `127.0.0.1` or `localhost`, the fixture host stays loopback and the worker installs `adb reverse tcp:<vm-port> tcp:<vm-port>`.
- Set `TERMEX_ANDROID_MANAGED_HOST_TRANSPORT=emulator_host` only if you want the old `10.0.2.2` host mapping instead.
- Each worker writes its logs under `TERMEX_ANDROID_WORK_ROOT/<vm>/`.
- The matrix runner keeps the per-distro shape and prints each worker result bundle path when the run ends.

## Scenarios

- `smoke` runs key auth, password auth, certificate auth.
- `auth` runs key auth, password auth, wrong password, certificate auth.
- `trust` runs slow host trust then connect.
- `changed-host` runs host-key trust, then changed-host reconnect proof.
- `routing` runs jump host, local forward, remote forward, and dynamic forward.
- `agent` runs forward-agent proof.
- `session` runs persistent tmux session restore.
- `full` and `all` run the full `RealSshFlowTest` set.
- `TERMEX_UI_TEST_PLAN` is the main plan switch; `TERMEX_UI_TEST_SET` still works as a legacy alias.
- Current live coverage includes key auth, slow host trust then connect, changed-host reconnect, password auth, wrong password path, certificate auth, jump host, local forward, remote forward, dynamic forward, forward-agent socket visibility, and persistent tmux session restore on the `releaseProof` build type.
- Non-live managed-emulator coverage includes stable automation selectors for guided onboarding, server settings, key import, certificate import, workplace and snippet flows, plus app-lock flow checks on API 29 and API 35.

## Gate Flow

Use the gate wrapper when you want the full check path:

1. Run `./gradlew :app:testDevUnitTest`
2. Run `./scripts/run_android_release_surface_proof.sh`
3. Run `./gradlew :app:pixel8api35ReleaseProofAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AutomationSelectorsTest`
4. Run `./gradlew :app:pixel2api29ReleaseProofAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AppLockFlowTest`
5. Run `./gradlew :app:pixel8api35ReleaseProofAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.termex.app.AppLockFlowTest`
6. Run `scripts/run_android_vm_matrix_ui_tests.sh`

That flow keeps the live VM matrix behind JVM and non-live UI gates.

## Examples

- Serial smoke on one VM: `TERMEX_VM_LIST=opensuse TERMEX_UI_TEST_SET=smoke ./scripts/run_android_vm_matrix_ui_tests.sh`
- Parallel smoke on all VMs: `TERMEX_UI_TEST_SET=smoke TERMEX_ANDROID_VM_MODE=parallel ./scripts/run_android_vm_matrix_ui_tests.sh`
- Change-host proof on one VM: `TERMEX_UI_TEST_SET=changed-host ./scripts/run_android_vm_matrix_ui_tests.sh`
- Routing proof on one VM: `TERMEX_UI_TEST_SET=routing ./scripts/run_android_vm_matrix_ui_tests.sh`
- Agent proof on one VM: `TERMEX_UI_TEST_SET=agent ./scripts/run_android_vm_matrix_ui_tests.sh`
- Parallel full matrix on all VMs: `TERMEX_ANDROID_VM_MODE=parallel ./scripts/run_android_vm_matrix_ui_tests.sh`
- Full gate with parallel live matrix: `TERMEX_ANDROID_VM_MODE=parallel ./scripts/run_android_feature_gate.sh`
