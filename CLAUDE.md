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

- **Main DB:** Room (`NovaChatDatabase`, version 18). Uses both `autoMigrations` and manual `Migration` objects. Always add a migration when changing the schema — never bump version without one.
- **Financial DB:** Separate SQLCipher-encrypted database (sqlcipher-android 4.14.1). The passphrase is stored in Android Keystore via `FinancialKeyManager`. Never store the passphrase anywhere else.

**SQLCipher critical:** `System.loadLibrary("sqlcipher")` must be called in `FinancialModule` before opening the database. The `cipher_page_size` pragma (`PRAGMA cipher_page_size = 4096`) must run in `postKey` (after key is set), **not** `preKey`. ProGuard rules must keep `net.zetetic.database.sqlcipher.**` or R8 will strip the native bridge and crash at runtime.

### Spam detection pipeline

`core/sms/SpamFilter.kt` orchestrates three layers in order:
1. `DeterministicSpamLayer` — rule-based (block list, regex patterns)
2. `HeuristicSpamLayer` — scoring heuristics
3. `SemanticSpamLayer` — ML model (LiteRT / TensorFlow Lite)

`ScamDetector` holds 251 compiled regex patterns and is **lazy-initialized** — do not eagerly instantiate it.

### Financial Intelligence

Premium feature (`core/sms/financial/`). Parses SMS from Israeli and US banks into structured `FinancialTransaction` entities stored in the encrypted DB. Uses ML Kit Entity Extraction + regex. Gated behind `LicenseManager.isPremium` (or active trial) and a one-time onboarding flow (`FinancialOnboardingRoute`).

Key behaviors:
- **Provider opt-in:** Users select which senders to parse during onboarding (`FinancialOnboardingScreen`). Disabled/removed senders are excluded from the dashboard queries — filter at the DAO level, not in the UI.
- **Duplicate prevention:** `FinancialTransactionDao` enforces uniqueness; `FinancialSmsParser` checks before insert on repeated inbox scans.
- **Month filtering:** `FinancialDashboardViewModel` scopes recent transactions to the currently viewed month. `FinancialParsingWorker` respects this scope.
- **Parser coverage:** `RegexParsingEngine` handles Isracard, AMEX, and Cal SMS formats in addition to the standard Israeli/US bank formats.

### Licensing & Trial

`LicenseManager` (`core/billing/`) manages premium access with a **21-day free trial**:
- `TrialState`: `NOT_STARTED` → `ACTIVE` → `EXPIRED`
- `startTrial()` records `trialStartTime` in DataStore and schedules `TrialExpiryWorker`.
- `hasPremiumAccess` is `true` when either `isPremium` or `trialState == ACTIVE`.
- `TrialOfferDialog` is shown to eligible users; `LicenseScreen` displays days remaining.

### Conversation inbox filters

`MessageCategory` enum (`domain/model/`) provides four inbox filter tabs: `ALL`, `CONTACTS`, `UNREAD`, `FAVORITES`. `ConversationsViewModel` exposes the active category and filters the conversation list accordingly.

### Conversation mute

`ConversationContextMenu` (`ui/components/`) includes a mute option that opens `MuteDurationPicker`. Mute state is persisted on `ConversationMetaEntity`. `SmsNotificationHandler` checks mute state before posting notifications.

### Localization

The app supports English and Hebrew (`iw`/`he`). Per-app language selection is available via `SettingsScreen` (options: system default, English, Hebrew). The locale config is declared in `res/xml/locales_config.xml` and referenced in `AndroidManifest.xml`. AppCompat auto-persists the locale for API < 33.
