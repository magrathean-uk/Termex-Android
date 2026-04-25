# Runbook

## Variants

- `debug` - local debug build
- `dev` - `.dev` local build
- `releaseProof` - `.proof` build used for instrumentation and release-surface proof
- `release` - minified, resource-shrunk, signed release bundle

Current Android floor is `minSdk 29`. `compileSdk` and `targetSdk` are `35`.

## Local Lanes

```bash
source /Users/bolyki/dev/source/build-env.sh
./gradlew assembleDev
./gradlew testDevUnitTest
./scripts/run_android_release_surface_proof.sh
```

Use `./scripts/run_android_feature_gate.sh` for the full local gate. It runs JVM tests, release compile proof, managed-emulator checks, and the live VM matrix.

## Managed Emulator And Live VM UI

- Instrumentation uses `com.termex.app.testing.TermexHiltTestRunner`.
- `releaseProof` is the default test build type.
- Live SSH fixtures default to `/Users/bolyki/dev/test/termex-vms`.
- `TERMEX_UI_TEST_PLAN` is the main plan switch. `TERMEX_UI_TEST_SET` still works as a legacy alias.
- Worker bundles land under `TERMEX_ANDROID_WORK_ROOT/<vm>/result_bundle/` with logs, exit status, and summary files.

Common entry points:

```bash
source /Users/bolyki/dev/source/build-env.sh
./scripts/run_android_vm_matrix_ui_tests.sh
./scripts/run_android_release_candidate.sh
```

Useful plans include `smoke`, `auth`, `trust`, `changed-host`, `routing`, `agent`, `session`, and `full`.

## Signing And Release

Local release signing comes from untracked `keystore.properties` or these environment variables:

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Helpers:

- `./scripts/check_release_signing.sh` - fail fast when signing input is missing
- `./scripts/setup_local_release_keystore.sh` - create local upload-key setup
- `./scripts/run_android_release_candidate.sh` - full signed release candidate lane

## Guardrails

- SSH private keys, passphrases, auth tokens, and signing material stay out of the repo.
- Host-key validation blocks connection until the user trusts the fingerprint.
- Diagnostic logs must stay redacted.
- Session restore stays local-only.
- No app feature depends on a paid account, billing product, Sentry project, or Google OAuth client.
