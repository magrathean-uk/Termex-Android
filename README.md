# Termex-Android

Termex-Android is the Android version of Termex: a Compose-based SSH client with onboarding, subscription gating, biometric lock, SSH keys and certificates, host-key verification, port forwarding, snippets, workspaces, and terminal sessions.

## Canonical docs

- [Agent guide](./AGENTS.md)
- [Security](./SECURITY.md)
- [Live UI tests](./LIVE_UI_TESTS.md)
- [Google Play subscription setup](./GOOGLE_PLAY_SUBSCRIPTION_SETUP.md)
- [Play release runbook](./PLAY_RELEASE_RUNBOOK.md)
- [Repo improvement plan](./REPO_IMPROVEMENT_PLAN.md)

## Layout

- `app/` - Android application source
- `docs/` - operational and key management notes
- `gradle/` - wrapper and build tooling
- Android 10+ only. `minSdk` is 29.

## Build

```bash
source /Users/bolyki/dev/source/build-env.sh
./gradlew assembleDev
```

## Notes

- Keep generated Gradle output out of the source tree.
- Treat signing and key rotation files as operational, not product docs.
- GitHub auth uses `~/dev/creds/git.key`; App Store Connect uploads use `~/dev/creds/AuthKey_96SRQR4URV.p8` and `~/dev/creds/asc.md`.
- Shared Gradle cache lives under `~/dev/library/gradle` through `build-env.sh`.
- Live UI coverage uses managed-emulator selector and app-lock lanes on `releaseProof`, plus the manual live VM matrix on the non-dev `releaseProof` APKs, with `adb reverse` by default and optional per-VM parallel fan-out. See [LIVE_UI_TESTS.md](./LIVE_UI_TESTS.md).
- Release-surface proof runs release compile/minify tasks plus the non-dev `releaseProof` build type with a fake subscription state. See [BUILD_AND_RUN.md](./BUILD_AND_RUN.md).
- Release-candidate proof runs the full feature gate, builds a signed `release` AAB, verifies the bundle signature, then installs split APKs with `bundletool`. See [PLAY_RELEASE_RUNBOOK.md](./PLAY_RELEASE_RUNBOOK.md).
- Terminal session restore is local-only and excluded from Android cloud backup and device transfer.
- Each VM worker writes a result bundle under `TERMEX_ANDROID_WORK_ROOT/<vm>/result_bundle/` with wrapper, emulator, instrumentation, exit status, and summary files.

## Recent improvements

- Host-key verification now blocks connection establishment until the user explicitly trusts unknown or changed fingerprints.
- SSH config import now carries `LocalForward`, `RemoteForward`, `DynamicForward`, `ProxyJump`, `ForwardAgent`, and `IdentitiesOnly` into Android server records.
- Biometric app lock is process-scoped, survives activity recreation, and re-locks after 30 seconds in the background.
- Diagnostic logs are redacted before persistence and legacy reload for passwords, bearer tokens, PEM blocks, and credential-bearing URLs.
