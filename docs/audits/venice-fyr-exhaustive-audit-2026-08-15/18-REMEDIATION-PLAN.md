# Remediation Plan — Venice Fyr Android Exhaustive Audit

**Audit date:** 2026-08-15  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `1da3142`  
**Upstream API docs:** `veniceai/api-docs` @ `6e69346b`, `swagger.yaml` `info.version 20260814.194349`  
**Status:** Build is currently broken (BASELINE-01). No release/CI exists.

This plan orders the findings into 15 work packages (WP-01..WP-15). The dependency order respects:

1. **Build + CI gate first** — nothing else can be validated until the repo compiles and CI runs.
2. **Venice API contract correctness before SDK public API stabilization** — fix wire mismatches before freezing the public surface.
3. **Persistence / security fixes before release hardening** — data integrity and secrets must be solid before R8/signing/docs.
4. **Documentation drift last** — docs are updated only after the behavior they describe is fixed.

Each package is intentionally narrow and verifiable; the goal is surgical fixes, not rewrites.

---

## WP-01 — Fix the compile break and stand up the local validation gate

| Field | Value |
|-------|-------|
| **Objective** | Restore `:venice-sdk` compilation so that `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` can run locally. |
| **Findings addressed** | BASELINE-01 |
| **Affected modules/files** | `:venice-sdk` — `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34` |
| **Dependencies on other WPs** | None. Gate for all subsequent WPs. |
| **Implementation strategy** | Add the missing `import io.github.spearchucker667.veniceforge.sdk.image.ImageClient` (or switch line 34 to the fully-qualified form used by `audioClient()`/`videoClient()`). No other code changes. |
| **Tests required** | Existing `ImageClientTest` must compile and pass once the import is restored. |
| **Validation commands** | `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` |
| **Compatibility concerns** | None — source-only fix, no public API or behavior change. |
| **Acceptance criteria** | All four Gradle gates complete without `Unresolved reference 'ImageClient'`. `ImageClientTest` runs. |

---

## WP-02 — Create CI/CD and dependency-verification scaffolding

| Field | Value |
|-------|-------|
| **Objective** | Automate PR and release validation so regressions cannot merge undetected. |
| **Findings addressed** | BUILD-01, BUILD-06, BUILD-07, BUILD-08, BUILD-09, BUILD-10, BUILD-11, BUILD-12, HYGIENE-02 |
| **Affected modules/files** | `.github/workflows/pr.yml` (new), `.github/workflows/release.yml` (new), `gradle.properties`, `gradle/libs.versions.toml`, `.gitignore`, module `build.gradle.kts` files |
| **Dependencies on other WPs** | WP-01 (must compile first). |
| **Implementation strategy** | 1. Add `.github/workflows/pr.yml` running `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` on JDK 17 with `gradle/wrapper-validation-action`. 2. Add release workflow that signs `:app` and `:venice-sdk` artifacts from GitHub secrets. 3. Remove unused catalog entries (`okhttp-logging`, `media3-ui-compose`) and unused `:app` deps (`datastore`, `work`, `media3`, `sqlite`, `androidx.test.ext:junit`). 4. Add `.kotlin/` and `.superpowers/` to `.gitignore`. 5. Optionally enable configuration cache after verifying plugin compatibility. 6. Remove `testInstrumentationRunner` from `:core:data` until `androidTest` sources exist. 7. Add explicit `kotlin { jvmToolchain(17) }` to all modules. 8. Drive `versionCode`/`versionName` from Git tag or `version.properties`. |
| **Tests required** | Verify PR workflow passes on a branch; verify wrapper validation catches tampering; verify APK/AAR artifacts are produced. |
| **Validation commands** | `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` in CI; `gh workflow run pr.yml` |
| **Compatibility concerns** | Removing unused dependencies is safe. Changing version source requires CI secret setup. |
| **Acceptance criteria** | Every PR runs tests/lint/assemble. Release workflow produces signed artifacts. No unused catalog entries remain. |

---

## WP-03 — Venice chat/streaming contract correctness

