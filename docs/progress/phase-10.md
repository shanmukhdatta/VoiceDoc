# Phase 10 — Consent-based encrypted local history
**STATUS: Implemented, Android instrumentation + UI testing required.**

- AndroidKeyStore-backed AES-256/GCM authenticated encryption with per-write nonce, versioned ciphertext, bounded message counts/lengths.
- Encrypted file in `noBackupFilesDir`, with app backup disabled at manifest level. No plaintext history files or Android INTERNET permission.
- Persistent opt-in preference + Settings enable/disable/delete UX; History screen displays entries only when consent is enabled; chat persists only when opted in; clear session writes current consented empty state.
- Failure to read or save encrypted store is surfaced as a warning; code never silently uploads or trains on messages.
- Android instrumentation tests: encrypted round-trip, non-readable ciphertext, delete and message-size bounds.
- Security limitations: not yet independently audited, no key migration/rotation, no storage-retention schedule, deletion does not guarantee forensic erasure on flash, secure-screen display behavior not hardened, Android encryption is only as trustworthy as device OS/hardware. Do not claim formal privacy certification.
- Model manifest SHA-256 helper already implemented in Phase 03; full asset lifecycle/import manager remains pending.
- Acceptance: APK compiles, emulator test passes, opt-in persists across restart, disable deletes ciphertext, no health text in logs or backups.
