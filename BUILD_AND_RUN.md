# Build and run

## Prerequisites

- JDK 17
- Android SDK installed
- Android 10+ device, emulator, or managed device for app runtime and instrumented tests
- `ANDROID_HOME` or a generated `local.properties` with `sdk.dir=...`
- network access on first Gradle run so the wrapper and dependencies can download
- Source `../build-env.sh` before Gradle so `GRADLE_USER_HOME` points at the shared depot.

`local.properties` is intentionally not included in this source bundle.

## Dev build

From the repo root:

```bash
source ../build-env.sh
./gradlew assembleDev
```

Install the resulting APK from `app/build/outputs/apk/dev/`.

## Unit tests

```bash
source ../build-env.sh
./gradlew testDevUnitTest
```

## Instrumented tests

Start an emulator or connect a device first, then run:

```bash
source ../build-env.sh
./gradlew connectedReleaseProofAndroidTest
```

For the live SSH VM matrix, use the dedicated worker scripts in `LIVE_UI_TESTS.md`. They build and run the non-dev `releaseProof` APKs.

## Release surface proof

Use the non-`dev` proof lane when you want a non-bypassed surface check with fake subscription state:

```bash
source ../build-env.sh
./scripts/run_android_release_surface_proof.sh
```

That lane runs release compile/minify proof plus the non-dev `releaseProof` variant, then installs and runs `ReleaseSurfaceProofTest` on the shared emulator.

## Release candidate proof

Prepare local signing once:

```bash
source ../build-env.sh
./scripts/setup_local_release_keystore.sh
```

Then run the full release-candidate lane:

```bash
source ../build-env.sh
BUNDLETOOL_JAR=/absolute/path/to/bundletool-all.jar ./scripts/run_android_release_candidate.sh
```

That lane runs the full feature gate, builds a signed `release` AAB, verifies the bundle signature, builds split APKs with `bundletool`, and installs them onto the first connected `adb` device or emulator.

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
source ../build-env.sh
./gradlew bundleRelease
```

If signing material is not present, use release compile proof only:

```bash
source ../build-env.sh
./gradlew :app:minifyReleaseWithR8 :app:optimizeReleaseResources :app:compileReleaseArtProfile
```

## Notes

- Billing, signing, and store publishing setup are still documented in `GOOGLE_PLAY_SUBSCRIPTION_SETUP.md` and `keystore.properties.example`.
- `scripts/check_release_signing.sh` fails early when release signing inputs are missing or invalid.
- First run may take time because the Gradle wrapper and Android dependencies are downloaded.