| Field | Value |
|-------|-------|
| **Objective** | Align the chat client with the Venice `/chat/completions` wire contract: non-streaming path, structured errors, full SSE parsing, and missing request/response fields. |
| **Findings addressed** | CHAT-01, CHAT-07, CHAT-08, CHAT-09, CHAT-10, CHAT-11, CHAT-13, CHAT-14, CHAT-15, CHAT-16, ARCH-02, ARCH-16, SDK-CORE-08 |
| **Affected modules/files** | `:venice-sdk` — `sdk/chat/ChatClient.kt`, `sdk/chat/ChatRequest.kt`, `sdk/chat/ChatStreamChunk.kt`, `sdk/chat/ChatStreamAccumulator.kt`, `sdk/chat/SseLineParser.kt`; `:app` — `android/chat/ChatViewModel.kt` (duplicate-message fix) |
| **Dependencies on other WPs** | WP-01 (compile), WP-02 (CI). Public API additions should land before WP-06 stabilizes the surface. |
| **Implementation strategy** | 1. Add `chatCompletion(apiKey, request): ChatCompletionResponse` for `stream=false`, or reject `stream=false` in `streamChat`. 2. Route HTTP errors through `VeniceForgeSdk.parseHttpError` instead of raw `ChatStreamChunk.Error`. 3. Parse `usage`, `id`, `created`, `model`, `cost`, `reasoning_content`, and multiple choices into new `ChatStreamChunk` subtypes. 4. Replace `SseLineParser` with a full SSE event accumulator that joins multi-line `data:` fields and preserves `event`/`id` metadata. 5. Remove synthetic `Finish("stop")`; emit an explicit incomplete/error terminal state. 6. Do not swallow `CancellationException`. 7. Add `returnSearchResultsAsDocuments`, `developer` role, reasoning fields, and missing OpenAI-compatible parameters. 8. Fix `ChatViewModel.submit` so the new user message is not duplicated in the context. |
| **Tests required** | Unit tests for non-streaming JSON response, 429/401/400 error mapping, multi-line SSE, usage event, multiple choices, cancellation propagation, and the duplicate-message regression test. |
| **Validation commands** | `./gradlew :venice-sdk:test :app:test` |
| **Compatibility concerns** | New `ChatStreamChunk` subtypes and removal of synthetic `stop` are behavioral changes. Document in CHANGELOG. |
| **Acceptance criteria** | `ChatClientTest` covers non-200 HTTP, malformed SSE, multiple choices, and usage events. `ChatViewModelTest` multi-turn test passes without duplicated user messages. |

---

## WP-04 — Image/media contract correctness (binary responses, multipart)

| Field | Value |
|-------|-------|
| **Objective** | Fix the image client so `/image/edit`, `/image/upscale`, `/image/multi-edit`, and background-removal return binary data and support multipart uploads; expose response headers. |
| **Findings addressed** | IMG-01, IMG-02, IMG-03, IMG-04, IMG-05, IMG-06, IMG-07, IMG-08, IMG-09, IMG-10, IMG-11, IMG-12 |
| **Affected modules/files** | `:venice-sdk` — `sdk/image/ImageClient.kt`, `sdk/image/ImageModels.kt`, `sdk/VeniceSdkException.kt`; `:app` — `android/image/ImageViewModel.kt` |
| **Dependencies on other WPs** | WP-01, WP-02. Should precede WP-06 because it changes public return types. |
| **Implementation strategy** | 1. Change `upscale`, `edit`, `multiEdit` to return `ByteArray` (or a wrapper with `bytes` + `contentType` + Venice headers). 2. Add `backgroundRemove`, `styles()`, and OpenAI-compatible `simpleGenerate`. 3. Add multipart upload variants for endpoints that accept `multipart/form-data`. 4. Make `GenerateImageResponse` required fields non-null and add the `request` echo field. 5. Add missing request fields (`embedExifMetadata`, `loraStrength`, `modelId`). 6. Map HTTP 402 to a new `VeniceSdkException.PaymentRequired` subtype. 7. Expose Venice response headers (`x-venice-is-blurred`, `X-Balance-Remaining`, etc.) in a typed wrapper. 8. Guard `generate()` against `returnBinary=true`. 9. Update `ImageViewModel.editImage()` to consume bytes instead of base64 JSON. |
| **Tests required** | Unit tests for binary `edit`/`upscale`/`multi-edit`, multipart body construction, 402 mapping, header exposure, and `returnBinary=true` guard. |
| **Validation commands** | `./gradlew :venice-sdk:test :app:test` |
| **Compatibility concerns** | Breaking return-type changes for `upscale`/`edit`/`multiEdit`; required to match upstream contract. |
| **Acceptance criteria** | `ImageClientTest` exercises all public image methods and error paths. `ImageViewModelTest` (new) verifies edit consumes bytes. |

---

## WP-05 — Video/audio queued-job state machine + quote-before-generate

