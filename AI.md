# AI.md

## Overview
Termex-Android is an Android SSH client with Compose UI, Hilt DI, Room persistence, terminal emulation, host-key verification, port forwarding, and subscription gating.

## Layout
- `app/src/main/java/com/termex/app/core`: SSH, security, billing, demo helpers.
- `app/src/main/java/com/termex/app/data`: Room entities/DAOs, repositories, encrypted storage, preferences.
- `app/src/main/java/com/termex/app/ui`: screens, components, navigation, view models, theme.
- `app/src/test`: JVM tests for SSH/parser/repository logic.
- `plan.md` and `REPO_IMPROVEMENT_PLAN.md`: backlog and remediation notes.

## Commands
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```
The project also has a `dev` variant in generated output; keep command choices aligned with the existing Gradle setup rather than inventing new flavours.

## Guardrails
- Do not edit generated files under `app/build/`.
- Preserve host-key verification, secure password storage, and demo-mode behaviour.
- Keep terminal/session state logic in the core/data layers, not in composables.
