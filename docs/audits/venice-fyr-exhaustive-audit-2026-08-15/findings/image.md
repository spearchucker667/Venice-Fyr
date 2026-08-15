# SDK IMAGE Audit Findings

**Auditor scope:** SDK IMAGE (`venice-sdk/.../sdk/image/ImageClient.kt`, `ImageModels.kt`, image tests).  
**Upstream source of truth:** `.source/venice-api-docs/swagger.yaml` (HEAD `6e69346b`, `info.version 20260814.194349`).  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `1da3142`, clean tree.  
**Date:** 2026-08-15.

---

## Ledger: files reviewed

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | Y | 7 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt` | 84 | Y | 7 |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt` | 105 | Y | 1 |
| `.source/venice-api-docs/swagger.yaml` (image schemas + endpoints) | ~1,450 | Y | — |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 122 | Y (related usage) | — |

---

## Executive summary

The SDK image surface is **partially implemented and structurally mismatched** with the official Venice API. `/image/generate` is mostly correct, but `/image/edit`, `/image/upscale`, and `/image/multi-edit` are implemented to return JSON objects while the upstream contract specifies raw binary image responses. Three endpoints (`/image/background-remove`, `/image/styles`, `/images/generations`) have constants but no SDK method. Multipart/form-data uploads are unsupported. The app-level `ImageViewModel` already consumes `edit()` as if it returned base64 JSON, so the edit feature is broken end-to-end.

---

## Findings

### IMG-01 | P1 | CONFIRMED
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

### IMG-02 | P2 | CONFIRMED
**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:20`, `ImageClient.kt:1-91`  
**Symbol:** `IMAGE_BACKGROUND_REMOVE`

**Evidence:**
- `VeniceEndpoints.kt:20` declares `IMAGE_BACKGROUND_REMOVE = "image/background-remove"`.
- `ImageClient.kt` has no method using this constant.
- Swagger `/image/background-remove` (lines 8330–8463) is a fully documented POST endpoint accepting JSON or multipart and returning a PNG binary.

**Expected:** SDK exposes a method for background removal.

**Actual:** Constant exists but no public API method exists.

**Impact:** Consumers cannot use background removal through the SDK.

**Root cause:** Endpoint constant added without corresponding client method.

**Related occurrences:** `VeniceEndpoints.kt:15-16` (`IMAGE_GENERATIONS_COMPAT`, `IMAGE_STYLES`) also lack client methods (see IMG-03, IMG-04).

**Venice reference:** `swagger.yaml:/paths/image/background-remove/post`.

**Remediation:** Add `backgroundRemove(apiKey, BackgroundRemoveImageRequest): ByteArray` method. Support both JSON (`image` base64 / `image_url`) and multipart file upload variants.

**Tests required:** Unit test for background-remove returning PNG bytes; verify request body shape.

---

### IMG-03 | P2 | CONFIRMED
**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:16`  
**Symbol:** `IMAGE_STYLES`

**Evidence:**
- `VeniceEndpoints.kt:16` declares `IMAGE_STYLES = "image/styles"`.
- `ImageClient.kt` has no method for this endpoint.
- Swagger `/image/styles` (lines 7668–7714) is a GET endpoint returning `{ "data": [...], "object": "list" }`.

**Expected:** SDK exposes a method to list available image styles.

**Actual:** No method exists.

**Impact:** Consumers must call the endpoint manually via `VeniceForgeSdk.getRaw`; no typed response model exists.

**Root cause:** Endpoint constant added without corresponding client method or response model.

**Venice reference:** `swagger.yaml:/paths/image/styles/get`.

**Remediation:** Add `styles()` method returning a typed `ImageStylesResponse(data: List<String>, object: String?)`.

**Tests required:** Unit test for `/image/styles` parsing the example response.

---

