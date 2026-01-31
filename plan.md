# Teslatlas-Android remediation & enhancement plan

Date: 2026-01-30
Owner: Main developer
Scope: Android app only (no iOS module)

## Objectives
1. Remove critical security risks and secrets.
2. Restore build reliability and data correctness.
3. Improve background sync, performance, and stability.
4. Deliver high-quality UX (themes, strings, transitions, accessibility).
5. Establish sustainable architecture, tooling, and tests.
6. Ensure multi-core CPU utilization, GPU usage where it is clearly beneficial, and ML features only where they add measurable value.

## Guiding constraints
- No direct DB access from mobile in the long term; migrate to an HTTPS API.
- Avoid GPU/Vulkan requirements unless actually needed; always provide CPU fallbacks.
- Keep sensitive data encrypted and out of backups.
- Optimize for real devices (memory class, battery, network).

---

## Phase 0 — Security containment (Blocker)
**Goal:** Eliminate key compromise and plaintext secrets before any further release.

1) **Rotate and remove signing keys (Critical)**
- Rotate compromised upload key and Play App Signing credentials.
- Remove `app/upload-keystore.jks` and purge from history.
- Replace hardcoded keystore passwords with `keystore.properties` + CI secrets.

2) **Remove and rotate secrets (Critical)**
- Remove DB credentials from Python script and onboarding UI defaults.
- Remove/replace Google Maps API key from resources and restrict by package/SHA.
- Add secrets scanning (gitleaks/trufflehog) to CI.

3) **Eliminate cleartext traffic + enforce TLS (Critical)**
- Set `usesCleartextTraffic=false` by default.
- Allow cleartext only in debug and only for explicit dev hosts.
- Replace `SSLConfiguration.Mode.Disable` in `TeslaMateDbClient` with TLS.
- Decide approach for self-signed certificates (import CA or pinning).

4) **Protect stored credentials (Critical)**
- Store DB password only in encrypted storage.
- Disable backups or exclude sensitive DataStore files.

**Deliverable:** Security remediation PR; keys rotated; app no longer leaks secrets.

---

## Phase 1 — Build blockers & correctness (High)
**Goal:** App builds and core flows work reliably.

1) **Resolve `MetricCard` overload ambiguity**
- Rename/remove one composable and update call sites.
- Fix click semantics when `onClick` is null.

2) **Fix background sync config access**
- Read DB config from DataStore flow inside `SyncWorker`.
- Remove unused `SYNC_PREFS` path or fully deprecate it.

3) **Unify Room database name + creation site**
- Define a single DB name constant.
- Build DB once (Application/DI), not in composables.

4) **Fix KeychainManager compile issue**
- Add missing `decodeFromString` import or explicit serializer.

5) **Remove reflection hack for Context**
- Use `LocalContext.current` directly.

6) **Normalize manifest requirements**
- Drop Vulkan/OpenGL requirements unless truly required.
- Re-evaluate `minSdk` vs feature usage and add runtime checks.

**Deliverable:** Build-green, onboarding + main flows functional, background sync operational.

---

## Phase 2 — Data integrity & migrations (High)
**Goal:** Prevent silent data corruption and duplicate writes.

1) **Fix battery health snapshot schema**
- Use `(carId, timestamp)` as primary key.
- Upsert on natural key.

2) **Fix per-car state entities**
- Make `carId` primary key (or composite keys) for SyncState/MapOptimizationState.

3) **Add Room migrations**
- Replace destructive migrations with real schema migrations.

**Deliverable:** Correct data model with stable migrations and no duplicate snapshot spam.

---

## Phase 3 — Performance & multi-core utilization (High)
**Goal:** Reduce jank, improve battery, enable safe multi-core usage.

1) **Move heavy work off main thread**
- Polyline decode/simplify in `Dispatchers.Default` or WorkManager.

2) **Cap polyline points and introduce LOD**
- Hard cap to a sane range (5k–20k).
- Generate and cache multiple levels of detail.

3) **Fix blocking DB calls and cancellation**
- Replace `Future.get()` with cancellable suspend bridges.
- Audit for thread blocking and unbounded timeouts.

4) **Eliminate N+1 queries**
- Use subqueries/lateral joins to fetch odometer in one query.

5) **Optimize route fetch**
- Downsample in SQL where possible; avoid full-position downloads.

