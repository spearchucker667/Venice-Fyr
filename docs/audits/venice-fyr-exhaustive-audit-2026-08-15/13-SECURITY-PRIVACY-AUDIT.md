# 13 — Security + Privacy Audit

**Auditor:** Security + Privacy  
**Scope:** `core/security/SecureSecretStore.kt`, `core/common/Redactor.kt` + tests, all `AndroidManifest.xml` files, `app/src/main/res/xml/network_security_config.xml`, end-to-end API-key flow (`ConfigScreen` → storage → SDK transport). Repo-wide greps for `Log.`, `println`, `Bearer`, `Authorization`, `apiKey`, `BuildConfig`, `SharedPreferences`, `DataStore`, `exported=`, `FileProvider`, `WebView`, `setJavaScriptEnabled`, `cleartext`, `allowBackup`.  
**Branch:** `main` @ `1da3142`  
**Date:** 2026-08-15

---

## Methodology

- Static analysis only; no Gradle builds or tests executed per audit rules.
- Read every line of every file in scope.
- Cross-checked behavior against `AGENTS.md` security boundaries and the Venice API source-of-truth (`swagger.yaml`, upstream HEAD `6e69346b`, info.version `20260814.194349`).
- Grep'd the full repository for credential-related patterns and telemetry/analytics indicators.
- Classified every conclusion as **CONFIRMED**, **INFERRED**, **SUSPECTED**, or **UNVERIFIED**.

---

## Ledger

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt` | 100 | Y | 3 |
| `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt` | 17 | Y | 2 |
| `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt` | 15 | Y | 1 |
| `app/src/main/AndroidManifest.xml` | 23 | Y | 0 |
| `venice-sdk/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| `core/data/src/main/AndroidManifest.xml` | 2 | Y | 0 |
| `core/designsystem/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| `core/security/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| `core/common/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| `app/src/main/res/xml/network_security_config.xml` | 3 | Y | 0 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` | 173 | Y | 2 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 266 | Y | 0 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` | 200 | Y | 1 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 122 | Y | 1 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | Y | 1 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 105 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` | 152 | Y | 2 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt` | 46 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt` | 119 | Y | 0 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` | 121 | Y | 0 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 161 | Y | 0 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt` | 220 | Y | 0 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt` | 105 | Y | 0 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt` | 102 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt` | 27 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt` | 14 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` | 47 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` | 22 | Y | 0 |
| `app/build.gradle.kts` | 68 | Y | 0 |
| `venice-sdk/build.gradle.kts` | 29 | Y | 0 |
| `core/security/build.gradle.kts` | 18 | Y | 0 |
| `core/data/build.gradle.kts` | 56 | Y | 0 |
| `gradle/libs.versions.toml` | 58 | Y | 0 |
| `venice-sdk/consumer-rules.pro` | 1 | Y | 0 |
| `build.gradle.kts` | 5 | Y | 0 |
| `.gitignore` | 19 | Y | 0 |

---

## Summary

| Severity | Count |
|----------|-------|
| P0 | 0 |
| P1 | 1 |
| P2 | 7 |
| P3 | 1 |

**Positive findings (CONFIRMED):**
- `:venice-sdk` never persists API keys; keys are supplied per request by callers.
- `SecureSecretStore` encrypts API keys with Android Keystore AES-GCM-256 and stores only ciphertext in `SharedPreferences`.
- `AndroidManifest.xml` correctly sets `android:allowBackup="false"`, `android:usesCleartextTraffic="false"`, and references a `network_security_config.xml` that denies cleartext traffic globally.
- Only `MainActivity` is exported; no `FileProvider`, `WebView`, `setJavaScriptEnabled`, or cleartext overrides were found.
- No `Log.`, `println`, `Timber`, or other runtime logging of credentials was found in production code.
- No telemetry, analytics, crash-reporting, or advertising dependencies are declared in `gradle/libs.versions.toml`.
- No hard-coded API keys or secrets were found in git-tracked source files.
- `VeniceSdkException` messages are constructed from status codes, safe server messages, and request IDs; the SDK tests assert that exception messages do not contain the API key.

**Key concerns:**
- `Redactor` is defined but never wired into production code, so the project's stated "no raw prompt/response/API-key logging" rule has no enforcement mechanism.
- `ChatClient` embeds raw SSE payloads in `ChatStreamChunk.Error` messages, creating a channel for prompt/response leakage.
- `SecureSecretStore` has zero unit tests and deletes ciphertext on any decryption failure, enabling trivial denial-of-key-availability attacks.
- Paid/mutating operations (image generation, video queue, chat) are invoked directly from UI without explicit user approval or duplicate-submission defenses.

---

## Detailed Findings

See [`findings/security.md`](findings/security.md) for the full finding records in the required format.

### Most Important Findings

1. **SEC-01** — `Redactor` is dead code; no production path redacts secrets in logs/errors (P1).
2. **SEC-02** — `ChatClient` includes raw SSE payloads in stream error messages (P1).
3. **SEC-03** — `SecureSecretStore` has no unit tests (P2).
4. **SEC-04** — `SecureSecretStore` deletes stored ciphertext on any decryption failure (P2).
5. **SEC-05** — API key lives in Compose-managed `remember` state in `ConfigScreen` (P2).
6. **SEC-06** — No duplicate-submission/idempotency defenses for paid/mutating operations (P2).
7. **SEC-07** — No explicit user-approval UI before paid/mutating operations (P2).
8. **SEC-08** — `Redactor` regex only covers `sk-`/`vn-` prefixes and may miss other Venice key formats (P2).
9. **SEC-09** — `ImageViewModel` and `ChatViewModel` surface raw exception messages in UI state (P2).
10. **SEC-10** — `SecureSecretStore` uses a truncated SHA-256 digest for Keystore alias derivation (P3).
