# Audit: App ViewModels + Coroutines + Lifecycle

**Scope:** `ChatViewModel.kt`, `ImageViewModel.kt`, `ChatViewModelTest.kt`, `FeatureCatalogTest.kt`, `VeniceForgeApp.kt`, `MainActivity.kt`  
**Auditor focus:** coroutine scope ownership, cancellation propagation, `CancellationException` handling, `StateFlow`/`SharedFlow` races, stale-job overwrites, duplicate submission, process-death/state restoration, API-key loading path into the SDK, error truthfulness, retry/idempotency, blocking work on Main, navigation/rotation/backgrounding behavior of in-flight streams/generations.

---

## Ledger

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` | 200 | Y | 6 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 122 | Y | 5 |
| `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt` | 246 | Y | 1 |
| `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt` | 12 | Y | 0 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 266 | Y | 2 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` | 17 | Y | 0 |

**Dependencies consulted (not in ledger):** `ChatClient.kt`, `ChatStreamAccumulator.kt`, `ChatStreamChunk.kt`, `ChatRequest.kt`, `ImageClient.kt`, `ImageModels.kt`, `ChatRepository.kt`, `SecureSecretStore.kt`, `CapabilitiesRepository.kt`, `ModelCatalog.kt`, `VeniceForgeSdk.kt`, `VeniceSdkException.kt`, `MessageEntity.kt`, `MessageDao.kt`, `ConversationDao.kt`, `DataServices.kt`, `ProfileRepository.kt`, `ChatScreen.kt`, `ImageScreen.kt`, `ConfigScreen.kt`, `swagger.yaml` (upstream HEAD `6e69346b`, info.version `20260814.194349`).

---

## Findings

### VM-01 | P1 | CONFIRMED
**Duplicate chat submissions create multiple message pairs and race on `streamJob`, leaving orphan streams.**

- **Area:** Chat / Coroutine lifecycle  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
- **Lines:** 86–187  
- **Symbol:** `submit(String)`

`submit()` performs synchronous validation and inserts placeholder `MessageEntity` objects, then schedules the actual streaming work in `viewModelScope.launch`. It never guards against a second invocation while a stream is already in flight. Because the `streamJob` field is assigned inside the asynchronous block (line 150), two rapid calls produce two user/assistant message pairs and two concurrent inner `launch` coroutines; the second assignment to `streamJob` overwrites the first. The orphan stream continues to update Room and `_state`, but `cancel()` can only cancel the job currently referenced by `streamJob`.

**Evidence:**
```kotlin
// ChatViewModel.kt:86-96  synchronous validation, no in-flight guard
fun submit(text: String) {
    val convId = conversationId ?: return
    val apiKey = apiKeyProvider() ?: run { ... }
    val modelId = _state.value.modelId
    if (modelId.isNullOrBlank()) { ... }
    // ... creates userMsg + assistantMsg immediately

    viewModelScope.launch {
        // ...
        streamJob = launch {               // ChatViewModel.kt:150
            chatClient.streamChat(apiKey, req).collect { chunk ->
                // orphan stream continues here
            }
        }
    }
}
```

**Expected:** `submit()` atomically checks an in-flight flag (or the current `streamJob`) and refuses/queues a second request until the first finishes or is cancelled.  
**Actual:** Multiple in-flight streams can coexist; only the most recently assigned `streamJob` is cancellable.  
**Impact:** Duplicate user messages, duplicate billable API calls, inconsistent conversation context, and loss of cancellation control.  
**Root cause:** State-machine and job ownership are split across synchronous setup and an unguarded asynchronous launch.  
**Related occurrences:** `ImageViewModel.generateImage()` / `editImage()` (VM-04).  
**Venice reference:** N/A (client-side lifecycle).  
**Android/Kotlin reference:** `viewModelScope` docs; coroutine job ownership best practice.  
**Remediation:** Add a `streamJob?.isActive == true` guard at the top of `submit()`; or use a single `Mutex`/`AtomicBoolean` in-flight flag and assign `streamJob` before launching the streaming child.  
**Tests required:** Unit test that calls `submit()` twice rapidly and asserts only one `ChatClient.streamChat` invocation and one user/assistant pair.  
**Compatibility impact:** Behavior change; UI already disables send while streaming, so no user-facing regression.

