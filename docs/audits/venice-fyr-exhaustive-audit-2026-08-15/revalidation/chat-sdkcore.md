# Revalidation: CHAT + SDK-CORE P1 Findings

**Scope:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/` and `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/`

**Upstream baseline:** `.source/venice-api-docs/swagger.yaml` `info.version` `20260814.194349`, upstream HEAD `6e69346b`.

**Implementation baseline:** repo `main` @ `ee2cd7a` (production source identical to audited `1da3142`, plus coordinator's `ImageClient` import fix in `VeniceForgeSdk.kt`).

**Method:** Static source inspection against authoritative swagger definitions; no Gradle executed per task rules.

---

## Disposition Summary

| ID | Original Severity/Status | Disposition | Corrected Severity/Status |
|---|---|---|---|
| CHAT-01 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| CHAT-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| CHAT-03 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| CHAT-04 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| CHAT-05 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |
| CHAT-06 | P1 / SUSPECTED | VALID | P1 / CONFIRMED |
| CHAT-07 | P1 / CONFIRMED | VALID (DUPLICATE of SDK-CORE-08) | P1 / CONFIRMED |
| SDK-CORE-01 | P1 / CONFIRMED | VALID (refined) | P1 / CONFIRMED |
| SDK-CORE-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED |

---

## CHAT-01 | Non-streaming `/chat/completions` is unsupported and `stream=false` is broken

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatClient.kt:28` exposes only `streamChat`:
  ```kotlin
  open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = callbackFlow { ... }
  ```
- `ChatRequest.kt:61` defaults `stream` to `true`.
- `ChatClient.kt:34` hard-codes `Accept: text/event-stream` regardless of `request.stream`.

**Spec evidence:**
- `swagger.yaml:1373-1376`: `stream` is an optional boolean that "Defaults to false" and the endpoint supports both streaming and non-streaming.

**Why original is right:** The SDK only implements the streaming path. If a caller sets `stream=false`, the request is still sent with `Accept: text/event-stream` and the JSON body will be parsed as SSE, producing a failure.

**Correct remediation:** Add `chatCompletion(apiKey, request): ChatCompletionResponse` for `stream=false`, or make `streamChat` throw if `request.stream == false`.

**Tests required:** Unit test for non-streaming JSON response parsing; integration test with `stream=false`.

---

## CHAT-02 | `ChatMessage.content` is string-only; multimodal/file/prompt-caching content parts are unsupported

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatRequest.kt:13-19`:
  ```kotlin
  data class ChatMessage(
      val role: String,
      val content: String? = null,
      ...
  )
  ```

**Spec evidence:**
- `swagger.yaml:678-955`: `content` is `anyOf` `string` or an array of content part objects (`text`, `image_url`, `input_audio`, `video_url`, `file`), each optionally carrying `cache_control`.

**Why original is right:** The SDK serializes `content` as a JSON string only, so vision, audio, video, file inputs, and prompt-caching markers cannot be sent.

**Correct remediation:** Model `content` as `JsonElement` or a sealed class of content parts; keep string helper constructors.

**Tests required:** Serialization round-trips for each content part type; fixture tests with `cache_control`.

---

## CHAT-03 | Large portions of the `/chat/completions` request schema are unimplemented

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatRequest.kt:58-68` exposes only: `model`, `messages`, `stream`, `temperature`, `top_p`, `max_tokens`, `max_completion_tokens`, `tools`, `venice_parameters`.

**Spec evidence:**
- `swagger.yaml:633-1673` defines many additional fields: `frequency_penalty`, `presence_penalty`, `logprobs`, `top_logprobs`, `min_p`, `min_temp`, `max_temp`, `n`, `stop`, `stop_token_ids`, `seed`, `repetition_penalty`, `prompt_cache_key`, `prompt_cache_retention`, `reasoning`, `reasoning_effort`, `response_format`, `tool_choice`, `parallel_tool_calls`, `fallbacks`, `store`, `verbosity`, `text`, `include`, `metadata`, `user`, `stream_options`.

**Why original is right:** The request model is minimal and omits commonly used parameters such as `response_format`, `reasoning`/`reasoning_effort`, `stop`, `tool_choice`, `parallel_tool_calls`, `stream_options`, and `prompt_cache_key`.

**Correct remediation:** Expand `ChatRequest` with nullable defaults for missing parameters; use `@EncodeDefault` carefully.

**Tests required:** Serialization tests for each new field; fixtures matching swagger examples.

---

