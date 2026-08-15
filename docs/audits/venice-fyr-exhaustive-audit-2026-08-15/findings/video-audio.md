# SDK Video + Audio Audit Findings

**Scope:** `:venice-sdk` video (`sdk/video/`) and audio (`sdk/audio/`) clients and models, plus related tests.  
**Upstream authority:** `.source/venice-api-docs/swagger.yaml` (upstream HEAD `6e69346b`, schema version `20260814.194349`).  
**Local baseline:** branch `main` @ `1da3142`, clean tree.  
**Auditor:** SDK Video + Audio auditor.  
**Date:** 2026-08-15.

---

## File Ledger

| Path | Lines | Reviewed | Findings | Notes |
|------|-------|------------|----------|-------|
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt` | 119 | Y | 5 | Core video transport implementation. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt` | 54 | Y | 3 | Video request/response DTOs and result sealed class. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt` | 46 | Y | 4 | Core audio transport implementation. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt` | 14 | Y | 1 | Audio request DTOs. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | Y | 0 | Endpoint constants only; verified against swagger. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | Y | 0 | Reviewed for SDK context/factory methods and error parsing. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 105 | Y | 0 | Reviewed for exception contract used by clients. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | Y | 0 | Confirmed HTTPS base URL requirement. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | Y | 0 | Pattern reference for binary media clients. |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt` | 17 | Y | 0 | Only verifies endpoint string constants. |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 161 | Y | 0 | No video/audio coverage. |
| `docs/reference/VENICE_API_SOURCE_MANIFEST.md` | 57 | Y | 1 | Documents video/audio as "Planned" despite implementation. |
| `CHANGELOG.md` | — | Y | 1 | Claims audio/video clients shipped. |

**Total production files in scope:** 4 (`VideoClient.kt`, `VideoModels.kt`, `AudioClient.kt`, `AudioModels.kt`).  
**Total test files in scope:** 0 (no video/audio unit tests exist).

---

## Executive Summary

The `:venice-sdk` video and audio surface is **incomplete and partially incorrect** against the authoritative `swagger.yaml`.

* **Video:** `VideoClient` exposes `/video/queue`, `/video/retrieve`, and `/video/complete`, but omits `/video/quote` and `/video/transcriptions`. The `QueueVideoRequest` model is missing most swagger fields and marks required fields as optional, so the SDK can emit requests that fail validation before reaching the server. The retrieve result model cannot represent a completed VPS-backed job (which returns JSON status + `download_url` only), and there is no polling helper or status enum.
* **Audio:** `AudioClient` only exposes `/audio/speech`. The entire queued-audio/music surface (`/audio/queue`, `/audio/retrieve`, `/audio/quote`, `/audio/complete`) and `/audio/transcriptions` are absent. `SpeechRequest` omits several documented optional parameters.
* **Testing:** No unit tests exist for either client, so the gaps are not caught by CI.
* **Documentation drift:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md` still lists video/audio as "Planned," while `CHANGELOG.md` claims they are implemented.

---

## Detailed Findings

### VID-01 | P1 | CONFIRMED
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

### VID-02 | P1 | CONFIRMED
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

### VID-03 | P1 | CONFIRMED
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

### VID-04 | P1 | CONFIRMED
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

