# 07-SDK-PUBLIC-API-AUDIT.md

**Auditor scope:** SDK core public API, model/capability discovery, transport centralization, and hard-coded model ID compliance.  
**Upstream source-of-truth:** `.source/venice-api-docs/swagger.yaml` (HEAD `6e69346b`, `info.version: 20260814.194349`).  
**Local branch:** `main @ 1da3142`, clean tree.

---

## 1. File ledger

| # | Path | Lines | Reviewed | Findings |
|---|------|-------|----------|----------|
| 1 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | Y | 8 |
| 2 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | Y | 1 |
| 3 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | Y | 1 |
| 4 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 105 | Y | 2 |
| 5 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt` | 26 | Y | 1 |
| 6 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt` | 58 | Y | 1 |
| 7 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` | 121 | Y | 5 |
| 8 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt` | 51 | Y | 4 |
| 9 | `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt` | 42 | Y | 2 |
| 10 | `venice-sdk/build.gradle.kts` | 29 | Y | 0 |
| 11 | `venice-sdk/consumer-rules.pro` | 1 | Y | 0 |
| 12 | `venice-sdk/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| 13 | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt` | 17 | Y | 1 |
| 14 | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 161 | Y | 0 |
| 15 | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt` | 102 | Y | 0 |
| 16 | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt` | 78 | Y | 0 |

*Supplementary files reviewed for transport-centralization context (not in explicit scope):* `sdk/chat/ChatClient.kt`, `sdk/image/ImageClient.kt`, `sdk/audio/AudioClient.kt`, `sdk/video/VideoClient.kt`.

---

## 2. Public API surface inventory

All declarations are public unless annotated otherwise. Kotlin default visibility is public.

### 2.1 Entry point & configuration

| Type | Kind | Public members / notes |
|------|------|------------------------|
| `VeniceForgeSdk` | class | `constructor(config = VeniceSdkConfig(), httpClient = OkHttpClient())`, `imageClient()`, `audioClient()`, `videoClient()`, `baseUrl(): HttpUrl`, `userAgent(): String`, `httpClient(): OkHttpClient` |
| `VeniceSdkConfig` | data class | `baseUrl: String`, `userAgent: String`; init validates HTTPS + trailing slash |
| `VeniceSdkException` | sealed class | Subclasses: `RateLimit`, `Authentication`, `Validation`, `Server`, `Http` (open), `Network`, `Protocol`, `Cancelled` |
| `RateLimitInfo` | data class | Token/request limit/remaining/reset metadata |

### 2.2 Model discovery & capabilities

| Type | Kind | Public members / notes |
|------|------|------------------------|
| `VeniceModel` | data class | `id`, `objectType`, `created`, `ownedBy`, `type`, `name`, `description`, `rawJson`, `modelSpec` |
| `ModelSpec` | data class | `@Serializable`; subset of `model_spec` fields |
| `ModelCapabilitiesSpec` | data class | `@Serializable`; boolean capability flags |
| `ModelType` | enum | `ALL`, `TEXT`, `IMAGE`, `VIDEO`, `AUDIO`, `TTS`, `ASR`, `EMBEDDING`, `MUSIC`, `UPSCALE`, `INPAINT`, `CODE`; `wireName`; `fromWireName(String?)` |
| `CapabilitiesRepository` | class | `constructor(sdk: VeniceForgeSdk)`, `fetchLiveCapabilities(apiKey: String): ModelCatalog` |
| `ModelCapabilities` | data class | Merged runtime capability view; ~25 properties |
| `ModelCatalog` | data class | `models`, `traits`, `compatibilityMapping`, `refreshedAt`, `defaultTextModelId`, `byId(id)`, `modelForTrait(trait)`, `modelForAlias(alias)` |

### 2.3 Endpoint inventory

| Type | Kind | Public members |
|------|------|----------------|
| `VeniceEndpoints` | object | String constants for all known Venice v1 paths; parameterized helpers for IDs/slugs/wallets |

### 2.4 Feature clients (part of public surface, audited for transport only)

| Type | Package | Public entry points |
|------|---------|---------------------|
| `ChatClient` | `sdk.chat` | `streamChat(apiKey, ChatRequest): Flow<ChatStreamChunk>` |
| `ImageClient` | `sdk.image` | `generate`, `generateBinary`, `upscale`, `edit`, `multiEdit` |
| `AudioClient` | `sdk.audio` | `speech` |
| `VideoClient` | `sdk.video` | `queue`, `complete`, `retrieve` |

### 2.5 Coroutine / nullability / exception contracts

- All network-facing SDK methods are `suspend` and run on `Dispatchers.IO` internally.
- API key parameters are non-null `String` and validated with `require(apiKey.isNotBlank())`.
- `listModels` returns non-null `List<VeniceModel>`; individual malformed entries are dropped (`mapNotNull`).
- `ModelCatalog.defaultTextModelId` is nullable; `byId`, `modelForTrait`, `modelForAlias` are nullable.
- Exceptions are thrown as `VeniceSdkException` subclasses; streaming errors in `ChatClient` are emitted as `ChatStreamChunk.Error` flow events instead of thrown exceptions.

