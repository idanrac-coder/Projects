# Changelog

All notable changes to Aura are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [3.17.1] - 2026-03-08

### Fixed
- Nebula theme chat background now correctly displays white instead of purple

## [3.17.0] - 2026-03-08

### Added
- Scroll-to-bottom floating button when viewing older messages in chat (tap to jump to latest)

### Fixed
- Background preview in Conversation Backgrounds now shows actual pattern/image (e.g. Stars, Marble)
- Cancel button in message long-press context menu no longer wraps or misaligns

## [3.16.1] - 2026-03-08

### Added
- Low punctuation ratio heuristic (propaganda often has very few punctuation marks)
- Additional PROPAGANDA patterns: missiles on way, authority claim, shelter safety, imperative flee
- Hebrew fear/catastrophe framing signal (2+ of: השמדה, הטילים, מקלט, משקרים, etc.)

## [3.16.0] - 2026-03-08

### Added
- Structural spam heuristics: line-fragmentation detection (common in propaganda/disinformation)
- PROPAGANDA category with broad Hebrew patterns (authority-undermining, flee/survival advice)
- Year in date headers when messages are from a different year (chat screen and conversation list)

## [3.15.1] - 2026-03-07

### Changed
- Add "Premium" label with gold styling to Conversation Backgrounds in Themes screen

## [3.15.0] - 2026-03-07

### Added
- Hebrew spam patterns: loan (קח הלוואה, בתנאים מיוחדים), OTP (קוד זמני, הזן את הקוד), delivery (איסוף, משלוח ממתין)
- Hebrew heuristics: פעולה מיידית, יש לך הודעה, לחץ כאן
- Quick replies disabled by default; enable in Settings → Notifications
- Conversation background preview: tap a background to see preview before applying
- Conversation backgrounds require Premium; non-premium users see upgrade prompt
- Notification "Report spam" action when message is low-confidence spam
- Spam Folder shows why each message was flagged (rule type or category)
- International sender filter (opt-in): hide messages from numbers outside your country
- Hebrew strings for spam UI (values-he/strings.xml)
- Category-specific auto-block thresholds: OTP/Phishing 0.80, Political 0.95
- Alphanumeric sender scrutiny: boost spam score for sender IDs like FREETAX, AMAZON

### Changed
- Themes: tapping "Conversation Backgrounds" when not Premium navigates to License screen

## [3.13.0] - 2026-03-07

### Added
- 10 background images for conversations: Floral, Geometric, Leaves, Dots, Waves, Abstract, Minimal Lines, Nature, Marble, Stars (Settings → Themes → Conversation Backgrounds)

## [3.12.0] - 2026-03-07

### Added
- Conversation backgrounds: choose from 15 presets (Sky Blue, Sunset, Ocean, Aurora, Midnight, Forest, Lavender, Minimal White/Gray, Warm Sand, Coral, Mint, Storm, Twilight) under Settings → Themes

## [3.11.0] - 2026-03-07

### Added
- Removing a block rule now restores all associated messages from the spam folder back to the inbox

### Fixed
- "Block by sender" in chat conversation now works for unknown/alphanumeric senders (e.g. "FREETAX")

### Changed
- Inbox message preview now shows 2 lines instead of 1 for better readability

## [3.10.4] - 2026-03-07

### Changed
- "Report spam" now blocks the sender, moves the entire conversation to the spam folder, and navigates back to inbox

## [3.10.3] - 2026-03-07

### Fixed
- Scam warning no longer reappears on messages already reported as spam (persisted across chat re-entry)

## [3.10.2] - 2026-03-07

### Fixed
- Hebrew spam rules made more flexible to catch natural text variations
- Tax refund rules now accept singular/plural (החזר/החזרי) and bare numbers without shekel sign
- Political spam: "איזו ממשלה" no longer requires exact verb form after it
- "יו"ר" regex accepts both regular quotes and Hebrew gershayim (״)
- Medical/disability rules split into independent patterns for better partial matching

### Added
- Political spam detection for Haredi/draft topics, law-enables-evasion clickbait, and named-person exposé patterns
- Broader "tax refund + large number" rule for messages without shekel abbreviation
- Standalone disability parking tag (תו נכה) and combo pattern detection

## [3.10.1] - 2026-03-07

### Fixed
- Hebrew tax refund scam regex now matches both word orders ("החזרי מס ממוצע" and "ממוצע החזרי מס")
- Shekel abbreviation ש"ח (with gershayim) now recognized in currency heuristic and loan scam rules
- Added broader tax refund detection rules for shekel amounts and "citizens of Israel" phrasing

## [3.10.0] - 2026-03-07

### Added
- Hebrew spam detection: phishing, OTP fraud, delivery, loan scams in Hebrew
- Israel-specific scam categories: tax refund, pension/severance, political spam, "money waiting," medical/disability parking
- Hebrew and Israeli heuristic word lists for urgency and personal-info phrases
- Keyword learning now supports Hebrew and other non-Latin scripts (Unicode letters kept)
- Shekel (₪, שח) in currency heuristics

### Fixed
- ALL CAPS heuristic no longer triggers false positives for Hebrew (Hebrew has no uppercase; only Latin words are counted)

### Added
- Unknown sender action banner in chat: Trust or Block an unsaved contact with one tap
- Banner auto-hides for saved contacts and already-trusted senders
- Trusting from the banner also clears all scam warnings in the conversation

## [3.9.0] - 2026-03-04

### Added
- Dedicated Trusted Senders screen accessible from Smart & Secure (alongside Blocking and Spam Folder)
- Manual add dialog to trust any phone number or sender name via the + button
- Remove button on each trusted sender to revoke trust and re-enable spam checks

### Changed
- Trusted Senders moved from inline section to its own navigable screen

## [3.8.0] - 2026-03-04

### Added
- Sender allowlist: tapping "Not spam" on any message permanently trusts that sender
- All scam warnings in the conversation are dismissed immediately when one message is marked safe
- Future messages from allowlisted senders skip spam analysis entirely

### Changed
- Database upgraded to version 10 with new `sender_allowlist` table

## [3.7.1] - 2026-03-03

### Added
- Show upgrade-to-premium dialog when free users hit the block rule limit from any screen
- Navigate directly to Premium purchase screen from the limit dialog

### Changed
- Block actions in Chat screen now wait for result before navigating back

## [3.7.0] - 2026-03-03

### Changed
- Increase free block rule limit from 5 to 15
- Enforce block rule limit at repository layer across all code paths (Chat, Conversations, auto-block)
- Show "Premium Account" instead of "Upgrade to Premium" in Settings for premium users

### Fixed
- Free users could bypass block rule limit by blocking from Chat or Conversations screens

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
