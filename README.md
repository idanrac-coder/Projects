# NovaChat - Android SMS/MMS Messaging App

A modern, feature-rich SMS/MMS messaging app for Android 12+ built with Kotlin and Jetpack Compose.

## Features

- **Conversations List** - Material 3 Expressive design with swipeable conversation rows
- **Smart Blocking** - Block by phone number (with wildcards), keywords (with regex), or sender name
- **Theme System** - 18 built-in themes + custom theme editor with live preview
- **Configurable Swipe Actions** - Choose from Archive, Delete, Pin, Mark Read/Unread, Mute, Block
- **Advanced Notifications** - Per-contact settings, scheduled DND, quick reply, grouping modes
- **Lifetime Premium License** - Google Play Billing for one-time purchase

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose (type-safe) |
| Billing | Google Play Billing 7.x |
| Images | Coil |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 (Android 15) |

## Project Structure

```
com.novachat/
├── app/          - Application class, MainActivity
├── core/
│   ├── billing/  - Google Play Billing, LicenseManager
│   ├── database/ - Room DB, DAOs, entities
│   ├── datastore/- DataStore preferences
│   ├── sms/      - SMS/MMS provider, receivers, sender
│   └── theme/    - Theme engine, built-in themes
├── data/
│   └── repository/ - Repository implementations
├── di/           - Hilt modules
├── domain/
│   ├── model/    - Domain models
│   └── repository/ - Repository interfaces
└── ui/
    ├── blocking/      - Block rules management
    ├── chat/          - Chat screen
    ├── components/    - Shared composables
    ├── compose/       - New message screen
    ├── conversations/ - Main conversation list
    ├── license/       - Premium purchase screen
    ├── navigation/    - Nav routes and host
    ├── notifications/ - Notification settings
    ├── search/        - Message search
    ├── settings/      - Settings hub
    ├── swipe/         - Swipe action config
    └── themes/        - Theme gallery + editor
```

## Setup

1. Open in Android Studio (Ladybug or newer)
2. Sync Gradle
3. Set up a Google Play Console project for billing (optional for dev)
4. Build and run on an Android 12+ device or emulator

## Freemium Model

**Free features:**
- Full SMS/MMS messaging
- 3 built-in themes
- Up to 5 block rules
- Basic swipe actions
- Standard notifications

**Premium (lifetime license):**
- All 18 built-in themes
- Custom theme editor
- Unlimited block rules
- Priority support

## Requirements

- Android 12 (API 31) or higher
- Must be set as default SMS app to send/receive messages
