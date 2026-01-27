# Repository Guidelines

## Project Structure & Module Organization
Termex-Android is a single-module Android app in `app/`. Main source lives in `app/src/main/java/com/termex/app`, with `core/` (SSH, terminal, billing), `data/` (Room/DataStore), `domain/` (models + repositories), `ui/` (Compose screens, ViewModels, navigation), and `di/` (Hilt modules). Resources are in `app/src/main/res`, and unit tests live in `app/src/test/java` (e.g., `core/ssh/AnsiParserTest.kt`). Utility scripts, when present, live in `scripts/` (see `scripts/verify_db_logic.py`).

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debug APK.
- `./gradlew test` — run JVM unit tests.
- `./gradlew build` — full build with checks.
Run the app from Android Studio using a connected device or emulator.

## Coding Style & Naming Conventions
Follow existing Kotlin formatting and naming in `app/src/main/java` (4-space indentation, `UpperCamelCase` for types, `lowerCamelCase` for functions/vars). Keep changes focused and avoid unrelated refactors. Compose screens and ViewModels are grouped by feature under `ui/`.

## Testing Guidelines
Unit tests use JUnit4. Place tests in `app/src/test/java` and use descriptive test names (Kotlin backticked names are common). Run `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines
Git history uses short summaries with no strict convention; use concise, imperative messages (e.g., “Add CI”). PRs should pass `./gradlew test`, describe the change, and include screenshots for UI updates. Do not commit build outputs or local config (`build/`, `.gradle/`, `local.properties`).

## Configuration & Security Notes
JDK 17 and Android Studio are required. Signing credentials are read from `local.properties`; keep secrets out of version control.
