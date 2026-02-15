# Copilot Instructions for Termex-Android

Termex is an SSH terminal client for Android using Jetpack Compose for the UI. This guide covers build commands, architecture, and key conventions.

## Build, Test, and Lint

### Build
```bash
# Full build (debug variant)
./gradlew build

# Build debug APK only
./gradlew assembleDebug

# Build release variant
./gradlew assembleRelease

# Dev variant with paywall bypass (for testing)
./gradlew assembleDev
```

### Run Tests
```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module/package
./gradlew test --tests "com.termex.app.core.ssh.*"

# Run a single test class
./gradlew test --tests "com.termex.app.core.ssh.AnsiParserTest"

# Run instrumentated tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Lint & Code Quality
```bash
# Check for build issues and warnings
./gradlew build --warning-mode=all

# Check Kotlin code style (if configured)
./gradlew lintDebug
```

### Install & Run
```bash
# Build and install debug APK to emulator/device
./gradlew installDebug

# Build, install, and run
./gradlew runDebug
```

## Project Architecture

### High-Level Structure
- **UI Layer** (`app/src/main/java/com/termex/app/ui/`): Jetpack Compose screens, ViewModels, navigation
- **Domain Layer** (`app/src/main/java/com/termex/app/domain/`): Use cases and business logic
- **Data Layer** (`app/src/main/java/com/termex/app/data/`): Repositories, DAOs, local/remote data sources
- **Core Layer** (`app/src/main/java/com/termex/app/core/`): SSH client, security, ANSI parsing, utilities
- **DI Layer** (`app/src/main/java/com/termex/app/di/`): Hilt dependency injection modules

### Key Technologies
- **Framework**: Android, Jetpack Compose
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (SQLite)
- **Local Storage**: DataStore (for preferences)
- **SSH**: Apache MINA SSHD (with support for ed25519 keys)
- **Testing**: JUnit 4, MockK, Turbine (Flow testing), Espresso

### Database & Persistence
- **Room Database**: `TermexDatabase` with DAOs for `ServerDao`, `SnippetDao`, `WorkplaceDao`, `KnownHostDao`, `SessionStateDao`
- **Database Migrations**: Managed in `TermexDatabase.ALL_MIGRATIONS`
- **DataStore**: User preferences stored as key-value pairs in `termex_prefs`
- **Secure Storage**: User SSH passwords encrypted via `SecurePasswordStore` (EncryptedSharedPreferences)

### ViewModels & State Management
- All screens use MVVM pattern with Hilt-injected ViewModels
- State flows are exposed for Compose to collect as state
- Examples: `TerminalViewModel`, `ServersViewModel`, `SnippetsViewModel`, `PortForwardingViewModel`

## Key Conventions & Patterns

### Hilt Dependency Injection
- Use `@AndroidEntryPoint` on Activities/Fragments
- `@Inject` fields in Activities/ViewModels
- Modules define providers with `@Module`, `@InstallIn(SingletonComponent::class)`, `@Provides`, `@Singleton`
- Example: `AppModule.kt` and `RepositoryModule.kt`

### Testing
- Unit tests in `app/src/test/java` use JUnit 4
- Mock external dependencies with MockK
- Use Turbine for testing Flow-based state
- Example tests: `AnsiParserTest`, `SshIntegrationTest`, `SessionRepositoryTest`

### Navigation & Routing
- Routes defined in `Routes.kt`
- Compose navigation using NavHost and NavController
- Main tabs structure in `MainTabs.kt`, onboarding flow in `OnboardingFlow.kt`

### Gradle Configuration
- Uses Kotlin DSL (`build.gradle.kts`)
- KSP (Kotlin Symbol Processing) for annotation processing (Room, Hilt)
- Parallel builds enabled for speed
- Three build types: `debug`, `dev` (with paywall bypass), `release` (with ProGuard)
- Signing configuration supports both `keystore.properties` file and environment variables

### Security & Secrets
- **Never commit** `keystore.properties`, `local.properties`, or any credential files
- SSH keys and user passwords stored in encrypted storage, never cleartext
- Release signing keys managed via CI secrets
- See `SECURITY.md` for detailed security guidelines
- See `GOOGLE_PLAY_SUBSCRIPTION_SETUP.md` for subscription and billing setup

### Compose & UI
- Material Design 3 theming in `TermexTheme`
- Edge-to-edge layout enabled
- Biometric lock screen integration for app launch
- Dark/light/auto theme modes stored in DataStore preferences

### SSH & Terminal Features
- SSH client built on Apache MINA SSHD
- ANSI escape sequence parsing and rendering
- Port forwarding support
- Known hosts management
- Multiple terminal sessions supported
- Snippets (saved commands) with WorkplaceVM organization

## Common Development Tasks

### Adding a New Screen
1. Create a `@Composable` function in `ui/screens/`
2. Create corresponding ViewModel if needed (inject in Activity via Hilt)
3. Add route to `Routes.kt`
4. Add NavHost entry in appropriate screen container

### Adding a New Database Entity
1. Create entity class in `data/local/entities/`
2. Add @Entity annotation and Room metadata
3. Create/update DAO in `data/local/`
4. Create migration in `TermexDatabase` if changing existing schema
5. Update `RepositoryModule.kt` to provide the DAO

### Working with SSH
- SSH operations mostly in `core/ssh/` (SshClient, ANSI parsing)
- Terminal output rendered with ANSI color/style support via `AnsiParser`
- See `AnsiParserTest` for parsing examples

### Signing for Release
- Set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` in CI secrets
- Locally use `keystore.properties` (see `keystore.properties.example`)
- Run `./gradlew assembleRelease` to build signed APK

## Gradle & Build Notes

- JVM target: Java 17 / Kotlin 2.0.21
- Compile SDK: 35, Min SDK: 26, Target SDK: 35
- Configuration-on-demand and build caching enabled for faster rebuilds
- ProGuard rules in `app/proguard-rules.pro` for release builds
- Check `gradle.properties` for parallel build settings (workers, caching, incremental compilation)

## MCP Servers (if configured)

Available tools for this repository:
- **GitHub MCP**: Search/browse issues, PRs, commits, and code across the repository
- **Filesystem MCP**: Enhanced file operations (viewing, searching, tree structure)
- **Context7 MCP**: Library documentation and code examples lookup
