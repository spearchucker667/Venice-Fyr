# P1 Findings — Venice Fyr Exhaustive Audit Consolidation

**Severity:** P1
**Total findings after deduplication:** 44

| Status | Count |
|--------|-------|
| CONFIRMED | 43 |
| SUSPECTED | 1 |

## APP-UI-001 | `ChatViewModel` and `ImageViewModel` extend `androidx.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Lifecycle / Architecture
**Module:** :app
**File:** VeniceForgeApp.kt
**Lines:** 86–120
**Symbol:** chatViewModel`, `imageViewModel
**Also reported as:** ARCH-01

**Area:** Lifecycle / Architecture | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Lines:** 86–120 | **Symbol:** `chatViewModel`, `imageViewModel`

**Evidence:**
```kotlin
val chatViewModel = remember(profileId) {
    profileId?.let { pid ->
        ChatViewModel(...)
    }
}
val imageViewModel = remember(profileId) {
    profileId?.let { pid ->
        ImageViewModel(...)
    }
}
```
`ChatViewModel` and `ImageViewModel` extend `androidx.lifecycle.ViewModel` (see `ChatViewModel.kt:46`, `ImageViewModel.kt:24`) and launch coroutines in `viewModelScope`. They are instantiated with plain `remember`, not `viewModel()` or a `ViewModelProvider`.

**Expected:** ViewModels are obtained via `viewModel()` (or `ViewModelProvider`) so they are scoped to a `ViewModelStoreOwner`, survive configuration changes, and are cleared when the owner is destroyed.

**Actual:** `remember` does not survive configuration changes and does not invoke `ViewModel.onCleared()`. On rotation, the old ViewModel instances are dropped but keep running; new instances are created for the new composition. This leaks the old ViewModels and their active streaming coroutines, and all UI state held in the ViewModel is lost across config changes.

**Impact:** Streaming chat/image operations can leak across rotation; user loses conversation state, model selection, and generation results on device rotation.

**Root cause:** ViewModels are treated as plain `@Composable remember` objects instead of lifecycle-aware components.

**Related occurrences:** `ChatScreen.kt:35` receives the leaked/recreated VM; `ImageScreen.kt:42` receives the leaked/recreated VM.

**Venice reference:** N/A (Android lifecycle contract).

**Android/Kotlin reference:** `ViewModel` is designed to be retained by a `ViewModelStore`; `remember` values are destroyed with the composition. See Android docs: "A ViewModel is always created in association with a scope... and stays in memory until the scope is permanently gone."

**Remediation:** Obtain ViewModels via `viewModel()` (or `ViewModelProvider`) keyed by `profileId`, or hoist them to a `ViewModelStoreOwner` (e.g., activity/destination). If a custom factory is needed, pass it to `viewModel(factory = ...)`.

**Tests required:** Rotation/config-change instrumentation test verifying that `ChatViewModel`/`ImageViewModel` survive and `onCleared()` is called exactly once when the Activity finishes.

**Compatibility impact:** Fixing this changes ViewModel lifecycle semantics; any code relying on re-creation on rotation will break.

---

---

## APP-UI-002 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Navigation / Crash
**Module:** :app
**File:** FeatureCatalog.kt` / `VeniceForgeApp.kt
**Lines:** FeatureCatalog.kt:47`, `VeniceForgeApp.kt:68–69
**Symbol:** FeatureCatalog.byId`, `selectedId
**Also reported as:** ARCH-14, VM-15

**Area:** Navigation / Crash | **Module:** `:app` | **File:** `FeatureCatalog.kt` / `VeniceForgeApp.kt` | **Lines:** `FeatureCatalog.kt:47`, `VeniceForgeApp.kt:68–69` | **Symbol:** `FeatureCatalog.byId`, `selectedId`

**Evidence:**
```kotlin
// FeatureCatalog.kt:47
fun byId(id: String): AppFeature = all.first { it.id == id }

// VeniceForgeApp.kt:68–69
var selectedId by rememberSaveable { mutableStateOf("chat") }
val selected = remember(selectedId) { FeatureCatalog.byId(selectedId) ?: FeatureCatalog.byId("chat") }
```

**Expected:** `byId` should return a nullable `AppFeature?` so the `?: FeatureCatalog.byId("chat")` fallback in `VeniceForgeApp.kt:69` can execute.

**Actual:** `byId` uses `List.first { ... }`, which throws `NoSuchElementException` when `id` is not found. The Elvis-operator fallback is unreachable dead code. If `rememberSaveable` restores an invalid `selectedId` (e.g., from an older app version or tampered state), the app crashes.

**Impact:** Potential startup/restore crash; navigation fallback is a no-op.

**Root cause:** Mismatch between nullable fallback intent and non-nullable throwing implementation.

**Related occurrences:** None in scope; `FeatureCatalog.all.first` pattern is unique here.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `Iterable.first(predicate)` throws `NoSuchElementException` if no element matches.

**Remediation:** Change `FeatureCatalog.byId` to return `AppFeature?` and use `firstOrNull { it.id == id }`.

**Tests required:** Unit test asserting `byId("unknown")` returns null; instrumentation test restoring an invalid `selectedId` falls back to "chat".

**Compatibility impact:** API signature change from non-null to nullable; update `VeniceForgeApp.kt` call site (already uses `?:`, so it becomes valid).

---

---

## ARCH-02 | The latest user message is duplicated in the multi-turn chat request context.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Chat / Venice API integration
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 99–139
**Lines:** 
**Symbol:** submit(text: String)

**The latest user message is duplicated in the multi-turn chat request context.**

- **Area:** Chat / Venice API integration  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 99–139  
- **Symbol:** `submit(text: String)`  
- **Evidence:**
  ```kotlin
  // Lines 99-123: appends the new user message AND a pending assistant placeholder.
  chatRepo.appendMessage(profileId, convId, userMsg)
  chatRepo.appendMessage(profileId, convId, assistantMsg)

  // Lines 127-139: loads the now-just-appended user message, then appends it again.
  val priorMessages = chatRepo.observeMessages(profileId, convId).first()
  val contextMessages = priorMessages
      .filter { it.status == MessageStatus.COMPLETED && it.textContent.isNotBlank() }
      .map { entity -> ... }
      .plus(ChatMessage.user(text))
  ```
  The existing test `ChatViewModelTest.kt:172-178` asserts that turn 2 sends exactly three messages (`user:Turn one`, `assistant:First answer`, `user:Turn two`). The current code produces four because the just-appended `userMsg` is already in `priorMessages` and is appended again as `ChatMessage.user(text)`.
- **Venice reference:** `.source/venice-api-docs/swagger.yaml` `ChatCompletionRequest.messages` (line 672) is the ordered list of conversation turns; duplicate consecutive `user` roles violate the expected alternating-turn contract.
- **Expected:** Each user turn appears exactly once in the request payload.
- **Actual:** The newest user turn appears twice.
- **Impact:** Model receives a malformed context, which can degrade response quality, confuse multi-turn reasoning, and waste tokens.
- **Root cause:** The current message is persisted before the request context is built, then re-added to the context list.
- **Related occurrences:** None other; the bug is localized to `ChatViewModel.submit`.
- **Remediation:** Build `contextMessages` from the existing completed messages **before** appending the new user message, or filter out the message whose `id == userMsg.id` from `priorMessages`.
- **Tests required:** The existing `ChatViewModelTest` multi-turn test already documents the expected behavior and should pass after the fix.
- **Compatibility impact:** Request shape change; no public API surface change.

---

---

## ARCH-05 | Paid/mutating operations have no explicit approval or duplicate-submission defense.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Security / Privacy / Product boundaries
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` lines 110–117; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` lines 145–159; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 52–81; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 86–187
**Lines:** 
**Symbol:** submit`, `generateImage`, `editImage

**Paid/mutating operations have no explicit approval or duplicate-submission defense.**

- **Area:** Security / Privacy / Product boundaries  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` lines 110–117; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` lines 145–159; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 52–81; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 86–187  
- **Symbol:** `submit`, `generateImage`, `editImage`  
- **Evidence:**
  - `AGENTS.md` non-negotiable boundary: “Paid/mutating operations require explicit approval and duplicate-submission defenses.”
  - `ChatScreen.kt` sends on a single `TextButton` click.
  - `ImageScreen.kt` Generate/Edit buttons immediately call `viewModel.generateImage()` / `viewModel.editImage()`.
