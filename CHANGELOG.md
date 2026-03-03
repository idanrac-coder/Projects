# Changelog

All notable changes to Aura are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [3.6.0] - 2026-03-03

### Added
- First-launch restore onboarding screen shown after permissions are granted on fresh install or reinstall
- Options to restore from Google Drive or local file before entering the app
- "Start Fresh" option to skip restore and proceed directly

### Changed
- Extract backup/restore utility functions into shared BackupUtils for reuse across screens

## [3.5.7] - 2026-03-03

### Added
- Debug-only "Reset License" button on Premium screen to consume test purchases via consumeAsync

## [3.5.6] - 2026-03-03

### Fixed
- License not revoking after refund — app now re-validates purchase with Google Play every time it resumes

## [3.5.5] - 2026-03-03

### Added
- Privacy Policy link in Settings under Data & Privacy section

## [3.5.4] - 2026-03-02

### Changed
- Smart & Secure screen now matches mockup exactly: VerifiedUser header icon, CheckCircle on right of stats bar, Shield icon on AI toggle row
- Stats bar text on left with green CheckCircle icon on right, green tinted background
- AI Scam Detection row has teal Shield icon before text label

## [3.5.3] - 2026-03-02

### Changed
- Stats bar ("spam blocked this month") uses green tinted background with shield icon on the left, matching mockup
- AI Scam Detection toggle card uses teal tinted background instead of neutral gray

## [3.5.2] - 2026-03-02

### Changed
- Move Blocking and Spam Folder navigation into Smart & Secure screen

### Removed
- Blocking and Spam Folder entries from Settings Messages section

## [3.5.1] - 2026-03-02

### Removed
- Scheduled Messages card from Smart & Secure screen
- Backup & Restore card from Smart & Secure screen

## [3.5.0] - 2026-03-02

### Added
- Smart & Secure screen with unified spam protection dashboard, AI learning stats, and scam detection toggle
- Bubble Style Picker in Themes screen to choose between Rounded, Cloud, Square, and Minimal bubble shapes
- Contact trust bypass: messages from saved contacts skip spam analysis entirely
- Unknown sender score boost: +0.10 confidence boost for numbers not in contacts
- Repeat offender auto-block: permanently blocks senders flagged as spam 2+ times
- Phone call button in chat screen top bar

### Changed
- Conversations list now uses iOS Messages-style clean layout with thin indented dividers
- Themes screen redesigned with unified gallery view and gold PRO badges replacing lock icons
- Message bubbles use wider max width (80%) and more spacious padding
- Scam detection toggle in preferences gates the entire spam analysis pipeline

## [3.4.3] - 2026-03-02

### Removed
- RECORD_AUDIO permission to unblock Google Play upload (voice recording deferred to future release)

## [3.4.2] - 2026-03-02

### Changed
- Include native debug symbols in release App Bundle for better crash analysis on Google Play Console

## [3.4.1] - 2026-03-02

### Changed
- Rebrand all remaining user-facing "NovaChat" / "Nova" references to "Aura"

## [3.4.0] - 2026-03-02

### Added
- Text selection and copy support in message bubbles
- Restore backup from local file or Google Drive
- Auto-backup scheduling with daily, weekly, or monthly frequency
- BackupWorker that overwrites the same backup file on each run
- Expandable notifications showing the full message text (BigTextStyle)
- Mark as read and Delete action buttons on notifications
- NotificationActionReceiver for handling notification actions
- Discount pricing on Premium screen ($5 with $15 strikethrough)
- Billing connection retry logic with exponential backoff

### Changed
- Backup file name simplified to `novachat-backup.zip` (no timestamp)
- Premium fallback price updated from $9.99 to $4.99
- Cloud Backup header renamed from "Google Drive Backup" to "Cloud Backup"

### Fixed
- Red error flash on first app launch (added delay + fade animation)
