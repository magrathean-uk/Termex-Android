# Third-Party Licenses

Termex-Android is built on top of the following open-source libraries. All
runtime dependencies use permissive licenses that are compatible with the
project's [MIT License](../LICENSE).

---

## Runtime Dependencies

### Apache MINA SSHD
| Field | Value |
|---|---|
| Artifact | `org.apache.sshd:sshd-core` |
| Version | 2.16.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://mina.apache.org/sshd-project/ |
| Notes | Core SSH client/server library. Powers all SSH connections in Termex. |

### BouncyCastle PKIX / Provider
| Field | Value |
|---|---|
| Artifacts | `org.bouncycastle:bcpkix-jdk18on`, `org.bouncycastle:bcprov-jdk18on` |
| Version | 1.78.1 |
| License | Bouncy Castle Licence (MIT-compatible) |
| SPDX ID | `MIT` |
| Homepage | https://www.bouncycastle.org/ |
| Notes | Cryptography provider. Used for SSH key handling and certificate operations. |

### EdDSA / Ed25519 (net.i2p.crypto)
| Field | Value |
|---|---|
| Artifact | `net.i2p.crypto:eddsa` |
| Version | 0.3.0 |
| License | CC0 1.0 Universal (Public Domain) |
| SPDX ID | `CC0-1.0` |
| Homepage | https://github.com/str4d/ed25519-java |
| Notes | Ed25519 key support for MINA SSHD. |

### SLF4J Simple
| Field | Value |
|---|---|
| Artifact | `org.slf4j:slf4j-simple` |
| Version | 2.0.13 |
| License | MIT License |
| SPDX ID | `MIT` |
| Homepage | https://www.slf4j.org/ |
| Notes | Logging facade used by Apache MINA SSHD. |

### AndroidX Core KTX
| Field | Value |
|---|---|
| Artifact | `androidx.core:core-ktx` |
| Version | 1.15.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/core |

### AndroidX Activity Compose
| Field | Value |
|---|---|
| Artifact | `androidx.activity:activity-compose` |
| Version | 1.9.3 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/activity |

### AndroidX Lifecycle
| Field | Value |
|---|---|
| Artifacts | `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-process`, `lifecycle-viewmodel-compose` |
| Version | 2.8.7 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/lifecycle |

### Jetpack Compose BOM
| Field | Value |
|---|---|
| Artifact | `androidx.compose:compose-bom` |
| Version | 2024.12.01 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/compose |
| Notes | Umbrella BOM for all Compose UI, Material 3, and tooling artifacts. |

### AndroidX Navigation Compose
| Field | Value |
|---|---|
| Artifact | `androidx.navigation:navigation-compose` |
| Version | 2.8.5 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/navigation |

### Hilt / Dagger
| Field | Value |
|---|---|
| Artifacts | `com.google.dagger:hilt-android`, `hilt-android-compiler`, `androidx.hilt:hilt-navigation-compose` |
| Version | 2.51.1 / 1.2.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://dagger.dev/hilt/ |
| Notes | Dependency injection framework. |

### AndroidX DataStore
| Field | Value |
|---|---|
| Artifact | `androidx.datastore:datastore-preferences` |
| Version | 1.1.1 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/topic/libraries/architecture/datastore |

### AndroidX Room
| Field | Value |
|---|---|
| Artifacts | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` |
| Version | 2.6.1 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/room |

### AndroidX Security Crypto
| Field | Value |
|---|---|
| Artifact | `androidx.security:security-crypto` |
| Version | 1.1.0-alpha06 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/security |

### AndroidX Biometric
| Field | Value |
|---|---|
| Artifact | `androidx.biometric:biometric` |
| Version | 1.1.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/jetpack/androidx/releases/biometric |

### AndroidX WorkManager
| Field | Value |
|---|---|
| Artifact | `androidx.work:work-runtime-ktx` |
| Version | 2.10.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/topic/libraries/architecture/workmanager |

### Kotlin Standard Library, Coroutines & Serialization
| Field | Value |
|---|---|
| Artifacts | `org.jetbrains.kotlin:*`, `org.jetbrains.kotlinx:kotlinx-coroutines-*`, `kotlinx-serialization-json` |
| Version | Kotlin 2.0.21, Coroutines 1.9.0, Serialization 1.7.3 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://kotlinlang.org/ |

### Google Material Components for Android
| Field | Value |
|---|---|
| Artifact | `com.google.android.material:material` |
| Version | 1.12.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://github.com/material-components/material-components-android |

---

## Debug-Only Dependencies

These libraries are included only in debug builds and are never shipped in
release APKs or App Bundles.

### LeakCanary
| Field | Value |
|---|---|
| Artifact | `com.squareup.leakcanary:leakcanary-android` |
| Version | 2.14 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://square.github.io/leakcanary/ |
| Notes | Debug-only. Detects memory leaks during development. |

---

## Test-Only Dependencies

These libraries are used in unit and instrumentation tests and are never
included in production builds.

### JUnit
| Field | Value |
|---|---|
| Artifact | `junit:junit` |
| Version | 4.13.2 |
| License | Eclipse Public License 1.0 |
| SPDX ID | `EPL-1.0` |
| Homepage | https://junit.org/junit4/ |

### MockK
| Field | Value |
|---|---|
| Artifacts | `io.mockk:mockk`, `io.mockk:mockk-android` |
| Version | 1.13.13 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://mockk.io/ |

### Turbine
| Field | Value |
|---|---|
| Artifact | `app.cash.turbine:turbine` |
| Version | 1.2.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://github.com/cashapp/turbine |

### OkHttp MockWebServer
| Field | Value |
|---|---|
| Artifact | `com.squareup.okhttp3:mockwebserver` |
| Version | 4.12.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://square.github.io/okhttp/ |

### AndroidX Test (JUnit Ext, Espresso, Orchestrator)
| Field | Value |
|---|---|
| Artifacts | `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`, `androidx.test:orchestrator` |
| Versions | 1.2.1 / 3.6.1 / 1.5.1 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://developer.android.com/training/testing |

### Hilt Android Testing
| Field | Value |
|---|---|
| Artifact | `com.google.dagger:hilt-android-testing` |
| Version | 2.51.1 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://dagger.dev/hilt/testing.html |

### Kotlinx Coroutines Test
| Field | Value |
|---|---|
| Artifact | `org.jetbrains.kotlinx:kotlinx-coroutines-test` |
| Version | 1.9.0 |
| License | Apache License 2.0 |
| SPDX ID | `Apache-2.0` |
| Homepage | https://github.com/Kotlin/kotlinx.coroutines |

---

## License Compatibility Summary

| License | Count | Scope |
|---|---|---|
| Apache License 2.0 | 20 | Runtime + test |
| MIT | 2 | Runtime (BouncyCastle, SLF4J) |
| CC0 1.0 | 1 | Runtime (eddsa) |
| Eclipse Public License 1.0 | 1 | Test-only (JUnit) |

All licenses are compatible with the project's MIT License. No copyleft
(GPL/LGPL) dependencies are present in the runtime dependency graph.
