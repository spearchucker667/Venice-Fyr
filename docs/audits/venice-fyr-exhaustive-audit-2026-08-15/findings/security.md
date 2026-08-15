# Security + Privacy Findings

## SEC-01 | Redactor is dead production code

**ID:** SEC-01 | **Severity:** P1 | **Status:** CONFIRMED | **Area:** Logging / Secret redaction | **Module:** `:core:common`

**File:** `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt`  
**Lines:** 8–16  
**Symbol:** `Redactor.redact`

**Evidence:**
- `Redactor.kt:8–16` defines `object Redactor` with `redact(value: String): String`.
- Repo-wide grep for `Redactor.redact` or `import ... Redactor` returns only `RedactorTest.kt:10`.
- `AGENTS.md` states: "No raw prompt/response/API-key logging."

**Expected:** Production code invokes `Redactor.redact` before emitting diagnostics, crash reports, logs, or error surfaces that may contain headers, payloads, or paths.

**Actual:** `Redactor` is only exercised by its own unit test. No production caller redacts anything.

**Impact:** The project's anti-logging rule is unenforced. Any future logging or diagnostic export added without calling `Redactor` will leak secrets, local paths, and potentially prompt/response bodies.

**Root cause:** `Redactor` was implemented as a standalone utility but never integrated into `:app`, `:venice-sdk`, or `:core:*` error/log paths.

**Related occurrences:** None in production code; only `RedactorTest.kt:10`.

**Venice reference:** N/A (project policy).

**Android/Kotlin reference:** N/A.

**Remediation:**
- Wire `Redactor.redact` into all exception-message and log surfaces in `:venice-sdk` and `:app`.
- Add a lint/unit-test rule that fails if new `Log.`/`println`/diagnostic calls are added without redaction.

**Tests required:**
- Unit test that `VeniceSdkException.message` does not contain a sample API key after redaction.
- Unit test that `ChatViewModel`/`ImageViewModel` error states do not contain a sample API key.

**Compatibility impact:** None; additive internal change.

---

## SEC-02 | ChatClient embeds raw SSE payloads in error messages

**ID:** SEC-02 | **Severity:** P1 | **Status:** CONFIRMED | **Area:** Streaming / Error handling | **Module:** `:venice-sdk`

