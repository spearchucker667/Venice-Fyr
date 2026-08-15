# Venice API Contract Matrix — Consolidated

**Scope:** `:venice-sdk` public API surface vs. authoritative `.source/venice-api-docs/swagger.yaml` (`info.version 20260814.194349`).

## Summary — Operations Implemented vs. Missing by Endpoint Group

| Endpoint group | Implemented | Partial / Broken | Missing | Notes |
|----------------|-------------|------------------|---------|-------|
| Chat + Streaming | 1 (`POST /chat/completions` streaming) | 2 (streaming metadata/usage parsing, SSE wire handling) | 1 (non-streaming `/chat/completions` response) | `stream=false` is unsupported. |
| Image | 2 (`/image/generate` JSON & binary) | 3 (`/image/upscale`, `/image/edit`, `/image/multi-edit` parse binary as JSON) | 3 (`/images/generations`, `GET /image/styles`, `/image/background-remove`) | Multipart upload is also missing. |
| Video + Audio | 0 | 4 (`/video/queue`, `/video/retrieve`, `/video/complete`, `/audio/speech`) | 7 (`/video/quote`, `/video/transcriptions`, `/audio/queue`, `/audio/retrieve`, `/audio/quote`, `/audio/complete`, `/audio/transcriptions`) | Queued-job lifecycle and quote-before-generate are incomplete. |
| Models / Capabilities | 3 (`GET /models`, `/models/traits`, `/models/compatibility_mapping`) | 0 (shape parsed; field coverage and `type` query are partial) | 0 | ~35% of `model_spec` fields exposed; `type` query omitted. |

## Chat + Streaming