- **Expected:** A confirmation step (dialog, sheet, or explicit approval coordinator) before incurring API costs, plus idempotency keys for retried requests.
- **Actual:** A single tap initiates a billed `/chat/completions`, `/image/generate`, or `/image/edit` request.
- **Impact:** Accidental spend; violates the project’s explicit-approval contract; no defense against retry storms.
- **Root cause:** The UI layer has no approval coordinator.
- **Related occurrences:** ConfigScreen model probe is lower risk but also lacks confirmation.
- **Remediation:** Introduce an approval coordinator that gates paid operations; attach idempotency keys to mutating requests where the Venice API supports them.
- **Tests required:** UI tests verifying the approval flow; unit tests for idempotency key generation.
- **Compatibility impact:** UX change; users will see an extra confirmation step.

---

---

## AUD-01 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Audio queued generation / music endpoints missing
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt
**Lines:** 15–46
**Symbol:** AudioClient

**Area:** Audio queued generation / music endpoints missing  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 15–46  
**Symbol:** `AudioClient`

**Evidence:** `AudioClient` contains only `speech`. `VeniceEndpoints.AUDIO_QUEUE`, `AUDIO_RETRIEVE`, `AUDIO_QUOTE`, and `AUDIO_COMPLETE` are defined but unused.

**Spec:** `swagger.yaml` defines `/audio/queue`, `/audio/retrieve`, `/audio/quote`, `/audio/complete` (lines 12280–12762) for music/audio generation.

**Expected:** SDK exposes `queue`, `retrieve`, `quote`, and `complete` methods for audio/music.

**Actual:** None of these methods exist.

**Impact:** Broken core feature. The SDK cannot generate music or queued non-speech audio, and cannot satisfy the documented audio surface.

**Root cause:** Only `/audio/speech` was implemented.

**Related occurrences:** `VeniceEndpoints.kt:42–44`; `AudioModels.kt` (no queue/quote/retrieve/complete models).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 12280–12762.

**Android/Kotlin reference:** N/A.

**Remediation:** Implement queued audio client methods and corresponding request/response models.

**Tests required:** Mock queue/retrieve/quote/complete lifecycle tests.

**Compatibility impact:** New API surface; additive.

---

---

## AUD-02 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Audio transcription endpoint missing
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt
**Lines:** 15–46
**Symbol:** AudioClient

**Area:** Audio transcription endpoint missing  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 15–46  
**Symbol:** `AudioClient`

**Evidence:** `AudioClient` has no method for `VeniceEndpoints.AUDIO_TRANSCRIPTIONS`.

**Spec:** `swagger.yaml` `/audio/transcriptions` (lines 11049–11263) accepts multipart/form-data with `file`, `model`, `response_format`, `timestamps`, `language` and returns JSON or text/plain.

**Expected:** SDK exposes `transcribe(apiKey, CreateTranscriptionRequest): TranscriptionResponse`.

**Actual:** Endpoint not implemented.

**Impact:** Missing feature. The SDK cannot perform speech-to-text.

**Root cause:** Endpoint omitted.

**Related occurrences:** `VeniceEndpoints.kt:39` defines the path but it is unused.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11049–11263.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `transcribe` method; implement multipart request builder.

**Tests required:** Mock transcription JSON and text/plain responses; verify multipart file upload.

**Compatibility impact:** New API surface; additive.

---

---

## BUILD-02 | Release build enables R8 but provides no keep rules

**Severity:** P1
**Status:** CONFIRMED
**Area:** Release build / R8
**Module:** :app
**File:** app/build.gradle.kts
**Lines:** 26-29
**Symbol:** release { isMinifyEnabled = true; isShrinkResources = true }