### 2.6 Binary-compatibility notes

- `VeniceForgeSdk` constructor uses default arguments; adding new required parameters would break binary compatibility.
- `ModelCapabilities`, `ModelCatalog`, `ModelSpec`, `ModelCapabilitiesSpec`, `VeniceModel` are data classes with many constructor properties. Adding new required properties breaks binary compatibility; adding with defaults preserves source compatibility only.
- `VeniceSdkException` is a sealed class; new subclasses can be added without breaking exhaustive `when` in the same module, but consumers outside the module cannot extend it.

---

## 3. Model discovery audit against swagger

### 3.1 `/models` (GET)

- **SDK method:** `VeniceForgeSdk.listModels(apiKey, type?)`.
- **Wire conformance:**
  - Sends `Authorization: Bearer <apiKey>` and `Accept: application/json`. OK.
  - Omits `type` query when `null`, matching spec (`required: false`). OK.
  - Sends typed `ModelType.wireName`. OK.
- **Gap:** The endpoint declares `security: [{}, {BearerAuth}]` — anonymous access is allowed. The SDK *requires* a non-blank API key, preventing anonymous model listing (`SDK-CORE-24`).
- **Gap:** `ModelResponse` requires `model_spec`; the SDK parser defaults `type` to `"text"` if absent, masking missing data.

### 3.2 `/models/traits` and `/models/compatibility_mapping`

- **SDK access:** `CapabilitiesRepository.fetchLiveCapabilities` calls `sdk.getRaw(...)` for both.
- **Wire conformance:**
  - Both endpoints support an optional `type` query parameter with default `text` (`swagger.yaml:8567`, `8645`).
  - **The SDK never passes `type`, so it only retrieves *text* traits and compatibility aliases by default.** This means image/audio/video default model traits and aliases are not discovered (`SDK-CORE-01`).
  - Both endpoints return `{"data": {...}, "object": "list", "type": "..."}`; the SDK correctly unwraps `data`. OK.

### 3.3 Model response parsing coverage

The SDK parses a subset of `ModelResponse` / `model_spec`. See `matrix/models.md` for the full field map. Key gaps:

- `context_length`, `discount_to_user` (top-level) are ignored.
- `model_spec.constraints`, `deprecation`, `regionRestrictions`, `pricing` are ignored.
- `model_spec.capabilities.quantization`, `reasoningEffortOptions`, `defaultReasoningEffort`, `maxImages`, `maxVideos` are ignored.
- Media-specific fields (`voices`, `voice_cloning`, `supported_formats`, `duration_options`, `lyrics_*`, etc.) are ignored.

These omissions are acceptable for a minimal chat client but limit the SDK's ability to drive capability-aware UI or enforce request constraints.

### 3.4 Trait / alias resolution

- `ModelCatalog.modelForTrait` and `modelForAlias` perform exact key lookups. OK.
- `ModelCatalog.defaultTextModelId` checks `traits["default"]` and `traits["text:default"]` only. It does not support `image:default`, `audio:default`, etc. (`SDK-CORE-02`).
- The fallback to the first available text model does not filter out `offline`, `beta`, or respect privacy preferences (`SDK-CORE-28`).
- Orphan traits (trait pointing to a model not in `/models`) return the raw trait value from `defaultTextModelId` even though `modelForTrait` returns `null`. This is by design but documented in tests.

---

## 4. Hard-coded model ID audit

**AGENTS.md rule:** *"Never hard-code a current Venice model catalog or permanent default model ID."*

A full-repo grep for model-ID-shaped strings found occurrences only in:

1. **Test fixtures** (`venice-sdk/src/test/resources/fixtures/models-with-capabilities/*.json`) — acceptable per the Fixture Rule; they represent a static snapshot for unit tests.
2. **Unit tests** (`CapabilitiesRepositoryTest`, `ChatClientTest`, `ChatRepositoryTest`, `ChatViewModelTest`) — test data, not production fallbacks.
3. **Documentation/plans** (`docs/superpowers/plans/...`) — planning artifacts, not runtime code.
4. **Comments/examples** in `ModelCatalog.kt` (e.g., `"gpt-4o"`, `"claude-3-5-sonnet"` in doc comments) — illustrative only.

**Production code:** No hard-coded model IDs were found in `src/main`. Default model selection flows through `ModelCatalog.defaultTextModelId`, which is runtime-driven from `/models/traits` and `/models`. `VeniceForgeApp.kt` and `ChatViewModel.kt` consume the runtime default and do not embed IDs.

**Verdict:** Production code complies with the no-hardcode rule. Test fixtures should be kept current with upstream schema revisions.