**Scope:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/` and `venice-sdk/src/test/.../sdk/chat/`

**Upstream baseline:** `.source/venice-api-docs/swagger.yaml` `info.version` `20260814.194349`, upstream HEAD `6e69346b`.

**Implementation baseline:** branch `main` @ `1da3142`.

---

## 1. Request: `POST /chat/completions` (`ChatCompletionRequest`)

| Field | Required | Spec type | Spec evidence (swagger line) | SDK status | SDK location / note |
|-------|----------|-----------|------------------------------|------------|---------------------|
| `model` | yes | string | 1258-1265 | Implemented | `ChatRequest.model` |
| `messages` | yes | array of message objects | 672-1244 | Partial | `ChatRequest.messages`; content is string-only, content parts and `developer` role missing |
| `stream` | no | boolean (default `false`) | 1373-1376 | Implemented (default `true`) | `ChatRequest.stream` default `true`; SDK only supports streaming |
| `stream_options` | no | object (`include_usage`) | 1377-1382 | Missing | — |
| `temperature` | no | number [0,2] | 1383-1391 | Implemented | `ChatRequest.temperature` |
| `top_p` | no | number [0,1] | 1398-1406 | Implemented | `ChatRequest.topP` |
| `top_k` | no | integer ≥0 | 1392-1397 | Missing | — |
| `max_tokens` | no | integer | 665-671 | Implemented | `ChatRequest.maxTokens` |
| `max_completion_tokens` | no | integer | 655-659 | Implemented | `ChatRequest.maxCompletionTokens` |
| `frequency_penalty` | no | number [-2,2] default 0 | 636-643 | Missing | — |
| `presence_penalty` | no | number [-2,2] default 0 | 1273-1280 | Missing | — |
| `logprobs` | no | boolean | 644-648 | Missing | — |
| `top_logprobs` | no | integer ≥0 | 649-654 | Missing | — |
| `min_p` | no | number [0,1] | 1246-1251 | Missing | — |
| `min_temp` | no | number [0,2] | 1252-1257 | Missing | — |
| `max_temp` | no | number [0,2] | 659-664 | Missing | — |
| `n` | no | integer default 1 | 1266-1272 | Missing | SDK only parses the first choice |
| `stop` | no | string / array(max 4) / null | 1351-1364 | Missing | — |
| `stop_token_ids` | no | array of numbers | 1365-1372 | Missing | — |
| `seed` | no | integer >0 | 1344-1350 | Missing | — |
| `repetition_penalty` | no | number ≥0 | 1296-1301 | Missing | — |
| `prompt_cache_key` | no | string | 1281-1285 | Missing | — |
| `prompt_cache_retention` | no | enum `default`/`extended`/`24h` | 1286-1295 | Missing | — |
| `reasoning` | no | object `{effort,summary}` | 1302-1329 | Missing | — |
| `reasoning_effort` | no | string enum | 1330-1343 | Missing | — |
| `response_format` | no | oneOf `json_schema`/`json_object`/`text` | 1549-1604 | Missing | Structured outputs unsupported |
| `tool_choice` | no | anyOf object/string | 1605-1621 | Missing | — |
| `tools` | no | nullable array of tool specs | 1622-1669 | Partial | `ChatRequest.tools`; only `function` type, no `strict`, no `id`, no `web_search`/`x_search` |
| `parallel_tool_calls` | no | boolean default `true` | 1644-1647 | Missing | — |
| `fallbacks` | no | array max 10 | 1411-1425 | Missing | — |
| `store` | no | boolean | 1426-1429 | Missing | — |
| `verbosity` | no | string enum | 1430-1439 | Missing | — |
| `text` | no | object (`verbosity`) | 1440-1452 | Missing | — |
| `include` | no | array of strings | 1453-1457 | Missing | — |
| `metadata` | no | object (string values) | 1459-1463 | Missing | — |
| `user` | no | string (discarded) | 1407-1410 | Missing | — |
| `venice_parameters` | no | object | 1464-1543 | Partial | `ChatRequest.veniceParameters` |

### 1.1 `venice_parameters` fields

| Field | Required | Spec type | Spec evidence | SDK status | Note |
|-------|----------|-----------|---------------|------------|------|
| `character_slug` | no | string | 1467-1470 | Implemented | `VeniceParameters.characterSlug` |
| `strip_thinking_response` | no | boolean default `false` | 1472-1476 | Implemented | `VeniceParameters.stripThinkingResponse` |
| `disable_thinking` | no | boolean default `false` | 1479-1483 | Implemented | `VeniceParameters.disableThinking` |
| `enable_e2ee` | no | boolean default `true` | 1484-1491 | Implemented as flag only | `VeniceParameters.enableE2ee`; no E2EE headers/encryption implemented |
| `enable_web_search` | no | string enum `auto`/`off`/`on` default `off` | 1492-1504 | Implemented (unvalidated string) | `VeniceParameters.enableWebSearch` |
| `enable_web_scraping` | no | boolean default `false` | 1505-1510 | Implemented | `VeniceParameters.enableWebScraping` |
| `enable_web_citations` | no | boolean default `false` | 1511-1516 | Implemented | `VeniceParameters.enableWebCitations` |
| `include_search_results_in_stream` | no | boolean default `false` | 1517-1522 | Implemented | `VeniceParameters.includeSearchResultsInStream` |
| `return_search_results_as_documents` | no | boolean | 1523-1527 | Missing | — |
| `include_venice_system_prompt` | no | boolean default `true` | 1528-1532 | Implemented | `VeniceParameters.includeVeniceSystemPrompt` |
| `enable_x_search` | no | boolean default `false` | 1533-1541 | Implemented | `VeniceParameters.enableXSearch` |
| `safe_mode` | no | **not present** in `/chat/completions` `venice_parameters` | 1464-1543 | Present but unsupported | `VeniceParameters.safeMode`; only documented for image endpoints |

### 1.2 `messages` object fields

| Field | Required | Spec type | Spec evidence | SDK status | Note |
|-------|----------|-----------|---------------|------------|------|
| `role` | yes | string enum `user`/`assistant`/`system`/`tool`/`developer` | 960-1230 | Partial | `ChatMessage.role`; `developer` role missing |
| `content` | yes (user/system/developer), optional (assistant/tool) | string **or** array of content parts | 678-955, 972-1222 | Partial | `ChatMessage.content` is `String?` only; multimodal parts (`text`, `image_url`, `input_audio`, `video_url`, `file`) unsupported |
| `name` | no | string nullable | 957-959, 1024-1026 | Implemented | `ChatMessage.name` |
| `reasoning_content` | no | string nullable (assistant) | 1027-1028, 1083-1084 | Missing | — |
| `reasoning_details` | no | array (assistant) | 1031-1054, 6375-6403 | Missing | — |
| `thought_signature` | no | string nullable (assistant) | 1059-1065, 6408-6414 | Missing | — |
| `tool_calls` | no | array nullable (assistant) | 1066-1069 | Implemented | `ChatMessage.toolCalls` |
| `tool_call_id` | yes (tool) | string | 1090-1091 | Implemented | `ChatMessage.toolCallId` |

### 1.3 Content part types (all unsupported)

| Part type | Spec evidence | SDK status |
|-----------|---------------|------------|
| `text` (with optional `cache_control`) | 680-727, 974-1020 | Missing |
| `image_url` | 728-773 | Missing |
| `input_audio` | 774-837, 1675-1737 | Missing |
| `video_url` | 838-955, 1739-1791 | Missing |
| `file` | 894-955 | Missing |

---

## 2. Non-streaming response `200 OK`

The SDK has **no non-streaming response type**. The fields below are documented in swagger lines 6234-6780 and are all unimplemented for callers that set `stream: false`.

| Field | Required | Spec type | Spec evidence | SDK status |
|-------|----------|-----------|---------------|------------|
| `choices` | yes | array | 6256-6469 | Not implemented |
| `choices[].finish_reason` | yes | string enum | 6261-6268 | Not implemented |
| `choices[].index` | yes | integer | 6269-6272 | Not implemented |
| `choices[].logprobs` | yes | object/null | 6273-6316 | Not implemented |
| `choices[].message` | yes | object | 6317-6453 | Not implemented |
| `choices[].stop_reason` | no | string enum | 6454-6461 | Not implemented |
| `created` | yes | integer | 6494-6497 | Not implemented |
| `cost` | no | object `{diem,usd}` | 6498-6515 | Not implemented |
| `id` | yes | string | 6516-6519 | Not implemented |
| `model` | yes | string | 6520-6523 | Not implemented |
| `object` | yes | string enum `chat.completion` | 6524-6529 | Not implemented |
| `prompt_logprobs` | no | object/null | 6530-6539 | Not implemented |
| `usage` | yes | object | 6540-6587 | Not implemented |
| `venice_parameters` | yes | object | 6588-6725 | Not implemented |

---

## 3. Streaming response chunks

The swagger does **not** define a streaming response schema; the fields below are the ones the SDK parses or ignores, inferred from OpenAI-compatible SSE behavior and the Venice reasoning-models/function-calling guides.

| Field | Inferred required | Type | SDK status | Note |
|-------|-------------------|------|------------|------|
| `id` | no | string | Ignored | `ChatClient` does not expose it |
| `created` | no | integer | Ignored | — |
| `model` | no | string | Ignored | — |
| `object` | no | string | Ignored | — |
| `choices` | yes | array | Partial | Only first element inspected (`ChatClient.kt:115`) |
| `choices[].index` | yes | integer | Implemented | Mapped to chunk index |
| `choices[].finish_reason` | no | string enum `stop`/`length`/`tool_calls` | Implemented | Emits `ChatStreamChunk.Finish` |
| `choices[].delta.role` | no | string | Ignored | — |
| `choices[].delta.content` | no | string | Implemented | Emits `ChatStreamChunk.Delta` |
| `choices[].delta.reasoning_content` | no | string | Ignored | Reasoning-models guide lines 54-100 |
| `choices[].delta.tool_calls` | no | array | Partial | Only `function` fields parsed; `type` ignored |
| `choices[].delta.tool_calls[].index` | no | integer | Implemented | — |
| `choices[].delta.tool_calls[].id` | no | string | Implemented | — |
| `choices[].delta.tool_calls[].function.name` | no | string | Implemented | — |
| `choices[].delta.tool_calls[].function.arguments` | no | string | Implemented | — |
| `choices[].logprobs` | no | object | Ignored | — |
| `usage` | no | object | Ignored | `ChatStreamChunk.Finish.usage` exists but is never populated |
| `cost` | no | object | Ignored | — |
| `[DONE]` sentinel | — | — | Implemented | `ChatClient.kt:64` |

---

## 4. SSE wire-format handling

| Requirement | SDK behavior | Evidence |
|-------------|--------------|----------|
| Line splitting on CRLF / LF | `BufferedReader.readLine()` handles both | `SseLineParser.kt:7` |
| Blank line as event boundary | Blank lines are skipped; each `data:` line is returned immediately | `SseLineParser.kt:8` |
| Comment lines (`:`) | Skipped | `SseLineParser.kt:9` |
| Multi-line `data:` fields | **Not accumulated**; only first line returned | `SseLineParser.kt:10` |
| `event:`, `id:`, `retry:` fields | Ignored | `SseLineParser.kt:11-12` |
| Partial data before newline | `readLine()` blocks; no explicit read timeout | `ChatClient.kt:59` |
| UTF-8 multi-byte boundaries | Handled by `BufferedReader` character decoding | `ChatClient.kt:59` |
| Malformed JSON payload | Emits `ChatStreamChunk.Error` | `ChatClient.kt:101-102` |
| Stream-side `{"error":...}` payload | Parsed into `ChatStreamChunk.Error` | `ChatClient.kt:106-111` |
| HTTP error status body | Returned raw in `Error` chunk; not parsed into `VeniceSdkException` | `ChatClient.kt:51-56` |

## Image


**Scope:** `:venice-sdk` image endpoints and models vs. upstream `swagger.yaml` (`info.version 20260814.194349`).

## Endpoint implementation matrix

| Endpoint | SDK method | Implemented | Content-Type sent | Response handling | Status |
|----------|------------|-------------|-------------------|-------------------|--------|
| `POST /image/generate` | `generate()` | Yes | `application/json` | JSON → `GenerateImageResponse` | OK for `return_binary=false` |
| `POST /image/generate` (binary) | `generateBinary()` | Yes | `application/json` | Binary `ByteArray` | OK |
| `POST /images/generations` | — | **No** | — | — | Missing (IMG-04) |
| `GET /image/styles` | — | **No** | — | — | Missing (IMG-03) |
| `POST /image/upscale` | `upscale()` | Yes, but wrong | `application/json` | Parses JSON | **Broken** (IMG-01) |
| `POST /image/edit` | `edit()` | Yes, but wrong | `application/json` | Parses JSON | **Broken** (IMG-01) |
| `POST /image/multi-edit` | `multiEdit()` | Yes, but wrong | `application/json` | Parses JSON | **Broken** (IMG-01) |
| `POST /image/background-remove` | — | **No** | — | — | Missing (IMG-02) |

## Request/response model matrix

| Model / field | SDK presence | SDK type | Swagger type | Notes |
|---------------|--------------|----------|--------------|-------|
| `GenerateImageRequest.model` | Yes | `String` | `string` | OK |
| `GenerateImageRequest.prompt` | Yes | `String` | `string` | OK |
| `GenerateImageRequest.negative_prompt` | Yes | `String?` | `string` | OK |
| `GenerateImageRequest.style_preset` | Yes | `String?` | `string` | OK |
| `GenerateImageRequest.height` | Yes | `Int?` | `integer` | OK |
| `GenerateImageRequest.width` | Yes | `Int?` | `integer` | OK |
| `GenerateImageRequest.steps` | Yes | `Int?` | `integer` | OK |
| `GenerateImageRequest.cfg_scale` | Yes | `Float?` | `number` | OK |
| `GenerateImageRequest.seed` | Yes | `Int?` | `integer` | OK |
| `GenerateImageRequest.safe_mode` | Yes | `Boolean?` | `boolean` | OK; explicit `false` preserved |
| `GenerateImageRequest.return_binary` | Yes | `Boolean?` | `boolean` | OK |
| `GenerateImageRequest.format` | Yes | `String?` | `string` enum | OK |
| `GenerateImageRequest.variants` | Yes | `Int?` | `integer` | OK |
| `GenerateImageRequest.aspect_ratio` | Yes | `String?` | `string` | OK |
| `GenerateImageRequest.resolution` | Yes | `String?` | `string` | OK |
| `GenerateImageRequest.quality` | Yes | `String?` | `string` enum | OK |
| `GenerateImageRequest.enable_web_search` | Yes | `Boolean?` | `boolean` | OK |
| `GenerateImageRequest.disable_prompt_optimization_thinking` | Yes | `Boolean?` | `boolean` | OK |
| `GenerateImageRequest.enhance_prompt` | Yes | `Boolean?` | `boolean` | OK |
| `GenerateImageRequest.style_references` | Yes | `List<StyleReference>?` | `array` | OK |
| `GenerateImageRequest.hide_watermark` | Yes | `Boolean?` | `boolean` | OK |
| `GenerateImageRequest.embed_exif_metadata` | **No** | — | `boolean` | Missing (IMG-07) |
| `GenerateImageRequest.lora_strength` | **No** | — | `integer` | Missing (IMG-07) |
| `GenerateImageRequest.inpaint` | **No** | — | `nullable` deprecated | Missing (IMG-07) |
| `SimpleGenerateImageRequest` | **No** | — | object | Missing endpoint/model (IMG-04) |
| `UpscaleImageRequest.image` | Yes | `String` | `anyOf` (file / string) | JSON base64 OK; multipart missing (IMG-05) |
| `UpscaleImageRequest.scale` | Yes | `Int?` | `number` | Functionally OK (values 2 or 4) |
| `UpscaleImageRequest.creativity` | Yes | `Float?` | `number` | OK |
| `EditImageRequest.image` | Yes | `String` | `anyOf` (file / string / uri) | JSON/URL OK; multipart missing (IMG-05) |
| `EditImageRequest.prompt` | Yes | `String` | `string` | OK |
| `EditImageRequest.model` | Yes | `String?` | `string` | OK |
| `EditImageRequest.modelId` | **No** | — | `string` deprecated | Missing (IMG-08) |
| `EditImageRequest.aspect_ratio` | Yes | `String?` | `string` enum | OK |
| `EditImageRequest.resolution` | Yes | `String?` | `string` | OK |
| `EditImageRequest.output_format` | Yes | `String?` | `string` enum | OK |
| `EditImageRequest.disable_prompt_optimization_thinking` | Yes | `Boolean?` | `boolean` | OK |
| `EditImageRequest.enhance_prompt` | Yes | `Boolean?` | `boolean` | OK |
| `EditImageRequest.safe_mode` | Yes | `Boolean?` | `boolean` | OK |
| `MultiEditImageRequest.images` | Yes | `List<String>` | `array` of string/uri | JSON/URL OK; multipart missing (IMG-05) |
| `MultiEditImageRequest.prompt` | Yes | `String` | `string` | OK |
| `MultiEditImageRequest.modelId` | Yes | `String?` | `string` | OK |
| `MultiEditImageRequest.aspect_ratio` | Yes | `String?` | `string` enum | OK |
| `MultiEditImageRequest.resolution` | Yes | `String?` | `string` | OK |
| `MultiEditImageRequest.quality` | Yes | `String?` | `string` enum | OK |
| `MultiEditImageRequest.output_format` | Yes | `String?` | `string` enum | OK |
| `MultiEditImageRequest.disable_prompt_optimization_thinking` | Yes | `Boolean?` | `boolean` | OK |
| `MultiEditImageRequest.enhance_prompt` | Yes | `Boolean?` | `boolean` | OK |
| `MultiEditImageRequest.safe_mode` | Yes | `Boolean?` | `boolean` | OK |
| `MultiEditImageMultipartRequest` | **No** | — | object | Missing (IMG-05) |
| `BackgroundRemoveImageRequest.image` | **No** | — | `anyOf` (file / string) | Missing endpoint/model (IMG-02) |
| `BackgroundRemoveImageRequest.image_url` | **No** | — | `string` uri | Missing endpoint/model (IMG-02) |
| `GenerateImageResponse.id` | Yes | `String?` | `string` required | Should be non-null (IMG-06) |
| `GenerateImageResponse.images` | Yes | `List<String>?` | `array` required | Should be non-null (IMG-06) |
| `GenerateImageResponse.timing` | Yes | `GenerateImageTiming?` | `object` required | Should be non-null (IMG-06) |
| `GenerateImageResponse.request` | **No** | — | `object` nullable | Missing (IMG-06) |

## Severity summary

| Severity | Count | Finding IDs |
|----------|-------|-------------|
| P0 | 0 | — |
| P1 | 1 | IMG-01 |
| P2 | 10 | IMG-02, IMG-03, IMG-04, IMG-05, IMG-06, IMG-07, IMG-08, IMG-09, IMG-10, IMG-11 |
| P3 | 2 | IMG-12, IMG-13 |
| **Total** | **13** | — |

## App-level impact

`app/src/main/java/.../image/ImageViewModel.kt` calls `imageClient.edit()` and expects `response.images?.firstOrNull()` to contain a base64 string. Because `/image/edit` actually returns binary image data per swagger, the current SDK implementation and the app ViewModel are mutually incompatible. Fixing IMG-01 requires updating both the SDK method signature and the app consumer.

## Video + Audio


**Upstream authority:** `.source/venice-api-docs/swagger.yaml` (upstream HEAD `6e69346b`, schema version `20260814.194349`).  
**Local baseline:** branch `main` @ `1da3142`.

---

## Endpoint Coverage Matrix

| Endpoint | HTTP Verb | SDK Method | SDK Model(s) | Status | Finding IDs |
|----------|-----------|------------|--------------|--------|-------------|
| `/video/queue` | POST | `VideoClient.queue` | `QueueVideoRequest`, `VideoQueueResponse` | **PARTIAL** | VID-01 |
| `/video/retrieve` | POST | `VideoClient.retrieve` | `RetrieveVideoRequest`, `RetrieveVideoResponseStatus`, `VideoRetrieveResult` | **PARTIAL** | VID-03, VID-05 |
| `/video/complete` | POST | `VideoClient.complete` | `CompleteVideoRequest` | **PARTIAL** | VID-06 |
| `/video/quote` | POST | — | — | **MISSING** | VID-02 |
| `/video/transcriptions` | POST | — | — | **MISSING** | VID-04 |
| `/audio/speech` | POST | `AudioClient.speech` | `SpeechRequest` | **PARTIAL** | AUD-03, AUD-04, AUD-05 |
| `/audio/transcriptions` | POST | — | — | **MISSING** | AUD-02 |
| `/audio/queue` | POST | — | — | **MISSING** | AUD-01 |
| `/audio/retrieve` | POST | — | — | **MISSING** | AUD-01 |
| `/audio/quote` | POST | — | — | **MISSING** | AUD-01 |
| `/audio/complete` | POST | — | — | **MISSING** | AUD-01 |

**Legend:**

* **VERIFIED** — SDK implementation matches swagger request/response shape and required fields.
* **PARTIAL** — SDK exposes the endpoint but request/response models or behavior diverge from swagger.
* **MISSING** — SDK exposes no method for the endpoint.

---

## Model Field Coverage Matrix

### `QueueVideoRequest` (`swagger.yaml` `QueueVideoRequest`)

| Swagger Field | SDK Field | Nullable in SDK | Required in Swagger | Status | Notes |
|---------------|-----------|-----------------|---------------------|--------|-------|
| `model` | `model` | N | Y | OK | |
| `prompt` | `prompt` | **Y** | Y | GAP | VID-01 |
| `duration` | `duration` | **Y** | Y | GAP | VID-01 |
| `negative_prompt` | `negativePrompt` | Y | N | OK | |
| `seed` | `seed` | Y | N | OK | |
| `aspect_ratio` | `aspectRatio` | Y | N | OK | |
| `resolution` | `resolution` | Y | N | OK | |
| `fps` | `fps` | Y | N | OK | |
| `hide_watermark` | `hideWatermark` | Y | N | OK | |
| `safe_mode` | `safeMode` | Y | N | OK | |
| `consents` | — | — | N | **MISSING** | VID-01 |
| `upscale_factor` | — | — | N | **MISSING** | VID-01 |
| `audio` | — | — | N | **MISSING** | VID-01 |
| `image_url` | — | — | N | **MISSING** | VID-01 |
| `end_image_url` | — | — | N | **MISSING** | VID-01 |
| `audio_url` | — | — | N | **MISSING** | VID-01 |
| `video_url` | — | — | N | **MISSING** | VID-01 |
| `reference_image_urls` | — | — | N | **MISSING** | VID-01 |
| `reference_video_urls` | — | — | N | **MISSING** | VID-01 |
| `reference_audio_urls` | — | — | N | **MISSING** | VID-01 |
| `elements` | — | — | N | **MISSING** | VID-01 |
| `scene_image_urls` | — | — | N | **MISSING** | VID-01 |
| `keyframes` | — | — | N | **MISSING** | VID-01 |

### `QuoteVideoRequest` (`swagger.yaml` `QuoteVideoRequest`)

| Swagger Field | SDK Field | Status | Notes |
|---------------|-----------|--------|-------|
| `model` | — | **MISSING** | VID-02 |
| `duration` | — | **MISSING** | VID-02 |
| `aspect_ratio` | — | **MISSING** | VID-02 |
| `resolution` | — | **MISSING** | VID-02 |
| `upscale_factor` | — | **MISSING** | VID-02 |
| `audio` | — | **MISSING** | VID-02 |
| `video_url` | — | **MISSING** | VID-02 |
| `reference_video_total_duration` | — | **MISSING** | VID-02 |

### `RetrieveVideoRequest` / `CompleteVideoRequest`

| Swagger Field | SDK Field | Required in Swagger | Status | Notes |
|---------------|-----------|---------------------|--------|-------|
| `model` | `model` | Y | OK | |
| `queue_id` | `queueId` | Y | OK | |
| `delete_media_on_completion` | `deleteMediaOnCompletion` | N | OK | Only on retrieve. |

### `RetrieveVideoResponseStatus` (`swagger.yaml` `/video/retrieve` JSON response)

| Swagger Field | SDK Field | Status | Notes |
|---------------|-----------|--------|-------|
| `status` (enum `PROCESSING`/`COMPLETED`) | `status: String` | GAP | VID-05 |
| `average_execution_time` | `averageExecutionTime` | OK | |
| `execution_duration` | `executionDuration` | OK | |
| `download_url` (VPS-backed) | — | **MISSING** | VID-03 |

### `CreateVideoTranscriptionRequestSchema`

| Swagger Field | SDK Field | Status | Notes |
|---------------|-----------|--------|-------|
| `url` | — | **MISSING** | VID-04 |
| `response_format` | — | **MISSING** | VID-04 |

### `CreateSpeechRequestSchema` (`AudioModels.kt` `SpeechRequest`)

| Swagger Field | SDK Field | Status | Notes |
|---------------|-----------|--------|-------|
| `model` | `model` | OK | Required. |
| `input` | `input` | OK | Required. |
| `voice` | `voice` | OK | |
| `response_format` | `responseFormat` | OK | |
| `speed` | `speed` | OK | |
| `language` | — | **MISSING** | AUD-03 |
| `prompt` | — | **MISSING** | AUD-03 |
| `streaming` | — | **MISSING** | AUD-03 |
| `temperature` | — | **MISSING** | AUD-03 |
| `top_p` | — | **MISSING** | AUD-03 |
| `safe_mode` (not in swagger) | `safeMode` | **EXTRA** | AUD-03 |

### Queued Audio (`QueueAudioRequest`, `QuoteAudioRequest`, `RetrieveAudioRequest`, `CompleteAudioRequest`)

| Model | SDK Equivalent | Status | Notes |
|-------|----------------|--------|-------|
| `QueueAudioRequest` | — | **MISSING** | AUD-01 |
| `QuoteAudioRequest` | — | **MISSING** | AUD-01 |
| `RetrieveAudioRequest` | — | **MISSING** | AUD-01 |
| `CompleteAudioRequest` | — | **MISSING** | AUD-01 |

### `CreateTranscriptionRequestSchema`

| Swagger Field | SDK Field | Status | Notes |
|---------------|-----------|--------|-------|
| `file` | — | **MISSING** | AUD-02 |
| `model` | — | **MISSING** | AUD-02 |
| `response_format` | — | **MISSING** | AUD-02 |
| `timestamps` | — | **MISSING** | AUD-02 |
| `language` | — | **MISSING** | AUD-02 |

---

## Test Coverage Matrix

| Component | Test File Exists | Tests Endpoint Shape | Tests Error Paths | Tests Binary/Status Discrimination | Tests Polling |
|-----------|------------------|----------------------|-------------------|-------------------------------------|---------------|
| `VideoClient.queue` | **NO** | N/A | N/A | N/A | N/A |
| `VideoClient.retrieve` | **NO** | N/A | N/A | N/A | N/A |
| `VideoClient.complete` | **NO** | N/A | N/A | N/A | N/A |
| `VideoClient.quote` | **NO** | N/A | N/A | N/A | N/A |
| `VideoClient.transcribe` | **NO** | N/A | N/A | N/A | N/A |
| `AudioClient.speech` | **NO** | N/A | N/A | N/A | N/A |
| `AudioClient.transcribe` | **NO** | N/A | N/A | N/A | N/A |
| `AudioClient.queue/retrieve/quote/complete` | **NO** | N/A | N/A | N/A | N/A |

**Finding:** X-01.

---

## Queued-Job State Machine Assessment

| Concern | SDK Support | Finding | Notes |
|---------|-------------|---------|-------|
| Submission (`/queue`) | Partial | VID-01 | Missing fields; required fields nullable. |
| Quote before generate (`/quote`) | **No** | VID-02, AUD-01 | Violates AGENTS.md / upstream `agents.md`. |
| Status polling (`/retrieve`) | Single-shot only | VID-07 | No built-in polling helper. |
| Polling bounds / interval / timeout | **No** | VID-07 | Callers must implement. |
| Unknown status handling | Raw `String` | VID-05 | No enum; `COMPLETED` JSON handled as `Processing`. |
| Missing result URLs (`download_url`) | **No** | VID-03 | VPS-backed completion cannot be fetched. |
| Completion/cleanup (`/complete`) | Partial | VID-06 | Response body ignored. |
| Cancellation | Coroutine only | VID-07 | No explicit cancellation contract. |

---

## Severity Roll-up

| Severity | Count | IDs |
|----------|-------|-----|
| P0 | 0 | — |
| P1 | 6 | VID-01, VID-02, VID-03, VID-04, AUD-01, AUD-02 |
| P2 | 7 | VID-05, VID-06, VID-07, AUD-03, AUD-04, AUD-05, X-01 |
| P3 | 1 | X-02 |

## Models / Capabilities


Coverage matrix for Venice `/models`, `/models/traits`, and `/models/compatibility_mapping` response fields vs. the `:venice-sdk` model/capability classes.

**Legend:**
- ✅ Covered — field is parsed and exposed in a public SDK property.
- ⚠️ Partial — field is parsed but not fully exposed, or exposed with caveats.
- ❌ Not covered — field is ignored by the SDK parser.
- N/A — field is not part of the swagger schema for this endpoint.

---

## 1. GET `/models` response (`ModelResponse`)

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `id` | `VeniceModel.id` | ✅ | Non-null string. |
| `object` | `VeniceModel.objectType` | ✅ | Defaults to `"model"`. |
| `created` | `VeniceModel.created` | ✅ | `Long?`. Swagger type is `number`. |
| `owned_by` | `VeniceModel.ownedBy` | ✅ | Defaults to `"venice.ai"`. |
| `type` | `VeniceModel.type` | ✅ | Defaults to `"text"` if absent. |
| `model_spec` | `VeniceModel.modelSpec` | ✅ | Nested `ModelSpec`. |
| `context_length` | — | ❌ | Top-level OpenAI-compatible context length; not exposed. |
| `discount_to_user` | — | ❌ | Reseller discount; not exposed. |

## 2. `model_spec` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `name` | `ModelSpec.name` / `ModelCapabilities.name` | ✅ | |
| `description` | `ModelSpec.description` / `ModelCapabilities.description` | ✅ | |
| `modelSource` | `ModelSpec.modelSource` | ✅ | |
| `availableContextTokens` | `ModelSpec.availableContextTokens`, `ModelCapabilities.availableContextTokens`, `ModelCapabilities.maxContextTokens` | ✅ | `maxContextTokens` is `Int?`; see SDK-CORE-11. |
| `maxCompletionTokens` | `ModelSpec.maxCompletionTokens`, `ModelCapabilities.maxCompletionTokens` | ✅ | `Int?`; see SDK-CORE-11. |
| `privacy` | `ModelSpec.privacy`, `ModelCapabilities.privacy` | ✅ | Enum `private`/`anonymized` in swagger; stored as `String?`. |
| `uncensored` | `ModelSpec.uncensored`, `ModelCapabilities.uncensored` | ✅ | |
| `offline` | `ModelSpec.offline`, `ModelCapabilities.offline` | ✅ | Used to derive `supportsStreaming`; see SDK-CORE-04. |
| `beta` | `ModelSpec.beta` | ✅ | |
| `betaModel` | `ModelSpec.betaModel` | ✅ | |
| `traits` | `ModelSpec.traits`, `ModelCapabilities.traits` | ✅ | |
| `regionRestrictions` | — | ❌ | Not parsed. |
| `deprecation` | — | ❌ | `autoRemap`, `date`, `removesAt`, `replacementModelId`, `startsAt` not parsed. |
| `pricing` | — | ❌ | Token/image/audio/video pricing objects not parsed. |
| `embeddingDimensions` | — | ❌ | Embedding-specific. |
| `maxInputTokens` | — | ❌ | Embedding-specific. |
| `supportsCustomDimensions` | — | ❌ | Embedding-specific. |
| `supports_lyrics` | — | ❌ | Music-specific. |
| `lyrics_required` | — | ❌ | Music-specific. |
| `supports_force_instrumental` | — | ❌ | Music-specific. |
| `supports_loop` | — | ❌ | Music-specific. |
| `voices` | — | ❌ | TTS/music-specific. |
| `voice_cloning` | — | ❌ | TTS-specific object. |
| `default_voice` | — | ❌ | Music-specific. |
| `supports_custom_voice_id` | — | ❌ | TTS/music-specific. |
| `supports_language_code` | — | ❌ | Music-specific. |
| `supports_speed` | — | ❌ | Music-specific. |
| `default_speed` | — | ❌ | Music-specific. |
| `min_speed` | — | ❌ | Music-specific. |
| `max_speed` | — | ❌ | Music-specific. |
| `duration_options` | — | ❌ | Music-specific. |
| `min_duration` | — | ❌ | Music-specific. |
| `max_duration` | — | ❌ | Music-specific. |
| `default_duration` | — | ❌ | Music-specific. |
| `supported_formats` | — | ❌ | TTS/music-specific. |
| `default_format` | — | ❌ | TTS/music-specific. |
| `prompt_character_limit` | — | ❌ | Music-specific. |
| `min_prompt_length` | — | ❌ | Music-specific. |
| `lyrics_character_limit` | — | ❌ | Music-specific. |
| `supportsStyleReferences` | — | ❌ | Image-specific. |

## 3. `model_spec.capabilities` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| `optimizedForCode` | `ModelCapabilitiesSpec.optimizedForCode` | ✅ | Not exposed as top-level `ModelCapabilities` property. |
| `quantization` | — | ❌ | Not parsed. |
| `supportsFunctionCalling` | `ModelCapabilitiesSpec.supportsFunctionCalling`, `ModelCapabilities.supportsToolCalling` | ✅ | Renamed to `supportsToolCalling` in `ModelCapabilities`. |
| `supportsAudioInput` | `ModelCapabilitiesSpec.supportsAudioInput`, `ModelCapabilities.supportsAudioInput` | ✅ | |
| `supportsReasoning` | `ModelCapabilitiesSpec.supportsReasoning`, `ModelCapabilities.supportsReasoning` | ✅ | |
| `supportsReasoningEffort` | `ModelCapabilitiesSpec.supportsReasoningEffort` | ✅ | Not exposed in `ModelCapabilities`. |
| `reasoningEffortOptions` | — | ❌ | Not parsed. |
| `defaultReasoningEffort` | — | ❌ | Not parsed. |
| `supportsResponseSchema` | `ModelCapabilitiesSpec.supportsResponseSchema`, `ModelCapabilities.supportsResponseSchema` | ✅ | |
| `supportsMultipleImages` | `ModelCapabilitiesSpec.supportsMultipleImages` | ✅ | Used only to derive `supportsImageInput`; not exposed standalone. |
| `maxImages` | — | ❌ | Not parsed. |
| `maxVideos` | — | ❌ | Not parsed. |
| `supportsVision` | `ModelCapabilitiesSpec.supportsVision`, `ModelCapabilities.supportsVision` | ✅ | |
| `supportsVideoInput` | `ModelCapabilitiesSpec.supportsVideoInput`, `ModelCapabilities.supportsVideoInput` | ✅ | |
| `supportsWebSearch` | `ModelCapabilitiesSpec.supportsWebSearch`, `ModelCapabilities.supportsWebSearch` | ✅ | |
| `supportsLogProbs` | `ModelCapabilitiesSpec.supportsLogProbs` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsTeeAttestation` | `ModelCapabilitiesSpec.supportsTeeAttestation` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsE2EE` | `ModelCapabilitiesSpec.supportsE2EE` | ✅ | Not exposed in `ModelCapabilities`. |
| `supportsXSearch` | `ModelCapabilitiesSpec.supportsXSearch`, `ModelCapabilities.supportsXSearch` | ✅ | |

## 4. `model_spec.constraints` fields

| swagger field | SDK property | Status | Notes |
|---------------|--------------|--------|-------|
| Image constraints (`aspectRatios`, `resolutions`, `qualities`, `steps`, etc.) | — | ❌ | Not parsed. |
| Text constraints (`temperature`, `top_p`, `frequency_penalty`, etc.) | — | ❌ | Not parsed. |
| Video constraints (`aspect_ratios`, `durations`, `model_type`, etc.) | — | ❌ | Not parsed. |
| Inpaint constraints (`maxInputImages`, `singleImageAspectRatio`, etc.) | — | ❌ | Not parsed. |

## 5. `/models/traits` and `/models/compatibility_mapping`

| swagger shape | SDK shape | Status | Notes |
|---------------|-------------|--------|-------|
| `{"data": {"<trait>": "<model-id>"}, ...}` | `Map<String, String>` | ✅ | Parsed correctly. |
| `type` query parameter (default `text`) | — | ❌ | Not sent; see SDK-CORE-01. |

## 6. Derived `ModelCapabilities` properties

| SDK property | Derivation | Authoritative? | Notes |
|--------------|------------|----------------|-------|
| `supportsImageInput` | `supportsVision \|\| supportsMultipleImages` | ⚠️ Partial | Reasonable heuristic. |
| `supportsStreaming` | `spec?.offline != true` | ⚠️ Partial | Uses `offline` as proxy; see SDK-CORE-04. |
| `supportsSystemPrompt` | hard-coded `true` | ❌ No | Not based on runtime data. |
| `supportsTextChat` | `type == "text" \|\| "code"` | ⚠️ Partial | `"code"` is not a swagger response type. |
| `supportsImageGeneration` | `type == "image"` | ⚠️ Partial | Misses `inpaint`/`upscale` image types. |
| `inputModalities` | text + image/video/audio based on capability flags | ✅ | Reasonable derivation. |
| `outputModalities` | text + image/video/audio based on `type` | ✅ | Misses ASR text output nuance. |
| `compatibleWith` | inverted compatibility map | ✅ | Computed from `/models/compatibility_mapping`. |

## 7. Summary statistics

- **Top-level `/models` fields covered:** 6 / 8 (75%)
- **`model_spec` fields covered:** ~18 / 50+ (~35%)
- **`model_spec.capabilities` fields covered:** 12 / 15 (80%)
- **`model_spec.constraints` covered:** 0%
- **Traits / compatibility shape:** 100% (but missing `type` query support)

The SDK currently targets a chat-centric subset of the Venice model metadata. Media-specific metadata (pricing, constraints, audio/video/music fields, deprecation) is largely unexposed, limiting capability-aware UI and request validation.
