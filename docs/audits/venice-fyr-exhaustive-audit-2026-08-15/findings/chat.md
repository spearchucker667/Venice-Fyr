# SDK Chat + Streaming Audit Findings

**Scope:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/` and `venice-sdk/src/test/.../sdk/chat/`

**Upstream baseline:** `.source/venice-api-docs/swagger.yaml` `info.version` `20260814.194349`, upstream HEAD `6e69346b`.

**Implementation baseline:** branch `main` @ `1da3142`, clean tree.

---

## Ledger

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` | 152 | Y | 7 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt` | 89 | Y | 5 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt` | 23 | Y | 2 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt` | 51 | Y | 2 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt` | 14 | Y | 2 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt` | 220 | Y | 3 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt` | 78 | Y | 1 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt` | 32 | Y | 1 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt` | 46 | Y | 0 |
| `venice-sdk/src/test/resources/fixtures/chat-stream/stream-good.sse` | 9 | Y | 0 |

---

## Severity Summary

| Severity | Count |
|----------|-------|
| P0 | 0 |
| P1 | 6 |
| P2 | 7 |
| P3 | 2 |

---

## Findings

### CHAT-01 | Non-streaming `/chat/completions` is unsupported and `stream=false` is broken

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

### CHAT-02 | `ChatMessage.content` is string-only; multimodal/file/prompt-caching content parts are unsupported

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

### CHAT-03 | Large portions of the `/chat/completions` request schema are unimplemented

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

### CHAT-04 | Tool definitions lack `strict`, `id`, and the `web_search`/`x_search` tool types

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

### CHAT-05 | `enable_e2ee` is accepted but the SDK does not implement E2EE headers or encryption

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

### CHAT-06 | `safe_mode` under `venice_parameters` is not documented for `/chat/completions`

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

### CHAT-07 | HTTP error response bodies are returned raw instead of being parsed into structured exceptions

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

### CHAT-08 | Streaming parser ignores most response metadata: `id`, `created`, `model`, `object`, `usage`, `cost`, multiple choices, reasoning, logprobs

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming response parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 100-151
- **Symbol:** `parseChunks`

**Evidence:**

`parseChunks` only reads `choices`, `choices[0].index`, `choices[0].delta.content`, `choices[0].delta.tool_calls`, and `choices[0].finish_reason`. It never inspects `id`, `created`, `model`, `object`, `usage`, `cost`, `logprobs`, `choices[0].delta.role`, `choices[0].delta.reasoning_content`, or additional choices.

**Spec:** swagger.yaml non-streaming response (lines 6256-6780) documents all of these fields. The reasoning-models guide (lines 54-100) documents `reasoning_content` in streaming deltas. The function-calling guide documents `tool_calls`.

**Expected:** The SDK surfaces metadata and optional fields that callers need for logging, billing, token accounting, reasoning display, and multi-choice generation.

**Actual:** These fields are silently dropped.

**Impact:** Incomplete observability; callers cannot show model name, token usage, cost, or reasoning content from streams.

**Root cause:** Minimal chunk model (`ChatStreamChunk`) and parser.

**Related occurrences:** `ChatStreamChunk.kt:5-23` defines `Usage` but it is never populated; `ChatClient.kt:77-78` synthesizes a `Finish` without usage.

**Venice reference:** swagger.yaml:6256-6780; `guides/features/reasoning-models.mdx`:54-100.

**Android/Kotlin reference:** N/A.

**Remediation:** Extend `ChatStreamChunk` with metadata fields; parse them from each SSE payload; emit usage when present.

**Tests required:** Fixtures containing `id`, `created`, `model`, `usage`, `cost`, `reasoning_content`, and multiple choices.

**Compatibility impact:** Additive to `ChatStreamChunk` sealed subtypes.

---