## CHAT-04 | Tool definitions lack `strict`, `id`, and the `web_search`/`x_search` tool types

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatRequest.kt:42-52`:
  ```kotlin
  data class ToolSpec(val type: String = "function", val function: ToolFunction)
  data class ToolFunction(val name: String, val description: String? = null, val parameters: JsonElement? = null)
  ```
- `ChatClient.kt:122-132` parses tool-call deltas but never inspects `type` inside the tool array.

**Spec evidence:**
- `swagger.yaml:1622-1669`: tools array items can be:
  - `{ "type": "function", "function": { "description", "name", "parameters", "strict" }, "id": "..." }`
  - `{ "type": "web_search" }` / `{ "type": "x_search" }`

**Why original is right:** The SDK only supports the basic OpenAI function shape; `strict`, `id`, and native search tools are absent.

**Correct remediation:** Add `strict: Boolean?` and `id: String?`; model search tool types as a sealed class or polymorphic JSON element.

**Tests required:** Serialization round-trip for `strict`, `id`, and search tool types.

---

## CHAT-05 | `enable_e2ee` is accepted but the SDK does not implement E2EE headers or encryption

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatRequest.kt:84`: `@SerialName("enable_e2ee") val enableE2ee: Boolean? = null`
- `ChatClient.kt:31-36` builds headers `Authorization`, `Accept`, `User-Agent` only; no E2EE headers are added.

**Spec evidence:**
- `swagger.yaml:1484-1491`: `enable_e2ee` defaults to `true` and "E2EE is used if E2EE headers are present".
- `guides/features/tee-e2ee-models.mdx:181-189`: E2EE requests require headers `X-Venice-TEE-Client-Pub-Key`, `X-Venice-TEE-Model-Pub-Key`, `X-Venice-TEE-Signing-Algo` and client-side encryption.

**Why original is right:** The SDK serializes the flag but performs no key agreement, attestation, or encryption, so `enable_e2ee=true` does not provide E2EE.

**Correct remediation:** Either remove `enableE2ee` from the chat SDK until E2EE is implemented, or implement the full handshake (`/tee/attestation`, ECDH, HKDF, AES-GCM) and header injection.

**Tests required:** E2EE integration tests against `/tee/attestation`; unit tests for header injection.

---

## CHAT-06 | `safe_mode` under `venice_parameters` is not documented for `/chat/completions`

**Original:** P1 / SUSPECTED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatRequest.kt:86-88`:
  ```kotlin
  @SerialName("safe_mode")
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val safeMode: Boolean? = null,
  ```

**Spec evidence:**
- `swagger.yaml:1464-1543`: `venice_parameters` for `/chat/completions` lists `character_slug`, `strip_thinking_response`, `disable_thinking`, `enable_e2ee`, `enable_web_search`, `enable_web_scraping`, `enable_web_citations`, `include_search_results_in_stream`, `return_search_results_as_documents`, `include_venice_system_prompt`, `enable_x_search`; `safe_mode` is absent.
- `swagger.yaml:2666-2669`, `3017-3020`, `3108-3112`, `3206-3210`: `safe_mode` appears only in image generation/editing/upscale/multi-edit request schemas.

**Why original is right:** Sending `safe_mode` under `venice_parameters` in a chat request sends a field the chat endpoint does not document. If the server validates `additionalProperties`, this will produce a 400.

**Correct remediation:** Remove `safe_mode` from `VeniceParameters` for chat; keep Family Safe Mode as an app-level UI/policy construct.

**Tests required:** Serialization test asserting `safe_mode` is absent from chat request JSON.

---

## CHAT-07 | HTTP error response bodies are returned raw instead of being parsed into structured exceptions

**Original:** P1 / CONFIRMED
**Disposition:** VALID (DUPLICATE of SDK-CORE-08)
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ChatClient.kt:51-56`:
  ```kotlin
  if (!response.isSuccessful) {
      val msg = response.body.string()
      trySend(ChatStreamChunk.Error(response.code, msg))
      ...
  }
  ```
- `VeniceForgeSdk.kt:141-201` already implements `parseHttpError` with typed `VeniceSdkException` subclasses.

**Spec evidence:**
- `swagger.yaml:6782-6872`: `/chat/completions` error responses map to `StandardError`/`DetailedError` (`swagger.yaml:208-232`).

**Why original is right:** `ChatClient` duplicates error handling instead of reusing `VeniceForgeSdk.parseHttpError`, losing rate-limit headers, request IDs, and error codes.