| Field | Value |
|-------|-------|
| **Area** | Release build / R8 |
| **Module** | `:app` |
| **File** | `app/build.gradle.kts` |
| **Lines** | 26-29 |
| **Symbol** | `release { isMinifyEnabled = true; isShrinkResources = true }` |
| **Evidence** | ```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```<br>No `proguardFiles(...)` or `consumerProguardFiles(...)` is present in any `build.gradle.kts`. |
| **Expected** | When `isMinifyEnabled = true`, the build must include ProGuard/R8 keep rules for reflection/serialization (kotlinx.serialization), Room entities, and any libraries that require them. |
| **Actual** | Release type has minification and resource shrinking enabled, but relies solely on default Android rules. |
| **Impact** | Runtime crashes in release builds: kotlinx.serialization needs `@Serializable` classes kept; Room needs `@Entity`/`@Dao` classes kept and constructors preserved; OkHttp/retrofit-style reflection may break. |
| **Root cause** | Keep rules were never authored. |
| **Related occurrences** | `venice-sdk/consumer-rules.pro` is also empty (BUILD-05). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [kotlinx.serialization ProGuard rules](https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro); [Room ProGuard rules](https://developer.android.com/training/data-storage/room). |
| **Remediation** | Add `proguardFiles(getDefaultProguardFile(...), "proguard-rules.pro")` to `:app` and author rules for serialization, Room, OkHttp, and Compose. |
| **Tests required** | Run `./gradlew :app:assembleRelease` and execute release UI tests / serialization round-trips / Room queries. |
| **Compatibility impact** | Release APK behavior will differ from debug; must verify before any release. |

---

---

## BUILD-03 | No release signing configuration

**Severity:** P1
**Status:** CONFIRMED
**Area:** Release signing
**Module:** :app
**File:** app/build.gradle.kts
**Lines:** 26-29
**Symbol:** release` build type

| Field | Value |
|-------|-------|
| **Area** | Release signing |
| **Module** | `:app` |
| **File** | `app/build.gradle.kts` |
| **Lines** | 26-29 |
| **Symbol** | `release` build type |
| **Evidence** | The only release configuration is:<br>```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
}
```<br>No `signingConfigs` block, no `signingConfig = signingConfigs.getByName(...)` assignment. |
| **Expected** | Release builds must be signed with a keystore whose credentials are supplied via environment variables or a secure CI secret store, never committed. |
| **Actual** | Release build is unsigned; `docs/RELEASE_CHECKLIST.md:44-51` lists signing items as unchecked manual tasks. |
| **Impact** | Cannot distribute a release APK/AAB through Google Play or sideloading without ad-hoc signing. Any manual signing is error-prone and risks key exposure. |
| **Root cause** | Signing configuration not implemented. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Sign your app](https://developer.android.com/studio/publish/app-signing). |
| **Remediation** | Add a `release` signing config reading `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` from environment; configure CI to inject secrets. |
| **Tests required** | Run `./gradlew :app:assembleRelease` in CI and verify APK is signed with expected certificate. |
| **Compatibility impact** | None. |

---

---

## BUILD-05 | `:venice sdk` consumer ProGuard rules are empty

**Severity:** P1
**Status:** CONFIRMED
**Area:** SDK release / consumer keep rules
**Module:** :venice-sdk
**File:** venice-sdk/build.gradle.kts`, `venice-sdk/consumer-rules.pro
**Lines:** venice-sdk/build.gradle.kts:12`; `consumer-rules.pro:1
**Symbol:** consumerProguardFiles("consumer-rules.pro")

| Field | Value |
|-------|-------|
| **Area** | SDK release / consumer keep rules |
| **Module** | `:venice-sdk` |
| **File** | `venice-sdk/build.gradle.kts`, `venice-sdk/consumer-rules.pro` |
| **Lines** | `venice-sdk/build.gradle.kts:12`; `consumer-rules.pro:1` |
| **Symbol** | `consumerProguardFiles("consumer-rules.pro")` |
| **Evidence** | `venice-sdk/build.gradle.kts:12` references `consumer-rules.pro`. The file contains only:<br>```
# Venice Forge SDK currently requires no consumer-specific keep rules.
```<br>Meanwhile `venice-sdk/src/main` contains many `@Serializable` data classes (e.g., `VeniceModel.kt:23`, `ChatRequest.kt`, `ImageModels.kt`, `AudioModels.kt`, `VideoModels.kt`). |
| **Expected** | A reusable SDK that exposes `@Serializable` models and OkHttp clients must ship consumer keep rules so consuming apps do not strip required classes. |
| **Actual** | Consumer rules file is empty; SDK consumers will rely on their own (possibly missing) rules. |
| **Impact** | Any consuming app with R8 enabled will likely crash at runtime when serializing/deserializing Venice API payloads or reflectively instantiating models. |
| **Root cause** | Keep rules were assumed unnecessary. |
| **Related occurrences** | `:app` release type also lacks keep rules (BUILD-02). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Library consumer ProGuard rules](https://developer.android.com/studio/projects/android-library#Considerations); kotlinx.serialization keep rules. |
| **Remediation** | Populate `consumer-rules.pro` with keep rules for `@Serializable` classes, companion objects, and any classes accessed via reflection. |
| **Tests required** | Build a minimal consumer app with `isMinifyEnabled = true` and verify Venice SDK serialization round-trips. |
| **Compatibility impact** | Corrects a latent runtime incompatibility for SDK consumers. |

---

---

## CHAT-01 | Non streaming `/chat/completions` is unsupported and `stream=false` is broken

**Severity:** P1
**Status:** CONFIRMED
**Area:** Chat completions
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 28-98
**Symbol:** ChatClient.streamChat

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Chat completions
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 28-98
- **Symbol:** `ChatClient.streamChat`

**Evidence:**

```kotlin
open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = callbackFlow { ... }
```

`ChatClient` exposes only `streamChat`; there is no non-streaming method. `ChatRequest.stream` defaults to `true` (`ChatRequest.kt:61`), but the client always sends the request and expects an `text/event-stream` response (`ChatClient.kt:34`). If a caller sets `stream=false`, the SDK will still try to parse the JSON response as SSE and fail.

**Spec:** swagger.yaml lines 1373-1376 define `stream` as optional boolean defaulting to `false`; the endpoint supports both streaming and non-streaming modes.

**Expected:** The SDK either rejects `stream=false` with a clear error or provides a typed non-streaming completion method and response type.

**Actual:** Only streaming is implemented; non-streaming responses are not handled.

**Impact:** Consumers cannot use the non-streaming path; any accidental `stream=false` request will fail.

**Root cause:** Single-method design that assumes streaming.

**Related occurrences:** `ChatRequest.kt:61`.

**Venice reference:** swagger.yaml:1373-1376, 6234-6780.

**Android/Kotlin reference:** N/A.

**Remediation:** Add a `chatCompletion(apiKey, request): ChatCompletionResponse` method for `stream=false`, or explicitly throw if `stream=false` is passed to `streamChat`.

**Tests required:** Unit tests for non-streaming JSON response parsing; integration test with `stream=false`.

**Compatibility impact:** New API surface; backward-compatible if added as a new method.

---

---

## CHAT-02 | `ChatMessage.content` is string only; multimodal/file/prompt caching content parts are unsupported

**Severity:** P1
**Status:** CONFIRMED
**Area:** Request serialization
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 13-26
**Symbol:** ChatMessage

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Request serialization
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 13-26
- **Symbol:** `ChatMessage`

**Evidence:**

```kotlin
@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    ...
)
```

**Spec:** swagger.yaml lines 678-955 describe `content` as `anyOf` `string` or an array of content part objects (`text`, `image_url`, `input_audio`, `video_url`, `file`), each optionally carrying `cache_control` for prompt caching.

**Expected:** `content` accepts polymorphic content parts so callers can send images, audio, video, files, and cache markers.

**Actual:** `content` is `String?` only. Any attempt to send multimodal/file inputs or cache control fails at serialization time.

**Impact:** Breaks vision, audio, video, document Q&A, and prompt-caching features advertised by the API.

**Root cause:** Over-simplified message model.

**Related occurrences:** `ChatRequest.kt:13-26`; content part types absent across the SDK.

**Venice reference:** swagger.yaml:678-955; `guides/features/file-inputs.mdx` lines 33-144; `guides/features/prompt-caching.mdx` lines 96-119.

**Android/Kotlin reference:** kotlinx.serialization polymorphism (`@Serializable(with = ...)` / sealed class).

**Remediation:** Model `content` as a `JsonElement` or a sealed class of content parts; keep string helper constructors.

**Tests required:** Serialization round-trips for each content part type; fixture tests with cache control.

**Compatibility impact:** Breaking change to `ChatMessage` constructor; needs migration helpers.

---

---

## CHAT-03 | Large portions of the `/chat/completions` request schema are unimplemented

**Severity:** P1
**Status:** CONFIRMED
**Area:** Request schema coverage
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 58-68
**Symbol:** ChatRequest

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Request schema coverage
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 58-68
- **Symbol:** `ChatRequest`

**Evidence:**

```kotlin
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null,
    val tools: List<ToolSpec>? = null,
    @SerialName("venice_parameters") val veniceParameters: VeniceParameters? = null,
)
```

**Spec:** swagger.yaml `ChatCompletionRequest` (lines 633-1673) defines many additional fields: `frequency_penalty`, `presence_penalty`, `logprobs`, `top_logprobs`, `min_p`, `min_temp`, `max_temp`, `n`, `stop`, `stop_token_ids`, `seed`, `repetition_penalty`, `prompt_cache_key`, `prompt_cache_retention`, `reasoning`, `reasoning_effort`, `response_format`, `tool_choice`, `parallel_tool_calls`, `fallbacks`, `store`, `verbosity`, `text`, `include`, `metadata`, `user`.

**Expected:** SDK exposes the commonly supported parameters, especially `response_format`, `reasoning`/`reasoning_effort`, `stop`, `tool_choice`, `parallel_tool_calls`, `stream_options`, `prompt_cache_key`.

**Actual:** Only model, messages, stream, temperature, top_p, max_tokens, max_completion_tokens, tools, and venice_parameters are exposed.

**Impact:** Consumers cannot use structured outputs, reasoning controls, stop sequences, tool-choice tuning, prompt caching, or many OpenAI-compatible parameters.

**Root cause:** Minimal initial request model.

**Related occurrences:** `ChatRequest.kt` is the only request type for chat.

**Venice reference:** swagger.yaml:636-1669; `guides/features/reasoning-models.mdx` lines 103-196; `guides/features/structured-responses.mdx` lines 30-136.

**Android/Kotlin reference:** N/A.

**Remediation:** Expand `ChatRequest` to include the missing parameters; use `@EncodeDefault` carefully to avoid sending unsupported defaults.

**Tests required:** Serialization tests for each new field; fixtures matching swagger examples.

**Compatibility impact:** Additive; safe if new fields are nullable with defaults.

---

---

## CHAT-04 | Tool definitions lack `strict`, `id`, and the `web_search`/`x_search` tool types

**Severity:** P1
**Status:** CONFIRMED
**Area:** Tool calling
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 42-52
**Symbol:** ToolSpec`, `ToolFunction

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Tool calling
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 42-52
- **Symbol:** `ToolSpec`, `ToolFunction`

**Evidence:**

```kotlin
@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: ToolFunction,
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null,
)
```

**Spec:** swagger.yaml lines 1622-1669 define tools as an array where each item can be:

- `{ "type": "function", "function": { "description", "name", "parameters", "strict" }, "id": "..." }`
- or `{ "type": "web_search" }` / `{ "type": "x_search" }`

**Expected:** SDK supports `strict`, optional `id`, and the native search tool types.

**Actual:** SDK only supports `function` tools without `strict` or `id`.

**Impact:** Callers cannot request structured tool arguments (`strict=true`) and cannot use Venice native search tools via the `tools` array.

**Root cause:** Tool model only covers the basic OpenAI function shape.

**Related occurrences:** `ChatClient.kt:122-132` also ignores `type` inside tool call deltas.

**Venice reference:** swagger.yaml:1622-1669.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `strict: Boolean?` and `id: String?` to `ToolFunction`/`ToolSpec`; model search tool types as a sealed class or polymorphic JSON element.

**Tests required:** Serialization round-trip for `strict`, `id`, and search tool types.

**Compatibility impact:** Additive.

---

---

## CHAT-05 | `enable_e2ee` is accepted but the SDK does not implement E2EE headers or encryption

**Severity:** P1
**Status:** CONFIRMED
**Area:** Privacy / E2EE
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 84
**Symbol:** VeniceParameters.enableE2ee

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Privacy / E2EE
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 84
- **Symbol:** `VeniceParameters.enableE2ee`

**Evidence:**

```kotlin
@SerialName("enable_e2ee") val enableE2ee: Boolean? = null,
```

`ChatClient.kt:31-36` builds the HTTP request with only `Authorization`, `Accept`, and `User-Agent`; no E2EE headers are added.

**Spec:** `guides/features/tee-e2ee-models.mdx` lines 181-183 state that E2EE requests must include headers `X-Venice-TEE-Client-Pub-Key`, `X-Venice-TEE-Model-Pub-Key`, and `X-Venice-TEE-Signing-Algo`, plus client-side ECDH/HKDF/AES-GCM encryption. swagger.yaml:1484-1491 documents `enable_e2ee` as a boolean defaulting to `true`.

**Expected:** Setting `enable_e2ee=true` triggers the full E2EE handshake and encryption, or the SDK hides the flag until implemented.

**Actual:** The boolean is serialized but no E2EE headers or encryption are performed; the request falls back to TEE-only or fails.

**Impact:** Users believe prompts are end-to-end encrypted when they are not, violating the privacy contract.

**Root cause:** UI-facing parameter without underlying cryptographic implementation.

**Related occurrences:** `ChatClient.kt:31-36`.

**Venice reference:** `guides/features/tee-e2ee-models.mdx`:181-189; swagger.yaml:1484-1491.

**Android/Kotlin reference:** N/A.

**Remediation:** Either remove `enableE2ee` from the chat SDK until E2EE is implemented, or implement attestation, key agreement, and request/response encryption.

**Tests required:** E2EE integration tests against `/tee/attestation`; unit tests for header injection.

**Compatibility impact:** Removing the field is a breaking change; implementing E2EE is additive.

---

---

## CHAT-06 | `safe_mode` under `venice_parameters` is not documented for `/chat/completions`

**Severity:** P1
**Status:** SUSPECTED
**Area:** Request schema / privacy
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 86-88
**Symbol:** VeniceParameters.safeMode

- **Severity:** P1
- **Status:** SUSPECTED
- **Area:** Request schema / privacy
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 86-88
- **Symbol:** `VeniceParameters.safeMode`

**Evidence:**

```kotlin
@SerialName("safe_mode")
@EncodeDefault(EncodeDefault.Mode.NEVER)
val safeMode: Boolean? = null,
```

**Spec:** swagger.yaml lines 1464-1543 list `venice_parameters` fields for `/chat/completions`; `safe_mode` is absent. `safe_mode` appears only in image endpoints (`guides/media/image-generation.mdx`, `guides/media/image-editing.mdx`).

**Expected:** The SDK only serializes fields accepted by the chat endpoint.

**Actual:** `safe_mode` is sent under `venice_parameters` in chat requests when set.

**Impact:** If Venice validates `additionalProperties` on `venice_parameters`, this will produce a 400 `Unrecognized key(s)` error. It also conflates local Family Safe Mode with Venice provider parameters.

**Root cause:** Local safe-mode requirement from `AGENTS.md` was mapped into the Venice request object instead of being kept as an app-level filter.

**Related occurrences:** `VeniceParametersSerializationTest.kt:17-34`; `image/ImageModels.kt` (out of scope) also uses `safe_mode`.

**Venice reference:** swagger.yaml:1464-1543; `guides/media/image-generation.mdx`:227-344.

**Android/Kotlin reference:** N/A.

**Remediation:** Remove `safe_mode` from `VeniceParameters` for chat; keep Family Safe Mode as an app-level UI/policy construct.

**Tests required:** Serialization test asserting `safe_mode` is absent from chat request JSON.

**Compatibility impact:** Removing the field is breaking for any caller already setting it.

---

---

## CHAT-07 | HTTP error response bodies are returned raw instead of being parsed into structured exceptions

**Severity:** P1
**Status:** CONFIRMED
**Area:** Error handling
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 51-56
**Symbol:** streamChat` error path
**Also reported as:** SDK-CORE-08