### IMG-04 | P2 | CONFIRMED
**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:15`  
**Symbol:** `IMAGE_GENERATIONS_COMPAT`

**Evidence:**
- `VeniceEndpoints.kt:15` declares `IMAGE_GENERATIONS_COMPAT = "images/generations"`.
- `ImageClient.kt` has no method for this endpoint.
- Swagger `/images/generations` (lines 7455–7667) is the OpenAI-compatible image generation endpoint returning `{ created, data: [{ b64_json | url }] }`.

**Expected:** SDK exposes an OpenAI-compatible image generation method.

**Actual:** No method exists.

**Impact:** Consumers targeting OpenAI-compatible tooling cannot use this endpoint through the SDK.

**Root cause:** Endpoint constant added without corresponding client method or response model.

**Venice reference:** `swagger.yaml:/paths/images/generations/post`.

**Remediation:** Add `simpleGenerate(apiKey, SimpleGenerateImageRequest): SimpleGenerateImageResponse` with the OpenAI-shaped response.

**Tests required:** Unit test for `/images/generations` request/response serialization.

---

### IMG-05 | P2 | CONFIRMED
**Area:** Request encoding / File upload  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 36–44, 65–74  
**Symbol:** `executeRequest`, `executeBinaryRequest`

**Evidence:**
- Both request paths in `ImageClient.kt` set `Content-Type: application/json` via `reqBody.toRequestBody(jsonMedia)` (`ImageClient.kt:44`, `ImageClient.kt:73`).
- Swagger `/image/upscale` (lines 7729–7736) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/edit` (lines 7867–7898) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/multi-edit` (lines 8122–8155) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/background-remove` (lines 8345–8352) accepts both `application/json` and `multipart/form-data`.

**Expected:** SDK supports multipart/form-data file uploads for endpoints that accept them.

**Actual:** SDK only sends JSON. Callers must base64-encode files, increasing payload size and CPU cost.

**Impact:** File upload workflows are unsupported; large images may hit memory/performance limits on Android.

**Root cause:** Single JSON-only request path; no multipart builder.

**Venice reference:** `swagger.yaml:/paths/*/post/requestBody/content/multipart/form-data` for image endpoints.

**Android/Kotlin reference:** OkHttp `MultipartBody.Builder` is the standard API for multipart uploads.

**Remediation:** Add multipart upload variants for `upscale`, `edit`, `multiEdit`, and `backgroundRemove` that accept `okhttp3.RequestBody` or file paths/URIs.

**Tests required:** Unit tests verifying multipart body parts and `Content-Type: multipart/form-data` boundary.

---

### IMG-06 | P2 | CONFIRMED
**Area:** Response model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 79–84  
**Symbol:** `GenerateImageResponse`

**Evidence:**
- `ImageModels.kt:79-84` defines `GenerateImageResponse(id, images, timing)` with all fields nullable.
- Swagger `/image/generate` 200 response schema (lines 7319–7356) defines `id`, `images`, and `timing` as `required`; it also defines a `request` field (nullable object) that echoes the original request.

**Expected:** `GenerateImageResponse` should match the swagger response shape: required `id`, `images`, `timing`; optional `request`.

**Actual:** All fields are nullable and `request` is missing.

**Impact:** Callers cannot access the echoed request, and nullability is wider than the contract guarantees. The SDK silently drops `request` data.

**Root cause:** Response model not updated to match current swagger.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/content/application/json/schema`.

**Remediation:** Add `request: JsonElement?` (or a typed request echo) to `GenerateImageResponse`; make `id`, `images`, `timing` non-nullable.

**Tests required:** Deserialize a generate response containing `request`; assert `request` is accessible.

**Compatibility impact:** Making fields non-nullable is a breaking change for Kotlin callers; consider deprecation path.

---

### IMG-07 | P2 | CONFIRMED
**Area:** Request model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 13–35  
**Symbol:** `GenerateImageRequest`

**Evidence:**
- `ImageModels.kt:13-35` includes `model`, `prompt`, `negativePrompt`, `stylePreset`, `height`, `width`, `steps`, `cfgScale`, `seed`, `safeMode`, `returnBinary`, `hideWatermark`, `format`, `variants`, `aspectRatio`, `resolution`, `quality`, `enableWebSearch`, `disablePromptOptimizationThinking`, `enhancePrompt`, `styleReferences`.
- Swagger `GenerateImageRequest` (lines 2583–2777) additionally defines:
  - `embed_exif_metadata` (boolean, default false)
  - `inpaint` (deprecated, nullable)
  - `lora_strength` (integer, 0–100)

**Expected:** SDK request model exposes all non-deprecated swagger fields.

**Actual:** `embed_exif_metadata` and `lora_strength` are missing; `inpaint` is also missing (deprecated but still in schema).

**Impact:** Callers cannot control EXIF embedding or LoRA strength for supported models.

**Root cause:** Request model not kept in sync with swagger.

**Venice reference:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/embed_exif_metadata`, `/components/schemas/GenerateImageRequest/properties/lora_strength`.

