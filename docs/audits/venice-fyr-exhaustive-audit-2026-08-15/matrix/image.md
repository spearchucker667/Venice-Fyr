# SDK IMAGE Audit Matrix

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