- **Severity:** P1
- **Status:** CONFIRMED
- **Area:** Error handling
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 51-56
- **Symbol:** `streamChat` error path

**Evidence:**

```kotlin
if (!response.isSuccessful) {
    val msg = response.body.string()
    trySend(ChatStreamChunk.Error(response.code, msg))
    ...
}
```

**Spec:** swagger.yaml lines 6782-6872 define error responses for `/chat/completions` (`400`, `401`, `402`, `415`, `429`, `500`, `503`, `504`) referencing `StandardError`/`DetailedError` schemas (lines 208-232). `VeniceForgeSdk.parseHttpError` already parses these into typed `VeniceSdkException`s.

**Expected:** HTTP errors are surfaced as `VeniceSdkException` subclasses (RateLimit, Authentication, Validation, etc.) with status code, error code, request ID, and retry-after.

**Actual:** The raw body string is wrapped in a `ChatStreamChunk.Error`; rate-limit headers, request ID, and typed error codes are lost.

**Impact:** Callers cannot distinguish rate limits from auth failures or extract retry timing; retry loops and UX are broken.

**Root cause:** ChatClient duplicates error handling instead of reusing `VeniceForgeSdk.parseHttpError`.

**Related occurrences:** `VeniceForgeSdk.kt:140-201`.

**Venice reference:** swagger.yaml:6782-6872, 208-232.

**Android/Kotlin reference:** N/A.

**Remediation:** Refactor `streamChat` to call `sdk.parseHttpError(response)` on non-success status and throw the resulting exception (or emit it through a typed error channel).

**Tests required:** Unit tests for each HTTP error status code asserting the correct `VeniceSdkException` subtype and metadata.

**Compatibility impact:** Changes the type of errors surfaced to consumers; needs migration note.

---

---

## DATA-03 | Area: Atomicity / Profile Management Module: `core:data`

**Severity:** P1
**Status:** CONFIRMED
**Area:** Atomicity / Profile Management
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt
**Lines:** 7–20
**Symbol:** ensureDefault

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt`  
**Lines:** 7–20  
**Symbol:** `ensureDefault`

**Evidence:**
```kotlin
suspend fun ensureDefault(): String {
    dao.findDefault()?.let { return it.id }
    val now = System.currentTimeMillis()
    val entity = ProfileEntity(...)
    dao.insert(entity)
    return DEFAULT_PROFILE_ID
}
```
`findDefault()` and `insert()` are not wrapped in a transaction. `ProfileDao.insert` uses `OnConflictStrategy.ABORT`.

**Expected:** Idempotent default-profile creation even under concurrent callers.

**Actual:** Two concurrent coroutines can both observe `findDefault() == null`, both attempt `insert`, and the second will throw `SQLiteConstraintException`.

**Impact:** First-launch crash or unhandled exception when multiple components trigger profile initialization.

**Root cause:** Read-then-write without transaction/atomic `INSERT OR IGNORE`.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; SQLite `INSERT OR IGNORE` semantics.

**Remediation:** Wrap `findDefault` + `insert` in `withTransaction`, or change DAO to `INSERT OR IGNORE` and retry.

**Tests required:** Concurrent `ensureDefault()` calls must produce exactly one profile without exceptions.

**Compatibility impact:** Behavior change only for the race window; no schema change.

---

---

## DATA-04 | Area: Conversation Consistency Module: `core:data`

**Severity:** P1
**Status:** CONFIRMED
**Area:** Conversation Consistency
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt
**Lines:** 45–54
**Symbol:** appendMessage

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 45–54  
**Symbol:** `appendMessage`

**Evidence:**
```kotlin
suspend fun appendMessage(profileId: String, conversationId: String, message: MessageEntity) {
    ...
    db.withTransaction {
        require(conversationDao.findById(profileId, conversationId) != null) { ... }
        messageDao.upsert(message)
    }
}
```
The transaction updates `messages` only; `conversations.updatedAt` is never touched.

**Expected:** Appending a message should atomically update the parent conversation's `updatedAt` so `observeConversations` ordering reflects recent activity.

**Actual:** `updatedAt` remains at creation time; a conversation with a newer message can appear below a conversation created later.

**Impact:** Broken "recent conversations" ordering — a core history feature.

**Root cause:** Repository omits the conversation update.

**Related occurrences:** `updateAssistantText` (DATA-05).

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; Room `@Update`.

**Remediation:** Inside the transaction, also call `conversationDao.update(copy(updatedAt = now))`.

**Tests required:** ChatRepository test asserting conversation `updatedAt` advances after `appendMessage`.

**Compatibility impact:** No schema change.

---

---

## DATA-05 | Area: Conversation Consistency / Atomicity Module: `core:data`

**Severity:** P1
**Status:** CONFIRMED
**Area:** Conversation Consistency / Atomicity
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt
**Lines:** 56–69
**Symbol:** updateAssistantText

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 56–69  
**Symbol:** `updateAssistantText`

**Evidence:**
```kotlin
suspend fun updateAssistantText(profileId: String, messageId: String, text: String, status: MessageStatus) {
    messageDao.updateTextAndStatus(
        profileId = profileId,
        id = messageId,
        text = text,
        status = status,
        updatedAt = System.currentTimeMillis(),
    )
}
```
There is no `db.withTransaction`, and the parent conversation's `updatedAt` is not updated.

**Expected:** Streaming updates should be atomic with the message row and should refresh the conversation's `updatedAt`.

**Actual:** Each streaming chunk updates the message only; conversation ordering is stale, and there is no transaction boundary if a future change adds side effects.

**Impact:** Stale conversation list during/after streaming; potential partial-write inconsistency.

**Root cause:** Repository updates only the message table and skips the conversation row.

**Related occurrences:** `appendMessage` (DATA-04).

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; generated `@Query` updates.

**Remediation:** Wrap in `db.withTransaction` and update the parent conversation's `updatedAt`.

**Tests required:** Test that `updateAssistantText` advances conversation `updatedAt` and that message + conversation updates are atomic.

**Compatibility impact:** No schema change.

---

---

## DATA-09 | Area: Security / Encryption Module: `core:data`

**Severity:** P1
**Status:** CONFIRMED
**Area:** Security / Encryption
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt
**Lines:** 36–43
**Symbol:** create

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 36–43  
**Symbol:** `create`

**Evidence:**
```kotlin
fun create(context: Context): AppDatabase =
    Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "venice_forge.db",
    )
        .build()