6) **Make TraceManager thread-safe**
- Use `Mutex` or confined dispatcher; atomic counters.

7) **Cache budgets based on device**
- Scale caches using memory class and max heap.
- Reduce disk cache to a realistic target (50–200MB).

8) **Remove `largeHeap`**
- Fix memory use rather than requesting huge heaps.

**Deliverable:** Smooth maps, reduced ANRs, stable sync, measurable performance gains.

---

## Phase 4 — UX, design, and accessibility (High)
**Goal:** Beautiful, consistent visuals with real localization and accessibility.

1) **Replace hard-coded colors**
- Use Material color scheme throughout (light/dark safe).

2) **Replace hard-coded strings**
- Use `stringResource` and translations.

3) **Fix units and locale defaults**
- Use `UnitContext.formatTemperature` everywhere.
- Default units based on locale/system.

4) **Improve loading/error states**
- Wrap sync in try/catch; show UI error states.

5) **Accessibility pass**
- Add content descriptions for meaningful icons.
- Ensure 48dp touch targets and proper semantics.

6) **Transitions & effects**
- Implement consistent motion system (enter/exit, shared element, list transitions).
- Use Compose `AnimatedContent`, `AnimatedVisibility`, and Lottie/particles where tasteful.

**Deliverable:** Polished UI with consistent theming, localization, and accessible interactions.

---

## Phase 5 — Architecture & maintainability (Medium)
**Goal:** Single source of truth and testable, scalable structure.

1) **Introduce DI (Hilt or manual)**
- Application-scoped DB, clients, repositories.

2) **Unify preferences/config storage**
- One repository: encrypted secrets + DataStore for non-sensitive prefs.

3) **Remove duplicate polyline utilities**
- One shared simplifier with tests and LOD support.

4) **Define module boundaries (optional)**
- Separate `data`, `domain`, `ui` layers for clarity.

**Deliverable:** Clean architecture, fewer bugs, easier testing.

---

## Phase 6 — Security architecture (Strategic)
**Goal:** Remove the direct DB connection anti-pattern.

1) **Build HTTPS API gateway**
- Token auth, TLS, server-side query optimization.
- Rate limiting and audit logging.

2) **Update app to use API**
- Replace direct Postgres usage.
- Simplify onboarding to API credentials.

**Deliverable:** Secure data access model suitable for real users.

---

## Phase 7 — Release engineering, CI, and testing (Medium)
**Goal:** Repeatable builds with automated quality gates.

1) **Add CI pipeline**
- `assemble`, `test`, `lint`, `detekt`, `ktlint`, secrets scan.

2) **Enable R8 and resource shrinking**
- Fix keep rules based on build output.

3) **Add tests**
- Unit tests for polyline and data mapping.
- DAO tests with Room in-memory DB.
- UI smoke tests for onboarding and paywall flows.

4) **Performance tooling**
- Baseline Profile + Macrobenchmark.
- LeakCanary and StrictMode in debug.

**Deliverable:** Reliable releases, faster regression detection.

---

## GPU/ML usage plan
- **GPU usage only when it materially helps** (e.g., large polyline processing, map overlays, image/compute tasks). Implement with capability checks and CPU fallback.
- **Multi-core CPU**: parallelize chunked processing with structured concurrency (`async/await`) and bounded dispatchers; avoid over-parallelization on small workloads.
- **ML features (if needed)**: evaluate scope (e.g., driving pattern detection, anomaly detection) and prototype with on-device MLKit or lightweight models. Only ship if it improves UX measurably.

---

## Cleanup tasks (Repo hygiene)
- Remove backup files (`*.bak`, `*.finalbak`) and committed IDE/OS artifacts.
- Add `.gitignore` rules and pre-commit checks to prevent recurrence.

---

## Success criteria
- No secrets in repo; keys rotated and protected.
- Build is reproducible and passes CI.
- Background sync works, data is consistent across screens.
- Performance: no UI-thread heavy work; stable memory usage.
- UX: theming + localization + accessibility pass complete.
- Security: cleartext disabled; TLS enabled end-to-end.

---

## Open questions / decisions needed
1) Preferred DI framework (Hilt vs manual).
2) Timeline and hosting model for HTTPS API gateway.
3) Target minimum Android version post-manifest cleanup.
4) Visual direction for UI polish (brand palette, motion system).
5) Whether to support self-signed DB certs during transition.