### CHAT-09 | SSE parser does not accumulate multi-line `data:` fields and ignores `event:`/`id:`/`retry:` fields

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** SSE wire parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt`
- **Lines:** 6-13
- **Symbol:** `SseLineParser.nextData`

**Evidence:**

```kotlin
fun nextData(): String? {
    val line = reader.readLine() ?: return null
    if (line.isEmpty()) return nextData()
    if (line.startsWith(":")) return nextData()
    if (line.startsWith("data:")) return line.removePrefix("data:").trim()
    return nextData()
}
```

**Spec:** The HTML Standard for Server-Sent Events requires that consecutive `data:` lines be concatenated with `\n` to form one event's data; `event:`, `id:`, and `retry:` fields are also part of the event semantics.

**Expected:** Multi-line data values are reconstructed; event metadata is available if needed.

**Actual:** Only the first `data:` line is returned; other fields are discarded. If Venice ever emits a multi-line data event, parsing will break.

**Impact:** Fragile SSE handling; potential JSON parse failures on multi-line payloads.

**Root cause:** Line-oriented shortcut instead of full SSE event accumulation.

**Related occurrences:** `SseLineParserTest.kt:11-24` only tests single-line data.

**Venice reference:** N/A (SSE is a web standard; swagger does not define SSE framing).

**Android/Kotlin reference:** HTML Living Standard § Server-Sent Events.

**Remediation:** Implement an event accumulator that buffers lines until a blank line, then returns the concatenated data and any event/id metadata.

**Tests required:** Multi-line data fixture; comment interleaving; event/id fields.

**Compatibility impact:** Internal parser change; external API unchanged.

---

### CHAT-10 | Synthetic `Finish("stop")` is emitted when the server provides no terminal chunk

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming lifecycle
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 76-80
- **Symbol:** `streamChat` terminal enforcement

**Evidence:**

```kotlin
if (!hasEmittedTerminal) {
    trySend(ChatStreamChunk.Finish(reason = "stop"))
    hasEmittedTerminal = true
}
```

**Spec:** `finish_reason` is nullable and can be `stop`, `length`, or `tool_calls`; a missing final chunk indicates a truncated or interrupted stream, not a successful stop.

**Expected:** If the stream ends without a terminal chunk, the SDK should emit an error or leave the stream incomplete, preserving the actual server state.

**Actual:** The SDK fabricates a `stop` finish reason, hiding incomplete responses.

**Impact:** Consumers believe a response completed normally when it may have been cut off by network or server issues.

**Root cause:** Test-driven assumption that every stream must end with exactly one finish event.

**Related occurrences:** `ChatClientTest.kt:68-85` asserts exactly one finish event.

**Venice reference:** swagger.yaml:6261-6268 (finish_reason enum).

**Android/Kotlin reference:** N/A.

**Remediation:** Remove synthetic `stop`; emit an `Error` or incomplete terminal state when no terminal chunk is received.

**Tests required:** Test for truncated stream behavior.

**Compatibility impact:** Changes stream termination contract; consumers may now receive `Error` instead of synthetic `Finish`.

---

### CHAT-11 | `CancellationException` is swallowed without surfacing cancellation to the consumer

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Coroutine lifecycle
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 83-84
- **Symbol:** `streamChat` catch block

**Evidence:**

```kotlin
} catch (e: CancellationException) {
    // Cancellation terminates the flow promptly; invokeOnCompletion cancels the OkHttp Call
}
```

**Expected:** In Kotlin coroutines/Flow, cancellation should propagate via `CancellationException` so downstream collectors can react.

**Actual:** The exception is caught and swallowed; the flow simply completes. Downstream code cannot distinguish cancellation from normal completion.

**Impact:** UI/state machines may incorrectly treat a canceled request as successfully completed.

**Root cause:** Swallowing cancellation to avoid emitting an error chunk.

**Related occurrences:** `ChatClientTest.kt:141-219` tests that the OkHttp call is canceled, but does not assert cancellation signaling.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin coroutines `Flow` cancellation semantics; `callbackFlow` should rethrow cancellation.

**Remediation:** Do not catch `CancellationException`; let `callbackFlow` close it naturally, or rethrow it after cleanup.

**Tests required:** Assert that cancellation propagates to the collector.

**Compatibility impact:** Behavioral change; callers relying on silent completion will see cancellation signals.

---

### CHAT-12 | `ChatStreamAccumulator` does not validate reconstructed tool-call argument JSON

- **Severity:** P2
- **Status:** INFERRED
- **Area:** Tool-call accumulation
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt`
- **Lines:** 17-20
- **Symbol:** `ChatStreamAccumulator.apply` (ToolCallDelta branch)