### VID-05 | P2 | CONFIRMED
**Area:** Video retrieve status not typed  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt`  
**Lines:** 36, 49  
**Symbol:** `status`

**Evidence:**
```kotlin
data class RetrieveVideoResponseStatus(
    val status: String,
    ...
)
```

**Spec:** `swagger.yaml` `/video/retrieve` JSON response (line 11865) defines `status` enum: `PROCESSING`, `COMPLETED`.

**Expected:** Strongly-typed enum for video retrieve status.

**Actual:** Raw `String`.

**Impact:** Reliability/architecture problem. Callers can pass or receive invalid statuses; unknown statuses are not handled explicitly.

**Root cause:** Schema enum not modeled.

**Related occurrences:** `VideoModels.kt:49` (`Processing.status`).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11865–11870.

**Android/Kotlin reference:** N/A.

**Remediation:** Introduce `VideoStatus` enum (`PROCESSING`, `COMPLETED`) and use it in response/result models.

**Tests required:** Serialization round-trip tests for each enum value.

**Compatibility impact:** Source-incompatible for callers reading `status` as `String`.

---

### VID-06 | P2 | CONFIRMED
**Area:** Video complete response ignored  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 22–25  
**Symbol:** `complete`

**Evidence:**
```kotlin
suspend fun complete(apiKey: String, request: CompleteVideoRequest) {
    // We just execute and ignore the response body if successful
    executeRawRequest(apiKey, VeniceEndpoints.VIDEO_COMPLETE, json.encodeToString(CompleteVideoRequest.serializer(), request))
}
```

**Spec:** `swagger.yaml` `/video/complete` 200 response (lines 11484–11504) returns `{ "success": boolean }` and documents: "A success value of false indicates cleanup did not complete and can be retried later."

**Expected:** SDK parses and returns the `success` flag so callers know whether cleanup succeeded.

**Actual:** SDK discards the body and returns `Unit`. A `success: false` response is treated as success.

**Impact:** Reliability problem. Failed cleanup goes unnoticed; media may remain in storage and continue to incur cost.

**Root cause:** `executeRawRequest` intentionally ignores successful response bodies.

**Related occurrences:** Same pattern would affect `/audio/complete` if implemented.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11484–11504.

**Android/Kotlin reference:** N/A.

**Remediation:** Parse the JSON body and return `Boolean` (or a typed response) from `complete`.

**Tests required:** Mock complete responses with `success: true` and `success: false`.

**Compatibility impact:** Return-type change from `Unit` to `Boolean`; source-incompatible.

---

### VID-07 | P2 | CONFIRMED
**Area:** No queued-job polling helper  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 15–118  
**Symbol:** `VideoClient`

**Evidence:** `VideoClient` exposes only single-shot `retrieve`. There is no method to poll until completion, no timeout, no interval, and no cancellation hook beyond the coroutine.

**Spec:** `swagger.yaml` describes `/video/queue` returning a `queue_id` and `/video/retrieve` returning `PROCESSING`/`COMPLETED` status. The project's `docs/API_INTEGRATION_GUIDE.md` line 47 says: "If queued (e.g. video/music), implement polling state machines."

**Expected:** SDK provides a polling helper (e.g., `pollUntilCompleted`) with configurable max attempts, interval, and timeout, and emits status updates.

**Actual:** No polling helper exists. Each caller must implement its own loop.

**Impact:** Reliability/architecture problem. Inconsistent polling behavior across apps; risk of infinite loops, excessive API calls, and poor UX.

**Root cause:** Queued-job lifecycle abstraction not built.

**Related occurrences:** Same gap applies to audio/music queue if/when implemented.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` `/video/queue`, `/video/retrieve`; `docs/API_INTEGRATION_GUIDE.md` line 47.

**Android/Kotlin reference:** `kotlinx.coroutines` `delay`, `withTimeout`, `Job.isActive`.

**Remediation:** Add a `pollForResult` suspend function with bounded retries, exponential back-off, and cooperative cancellation checks.

**Tests required:** Mock polling sequence (PROCESSING → COMPLETED), timeout path, cancellation path.

**Compatibility impact:** New API surface; additive.

---

### AUD-01 | P1 | CONFIRMED
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

### AUD-02 | P1 | CONFIRMED
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

### AUD-03 | P2 | CONFIRMED
**Area:** Speech request model incomplete  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt`  
**Lines:** 7–14  
**Symbol:** `SpeechRequest`

**Evidence:**
```kotlin
@Serializable
data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String? = null,
    @SerialName("response_format") val responseFormat: String? = null,
    val speed: Float? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null
)
```

**Spec:** `swagger.yaml` `CreateSpeechRequestSchema` (lines 3355–3467) includes `input`, `language`, `model`, `prompt`, `response_format`, `speed`, `streaming`, `temperature`, `top_p`, `voice`.

**Expected:** SDK model supports `language`, `prompt`, `streaming`, `temperature`, `top_p`.

**Actual:** These fields are missing. Callers cannot request streaming TTS, style prompts, language hints, or sampling controls.

**Impact:** Incorrect Venice integration. Callers cannot use documented TTS features; `safe_mode` is also present in the SDK model but is **not** in the swagger schema for `/audio/speech`.

**Root cause:** Model was written from an incomplete subset of the schema.

**Related occurrences:** `AudioModels.kt:7–14` only.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 3355–3467 (`CreateSpeechRequestSchema`).

**Android/Kotlin reference:** N/A.

**Remediation:** Add missing fields; remove `safe_mode` from `SpeechRequest` unless it is confirmed to be accepted by the server (it is not in swagger).

**Tests required:** Serialization round-trip for all speech fields.

**Compatibility impact:** Removing `safe_mode` is source-incompatible; adding optional fields is additive.

---

### AUD-04 | P2 | CONFIRMED
**Area:** Speech response content type not exposed  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 19–45  
**Symbol:** `speech`

**Evidence:**
```kotlin
suspend fun speech(apiKey: String, request: SpeechRequest): ByteArray = withContext(Dispatchers.IO) {
    ...
    res.body?.bytes() ?: throw VeniceSdkException.Protocol("Empty binary response from /audio/speech", null)
}
```

**Spec:** `swagger.yaml` `/audio/speech` 200 response (lines 10935–10959) can return `audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, or `audio/wav`.

**Expected:** Caller knows which format was returned (via `Content-Type` header or a wrapper type).

**Actual:** Only `ByteArray` is returned; the `Content-Type` is discarded.

**Impact:** UX/reliability problem. The caller must guess or hard-code the format, which may not match the actual bytes.

**Root cause:** Binary response API returns raw bytes without metadata.

**Related occurrences:** `ImageClient.executeBinaryRequest` has the same limitation (not in scope).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 10935–10959.