---

### VM-02 | P1 | CONFIRMED
**Stream exceptions are not caught, leaving `isStreaming = true` and the assistant message in `STREAMING`/`PENDING` forever.**

- **Area:** Chat / Error handling  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
- **Lines:** 150–185  
- **Symbol:** inner `launch { ... collect { ... } }`

The streaming `collect` block handles terminal events emitted by `ChatClient` (`Finish`, `Error`) but does not wrap `collect` in a `try/catch`. If `chatClient.streamChat()` throws (e.g., `VeniceSdkException.Network`, `Protocol`, or an unexpected `Throwable`), the coroutine fails with an unhandled exception. `_state.isStreaming` remains `true` and the assistant `MessageEntity` remains `STREAMING`/`PENDING` in Room.

**Evidence:**
```kotlin
// ChatViewModel.kt:150-185
streamJob = launch {
    val accumulator = ChatStreamAccumulator()
    chatClient.streamChat(apiKey, req).collect { chunk ->   // no try/catch
        ...
    }
}
```

**Expected:** Any exception during streaming transitions `isStreaming` to `false`, records a user-facing error, and marks the assistant message `FAILED`.  
**Actual:** Unhandled exception leaves UI stuck in streaming state and DB in `STREAMING`/`PENDING`.  
**Impact:** UI freeze indicator, unrecoverable conversation state, user confusion.  
**Root cause:** Missing exception boundary around the streaming collection.  
**Related occurrences:** `ImageViewModel` catches too broadly instead (VM-03).  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Kotlin Flow exception handling; `viewModelScope` uncaught exceptions cancel the scope.  
**Remediation:** Wrap the `collect` in `try { ... } catch (e: Throwable) { ... } finally { ... }`; in `catch`, update assistant status to `FAILED` and `_state.error` to a safe message; in `finally`, set `isStreaming = false`. Re-throw `CancellationException` if appropriate.  
**Tests required:** Unit test injecting a `ChatClient` that throws `VeniceSdkException.Network` and asserting final `isStreaming == false`, assistant status `FAILED`, and non-null error.  
**Compatibility impact:** Fixes stuck UI; no breaking change.

---

### VM-03 | P1 | CONFIRMED
**`ImageViewModel` catches `CancellationException` in broad `catch (e: Exception)`, reporting cancellation as a user-facing error.**

- **Area:** Image / Coroutine lifecycle  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 65–80, 97–120  
- **Symbol:** `generateImage()`, `editImage()`

Both image operations use `catch (e: Exception)`. `CancellationException` (JVM type alias for `java.util.concurrent.CancellationException`) extends `IllegalStateException` → `Exception`, so it is caught and surfaced as `error = e.message`. This turns normal coroutine cancellation (e.g., ViewModel cleared, user navigating away, scope cancelled) into a displayed error such as "StandaloneCoroutine was cancelled".

**Evidence:**
```kotlin
// ImageViewModel.kt:65-79
viewModelScope.launch {
    try {
        val bytes = imageClient.generateBinary(apiKey, req)
        ...
    } catch (e: Exception) {
        _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
    }
}
```

**Expected:** `CancellationException` re-thrown (or caught explicitly and re-thrown) so cancellation propagates cleanly; only non-cancellation errors update `error`.  
**Actual:** Cancellation is swallowed and displayed as an error.  
**Impact:** User sees spurious errors; cancellation semantics violated.  
**Root cause:** Overly broad exception handler.  
**Related occurrences:** `ImageScreen.kt:183` also has `catch (e: Exception)` when decoding a result bitmap; that one is local UI fallback and less severe.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Kotlin `CancellationException` propagation; `viewModelScope` cancellation.  
**Remediation:** Catch specific SDK exception types (`VeniceSdkException`) and re-throw `CancellationException`:
```kotlin
catch (e: CancellationException) { throw e }
catch (e: VeniceSdkException) { ... }
catch (e: Exception) { ... }
```
**Tests required:** Unit test that cancels `viewModelScope` during `generateImage()` and asserts `error` remains null.  
**Compatibility impact:** Behavior change; cancellation no longer surfaces as error.