**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`  
**Lines:** 101–102, 112, 115  
**Symbol:** `parseChunks`

**Evidence:**
- `ChatClient.kt:101–102`: `?: return listOf(ChatStreamChunk.Error(null, "invalid SSE JSON: $payload"))`
- `ChatClient.kt:112`: `return listOf(ChatStreamChunk.Error(null, payload))`
- `ChatClient.kt:115`: `?: return listOf(ChatStreamChunk.Error(null, payload))`

**Expected:** Stream parse errors surface a safe, fixed message (e.g., "invalid stream event") and never echo the raw server payload, which may contain user prompts, model outputs, or provider error details.

**Actual:** Three error branches return the raw SSE `payload` verbatim inside `ChatStreamChunk.Error.message`. That message is then propagated to `ChatViewModel` and rendered in the UI (`ChatViewModel.kt:180`).

**Impact:** User prompts and assistant responses can leak into UI error state and any future diagnostics/crash logs. If a Venice error response ever echoes request metadata, the API key could also be reflected.

**Root cause:** Defensive parsing falls back to echoing the unparseable payload instead of a constant safe message.

**Related occurrences:**
- `ChatClient.kt:87`: `trySend(ChatStreamChunk.Error(code = null, message = e.message ?: ...))` — network/IO exception messages are also forwarded verbatim.
- `ImageViewModel.kt:78,118` and `ChatViewModel.kt:180` render `e.message` in UI state.

**Venice reference:** N/A (transport-layer behavior).

**Android/Kotlin reference:** N/A.

**Remediation:**
- Replace `"invalid SSE JSON: $payload"` and raw `payload` returns with a constant safe message.
- If payload must be retained for debugging, store it in a non-message field and redact before logging/display.
- Sanitize `e.message` from network exceptions before surfacing in UI state.

**Tests required:**
- Unit test that malformed SSE payloads do not appear in `ChatStreamChunk.Error.message`.
- Unit test that network exceptions with synthetic messages containing an API key are not echoed.

**Compatibility impact:** Changes the public `ChatStreamChunk.Error.message` content for malformed events; consumers relying on payload echo will break.

---

## SEC-03 | SecureSecretStore has no unit tests

**ID:** SEC-03 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Credential persistence / Test coverage | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 1–100  
**Symbol:** `SecureSecretStore`

**Evidence:**
- `core/security/src/**/*.kt` glob returns only the main `SecureSecretStore.kt`.
- `core/security/build.gradle.kts:17` declares `testImplementation(libs.junit)` but no test sources exist.

**Expected:** A security-critical component that manages API keys should have unit tests covering save/load/delete, corruption handling, key-rotation/upgrade scenarios, and Keystore unavailability.

**Actual:** No tests exist for `SecureSecretStore`.

**Impact:** Regressions in Keystore-backed encryption, IV handling, or corruption logic could go undetected. The component is also difficult to validate on Robolectric because Android Keystore is hardware-backed in many test environments.

**Root cause:** `:core:security` module was created without a corresponding test source set.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android Keystore behavior is vendor-specific and best verified with tests.

**Remediation:**
- Add `SecureSecretStoreTest` using a fake `KeyStore` provider or Robolectric + Android Keystore shadow where available.
- Cover: round-trip save/load, delete, tampered ciphertext, wrong IV, blank inputs, and multiple profiles.

**Tests required:** New `SecureSecretStoreTest`.

**Compatibility impact:** None.

---

## SEC-04 | SecureSecretStore deletes ciphertext on any decryption failure

**ID:** SEC-04 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Credential persistence / Availability | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 38–55  
**Symbol:** `loadApiKey`

**Evidence:**
- `SecureSecretStore.kt:50–53`:
  ```kotlin
  }.getOrElse {
      // Treat undecryptable/corrupt ciphertext as unavailable and remove it.
      prefs.edit().remove(prefKey(profileId)).apply()
      null
  }
  ```

**Expected:** Decryption failures should return `null` without mutating stored ciphertext, or at minimum require user confirmation before deleting the only copy of an encrypted secret.

**Actual:** Any exception during decryption (corruption, Keystore key invalidation after biometric/lock-screen change, vendor Keystore bug, tampering) silently deletes the stored ciphertext.

**Impact:** A malicious or buggy caller, a flaky Keystore, or a device migration can permanently destroy the user's stored API key with no audit trail and no way to recover. This is a denial-of-availability vulnerability for credentials.

**Root cause:** The recovery path conflates "return null" with "delete stored data".

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android Keystore keys can be invalidated by lock-screen changes, biometric enrollment changes, or device migration; see `KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment` and related semantics.

**Remediation:**
- Remove the `prefs.edit().remove(...)` call from the catch block; return `null` and surface a distinct error to the UI.
- If deletion is desired, expose a separate `deleteApiKey` call and require explicit user action.

**Tests required:**
- Unit test that a decryption failure does not remove the stored preference entry.

**Compatibility impact:** Changes observable behavior: failed loads will no longer erase keys. This is the intended safer behavior.

---

## SEC-05 | API key held in Compose-managed mutable state

**ID:** SEC-05 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** UI / Memory exposure | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt`  
**Lines:** 49, 59, 95–96, 108  
**Symbol:** `apiKey` state

**Evidence:**
- `ConfigScreen.kt:49`: `var apiKey by remember { mutableStateOf("") }`
- `ConfigScreen.kt:59`: `apiKey = existing` (loaded decrypted key copied into state)
- `ConfigScreen.kt:95–96`: `OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, ...)`
- `ConfigScreen.kt:108`: `secureStore.saveApiKey(profileId, apiKey)`

