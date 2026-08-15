# SDK Chat + Streaming Contract Matrix

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
