# Termex Android review and parity report

## Scope

Reviewed the Android baseline against the iOS source and docs (`README.md`, `FEATURES.md`, `HOW_IT_WORKS.md`, `AI.md`) and the Android repo docs (`README.md`, `SECURITY.md`, `REPO_IMPROVEMENT_PLAN.md`, `GOOGLE_PLAY_SUBSCRIPTION_SETUP.md`, `AI.md`). The iOS app was treated as the workflow/behavior source of truth, while Android security foundations were preserved.

## What changed

### 1. Secret storage hardening

Android password persistence was moved away from deprecated `EncryptedSharedPreferences` as the primary store. New writes now go through a keystore-backed AES/GCM store, with lazy migration support for legacy values from the old encrypted prefs store. Existing callers keep the same API surface.

Files:
- `app/src/main/java/com/termex/app/data/crypto/SecurePasswordStore.kt`
- `app/src/main/java/com/termex/app/core/ssh/SshConfigBuilder.kt` (existing migration path reused)
- `app/src/main/java/com/termex/app/ui/viewmodel/ServerSettingsViewModel.kt` (existing migration path reused)

### 2. Port-forward persistence bug fix

The Room converter dropped `bindAddress` when serializing `PortForward`, which meant remote forward bind addresses such as `0.0.0.0` were silently lost after save/load. The converter now round-trips `bindAddress` and preserves backward compatibility for legacy payloads by defaulting to `127.0.0.1`.

Files:
- `app/src/main/java/com/termex/app/data/local/Converters.kt`
- `app/src/test/java/com/termex/app/data/local/ConvertersTest.kt`

### 3. Diagnostics surface added

Android had no equivalent to the iOS diagnostics/reporting surface. Added:
- persistent local diagnostics event store
- connection and host-key event recording
- diagnostics summary in Settings
- dedicated Diagnostics screen
- shareable plaintext diagnostics export
- destructive clear actions for diagnostics and saved sessions

Files:
- `app/src/main/java/com/termex/app/data/diagnostics/DiagnosticsRepository.kt`
- `app/src/main/java/com/termex/app/ui/viewmodel/DiagnosticsViewModel.kt`
- `app/src/main/java/com/termex/app/ui/screens/DiagnosticsScreen.kt`
- `app/src/main/java/com/termex/app/ui/viewmodel/SettingsViewModel.kt`
- `app/src/main/java/com/termex/app/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/termex/app/ui/navigation/Routes.kt`
- `app/src/main/java/com/termex/app/TermexApp.kt`

### 4. Home/dashboard parity pass

The Android “Servers” tab was reworked toward the iOS root hierarchy. It now surfaces:
- active sessions
- recent saved sessions
- workplaces
- tool entry points (SSH config browser, known hosts, certificates)
- credential/jump-host issue summary ahead of raw server rows

This brings Android closer to the iOS task flow where the home screen is an operational dashboard rather than only a flat server list.

Files:
- `app/src/main/java/com/termex/app/ui/viewmodel/ServersViewModel.kt`
- `app/src/main/java/com/termex/app/ui/screens/ServerListScreen.kt`
- `app/src/main/java/com/termex/app/ui/screens/MainTabs.kt`

### 5. Terminal connection reporting

Added an in-session connection report sheet from the terminal toolbar. It exposes connection state, target, auth mode, jump host, port-forward count, security flags, and recent diagnostics for the current server. This gives Android a direct support/debug surface closer to the iOS app’s operational transparency.

Files:
- `app/src/main/java/com/termex/app/ui/components/ConnectionReportSheet.kt`
- `app/src/main/java/com/termex/app/ui/viewmodel/TerminalViewModel.kt`
- `app/src/main/java/com/termex/app/ui/screens/TerminalScreen.kt`
- `app/src/main/java/com/termex/app/core/ssh/ConnectionManager.kt`

### 6. Workplaces polish

The ViewModel already supported workplace editing, but the screen did not expose it consistently. The edit affordance is now present in the workplaces UI.

Files:
- `app/src/main/java/com/termex/app/ui/screens/WorkplacesScreen.kt`

### 7. Resource and navigation updates

Added the strings and route wiring required for the new dashboard, diagnostics, and terminal report flows.

Files:
- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/termex/app/ui/navigation/Routes.kt`
- `app/src/main/java/com/termex/app/TermexApp.kt`
- `app/src/main/java/com/termex/app/ui/screens/MainTabs.kt`

## Parity assessment by area

| Area | Status | Notes |
|---|---|---|
| Onboarding | Partial | Android still uses an informational pager. iOS has a more guided first-server setup flow. |
| Server list / home | Improved | Now behaves more like an operational dashboard with active/recent/workplaces/tools and issue surfacing. |
| Password auth | Improved | Secret storage hardened; existing server password migration preserved. |
| Key auth | Partial | Existing support preserved, but Android still models a single selected key rather than iOS-style multi-identity attachment. |
| Certificate auth | Partial | Import/list/delete exists, but Android still lacks full certificate attachment parity and richer certificate metadata handling. |
| Known-host trust | Preserved | Existing trust / mismatch flow retained; diagnostics now records host-key decisions. |
| Snippets | Preserved | Existing snippet picker retained. |
| Port forwarding | Improved | Real persistence bug fixed; terminal report now surfaces forward count. |
| SSH config import/browse | Preserved | Existing browser/import flow retained and surfaced more prominently from home/tools. |
| Workspaces / multi-terminal | Improved | Home/tools access improved; workplace edit action restored. |
| Session restore | Partial | Existing saved session support retained and surfaced, but still simpler than iOS state/workflow restoration. |
| Diagnostics | Improved | Android now has a first-class diagnostics screen and shareable export. |
| App lock | Preserved | Existing biometric lock preserved. |
| Subscription gating | Preserved | Existing billing/paywall model preserved. |

## Remaining gaps not closed in this pass

1. Guided onboarding parity is still incomplete. Android onboarding does not yet walk the user into first-server creation/import the way the iOS product does.
2. Server configuration remains structurally thinner than iOS. The Android `Server` model still has one `keyId`, no per-server certificate attachment map, no persistent tmux/session policy fields, and no startup command field.
3. Certificate handling remains shallow. Android imports certificate files, but does not yet match the richer iOS attachment/configuration semantics.
4. Shared server / transfer workflows present in iOS are still absent on Android.
5. Diagnostics export is plaintext, not a richer bundle/archive comparable to the iOS diagnostics export path.
6. Large-screen adaptive navigation was not converted to a full adaptive navigation suite pass; this update focused on hierarchy/task flow parity first.

## Validation performed

Performed here:
- repo/doc review against iOS and Android docs
- source diff against the original Android baseline
- resource reference sweep (`R.string` / `R.plurals` coverage)
- XML parse validation for `strings.xml`
- targeted code review of modified flow points

Not fully performed here:
- full Gradle build
- unit test execution
- instrumented/device test execution

Reason: the container could not download the Gradle distribution from `services.gradle.org`, so wrapper-based build/test execution was blocked by environment networking, not intentionally skipped.

## Packaging notes

- `local.properties` should not ship in source bundles and is excluded from the final zip.
- No build outputs, caches, or signing secrets were intentionally included.
