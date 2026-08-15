# Android Security and Storage Contract

1. API keys are app-owned secrets. `:venice-sdk` accepts credentials for calls but must never persist them.
2. API keys are encrypted with Android Keystore-backed storage. Room stores prompts, responses, and other app data unencrypted inside the app-private Android sandbox; do not represent that database as encrypted. Never store API keys in DataStore, Room, or plain preferences.
3. Credentials stay in credential-protected storage. Do not use Direct Boot/device-protected storage for tokens/passwords.
4. Do not log raw Authorization headers, API keys, prompt bodies, response bodies, attachment content, or full local URIs/paths.
5. All provider endpoints default to HTTPS. Cleartext networking remains disabled.
6. External file access uses scoped Android APIs: Photo Picker, SAF Open/CreateDocument, and persisted OpenDocumentTree grants.
7. Do not request `MANAGE_EXTERNAL_STORAGE`. Do not add legacy broad storage permissions unless a future, documented requirement proves they are unavoidable.
8. Generated media is written to app-private storage first, validated (MIME/signature/size/hash), then explicitly exported by the user.
9. Paid or irreversible operations require an explicit confirmation layer matching desktop intent.
10. The local Family Safe Mode is an app policy and remains distinct from Venice API `safe_mode`. Provider fields must preserve explicit user/app choices exactly.
11. Profile boundaries apply to secrets, conversations, media metadata, prompts, documents, backups, settings, and background jobs.
12. Diagnostics exports are redacted by construction and exclude raw prompt/response payloads.
13. No telemetry/crash analytics SDK is introduced without an explicit product decision and user-facing privacy update.
