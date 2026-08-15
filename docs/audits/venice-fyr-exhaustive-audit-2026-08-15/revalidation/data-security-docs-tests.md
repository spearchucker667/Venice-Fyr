# Revalidation: Data / Security / Docs / Tests P1 Findings

**Scope:** Revalidate all P1 findings in `core-data.md`, `security.md`, `docs.md`, `tests.md`, and `hygiene.md` against current source at `main @ ee2cd7a` (production source identical to audited `1da3142`, plus coordinator's `import io.github.spearchucker667.veniceforge.sdk.image.ImageClient` compile fix in `VeniceForgeSdk.kt`).

**Methodology:** Static/read-only analysis only. No Gradle commands executed. Sources inspected with `Read` and `Grep`. Venice API source-of-truth: `.source/venice-api-docs/` (upstream `6e69346b`, swagger `info.version 20260814.194349`).

**Note on known false finding:** ARCH-02 is acknowledged as FALSE per coordinator briefing; it is outside this revalidation scope.

---

## Disposition summary

| ID | Original severity/status | Disposition | Corrected severity/status |
|---|---|---|---|
| DATA-03 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| DATA-04 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| DATA-05 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| DATA-06 | P2 / CONFIRMED | RECLASSIFIED | P1 / CONFIRMED |
| DATA-09 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| SEC-01 | P1 / CONFIRMED | RECLASSIFIED | P2 / CONFIRMED |
| SEC-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| DOC-01 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| DOC-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-FIXTURE-01 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-HARDCODE-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-COVERAGE-03 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-COVERAGE-04 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-MISSING-06 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-MISSING-18 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| TEST-MISSING-19 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |

---

## Core Data (DATA)

### DATA-03 | P1 | VALID

**Original:** `ProfileRepository.ensureDefault` is not atomic; concurrent callers can race and the second `insert` throws `SQLiteConstraintException`.

**Source evidence:**
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt:7–20`:
  ```kotlin
  class ProfileRepository(private val dao: ProfileDao) {
      suspend fun ensureDefault(): String {
          dao.findDefault()?.let { return it.id }
          val now = System.currentTimeMillis()
          val entity = ProfileEntity(...)
          dao.insert(entity)
          return DEFAULT_PROFILE_ID
      }
  }
  ```
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt` uses `@Insert(onConflict = OnConflictStrategy.ABORT)` (confirmed by schema/DAO review).

**Why original is right:** The read-then-write is unwrapped; two coroutines can both see `findDefault() == null` and the second `insert` aborts. No `withTransaction`, no `INSERT OR IGNORE`, no retry.

**Correct remediation:** Wrap `findDefault` + `insert` in `db.withTransaction`, or change `ProfileDao.insert` to `OnConflictStrategy.IGNORE` and loop until `findDefault()` returns non-null.

**Tests required:** Concurrent `ensureDefault()` calls must produce exactly one profile without exceptions.

---

### DATA-04 | P1 | VALID

**Original:** `ChatRepository.appendMessage` does not update the parent conversation's `updatedAt`, so `observeConversations` ordering is stale.

**Source evidence:**
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt:45–54`:
  ```kotlin
  suspend fun appendMessage(profileId: String, conversationId: String, message: MessageEntity) {
      ...
      db.withTransaction {
          require(conversationDao.findById(profileId, conversationId) != null) { ... }
          messageDao.upsert(message)
      }
  }
  ```
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt:26–27`:
  ```kotlin
  @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC")
  fun observeForProfile(profileId: String): Flow<List<ConversationEntity>>
  ```

**Why original is right:** The transaction touches only `messages`; `conversations.updatedAt` is never assigned after creation. `observeForProfile` sorts by `updatedAt DESC`, so a conversation with newer messages can appear below a conversation created later.

**Correct remediation:** Inside the transaction, load the conversation, copy with `updatedAt = now`, and call `conversationDao.update(...)`.

**Tests required:** `ChatRepositoryTest` asserting `updatedAt` advances after `appendMessage`.

---

### DATA-05 | P1 | VALID

**Original:** `ChatRepository.updateAssistantText` has no transaction boundary and does not refresh the parent conversation's `updatedAt`.

**Source evidence:**
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt:56–69`:
  ```kotlin
  suspend fun updateAssistantText(...) {
      messageDao.updateTextAndStatus(
          profileId = profileId,
          id = messageId,
          text = text,
          status = status,
          updatedAt = System.currentTimeMillis(),
      )
  }
  ```
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt:23–24`:
  ```kotlin
  @Query("UPDATE messages SET textContent = :text, status = :status, updatedAt = :updatedAt WHERE id = :id AND profileId = :profileId")
  suspend fun updateTextAndStatus(...)
  ```

**Why original is right:** The method is a plain DAO call with no `db.withTransaction`; the conversation row is untouched. Streaming chunks therefore do not advance conversation ordering.

**Correct remediation:** Wrap in `db.withTransaction`; load the message's `conversationId`, then update both the message and the conversation's `updatedAt`.

**Tests required:** Test that `updateAssistantText` advances conversation `updatedAt` and that message + conversation updates are atomic.

---

### DATA-06 | P1 | RECLASSIFIED from P2

**Original (P2):** `ChatRepository.updateAssistantText` accepts `profileId` and `messageId` but not `conversationId`; the DAO WHERE clause is `id = :id AND profileId = :profileId`, allowing cross-conversation mutation within the same profile.

**Source evidence:**
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt:56–69` (signature lacks `conversationId`).
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt:23–24`:
  ```kotlin
  @Query("UPDATE messages SET textContent = :text, status = :status, updatedAt = :updatedAt WHERE id = :id AND profileId = :profileId")
  ```

**Why this is a P1:** A caller with a valid `messageId` from a different conversation in the same profile can corrupt that message's content/status. The impact is data corruption, not merely isolation hygiene. The original P2 undervalues the risk.

**Correct remediation:** Add `conversationId` to the method signature and include it in the DAO WHERE clause (or load the message and verify `conversationId` before updating).

**Tests required:** Negative test updating a message in the wrong conversation.

---

### DATA-09 | P1 | VALID

**Original:** `AppDatabase.create` builds a plaintext Room database while `docs/SECURITY_AND_STORAGE_CONTRACT.md` claims "App persistence uses Android Keystore-backed encryption."

**Doc claim:**
- `docs/SECURITY_AND_STORAGE_CONTRACT.md:2`: "App persistence uses Android Keystore-backed encryption."

**Code evidence:**
- `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt:36–43`:
  ```kotlin
  fun create(context: Context): AppDatabase =
      Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "venice_forge.db",
      )
          .build()
  ```
- No SQLCipher, `SupportFactory`, or Keystore-backed key is configured.

**Why original is right:** This is a direct code-vs-document contract conflict. Chat history, prompts, and responses are stored in plaintext SQLite in app-private storage.

**Correct remediation:** This is a product decision to escalate. Options:
1. Encrypt the Room database with SQLCipher + Keystore-derived key (and migrate existing data), or
2. Correct `docs/SECURITY_AND_STORAGE_CONTRACT.md` to state that only API keys are Keystore-encrypted and that local chat history is plaintext by design.

**Tests required:** If encryption is chosen, verify database file is not plaintext; key rotation/recovery tests. If docs are corrected, no code tests required.

---

## Security (SEC)

### SEC-01 | RECLASSIFIED from P1 to P2

**Original (P1):** `Redactor` is dead production code; it is only exercised by `RedactorTest.kt` and no production caller invokes it, so future logging will leak secrets.

**Revalidation evidence:**
- `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt:8–16` defines `Redactor.redact`.
- Repo-wide `Grep` for `\bLog\.|\bprintln\(|\bprintStackTrace\(|\bTimber\.|\bLogger\.|\blogcat` across all `*.kt` files returned **no production matches**.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt:1–105` builds messages from status codes, safe server messages, and request IDs only; no raw payloads or keys are included.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:141–202` parses HTTP errors into structured exceptions using `safeMessage` extracted from JSON `error.message`/`error.code`; the raw response body is not surfaced in exception messages.

**Why reclassified:** The finding is real as a latent design gap — `Redactor` is indeed unintegrated — but there is **no active production logging or telemetry path** that currently leaks secrets. The P1 severity assumed an active vulnerability; the current code has no such path. It remains a P2 maintainability/governance issue.

**Correct remediation:** Keep `Redactor`; wire it into any future diagnostic/log surfaces; add a lint rule requiring redaction for new `Log.`/`println`/diagnostic calls.

**Tests required:** Add tests verifying `Redactor` coverage and that future log calls are redacted.

---

### SEC-02 | P1 | VALID

**Original:** `ChatClient.parseChunks` embeds raw SSE payloads in `ChatStreamChunk.Error.message`.

**Source evidence:**
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt:100–115`:
  ```kotlin
  private fun parseChunks(payload: String): List<ChatStreamChunk> {
      val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
          ?: return listOf(ChatStreamChunk.Error(null, "invalid SSE JSON: $payload"))
      ...
      if (choices !is JsonArray) {
          ...
          return listOf(ChatStreamChunk.Error(null, payload))
      }
      ...
      val first = choices.firstOrNull() as? JsonObject ?: return listOf(ChatStreamChunk.Error(null, payload))
  ```

**Why original is right:** Three branches return the verbatim `payload` (or a string containing it) inside the user-facing `Error.message`. The payload may contain user prompts, assistant responses, or provider error details. `ChatViewModel.kt:180` copies `chunk.message` directly into `ChatUiState.error`, and `ImageViewModel.kt:78,118` copies `e.message` into UI state, so the leak reaches the UI and any accessibility/crash path.

**Correct remediation:** Replace `"invalid SSE JSON: $payload"` and raw `payload` returns with a constant safe message (e.g., `"invalid stream event"`). If payload must be retained for debugging, store it in a non-message field and redact before logging/display.

**Tests required:** Unit test that malformed SSE payloads do not appear in `ChatStreamChunk.Error.message`; unit test that network exceptions with synthetic messages containing an API key are not echoed.

---

## Documentation (DOC)

### DOC-01 | P1 | VALID

**Original:** `docs/SDK_EXAMPLES.md` references `chunk.text` and `chunk.arguments`, but the current SDK uses `textFragment` and `argumentsFragment`.

**Source evidence:**
- `docs/SDK_EXAMPLES.md:71–75`:
  ```kotlin
  is ChatStreamChunk.Delta -> {
      chunk.text?.let { print(it) }
  }
  is ChatStreamChunk.ToolCallDelta -> {
      println("Tool call #${chunk.index}: ${chunk.name} args: ${chunk.arguments}")
  }
  ```
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt:7–13`:
  ```kotlin
  data class Delta(val index: Int, val textFragment: String?) : ChatStreamChunk()
  data class ToolCallDelta(
      val index: Int,
      val callId: String?,
      val name: String?,
      val argumentsFragment: String?,
  ) : ChatStreamChunk()
  ```

**Why original is right:** The example will not compile against the current `:venice-sdk` public API.

**Correct remediation:** Update `docs/SDK_EXAMPLES.md:71` to `chunk.textFragment` and line 75 to `chunk.argumentsFragment`.

**Tests required:** Verify the corrected snippet compiles against `:venice-sdk`.

---

### DOC-02 | P1 | VALID

**Original:** `docs/FEATURE_PARITY_MATRIX.md` marks `chat`, `image`, `audio`, `video` as **Foundation**, while `FeatureCatalog.kt` marks them as `SCAFFOLDED`.

**Source evidence:**
- `docs/FEATURE_PARITY_MATRIX.md:7,10,15,17`:
  - `chat` → Foundation
  - `image` → Foundation
  - `audio` → Foundation (SDK)
  - `video` → Foundation (SDK)
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:23,26,31,33`:
  - `chat` → `AndroidPortStatus.SCAFFOLDED`
  - `image` → `AndroidPortStatus.SCAFFOLDED`
  - `audio` → `AndroidPortStatus.SCAFFOLDED`
  - `video` → `AndroidPortStatus.SCAFFOLDED`

**Why original is right:** The runtime registry used by the app contradicts the published parity matrix. This creates scope/expectation mismatches.

**Correct remediation:** Reconcile the two sources. Given that `ChatScreen`/`ImageScreen` exist and SDK clients exist for audio/video, the matrix likely reflects intent while `FeatureCatalog` reflects earlier scaffolding. Update `FeatureCatalog.kt` to `FOUNDATION` for `chat`, `image`, `audio`, `video` (or split SDK vs UI status), and document the definition-of-done for each status.

**Tests required:** None beyond ensuring `FeatureCatalogTest` is updated if statuses change.

---

## Tests (TEST)

### TEST-FIXTURE-01 | P1 | VALID

**Original:** `models.json` fixture contains fields not defined in `swagger.yaml` `ModelResponse.model_spec`.

**Fixture evidence:**
- `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json:10–42` includes:
  - `model_spec.name`
  - `model_spec.description`
  - `model_spec.pricing`
  - `model_spec.traits`
  - `model_spec.uncensored`
  - top-level `metadata`

**Spec evidence:**
- `.source/venice-api-docs/swagger.yaml:4659–4695` defines `ModelResponse` properties: `context_length`, `created`, `discount_to_user`, `id`, `model_spec`.
- `.source/venice-api-docs/swagger.yaml:4695–4919` defines `model_spec` properties: `availableContextTokens`, `maxCompletionTokens`, `beta`, `betaModel`, `privacy`, `regionRestrictions`, `deprecation`, `capabilities`, `constraints`. It does **not** define `name`, `description`, `pricing`, `traits`, or `uncensored`.

**Why original is right:** The fixture invents convenience fields. `VeniceModel.kt` and `ModelSpec` parse `name`, `description`, `traits`, and `uncensored`, but these are not guaranteed by the authoritative spec.

**Correct remediation:** Rebuild `models.json` from a recorded `/models` response and trim it to fields present in `swagger.yaml`; add a fixture contract test that fails if a fixture field is not in the swagger schema.

**Tests required:** New fixture-schema contract test.

---

### TEST-HARDCODE-02 | P1 | VALID

**Original:** Tests encode specific Venice model IDs as string literals, violating the AGENTS.md Model Rule.

**Evidence:**
- `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt:90`: `"llama-3.3-70b"`
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt:34`: `"llama-3.3-70b"`
- `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt:60,74,77,78`: `"llama-3.3-70b"`, `"deepseek-r1"`, `"gpt-4o"`, `"claude-3-5-sonnet"`, `"deepseek-reasoner"`
- `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json:6,45`: `"llama-3.3-70b"`, `"deepseek-r1"`

**Spec evidence:**
- `AGENTS.md`: "Never hard-code a current Venice model catalog or permanent default model ID. Model IDs and capabilities are runtime data."
- `.source/venice-api-docs/AGENTS.md:66`: "Discover, don't hardcode."

**Why original is right:** Model IDs rotate; tests will produce false positives when the catalog changes.

**Correct remediation:** Replace literal model IDs with synthetic IDs in unit tests; in integration tests resolve IDs via `/models/traits`.

**Tests required:** Refactor affected tests; add CI check that fails on new hardcoded Venice model IDs in test sources.

---

### TEST-COVERAGE-03 | P1 | VALID

**Original:** No tests exist for `AudioClient.speech`.

**Evidence:**
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt:15–45` implements `POST /audio/speech`.
- `find venice-sdk/src/test -name '*Audio*'` returns nothing (confirmed by repo glob).

**Why original is right:** The TTS endpoint has zero test coverage; regressions in URL, auth header, Accept header, or error mapping go undetected.

**Correct remediation:** Add `AudioClientTest` covering request method/URL/auth/Accept header, binary 200 response, 4xx/5xx error mapping, and timeout/IO exception mapping.

**Tests required:** New `AudioClientTest.kt`.

---

### TEST-COVERAGE-04 | P1 | VALID

**Original:** No tests exist for `VideoClient.queue`, `retrieve`, `complete`.

**Evidence:**
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt:15–119` implements the async video job state machine.
- `find venice-sdk/src/test -name '*Video*'` returns nothing (confirmed by repo glob).

**Why original is right:** The video queue/retrieve/complete flow is untested; content-type switching and status parsing regressions are not caught.

**Correct remediation:** Add `VideoClientTest` covering queue JSON response parsing, retrieve returning `Processing` vs `Completed` based on `Content-Type`, complete success, and 4xx/5xx/404/timeout errors.

**Tests required:** New `VideoClientTest.kt`.

---

### TEST-MISSING-06 | P1 | VALID

**Original:** `ChatViewModelTest` covers only happy paths; failure/cancellation/lifecycle cases are missing.

**Evidence:**
- `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt` covers single-turn, multi-turn, and loading existing conversation.
- It does **not** cover:
  - `ChatStreamChunk.Error` path (`ChatViewModel.kt:173–181`)
  - `cancel()` (`ChatViewModel.kt:189–193`)
  - missing API key (`ChatViewModel.kt:88–91`)
  - missing model selection (`ChatViewModel.kt:93–96`)
  - tool-call deltas
  - `ViewModel` cleared while streaming
  - configuration change / process death

**Why original is right:** The most user-visible error paths are unverified.

**Correct remediation:** Add tests for error chunk, cancellation, missing API key, missing model, tool-call accumulation, and `viewModelScope` cancellation.

**Tests required:** Expand `ChatViewModelTest`.

---

### TEST-MISSING-18 | P1 | VALID

**Original:** No test verifies that explicit `safe_mode=false` is serialized in media requests.

**Evidence:**
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt` defines `GenerateImageRequest` and `EditImageRequest` with `safeMode` fields.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt` defines `QueueVideoRequest` with `safeMode`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt` defines `SpeechRequest` with `safeMode`.
- No test asserts `safe_mode=false` serialization for any media request.

**Spec evidence:**
- `AGENTS.md`: "Preserve explicit `safe_mode=false` when selected."
- `.source/venice-api-docs/swagger.yaml:2666–2671` documents `/image/generate safe_mode`.

**Why original is right:** The project has an explicit contract to preserve `safe_mode=false`; without tests, a serialization default could silently drop it.

**Correct remediation:** Add serialization tests for `safe_mode=false` in image, video, and audio request models.

**Tests required:** New/expanded model serialization tests.

---

### TEST-MISSING-19 | P1 | VALID

**Original:** The `:venice-sdk` security boundary (no persistence, no raw logging) is not tested.

**Evidence:**
- `venice-sdk/src/test` contains no test verifying that the SDK does not persist API keys or log raw prompts/responses.
- The only security-adjacent tests are `VeniceForgeSdkTest.kt:102,131`, which assert exception messages do not contain the API key.

**Spec evidence:**
- `AGENTS.md`: "`:venice-sdk` never persists API keys." and "No raw prompt/response/API-key logging."

**Why original is right:** Security rules require automated enforcement to prevent regressions.

**Correct remediation:** Add tests verifying:
1. SDK does not write keys to disk/shared prefs.
2. Logs/diagnostics do not contain keys.
3. Key redaction in diagnostics.

**Tests required:** New security-focused tests in `:venice-sdk` and/or `:core:common`.

---

## Key evidence quotes (max 12 lines)

1. `ProfileRepository.kt:7–20`: `ensureDefault` read-then-write with no transaction.
2. `ChatRepository.kt:45–54`: `appendMessage` transaction updates only `messages`.
3. `ChatRepository.kt:56–69`: `updateAssistantText` has no transaction and no conversation `updatedAt` update.
4. `MessageDao.kt:23–24`: `updateTextAndStatus` WHERE clause is `id` + `profileId` only.
5. `AppDatabase.kt:36–43`: plaintext `Room.databaseBuilder`.
6. `docs/SECURITY_AND_STORAGE_CONTRACT.md:2`: "App persistence uses Android Keystore-backed encryption."
7. `ChatClient.kt:100–115`: raw SSE `payload` echoed in three `ChatStreamChunk.Error` branches.
8. `docs/SDK_EXAMPLES.md:71,75`: `chunk.text` and `chunk.arguments` do not exist on current SDK.
9. `FeatureCatalog.kt:23,26,31,33`: `chat`, `image`, `audio`, `video` are `SCAFFOLDED`.
10. `docs/FEATURE_PARITY_MATRIX.md:7,10,15,17`: same features are `Foundation`.
11. `models.json:10–42`: fixture fields `name`, `description`, `pricing`, `traits`, `uncensored`, `metadata` are swagger-undefined.
12. Repo-wide `Grep` for `Log.`, `println`, `printStackTrace`, `Timber`, `Logger`, `logcat`: **no production matches**.

---

## Output file

- `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/data-security-docs-tests.md`