---

### VM-04 | P1 | CONFIRMED
**Image generation/edit lacks a ViewModel-level guard against duplicate billable submissions.**

- **Area:** Image / Coroutine lifecycle  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 52–81, 83–121  
- **Symbol:** `generateImage()`, `editImage()`

`generateImage()` and `editImage()` read `_uiState.value`, validate inputs, then synchronously set `isGenerating = true` and launch the request. There is no check of `state.isGenerating` at the start, so a second call that races past the UI button-disable can still launch a second coroutine. Unlike `ChatViewModel`, there is not even a `Job` reference to track the in-flight work.

**Evidence:**
```kotlin
// ImageViewModel.kt:52-65
fun generateImage() {
    val state = _uiState.value
    val model = state.selectedModelId ?: return
    val prompt = state.prompt.takeIf { it.isNotBlank() } ?: return
    ...
    _uiState.update { it.copy(isGenerating = true, error = null, resultImageUri = null) }
    viewModelScope.launch { ... }
}
```

**Expected:** Early return if `state.isGenerating`; or single `Job` reference and atomic state transition.  
**Actual:** Multiple concurrent image requests possible.  
**Impact:** Duplicate billed image generations/edits; wasted credits; UI state thrashing.  
**Root cause:** No in-flight guard in ViewModel; relies solely on UI disabling buttons.  
**Related occurrences:** `ChatViewModel.submit()` (VM-01).  
**Venice reference:** Image endpoints are paid per request (`swagger.yaml` image request schemas).  
**Android/Kotlin reference:** N/A.  
**Remediation:** Add `if (_uiState.value.isGenerating) return` at the top of both functions; optionally keep a `Job` reference for cancellation.  
**Tests required:** Unit test that invokes `generateImage()` twice and asserts only one `ImageClient.generateBinary` call.  
**Compatibility impact:** Prevents accidental duplicates; no breaking change.

---

### VM-05 | P1 | CONFIRMED
**API-key provider and Keystore decryption run on the Main thread inside `submit()` and image operations.**

- **Area:** Security / Performance  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt:88`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:56,88`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:91,102,125`
- **Symbol:** `apiKeyProvider()` invocations

`apiKeyProvider` is implemented as `{ secureStore.loadApiKey(pid) }`. `SecureSecretStore.loadApiKey` performs AES-GCM decryption via Android Keystore (lines 38–55). In `ChatViewModel.submit()` and `ImageViewModel.generateImage()`/`editImage()`, the provider is called synchronously on the caller thread — the UI/Main thread — because these functions are not `suspend` and `viewModelScope.launch` has not yet switched dispatchers. The same blocking call happens in `VeniceForgeApp` inside `LaunchedEffect(profileId)` (line 125).

**Evidence:**
```kotlin
// ChatViewModel.kt:88
val apiKey = apiKeyProvider() ?: run { ... }

// ImageViewModel.kt:56
val apiKey = apiKeyProvider()