**Expected:** Sensitive credentials should be cleared from memory as soon as they are persisted, and should not be retained in UI state longer than necessary.

**Actual:** The decrypted API key lives in a Compose `mutableStateOf` for the lifetime of the screen composition. It survives configuration changes and is present in the composition's snapshot state.

**Impact:** The key is exposed in memory for the duration of the Settings screen, increasing the window for memory dumps, screenshots, and process-level attacks. It also means the key is present in Compose tooling/state inspection.

**Root cause:** Direct two-way binding of the secret text field to a plain `String` state.

**Related occurrences:** None; other screens use `apiKeyProvider: () -> String?` and do not retain the key in state.

**Venice reference:** N/A.

**Android/Kotlin reference:** Compose `TextFieldValue`/state is held in the composition; see Android security best practice to minimize secret lifetime in memory.

**Remediation:**
- Use a sealed input state (e.g., `Boolean hasKey` + `TextFieldValue` for transient entry) and clear the input buffer immediately after save.
- Load the existing key only to determine whether a key is present, not to populate the text field.

**Tests required:**
- UI test asserting the input buffer is cleared after "Save".

**Compatibility impact:** None; internal UI-state change.

---

## SEC-06 | No duplicate-submission/idempotency defenses for paid/mutating operations

**ID:** SEC-06 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Paid/mutating operations | **Module:** `:app`, `:venice-sdk`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
**Lines:** `ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121`  
**Symbol:** `submit`, `generateImage`, `editImage`

**Evidence:**
- `ChatViewModel.kt:86–187`: `submit(text)` launches a new coroutine and chat stream without any idempotency key or debounce.
- `ImageViewModel.kt:52–121`: `generateImage()` / `editImage()` immediately call `imageClient.generateBinary(...)` / `imageClient.edit(...)` on every UI trigger.
- `AGENTS.md`: "Paid/mutating operations require explicit approval and duplicate-submission defenses."

**Expected:** Mutating/paid operations (chat completion, image generation, video queue) should include an idempotency key and/or debounce to prevent accidental double billing from rapid taps, configuration changes, or retry logic.

**Actual:** No idempotency keys, no debounce, and no request deduplication. Each tap creates a new paid request.

**Impact:** Users can be billed multiple times for the same operation due to accidental double-tap, process death/recreation, or aggressive retry.

**Root cause:** UI/ViewModel layer was scaffolded without transaction semantics.

**Related occurrences:**
- `VideoClient.kt:19–118` exposes queue/complete/retrieve methods with no idempotency parameter.
- `AudioClient.kt:19–45` exposes `speech` with no idempotency parameter.

**Venice reference:** `swagger.yaml` supports `idempotency_key` style headers for some endpoints; verify per endpoint.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Generate a stable idempotency key per user action (e.g., UUID + action hash) and send it in the `Idempotency-Key` header where Venice supports it.
- Disable action buttons while a request is in flight.

**Tests required:**
- Unit test that rapid double-tap results in exactly one network request.
- Unit test that idempotency key is stable across configuration change.

**Compatibility impact:** May require adding an `idempotencyKey` parameter to SDK methods; additive if defaulted.

---

## SEC-07 | No explicit user approval before paid/mutating operations

**ID:** SEC-07 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Paid/mutating operations / UX | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
**Lines:** `ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121`  
**Symbol:** `submit`, `generateImage`, `editImage`

**Evidence:**
- `ChatViewModel.kt:86–187`: `submit(text)` sends the request immediately after model validation.
- `ImageViewModel.kt:52–121`: `generateImage()` / `editImage()` send requests immediately after input validation.
- `AGENTS.md`: "Paid/mutating operations require explicit approval and duplicate-submission defenses."

**Expected:** Before any operation that consumes API credits (chat, image, audio, video), the user should confirm the action, especially when costs may be high (image generation, video queue).

