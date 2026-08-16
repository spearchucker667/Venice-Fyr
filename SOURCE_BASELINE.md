# Source Baseline

- Generated: 2026-08-15
- Desktop product: Venice Forge `3.0.0-beta.2`
- Desktop remote: `spearchucker667/Venice_Forge` / `main`
- Latest desktop remote commit observed: `bc5c17374ef4937f5837f5580d29a88bfab333ee`
- Venice API official repository: `https://github.com/veniceai/api-docs.git` / `main`
- Venice API upstream commit: `6e69346b13695bd53ba33a1d34e7b28841e10f98`
- Venice OpenAPI schema version: `20260814.194349`
- Canonical Android feature IDs: 22, from desktop `src/config/tabs.ts`

## Android Toolchain Selected

- AGP 9.3.0
- Gradle 9.5.0
- Kotlin / Compose compiler plugin 2.3.21
- compileSdk / targetSdk 37
- minSdk 26
- Compose BOM 2026.06.00
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- DataStore 1.2.1
- WorkManager 2.11.2
- Media3 1.10.1
- OkHttp 5.3.0
- kotlinx.coroutines 1.11.0
- kotlinx.serialization JSON 1.11.0

The dependency set intentionally favors stable releases. Re-verify versions before each release branch; do not use dynamic version selectors.

## Runtime Source-of-Truth Bootstrap Contracts

### 1. Venice API Docs Mirror
- Script: `scripts/bootstrap-venice-api-docs.sh`
- State file: `.local/venice-api-docs.env`
- Source mirror: `/Users/super_user/Projects/Venice Fyr/.source/venice-api-docs/`
- Reference manifest: `docs/reference/VENICE_API_SOURCE_MANIFEST.md`
- Documentation guide: `docs/VENICE_API_SOURCE_BOOTSTRAP.md`

### 2. Venice Forge Desktop Mirror
- Script: `scripts/bootstrap-desktop-source.sh`
- State file: `.local/desktop-source.env`
- Source mirror: `/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/`
- Documentation guide: `docs/DESKTOP_SOURCE_BOOTSTRAP.md`

## Milestone 1 — Typed Capabilities, Authoritative Traits, SSE, Multi-Turn, Room v1

- Venice API Docs HEAD: `6e69346b13695bd53ba33a1d34e7b28841e10f98` (Swagger `20260814.194349`)
- Desktop HEAD: `bc5c17374ef4937f5837f5580d29a88bfab333ee`
- Implemented:
  - Runtime dynamic model discovery and `model_spec` parsing (`VeniceModel`, `ModelSpec`, `ModelCapabilities`, `ModelCatalog`).
  - Authoritative `/models/traits` (`{"default": "model-id"}`) and `/models/compatibility_mapping` parsing.
  - Multi-turn conversation history persistence and request serialization.
  - SSE streaming with spec-compliant multiline event parsing, multi-tool-call delta parsing, and single terminal event enforcement.
  - Cancellation-native asynchronous OkHttp streaming; consumer cancellation cancels the underlying call without waiting for a blocked response read.
  - Explicit terminal-state handling: `finish_reason` and `[DONE]` succeed, while unexpected EOF fails with `VeniceSdkException.Protocol`.
  - Structured SDK error hierarchy (`RateLimit`, `Authentication`, `Validation`, `Server`, `Http`) with header extraction.
  - Shared SDK instance across UI and background layers.

### 2026-08-15 Chat Transport Hardening Session

- Venice Fyr starting HEAD: `c9e69cc581050b485eb2b990e8c9e207e9867df5`.
- Venice API Docs HEAD: `6e69346b13695bd53ba33a1d34e7b28841e10f98` (Swagger `20260814.194349`).
- Upstream paths consulted: `swagger.yaml` (`POST /chat/completions`, `ChatCompletionRequest`), `api-reference/error-codes.mdx`, `guides/projects/rust-llm-gateway.mdx`, `agents.md`, `skill.md`, and the required `api-reference/`, `guides/`, `overview/`, and `data/static-models.json` source subset.
- Drift review from the prior `db3b9f4` snapshot found no chat wire change relevant to this correction; the Swagger change adds `discount_to_user` model metadata and advances the schema version.

### 2026-08-15 Post-Remediation Contract Audit

- Venice Fyr starting HEAD: `df8f383b590ad7f8e40201e1b6b64ab039712f54`.
- Venice API Docs HEAD: `6e69346b13695bd53ba33a1d34e7b28841e10f98` (Swagger `20260814.194349`; no source drift).
- Desktop parity HEAD: `bc5c17374ef4937f5837f5580d29a88bfab333ee`.
- Upstream paths consulted: `swagger.yaml`; `agents.md`; `skill.md`; `api-reference/endpoint/{chat,image,video,audio}`; `guides/features/reasoning-models.mdx`; `guides/media/`; `overview/`; and `data/static-models.json`.
- Contract corrections: binary image edit/multi-edit/upscale results, video JSON `COMPLETED`, typed chat reasoning controls/deltas, queued audio lifecycle, audio transcription/voice upload, video quote/transcription, and HTTP 402 classification.
- Runtime discovery correction: `/models/traits` and `/models/compatibility_mapping` now receive each discovered valid modality instead of silently inheriting the server's text default.

### 2026-08-15 Emulator and Durable Media Session

- Venice Fyr starting HEAD: `3883970779ad1f0f69e0f528047650149936d98b`.
- Venice API Docs HEAD: `6e69346b13695bd53ba33a1d34e7b28841e10f98` (Swagger `20260814.194349`; unchanged from the repository baseline).
- Desktop parity HEAD: `bc5c17374ef4937f5837f5580d29a88bfab333ee`.
- Upstream paths consulted: `swagger.yaml` image/model/authentication contracts; `agents.md`; `skill.md`; `api-reference/`; `guides/`; `overview/`; and `data/static-models.json`.
- Product correction: completed generated images moved from cache-only process state to validated app-private files plus profile-scoped Room v2 metadata and latest-result restore. Venice request serialization did not change.