// VeniceForgeApp.kt:125
val key = pid?.let(secureStore::loadApiKey)
```

**Expected:** API-key retrieval is performed on a background dispatcher (e.g., `withContext(Dispatchers.IO)` or via a `suspend` provider).  
**Actual:** Keystore decryption runs on the Main thread, risking ANR.  
**Impact:** UI jank or ANR; violates the principle of keeping crypto off the main thread.  
**Root cause:** Provider lambda is invoked synchronously from non-suspend ViewModel functions that run on Main.  
**Related occurrences:** Same provider pattern in `VeniceForgeApp`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Android Keystore best practice: cryptographic operations must not run on the main thread.  
**Remediation:** Make `apiKeyProvider` a `suspend` lambda and call it inside `viewModelScope.launch`; or change `submit()`/`generateImage()` to `suspend` and dispatch before reading the key.  
**Tests required:** Add Main-dispatcher test asserting provider is not called synchronously on the calling thread.  
**Compatibility impact:** API surface change for `apiKeyProvider`; callers must update.

---

### VM-06 | P1 | CONFIRMED
**Base64 decoding of edited image response runs on the Main thread.**

- **Area:** Image / Performance  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 111–114  
- **Symbol:** `editImage()`

After `imageClient.edit()` returns, the ViewModel decodes the base64 image string on the `viewModelScope` dispatcher (Main by default):
```kotlin
val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
```
`saveBytesToCache` is suspend and switches to IO, but the decode itself is synchronous CPU work on Main.

**Evidence:**
```kotlin
// ImageViewModel.kt:111-114
val base64 = response.images?.firstOrNull()
val uri = if (base64 != null) {
    val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
    saveBytesToCache(decodedBytes)
} else null
```

**Expected:** Decode large byte arrays on `Dispatchers.IO` or `Dispatchers.Default`.  
**Actual:** Blocking CPU work on Main for potentially large image payloads.  
**Impact:** ANR/jank when editing high-resolution images.  
**Root cause:** Missing dispatcher switch around base64 decode.  
**Related occurrences:** `generateImage()` avoids this because `imageClient.generateBinary()` already returns bytes from IO.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Android "Summary of performance and view hierarchies" — keep heavy work off Main.  
**Remediation:** Wrap decode in `withContext(Dispatchers.IO) { Base64.decode(...) }`.  
**Tests required:** None specific; covered by general Main-thread assertion tests.  
**Compatibility impact:** None.

---

### VM-07 | P1 | CONFIRMED
**Conversation identity and streaming state are lost on process death; no SavedState restoration.**

- **Area:** Chat / Lifecycle / Process death  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
- **Lines:** 46–74, 54–55  
- **Symbol:** `conversationId`, `streamJob`, `_state`

`conversationId` and `streamJob` are plain `var` properties. `ChatUiState` is held in a `MutableStateFlow`. None of these survive process death. When the system kills the app and the user returns, `init` recreates or picks the most recent conversation, which may not be the active one. Any in-flight stream is cancelled and its result lost, even though the server may have billed for the tokens.

**Evidence:**
```kotlin
// ChatViewModel.kt:54-55
private var conversationId: String? = null
private var streamJob: Job? = null
```

**Expected:** Critical identifiers (conversationId, at minimum) are persisted via `SavedStateHandle` or Room; in-flight billable work is handed off to a process-surviving worker (e.g., WorkManager) for recovery.  
**Actual:** All transient state is lost on process death.  
**Impact:** User returns to the wrong conversation; paid generations are lost; poor UX.  
**Root cause:** ViewModel has no saved-state integration and no durable work boundary.  
**Related occurrences:** `ImageViewModel` result URI (VM-08).  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `SavedStateHandle`; process death best practices; WorkManager for deferrable/restartable work.  
**Remediation:** Accept `SavedStateHandle` in the constructor and persist `conversationId`; for billable in-flight operations, consider a foreground service or WorkManager with a pending-job queue.  
**Tests required:** Process-death recreation test (e.g., Robolectric `recreate()` or instrumented test) asserting the same conversation is restored.  
**Compatibility impact:** Adds `SavedStateHandle` dependency; behavior change on process restore.

---

### VM-08 | P2 | CONFIRMED
**Generated image URI is lost on process death; no SavedState restoration.**

- **Area:** Image / Lifecycle / Process death  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 19, 31–32  
- **Symbol:** `resultImageUri`, `_uiState`

The result image URI is held only in `ImageUiState` and the backing `MutableStateFlow`. On process death, the URI is lost and the user must regenerate (and repay) the image.

**Evidence:**
```kotlin
// ImageViewModel.kt:15-22
data class ImageUiState(
    ...
    val resultImageUri: Uri? = null,
    ...
)
```

**Expected:** Result URI and in-flight request state survive process death via `SavedStateHandle` or persisted job state.  
**Actual:** Result is transient.  
**Impact:** User loses paid/generated media after process death.  
**Root cause:** No saved-state or durable job persistence.  
**Related occurrences:** `ChatViewModel` conversation/stream state (VM-07).  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `SavedStateHandle`; `rememberSaveable` for Compose UI state.  
**Remediation:** Persist `resultImageUri` and request parameters in `SavedStateHandle`; use WorkManager for generation/edit jobs.  
**Tests required:** Process-death recreation test asserting restored URI.  
**Compatibility impact:** Adds `SavedStateHandle`; behavior change on restore.

---

### VM-09 | P2 | CONFIRMED
**User cancellation does not mark the assistant message `CANCELLED` in Room.**

- **Area:** Chat / Cancellation  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
- **Lines:** 189–193  
- **Symbol:** `cancel()`

`cancel()` cancels `streamJob` and updates `_state.isStreaming = false`, but it never updates the assistant message's `MessageStatus` in the database. The message remains `PENDING` (if cancelled before first delta) or `STREAMING` (if cancelled mid-stream).

**Evidence:**
```kotlin
// ChatViewModel.kt:189-193
fun cancel() {
    streamJob?.cancel()
    streamJob = null
    _state.update { it.copy(isStreaming = false) }
}
```

**Expected:** On cancellation, the assistant message is updated to `MessageStatus.CANCELLED`.  
**Actual:** Message stays `PENDING`/`STREAMING` in Room.  
**Impact:** Conversation history shows incomplete messages as in-progress forever.  
**Root cause:** `cancel()` only updates UI state, not persistence.  
**Related occurrences:** N/A.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Room entity status semantics.  
**Remediation:** In `cancel()`, call `chatRepo.updateAssistantText(..., status = MessageStatus.CANCELLED)` using the last known assistant ID.  
**Tests required:** Unit test cancelling mid-stream and asserting assistant status is `CANCELLED`.  
**Compatibility impact:** Fixes history accuracy; no breaking change.

---

### VM-10 | P2 | CONFIRMED
**Image error messages expose raw exception text instead of safe, user-facing messages.**

- **Area:** Image / Error truthfulness  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 77–78, 117–118  
- **Symbol:** `generateImage()`, `editImage()`

Both functions set `error = e.message ?: "Unknown error"`. `VeniceSdkException` messages contain technical detail such as HTTP status and request IDs (see `VeniceSdkException.kt:21,31,42,52,63,75`). While these messages do not leak credentials, they are not user-friendly and may include internal request IDs or stack-trace-like strings for unexpected exceptions.

**Evidence:**
```kotlin
// ImageViewModel.kt:77-78
catch (e: Exception) {
    _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
}
```

**Expected:** Map SDK exceptions to localized, user-facing strings (e.g., "Network error. Please try again.", "Invalid API key.").  
**Actual:** Raw exception messages displayed.  
**Impact:** Poor UX; potential leakage of internal request IDs.  
**Root cause:** Direct use of `Throwable.message` as UI text.  
**Related occurrences:** `ChatViewModel` surfaces stream chunk errors directly (`ChatStreamChunk.Error.message`), which are server-provided and generally safe.  
**Venice reference:** `VeniceSdkException` hierarchy in `venice-sdk`.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Pattern-match `e` to `VeniceSdkException` subtypes and produce localized strings; fall back to a generic message.  
**Tests required:** Unit tests for each `VeniceSdkException` subtype asserting a known user-facing error string.  
**Compatibility impact:** UI text change.

---

### VM-11 | P2 | CONFIRMED
**No explicit retry logic is good, but billable image operations have no idempotency or duplicate-submission defense.**

- **Area:** Image / Billing / Reliability  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 52–121  
- **Symbol:** `generateImage()`, `editImage()`

The ViewModel does not retry failed requests, which is correct for billable endpoints. However, there is also no idempotency key or client-generated request ID passed to Venice, and no local deduplication. A network timeout that the SDK reports as failure may actually have been processed by Venice, so a manual user retry could bill twice for the same prompt.

**Evidence:**
```kotlin
// ImageViewModel.kt:67-74
val req = GenerateImageRequest(
    model = model,
    prompt = prompt,
    height = 512,
    width = 512,
    returnBinary = true
)
val bytes = imageClient.generateBinary(apiKey, req)
```

**Expected:** For paid generation, either (a) use a Venice-supported idempotency mechanism, or (b) persist a pending job and reconcile before retrying.  
**Actual:** Each tap is a new, independent billed request.  
**Impact:** Potential double billing on user retry after ambiguous failures.  
**Root cause:** No request-scoped idempotency token or durable job tracking.  
**Related occurrences:** `ChatViewModel` streaming is less idempotent by nature; image generation is the primary concern.  
**Venice reference:** `swagger.yaml` image request schemas do not expose an idempotency-key field.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Document the limitation; consider a "pending generation" queue with server-side reconciliation if Venice later supports idempotency keys; at minimum, disable the generate button from the moment the request starts until completion/error.  
**Tests required:** N/A (requires Venice API support).  
**Compatibility impact:** None today.

---

### VM-12 | P2 | CONFIRMED
**`ChatViewModelTest` does not exercise cancellation, errors, rapid submissions, or process-death scenarios.**

- **Area:** Chat / Test coverage  
- **Module:** `:app` (test)  
- **File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`  
- **Lines:** 58–245  
- **Symbol:** all test methods