| Field | Value |
|-------|-------|
| **Objective** | Implement the missing queued-media lifecycle (quote, queue, poll, retrieve, complete) and transcription endpoints for video and audio. |
| **Findings addressed** | VID-01, VID-02, VID-03, VID-04, VID-05, VID-06, VID-07, AUD-01, AUD-02, AUD-03, AUD-04, AUD-05, X-01, X-02, ARCH-06 |
| **Affected modules/files** | `:venice-sdk` — `sdk/video/VideoClient.kt`, `sdk/video/VideoModels.kt`, `sdk/audio/AudioClient.kt`, `sdk/audio/AudioModels.kt`; `:app` — future WorkManager workers (WP-12) |
| **Dependencies on other WPs** | WP-01, WP-02. Public API additions feed into WP-06. |
| **Implementation strategy** | 1. Expand `QueueVideoRequest` to match swagger (required `model`/`prompt`/`duration`, plus all optional fields). 2. Add `VideoClient.quote`, `transcribe`, and a typed `VideoStatus` enum. 3. Fix `retrieve` to branch on `status == COMPLETED` and expose `download_url` for VPS-backed models. 4. Make `complete` return the parsed `success` flag. 5. Add `pollUntilCompleted` helper with bounded exponential back-off. 6. Add queued audio methods (`queue`, `retrieve`, `quote`, `complete`) and `transcribe`. 7. Complete `SpeechRequest` fields and remove unsupported `safe_mode`. 8. Return content type with audio bytes. 9. Add `AudioClientTest` and `VideoClientTest`. |
| **Tests required** | Mocked lifecycle tests for quote → queue → PROCESSING → COMPLETED, VPS `download_url`, `complete(false)`, and transcription JSON/text. |
| **Validation commands** | `./gradlew :venice-sdk:test` |
| **Compatibility concerns** | New sealed subclasses in `VideoRetrieveResult`; source-compatible only if callers handle `else`. |
| **Acceptance criteria** | `VideoClientTest` and `AudioClientTest` exist and pass. `VENICE_API_SOURCE_MANIFEST` updated to reflect implemented endpoints. |

---

## WP-06 — SDK public API stability + model/capability discovery

| Field | Value |
|-------|-------|
| **Objective** | Stabilize the SDK public surface: model discovery, capability parsing, typed exceptions, and transport encapsulation. |
| **Findings addressed** | SDK-CORE-01, SDK-CORE-02, SDK-CORE-03, SDK-CORE-04, SDK-CORE-05, SDK-CORE-06, SDK-CORE-07, SDK-CORE-11, SDK-CORE-12, SDK-CORE-13, SDK-CORE-14, SDK-CORE-15, SDK-CORE-16, SDK-CORE-17, SDK-CORE-18, SDK-CORE-19, SDK-CORE-20, CHAT-02, CHAT-03, CHAT-04, CHAT-06, IMG-06, IMG-07, IMG-08 |
| **Affected modules/files** | `:venice-sdk` — `sdk/VeniceForgeSdk.kt`, `sdk/VeniceSdkException.kt`, `sdk/VeniceSdkConfig.kt`, `sdk/VeniceEndpoints.kt`, `sdk/ModelType.kt`, `sdk/VeniceModel.kt`, `sdk/capabilities/*`, `sdk/chat/ChatRequest.kt`, `sdk/image/ImageModels.kt` |
| **Dependencies on other WPs** | WP-01, WP-02, WP-03, WP-04, WP-05. This package stabilizes the surface after contract fixes. |
| **Implementation strategy** | 1. Forward `type` query parameter in `/models/traits` and `/models/compatibility_mapping`; add modality-aware default-model resolvers. 2. Expand `ModelSpec`/`ModelCapabilities` with `constraints`, `pricing`, `deprecation`, `quantization`, reasoning options, `maxImages`, `maxVideos`, and media fields; keep `rawJson`. 3. Fix capability defaults (`supportsSystemPrompt`, `supportsTextChat`, `supportsImageGeneration`, `ModelType.CODE`). 4. Make `maxContextTokens`/`maxCompletionTokens` `Long?`. 5. Return sealed result from `fetchLiveCapabilities` instead of swallowing errors; let cancellation propagate. 6. Add in-memory cache with TTL + `forceRefresh` to `CapabilitiesRepository`. 7. Remove blank-key requirement from `listModels`; add anonymous overload. 8. Make `baseUrl()`/`httpClient()` `internal`; expose narrower transport abstractions. 9. Make `VeniceSdkException.Http` a `data class` (seal hierarchy); add `NotFound` for 404. 10. Fix `retryAfterSeconds` to compute duration from absolute timestamp. 11. Allow custom user-agent app identifier and configurable timeouts in `VeniceSdkConfig`. 12. Remove `safe_mode` from `VeniceParameters` for chat. |
| **Tests required** | Unit tests for trait/compatibility `type` param, cache TTL, cancellation propagation, anonymous model listing, exception equality, retry-after computation, and expanded model deserialization. |
| **Validation commands** | `./gradlew :venice-sdk:test` |
| **Compatibility concerns** | Multiple source-breaking changes (visibility reductions, `Http` data class, `Long` token types). Bundle into a single minor/major SDK version bump and migration notes. |
| **Acceptance criteria** | `CapabilitiesRepositoryTest` uses synthetic model IDs and passes. `VeniceForgeSdkTest` covers timeout/IO and 500/503. No public transport internals exposed. |

