# Security and Privacy

Confirmed: no WebView, no telemetry SDK, no broad storage permission, cleartext disabled, backup disabled, only launcher activity exported, SDK does not persist keys, credentials use Keystore-backed AES/GCM ciphertext, Room is app-private but unencrypted, and stream-side error text is redacted.

Open: Config keeps plaintext key text in Compose state while visible; future diagnostics/export must be redacted by construction; media reads need size bounds; app queue/job state must never persist API keys; no live certificate/auth test was performed. The removed `enable_e2ee` field prevents callers from believing an unimplemented toggle provides encryption.
