# Media Findings Revalidation

**Scope:** `:venice-sdk` image, video, and audio clients/models (`venice-sdk/.../sdk/{image,video,audio}/`) plus the app-level image consumer.

**Upstream authority:** `.source/venice-api-docs/swagger.yaml` @ upstream `6e69346b`, `info.version 20260814.194349`.

**Local baseline:** `main` @ `ee2cd7a` (coordinator compile fix for `VeniceForgeSdk.kt` applied; not reverted).

**Method:** static read-only review of source and swagger; no Gradle executed per agent rules.

---

## Disposition summary

| ID | Original severity/status | Disposition | Corrected severity/status |
|---|---|---|---|
| IMG-01 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| IMG-02 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-03 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-04 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-05 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-06 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-07 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-08 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-09 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-10 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-11 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| IMG-12 | P3 / CONFIRMED | **VALID** | P3 / CONFIRMED |
| IMG-13 | P3 / CONFIRMED | **VALID** | P3 / CONFIRMED |
| VID-01 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| VID-02 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| VID-03 | P1 / CONFIRMED | **PARTIALLY_VALID** | P1 / CONFIRMED (corrected root cause) |
| VID-04 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| VID-05 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| VID-06 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| VID-07 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| AUD-01 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| AUD-02 | P1 / CONFIRMED | **VALID** | P1 / CONFIRMED |
| AUD-03 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| AUD-04 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| AUD-05 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| X-01 | P2 / CONFIRMED | **VALID** | P2 / CONFIRMED |
| X-02 | P3 / CONFIRMED | **VALID** | P3 / CONFIRMED |

---

## Findings

### IMG-01 | P1 | VALID

**Original:** P1 / CONFIRMED — `upscale`, `edit`, `multiEdit` parse binary image responses as JSON.

**Source evidence:**
```kotlin
// venice-sdk/.../sdk/image/ImageClient.kt:27-34
suspend fun upscale(...): GenerateImageResponse =
    executeRequest(...)
suspend fun edit(...): GenerateImageResponse =
    executeRequest(...)
suspend fun multiEdit(...): GenerateImageResponse =
    executeRequest(...)

// ImageClient.kt:59-61
val body = res.body?.string().orEmpty()
json.decodeFromString(GenerateImageResponse.serializer(), body)
```

**Spec evidence:**
```yaml
# swagger.yaml:/paths/image/upscale/post/responses/200/content
image/png:
  schema:
    format: binary
# swagger.yaml:/paths/image/edit/post/responses/200/content (lines 7994-8006)
content:
  image/png: ...
  image/jpeg: ...
  image/webp: ...
# swagger.yaml:/paths/image/multi-edit/post/responses/200/content (lines 8236-8248)
image/png: ...
image/jpeg: ...
image/webp: ...
```

**Why original was right/wrong:** Correct. The SDK sends `Accept: application/json` and deserializes `GenerateImageResponse`, but the contract is raw binary `image/*` for these three endpoints. A successful 200 will fail JSON parsing.

**Correct remediation:** Change `upscale`, `edit`, and `multiEdit` to return `ByteArray`, send `Accept: image/*` (or a format-specific MIME), and consume `ResponseBody.bytes()`. Update `ImageViewModel.kt:110-115`, which currently expects `response.images?.firstOrNull()` from `edit()`.

**Tests required:** Mock 200 binary `image/png` responses for `edit`, `upscale`, and `multiEdit`; verify `Accept` header; verify non-2xx error bodies are still parsed.

---

### IMG-02 | P2 | VALID

**Source evidence:** `VeniceEndpoints.kt:20` declares `IMAGE_BACKGROUND_REMOVE = "image/background-remove"`; `ImageClient.kt` has no method using it.

**Spec evidence:** `swagger.yaml:/paths/image/background-remove/post` (lines 8330-8382) returns `image/png` binary and accepts JSON or multipart.

**Why original was right/wrong:** Correct. Endpoint constant exists but no public SDK method exists.

**Correct remediation:** Add `backgroundRemove(apiKey, BackgroundRemoveImageRequest): ByteArray`. Support JSON (`image` base64 / `image_url`) and multipart file upload variants.

**Tests required:** Unit test returning PNG bytes; verify request body shape for JSON and multipart.

---

### IMG-03 | P2 | VALID

**Source evidence:** `VeniceEndpoints.kt:16` declares `IMAGE_STYLES`; `ImageClient.kt` has no method.

