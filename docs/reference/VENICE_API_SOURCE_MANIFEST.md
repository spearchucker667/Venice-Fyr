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
| **Model Traits (`/models/traits`)** | `swagger.yaml` (`GET /models/traits`), `agents.md` | `venice-sdk/.../capabilities/CapabilitiesRepository.kt` | `CapabilitiesRepositoryTest.kt` | **VERIFIED**: Maps symbolic traits (`default`, `fastest`, `uncensored`) to model IDs |
| **Model Compatibility (`/models/compatibility_mapping`)** | `swagger.yaml` (`GET /models/compatibility_mapping`) | `venice-sdk/.../capabilities/CapabilitiesRepository.kt` | `CapabilitiesRepositoryTest.kt` | **VERIFIED**: Maps provider aliases (`gpt-4o`, `claude-3-5-sonnet`) to Venice models |
| **Chat Completions (`/chat/completions`)** | `swagger.yaml` (`POST /chat/completions`), `guides/features/` | `venice-sdk/.../chat/ChatClient.kt`, `ChatRequest.kt` | `ChatClientTest.kt`, `ChatViewModelTest.kt` | **VERIFIED**: SSE streaming, multi-turn context, multiple tool-call deltas, single terminal event |
| **Venice Parameters** | `swagger.yaml` (`ChatCompletionRequest.venice_parameters`) | `venice-sdk/.../chat/ChatRequest.kt` (`VeniceParameters`) | `VeniceParametersSerializationTest.kt` | **VERIFIED**: `enable_web_search`, `include_venice_system_prompt`, `safe_mode` (preserves explicit false) |
| **Errors & Rate Limits** | `api-reference/error-codes.mdx`, `api-reference/rate-limiting.mdx` | `venice-sdk/.../VeniceSdkException.kt`, `VeniceForgeSdk.kt` | `VeniceSdkExceptionTest.kt` | **VERIFIED**: Structured `RateLimit`, `Authentication`, `Validation`, `Server`, `Http` exceptions with rate limit headers |
| **Images (`/image/*`)** | `swagger.yaml` (`POST /image/generate`, `/edit`, `/multi-edit`, `/upscale`, `/background-remove`) | Planned `:venice-sdk` image service | Pending Milestone 2 | Planned (Desktop Parity Scaffolding) |
| **Video (`/video/*`)** | `swagger.yaml` (`POST /video/queue`, `GET /retrieve`, `POST /quote`, `POST /complete`) | Planned `:venice-sdk` video service | Pending Milestone 3 | Planned (Queue State Machine Contract) |
| **Audio & Music (`/audio/*`)** | `swagger.yaml` (`POST /audio/speech`, `/transcriptions`, `/queue`, `/quote`, `/retrieve`) | Planned `:venice-sdk` audio service | Pending Milestone 3 | Planned (TTS, STT, Music Queues) |
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
   - Upstream defines `type` as optional with values `["all", "text", "image", "video", "audio", "tts", "asr", "embedding", "music", "upscale", "inpaint", "code"]`.
   - Local implementation omits the query parameter when `type == null` rather than sending a fabricated default.
3. **/models/traits shape**:
   - Returns a key-value dictionary `{"data": {"default": "model-id", ...}}`.
   - Previous local assumption of per-model capability array was corrected to match the upstream dictionary schema. Model capabilities are derived from `model_spec` on `GET /models`.