**Actual:** No confirmation dialog or explicit approval step exists.

**Impact:** Accidental submissions can incur real cost; violates the project's stated approval boundary.

**Root cause:** Feature scaffolding omitted confirmation UX.

**Related occurrences:**
- `FeatureCatalog.kt:39` notes "confirmations must remain explicit for mutating/paid operations" for future Workflows feature, but current Generate/Chat features lack them.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Add a confirmation dialog for image/video/audio generation and for the first chat submission in a session.
- Persist a user preference to skip confirmations for low-cost actions if desired.

**Tests required:**
- UI test that confirmation dialog is shown and can be confirmed/cancelled.

**Compatibility impact:** None; UX addition.

---

## SEC-08 | Redactor regex may miss non-sk/vn Venice key formats

**ID:** SEC-08 | **Severity:** P2 | **Status:** INFERRED | **Area:** Logging / Secret redaction | **Module:** `:core:common`

**File:** `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt`  
**Lines:** 10  
**Symbol:** `apiKey` regex

**Evidence:**
- `Redactor.kt:10`: `private val apiKey = Regex("(?i)\\b(?:sk|vn)-[A-Za-z0-9._-]{8,}\\b")`
- Venice API documentation and desktop source may use other prefixes or bare token formats.

**Expected:** Redaction patterns should match all documented Venice API key formats.

**Actual:** Only keys beginning with `sk-` or `vn-` are matched. A Venice key using a different prefix or a raw alphanumeric token would not be redacted.

**Impact:** If `Redactor` is ever wired into production logs (see SEC-01), non-matching keys will leak.

**Root cause:** Regex derived from common OpenAI/Venice prefixes without confirming the full Venice key alphabet.

**Related occurrences:** None.

**Venice reference:** Verify against `swagger.yaml` `securitySchemes` and `api-keys` guide.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Confirm the exact Venice API key format(s) from `venice-api-docs` and update the regex.
- Consider redacting the entire `Authorization` header value as a fallback.

**Tests required:**
- Unit tests with representative real Venice key shapes.

**Compatibility impact:** None.

---

## SEC-09 | ViewModels surface raw exception messages in UI state

**ID:** SEC-09 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Error handling / UI | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
**Lines:** `ImageViewModel.kt:78,118`, `ChatViewModel.kt:180`  
**Symbol:** `error = e.message`

**Evidence:**
- `ImageViewModel.kt:78`: `_uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }`
- `ImageViewModel.kt:118`: same pattern.
- `ChatViewModel.kt:180`: `_state.update { it.copy(isStreaming = false, error = chunk.message) }`

**Expected:** Error messages displayed to users should be sanitized and not expose internal details, network paths, or potentially reflected secrets.

**Actual:** Raw exception messages from the SDK/network layer are displayed verbatim in the UI.

**Impact:** If any downstream exception message contains sensitive data (e.g., a proxy error, a reflected header, or a malformed SSE payload per SEC-02), it will be shown to the user and be eligible for screenshots/accessibility logs.

**Root cause:** Direct mapping of `Throwable.message` / `ChatStreamChunk.Error.message` to UI state without sanitization.

**Related occurrences:**
- `ConfigScreen.kt:136`: `status = "Model probe failed: ${it.message ?: it::class.simpleName}"`

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Map exceptions to user-safe, localized error strings.
- If detailed messages are needed for support, route them through `Redactor` and keep them out of UI state.

**Tests required:**
- Unit test that a synthetic exception message containing an API key is not displayed in UI state.

**Compatibility impact:** User-facing error strings change; positive UX improvement.

---

## SEC-10 | SecureSecretStore uses truncated SHA-256 digest for alias derivation

**ID:** SEC-10 | **Severity:** P3 | **Status:** INFERRED | **Area:** Credential persistence / Cryptography | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 85–91  
**Symbol:** `alias`, `prefKey`, `digest`