---

## WP-07 — Transport errors, timeouts, and retry policy

| Field | Value |
|-------|-------|
| **Objective** | Give the SDK sensible default timeouts, structured error classification, and a coherent retry contract without accidental double-billing. |
| **Findings addressed** | SDK-CORE-09, SDK-CORE-13, CHAT-07, CHAT-10, IMG-09, VM-11, SEC-06, SEC-07 |
| **Affected modules/files** | `:venice-sdk` — `sdk/VeniceSdkConfig.kt`, `sdk/VeniceForgeSdk.kt`, `sdk/VeniceSdkException.kt`, `sdk/chat/ChatClient.kt`; `:app` — `android/chat/ChatViewModel.kt`, `android/image/ImageViewModel.kt` |
| **Dependencies on other WPs** | WP-01, WP-02, WP-03, WP-06. |
| **Implementation strategy** | 1. Add `connectTimeout`, `readTimeout`, `writeTimeout` to `VeniceSdkConfig` with sensible defaults (e.g., 30s/60s/60s) and apply them in a shared `OkHttpClient` builder. 2. Ensure `ChatClient` routes HTTP errors through `parseHttpError`. 3. Expose `RateLimitInfo` correctly (duration vs timestamp). 4. Map 402 to `PaymentRequired`. 5. In `:app`, disable action buttons from the moment a paid request starts until completion/error; add confirmation dialogs for image/video/audio generation. 6. Document that idempotency keys are not yet supported by Venice media endpoints; do not retry billable endpoints automatically. |
| **Tests required** | Unit tests verifying timeout configuration is honored, 402/429 mapping, and UI confirmation flow. |
| **Validation commands** | `./gradlew :venice-sdk:test :app:test` |
| **Compatibility concerns** | Default timeout changes may surface latent slow-request behavior. UI confirmation is a UX change. |
| **Acceptance criteria** | All SDK clients use configured timeouts. `VeniceSdkException` subclasses cover 402, 404, 429, and network I/O. Paid operations show explicit approval UI. |

---

## WP-08 — Coroutine, cancellation, and ViewModel lifecycle

| Field | Value |
|-------|-------|
| **Objective** | Replace `remember`-created ViewModels with framework-scoped instances, fix duplicate submissions, plug process-death gaps, and clean up cancellation paths. |
| **Findings addressed** | APP-UI-001, APP-UI-003, APP-UI-004, APP-UI-005, APP-UI-009, APP-UI-020, ARCH-01, ARCH-04, ARCH-05, ARCH-11, ARCH-12, VM-01, VM-02, VM-03, VM-04, VM-07, VM-08, VM-09, VM-10, VM-12, VM-13, VM-15 |
| **Affected modules/files** | `:app` — `android/MainActivity.kt`, `android/VeniceForgeApp.kt`, `android/chat/ChatScreen.kt`, `android/image/ImageScreen.kt`, `android/ui/ConfigScreen.kt`, `android/chat/ChatViewModel.kt`, `android/image/ImageViewModel.kt`, `android/feature/FeatureCatalog.kt` |
| **Dependencies on other WPs** | WP-01, WP-02. Should follow WP-06 if `apiKeyProvider` signature changes. |
| **Implementation strategy** | 1. Obtain `ChatViewModel`/`ImageViewModel` via `viewModel()` with a custom factory keyed by `profileId`. 2. Make `apiKeyProvider` `suspend` and call it inside `viewModelScope.launch` (or dispatch before use). 3. Add `SavedStateHandle` to ViewModel constructors; persist `conversationId`, `resultImageUri`, and `profileId`. 4. Guard `ChatViewModel.submit()` and `ImageViewModel` generate/edit with an in-flight flag/Job. 5. Wrap stream collection in `try/catch/finally`; set `isStreaming=false` and mark assistant `FAILED` on exception. 6. Re-throw `CancellationException` in image operations; mark assistant `CANCELLED` on user cancel. 7. Map SDK exceptions to localized user-facing strings. 8. Replace `collectAsState()` with `collectAsStateWithLifecycle()`. 9. Convert transient screen state to `rememberSaveable` or ViewModel-held state. 10. Fix `FeatureCatalog.byId` to return `AppFeature?`. 11. Add loading state distinguishing “key loading” from “key missing”. |
| **Tests required** | Rotation/config-change test, rapid-submit test, process-death recreation test, cancellation test, missing-key/model test, and `ImageViewModelTest`. |
| **Validation commands** | `./gradlew :app:test`; instrumented rotation/process-death tests on emulator/device. |
| **Compatibility concerns** | ViewModel lifecycle semantics change; any code relying on re-creation on rotation will break. `apiKeyProvider` signature change affects callers. |
| **Acceptance criteria** | ViewModels survive rotation and process death. Rapid taps produce exactly one request. Cancellation does not surface as error. `FeatureCatalog.byId("unknown")` returns null. |

