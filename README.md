# Termex-Android

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Development on this project has ceased.** The repository is archived for reference and the code is available under the MIT license.

Termex-Android is a free, open-source Android SSH client built with Jetpack Compose.

It supports guided server setup, host-key verification, password and key authentication, certificates, jump hosts, snippets, workspaces, biometric app lock, diagnostics, local archive transfer, saved sessions, and local/remote/dynamic port forwarding.

## Repo Map

- `app/` - Android app, tests, schemas, and build variants
- `app/src/main/java/com/termex/app/core/` - SSH, transfer, security, sync, and runtime code
- `app/src/main/java/com/termex/app/data/` - persistence and repositories
- `app/src/main/java/com/termex/app/domain/` - domain models
- `app/src/main/java/com/termex/app/ui/` - Compose screens and view models
- `scripts/` - emulator, release, and live VM validation helpers

## Local Build

```bash
source /Users/bolyki/dev/source/build-env.sh
./gradlew assembleDev
./gradlew testDevUnitTest
```

## Open-Source Defaults

- No paywall, subscriptions, billing SDK, or purchase restore flow.
- No Sentry or external crash-reporting account.
- No Google sign-in client ID requirement.
- `keystore.properties`, `local.properties`, `.env*`, signing keys, and VM fixtures stay local only.

## Variants

- `debug` - local debug build
- `dev` - `.dev` local build
- `releaseProof` - `.proof` instrumentation and release-surface proof
- `release` - minified, resource-shrunk, signed release bundle

## License

Termex-Android is released under the [MIT License](LICENSE).
Copyright (c) 2024–2025 Magrathean UK.

This project uses open-source dependencies. See [docs/THIRD_PARTY_LICENSES.md](docs/THIRD_PARTY_LICENSES.md)
for the full list of libraries and their licenses.
