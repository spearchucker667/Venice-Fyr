# 05-VENICE-SOURCE-OF-TRUTH.md

Venice API source-of-truth contract facts extracted from the official `veniceai/api-docs` mirror.

## Audit metadata

| Item | Value |
|------|-------|
| Audit date (UTC) | 2026-08-15T22:37:37Z |
| Repository HEAD | `1da314241b0ffcd622dcf2732940e2df432172c7` |
| Upstream remote | `https://github.com/veniceai/api-docs.git` |
| Upstream branch | `main` |
| Upstream HEAD | `6e69346b13695bd53ba33a1d34e7b28841e10f98` |
| Swagger `info.version` | `20260814.194349` |
| Bootstrap script | `scripts/bootstrap-venice-api-docs.sh` |
| Local state file | `.local/venice-api-docs.env` |
| Source mirror path | `.source/venice-api-docs/` |

## Drift detection

`SOURCE_BASELINE.md` records upstream HEAD `6e69346b13695bd53ba33a1d34e7b28841e10f98` and Swagger version `20260814.194349`. The current mirror HEAD and Swagger version match exactly, so no upstream drift is present.

## Source files consulted

| Path | Lines | Reviewed | Role |
|------|-------|----------|------|
| `.source/venice-api-docs/swagger.yaml` | 14595 | Y | authoritative source / governance |
| `.source/venice-api-docs/agents.md` | 93 | Y | authoritative source / governance |
| `.source/venice-api-docs/skill.md` | 10159 | Y | authoritative source / governance |
| `.source/venice-api-docs/data/static-models.json` | 1 | Y | authoritative source / governance |
| `.source/venice-api-docs/api-reference/error-codes.mdx` | 98 | Y | authoritative source / governance |
| `.source/venice-api-docs/api-reference/rate-limiting.mdx` | 104 | Y | authoritative source / governance |
| `.source/venice-api-docs/api-reference/api-spec.mdx` | 286 | Y | authoritative source / governance |
| `.source/venice-api-docs/guides/media/video-generation.mdx` | 399 | Y | authoritative source / governance |
| `.source/venice-api-docs/guides/media/music-and-sound-effects.mdx` | 208 | Y | authoritative source / governance |
| `.source/venice-api-docs/guides/projects/rust-llm-gateway.mdx` | 1241 | Y | authoritative source / governance |
| `.source/venice-api-docs/guides/features/reasoning-models.mdx` | 304 | Y | authoritative source / governance |
| `.source/venice-api-docs/models/video.mdx` | 25 | Y | authoritative source / governance |
| `.source/venice-api-docs/models/music.mdx` | 65 | Y | authoritative source / governance |
| `AGENTS.md` | 138 | Y | authoritative source / governance |
| `SOURCE_BASELINE.md` | 57 | Y | authoritative source / governance |

## Base URL

- Swagger `servers[0].url`: `https://api.venice.ai/api/v1` (`swagger.yaml:40`).
- All endpoint paths below are relative to this base URL.

## Authentication

### API key (Bearer)

- Security scheme: `BearerAuth` (`swagger.yaml:112-115`).
- Required header: `Authorization: Bearer <VENICE_API_KEY>` (`swagger.yaml:37-38`, `api-reference/api-spec.mdx:15-23`).
- `bearerFormat: JWT` is declared in the schema, but Venice accepts opaque API keys.

### x402 wallet auth

- Alternative security scheme: `siwx` (`swagger.yaml:116+`).
- Header: `SIGN-IN-WITH-X` (base64-encoded signed SIWX payload).
- Also accepts `PAYMENT-SIGNATURE` / legacy `X-402-Payment` / `X-PAYMENT` for top-up.
- Inference endpoints return `402` with top-up instructions when balance is insufficient.

## Full endpoint inventory (`swagger.yaml`)

