# SDK Video + Audio Audit Matrix

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