**Evidence:**

```kotlin
is ChatStreamChunk.ToolCallDelta -> {
    val tc = toolCalls.getOrPut(chunk.index) { MutableToolCall() }
    chunk.callId?.let { tc.id = it }
    chunk.name?.let { tc.name = it }
    if (!chunk.argumentsFragment.isNullOrEmpty()) tc.arguments.append(chunk.argumentsFragment)
}
```

**Spec:** The API streams `function.arguments` as partial JSON strings; the final concatenation should be valid JSON.

**Expected:** The accumulator either validates the final JSON or exposes a parse helper so callers know when the arguments are incomplete/invalid.

**Actual:** Fragments are concatenated blindly; `snapshot()` returns a raw string with no validity check.

**Impact:** Callers may attempt to parse malformed JSON and crash; incomplete streams produce partial JSON without indication.

**Root cause:** No validation step in accumulation.

**Related occurrences:** `ChatStreamAccumulatorTest.kt:18-29` tests concatenation but not JSON validity.

**Venice reference:** swagger.yaml:1622-1669; `guides/features/function-calling.mdx`:124-150.

**Android/Kotlin reference:** N/A.

**Remediation:** Add an optional JSON validation in `snapshot()` and surface an error/flag if the arguments are not valid JSON.

**Tests required:** Test with incomplete and malformed argument fragments.

**Compatibility impact:** Additive if exposed as a new property.

---