```
The database is created with plain Room/SQLite. No SQLCipher, no `SupportFactory` encryption, no Keystore-backed key.

`docs/SECURITY_AND_STORAGE_CONTRACT.md` point 2 states: "App persistence uses Android Keystore-backed encryption."

**Expected:** Chat history, prompts, and responses are encrypted at rest using a Keystore-backed key.

**Actual:** All `core:data` tables are stored in plaintext SQLite in app-private storage.

**Impact:** Sensitive user prompts/responses are readable via rooted-device access or backup extraction; violates the project's storage contract.

**Root cause:** `AppDatabase.create` does not configure encryption.

**Related occurrences:** `SecureSecretStore` encrypts API keys, but no equivalent exists for the Room database.

**Venice reference:** N/A.

**Android/Kotlin reference:** `androidx.security.crypto.EncryptedFile`/SQLCipher; Android Keystore.

**Remediation:** Adopt SQLCipher with a Keystore-derived key, or store sensitive message content in encrypted blobs outside Room.

**Tests required:** Verify database file is not plaintext; key rotation/recovery tests.

**Compatibility impact:** Major change; existing DB must be migrated to encrypted format.

---

---

## DOC-01 | - `docs/SDK_EXAMPLES.

**Severity:** P1
**Status:** CONFIRMED
**Area:** SDK examples
**Module:** :venice-sdk` public API surface
**File:** docs/SDK_EXAMPLES.md
**Lines:** 71–75
**Symbol:** ChatStreamChunk.Delta.text`, `ChatStreamChunk.ToolCallDelta.arguments

**Area:** SDK examples  
**Module:** `:venice-sdk` public API surface  
**File:** `docs/SDK_EXAMPLES.md`  
**Lines:** 71–75  
**Symbol:** `ChatStreamChunk.Delta.text`, `ChatStreamChunk.ToolCallDelta.arguments`

**Evidence:**
- `docs/SDK_EXAMPLES.md:71` reads `chunk.text?.let { print(it) }`.
- `docs/SDK_EXAMPLES.md:75` reads `println("Tool call #${chunk.index}: ${chunk.name} args: ${chunk.arguments}")`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt:7` declares `data class Delta(val index: Int, val textFragment: String?)`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt:8–13` declares `ToolCallDelta` with `val argumentsFragment: String?`, not `arguments`.

**Expected:** The example should reference `chunk.textFragment` and `chunk.argumentsFragment` to compile against the current SDK.

**Actual:** The example references `chunk.text` and `chunk.arguments`, which do not exist on the current sealed classes.

**Impact:** The primary SDK usage example will not compile and will mislead consumers integrating `:venice-sdk`.

**Root cause:** The `ChatStreamChunk` API was renamed (`text` → `textFragment`, `arguments` → `argumentsFragment`) after `docs/SDK_EXAMPLES.md` was written; the documentation was not updated.

**Related occurrences:** None found in other docs; `ChatScreen.kt` and `ChatViewModel.kt` use the SDK correctly and do not rely on the example.

**Venice reference:** N/A — Kotlin SDK API naming issue.

**Android/Kotlin reference:** Kotlin data class property names are part of the public API; examples must match them.

**Remediation:** Update `docs/SDK_EXAMPLES.md` lines 71 and 75 to use `textFragment` and `argumentsFragment`. Add a CI/static check that compiles snippets in docs if possible.

**Tests required:** None for docs, but verify the corrected snippet compiles against `:venice-sdk`.

**Compatibility impact:** Correcting the docs is safe; no API change.

---

---

## DOC-02 | - `docs/FEATURE_PARITY_MATRIX.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Feature parity status
**Module:** :app` / `:venice-sdk
**File:** docs/FEATURE_PARITY_MATRIX.md`, `CHANGELOG.md`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt
**Lines:** docs/FEATURE_PARITY_MATRIX.md:7,10,15,17`; `CHANGELOG.md:17–20`; `FeatureCatalog.kt:23,26,31,33

**Area:** Feature parity status  
**Module:** `:app` / `:venice-sdk`  
**Files:** `docs/FEATURE_PARITY_MATRIX.md`, `CHANGELOG.md`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt`  
**Lines:** `docs/FEATURE_PARITY_MATRIX.md:7,10,15,17`; `CHANGELOG.md:17–20`; `FeatureCatalog.kt:23,26,31,33`

**Evidence:**
- `docs/FEATURE_PARITY_MATRIX.md:7` lists `chat` → Android now **Foundation**.
- `docs/FEATURE_PARITY_MATRIX.md:10` lists `image` → Android now **Foundation**.
- `docs/FEATURE_PARITY_MATRIX.md:15` lists `audio` → Android now **Foundation (SDK)**.
- `docs/FEATURE_PARITY_MATRIX.md:17` lists `video` → Android now **Foundation (SDK)**.
- `CHANGELOG.md:17` says "Image Studio foundation"; `CHANGELOG.md:19–20` say `:venice-sdk` Audio and Video clients added.
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:23` lists `chat` → `AndroidPortStatus.SCAFFOLDED`.
- `FeatureCatalog.kt:26` lists `image` → `SCAFFOLDED`.
- `FeatureCatalog.kt:31` lists `audio` → `SCAFFOLDED`.
- `FeatureCatalog.kt:33` lists `video` → `SCAFFOLDED`.

**Expected:** A single source of truth for parity status. `FeatureCatalog.kt` is the runtime registry used by the app; the parity matrix and changelog should agree with it.

**Actual:** The matrix/changelog claim Foundation-level implementation for chat/image/audio/video, while the in-app feature registry marks them as `SCAFFOLDED`.

**Impact:** Contributors and users receive contradictory signals about which features are implemented. This undermines the parity matrix's authority and risks scope/expectation mismatches.

**Root cause:** The parity matrix and changelog were updated independently of the `FeatureCatalog` enum after milestone work landed.

**Related occurrences:** `FeatureCatalog.kt` statuses for `privacy`, `settings`, and `status` (`FOUNDATION`) agree with the matrix, so the drift is isolated to the generate/media features.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Reconcile the four features. If the working chat screen, image screen, and SDK audio/video clients are considered Foundation, update `FeatureCatalog.kt`. If they are still scaffolded, update the matrix and changelog. Document the chosen definition-of-done for each status.

**Tests required:** None; this is a documentation/registry consistency fix.

**Compatibility impact:** None.

---

---

## IMG-01 | - `ImageClient.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Response handling / Core feature
**Module:** :venice-sdk
**File:** ImageClient.kt
**Lines:** 27–34, 36–63
**Symbol:** upscale`, `edit`, `multiEdit`, `executeRequest

**Area:** Response handling / Core feature  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 27–34, 36–63  
**Symbol:** `upscale`, `edit`, `multiEdit`, `executeRequest`

**Evidence:**
- `ImageClient.kt:27-34` defines `upscale`, `edit`, and `multiEdit` to call `executeRequest`, which at `ImageClient.kt:59-61` parses the body as `GenerateImageResponse` JSON.
- Swagger `/image/upscale` (lines 7715–7847) 200 response `content` is `image/png` binary only; no JSON schema.
- Swagger `/image/edit` (lines 7848–8087) 200 response `content` is `image/png`, `image/jpeg`, or `image/webp` binary; no JSON schema.
- Swagger `/image/multi-edit` (lines 8088–8330) 200 response `content` is `image/png`, `image/jpeg`, or `image/webp` binary; no JSON schema.

