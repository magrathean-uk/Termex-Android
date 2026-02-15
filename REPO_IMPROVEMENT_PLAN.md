# Repository Improvement Plan

## 1) Snapshot

- **Purpose:** Secure SSH terminal client for mobile devices, allowing users to manage server connections, SSH keys, port forwarding, command snippets, and multi-terminal workplaces behind a subscription paywall.
- **Key user journeys:**
  - Onboarding → paywall → subscription → main app
  - Add/edit/delete server connections
  - Connect to server via SSH (password, key, jump host)
  - Terminal interaction with ANSI rendering and extended keyboard
  - Manage SSH keys (generate, import, delete)
  - Configure and activate port forwards (local, remote, dynamic)
  - Organize servers into workplaces for multi-terminal sessions
  - Save and reuse command snippets
  - Biometric lock, theme preferences, demo mode
- **Build/run/test entry points:**
  - Build: `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew assembleDev`
  - Test: `./gradlew test`, `./gradlew test --tests "com.termex.app.core.ssh.AnsiParserTest"`
  - Lint: `./gradlew lintDebug`
  - CI: `.github/workflows/ci.yml` — runs `./gradlew test` on push/PR to `main`

---

## 2) Top risks (P0 only)

- **Backup rules include encrypted credential store.** `backup_rules.xml` and `data_extraction_rules.xml` include all `sharedpref` and `database` domains. This means `termex_secrets` (encrypted passwords) and the Room database (server configs with `passwordKeychainID`) are backed up to cloud and transferred between devices, potentially leaking credential references.
  - Evidence: `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/termex/app/data/crypto/SecurePasswordStore.kt:20-22`
  - Failure mode: Security — backed-up encrypted prefs may fail to decrypt on a different device (different `MasterKey`), or credential metadata leaks
  - Fix: Exclude `termex_secrets` from backup; only back up non-sensitive prefs and database

- **`runBlocking` in `onResume` on the main thread.** `MainActivity.kt:85` calls `runBlocking` inside `onResume()` to read from DataStore. This blocks the UI thread during every app resume, risking ANRs, especially if DataStore is contended.
  - Evidence: `app/src/main/java/com/termex/app/MainActivity.kt:80-94`
  - Failure mode: ANR / freeze on resume
  - Fix: Cache the biometric preference in a field updated by a coroutine collector instead of blocking on each resume

- **`runBlocking` in SSH host key verification on IO thread.** `HostKeyVerifier.verifyServerKey` (line 82) uses `runBlocking` inside the MINA SSHD callback, blocking an IO thread while waiting for the async user decision via `CompletableDeferred`. If the user is slow or the UI callback deadlocks, this blocks indefinitely.
  - Evidence: `app/src/main/java/com/termex/app/core/ssh/HostKeyVerifier.kt:75-85`
  - Failure mode: Indefinite thread block, potential connection timeout, ANR if called from wrong thread
  - Fix: Add a timeout to `runBlocking` or restructure to use a suspending verification bridge

- **Leftover script from a different project.** `scripts/verify_db_logic.py` references `TESLA_DB_HOST`, `TESLA_DB_USER`, `TESLA_DB_PASS`, and queries a PostgreSQL database unrelated to this codebase. If credentials were ever set, this is a credential exposure risk. The script itself reveals internal database schema of another project.
  - Evidence: `scripts/verify_db_logic.py` (entire file)
  - Failure mode: Credential leakage, confusion, repo hygiene
  - Fix: Delete the file; verify git history is clean

- **`SubscriptionManager` reconnect loop with no backoff.** `onBillingServiceDisconnected()` immediately calls `startConnection()` with no delay or retry limit. On devices where billing service is unavailable, this creates an infinite tight reconnect loop.
  - Evidence: `app/src/main/java/com/termex/app/core/billing/SubscriptionManager.kt:55-58`
  - Failure mode: CPU spin, battery drain, excessive log spam
  - Fix: Add exponential backoff with a retry cap

---

## 3) Prioritised backlog

