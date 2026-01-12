# Contributing

Thanks for helping improve Termex-Android.

## Development setup

- Android Studio (latest stable)
- JDK 17
- Android SDK installed via Android Studio

Open the project in Android Studio and let Gradle sync finish.

## Build

- `./gradlew assembleDebug`

## Tests

- `./gradlew test`

## Code style

- Follow existing Kotlin formatting and naming.
- Keep changes focused and avoid unrelated refactors.

## Before you open a PR

- Make sure `./gradlew test` passes.
- Describe the change clearly and include screenshots for UI changes.
- Do not commit build outputs or local config (`build/`, `.gradle/`, `local.properties`).