**Expected:** These methods should return raw `ByteArray` binary image data and set `Accept: image/*` (or format-specific MIME type).

**Actual:** SDK attempts to decode binary image bytes as JSON into `GenerateImageResponse`, which will throw `VeniceSdkException.Protocol` on a successful 200 response.

**Impact:** Image editing, upscaling, and multi-edit are completely broken at runtime. The app consumes `edit()` expecting base64 JSON (`ImageViewModel.kt:110-115`), compounding the failure.

**Root cause:** Implementation assumed a uniform JSON response shape for all image endpoints, ignoring per-endpoint `content` types in swagger.

**Related occurrences:** `app/src/main/java/.../image/ImageViewModel.kt:110-115` expects `response.images?.firstOrNull()` from `edit()`.

**Venice reference:** `swagger.yaml:/paths/image/edit/post/responses/200`, `/paths/image/upscale/post/responses/200`, `/paths/image/multi-edit/post/responses/200`.

**Android/Kotlin reference:** OkHttp `ResponseBody.bytes()` is the standard way to consume binary responses; JSON deserialization is inappropriate for `image/*` content.

**Remediation:** Change `upscale`, `edit`, and `multiEdit` to return `ByteArray`. Provide JSON-parsing variants only if a future swagger revision adds JSON responses. Update `ImageViewModel` to consume bytes directly.

**Tests required:** Unit tests for `edit`, `upscale`, `multi-edit` returning `image/png` bytes; verify `Accept` header; verify non-2xx error JSON bodies are still parsed.

**Compatibility impact:** Breaking API change for current consumers; required to match upstream contract.

---

---

## SDK-CORE-01 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Model/Capability Discovery
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt
**Lines:** 94-120
**Symbol:** fetchTraits`, `fetchCompatibility

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
private suspend fun fetchTraits(apiKey: String): Map<String, String> {
    val raw = try {
        sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)
    } catch (_: Exception) {
        return emptyMap()
    }
    ...
}

private suspend fun fetchCompatibility(apiKey: String): Map<String, String> {
    val raw = try {
        sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)
    } catch (_: Exception) {
        return emptyMap()
    }
    ...
}
```

**Venice reference:** `swagger.yaml:8544-8619` (`/models/traits`) and `swagger.yaml:8622-8660` (`/models/compatibility_mapping`) define an optional `type` query parameter with `default: text`.

**Expected:** `CapabilitiesRepository` should pass `type` (or fetch all relevant types) so image/audio/video traits and aliases are discoverable.

**Actual:** No `type` query parameter is sent; the Venice server defaults to `text`, so only text traits/aliases are returned.

**Impact:** Image, audio, video, and embedding default-model traits and compatibility aliases are invisible to the SDK. Apps cannot resolve default models for non-text modalities through `ModelCatalog`.

**Root cause:** Missing query parameter in `getRaw` calls.

**Related occurrences:** `ModelCatalog.defaultTextModelId` (line 20-24) is also text-centric.

**Android/Kotlin reference:** OkHttp `HttpUrl.Builder.addQueryParameter`.

**Remediation:** Add an optional `type: ModelType?` parameter to `fetchLiveCapabilities` and forward it to both `/models/traits` and `/models/compatibility_mapping`; default to `null` to preserve backward behavior, or call per-modality and merge.

**Tests required:** Unit tests verifying that `type=image`/`audio`/`video` query params are appended and that non-text traits/aliases are parsed.

**Compatibility impact:** Source-compatible if new parameter has a default value; binary-compatible if added as an overload.

---

---

## SDK-CORE-02 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Model/Capability Discovery
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt
**Lines:** 20-24
**Symbol:** defaultTextModelId

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt`  
**Lines:** 20-24  
**Symbol:** `defaultTextModelId`

**Evidence:**
```kotlin
val defaultTextModelId: String?
    get() = traits["default"]
        ?: traits["text:default"]
        ?: models.firstOrNull { it.supportsTextChat && !it.offline }?.id
        ?: models.firstOrNull { it.supportsTextChat }?.id
```

**Venice reference:** `swagger.yaml:6120-6127` example traits include `default`, `fastest`; upstream docs mention `text:default`, `text:uncensored`, `image:fast`.

**Expected:** Default model resolution should support modality-specific traits (`image:default`, `audio:default`, `video:default`) and avoid selecting offline/beta models as fallbacks.

**Actual:** Only `"default"` and `"text:default"` are checked. The fallback to the first non-offline text model is reasonable, but the first offline-aware check is skipped if the model is offline (the final fallback ignores `offline`).

**Impact:** Non-text default models cannot be resolved. The fallback may select a model that is offline or in beta.

**Root cause:** Hard-coded trait key list and single-modality focus.

**Related occurrences:** `CapabilitiesRepository.fetchLiveCapabilities` (SDK-CORE-01).

**Android/Kotlin reference:** Kotlin `Map.get`.

**Remediation:** Add modality-aware resolvers (e.g., `defaultModelIdFor(type: ModelType)`), and filter out `offline=true` and optionally `beta=true` from fallbacks.

**Tests required:** Tests for `image:default`, `audio:default`, and offline-model filtering.

**Compatibility impact:** Additive API; no breaking changes if new methods are added.

---

---

## SEC-01 | Redactor is dead production code

**Severity:** P1
**Status:** CONFIRMED
**Area:** Logging / Secret redaction
**Module:** :core:common
**File:** core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt
**Lines:** 8–16
**Symbol:** Redactor.redact
**Also reported as:** ARCH-10

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

---

## SEC-02 | ChatClient embeds raw SSE payloads in error messages

**Severity:** P1
**Status:** CONFIRMED
**Area:** Streaming / Error handling
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 101–102, 112, 115
**Symbol:** parseChunks

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

---

## TEST-COVERAGE-03 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Missing client tests
**Module:** :venice-sdk
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt
**Lines:** 19–45
**Symbol:** AudioClient.speech

**Area:** Missing client tests  
**Module:** `:venice-sdk`  
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 19–45  
**Symbol:** `AudioClient.speech`  
**Evidence:** No test file exists for `AudioClient`. `find venice-sdk/src/test -name '*Audio*'` returns nothing. The production class implements `POST /audio/speech` and returns binary audio bytes.  
**Expected:** Every SDK client surface has unit tests for request serialization, success, and error paths.  
**Actual:** 0 tests for TTS.  
**Impact:** TTS endpoint regressions (wrong URL, missing auth header, wrong Accept header, error mapping) go undetected.  
**Root cause:** Test module never created.  
**Related occurrences:** None.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:10878–11043` (`/audio/speech`).  
**Android/Kotlin reference:** `AudioClient.kt:19–45`.  
**Remediation:** Add `AudioClientTest` covering: request method/URL/auth/Accept header, binary 200 response, 4xx/5xx error mapping, timeout/IO exception mapping.  
**Tests required:** New `AudioClientTest.kt`.  
**Compatibility impact:** Low.

---

---

## TEST-COVERAGE-04 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Missing client tests
**Module:** :venice-sdk
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 19–63
**Symbol:** VideoClient.queue`, `VideoClient.retrieve`, `VideoClient.complete

**Area:** Missing client tests  
**Module:** `:venice-sdk`  
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 19–63  
**Symbol:** `VideoClient.queue`, `VideoClient.retrieve`, `VideoClient.complete`  
**Evidence:** No test file exists for `VideoClient`. `find venice-sdk/src/test -name '*Video*'` returns nothing. The production class implements the async video job state machine (`queue` → `retrieve` → `complete`).  
**Expected:** Video job state machine tested.  
**Actual:** 0 tests for video.  
**Impact:** Bugs in queue/retrieve/complete flow, content-type switching, and status parsing are not caught.  
**Root cause:** Test module never created.  
**Related occurrences:** None.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:11570–11969` (`/video/queue`, `/video/retrieve`).  
**Android/Kotlin reference:** `VideoClient.kt:19–63`.  
**Remediation:** Add `VideoClientTest` covering: queue JSON response parsing, retrieve returning `Processing` vs `Completed` based on `Content-Type`, complete success, 4xx/5xx/404 errors, timeout.  
**Tests required:** New `VideoClientTest.kt`.  
**Compatibility impact:** Low.

---

---