---

## WP-09 — Persistence integrity, migrations, and encryption

| Field | Value |
|-------|-------|
| **Objective** | Harden `core:data` so the database is a true singleton, enforces referential integrity, survives schema evolution, and encrypts sensitive data at rest. |
| **Findings addressed** | DATA-01, DATA-02, DATA-03, DATA-04, DATA-05, DATA-06, DATA-07, DATA-08, DATA-09, DATA-10, DATA-11, DATA-12, DATA-13, DATA-14, DATA-15, ARCH-03, ARCH-15 |
| **Affected modules/files** | `:core:data` — `AppDatabase.kt`, `DataServices.kt`, `Converters.kt`, `dao/*`, `entity/*`, `repo/*`, test `MigrationTest.kt`; `:app` — `android/VeniceForgeApp.kt` |
| **Dependencies on other WPs** | WP-01, WP-02. Should precede WP-15 release hardening. |
| **Implementation strategy** | 1. Initialize `DataServices` once in `Application.onCreate` (or a DI singleton) and inject into ViewModels. 2. Add self-referencing FK on `MessageEntity.parentMessageId` and FK on `MessageEntity.profileId`. 3. Wrap `ProfileRepository.ensureDefault` in `withTransaction` or `INSERT OR IGNORE`. 4. Update `ChatRepository.appendMessage` and `updateAssistantText` to touch `conversations.updatedAt` inside transactions. 5. Add `conversationId` to `updateAssistantText` signature and WHERE clause. 6. Rename `deleteCascade` or make it manually cascade; add profile scoping to `MessageToolCallDao.observeForMessage`. 7. Make `ChatRepository` constructor `internal`. 8. Add unique partial index for `profiles.isDefault = 1`. 9. Replace enum `valueOf` with `enumValues<T>().find { it.name == v }` with explicit unknown mapping. 10. Adopt SQLCipher (or encrypted blobs) with Keystore-derived key; add migration path from plaintext. 11. Add corruption handler with destructive fallback or backup restore. 12. Add `fallbackToDestructiveMigration()` or explicit migrations; expand `MigrationTest` to assert FKs, indices, and column types. |
| **Tests required** | Concurrent `ensureDefault`, conversation `updatedAt` advance, cross-conversation mutation negative test, profile isolation for tool calls, corruption simulation, migration round-trip, encrypted DB file probe. |
| **Validation commands** | `./gradlew :core:data:test` |
| **Compatibility concerns** | Schema changes require migration. Encryption migration must not lose existing data without user approval. |
| **Acceptance criteria** | `MigrationTest` validates FKs and indices. DB file is not plaintext after encryption migration. `ProfileRepository` concurrent test passes. |

---

## WP-10 — Security, secrets, and redaction wiring

| Field | Value |
|-------|-------|
| **Objective** | Ensure secrets never leak through logs, UI, or memory, and that credential storage is resilient to corruption and migration. |
| **Findings addressed** | SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, SEC-07, SEC-08, SEC-09, SEC-10, ARCH-10, APP-UI-019 |
| **Affected modules/files** | `:core:common` — `Redactor.kt`; `:core:security` — `SecureSecretStore.kt`; `:venice-sdk` — `sdk/chat/ChatClient.kt`, `sdk/VeniceSdkException.kt`; `:app` — `android/ui/ConfigScreen.kt`, `android/chat/ChatViewModel.kt`, `android/image/ImageViewModel.kt`, `android/VeniceForgeApp.kt` |
| **Dependencies on other WPs** | WP-01, WP-02, WP-08 (ViewModel error mapping), WP-09 (SecureSecretStore migration). |
| **Implementation strategy** | 1. Route all exception messages and diagnostic output through `Redactor.redact()`; add lint rule against raw logging. 2. Replace raw SSE payload echo in `ChatClient` with a constant safe message; redact network exception messages before UI display. 3. Add `SecureSecretStoreTest` using a fake KeyStore provider; cover round-trip, tampered ciphertext, blank inputs, multiple profiles. 4. Remove ciphertext deletion on decryption failure; surface distinct error and require explicit user action to delete. 5. Minimize API-key lifetime in `ConfigScreen`: use sealed input state, clear buffer after save, do not populate text field with decrypted key. 6. Confirm Venice key prefixes and update `Redactor` regex; redact entire `Authorization` header value as fallback. 7. Map exceptions to localized strings in ViewModels. 8. Stop truncating SHA-256 alias to 32 chars; provide migration for existing stored keys. 9. Replace `Uri.fromFile` with `FileProvider` content URIs. |
| **Tests required** | Redaction parameterized tests, `SecureSecretStoreTest`, ViewModel error-sanitization tests, `FileProvider` URI test. |
| **Validation commands** | `./gradlew :core:common:test :core:security:test :app:test :venice-sdk:test` |
| **Compatibility concerns** | Changing alias derivation invalidates existing keys; migration required. Removing auto-delete changes observable behavior intentionally. |
| **Acceptance criteria** | No raw API key/prompt appears in exception messages or UI state. `SecureSecretStoreTest` passes. Decryption failure does not erase ciphertext. |

