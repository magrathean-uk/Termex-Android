# Build and run

## Prerequisites

- JDK 17
- Android SDK installed
- `ANDROID_HOME` or a generated `local.properties` with `sdk.dir=...`
- network access on first Gradle run so the wrapper and dependencies can download

`local.properties` is intentionally not included in this source bundle.

## Dev build

From the repo root:

```bash
./gradlew assembleDev
```

Install the resulting APK from `app/build/outputs/apk/dev/`.

## Unit tests

```bash
./gradlew testDevUnitTest
```

## Instrumented tests

Start an emulator or connect a device first, then run:

```bash
./gradlew connectedDevAndroidTest
```

## Android Studio

Open the repo root in Android Studio, let Gradle sync, select the `dev` build variant, then run the `app` configuration.

## Release build

Release signing is driven by either `keystore.properties` or these environment variables:

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Then run:

```bash
./gradlew assembleRelease
```

## Notes

- Billing, signing, and store publishing setup are still documented in `GOOGLE_PLAY_SUBSCRIPTION_SETUP.md` and `keystore.properties.example`.
- First run may take time because the Gradle wrapper and Android dependencies are downloaded.