### CHAT-13 | `ChatStreamChunk.Finish.usage` is defined but never populated from the stream

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming response parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt`
- **Lines:** 14
- **Symbol:** `ChatStreamChunk.Finish`

**Evidence:**

```kotlin
data class Finish(val reason: String, val usage: Usage? = null) : ChatStreamChunk()
```

No code path in `ChatClient.parseChunks` creates a `Finish` with a non-null `usage`.

**Spec:** When `stream_options.include_usage` is requested, the final SSE chunk contains a top-level `usage` object (OpenAI-compatible streaming behavior).

**Expected:** Usage is parsed from the final chunk and included in the terminal `Finish` event.

**Actual:** `usage` is always `null`.

**Impact:** Token accounting and cost display cannot be derived from the stream.

**Root cause:** Parser ignores the `usage` field.

**Related occurrences:** `ChatClient.kt:77-78`.

**Venice reference:** swagger.yaml:1377-1382 (`stream_options.include_usage`); swagger.yaml:6540-6587 (`usage` schema).

**Android/Kotlin reference:** N/A.

**Remediation:** Parse `usage` from the SSE payload and pass it to `ChatStreamChunk.Finish`.

**Tests required:** Fixture with final `usage` chunk.

**Compatibility impact:** Additive.

---

### CHAT-14 | `developer` role and `reasoning_content`/`reasoning_details`/`thought_signature` are unsupported

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Request/response schema
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 13-26
- **Symbol:** `ChatMessage`

**Evidence:**

`ChatMessage` has no `reasoning_content`, `reasoning_details`, or `thought_signature` fields, and its `role` is an unconstrained `String` with helper constructors only for `user`, `assistant`, `system`, and `tool`.

**Spec:** swagger.yaml lines 1171-1236 define the `developer` message role; lines 1027-1065 and 6375-6414 define reasoning fields for assistant messages.

**Expected:** SDK supports `developer` messages and round-trips reasoning metadata for models that require it (e.g., Gemini thought signatures).

**Actual:** These fields are dropped on serialization/deserialization.

**Impact:** Breaks reasoning-model workflows and multi-turn tool-call conversations that require thought signatures.

**Root cause:** Message model is missing fields added for reasoning models.

**Related occurrences:** `ChatClient.kt:117-139` also ignores `delta.reasoning_content`.

**Venice reference:** swagger.yaml:1027-1065, 1171-1236, 6375-6414; `guides/features/reasoning-models.mdx`:14-100.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `reasoning_content`, `reasoning_details`, `thought_signature`, and a `developer` helper to `ChatMessage`.

**Tests required:** Round-trip serialization for reasoning fields and developer role.

**Compatibility impact:** Additive.

---

### CHAT-15 | `return_search_results_as_documents` is missing from `VeniceParameters`

- **Severity:** P3
- **Status:** CONFIRMED
- **Area:** Venice-specific parameters
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 75-88
- **Symbol:** `VeniceParameters`

**Evidence:**

`VeniceParameters` contains `enableWebSearch`, `enableWebScraping`, `enableWebCitations`, `enableXSearch`, `characterSlug`, `includeVeniceSystemPrompt`, `stripThinkingResponse`, `disableThinking`, `enableE2ee`, `includeSearchResultsInStream`, and `safeMode`, but not `return_search_results_as_documents`.

**Spec:** swagger.yaml lines 1523-1527 document `return_search_results_as_documents` under `venice_parameters`.

**Expected:** SDK exposes the parameter.

**Actual:** Parameter is absent.

**Impact:** Callers cannot request search results as a tool-call documents block.

**Root cause:** Incomplete Venice parameters model.

**Related occurrences:** None.

**Venice reference:** swagger.yaml:1523-1527.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `returnSearchResultsAsDocuments: Boolean?` to `VeniceParameters`.

**Tests required:** Serialization test.

**Compatibility impact:** Additive.

---

### CHAT-16 | Many optional OpenAI-compatible parameters are missing

- **Severity:** P3
- **Status:** CONFIRMED
- **Area:** Request schema completeness
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 58-68
- **Symbol:** `ChatRequest`

**Evidence:**

`ChatRequest` omits `frequency_penalty`, `presence_penalty`, `top_k`, `min_p`, `min_temp`, `max_temp`, `seed`, `repetition_penalty`, `stop`, `stop_token_ids`, `prompt_cache_key`, `prompt_cache_retention`, `fallbacks`, `store`, `verbosity`, `text`, `include`, `metadata`, and `user`.

**Spec:** swagger.yaml lines 636-1669.

**Expected:** SDK exposes commonly used optional parameters.

**Actual:** These parameters cannot be sent.

**Impact:** Reduced OpenAI compatibility; advanced sampling, caching, and metadata features unavailable.

**Root cause:** Minimal initial model.

**Related occurrences:** `ChatRequest.kt`.

**Venice reference:** swagger.yaml:636-1669.

**Android/Kotlin reference:** N/A.

**Remediation:** Add the missing nullable parameters to `ChatRequest`.

**Tests required:** Serialization round-trips.

**Compatibility impact:** Additive.

---

## Test Fixture Notes

- `stream-good.sse` is a valid OpenAI-style SSE stream and matches the fields the SDK parses (`id`, `choices`, `delta.content`, `finish_reason`, `[DONE]`). It does not exercise `usage`, `cost`, `reasoning_content`, multiple choices, or metadata.
- `ChatClientTest` fixtures are inline strings; they correctly model tool-call deltas and stream-side error objects, but they do not assert against the swagger schema for request/response fields.
- `VeniceParametersSerializationTest` verifies `safe_mode: false` preservation, which is a test of an unsupported field (see CHAT-06).