| Method | Path | OperationId | Summary |
|--------|------|-------------|---------|
| GET | `/api_keys` | `getApiKeys` | /api/v1/api_keys |
| DELETE | `/api_keys` | `deleteApiKey` | /api/v1/api_keys |
| POST | `/api_keys` | `createApiKey` | /api/v1/api_keys |
| PATCH | `/api_keys` | `updateApiKey` | /api/v1/api_keys |
| GET | `/api_keys/generate_web3_key` | `getApiKeyGenerateWeb3Key` | /api/v1/api_keys/generate_web3_key |
| POST | `/api_keys/generate_web3_key` | `postApiKeyGenerateWeb3Key` | /api/v1/api_keys/generate_web3_key |
| GET | `/api_keys/rate_limits` | `getApiKeyRateLimits` | /api/v1/api_keys/rate_limits |
| GET | `/api_keys/rate_limits/log` | `getApiKeyRateLimitLogs` | /api/v1/api_keys/rate_limits/log |
| GET | `/api_keys/{id}` | `getApiKeyById` | Get API key details by ID |
| POST | `/audio/complete` | `completeAudio` | /api/v1/audio/complete |
| POST | `/audio/queue` | `queueAudio` | /api/v1/audio/queue |
| POST | `/audio/quote` | `quoteAudio` | /api/v1/audio/quote |
| POST | `/audio/retrieve` | `retrieveAudio` | /api/v1/audio/retrieve |
| POST | `/audio/speech` | `createSpeech` | /api/v1/audio/speech |
| POST | `/audio/transcriptions` | `createTranscription` | /api/v1/audio/transcriptions |
| POST | `/audio/voices` | `createClonedVoice` | /api/v1/audio/voices |
| POST | `/augment/scrape` | `webScrape` | /api/v1/augment/scrape |
| POST | `/augment/search` | `webSearch` | /api/v1/augment/search |
| POST | `/augment/text-parser` | `createTextParser` | /api/v1/augment/text-parser |
| GET | `/billing/balance` | `getBillingBalance` | /api/v1/billing/balance |
| GET | `/billing/usage` | `getBillingUsage` | /api/v1/billing/usage |
| GET | `/billing/usage-analytics` | `getBillingUsageAnalytics` | /api/v1/billing/usage-analytics |
| GET | `/billing/usage-history` | `getBillingUsageHistory` | /api/v1/billing/usage-history |
| GET | `/characters` | `listCharacters` | /api/v1/characters |
| GET | `/characters/{slug}` | `getCharacterBySlug` | /api/v1/characters/{slug} |
| GET | `/characters/{slug}/reviews` | `getCharacterReviews` | /api/v1/characters/{slug}/reviews |
| POST | `/chat/completions` | `createChatCompletion` | /api/v1/chat/completions |
| GET | `/crypto/rpc/networks` | `listCryptoRpcNetworks` | List supported crypto RPC networks |
| POST | `/crypto/rpc/{network}` | `cryptoRpcProxy` | Proxy a JSON-RPC request to a supported blockchain |
| POST | `/embeddings` | `createEmbedding` | /api/v1/embeddings |
| POST | `/image/background-remove` | `backgroundRemoveImage` | /api/v1/image/background-remove |
| POST | `/image/edit` | `editImage` | /api/v1/image/edit |
| POST | `/image/generate` | `generateImage` | /api/v1/image/generate |
| POST | `/image/multi-edit` | `multiEditImage` | /api/v1/image/multi-edit |
| GET | `/image/styles` | `` | /api/v1/image/styles |
| POST | `/image/upscale` | `upscaleImage` | /api/v1/image/upscale |
| POST | `/images/generations` | `simpleGenerateImage` | /api/v1/image/generations |
| GET | `/models` | `listModels` | /api/v1/models |
| GET | `/models/compatibility_mapping` | `listModelCompatibilityMapping` | /api/v1/models/compatibility_mapping |
| GET | `/models/traits` | `listModelTraits` | /api/v1/models/traits |
| POST | `/responses` | `createResponse` | Create a response (Alpha) |
| POST | `/video/complete` | `completeVideo` | /api/v1/video/complete |
| POST | `/video/queue` | `queueVideo` | /api/v1/video/queue |
| POST | `/video/quote` | `quoteVideo` | /api/v1/video/quote |
| POST | `/video/retrieve` | `retrieveVideo` | /api/v1/video/retrieve |
| POST | `/video/transcriptions` | `createVideoTranscription` | /api/v1/video/transcriptions |
| GET | `/x402/balance/{walletAddress}` | `getX402Balance` | /api/v1/x402/balance/{walletAddress} |
| POST | `/x402/top-up` | `topUpX402Balance` | /api/v1/x402/top-up |
| GET | `/x402/transactions/{walletAddress}` | `getX402Transactions` | /api/v1/x402/transactions/{walletAddress} |