## TEST-FIXTURE-01 | - `model_spec.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Fixture fidelity
**Module:** :venice-sdk
**File:** venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json
**Lines:** 10–42
**Symbol:** model_spec` object, `metadata` object

**Area:** Fixture fidelity  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json`  
**Lines:** 10–42  
**Symbol:** `model_spec` object, `metadata` object  
**Evidence:** The fixture contains fields that are **not defined** in the authoritative `ModelResponse` schema in `swagger.yaml`:
- `model_spec.name` and `model_spec.description` (fixture lines 12–13, 51–52)
- `model_spec.pricing` (fixture lines 28–36, 65–73)
- `model_spec.traits` (fixture lines 17–21, 56)
- `model_spec.uncensored` (fixture lines 16, 55)
- top-level `metadata` (fixture lines 40–42)

`swagger.yaml` `ModelResponse.model_spec` defines only: `availableContextTokens`, `maxCompletionTokens`, `beta`, `betaModel`, `privacy`, `regionRestrictions`, `deprecation`, `capabilities`, `constraints` (`.source/venice-api-docs/swagger.yaml:4696–4919`). Top-level `ModelResponse` properties do not include `metadata` (`.source/venice-api-docs/swagger.yaml:4659–4695`).

**Expected:** Test fixtures model an authoritative current Venice schema and document any extensions.  
**Actual:** The fixture invents convenience fields that the SDK happens to parse, but which are not guaranteed by the spec.  
**Impact:** Tests pass against a fictional schema; if Venice removes or renames these fields, production parsing will break while tests stay green.  
**Root cause:** Fixture hand-authored from SDK implementation rather than from `swagger.yaml` or a recorded `/models` payload.  
**Related occurrences:** `CapabilitiesRepositoryTest.kt:54–100` derives assertions from these invented fields.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:4659–4919` (`ModelResponse`).  
**Android/Kotlin reference:** N/A.  
**Remediation:** Rebuild `models.json` from a recorded `/models` response and trim it to fields present in `swagger.yaml`; add a fixture contract test that fails if a fixture field is not in the swagger schema.  
**Tests required:** New fixture-schema contract test.  
**Compatibility impact:** Medium — may force SDK parsing changes if Venice drops these fields.

---

---

## TEST-HARDCODE-02 | Files / Lines:

**Severity:** P1
**Status:** CONFIRMED
**Area:** Model-ID hardcoding
**Module:** Multiple
**File:** models.json
**Lines:** **
**Symbol:** String literals used as model IDs

**Area:** Model-ID hardcoding  
**Module:** Multiple  
**Files / Lines:**
- `ChatViewModelTest.kt:90` — `"llama-3.3-70b"`
- `ChatRepositoryTest.kt:34` — `"llama-3.3-70b"`
- `CapabilitiesRepositoryTest.kt:60` — `"llama-3.3-70b"`
- `ImageClientTest.kt:57` — `"test-model"`
- `VeniceParametersSerializationTest.kt:24` — `"test-model"`
- `models.json` fixture — `"llama-3.3-70b"`, `"deepseek-r1"`

**Symbol:** String literals used as model IDs  
**Evidence:** Tests encode specific Venice model IDs as string literals. AGENTS.md Model Rule states: "**Never hard-code a current Venice model catalog or permanent default model ID.** Model IDs and capabilities are runtime data." The upstream docs repeat: "Discover, don't hardcode." (`.source/venice-api-docs/AGENTS.md:66`).

**Expected:** Tests use parameterized or trait-resolved model IDs, or synthetic IDs when testing serialization shape.  
**Actual:** Current model IDs are baked into fixtures and assertions.  
**Impact:** Tests become stale when models rotate; `CapabilitiesRepositoryTest` will fail when the default trait mapping changes, creating noisy false positives.  
**Root cause:** Convenience fixtures copied from current catalog.  
**Related occurrences:** 5 test files and 1 fixture file.  
**Venice reference:** AGENTS.md Model Rule; `.source/venice-api-docs/AGENTS.md:66`.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Replace literal model IDs with synthetic IDs in unit tests; in integration tests resolve IDs via `/models/traits`.  
**Tests required:** Refactor affected tests; add CI check that fails on new hardcoded Venice model IDs in test sources.  
**Compatibility impact:** Low — test-only change.

---

---

## TEST-MISSING-18 | - `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.

**Severity:** P1
**Status:** CONFIRMED
**Area:** safe_mode` semantics not tested for media requests
**Module:** :venice-sdk
**File:** *(production)*
**Lines:** 
**Symbol:** safeMode` fields

**Area:** `safe_mode` semantics not tested for media requests  
**Module:** `:venice-sdk`  
**Files:** *(production)*
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt`
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt`
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt`

**Symbol:** `safeMode` fields  
**Evidence:** AGENTS.md states: "**Preserve explicit `safe_mode=false` when selected.**" No test verifies that `safe_mode=false` is serialized in `GenerateImageRequest`, `EditImageRequest`, `QueueVideoRequest`, or `SpeechRequest`.  
**Expected:** Explicit `safe_mode=false` preserved in media request serialization.  
**Actual:** No tests for `safe_mode` in media requests.  
**Impact:** Could regress privacy/product semantics for image/video/audio generation.  
**Root cause:** Missing tests.  
**Related occurrences:** `ImageModels.kt:23–33`, `VideoModels.kt:17`, `AudioModels.kt:13`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:2666–2671` (`/image/generate safe_mode`).  
**Android/Kotlin reference:** Kotlinx Serialization `@SerialName`.  
**Remediation:** Add serialization tests for `safe_mode=false` in image, video, and audio request models.  
**Tests required:** New/expanded model serialization tests.  
**Compatibility impact:** Medium — behavior change if bug exists.

---

---

## TEST-MISSING-19 | - SDK does not write keys to disk/shared prefs - Logs/diagnostics do not contain keys - Key redaction in diagnostics ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Security boundary not tested
**Module:** :venice-sdk
**File:** venice-sdk/src/test` (overall)
**Lines:** 
**Symbol:** API key handling

**Area:** Security boundary not tested  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test` (overall)  
**Symbol:** API key handling  
**Evidence:** AGENTS.md states: "**`:venice-sdk` never persists API keys.**" and "**No raw prompt/response/API-key logging.**" The only security-adjacent test is `VeniceForgeSdkTest.kt:102,131`, which checks that exception messages do not contain the API key. There is no test verifying:
- SDK does not write keys to disk/shared prefs
- Logs/diagnostics do not contain keys
- Key redaction in diagnostics

**Expected:** Security rules backed by tests.  
**Actual:** Minimal security test coverage.  
**Impact:** A regression that logs or persists keys could go undetected.  
**Root cause:** Missing security-focused tests.  
**Related occurrences:** AGENTS.md security boundaries.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Android Keystore / SharedPreferences / Logcat.  
**Remediation:** Add tests verifying no persistence and key redaction in diagnostics.  
**Tests required:** New security tests.  
**Compatibility impact:** Low.

---

---

## VID-01 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Video queued generation
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt
**Lines:** 7–18
**Symbol:** QueueVideoRequest