The existing tests verify happy-path streaming, multi-turn context, and conversation selection. They do not cover:
- Cancelling a stream and asserting `CANCELLED` status.
- A `ChatClient` that throws an exception.
- Two rapid `submit()` calls.
- Process recreation / `SavedStateHandle` behavior.

**Evidence:**
```kotlin
// ChatViewModelTest.kt:58-245
@Test fun `submit writes user message and accumulates assistant chunks`() = runTest { ... }
@Test fun `multi-turn chat constructs request with complete prior conversation context`() = runTest { ... }
@Test fun `init picks most recent existing conversation instead of creating new one`() = runTest { ... }
```

**Expected:** Tests for failure modes and concurrency guards.  
**Actual:** Only happy-path coverage.  
**Impact:** Regressions in cancellation, error handling, and duplicate submission are likely to go undetected.  
**Root cause:** Test plan focused on success paths.  
**Related occurrences:** No `ImageViewModelTest` exists at all.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `kotlinx-coroutines-test` documentation.  
**Remediation:** Add tests for VM-01, VM-02, VM-09, and VM-07. Create `ImageViewModelTest` covering VM-03, VM-04, VM-10.  
**Tests required:** See individual findings.  
**Compatibility impact:** N/A.

---

### VM-13 | P2 | CONFIRMED
**`VeniceForgeApp` holds `profileId` in non-saved `mutableStateOf`, causing ViewModels to be null briefly after process death.**