Total: 49 operations across 45 paths.

## Standard error response schema

### `StandardError` (`swagger.yaml:208-215`)
```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "string",
      "description": "A description of the error"
    }
  },
  "required": [
    "error"
  ]
}
```

### `DetailedError` (`swagger.yaml:216-232`)
```json
{
  "type": "object",
  "properties": {
    "details": {
      "type": "object",
      "properties": {},
      "description": "Details about the incorrect input",
      "example": {
        "_errors": [],
        "field": {
          "_errors": [
            "Field is required"
          ]
        }
      }
    },
    "error": {
      "type": "string",
      "description": "A description of the error"
    }
  },
  "required": [
    "error"
  ]
}
```

### Operational error codes (`api-reference/error-codes.mdx`)

Key codes other auditors should handle:
- `401` / `AUTHENTICATION_FAILED`, `AUTHENTICATION_FAILED_INACTIVE_KEY`, `PRO_ONLY_MODEL`
- `402` / `INSUFFICIENT_BALANCE`, `API_KEY_DIEM_SPEND_LIMIT_EXCEEDED`, `API_KEY_USD_SPEND_LIMIT_EXCEEDED`
- `403` / `UNAUTHORIZED`, `API_ACCESS_DISABLED`
- `400` / `INVALID_REQUEST`, `INVALID_MODEL`, `REQUEST_ID_NOT_FOUND`, `TOO_MANY_TOKENS`
- `404` / `CHARACTER_NOT_FOUND`, `MODEL_NOT_FOUND`, `MEDIA_NOT_FOUND`
- `413` / `PAYLOAD_TOO_LARGE`
- `422` / `CONTENT_POLICY_VIOLATION`, `VIDEO_DURATION_TOO_LONG`, `IMAGE_TOO_LARGE`, `ASR_UPSTREAM_VALIDATION_FAILED`
- `429` / `RATE_LIMIT_EXCEEDED`, `MODEL_OVERLOADED`
- `500` / `INFERENCE_FAILED`, `UNKNOWN_ERROR`, `UPSCALE_FAILED`, `IMAGE_EDIT_ERROR`
- `502` / `TEE_ATTESTATION_FAILED`, `TEE_SIGNATURE_FAILED`, `ASR_UPSTREAM_FAILED`
- `503` / `MODEL_OFFLINE`, `MODEL_AT_CAPACITY`
- `504` / `REQUEST_TIMEOUT`

## Rate-limit headers

Returned on every authenticated response (`api-reference/rate-limiting.mdx:72-84`, `api-reference/api-spec.mdx:200-208`):

| Header | Meaning |
|--------|---------|
| `x-ratelimit-limit-requests` | Max requests in current window |
| `x-ratelimit-remaining-requests` | Requests remaining in current window |
| `x-ratelimit-reset-requests` | Unix timestamp when request window resets |
| `x-ratelimit-limit-tokens` | Max tokens per minute |
| `x-ratelimit-remaining-tokens` | Tokens remaining in current minute |
| `x-ratelimit-reset-tokens` | Seconds until token limit resets |
| `x-ratelimit-type` | `user`, `api_key`, or `global` |

Canonical limits are fetched from `GET /api_keys/rate_limits`. Default text-model tiers are XS/S/M/L (`api-reference/rate-limiting.mdx:25-46`).

## SSE stream format for chat

- Request: `POST /chat/completions` with `"stream": true` (`swagger.yaml:1373-1375`).
- Framing: Server-Sent Events (`text/event-stream`). Each non-empty line is prefixed with `data: `.
- Each event body is a JSON `chat.completion.chunk` object (OpenAI-compatible).
- Stream terminator: `data: [DONE]` followed by a blank line (`guides/projects/rust-llm-gateway.mdx:1198-1206`).
- Reasoning content arrives in `choices[0].delta.reasoning_content` before final answer (`guides/features/reasoning-models.mdx:54-101`).
- Optional: `venice_parameters.include_search_results_in_stream` emits search results as the first chunk (`api-reference/api-spec.mdx:1517-1521`).

> Note: `swagger.yaml` declares the `/chat/completions` 200 response only as `application/json` and does not explicitly list a `text/event-stream` content type or a streaming response schema. The SSE contract is documented in guides and observed OpenAI-compatible behavior.