**Remediation:** Add `embedExifMetadata: Boolean?`, `loraStrength: Int?`, and optionally `inpaint` (marked deprecated) to `GenerateImageRequest`.

**Tests required:** Serialization test verifying new fields map to correct wire names.

---

### IMG-08 | P2 | CONFIRMED
**Area:** Request model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 45–55  
**Symbol:** `EditImageRequest`

**Evidence:**
- `ImageModels.kt:45-55` defines `EditImageRequest(image, prompt, model, aspectRatio, resolution, outputFormat, disablePromptOptimizationThinking, enhancePrompt, safeMode)`.
- Swagger `EditImageRequest` (lines 2933–3027) includes `modelId` (deprecated) in addition to `model`.

**Expected:** SDK request model includes deprecated `modelId` for backwards compatibility.

**Actual:** `modelId` is missing.

**Impact:** Existing code or docs referencing `modelId` cannot be used with the SDK.

**Root cause:** Request model not kept in sync with swagger.

**Venice reference:** `swagger.yaml:/components/schemas/EditImageRequest/properties/modelId`.

**Remediation:** Add `@SerialName("modelId") val modelId: String? = null` to `EditImageRequest`.

**Tests required:** Serialization test verifying `modelId` is sent when provided.

---

### IMG-09 | P2 | CONFIRMED
**Area:** Error handling / Paid operations  
**Module:** `:venice-sdk`  
**File:** `VeniceForgeSdk.kt`  
**Lines:** 166–200  
**Symbol:** `parseHttpError`

**Evidence:**
- `VeniceForgeSdk.kt:166-200` maps 401/403 to `Authentication`, 400/422 to `Validation`, 500–599 to `Server`, 429 to `RateLimit`, and everything else to generic `Http`.
- Swagger image endpoints return `402` for insufficient balance / x402 payment required and `415` for invalid content-type.
- `415` responses use `StandardError`; `402` responses may be `StandardError` or `X402InferencePaymentRequired`.

**Expected:** SDK surfaces `402 Payment Required` as a distinct exception type so callers can detect insufficient balance.

**Actual:** `402` falls into the generic `Http` exception; callers cannot easily distinguish payment failures from other errors.

**Impact:** Paid/mutating image operations cannot gracefully guide users to top-up; AGENTS.md requires explicit approval for paid operations, but the SDK does not expose the signal.

**Root cause:** Exception mapping does not account for image-endpoint-specific 402 semantics.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/402`, `/paths/image/edit/post/responses/402`, etc.

**Remediation:** Add `VeniceSdkException.PaymentRequired` (or extend `Http` with a dedicated subtype) for HTTP 402, parsing `X402InferencePaymentRequired` fields.

**Tests required:** Unit tests for 402 responses on image endpoints.

---

### IMG-10 | P2 | CONFIRMED
**Area:** Response metadata / Information loss  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 36–63  
**Symbol:** `executeRequest`

**Evidence:**
- `ImageClient.kt:36-63` reads only the response body string; no headers are captured.
- Swagger `/image/generate` 200 response defines headers: `x-venice-is-blurred`, `x-venice-is-content-violation`, `x-venice-model-deprecation-warning`, `x-venice-model-deprecation-date`, `x-venice-deprecated`, `x-venice-deprecated-replacement`, `X-Balance-Remaining`.
- Swagger `/image/edit` and `/image/multi-edit` define `x-venice-is-content-violation`, `x-venice-model-id`, `x-venice-model-name`, and deprecation headers.

**Expected:** SDK exposes relevant Venice response headers to callers.

**Actual:** All Venice-specific headers are discarded.

**Impact:** Callers cannot detect blurred images, content violations, model deprecation, or remaining x402 balance. This violates the principle of preserving upstream semantics.

**Root cause:** `executeRequest` only returns the deserialized body, not header metadata.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/headers`, `/paths/image/edit/post/responses/200/headers`.

**Remediation:** Return a wrapper object (e.g., `ImageGenerationResult`) containing `body: GenerateImageResponse` and `headers: ImageResponseHeaders`, or expose headers via a callback.

**Compatibility impact:** Breaking API change; can be offered as an additional method while deprecating the old one.

---

