# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All commands run from the `NovaChat/` subdirectory (the Gradle root):

```bash
cd NovaChat
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (R8 minification + resource shrinking)
./gradlew bundleRelease          # Release AAB for Play Console upload
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator required)
./gradlew clean                  # Clean build artifacts
```

When uploading to Play Console, increment **both** `versionCode` (integer) and `versionName` (semver string) in `app/build.gradle.kts`. Every Play Console submission requires a unique, strictly increasing `versionCode`.

## Architecture Overview

NovaChat is a Jetpack Compose SMS/MMS app targeting Android 12+ (API 31). It must be set as the device default SMS app to function.

### Layer structure

```
domain/repository/   ← interfaces only (no Android dependencies)
data/repository/     ← implementations backed by Room DAOs + Android SmsProvider
di/                  ← Hilt modules wiring interfaces to impls
ui/<screen>/         ← Screen composable + ViewModel per feature
core/                ← cross-cutting infrastructure (DB, SMS, theme, billing, ML)
```

### Dependency injection

Five Hilt modules in `di/`:
- `AppModule` — DataStore, ContentResolver
- `DatabaseModule` — Room DB, all DAOs, migrations
- `RepositoryModule` — `@Binds` interface → impl for every repository
- `FinancialModule` — separate SQLCipher-encrypted DB + Android Keystore key manager
- `SmsSpamModule` — spam detection pipeline components

`NovaChatApplication` is `@HiltAndroidApp`. `MainActivity` is `@AndroidEntryPoint`. All ViewModels use `@HiltViewModel`.

### Navigation

Routes are defined as `@Serializable` objects/data classes in `ui/navigation/NavRoutes.kt`. The outer `Scaffold` and `NavHost` live in `ui/navigation/NovaChatNavHost.kt`. Bottom navigation is shown only on the three top-level routes (`ConversationsRoute`, `FinancialDashboardRoute`, `SettingsRoute`), detected via `::class.qualifiedName` comparison.

**R8 / ProGuard critical:** Release builds use `isMinifyEnabled = true`. The ProGuard rule `-keepnames class com.novachat.ui.navigation.*Route` must be present or R8 will rename route classes and break navigation at runtime. Do not remove it.

### Window insets / edge-to-edge

`enableEdgeToEdge()` is called in `MainActivity`. The outer Scaffold in `NovaChatNavHost` handles all system insets via its `innerPadding`. The `NavHost` modifier chain is:
```kotlin
.padding(innerPadding).consumeWindowInsets(innerPadding)
```
`consumeWindowInsets` is essential — without it, inner Scaffolds on every screen re-apply the same insets, producing double blank space at the top/bottom. Do not remove it.

### Theme engine

`core/theme/NovaChatThemeEngine.kt` computes a Material 3 `ColorScheme` from a `NovaChatTheme` model and exposes it via four `CompositionLocal` providers: `LocalChatColors`, `LocalChatShapes`, `LocalChatWallpaper`, `LocalActiveTheme`. Use these locals — not `MaterialTheme` — when accessing chat-specific colors or bubble shapes.

### Data layer patterns

- Repositories expose `Flow<>` for streaming data and `suspend fun` for mutations.
- ViewModels collect with `collectAsStateWithLifecycle()` and expose a single `UiState` `StateFlow`.
- `ConversationRepository` has a cache layer (`getCachedConversations`, `invalidateConversationsCache`). Call invalidation after writes, not unconditionally.

### Database

- **Main DB:** Room (`NovaChatDatabase`, version 17). Uses both `autoMigrations` and manual `Migration` objects. Always add a migration when changing the schema — never bump version without one.
- **Financial DB:** Separate SQLCipher-encrypted database. The passphrase is stored in Android Keystore via `FinancialKeyManager`. Never store the passphrase anywhere else.

### Spam detection pipeline

`core/sms/SpamFilter.kt` orchestrates three layers in order:
1. `DeterministicSpamLayer` — rule-based (block list, regex patterns)
2. `HeuristicSpamLayer` — scoring heuristics
3. `SemanticSpamLayer` — ML model (LiteRT / TensorFlow Lite)

`ScamDetector` holds 251 compiled regex patterns and is **lazy-initialized** — do not eagerly instantiate it.

### Financial Intelligence

Premium feature (`core/sms/financial/`). Parses SMS from Israeli and US banks into structured `FinancialTransaction` entities stored in the encrypted DB. Uses ML Kit Entity Extraction + regex. Gated behind `LicenseManager.isPremium` and a one-time onboarding flow (`FinancialOnboardingRoute`).