**Android/Kotlin reference:** N/A.

**Remediation:** Return a wrapper object containing `bytes` and `contentType`, or add a separate method.

**Tests required:** Mock speech responses with each audio MIME type and assert returned metadata.

**Compatibility impact:** Return-type change; source-incompatible.

---

### AUD-05 | P2 | CONFIRMED
**Area:** Audio speech Accept header too broad  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Line:** 26  
**Symbol:** `Accept`

**Evidence:**
```kotlin
.header("Accept", "audio/*")
```

**Spec:** `swagger.yaml` `/audio/speech` lists specific audio MIME types (`audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, `audio/wav`).

**Expected:** SDK sends an `Accept` header that reflects the requested `response_format` or the model's supported formats.

**Actual:** Wildcard `audio/*` is sent.

**Impact:** Reliability/UX problem. Server may return a default format the caller did not request; caller cannot distinguish format.

**Root cause:** Simplified header.

**Related occurrences:** `AudioClient.kt:26` only.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 10935–10959.

**Android/Kotlin reference:** N/A.

**Remediation:** Set `Accept` based on `request.responseFormat` (mapped to MIME type), or omit and rely on `response_format` body field.

**Tests required:** Verify `Accept` header in mocked request.

**Compatibility impact:** None.

---

### X-01 | P2 | CONFIRMED
**Area:** Missing unit tests for video and audio clients  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/...`  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:** `venice-sdk/src/test` contains `ImageClientTest.kt`, `ChatClientTest.kt`, etc., but no `VideoClientTest.kt` or `AudioClientTest.kt`.

**Expected:** Each public SDK client has unit tests covering success paths, error paths, and serialization.

**Actual:** No tests exist for video or audio clients.

**Impact:** Architecture/test problem. The gaps documented in this audit are not caught by CI.

**Root cause:** Tests were not written.

**Related occurrences:** Entire `sdk/video/` and `sdk/audio/` packages.

**Venice reference:** N/A.

**Android/Kotlin reference:** JUnit4 / `kotlinx.coroutines.test.runTest`.

**Remediation:** Add `VideoClientTest.kt` and `AudioClientTest.kt` with `MockWebServer` or interceptor-based mocks.

**Tests required:** N/A (this finding is about missing tests).

**Compatibility impact:** None.

---

### X-02 | P3 | CONFIRMED
**Area:** Documentation drift between manifest and changelog  
**Module:** docs  
**File:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md`, `CHANGELOG.md`  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:**
* `docs/reference/VENICE_API_SOURCE_MANIFEST.md` lines 37–38 list Video and Audio & Music as "Planned" with status "Pending Milestone 3".
* `CHANGELOG.md` line 19 claims: ":venice-sdk Audio client (`AudioClient`) with `/audio/speech` direct binary stream support" and line 20 claims ":venice-sdk Video client (`VideoClient`) with `/video/queue`, `/video/retrieve`, `/video/complete`, and dynamic Content-Type stream/status discriminator."

**Expected:** Source manifest reflects implemented clients and remaining gaps.

**Actual:** Manifest contradicts changelog.

**Impact:** Docs/minor problem. Misleading project status; other agents may plan around outdated information.

**Root cause:** Manifest not updated after implementation.

**Related occurrences:** N/A.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Update `VENICE_API_SOURCE_MANIFEST.md` to show implemented endpoints and missing endpoints; update parity matrix.

**Tests required:** N/A.

**Compatibility impact:** None.

---

## Cross-Cutting Observations

1. **No x402 / SIWX auth path.** All clients send `Authorization: Bearer $apiKey`. The swagger endpoints also accept `siwx` security, but the SDK does not expose an x402 credential provider. This is consistent with the rest of the SDK (chat, image) and is noted here for completeness, not as a unique defect.
2. **Duplicate `Json` instances.** `VideoClient` and `AudioClient` each create a private `Json { ignoreUnknownKeys = true; encodeDefaults = false }`. This matches `ImageClient` and is stylistically consistent, but a shared SDK serializer would reduce duplication.
3. **No client-side media-size validation.** The SDK does not validate prompt length, file size, or reference URL count before sending. Server-side validation handles this, but earlier failure would improve UX.
4. **AGENTS.md safe_mode rule.** The project rule says "Preserve explicit `safe_mode=false` when selected." `QueueVideoRequest.safeMode` is nullable, so explicit `false` is preserved in serialization (`encodeDefaults = false` means the field is omitted when `null`, but `false` is encoded). This is correct for video. However, `SpeechRequest.safeMode` should not exist per swagger.

---

## Severity Summary

| Severity | Count | Finding IDs |
|----------|-------|-------------|
| P0 | 0 | — |
| P1 | 6 | VID-01, VID-02, VID-03, VID-04, AUD-01, AUD-02 |
| P2 | 7 | VID-05, VID-06, VID-07, AUD-03, AUD-04, AUD-05, X-01 |
| P3 | 1 | X-02 |

**Total findings:** 14.