### IMG-11 | P2 | CONFIRMED
**Area:** API footgun / Binary vs JSON  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 19–25  
**Symbol:** `generate`, `generateBinary`

**Evidence:**
- `ImageClient.kt:19-20` `generate()` always sends `Accept: application/json` and parses JSON.
- `ImageClient.kt:22-25` `generateBinary()` requires `returnBinary == true` and sends `Accept: image/*`.
- Swagger `/image/generate` returns binary when `return_binary=true`.

**Expected:** If a caller accidentally calls `generate()` with `returnBinary=true`, the SDK should either reject it or route to binary handling.

**Actual:** `generate()` does not validate `returnBinary`; it will request JSON and then fail to parse binary image bytes.

**Impact:** Easy-to-make caller error results in a confusing `Protocol` exception instead of a clear validation error or correct binary response.

**Root cause:** Two separate methods without guardrails; `generate()` is not binary-safe.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/content/image/*`.

**Remediation:** Either merge `generate`/`generateBinary` into a single method that inspects `returnBinary`, or add an explicit check in `generate()` throwing `IllegalArgumentException` when `returnBinary == true`.

**Tests required:** Unit test asserting `generate()` with `returnBinary=true` fails fast with a clear exception.

---

### IMG-12 | P3 | CONFIRMED
**Area:** Request validation / Model constraints  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 13–35  
**Symbol:** `GenerateImageRequest`

**Evidence:**
- Swagger `GenerateImageRequest` defines `height`/`width` constraints: `minimum: 0`, `exclusiveMinimum: true`, `maximum: 1280` (lines 2609–2615, 2734–2740).
- Swagger defines `cfg_scale` `minimum: 0`, `exclusiveMinimum: true`, `maximum: 20` (lines 2586–2592).
- SDK does not validate these constraints before sending.

**Expected:** SDK optionally validates numeric constraints client-side to fail fast.

**Actual:** Invalid values are sent to the server, resulting in 400 errors.

**Impact:** Minor UX/cost issue; every invalid request consumes a network round-trip.

**Root cause:** No validation layer in request models.

**Venice reference:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/height`.

**Remediation:** Add lightweight validation in `GenerateImageRequest` init block or in `ImageClient.generate()`.

**Tests required:** Unit tests for out-of-range width/height/cfg_scale.

---

### IMG-13 | P3 | CONFIRMED
**Area:** Test coverage  
**Module:** `:venice-sdk`  
**File:** `ImageClientTest.kt`  
**Lines:** 1–105  
**Symbol:** `ImageClientTest`

**Evidence:**
- `ImageClientTest.kt` contains only two tests: `generate maps request and response correctly` (lines 38–76) and `edit maps request correctly` (lines 78–104).
- No tests for `upscale`, `multiEdit`, `generateBinary`, error responses, `safe_mode` serialization, multipart, or response headers.

**Expected:** Each public image method and major response path has unit coverage.

**Actual:** Coverage is minimal; several methods have no tests.

**Impact:** Defects like IMG-01 (binary response mismatch) were not caught by tests.

**Root cause:** Test suite not expanded alongside SDK surface.

**Venice reference:** N/A.

**Remediation:** Add tests for all public methods, error paths, binary responses, and serialization edge cases.

**Tests required:** See remediation items for IMG-01 through IMG-12.

---

## Non-findings (verified correct)

### safe_mode=false preservation
**Status:** CONFIRMED correct.  
`GenerateImageRequest.safeMode: Boolean? = null` (`ImageModels.kt:23`) uses a nullable Boolean with default `null`. With `Json { encodeDefaults = false }` (`ImageClient.kt:16`), kotlinx.serialization omits only values equal to the declared default (`null`). An explicit `false` value is not equal to `null`, so it is serialized as `"safe_mode":false`. The same logic applies to `EditImageRequest.safeMode` and `MultiEditImageRequest.safeMode`. This satisfies AGENTS.md "Preserve explicit safe_mode=false when selected."

### `/image/generate` JSON path
**Status:** CONFIRMED correct.  
`generate()` sends `Accept: application/json` and parses `GenerateImageResponse`, matching swagger `/image/generate` when `return_binary=false`.

### `/image/generate` binary path
**Status:** CONFIRMED correct.  
`generateBinary()` sends `Accept: image/*` and returns `ByteArray`, matching swagger when `return_binary=true`.

---

## End of findings