---

## WP-11 — Compose UI correctness, accessibility, and localization

| Field | Value |
|-------|-------|
| **Objective** | Make the Compose layer correct across configuration changes, accessible, localizable, and honest about implemented features. |
| **Findings addressed** | APP-UI-002, APP-UI-006, APP-UI-007, APP-UI-008, APP-UI-010, APP-UI-011, APP-UI-012, APP-UI-013, APP-UI-014, APP-UI-015, APP-UI-016, APP-UI-017, APP-UI-018, APP-UI-019, APP-UI-020, ARCH-14, VM-15 |
| **Affected modules/files** | `:app` — `android/MainActivity.kt`, `android/VeniceForgeApp.kt`, `android/chat/ChatScreen.kt`, `android/image/ImageScreen.kt`, `android/ui/ConfigScreen.kt`, `android/feature/FeatureCatalog.kt`; `:core:designsystem` — `CodexPet.kt`, `VeniceForgeTheme.kt`, `VeniceLoadingIndicator.kt`; `app/src/main/res/values/strings.xml`, `themes.xml` |
| **Dependencies on other WPs** | WP-01, WP-02, WP-08 (ViewModel lifecycle), WP-10 (FileProvider). |
| **Implementation strategy** | 1. Remove hard-coded `darkTheme = true` from `MainActivity`; respect system setting. 2. Externalize all user-visible strings to `strings.xml`. 3. Add `VeniceShapes` and pass to `MaterialTheme`. 4. Replace main-thread bitmap decode with Coil `AsyncImage` or `produceState` + `Dispatchers.IO`. 5. Dispose `CodexPet` `ImageBitmap` in `DisposableEffect`. 6. Disable or badge scaffolded features in the navigation drawer. 7. Apply `minimumInteractiveComponentSize()` to small touch targets. 8. Move content descriptions to `strings.xml`; derive image description from prompt if feasible. 9. Convert `remember` screen state to `rememberSaveable` or ViewModel state. 10. Use `CapabilitiesRepository` in `ConfigScreen` for model discovery. 11. Migrate `Uri.fromFile` to `FileProvider`. |
| **Tests required** | UI tests for rotation survival, locale switch, accessibility scanner pass, and disabled scaffolded features. |
| **Validation commands** | `./gradlew :app:lintDebug :app:test`; manual accessibility scanner run. |
| **Compatibility concerns** | Visual change (light mode support, shape tokens). Navigation surface reduction if features are hidden. |
| **Acceptance criteria** | Lint passes with no hard-coded string warnings. Accessibility scanner reports no touch-target or content-description issues. Scaffolded features are clearly marked. |

---

## WP-12 — Storage, export, and background work

| Field | Value |
|-------|-------|
| **Objective** | Move durable/queued work off ViewModel scopes, add cache management, and provide a redacted backup/export path. |
| **Findings addressed** | ARCH-06, ARCH-07, ARCH-09, DATA-16, VM-07, VM-08, VM-11 |
| **Affected modules/files** | `:app` — new `worker/` package, `android/chat/ChatViewModel.kt`, `android/image/ImageViewModel.kt`; `:core:data` — new export/import APIs; `app/build.gradle.kts` (re-add WorkManager/DataStore/Media3 when used) |
| **Dependencies on other WPs** | WP-01, WP-02, WP-05 (queued job API), WP-08 (ViewModel state), WP-09 (persistence). |
| **Implementation strategy** | 1. Implement WorkManager workers for video/audio queued generation that survive process death and retry. 2. Re-add `androidx.work`, `androidx.datastore.preferences`, and `androidx.media3.exoplayer` only when consumed by production code. 3. Implement content-addressed image cache with max-size eviction; store metadata in Room. 4. Add profile-scoped, encrypted `.vfbackup` export/import via Storage Access Framework. 5. Persist pending paid jobs and reconcile before retry. 6. Use DataStore for UI preferences and current profile. |
| **Tests required** | Worker unit tests, process-death recovery test, cache eviction test, backup round-trip test. |
| **Validation commands** | `./gradlew :app:test`; instrumented process-death test. |
| **Compatibility concerns** | New permissions/features (SAF, foreground service for workers). Backup format becomes a compatibility contract. |
| **Acceptance criteria** | Queued video/audio jobs survive process death. Cache size is bounded. Backup/export round-trips without leaking prompts. |