- **Area:** App wiring / Process death  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt`  
- **Lines:** 83–96, 122–135  
- **Symbol:** `profileId`, `chatViewModel`, `imageViewModel`

```kotlin
var profileId by remember { mutableStateOf<String?>(null) }
LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }
```

`profileId` is not `rememberSaveable`. After process death and restoration, `profileId` is `null` until `profileRepo.ensureDefault()` completes. During that window `chatViewModel` and `imageViewModel` are `null`, so the UI shows the "No API key loaded" placeholder even if a key exists. The actual data is safe in Room/Keystore, but the transient UX is degraded.

**Expected:** `profileId` survives process death via `rememberSaveable` or is restored from a durable source before first composition.  
**Actual:** Brief null-profile flash after process death.  
**Impact:** UI flicker / misleading "no API key" message.  
**Root cause:** Compose state not saved across process death.  
**Related occurrences:** VM-07, VM-08.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `rememberSaveable` docs.  
**Remediation:** Change to `rememberSaveable { mutableStateOf<String?>(null) }` or read the default profile ID synchronously from a saved-state source.  
**Tests required:** Process-death recreation test asserting ViewModels are non-null immediately after restore.  
**Compatibility impact:** None.

---

### VM-14 | P2 | INFERRED
**Image generation/edit requests omit `safe_mode`, defaulting to the API default of `true`.**

- **Area:** Image / Venice API semantics  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 67–73, 105–109  
- **Symbol:** `GenerateImageRequest`, `EditImageRequest`

`GenerateImageRequest` and `EditImageRequest` are constructed without `safeMode`. Per `swagger.yaml`:

> `safe_mode`: type boolean, default true, description "Whether to use safe mode. If enabled, this will blur images that are classified as having adult content." (`swagger.yaml:2666-2671`, `3017-3022`, `3108-3113`, `3206-3211`)

Project `AGENTS.md` states: "Preserve explicit `safe_mode=false` when selected." There is currently no UI or ViewModel state for `safe_mode`, so the effective value is the API default `true`.

**Expected:** Either explicit UI control for `safe_mode` or an explicit default chosen by the app and passed in requests.  
**Actual:** `safe_mode` omitted; API default `true` applied.  
**Impact:** Unexpected content filtering/blurring for a Venice client; violates project rule if a future setting is ignored.  
**Root cause:** No `safe_mode` field in UI or ViewModel.  
**Related occurrences:** `ChatRequest.VeniceParameters.safeMode` is also not set, but chat `safe_mode` semantics differ and are not covered by the same AGENTS rule.  
**Venice reference:** `swagger.yaml` image request `safe_mode` fields.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Add `safeMode` to `ImageUiState`, expose a toggle, and pass it to requests.  
**Tests required:** Unit test asserting `safeMode` is serialized when set.  
**Compatibility impact:** Behavior change if default is switched to `false`.

---

### VM-15 | P3 | CONFIRMED
**`FeatureCatalog.byId` is non-nullable and throws, making the fallback in `VeniceForgeApp` unreachable.**

- **Area:** Feature catalog / Defensive coding  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:47`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:69`
- **Symbol:** `FeatureCatalog.byId`, `selected`

```kotlin
// FeatureCatalog.kt:47
fun byId(id: String): AppFeature = all.first { it.id == id }