## Media job semantics

### Video (`guides/media/video-generation.mdx`)

Flow: `POST /video/quote` → `POST /video/queue` → poll `POST /video/retrieve` → (optional) `POST /video/complete`.

| Step | Endpoint | Key request/response facts |
|------|----------|--------------------------|
| Quote | `POST /video/quote` | Response `{ "quote": number }` USD (`swagger.yaml` inline schema). |
| Queue | `POST /video/queue` | Required: `model`, `prompt`, `duration`. Response: `{ model, queue_id, download_url? }`. Private Grok models return a one-time `download_url` (`video-generation.mdx:36-87`). |
| Retrieve | `POST /video/retrieve` | Required: `model`, `queue_id`. Returns `application/json` `{ status, average_execution_time, execution_duration }` while processing; eventually returns `video/mp4` binary OR `application/json` `{ status: "COMPLETED" }` plus `download_url` (`video-generation.mdx:89-127`). |
| Complete | `POST /video/complete` | Required: `model`, `queue_id`. Response `{ success: boolean }` deletes stored media (`video-generation.mdx:139-156`). |

### Audio / music (`guides/media/music-and-sound-effects.mdx`)

Flow: `POST /audio/quote` → `POST /audio/queue` → poll `POST /audio/retrieve` → (optional) `POST /audio/complete`.

| Step | Endpoint | Key request/response facts |
|------|----------|--------------------------|
| Quote | `POST /audio/quote` | Response `{ "quote": number }` USD. |
| Queue | `POST /audio/queue` | Required: `model`, `prompt`. Response `{ model, queue_id, status: "QUEUED" }` (`music-and-sound-effects.mdx:53-90`). |
| Retrieve | `POST /audio/retrieve` | Returns `application/json` processing status while queued; eventually returns `audio/mpeg`, `audio/wav`, or `audio/flac` binary (`music-and-sound-effects.mdx:92-125`). |
| Complete | `POST /audio/complete` | Deletes stored audio after download. |

### Image

- `POST /image/generate` and OpenAI-compatible `POST /images/generations` return image binary or JSON URL depending on model/Accept.
- `POST /image/edit`, `/image/multi-edit`, `/image/upscale`, `/image/background-remove` return image binary (`image/png`, `image/jpeg`, `image/webp`).
- `GET /image/styles` lists available styles.

## Model discovery

- `GET /models?type=<asr|embedding|image|music|text|tts|upscale|inpaint|video|all|code>` returns `{ data: [ModelResponse], object, type }` (`swagger.yaml:4659+`, `api-reference/endpoint/models/list.mdx`).
- `GET /models/traits` returns `{ data: { "<trait>": "<model-id>" }, object, type }` (`swagger.yaml:6120+`).
- `GET /models/compatibility_mapping` returns `{ data: { "<alias>": "<model-id>" }, object, type }` (`swagger.yaml:6128+`).
- Model capabilities (vision, function calling, reasoning, E2EE, etc.) live in `ModelResponse.model_spec.capabilities` and must be treated as runtime data (`AGENTS.md:87-88`).

## Static model snapshot

- `.source/venice-api-docs/data/static-models.json` is a reference snapshot for fixture construction only; it must not be used as a production allowlist (`AGENTS.md:87-88`, `VENICE_API_SOURCE_BOOTSTRAP.md:72`).

## Key contract take-aways for other auditors

1. Base URL is fixed at `https://api.venice.ai/api/v1`; no environment switching without explicit product decision.
2. Auth is `Authorization: Bearer <key>`; x402 wallet auth is an alternative for paid endpoints.
3. Errors are not uniform: `StandardError` for most, `DetailedError` for 400 validation, `ContentViolationError`/`ProviderContentPolicyError` for media content policy, `X402InferencePaymentRequired` for 402.
4. Rate-limit headers are lower-case with hyphens; token reset is in seconds, request reset is a Unix timestamp.
5. Chat streaming is SSE; the terminal event is `data: [DONE]`; the swagger response declaration is incomplete.
6. Video and audio generation are async queue/poll/complete jobs; images are synchronous binary responses.
7. Never hard-code model IDs; use `/models`, `/models/traits`, and `/models/compatibility_mapping` at runtime.
