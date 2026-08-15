# Venice API Spec Currency Allowlist

**Source of truth:** `.source/venice-api-docs/swagger.yaml`
**Upstream HEAD:** `6e69346b`
**Swagger version:** `20260814.194349`
**Generated:** 2026-08-15
**Scope:** endpoints targeted by the SDK remediation wave (video, audio, image, embeddings, characters).

---

## Executive Summary

All endpoints requested for verification **EXIST** in the current authoritative `swagger.yaml`. Two important spec-currency corrections affect the queued-media implementation:

1. **`download_url` appears on `/video/queue` responses, NOT on `/video/retrieve`, `/audio/queue`, or `/audio/retrieve`.** When a VPS-backed video model returns a pre-signed URL at queue time, the subsequent `/video/retrieve` calls return JSON status only; the caller must fetch the pre-signed URL after status is `COMPLETED`. Audio retrieve returns binary audio directly.
2. **`/video/retrieve` JSON status enum is `PROCESSING` / `COMPLETED`.** The retrieve endpoint itself does **not** return a `download_url`.

---

## Endpoint Allowlist

### `POST /video/quote`
- **Status:** EXISTS (`swagger.yaml:11778`)
- **Request schema:** `QuoteVideoRequest` (`swagger.yaml:3819`)
  - Required: `model` (string), `duration` (string enum: `1s`…`30s`, `"-1"`, `"1 gen"`, `"auto"`, `"Auto"`)
  - Optional: `aspect_ratio` (string enum), `resolution` (string enum), `upscale_factor` (integer enum `1|2|4`), `audio` (boolean, default `true`), `video_url` (string), `reference_video_total_duration` (number ≥0)
- **Response:** `200 application/json` → `{ "quote": number }` (`swagger.yaml:11792`)
- **Citation:** `swagger.yaml:11778-11816`

### `POST /video/transcriptions`
- **Status:** EXISTS (`swagger.yaml:11983`)
- **Request schema:** `CreateVideoTranscriptionRequestSchema` (`swagger.yaml:3985`)
  - Required: `url` (string)
  - Optional: `response_format` (string enum `json|text`, default `json`)
- **Response:** `200 application/json` → `{ "transcript": string, "lang": string }` OR `200 text/plain` → transcript text (`swagger.yaml:12016`)
- **Citation:** `swagger.yaml:11983-12048`

### `POST /audio/speech`
- **Status:** EXISTS (`swagger.yaml:10878`)
- **Request schema:** `CreateSpeechRequestSchema` (`swagger.yaml:3355`)
  - Required: `input` (string, 1-4096 chars)
  - Optional: `model` (string enum, default `tts-kokoro`), `voice` (string, default `af_sky`), `response_format` (string enum `mp3|opus|aac|flac|wav|pcm`), `speed` (number 0.25-4, default `1`), `language` (string), `prompt` (string ≤500), `streaming` (boolean, default `false`), `temperature` (number 0-2), `top_p` (number 0-1)
  - **Note:** `safe_mode` is **NOT** in this schema.
