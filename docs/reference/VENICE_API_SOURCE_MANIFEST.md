# Venice API Source Manifest

## Upstream API Documentation Baseline

```text
Canonical Upstream:    https://github.com/veniceai/api-docs.git
Local Source Mirror:   .source/venice-api-docs/
Upstream HEAD:         6e69346b13695bd53ba33a1d34e7b28841e10f98
OpenAPI Schema Version: 20260814.194349
Refreshed Date:        2026-08-15
State Environment:     .local/venice-api-docs.env
```

---

## Source Precedence and Authority

1. **`$VENICE_API_DOCS_SOURCE/swagger.yaml`**: Primary authority on endpoint routes, HTTP verbs, query parameters, request/response headers, JSON schemas, required fields, and enums.
2. **`$VENICE_API_DOCS_SOURCE/` Guides & Reference**: English documentation in `api-reference/`, `guides/`, `overview/`, `agents.md`, and `skill.md` for runtime workflows, rate limits, error codes, and streaming protocols.
3. **Live Venice Runtime Discovery**: Dynamic metadata returned from `/models`, `/models/traits`, and `/models/compatibility_mapping`.
4. **Tested `:venice-sdk` Implementations**: Local Kotlin SDK code, validated against authoritative fixtures.
5. **Venice Forge Desktop Source**: For desktop product UX and parity contracts (see [`DESKTOP_SOURCE_BOOTSTRAP.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/DESKTOP_SOURCE_BOOTSTRAP.md)).

---

## API Surface Mapping

| Surface | Official Upstream Source | Local Module / Path | Verification & Test Coverage | Parity & Implementation Status |
|---|---|---|---|---|
| **Models (`/models`)** | `swagger.yaml` (`GET /models`), `agents.md` | `venice-sdk/.../VeniceForgeSdk.kt`, `ModelType.kt`, `VeniceModel.kt` | `VeniceForgeSdkTest.kt`, `ModelCatalogTest.kt` | **VERIFIED**: Runtime discovery, `ModelSpec` parsing, no static allowlists |
| **Model Traits (`/models/traits`)** | `swagger.yaml` (`GET /models/traits`), `agents.md` | `venice-sdk/.../capabilities/CapabilitiesRepository.kt` | `CapabilitiesRepositoryTest.kt` | **VERIFIED**: propagates discovered modality `type`; non-text generic keys are namespaced |
| **Model Compatibility (`/models/compatibility_mapping`)** | `swagger.yaml` (`GET /models/compatibility_mapping`) | `venice-sdk/.../capabilities/CapabilitiesRepository.kt` | `CapabilitiesRepositoryTest.kt` | **VERIFIED**: propagates discovered modality and namespaces non-text aliases |
| **Chat Completions (`/chat/completions`)** | `swagger.yaml` (`POST /chat/completions`), `guides/features/reasoning-models.mdx` | `venice-sdk/.../chat/` | `ChatClientTest.kt`, `SseLineParserTest.kt`, `VeniceParametersSerializationTest.kt` | **FOUNDATION**: cancellation-native SSE, strict terminal framing, structured errors, tool deltas, typed reasoning controls, and separate `reasoning_content`; non-streaming and multimodal content remain open |
| **Venice Parameters** | `swagger.yaml` (`ChatCompletionRequest.venice_parameters`) | `ChatRequest.kt` (`VeniceParameters`) | `VeniceParametersSerializationTest.kt` | **VERIFIED SUBSET**: documented search/system/thinking fields only; image-only `safe_mode` and unimplemented E2EE claims are not exposed |
| **Errors & Rate Limits** | `swagger.yaml`, `api-reference/error-codes.mdx` | `VeniceSdkException.kt`, `VeniceForgeSdk.kt` | `VeniceForgeSdkTest.kt`, media client tests | **VERIFIED FOUNDATION**: typed 400/401/402/403/422/429/5xx classes and request/rate-limit metadata |
| **Images (`/image/*`)** | `swagger.yaml` image paths | `image/ImageClient.kt`, `ImageModels.kt` | `ImageClientTest.kt` | **PARTIAL**: generate plus binary edit/multi-edit/upscale; background removal, styles, compatibility generation, and multipart remain open |
| **Video (`/video/*`)** | `swagger.yaml` video paths | `video/VideoClient.kt`, `VideoModels.kt` | `VideoClientTest.kt` | **SDK FOUNDATION**: queue, quote, retrieve, complete, transcription; JSON `COMPLETED` is distinct from binary completion and queue `download_url` stays queue-owned |
| **Audio & Music (`/audio/*`)** | `swagger.yaml` audio paths | `audio/AudioClient.kt`, `AudioModels.kt` | `AudioClientTest.kt` | **SDK FOUNDATION**: speech, quote, queue, retrieve, complete, multipart transcription, and voice cloning; durable polling/UI remain open |
| **Embeddings (`/embeddings`)** | `swagger.yaml` (`POST /embeddings`) | Planned `:venice-sdk` embedding service | Pending Milestone 4 | Planned |
| **Augment (`/augment/*`)** | `swagger.yaml` (`POST /augment/search`, `/scrape`, `/text-parser`) | Planned `:venice-sdk` augment service | Pending Milestone 4 | Planned (Web search, scraping, parsing) |
| **Characters (`/characters`)** | `swagger.yaml` (`GET /characters`, `GET /characters/{slug}`) | Planned `:venice-sdk` character service | Pending Milestone 5 | Planned |
| **Billing (`/billing/*`)** | `swagger.yaml` (`GET /billing/balance`, `/billing/usage-history`) | Planned `:venice-sdk` billing service | Pending Milestone 5 | Planned (`/billing/usage` marked deprecated upstream) |
| **x402 Wallet (`/x402/*`)** | `swagger.yaml` (`GET /x402/balance`, `POST /x402/top-up`) | Planned `:venice-sdk` x402 service | Pending Milestone 5 | Planned |

---

## Known Upstream Divergence & Deprecations

1. **/billing/usage vs /billing/usage-history**:
   - Upstream marks `GET /billing/usage` as deprecated in favor of `GET /billing/usage-history`.
   - Local implementation rule: Any new billing instrumentation must target `/billing/usage-history`.
2. **/models?type query parameter**:
   - Upstream defines `type` as optional with values `["asr", "embedding", "image", "music", "text", "tts", "upscale", "inpaint", "video", "all", "code"]`.
   - Local implementation omits the query parameter when `type == null` rather than sending a fabricated default.
3. **/models/traits shape**:
   - Returns a key-value dictionary `{"data": {"default": "model-id", ...}}`.
   - Previous local assumption of per-model capability array was corrected to match the upstream dictionary schema. Model capabilities are derived from `model_spec` on `GET /models`.