**Evidence:**
- `SecureSecretStore.kt:88–91`:
  ```kotlin
  private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
      .digest(value.toByteArray(StandardCharsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
      .take(32)
  ```

**Expected:** Keystore alias and preference key should uniquely identify the profile without unnecessary collision risk.

**Actual:** The full 64-character SHA-256 hex digest is truncated to 32 characters (128 bits). While still collision-resistant for a small number of profiles, it is weaker than the full digest and unnecessary.

**Impact:** Negligible in practice for this app, but it reduces the security margin and deviates from standard practice of using the full hash.

**Root cause:** Explicit `.take(32)` truncation.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Use the full 64-character SHA-256 hex digest, or switch to a deterministic encoding of the profile ID with appropriate length.
- Note: changing alias derivation will invalidate existing stored keys; migration logic is required.

**Tests required:**
- Unit test that alias/prefKey are stable and unique per profile ID.

**Compatibility impact:** Changing alias derivation requires a migration path for existing users.

---

## Positive Findings

### SEC-POS-01 | SDK never persists API keys

**Status:** CONFIRMED | **Module:** `:venice-sdk`

- `VeniceForgeSdk.kt:24–26` documents that the SDK "intentionally does not persist API keys."
- All SDK methods (`listModels`, `getRaw`, `streamChat`, `generate`, `speech`, `queue`, etc.) accept `apiKey` as a parameter and do not store it in fields or files.
- No `SharedPreferences`, `DataStore`, or file writes of `apiKey` were found in `:venice-sdk`.

### SEC-POS-02 | Encrypted credential storage

**Status:** CONFIRMED | **Module:** `:core:security`

- `SecureSecretStore.kt:30–35` encrypts the API key with an AES-GCM-256 key from Android Keystore and stores only `iv || ciphertext` Base64 in `SharedPreferences`.
- `SecureSecretStore.kt:65–81` creates Keystore keys with `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, and 256-bit key size.

### SEC-POS-03 | Backup and cleartext correctly disabled

**Status:** CONFIRMED | **Module:** `:app`

- `app/src/main/AndroidManifest.xml:5–6`: `android:allowBackup="false"`, `android:fullBackupContent="false"`.
- `app/src/main/AndroidManifest.xml:13`: `android:usesCleartextTraffic="false"`.
- `app/src/main/res/xml/network_security_config.xml:2`: `<base-config cleartextTrafficPermitted="false" />`.

### SEC-POS-04 | Minimal exported surface

**Status:** CONFIRMED | **Module:** `:app`

- Only `MainActivity` is exported (`android:exported="true"`) and it has the standard `MAIN`/`LAUNCHER` intent filter.
- No `FileProvider`, `WebView`, `setJavaScriptEnabled`, or additional exported components were found.

### SEC-POS-05 | No telemetry or analytics dependencies

**Status:** CONFIRMED | **Module:** repo-wide

- `gradle/libs.versions.toml` contains no Firebase, Crashlytics, Bugsnag, Sentry, Mixpanel, Amplitude, or other telemetry/analytics/crash-reporting libraries.
- Repo-wide grep for telemetry/analytics terms returned only the harmless endpoint constant `BILLING_USAGE_ANALYTICS`.

### SEC-POS-06 | No runtime credential logging

**Status:** CONFIRMED | **Module:** repo-wide

- Repo-wide grep for `Log.`, `println`, `Timber`, `Logger`, `logcat` returned no production-code matches.
- `VeniceSdkException` messages are built from status codes, safe server messages, and request IDs; tests assert keys are not leaked.

### SEC-POS-07 | No hard-coded secrets in tracked files

**Status:** CONFIRMED | **Module:** repo-wide

- Git-tracked files were scanned for `Bearer <token>`, `sk-...`, `vn-...`, and other high-entropy secret-like strings; no production secrets were found.
- `.gitignore` excludes `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `local.properties`, `.local/`, and `.source/`.
