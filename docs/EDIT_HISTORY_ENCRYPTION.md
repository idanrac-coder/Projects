# Edit History Encryption Strategy

## Context

NovaChat stores RCS message edit history in a local Room database table (`message_edits`).
Each row contains the previous body text, the new body text, and a timestamp.
Currently, message content (both in the Android SMS provider and in our Room tables) is
stored in plaintext. When E2EE is added to NovaChat, the edit history must be encrypted
with the same guarantees.

## Goals

1. **Confidentiality**: Edit history content must be unreadable without the correct session key.
2. **Integrity**: Tampered ciphertext must be detectable (authenticated encryption).
3. **Consistency**: The same key material and protocol used for the main message body must
   protect its edit history entries.
4. **Performance**: Encryption/decryption of edits must complete in <5 ms per entry on mid-range devices.

## Proposed Architecture

### Key Hierarchy

```
Identity Key (per-device, Ed25519)
  └── Session Key (per-conversation, X25519 + Double Ratchet)
        └── Message Key (per-message, derived via KDF chain)
              └── Edit Sub-Key = HKDF(message_key, "edit-history", edit_index)
```

Each edit derives its own sub-key from the parent message key so that:
- Knowing an edit sub-key does not reveal other edits or the original message key.
- The edit index prevents key reuse across edits of the same message.

### Encryption Scheme

- **Algorithm**: AES-256-GCM (authenticated encryption with associated data).
- **Nonce**: 12-byte random, stored alongside the ciphertext.
- **AAD**: `messageId || editIndex || timestamp` — binds ciphertext to its metadata.
- **Library**: Android Keystore for key protection; Tink or libsignal for crypto primitives.

### Database Schema Changes

When E2EE is enabled, `message_edits` columns change:

| Column            | Current (plaintext) | Future (encrypted)                     |
|-------------------|---------------------|----------------------------------------|
| `previousBody`    | TEXT                | BLOB (nonce + ciphertext + tag)        |
| `newBody`         | TEXT                | BLOB (nonce + ciphertext + tag)        |
| `editKeyIndex`    | —                   | INTEGER (edit sub-key derivation index) |
| `encryptionVersion` | —                 | INTEGER (protocol version for migration)|

### Encryption Flow (Write)

1. User edits a message within the 15-minute window.
2. `ConversationRepository.saveMessageEdit()` is called.
3. Look up the **message key** for the original message from the E2EE session store.
4. Derive `editSubKey = HKDF-SHA256(messageKey, "edit-history" || editIndex)`.
5. Encrypt `previousBody` and `newBody` separately with AES-256-GCM using `editSubKey`.
6. Store the ciphertext BLOBs + `editKeyIndex` in Room.

### Decryption Flow (Read)

1. User opens the "Message Details" / edit history sheet.
2. `ConversationRepository.getEditHistory()` fetches encrypted rows.
3. For each row, derive `editSubKey` from the stored message key + `editKeyIndex`.
4. Decrypt with AES-256-GCM; verify AAD matches.
5. Return plaintext `MessageEdit` domain objects to the UI.

### Key Storage

- **Session keys** are stored in an encrypted Room database or Android Keystore-backed file.
- **Message keys** are retained for the edit window (15 minutes) plus a configurable grace period
  (default: 24 hours) to allow reading recent edit history, then purged.
- After key purge, edit history entries become permanently unreadable (forward secrecy).

## Migration Plan

1. **Phase A** (current): All edit history stored in plaintext. No keys involved.
2. **Phase B** (E2EE MVP): New edits are encrypted; old plaintext rows are migrated in-place
   on first access using the conversation session key, then the plaintext columns are zeroed.
3. **Phase C** (post-migration): Schema drops plaintext columns entirely. Only encrypted BLOBs remain.

## Security Considerations

- **Forward secrecy for edits**: Once the message key is deleted after the grace period,
  the edit sub-keys cannot be re-derived. This is a feature, not a bug.
- **Backup/restore**: Encrypted edit history is included in backup, but requires the
  identity key to be restored alongside it. Standard key-backup flow applies.
- **Multi-device sync**: If implemented, edit history encryption keys must be part of the
  multi-device key sharing protocol (e.g., via QR code or secure channel).
- **No server-side access**: Since NovaChat is an on-device SMS app, there is no server.
  However, if cloud backup is added, the backup must be encrypted at rest with a
  user-derived passphrase (PBKDF2/Argon2 + AES-256-GCM).

## Dependencies

- `com.google.crypto.tink:tink-android` or `org.signal:libsignal-client`
- Android Keystore API (API 23+)
- Room schema migration (version N → N+1)

## Timeline

This strategy will be implemented when E2EE is added to NovaChat's core messaging pipeline.
The edit history table is designed to be forward-compatible with the encrypted schema.