- **Response:** `200` binary → `audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, `audio/wav` (`swagger.yaml:10925`)
- **Citation:** `swagger.yaml:10878-11048`

### `POST /audio/transcriptions`
- **Status:** EXISTS (`swagger.yaml:11049`)
- **Request schema:** `CreateTranscriptionRequestSchema` (`swagger.yaml:3476`), content-type `multipart/form-data`
  - Required: `file` (binary)
  - Optional: `model` (string enum, default `nvidia/parakeet-tdt-0.6b-v3`), `response_format` (string enum `json|text`, default `json`), `timestamps` (boolean, default `false`), `language` (string)
- **Response:** `200 application/json` → `{ "text": string, "duration": number, "timestamps": { "word": [...], "segment": [...], "char": [...] } }` OR `200 text/plain` (`swagger.yaml:11082`)
- **Citation:** `swagger.yaml:11049-11263`

### `POST /audio/queue`
- **Status:** EXISTS (`swagger.yaml:12402`)
- **Request schema:** `QueueAudioRequest` (`swagger.yaml:4041`)
  - Required: `model` (string), `prompt` (string)
  - Optional: `lyrics_prompt` (string), `duration_seconds` (integer or numeric string), `force_instrumental` (boolean), `lyrics_optimizer` (boolean), `loop` (boolean), `voice` (string), `language_code` (string), `speed` (number 0.25-4)
- **Response:** `200 application/json` → `{ "model": string, "queue_id": string, "status": "QUEUED" }` (`swagger.yaml:12434`)
  - **No `download_url` field.** Audio retrieve returns binary audio directly when complete.
- **Citation:** `swagger.yaml:12402-12552`

### `POST /audio/quote`
- **Status:** EXISTS (`swagger.yaml:12553`)
- **Request schema:** `QuoteAudioRequest` (`swagger.yaml:4113`)
  - Required: `model` (string)
  - Optional: `duration_seconds` (integer or numeric string), `character_count` (integer >0)
- **Response:** `200 application/json` → `{ "quote": number }` (`swagger.yaml:12566`)
- **Citation:** `swagger.yaml:12553-12590`

### `POST /audio/retrieve`
- **Status:** EXISTS (`swagger.yaml:12591`)
- **Request schema:** `RetrieveAudioRequest` (`swagger.yaml:4159`)
  - Required: `model` (string), `queue_id` (string)
  - Optional: `delete_media_on_completion` (boolean, default `false`)
- **Response:** `200` mixed (`swagger.yaml:12625`)
  - `application/json` status → `{ "status": "PROCESSING", "average_execution_time": number, "execution_duration": number }`
  - Binary audio → `audio/mpeg`, `audio/wav`, `audio/flac`
  - **No `download_url` field on this response.**
- **Citation:** `swagger.yaml:12591-12762`

### `POST /audio/complete`
- **Status:** EXISTS (`swagger.yaml:12280`)
- **Request schema:** `CompleteAudioRequest` (`swagger.yaml:4143`)
  - Required: `model` (string), `queue_id` (string)
- **Response:** `200 application/json` → `{ "success": boolean }` (`swagger.yaml:12315`)
- **Citation:** `swagger.yaml:12280-12401`

### `POST /image/background-remove`
- **Status:** EXISTS (`swagger.yaml:8330`)
- **Request schema:** `BackgroundRemoveImageRequest` (`swagger.yaml:3233`), content-type `application/json` or `multipart/form-data`
  - Optional: `image` (file or base64 string), `image_url` (string URI)
- **Response:** `200 image/png` binary (`swagger.yaml:8368`)
- **Citation:** `swagger.yaml:8330-8463`

### `GET /image/styles`
- **Status:** EXISTS (`swagger.yaml:7668`)
- **Request:** none (unauthenticated optional)
- **Response:** `200 application/json` → `{ "data": [string], "object": "list" }` (`swagger.yaml:7678`)
- **Citation:** `swagger.yaml:7668-7714`

### `POST /images/generations`
- **Status:** EXISTS (`swagger.yaml:7455`)
- **Request schema:** `SimpleGenerateImageRequest` (`swagger.yaml:2778`)
  - Required: `prompt` (string)
  - Optional: `model`, `background`, `moderation`, `n` (integer, max 1), `output_compression`, `output_format`, `quality`, `response_format`, `size`, `style`, `user`
- **Response:** `200 application/json` → `{ "created": integer, "data": [{ "b64_json": string } | { "url": string }] }` (`swagger.yaml:7498`)
- **Citation:** `swagger.yaml:7455-7667`

### `POST /embeddings`
- **Status:** EXISTS (`swagger.yaml:10670`)
- **Request schema:** `CreateEmbeddingRequestSchema` (`swagger.yaml:3252`)
  - Required: `input` (string, string array, token array, or array of token arrays), `model` (string)
  - Optional: `dimensions` (integer ≥1), `encoding_format` (string enum `float|base64`, default `float`), `user` (string)
- **Response:** `200 application/json` → `{ "data": [{ "embedding": [number], "index": integer, "object": "embedding" }], "model": string, "object": "list", "usage": { "prompt_tokens": integer, "total_tokens": integer } }` (`swagger.yaml:10711`)
- **Citation:** `swagger.yaml:10670-10877`

### `GET /characters`
- **Status:** EXISTS (`swagger.yaml:10015`)
- **Query parameters:** `categories` (array[string]), `isAdult` (string enum `true|false`), `isPro` (string enum `true|false`), `isWebEnabled` (string enum `true|false`), `limit` (integer 0-100, default 50), `modelId` (array[string]), `offset` (integer, default 0), `search` (string), `sortBy` (enum), `sortOrder` (enum `asc|desc`), `tags` (array[string])
- **Response:** `200 application/json` → `{ "data": [Character], "object": "list" }` (`swagger.yaml:10155`)
- **Citation:** `swagger.yaml:10015-10300`

---

## `/video/retrieve` Response Schema Variants

The `/video/retrieve` `200` response is polymorphic by `Content-Type` (`swagger.yaml:11850`):

1. **JSON status** (`application/json`):
   ```yaml
   status: string enum [PROCESSING, COMPLETED]
   average_execution_time: number
   execution_duration: number
   ```
   Required fields: `status`, `average_execution_time`, `execution_duration`.

2. **Binary video** (`video/mp4`):
   ```yaml
   schema:
     type: string
     format: binary
   ```

**Important:** The `/video/retrieve` JSON schema does **not** contain a `download_url`. The pre-signed download URL is returned by `/video/queue` when `download_url` is present (`swagger.yaml:11625`).

---

## `/image/*` Response Content Types

| Endpoint | Method | Success Content-Type(s) | Notes |
|----------|--------|------------------------|-------|
| `/image/generate` | POST | `application/json`, `image/jpeg`, `image/png`, `image/webp` | JSON when `return_binary=false`; binary when `return_binary=true` |
| `/images/generations` | POST | `application/json` | OpenAI-compatible JSON |
| `/image/styles` | GET | `application/json` | List of style strings |
| `/image/upscale` | POST | `image/png` | Binary only |
| `/image/edit` | POST | `image/png`, `image/jpeg`, `image/webp` | Binary only |
| `/image/multi-edit` | POST | `image/png`, `image/jpeg`, `image/webp` | Binary only |
| `/image/background-remove` | POST | `image/png` | Binary only |

---

## Dispositions (spec-currency relevant findings)

| ID | Original Severity/Status | Disposition | Corrected Severity/Status | Why |
|----|--------------------------|-------------|---------------------------|-----|
| VID-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED | `POST /video/quote` exists at `swagger.yaml:11778`; SDK method is missing. |
| VID-03 | P1 / CONFIRMED | PARTIALLY_VALID | P2 / RECLASSIFIED | `download_url` exists, but it is returned by `/video/queue` (`swagger.yaml:11625`), not `/video/retrieve`. The SDK still needs to capture and expose it, but the original claim that retrieve returns it is wrong. |
| VID-04 | P1 / CONFIRMED | VALID | P1 / CONFIRMED | `POST /video/transcriptions` exists at `swagger.yaml:11983`; SDK method is missing. |
| AUD-01 | P1 / CONFIRMED | VALID | P1 / CONFIRMED | `/audio/queue`, `/audio/retrieve`, `/audio/quote`, `/audio/complete` all exist (`swagger.yaml:12402`, `12591`, `12553`, `12280`); SDK methods are missing. |
| AUD-02 | P1 / CONFIRMED | VALID | P1 / CONFIRMED | `POST /audio/transcriptions` exists at `swagger.yaml:11049`; SDK method is missing. |
| AUD-03 | P2 / CONFIRMED | VALID | P2 / CONFIRMED | `CreateSpeechRequestSchema` includes `language`, `prompt`, `streaming`, `temperature`, `top_p` and does **not** include `safe_mode` (`swagger.yaml:3355`). |
| IMG-02 | P2 / CONFIRMED | VALID | P2 / CONFIRMED | `POST /image/background-remove` exists at `swagger.yaml:8330`; SDK method is missing. |
| IMG-03 | P2 / CONFIRMED | VALID | P2 / CONFIRMED | `GET /image/styles` exists at `swagger.yaml:7668`; SDK method is missing. |
| IMG-04 | P2 / CONFIRMED | VALID | P2 / CONFIRMED | `POST /images/generations` exists at `swagger.yaml:7455`; SDK method is missing. |

---

## Key Evidence Quotes (max 12)

1. `/video/quote` path declaration: `swagger.yaml:11778` — `  /video/quote:`
2. `/video/quote` response: `swagger.yaml:11792` — `description: Video generation price quote`
3. `/video/retrieve` status enum: `swagger.yaml:11865` — `enum: [PROCESSING, COMPLETED]`
4. `/video/queue` download_url: `swagger.yaml:11625` — `download_url: ... Pre-signed URL to download the completed video. Only present for VPS-backed models.`
5. `/audio/queue` response: `swagger.yaml:12434` — `{ "model": string, "queue_id": string, "status": "QUEUED" }` (no download_url).
6. `/audio/retrieve` status enum: `swagger.yaml:12640` — `enum: [PROCESSING]`
7. `/audio/speech` content types: `swagger.yaml:10936` — `audio/aac: ... audio/flac: ... audio/mpeg: ...`
8. `/audio/speech` schema no safe_mode: `swagger.yaml:3355` — `CreateSpeechRequestSchema:` (properties are `input`, `language`, `model`, `prompt`, `response_format`, `speed`, `streaming`, `temperature`, `top_p`, `voice`)
9. `/image/background-remove` response: `swagger.yaml:8379` — `image/png: ... format: binary`
10. `/image/styles` response: `swagger.yaml:7680` — `data: type: array, items: type: string`
11. `/images/generations` response: `swagger.yaml:7553` — `application/json: ... data: items: anyOf [b64_json, url]`
12. `/embeddings` response: `swagger.yaml:10729` — `data: ... embedding: type: array, items: type: number`