| Priority | Area | Problem / symptom | Evidence | Proposed change | Acceptance criteria | Verification | Complexity |
|---|---|---|---|---|---|---|---|
| P0 | Security/Privacy | Backup rules include encrypted credential store (`termex_secrets`) and database, risking data leakage | `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Exclude `termex_secrets` shared prefs and sensitive DataStore files from both backup configs | Encrypted prefs and credential-bearing prefs are excluded from cloud backup | Manual: verify backup rules XML; test backup/restore cycle | S |
| P0 | Correctness | `runBlocking` in `onResume` blocks main thread for DataStore read | `MainActivity.kt:80-94` | Replace with a cached field updated by a coroutine collector started in `onCreate` | No `runBlocking` in `onResume`; biometric check reads from memory | Unit test; StrictMode violation check in debug build | S |
| P0 | Correctness | `runBlocking` in `HostKeyVerifier.verifyServerKey` blocks IO thread indefinitely waiting for user input | `HostKeyVerifier.kt:75-85` | Add a timeout (e.g. 120s) to the `runBlocking` call; consider restructuring to suspending bridge | Verification never blocks indefinitely; times out gracefully | Test: simulate slow user response | M |
| P0 | Security | Leftover database verification script from unrelated project in `scripts/` | `scripts/verify_db_logic.py` | Delete the file; add to `.gitignore` or remove `scripts/` directory | No unrelated scripts in repo | `ls scripts/` returns empty or absent | S |
| P0 | Performance | `SubscriptionManager.onBillingServiceDisconnected` reconnects immediately with no backoff | `SubscriptionManager.kt:55-58` | Add exponential backoff (1s, 2s, 4s…) with max 5 retries | No tight reconnect loop when billing unavailable | Test on device without Play Services | S |
| P0 | Security | `exportSchema = false` in Room database — no schema export for migration validation | `TermexDatabase.kt:18` | Set `exportSchema = true` and configure schema output directory in `build.gradle.kts` | Schema JSON files generated for each version; CI can diff schemas | Build produces schema files | S |
| P1 | Build | `gradle.properties` contains duplicate entries (`org.gradle.jvmargs`, `org.gradle.parallel`, etc.) — last-wins behavior is fragile | `gradle.properties:1-21` (lines 1, 13, 20 duplicate `jvmargs`; lines 9, 17 duplicate `parallel`) | Deduplicate; keep the most complete version of each property | No duplicate keys in `gradle.properties` | Grep for duplicate keys | S |
| P1 | Build | Deprecated `android.defaults.buildfeatures.buildconfig=true` in `gradle.properties` (will be removed in plugin v10) | `gradle.properties:5`, build warning output | Move `buildConfig = true` into `app/build.gradle.kts` under `android.buildFeatures {}` block; remove from `gradle.properties` | No deprecation warning during build | `./gradlew assembleDebug 2>&1 \| grep -i deprecated` returns empty | S |
| P1 | Build | Backup `.bak` and `.finalbak` files checked into repo | `app/src/main/AndroidManifest.xml.bak`, `app/build.gradle.kts.finalbak` | Delete backup files; add `*.bak` and `*.finalbak` to `.gitignore` | No backup files in repo | `find . -name "*.bak" -o -name "*.finalbak"` returns empty | S |
| P1 | Build | Resource shrinking not enabled for release build (only code minification) | `app/build.gradle.kts:59` (only `isMinifyEnabled = true`) | Add `isShrinkResources = true` to release build type | Release APK has unused resources stripped | Compare APK sizes before/after | S |
| P1 | Correctness | ANSI parser does not handle CSI sequences other than SGR (e.g. cursor movement `ESC[H`, erase `ESC[J`, `ESC[K`). Real SSH sessions emit these heavily. | `AnsiParser.kt:96-154` — only handles `m` suffix | Add handling for cursor movement (`H`, `A`, `B`, `C`, `D`), erase (`J`, `K`), and `TerminalBuffer` cursor positioning | `vim`, `top`, `htop` render correctly in terminal view | Add unit tests for cursor movement and erase sequences | L |
| P1 | Correctness | `TerminalBuffer` uses `synchronized(lock)` for thread safety but emits `StateFlow` inside the lock, which can cause deadlocks if collectors synchronize on the same lock | `TerminalBuffer.kt:69, 136-142` | Emit content outside the `synchronized` block by copying data inside the lock and emitting after release | No deadlock under concurrent read/write | Stress test: concurrent writes + collectors | M |
| P1 | Correctness | `SSHClient` creates a new `SshClient` on every `connect()` call but doesn't reuse or pool. Each client starts its own NIO thread pool. | `SSHClient.kt:217` — `SshClient.setUpDefaultClient()` per connection | Reuse a singleton `SshClient` instance or shut down properly | No thread leak after repeated connect/disconnect | Thread count monitoring in tests | M |
| P1 | UI/UX | Hard-coded strings in `PaywallScreen.kt` (17+ occurrences like "Termex", "PRO", "Professional SSH Terminal", etc.) — not localisable | `PaywallScreen.kt:184-226` and throughout | Extract to `strings.xml` and use `stringResource()` | All user-visible strings use string resources | Grep for bare string literals in screen files | M |
| P1 | UI/UX | Hard-coded gradient and accent colors in `PaywallScreen.kt` outside the theme system | `PaywallScreen.kt:76-80` | Move colors to `TerminalColorScheme` or theme; derive from Material color scheme | Colors adapt to dark/light theme and dynamic color | Visual check in both themes | S |
| P1 | Security | Private SSH keys stored as plain files in app internal storage (`ssh_keys/` directory) without additional encryption | `KeyRepositoryImpl.kt:30-31` | Encrypt private key files at rest using `EncryptedFile` from `security-crypto` library | Private keys are encrypted on disk | Verify file contents are not readable PEM | M |
| P1 | Testing | Only 4 test files exist; no tests for repositories, ViewModels, database DAOs, billing, or UI | `app/src/test/java/` — 4 files total; `AnsiParserTest`, `RealSSHTest`, `SshIntegrationTest`, `SessionRepositoryTest` | Add: (1) Room DAO tests with in-memory DB, (2) ViewModel tests with MockK, (3) UI tests for onboarding/paywall flows | ≥80% coverage on `data/repository`, `core/billing`, ViewModels | CI test pass | L |
| P1 | Build | CI pipeline only runs `./gradlew test` — no lint, no build verification, no secrets scanning | `.github/workflows/ci.yml` | Add `lintDebug`, `assembleRelease` (without signing), and a secrets scan step (e.g. `trufflehog`) | CI catches lint issues, build breaks, and committed secrets | CI workflow passes end-to-end | M |
| P1 | Correctness | `TerminalView` Canvas redraws every cell on every frame — no rendering optimisation for unchanged content | `TerminalView.kt:91-152` | Use `remember` with key-based invalidation; only redraw changed lines. Consider `drawWithCache` | Measurable FPS improvement during terminal output; < 16ms frame time | Benchmark with continuous output | M |
| P1 | Maintainability | `MainTabs.kt` is 788 lines — contains all 4 tab screens (servers, keys, snippets, settings) inline | `app/src/main/java/com/termex/app/ui/screens/MainTabs.kt` | Extract each tab into its own file: `ServersTab.kt`, `KeysTab.kt`, `SnippetsTab.kt`, `SettingsTab.kt` | `MainTabs.kt` only contains navigation shell; each tab < 200 lines | Build passes; UI unchanged | M |
| P2 | UI/UX | Missing `contentDescription` on 7 UI elements | `grep -rn "contentDescription = null"` returns 7 hits across UI files | Add meaningful content descriptions for all interactive icons | All interactive elements have non-null content descriptions | TalkBack walkthrough | S |
| P2 | Performance | `TerminalView` hardcodes `charWidthPx = fontSize * 0.6` — incorrect for many monospace fonts | `TerminalView.kt:56` | Measure actual character width from the `Paint` object using `measureText("M")` | Character width matches actual font metrics | Visual alignment test with cursor | S |
| P2 | Data | `session_states` table grows unbounded — old sessions never cleaned up automatically | `SessionRepository.kt:43-46` has `cleanupOldSessions` but it's never called | Call `cleanupOldSessions` on app startup or periodically (e.g. keep last 30 days) | Old sessions pruned; database size stays bounded | Check row count after multiple connect/disconnect cycles | S |
| P2 | Maintainability | `Converters.portForwardsToJson/FromJson` uses `JSONObject`/`JSONArray` instead of kotlinx.serialization which is already a dependency | `Converters.kt:21-56`, `build.gradle.kts:4` (serialization plugin applied) | Migrate to `@Serializable` data class + `Json.encodeToString`/`decodeFromString` | Port forward serialization uses kotlinx.serialization | Unit test round-trip encode/decode | S |
| P2 | Build | Compose BOM pinned to `2024.12.01`; several dependencies are at older patch versions | `app/build.gradle.kts:95` | Update BOM and dependencies to current stable versions | Build clean with latest stable BOM | `./gradlew dependencies --configuration releaseRuntimeClasspath` | S |

---

## 4) Quality gates (definition of "done")

- **Build hygiene:**
  - `./gradlew assembleDebug` and `./gradlew assembleRelease` complete with zero errors
  - No deprecation warnings from build system (move `buildConfig` flag, fix duplicate gradle properties)
  - No backup or generated files committed (`.bak`, `.finalbak`, `.DS_Store`)
- **Tests:**
  - `./gradlew test` passes with 0 failures
  - Core paths covered: ANSI parsing, SSH config building, Room DAOs, session persistence, subscription state machine
  - ViewModel tests for `TerminalViewModel`, `AppViewModel` with MockK
- **Lint / static analysis:**
  - `./gradlew lintDebug` produces 0 errors
  - No `runBlocking` calls on the main thread (enforce via custom lint or StrictMode)
  - No hardcoded strings in UI composables (enforce via lint rule `HardcodedText`)
- **Runtime health:**
  - No StrictMode violations in debug
  - LeakCanary reports 0 retained objects after connect/disconnect cycle
  - No ANR during onboarding → paywall → terminal flows
- **UX baseline:**
  - All screens render correctly in light and dark themes
  - All interactive elements have content descriptions
  - Terminal cursor blinks, text renders correctly with ANSI colors
  - Paywall blocks back navigation; subscription flow completes
- **Security baseline:**
  - No secrets in git history (verify with `trufflehog` or `gitleaks`)
  - `usesCleartextTraffic="false"` in manifest (already set)
  - Network security config rejects cleartext (already set)
  - Encrypted prefs excluded from cloud backup
  - Private SSH keys encrypted at rest

---

## 5) Design/UX alignment notes

- **Current UI patterns:**
  - Material Design 3 with bottom navigation (4 tabs: Servers, Keys, Snippets, Settings)
  - Custom canvas-drawn terminal emulator with pinch-to-zoom and extended keyboard
  - Premium-styled paywall with gradient background, animations, and gold accent
  - Onboarding flow with hidden demo mode activation (5 taps on logo)
  - Biometric lock screen on app resume
- **Inconsistencies / UX debt:**
  - PaywallScreen uses hardcoded gradient colors (`GradientStart`/`GradientMid`/`GradientEnd`, `AccentGold`, `AccentTeal`) that don't derive from the theme and won't adapt to dynamic color. Evidence: `PaywallScreen.kt:76-80`
  - Terminal top bar uses hardcoded `Color(0xFF1C1C1E)` instead of theme surface color. Evidence: `TerminalScreen.kt:138-140`
  - `MainTabs.kt` (788 lines) mixes navigation shell with inline screen content, making individual screens harder to maintain and inconsistent with other screens that have dedicated files
  - No empty-state illustrations or guidance — empty server list, empty keys list, empty snippets list just show blank screens
- **Proposed design corrections:**
  - Derive PaywallScreen colors from theme to support dynamic color and ensure accessibility
  - Replace hardcoded terminal UI colors with theme-aware variants
  - Add empty-state composables with helpful actions (e.g. "Add your first server" button)
  - Use consistent animation patterns across all navigation transitions
- **Accessibility checklist:**
  - **Content descriptions:** 7 elements have `contentDescription = null` — add meaningful descriptions
  - **Touch targets:** Verify all interactive elements meet 48dp minimum (terminal keyboard keys are a risk area)
  - **Focus management:** Terminal hidden `BasicTextField` (`TerminalScreen.kt:208-229`) should properly manage focus and announce state changes
  - **Contrast:** PaywallScreen white-on-dark-gradient text (`Color.White.copy(alpha = 0.4f)` — line 419) may fail WCAG contrast ratio
  - **Screen reader:** Terminal canvas content is invisible to TalkBack — consider adding `semantics` for last output line or a "read last line" action

---

## 6) Performance and resource plan

- **Hotspots identified:**
  - **Terminal rendering:** `TerminalView.kt` Canvas redraws every cell on every frame. With 80×24 = 1920 cells, each requiring `drawText` + potential `drawRect`, this produces ~4000 draw calls per frame during scrolling. Evidence: `TerminalView.kt:91-152`
  - **ANSI parsing on every write:** Each chunk of SSH output is parsed through `AnsiParser` character-by-character in a `synchronized` block. Large output bursts (e.g. `cat` of a large file) will stall the UI. Evidence: `TerminalBuffer.kt:68-109`
  - **`StateFlow` emission per write:** Every `write()` call emits a new `contentFlow` value, creating a new list of `TerminalLine` objects. High-frequency writes cause excessive GC pressure. Evidence: `TerminalBuffer.kt:136-142`
  - **SSH thread pool per connection:** Each `connect()` creates a new `SshClient.setUpDefaultClient()` which starts NIO threads. Repeated connect/disconnect cycles leak threads. Evidence: `SSHClient.kt:217`
- **Proposed measurements:**
  - Frame rendering time: Use Compose `rememberLaunchedEffect` with frame callback to measure render duration
  - Memory allocation rate: Profile with Allocation Tracker during continuous terminal output
  - Thread count: Monitor after 5 connect/disconnect cycles; expect stable count
  - Startup time: Measure cold start to first frame with `reportFullyDrawn()`
- **Optimisation backlog:**
  - Batch `TerminalBuffer.write()` emissions using `conflate()` or debounce — only emit at most once per 16ms
  - Cache unchanged `TerminalLine` objects to reduce GC pressure (don't recreate if content hasn't changed)
  - Use `drawWithCache` or `Bitmap` backing for terminal view to avoid per-cell draw calls
  - Reuse `SshClient` instance across connections or ensure proper thread pool shutdown

---

## 7) Security & privacy review

- **Secrets management:**
  - Passwords stored via `EncryptedSharedPreferences` with AES-256-GCM (`SecurePasswordStore.kt`). ✅ Correct approach.
  - SSH private keys stored as **plain PEM files** in app-internal `ssh_keys/` directory (`KeyRepositoryImpl.kt:30-31`). While app-internal storage is sandboxed, this provides no defence-in-depth against device compromise or backup extraction. Fix: encrypt key files at rest.
  - `keystore.properties` correctly `.gitignore`-d. ✅
  - Signing config falls back to environment variables (`signingProp` in `app/build.gradle.kts:20-25`). ✅ CI-friendly.
  - `scripts/verify_db_logic.py` references `TESLA_DB_PASS` and PostgreSQL database credentials from an unrelated project. Must be deleted.
- **Data handling:**
  - Backup rules (`backup_rules.xml`, `data_extraction_rules.xml`) include all `sharedpref` and `database` domains. This backs up `termex_secrets` (encrypted passwords) and the Room database to cloud. The `MasterKey` used for encryption is device-bound — backup data will be unreadable on other devices but the metadata is still exposed. Fix: exclude sensitive domains.
  - `allowBackup="false"` is set in the manifest ✅, but `fullBackupContent` and `dataExtractionRules` are still referenced. These rules apply to `dataExtractionRules` (device-transfer) even when `allowBackup` is false on certain API levels.
  - Session state stores terminal buffer text in Room (`session_states` table) which could contain sensitive output. No automatic cleanup is triggered.
- **Network hardening:**
  - `usesCleartextTraffic="false"` ✅
  - Network security config restricts to system trust anchors only ✅
  - SSH connections use MINA SSHD with BouncyCastle for modern crypto ✅
  - Host key verification implemented with TOFU (trust-on-first-use) model ✅
  - Certificate verification can be disabled per-server (`verifyHostKeyCertificates` flag) — this is a user choice, but the UI should show a clear warning
- **Supply-chain hygiene:**
  - Dependencies pinned to exact versions in `build.gradle.kts` ✅
  - No dependency lock file (`gradle.lockfile`) — builds are reproducible only if remote repos serve identical artifacts
  - `security-crypto:1.1.0-alpha06` is an alpha release. Consider monitoring for stable release.
  - BouncyCastle `1.78.1` is current ✅
  - Apache MINA SSHD `2.16.0` — verify no CVEs

---

## 8) Technical debt register

| Issue | Why it hurts | Minimal remediation | Guardrail |
|---|---|---|---|
| `MainTabs.kt` at 788 lines with 4 inline screens | Hard to navigate, modify, or test individual tabs; merge conflicts likely | Extract each tab to its own composable file | Lint rule or PR check: no composable file > 300 lines |
| `AnsiParser` only handles SGR codes (color/style) | Any real SSH session (editors, pagers, progress bars) will display garbled output | Incrementally add CSI cursor movement (`A-D`, `H`), erase (`J`, `K`), and scroll region support | Test suite with captured real terminal output |
| `TerminalBuffer` uses `synchronized` + immediate `StateFlow` emit | Potential deadlock under contention; excessive emissions degrade UI performance | Copy-out-then-emit pattern; add emission throttling | Stress test with concurrent writers |
| `Converters` uses `JSONObject` when kotlinx.serialization is already available | Two serialization approaches in the same codebase; manual JSON is error-prone | Migrate `PortForward` serialization to `@Serializable` | Build-time check: no `org.json` imports outside converters |
| `UserPreferencesRepository` stores onboarding state as string `"true"/"false"` instead of boolean | Fragile; easy to introduce bugs with string comparison | Change to `booleanPreferencesKey` with migration for existing users | Code review convention: all boolean prefs use `booleanPreferencesKey` |
| No domain-layer interfaces for `SessionRepository` | `SessionRepository` is a concrete class injected directly, unlike all other repositories which have interfaces in `domain/` | Add `SessionRepository` interface in `domain/`; bind via `RepositoryModule` | Convention: all repositories have domain interfaces |