**Correct remediation:** In `streamChat`, call `sdk.parseHttpError(response)` on non-success status and surface the resulting exception.

**Tests required:** Unit tests for 429/401/400/5xx streaming responses asserting correct `VeniceSdkException` subtype and metadata.

---

## SDK-CORE-01 | CapabilitiesRepository does not propagate model type to traits/compatibility endpoints

**Original:** P1 / CONFIRMED
**Disposition:** VALID (refined)
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `CapabilitiesRepository.kt:25`: `val models = sdk.listModels(apiKey, null)` — this call is correct: omitting `type` returns all models.
- `CapabilitiesRepository.kt:94-120`: `fetchTraits` and `fetchCompatibility` call `sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)` and `sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)` without appending a `type` query parameter.

**Spec evidence:**
- `swagger.yaml:8544-8572`: `GET /models/traits` has an optional `type` query parameter with enum `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video` and `default: text`.
- `swagger.yaml:8622-8650`: `GET /models/compatibility_mapping` has the same optional `type` query parameter with `default: text`.

**Why original is right/wrong:** The original finding is correct that non-text traits/compatibility aliases are missed, but it misidentifies `sdk.listModels(apiKey, null)` as the locus. The precise defect is in `fetchTraits`/`fetchCompatibility`: because no `type` is sent, the server defaults to `text`, so only `text:default`/`text:fastest`-style traits and text-model compatibility aliases are discovered. Image, audio, video, and embedding traits/aliases are invisible.

**Correct remediation:** Add an optional `type: ModelType?` parameter to `fetchLiveCapabilities` and forward it as a query parameter to both `/models/traits` and `/models/compatibility_mapping`. Default to `null` to preserve backward-compatible text-default behavior, or iterate over all relevant modalities and merge.

**Tests required:** Unit tests verifying that `type=image`/`audio`/`video` query params are appended and that non-text traits/aliases are parsed.

---

## SDK-CORE-02 | `ModelCatalog.defaultTextModelId` is text-centric and ignores modality-specific traits

**Original:** P1 / CONFIRMED
**Disposition:** VALID
**Corrected:** P1 / CONFIRMED

**Source evidence:**
- `ModelCatalog.kt:20-24`:
  ```kotlin
  val defaultTextModelId: String?
      get() = traits["default"]
          ?: traits["text:default"]
          ?: models.firstOrNull { it.supportsTextChat && !it.offline }?.id
          ?: models.firstOrNull { it.supportsTextChat }?.id
  ```
- `ModelCapabilities.kt:43-44`: `supportsTextChat` treats `"code"` as a valid type, which never appears in `/models` responses (see SDK-CORE-04/12).

**Spec evidence:**
- `swagger.yaml:6055-6067`: `ModelResponse.type` enum is `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video`.
- `swagger.yaml:8555-8572`: `/models/traits` `type` parameter controls which modality's traits are returned.

**Why original is right:** The resolver only checks `"default"` and `"text:default"`, so non-text default models (`image:default`, `audio:default`, etc.) cannot be resolved. The fallback also ignores `beta` and may select an offline model in the final branch.

**Correct remediation:** Add modality-aware resolvers (e.g., `defaultModelIdFor(type: ModelType)`), and filter out `offline=true` and optionally `beta=true` from fallbacks.

**Tests required:** Tests for `image:default`, `audio:default`, and offline-model filtering.

---

## Key Evidence Quotes (max 12 lines)

1. `ChatClient.kt:28` — `open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk>`
2. `ChatClient.kt:34` — `.header("Accept", "text/event-stream")`
3. `ChatRequest.kt:15` — `val content: String? = null`
4. `ChatRequest.kt:61` — `val stream: Boolean = true`
5. `ChatRequest.kt:86-88` — `safeMode: Boolean?` under `venice_parameters`
6. `swagger.yaml:1373-1376` — `stream` "Defaults to false"
7. `swagger.yaml:1484-1491` — `enable_e2ee` "E2EE is used if E2EE headers are present"
8. `swagger.yaml:8544-8572` — `/models/traits?type=` with `default: text`
9. `swagger.yaml:8622-8650` — `/models/compatibility_mapping?type=` with `default: text`
10. `swagger.yaml:6055-6067` — `ModelResponse.type` enum does not contain `code`
11. `CapabilitiesRepository.kt:25` — `sdk.listModels(apiKey, null)` (returns all models; not the defect)
12. `CapabilitiesRepository.kt:94-120` — `fetchTraits`/`fetchCompatibility` call `getRaw` without `type` query parameter
