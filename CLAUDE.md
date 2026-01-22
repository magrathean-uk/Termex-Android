# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew test             # Run all unit tests
./gradlew build            # Full build with checks
```

Run the app using Android Studio's Run button with a connected device or emulator.

## Architecture

Termex is an SSH terminal emulator for Android built with Jetpack Compose and Clean Architecture.

### Layers

- **domain/** - Pure Kotlin models (Server, Snippet, SSHKey) and repository interfaces. No Android dependencies.
- **data/** - Room database entities, DAOs, repository implementations, and DataStore preferences. Entities have `.toDomain()` and domain models have `.toEntity()` extension functions.
- **core/** - SSH client wrapper (SSHJ), terminal buffer/ANSI parser, billing integration.
- **ui/** - Compose screens, ViewModels, navigation routes, and theming.

### Key Patterns

- **Hilt DI**: All ViewModels use `@HiltViewModel`. Module bindings in `di/AppModule.kt` and `di/RepositoryModule.kt`.
- **StateFlow**: ViewModels expose `StateFlow<T>` for reactive UI. Screens observe with `collectAsState()`.
- **Repository Pattern**: Domain layer defines interfaces, data layer implements them.
- **Room + Flow**: All DAO queries return `Flow` for reactive updates.
- **Compose Navigation**: Routes defined as sealed classes in `ui/navigation/Routes.kt`.

### Database

Room database "termex-database" with entities: ServerEntity, WorkplaceEntity, SnippetEntity. When adding fields/entities, create migration files.

### SSH Implementation

- Uses SSHJ library with full BouncyCastle for modern crypto (X25519 support)
- `core/ssh/SSHClient.kt` - Connection management
- `core/ssh/TerminalBuffer.kt` - Terminal emulation
- `core/ssh/AnsiParser.kt` - ANSI escape sequence parsing (has unit tests)

## Code Style

- Follow existing Kotlin formatting and naming
- Keep changes focused; avoid unrelated refactors
- Before PRs: ensure `./gradlew test` passes
- Do not commit build outputs or local config (`build/`, `.gradle/`, `local.properties`)