---

## WP-13 — Performance and concurrency

| Field | Value |
|-------|-------|
| **Objective** | Eliminate main-thread blocking work, resource leaks, races, and redundant network calls. |
| **Findings addressed** | APP-UI-007, APP-UI-008, ARCH-08, ARCH-09, ARCH-13, VM-05, VM-06, SDK-CORE-07, SDK-CORE-14, VM-01, VM-04, ARCH-04, TEST-MISSING-20 |
| **Affected modules/files** | `:app` — `android/image/ImageScreen.kt`, `android/image/ImageViewModel.kt`, `android/chat/ChatViewModel.kt`, `android/VeniceForgeApp.kt`; `:core:designsystem` — `CodexPet.kt`; `:venice-sdk` — `sdk/capabilities/CapabilitiesRepository.kt`, `sdk/chat/ChatClient.kt` |
| **Dependencies on other WPs** | WP-01, WP-02, WP-08, WP-09, WP-12. |
| **Implementation strategy** | 1. Move Keystore decryption off main thread (see WP-08). 2. Move base64 decode and bitmap decode to `Dispatchers.IO`/`Default`. 3. Dispose `ImageBitmap` native resources. 4. Implement bounded image cache (see WP-12). 5. Add in-memory TTL cache to `CapabilitiesRepository` and avoid triple sequential network calls on every invocation. 6. Let `CancellationException` propagate in `CapabilitiesRepository` and streaming paths. 7. Add atomic in-flight guards to prevent duplicate submissions. 8. Add concurrency stress tests. |
| **Tests required** | Strict-mode disk-read detection, large-image decode performance, concurrent `submit()` test, cache hit/TTL test, cancellation propagation test. |
| **Validation commands** | `./gradlew :app:test :venice-sdk:test` |
| **Compatibility concerns** | Caching changes timing of model-discovery updates; document TTL. |
| **Acceptance criteria** | No disk-read or crypto on main thread in strict mode. `CapabilitiesRepository` does not issue three network calls within TTL window. Duplicate rapid taps produce one request. |

---

## WP-14 — Test coverage

| Field | Value |
|-------|-------|
| **Objective** | Backfill the critical test gaps so contract fixes and refactorings are protected by automated tests. |
| **Findings addressed** | TEST-FIXTURE-01, TEST-HARDCODE-02, TEST-COVERAGE-03, TEST-COVERAGE-04, TEST-MOCK-05, TEST-MISSING-06, TEST-MISSING-07, TEST-MISSING-08, TEST-MISSING-09, TEST-MISSING-10, TEST-FIXTURE-11, TEST-MISSING-12, TEST-MISSING-13, TEST-MISSING-14, TEST-MISSING-15, TEST-MISSING-16, TEST-MISSING-17, TEST-MISSING-18, TEST-MISSING-19, TEST-MISSING-20 |
| **Affected modules/files** | `:venice-sdk/src/test/**`, `:app/src/test/**`, `:core:data/src/test/**`, `:core:common/src/test/**`, `:core:security/src/test/**` |
| **Dependencies on other WPs** | WP-01, WP-02. Runs in parallel with WPs 03–13, but should follow the contract fixes it tests. |
| **Implementation strategy** | 1. Rebuild `models.json` from a recorded `/models` response trimmed to swagger-defined fields; add fixture-schema contract test. 2. Replace hardcoded model IDs with synthetic IDs in unit tests; add CI check against new hardcoded IDs. 3. Add `AudioClientTest` and `VideoClientTest`. 4. Expand `ImageClientTest` to cover `upscale`, `multiEdit`, `generateBinary`, errors, headers. 5. Expand `ChatClientTest` for HTTP non-200, invalid SSE, multiple choices, missing `choices`. 6. Expand `ChatViewModelTest` for errors, cancellation, rapid submit, missing key/model. 7. Expand `ChatStreamAccumulatorTest`, `SseLineParserTest`, `VeniceParametersSerializationTest`, `RedactorTest`, `FeatureCatalogTest`, `MigrationTest`, `SecureSecretStoreTest`. 8. Add security tests verifying no SDK persistence and key redaction. 9. Add concurrency/lifecycle stress tests. |
| **Tests required** | See each targeted test file above. |
| **Validation commands** | `./gradlew test` |
| **Compatibility concerns** | Test-only changes; no production compatibility impact. |
| **Acceptance criteria** | `test` task passes and line/branch coverage for `:venice-sdk` chat/image/video/audio and `:app` ViewModels measurably improves. No hardcoded Venice model IDs in tests. |

---

## WP-15 — Release hardening (R8/signing) and documentation drift