**Area:** Video queued generation  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt`  
**Lines:** 7–18  
**Symbol:** `QueueVideoRequest`

**Evidence:**
```kotlin
@Serializable
data class QueueVideoRequest(
    val model: String,
    val prompt: String,
    @SerialName("negative_prompt") val negativePrompt: String? = null,
    val seed: Int? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    val duration: String? = null,
    val fps: Int? = null,
    val resolution: String? = null,
    @SerialName("hide_watermark") val hideWatermark: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null,
)
```

**Spec:** `swagger.yaml` `QueueVideoRequest` (lines 3548–3818) requires `model`, `prompt`, and `duration`, and additionally defines `consents`, `negative_prompt`, `duration`, `aspect_ratio`, `resolution`, `upscale_factor`, `audio`, `image_url`, `end_image_url`, `audio_url`, `video_url`, `reference_image_urls`, `reference_video_urls`, `reference_audio_urls`, `elements`, `scene_image_urls`, and `keyframes`.

**Expected:** `QueueVideoRequest` should mirror the swagger schema, including all optional fields, and `model`, `prompt`, and `duration` should be non-nullable (required by spec).

**Actual:** `duration` and `prompt` are nullable (`String?`) and default to `null`. All advanced fields (`consents`, `audio`, `image_url`, reference URLs, keyframes, etc.) are missing. The SDK can serialize a request without `duration`, which the server will reject with HTTP 400.

**Impact:** Broken core feature. Callers cannot use image-to-video, video-to-video, reference media, keyframes, audio toggle, or consent flows. Requests missing required fields fail at the server.

**Root cause:** Model was hand-written from an incomplete subset of the schema.

**Related occurrences:** Same pattern affects `VideoModels.kt` line 7 only.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 3548–3818 (`QueueVideoRequest`).

**Android/Kotlin reference:** Kotlin nullability and `@Serializable` data classes.

**Remediation:** Expand `QueueVideoRequest` to match swagger exactly, or use a code-generated schema binding. Make `model`, `prompt`, `duration` non-nullable.

**Tests required:** Unit tests that serialize representative queue payloads and assert field presence; tests for required-field omission.

**Compatibility impact:** Adding required non-null fields is a source-incompatible change for existing Kotlin callers, but it aligns the SDK with the authoritative API contract.

---

---

## VID-02 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Video quote endpoint
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 15–118 (entire file)
**Symbol:** VideoClient

**Area:** Video quote endpoint  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 15–118 (entire file)  
**Symbol:** `VideoClient`

**Evidence:** `VideoClient` declares only `queue`, `complete`, and `retrieve` (`VideoClient.kt:19`, `22`, `27`). `VeniceEndpoints.VIDEO_QUOTE` exists (`VeniceEndpoints.kt:48`) but is never used by the client.

**Spec:** `swagger.yaml` `/video/quote` (lines 11778–11816) accepts `QuoteVideoRequest` and returns `{ "quote": number }`.

**Expected:** SDK exposes `quote(apiKey, QuoteVideoRequest): VideoQuoteResponse`.

**Actual:** No quote method exists.

**Impact:** Broken core feature / incorrect Venice integration. AGENTS.md rule 2 and upstream `agents.md` both state: "Quote before generating media." Without the quote endpoint, apps cannot show price before charging the user, violating the project's pricing/approval contract for paid/mutating operations.

**Root cause:** Endpoint was not implemented.

**Related occurrences:** `VeniceEndpoints.kt:48` defines the path but it is unused in `VideoClient`.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11778–11816; `.source/venice-api-docs/AGENTS.md` line 67 ("Quote before generating media").

**Android/Kotlin reference:** N/A.

**Remediation:** Add `quote` method and `QuoteVideoRequest`/`VideoQuoteResponse` models matching swagger.

**Tests required:** Mocked quote request/response test; verify required fields.

**Compatibility impact:** New API surface; additive only.

---

---

## VID-03 | > "Video file if completed, or processing status if still in progress" > `application/json` schema contains `status` enum `PROCESSING` / `COMPLETED`, `average_execution_time`, `execution_duration`.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Video retrieve result semantics / VPS-backed models
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 52–62
**Symbol:** retrieve

**Area:** Video retrieve result semantics / VPS-backed models  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 52–62  
**Symbol:** `retrieve`

**Evidence:**
```kotlin
val contentType = res.header("Content-Type", "") ?: ""
if (contentType.contains("application/json")) {
    val bodyStr = res.body?.string().orEmpty()
    val statusRes = runCatching { json.decodeFromString(RetrieveVideoResponseStatus.serializer(), bodyStr) }
        .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /video/retrieve", it) }
    VideoRetrieveResult.Processing(statusRes.status, statusRes.averageExecutionTime, statusRes.executionDuration)
} else {
    val bytes = res.body?.bytes() ?: throw VeniceSdkException.Protocol("Empty binary response", null)
    VideoRetrieveResult.Completed(bytes)
}
```

**Spec:** `swagger.yaml` `/video/retrieve` 200 response (lines 11850–11890) says:
> "Video file if completed, or processing status if still in progress"
> `application/json` schema contains `status` enum `PROCESSING` / `COMPLETED`, `average_execution_time`, `execution_duration`.
> `video/mp4` schema is binary.

The `/video/queue` 200 response (lines 11601–11635) says:
> `download_url`: "Pre-signed URL to download the completed video. Only present for VPS-backed models. When provided, the retrieve endpoint returns JSON status only (no video stream). Fetch this URL after status is COMPLETED to get the video/mp4 file."

**Expected:** When `status == "COMPLETED"` and the body is JSON (VPS-backed case), the SDK should expose the completed state and provide the `download_url` so the caller can fetch the actual media.

**Actual:** Any JSON response is mapped to `VideoRetrieveResult.Processing`, regardless of `status`. The `download_url` field is not part of `RetrieveVideoResponseStatus` or `VideoRetrieveResult`, so completed VPS jobs are reported as still processing with no URL.

**Impact:** Broken core feature. Callers polling VPS-backed models will never observe completion and cannot download the result.

**Root cause:** Retrieve discriminator only looks at `Content-Type`, not the `status` field, and the response model omits `download_url`.

**Related occurrences:** `VideoModels.kt:35–39` (`RetrieveVideoResponseStatus`); `VideoModels.kt:47–53` (`VideoRetrieveResult`).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11625–11631 (`download_url` description); lines 11864–11885 (`/video/retrieve` JSON schema).

**Android/Kotlin reference:** N/A.

**Remediation:** Add `downloadUrl` to `RetrieveVideoResponseStatus`; branch on `status == "COMPLETED"` and return a `Completed` variant that includes the URL or bytes; add a `VideoRetrieveResult.CompletedWithUrl` variant.

**Tests required:** Mock retrieve responses for `PROCESSING` JSON, `COMPLETED` JSON with `download_url`, and binary `video/mp4`.

**Compatibility impact:** Adds new sealed subclass; source-compatible for exhaustive `when` only if callers already handle `else`.

---

---

## VID-04 | ---

**Severity:** P1
**Status:** CONFIRMED
**Area:** Video transcription endpoint
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 15–118
**Symbol:** VideoClient

**Area:** Video transcription endpoint  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 15–118  
**Symbol:** `VideoClient`

**Evidence:** `VideoClient` has no method for `VeniceEndpoints.VIDEO_TRANSCRIPTIONS`.

**Spec:** `swagger.yaml` `/video/transcriptions` (lines 11983–12048) accepts `CreateVideoTranscriptionRequestSchema` (`url`, `response_format`) and returns `{ "transcript": string, "lang": string }` or `text/plain`.

**Expected:** SDK exposes `transcribe(apiKey, CreateVideoTranscriptionRequest): VideoTranscriptionResponse`.

**Actual:** Endpoint is not implemented.

**Impact:** Missing feature. The SDK cannot satisfy the documented video surface.

**Root cause:** Endpoint omitted from client.

**Related occurrences:** `VeniceEndpoints.kt:50` defines the path but it is unused.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11983–12048.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `transcribe` method and request/response models.

**Tests required:** Mock transcription JSON and text/plain responses.

**Compatibility impact:** New API surface; additive.

---

---

## VM-01 | Duplicate chat submissions create multiple message pairs and race on `streamJob`, leaving orphan streams.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Chat / Coroutine lifecycle
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt
**Lines:** 86–187
**Symbol:** submit(String)
**Also reported as:** ARCH-04

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

---

## VM-02 | Stream exceptions are not caught, leaving `isStreaming = true` and the assistant message in `STREAMING`/`PENDING` forever.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Chat / Error handling
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt
**Lines:** 150–185
**Symbol:** inner `launch { ... collect { ... } }

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

---

## VM-03 | `ImageViewModel` catches `CancellationException` in broad `catch (e: Exception)`, reporting cancellation as a user-facing error.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Image / Coroutine lifecycle
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 65–80, 97–120
**Symbol:** generateImage()`, `editImage()

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

---

## VM-04 | Image generation/edit lacks a ViewModel-level guard against duplicate billable submissions.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Image / Coroutine lifecycle
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 52–81, 83–121
**Symbol:** generateImage()`, `editImage()

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

---

## VM-05 | API-key provider and Keystore decryption run on the Main thread inside `submit()` and image operations.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Security / Performance
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt:88`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:56,88`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:91,102,125
**Lines:** 
**Symbol:** apiKeyProvider()` invocations

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

---

## VM-06 | Base64 decoding of edited image response runs on the Main thread.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Image / Performance
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 111–114
**Symbol:** editImage()
**Also reported as:** ARCH-13

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

---

## VM-07 | Conversation identity and streaming state are lost on process death; no SavedState restoration.

**Severity:** P1
**Status:** CONFIRMED
**Area:** Chat / Lifecycle / Process death
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt
**Lines:** 46–74, 54–55
**Symbol:** conversationId`, `streamJob`, `_state

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

---