**Spec evidence:** `swagger.yaml:/paths/image/styles/get` (lines 7668-7702) returns `{ "data": [...], "object": "list" }`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `styles(): ImageStylesResponse` with `data: List<String>` and `object: String?`.

**Tests required:** Parse example response.

---

### IMG-04 | P2 | VALID

**Source evidence:** `VeniceEndpoints.kt:15` declares `IMAGE_GENERATIONS_COMPAT`; `ImageClient.kt` has no method.

**Spec evidence:** `swagger.yaml:/paths/images/generations/post` (lines 7455-7574) returns OpenAI-compatible `{ created, data: [{ b64_json | url }] }`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `simpleGenerate(apiKey, SimpleGenerateImageRequest): SimpleGenerateImageResponse`.

**Tests required:** Request/response serialization.

---

### IMG-05 | P2 | VALID

**Source evidence:** `ImageClient.kt:44` and `:73` set `Content-Type: application/json` via `reqBody.toRequestBody(jsonMedia)`; no multipart builder exists.

**Spec evidence:** `/image/upscale`, `/image/edit`, `/image/multi-edit`, and `/image/background-remove` all accept `multipart/form-data` (e.g., `swagger.yaml:/paths/image/edit/post/requestBody/content/multipart/form-data`).

**Why original was right/wrong:** Correct. The SDK only supports JSON/base64 uploads.

**Correct remediation:** Add multipart upload variants for `upscale`, `edit`, `multiEdit`, and `backgroundRemove` that accept `okhttp3.RequestBody` or file paths/URIs.

**Tests required:** Verify multipart body parts and `Content-Type: multipart/form-data` boundary.

---

### IMG-06 | P2 | VALID

**Source evidence:** `ImageModels.kt:79-84` defines `GenerateImageResponse(id, images, timing)` with all fields nullable and no `request` field.

**Spec evidence:** `swagger.yaml:/paths/image/generate/post/responses/200/content/application/json/schema` (lines 7319-7356) requires `id`, `images`, `timing`, and includes a nullable `request` echo object.

**Why original was right/wrong:** Correct.

**Correct remediation:** Make `id`, `images`, and `timing` non-nullable; add `request: JsonElement?` (or a typed echo) to `GenerateImageResponse`.

**Tests required:** Deserialize a generate response containing `request`; assert `request` is accessible.

---

### IMG-07 | P2 | VALID

**Source evidence:** `ImageModels.kt:13-35` `GenerateImageRequest` omits `embed_exif_metadata` and `lora_strength`.