| Field | Value |
|-------|-------|
| **Objective** | Make release builds runnable and signed, author keep rules, and reconcile all documentation with the fixed implementation. |
| **Findings addressed** | BUILD-02, BUILD-03, BUILD-04, BUILD-05, DOC-01, DOC-02, DOC-03, DOC-04, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, SDK-CORE-10, X-02 |
| **Affected modules/files** | `app/build.gradle.kts`, `venice-sdk/consumer-rules.pro` (new rules), `docs/SDK_EXAMPLES.md`, `docs/FEATURE_PARITY_MATRIX.md`, `docs/VENICE_API_PORT_MATRIX.md`, `docs/reference/VENICE_API_SOURCE_MANIFEST.md`, `README.md`, `CHANGELOG.md`, `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md`, `VeniceEndpoints.kt` header comment |
| **Dependencies on other WPs** | WP-01..WP-14. Docs are updated only after behavior is fixed. |
| **Implementation strategy** | 1. Add ProGuard/R8 keep rules to `:app` for kotlinx.serialization, Room, OkHttp, and Compose. 2. Populate `venice-sdk/consumer-rules.pro` with keep rules for `@Serializable` classes and companions. 3. Add release signing config reading from environment variables; configure CI secrets. 4. Update `VeniceEndpoints.kt` header comment to current schema version `20260814.194349`. 5. Update `README.md` API version. 6. Reconcile `FeatureCatalog`, parity matrix, and changelog statuses. 7. Update `VENICE_API_SOURCE_MANIFEST` and `VENICE_API_PORT_MATRIX` to reflect implemented endpoints and actual test files. 8. Fix `SDK_EXAMPLES.md` to use `textFragment`/`argumentsFragment`. 9. Update candidate docs pack review header to current HEAD or add historical-commit note. 10. Remove or re-add dependencies only as they are consumed (BUILD-04/07). |
| **Tests required** | Release build + minified smoke test; verify signed APK certificate; verify SDK serialization round-trip in a minified consumer app. |
| **Validation commands** | `./gradlew :app:assembleRelease :venice-sdk:assembleRelease`; install release APK; run serialization smoke test. |
| **Compatibility concerns** | Release builds will behave differently from debug due to R8; must verify before any distribution. Signing config must be documented for CI operators. |
| **Acceptance criteria** | Release APK/AAB is signed. R8 release build does not crash on startup. All referenced docs agree with current code and upstream schema version. |

---

## Dependency graph summary

```
WP-01 (compile fix)
  │
  ▼
WP-02 (CI/CD + deps)
  │
  ├──► WP-03 (chat contract)
  ├──► WP-04 (image/media contract)
  ├──► WP-05 (video/audio queue)
  │
  ▼
WP-06 (SDK API stability)
  │
  ├──► WP-07 (transport/timeouts)
  ├──► WP-08 (ViewModel lifecycle)
  ├──► WP-09 (persistence)
  ├──► WP-10 (security/redaction)
  ├──► WP-11 (UI/a11y/l10n)
  ├──► WP-12 (background/export)
  ├──► WP-13 (performance/concurrency)
  ├──► WP-14 (test coverage)
  │
  ▼
WP-15 (release hardening + docs)
```

## WP list (one-line objectives)

| WP | One-line objective |
|----|--------------------|
| WP-01 | Fix the missing `ImageClient` import so the repo compiles and local Gradle gates run. |
| WP-02 | Add GitHub Actions PR/release workflows, remove unused deps, and align build tooling. |
| WP-03 | Align chat/streaming with the Venice `/chat/completions` contract and fix duplicate-message bug. |
| WP-04 | Fix image endpoints to return binary responses, support multipart, and expose response headers. |
| WP-05 | Implement queued video/audio quote/poll/retrieve/complete and transcription endpoints. |
| WP-06 | Stabilize SDK public API: model discovery, capabilities, typed exceptions, transport encapsulation. |
| WP-07 | Add configurable timeouts, structured error classification, and explicit approval for paid operations. |
| WP-08 | Use framework `ViewModel`s, `SavedStateHandle`, and correct cancellation/duplicate-submission guards. |
| WP-09 | Enforce Room singleton, referential integrity, migrations, and at-rest encryption. |
| WP-10 | Wire redaction, harden `SecureSecretStore`, and minimize secret lifetime in UI. |
| WP-11 | Localize UI, fix accessibility, follow system theme, and honestly surface scaffolded features. |
| WP-12 | Move queued work to WorkManager, add bounded media cache, and implement encrypted backup/export. |
| WP-13 | Eliminate main-thread blocking work, resource leaks, redundant network calls, and races. |
| WP-14 | Backfill unit tests for chat, image, video, audio, ViewModels, persistence, security, and fixtures. |
| WP-15 | Author R8 keep rules, add release signing, and reconcile all docs with fixed behavior. |