---

## 5. Unknown-model / unknown-enum tolerance

- `ModelType.fromWireName` returns `null` for unknown wire names. The deprecated `listModels(apiKey, type: String)` then calls `listModels(apiKey, null)`, **silently dropping the unknown type filter and returning all models** (`SDK-CORE-06`).
- `VeniceForgeSdk.parseModel` drops malformed entries via `mapNotNull`; missing optional fields default safely. This is tolerant.
- `CapabilitiesRepository.fetchTraits` / `fetchCompatibility` silently swallow parse failures and return empty maps (`SDK-CORE-05`).
- `ChatClient.parseChunks` emits `ChatStreamChunk.Error` for malformed SSE JSON rather than throwing, which is appropriate for streaming.

---

## 6. Caching, staleness, and runtime discovery behavior

- **No caching:** `CapabilitiesRepository` and `VeniceForgeSdk` have no in-memory or disk cache. Every call fetches `/models`, `/models/traits`, `/models/compatibility_mapping`.
- **No TTL / staleness:** `ModelCatalog.refreshedAt` records the fetch time but no logic uses it.
- **Sequential fetching:** The three endpoints are called sequentially, increasing latency (`SDK-CORE-18`).
- **Partial failure masking:** Traits/compatibility failures are swallowed; the caller receives a catalog with empty trait/alias maps and may not realize the discovery is incomplete (`SDK-CORE-05`).
- **Cancellation bug:** `fetchTraits`/`fetchCompatibility` catch `Exception`, which includes `CancellationException`, causing cancelled coroutines to appear successful with partial data (`SDK-CORE-61`).

---

## 7. Transport centralization audit

### 7.1 Centralized pieces

- Base URL, user agent, and `OkHttpClient` are centralized through `VeniceForgeSdk`.
- Auth header format (`Bearer $apiKey`) is consistent across all clients.
- HTTP error parsing is centralized in `VeniceForgeSdk.parseHttpError`.
- Rate-limit header extraction is centralized in `Response.extractRateLimitInfo`.

### 7.2 Decentralized / duplicated pieces

- Each feature client (`ChatClient`, `ImageClient`, `AudioClient`, `VideoClient`) builds its own `Request` with `Request.Builder()`. The pattern is duplicated ~6 times.
- Timeout/network exception mapping (`SocketTimeoutException` → `Network(isTimeout=true)`, `IOException` → `Network(isTimeout=false)`) is duplicated in every client.
- `ChatClient` does **not** call `sdk.parseHttpError` for non-2xx responses; it emits the raw body as a stream error event, losing structured error classification (`SDK-CORE-08`).

### 7.3 Timeouts and client configuration

- `VeniceSdkConfig` does not expose connect/read/write timeouts.
- `VeniceForgeSdk` default constructor creates a new `OkHttpClient()` with OkHttp's default timeouts (10 s connect/read/write). No connection-pool sharing, no dispatcher limits (`SDK-CORE-09`).

### 7.4 Injectable / mockable client

- `httpClient` is constructor-injectable, enabling unit tests with interceptors (demonstrated in `VeniceForgeSdkTest` and `CapabilitiesRepositoryTest`). Good.
- `VeniceForgeSdk` exposes `httpClient()` accessor publicly, which leaks the internal OkHttp instance.

### 7.5 Direct HTTP bypasses

- No client bypasses the injected `sdk.httpClient()`.
- `getRaw` is `internal` and used only by `CapabilitiesRepository`.

---

## 8. Summary of most important findings

1. **SDK-CORE-01 (P1):** `CapabilitiesRepository` omits the `type` query param on `/models/traits` and `/models/compatibility_mapping`, so only text traits/aliases are discovered.
2. **SDK-CORE-02 (P1):** `ModelCatalog.defaultTextModelId` does not resolve modality-specific default traits (`image:default`, etc.) and may fall back to an offline/beta model.
3. **SDK-CORE-24 (P2):** `VeniceForgeSdk.listModels` requires an API key even though `/models` allows anonymous access.
4. **SDK-CORE-61 (P2):** `CapabilitiesRepository` catches `CancellationException`, breaking coroutine cancellation semantics.
5. **SDK-CORE-22 (P2):** `RateLimitInfo.resetRequestsTimestamp` (epoch seconds) is used as a fallback for `retryAfterSeconds`, creating a unit mismatch.
6. **SDK-CORE-03 (P2):** SDK model/capability classes omit many swagger fields (constraints, pricing, deprecation, quantization, reasoning effort options, media-specific fields).
7. **SDK-CORE-08 (P2):** `ChatClient` bypasses structured error parsing for non-2xx responses.
8. **SDK-CORE-09 (P2):** Default `OkHttpClient` has no configured timeouts or shared connection pool.

See `findings/sdk-core.md` for full finding details and `matrix/models.md` for the swagger-to-SDK field coverage matrix.