**Spec evidence:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/embed_exif_metadata` (line 2594) and `/properties/lora_strength` (line 2628). `inpaint` is also present but deprecated.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `embedExifMetadata: Boolean?`, `loraStrength: Int?`, and optionally deprecated `inpaint` to `GenerateImageRequest`.

**Tests required:** Serialization test verifying wire names.

---

### IMG-08 | P2 | VALID

**Source evidence:** `ImageModels.kt:45-55` `EditImageRequest` has no `modelId`.

**Spec evidence:** `swagger.yaml:/components/schemas/EditImageRequest/properties/modelId` (lines 2992-2997) is deprecated but present.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `@SerialName("modelId") val modelId: String? = null` to `EditImageRequest`.

**Tests required:** Serialization test verifying `modelId` is sent when provided.

---

### IMG-09 | P2 | VALID

**Source evidence:** `VeniceForgeSdk.kt:167-201` maps 401/403→Authentication, 400/422→Validation, 500-599→Server, 429→RateLimit, and everything else (including 402) to generic `Http`.

**Spec evidence:** Image endpoints return `402` for insufficient balance (e.g., `swagger.yaml:/paths/image/generate/post/responses/402`).

**Why original was right/wrong:** Correct. `402` is not surfaced distinctly, so callers cannot detect payment failures.

**Correct remediation:** Add `VeniceSdkException.PaymentRequired` (or extend `Http`) for HTTP 402, parsing `X402InferencePaymentRequired` fields when present.

**Tests required:** Unit tests for 402 responses on image endpoints.

---

### IMG-10 | P2 | VALID

**Source evidence:** `ImageClient.kt:36-63` `executeRequest` reads only the response body string; no headers are captured.

**Spec evidence:** `/image/generate` 200 defines headers `x-venice-is-blurred`, `x-venice-is-content-violation`, `x-venice-model-deprecation-warning`, `x-venice-model-deprecation-date`, `x-venice-deprecated`, `x-venice-deprecated-replacement`, `X-Balance-Remaining` (lines 7296-7315). `/image/edit` and `/image/multi-edit` define similar headers.

**Why original was right/wrong:** Correct.

**Correct remediation:** Return a wrapper object (e.g., `ImageGenerationResult`) containing `body: GenerateImageResponse` and `headers: ImageResponseHeaders`, or expose headers via a callback.

**Tests required:** Assert header parsing for mocked responses.

---

### IMG-11 | P2 | VALID

**Source evidence:** `ImageClient.kt:19-25`: `generate()` always sends `Accept: application/json`; `generateBinary()` requires `returnBinary == true` and sends `Accept: image/*`.

**Spec evidence:** `swagger.yaml:/paths/image/generate/post/responses/200/content` includes `image/jpeg`, `image/png`, `image/webp` for `return_binary=true`.

**Why original was right/wrong:** Correct. A caller can call `generate()` with `returnBinary=true` and get a confusing JSON parse failure.

**Correct remediation:** Either merge `generate`/`generateBinary` into a single method that inspects `returnBinary`, or add an explicit check in `generate()` throwing `IllegalArgumentException` when `returnBinary == true`.

**Tests required:** Unit test asserting `generate()` with `returnBinary=true` fails fast.

---

### IMG-12 | P3 | VALID

**Source evidence:** `ImageModels.kt:13-35` `GenerateImageRequest` has no client-side validation.

**Spec evidence:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/height` (lines 2609-2615) defines `minimum: 0`, `exclusiveMinimum: true`, `maximum: 1280`; `cfg_scale` (lines 2586-2592) `minimum: 0`, `exclusiveMinimum: true`, `maximum: 20`.

**Why original was right/wrong:** Correct, but minor UX/cost issue.

**Correct remediation:** Add lightweight validation in `GenerateImageRequest` init block or in `ImageClient.generate()`.

**Tests required:** Unit tests for out-of-range width/height/cfg_scale.

---

### IMG-13 | P3 | VALID

**Source evidence:** `ImageClientTest.kt` contains only two tests and no coverage for `upscale`, `multiEdit`, `generateBinary`, errors, multipart, or headers.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add tests for all public image methods, error paths, binary responses, and serialization edge cases.

**Tests required:** See IMG-01 through IMG-12.

---

### VID-01 | P1 | VALID

**Source evidence:** `VideoModels.kt:7-18` `QueueVideoRequest` marks `prompt` and `duration` nullable and is missing `consents`, `upscale_factor`, `audio`, `image_url`, `end_image_url`, `audio_url`, `video_url`, reference URLs, `elements`, `scene_image_urls`, and `keyframes`.

**Spec evidence:** `swagger.yaml:/components/schemas/QueueVideoRequest` (lines 3548-3818) requires `model`, `prompt`, and `duration` and defines all of the above optional fields.

**Why original was right/wrong:** Correct.

**Correct remediation:** Expand `QueueVideoRequest` to match swagger; make `model`, `prompt`, `duration` non-nullable.

**Tests required:** Serialization tests for representative queue payloads and required-field omission.

---

### VID-02 | P1 | VALID

**Source evidence:** `VideoClient.kt` declares only `queue`, `complete`, `retrieve`; `VeniceEndpoints.VIDEO_QUOTE` (`VeniceEndpoints.kt:48`) is unused.

**Spec evidence:** `swagger.yaml:/paths/video/quote/post` (lines 11778-11816) accepts `QuoteVideoRequest` and returns `{ "quote": number }`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `quote(apiKey, QuoteVideoRequest): VideoQuoteResponse`.

**Tests required:** Mocked quote request/response test.

---

### VID-03 | P1 | PARTIALLY_VALID

**Source evidence:**
```kotlin
// VideoClient.kt:52-62
if (contentType.contains("application/json")) {
    ...
    VideoRetrieveResult.Processing(statusRes.status, ...)
} else {
    ...
    VideoRetrieveResult.Completed(bytes)
}
```
`RetrieveVideoResponseStatus` (`VideoModels.kt:35-39`) has no `downloadUrl`.

**Spec evidence:**
```yaml
# swagger.yaml:/paths/video/retrieve/post/responses/200/content/application/json/schema (lines 11861-11885)
properties:
  status:
    enum: [PROCESSING, COMPLETED]
  average_execution_time: ...
  execution_duration: ...
required: [status, average_execution_time, execution_duration]

# swagger.yaml:/paths/video/queue/post/responses/200 (lines 11612-11635)
properties:
  model: ...
  queue_id: ...
  download_url:
    description: "Pre-signed URL to download the completed video. Only present for VPS-backed models. When provided, the retrieve endpoint returns JSON status only (no video stream)."
```

**Why original was right/wrong:** Partially correct. The SDK does misclassify a `COMPLETED` JSON response as `Processing`, which is a real P1 bug. However, the original remediation incorrectly implies `download_url` should be added to the `/video/retrieve` JSON response model. The swagger places `download_url` only on the `/video/queue` response; `/video/retrieve` JSON contains only `status`, `average_execution_time`, and `execution_duration`.

**Correct remediation:**
1. Introduce a `VideoStatus` enum (`PROCESSING`, `COMPLETED`).
2. Parse the JSON body and branch on `status`.
3. Return `VideoRetrieveResult.Processing` only when `status == PROCESSING`.
4. Return `VideoRetrieveResult.Completed` when the response is binary `video/mp4`.
5. For VPS-backed models, callers must use `VideoQueueResponse.downloadUrl` after observing `COMPLETED`; do **not** add `download_url` to `RetrieveVideoResponseStatus`.

**Tests required:** Mock retrieve responses for `PROCESSING` JSON, `COMPLETED` JSON, and binary `video/mp4`.

---

### VID-04 | P1 | VALID

**Source evidence:** `VideoClient.kt` has no method for `VeniceEndpoints.VIDEO_TRANSCRIPTIONS`.

**Spec evidence:** `swagger.yaml:/paths/video/transcriptions/post` (lines 11983-12048) accepts `CreateVideoTranscriptionRequestSchema` (`url`, `response_format`) and returns `{ "transcript", "lang" }` or `text/plain`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `transcribe(apiKey, CreateVideoTranscriptionRequest): VideoTranscriptionResponse`.

**Tests required:** Mock JSON and text/plain responses.

---

### VID-05 | P2 | VALID

**Source evidence:** `VideoModels.kt:36` `RetrieveVideoResponseStatus.status` is a raw `String`.

**Spec evidence:** `swagger.yaml:/paths/video/retrieve/post/responses/200/content/application/json/schema/properties/status` (lines 11865-11870) enum is `PROCESSING` / `COMPLETED`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Introduce `VideoStatus` enum and use it in response/result models.

**Tests required:** Serialization round-trip for each enum value.

---

### VID-06 | P2 | VALID

**Source evidence:** `VideoClient.kt:22-25` `complete` executes and ignores the response body, returning `Unit`.

**Spec evidence:** `swagger.yaml:/paths/video/complete/post/responses/200` (lines 11484-11504) returns `{ "success": boolean }` and documents: "A success value of false indicates cleanup did not complete and can be retried later."

**Why original was right/wrong:** Correct.

**Correct remediation:** Parse the JSON body and return `Boolean` (or a typed response) from `complete`.

**Tests required:** Mock complete responses with `success: true` and `success: false`.

---

### VID-07 | P2 | VALID

**Source evidence:** `VideoClient.kt` exposes only single-shot `retrieve`; no polling helper.

**Spec evidence:** `swagger.yaml:/paths/video/queue/post` returns `queue_id`; `/paths/video/retrieve/post` returns `PROCESSING`/`COMPLETED` status.

**Why original was right/wrong:** Correct as an architecture/reliability gap, though not a wire-contract violation.

**Correct remediation:** Add a `pollForResult` suspend function with bounded retries, exponential back-off, and cooperative cancellation.

**Tests required:** Mock polling sequence (PROCESSING → COMPLETED), timeout path, cancellation path.

---

### AUD-01 | P1 | VALID

**Source evidence:** `AudioClient.kt:15-46` contains only `speech`; `VeniceEndpoints.AUDIO_QUEUE`, `AUDIO_RETRIEVE`, `AUDIO_QUOTE`, `AUDIO_COMPLETE` are unused.

**Spec evidence:** `swagger.yaml` defines `/audio/queue`, `/audio/retrieve`, `/audio/quote`, `/audio/complete` (lines 12280-12762) for music/audio generation. Queued audio request/response schemas: `QueueAudioRequest` (lines 4041-4112), `QuoteAudioRequest` (lines 4113-4142), `CompleteAudioRequest` (lines 4143-4158), `RetrieveAudioRequest` (lines 4159-4181).

**Why original was right/wrong:** Correct.

**Correct remediation:** Implement `queue`, `retrieve`, `quote`, and `complete` methods for audio/music with matching request/response models.

**Tests required:** Mock queue/retrieve/quote/complete lifecycle tests.

---

### AUD-02 | P1 | VALID

**Source evidence:** `AudioClient.kt` has no method for `VeniceEndpoints.AUDIO_TRANSCRIPTIONS`.

**Spec evidence:** `swagger.yaml:/paths/audio/transcriptions/post` (lines 11049-11263) accepts `multipart/form-data` with `file`, `model`, `response_format`, `timestamps`, `language` and returns JSON or text/plain.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `transcribe(apiKey, CreateTranscriptionRequest): TranscriptionResponse`; implement multipart file upload.

**Tests required:** Mock transcription JSON and text/plain responses; verify multipart file upload.

---

### AUD-03 | P2 | VALID

**Source evidence:** `AudioModels.kt:7-14` `SpeechRequest` includes `safeMode` and omits `language`, `prompt`, `streaming`, `temperature`, `top_p`.

**Spec evidence:** `swagger.yaml:/components/schemas/CreateSpeechRequestSchema` (lines 3355-3467) includes `language`, `prompt`, `streaming`, `temperature`, `top_p`, and `voice`; it does **not** include `safe_mode`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add missing fields to `SpeechRequest`; remove `safe_mode` from `SpeechRequest`.

**Tests required:** Serialization round-trip for all speech fields; assert `safe_mode` is not emitted.

---

### AUD-04 | P2 | VALID

**Source evidence:** `AudioClient.kt:43` `speech` returns only `ByteArray`; `Content-Type` is discarded.

**Spec evidence:** `swagger.yaml:/paths/audio/speech/post/responses/200/content` (lines 10935-10959) can return `audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, or `audio/wav`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Return a wrapper object containing `bytes` and `contentType`, or add a separate method.

**Tests required:** Mock speech responses with each audio MIME type and assert returned metadata.

---

### AUD-05 | P2 | VALID

**Source evidence:** `AudioClient.kt:26` sets `.header("Accept", "audio/*")`.

**Spec evidence:** `swagger.yaml:/paths/audio/speech/post/responses/200/content` lists specific audio MIME types (`audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, `audio/wav`).

**Why original was right/wrong:** Correct.

**Correct remediation:** Set `Accept` based on `request.responseFormat` mapped to MIME type, or omit and rely on `response_format` body field.

**Tests required:** Verify `Accept` header in mocked request.

---

### X-01 | P2 | VALID

**Source evidence:** `venice-sdk/src/test` contains no `VideoClientTest.kt` or `AudioClientTest.kt`.

**Why original was right/wrong:** Correct.

**Correct remediation:** Add `VideoClientTest.kt` and `AudioClientTest.kt` covering success paths, error paths, and serialization.

**Tests required:** N/A (this finding is about missing tests).

---

### X-02 | P3 | VALID

**Source evidence:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md` lists Video and Audio & Music as "Planned"; `CHANGELOG.md` claims they are implemented.

**Why original was right/wrong:** Correct.

**Correct remediation:** Update `VENICE_API_SOURCE_MANIFEST.md` to reflect implemented endpoints and remaining gaps; update `docs/FEATURE_PARITY_MATRIX.md`.

**Tests required:** N/A.

---

## Key spec quotes (for quick reference)

1. `/image/edit` 200 response is binary only:
   ```yaml
   content:
     image/png: { schema: { format: binary, type: string } }
     image/jpeg: { schema: { format: binary, type: string } }
     image/webp: { schema: { format: binary, type: string } }
   ```
2. `/image/upscale` 200 response is binary only:
   ```yaml
   content:
     image/png: { schema: { format: binary, type: string } }
   ```
3. `/image/background-remove` 200 response is binary only:
   ```yaml
   content:
     image/png: { schema: { format: binary, type: string } }
   ```
4. `/video/queue` 200 response includes optional `download_url`:
   ```yaml
   download_url:
     description: Pre-signed URL to download the completed video. Only present for VPS-backed models.
   ```
5. `/video/retrieve` 200 JSON response contains only status + timing:
   ```yaml
   properties:
     status: { enum: [PROCESSING, COMPLETED] }
     average_execution_time: { type: number }
     execution_duration: { type: number }
   required: [status, average_execution_time, execution_duration]
   ```
6. `/audio/speech` response content types:
   ```yaml
   content:
     audio/aac: ... audio/flac: ... audio/mpeg: ... audio/opus: ... audio/pcm: ... audio/wav: ...
   ```