// VeniceForgeApp.kt:69
val selected = remember(selectedId) { FeatureCatalog.byId(selectedId) ?: FeatureCatalog.byId("chat") }
```

`List.first { ... }` throws `NoSuchElementException` when no match is found, so the `?:` fallback to `"chat"` is dead code. In practice `selectedId` is only ever set to known feature IDs, but the code is misleading and would crash if an invalid ID were ever introduced.

**Expected:** `byId` returns `AppFeature?` and the fallback is meaningful.  
**Actual:** Non-null function makes fallback unreachable.  
**Impact:** Potential crash on corrupted/unknown feature ID.  
**Root cause:** Return type mismatch with fallback intent.  
**Related occurrences:** N/A.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Kotlin `first` vs `firstOrNull`.  
**Remediation:** Change `FeatureCatalog.byId` to use `firstOrNull()` and return `AppFeature?`; update callers.  
**Tests required:** Add test for unknown ID returning null/fallback.  
**Compatibility impact:** API signature change for `byId`.

---

## Positive observations

- `viewModelScope` is used correctly as the lifecycle-bound scope; no custom scopes were introduced that would outlive the ViewModel.
- `ChatClient.streamChat` uses `callbackFlow` with `invokeOnCompletion` to cancel the OkHttp `Call` on coroutine cancellation, so cancellation propagates to the network layer.
- `ImageViewModel` does not retry failed image requests, avoiding accidental double billing.
- API keys are never persisted in plaintext; `SecureSecretStore` uses Android Keystore AES-GCM.
- `ChatRepository` scopes all persistence to `profileId` and uses Room transactions.

---

## Summary

**Files reviewed:** 6 production/test files in scope + 14 supporting files for context.  
**Findings:** 15 total — P1: 6, P2: 8, P3: 1.

**Most important findings:**
- **VM-01** — Chat duplicate submissions race on `streamJob` and create orphan streams.
- **VM-02** — Chat stream exceptions leave UI stuck in streaming state.
- **VM-03** — Image ViewModel swallows `CancellationException` as user error.
- **VM-04** — Image generation/edit has no ViewModel-level duplicate guard.
- **VM-05** — Keystore decryption runs on the Main thread via `apiKeyProvider()`.
- **VM-06** — Base64 image decode blocks the Main thread.
- **VM-07** — Chat conversation/stream state lost on process death.
- **VM-08** — Generated image URI lost on process death.
- **VM-09** — Cancellation does not mark assistant message `CANCELLED`.
- **VM-10** — Raw exception messages shown to users for image errors.
- **VM-12** — Test coverage missing for failure modes and concurrency.
- **VM-13** — `profileId` not saved across process death, causing transient null ViewModels.
