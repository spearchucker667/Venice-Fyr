# findings/sdk-core.md

Detailed findings for the SDK core, public API, model/capability discovery, and transport-centralization audit.

---

## SDK-CORE-01 | P1 | CONFIRMED

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
private suspend fun fetchTraits(apiKey: String): Map<String, String> {
    val raw = try {
        sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)
    } catch (_: Exception) {
        return emptyMap()
    }
    ...
}

private suspend fun fetchCompatibility(apiKey: String): Map<String, String> {
    val raw = try {
        sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)
    } catch (_: Exception) {
        return emptyMap()
    }
    ...
}
```

**Venice reference:** `swagger.yaml:8544-8619` (`/models/traits`) and `swagger.yaml:8622-8660` (`/models/compatibility_mapping`) define an optional `type` query parameter with `default: text`.

**Expected:** `CapabilitiesRepository` should pass `type` (or fetch all relevant types) so image/audio/video traits and aliases are discoverable.

**Actual:** No `type` query parameter is sent; the Venice server defaults to `text`, so only text traits/aliases are returned.

**Impact:** Image, audio, video, and embedding default-model traits and compatibility aliases are invisible to the SDK. Apps cannot resolve default models for non-text modalities through `ModelCatalog`.

**Root cause:** Missing query parameter in `getRaw` calls.

**Related occurrences:** `ModelCatalog.defaultTextModelId` (line 20-24) is also text-centric.

**Android/Kotlin reference:** OkHttp `HttpUrl.Builder.addQueryParameter`.

**Remediation:** Add an optional `type: ModelType?` parameter to `fetchLiveCapabilities` and forward it to both `/models/traits` and `/models/compatibility_mapping`; default to `null` to preserve backward behavior, or call per-modality and merge.

**Tests required:** Unit tests verifying that `type=image`/`audio`/`video` query params are appended and that non-text traits/aliases are parsed.

**Compatibility impact:** Source-compatible if new parameter has a default value; binary-compatible if added as an overload.

---

## SDK-CORE-02 | P1 | CONFIRMED

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt`  
**Lines:** 20-24  
**Symbol:** `defaultTextModelId`

**Evidence:**
```kotlin
val defaultTextModelId: String?
    get() = traits["default"]
        ?: traits["text:default"]
        ?: models.firstOrNull { it.supportsTextChat && !it.offline }?.id
        ?: models.firstOrNull { it.supportsTextChat }?.id
```

**Venice reference:** `swagger.yaml:6120-6127` example traits include `default`, `fastest`; upstream docs mention `text:default`, `text:uncensored`, `image:fast`.

**Expected:** Default model resolution should support modality-specific traits (`image:default`, `audio:default`, `video:default`) and avoid selecting offline/beta models as fallbacks.

**Actual:** Only `"default"` and `"text:default"` are checked. The fallback to the first non-offline text model is reasonable, but the first offline-aware check is skipped if the model is offline (the final fallback ignores `offline`).

**Impact:** Non-text default models cannot be resolved. The fallback may select a model that is offline or in beta.

**Root cause:** Hard-coded trait key list and single-modality focus.

**Related occurrences:** `CapabilitiesRepository.fetchLiveCapabilities` (SDK-CORE-01).

**Android/Kotlin reference:** Kotlin `Map.get`.

**Remediation:** Add modality-aware resolvers (e.g., `defaultModelIdFor(type: ModelType)`), and filter out `offline=true` and optionally `beta=true` from fallbacks.

**Tests required:** Tests for `image:default`, `audio:default`, and offline-model filtering.

**Compatibility impact:** Additive API; no breaking changes if new methods are added.

---

## SDK-CORE-03 | P2 | CONFIRMED

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** VeniceModel.kt:8-58, ModelCapabilities.kt:11-38  
**Symbol:** `VeniceModel`, `ModelSpec`, `ModelCapabilitiesSpec`, `ModelCapabilities`

**Evidence:**
`ModelSpec` contains only: `name`, `description`, `modelSource`, `availableContextTokens`, `maxCompletionTokens`, `privacy`, `uncensored`, `offline`, `beta`, `betaModel`, `traits`, `capabilities`.
`ModelCapabilities` omits constraints, pricing, deprecation, quantization, reasoning effort options, max images/videos, media-specific fields, etc.

**Venice reference:** `swagger.yaml:4659-6075` defines `ModelResponse` and `model_spec` with fields such as `context_length`, `discount_to_user`, `constraints`, `deprecation`, `regionRestrictions`, `pricing`, `capabilities.quantization`, `capabilities.reasoningEffortOptions`, `capabilities.maxImages`, `capabilities.maxVideos`, `embeddingDimensions`, `voices`, `voice_cloning`, `supported_formats`, etc.

**Expected:** The SDK should expose enough model metadata for callers to make capability-aware decisions and validate request parameters.

**Actual:** A large subset of authoritative model metadata is discarded during parsing.

**Impact:** Apps cannot enforce per-model constraints (aspect ratios, resolutions, max images, max videos), display pricing, or reason about deprecation/replacement. UI may offer options that the selected model does not support.

**Root cause:** Minimalistic parser/data model that only covers chat-relevant fields.

**Related occurrences:** `VeniceForgeSdk.parseModelSpec` lines 222-261.

**Android/Kotlin reference:** Kotlinx Serialization `Json { ignoreUnknownKeys = true }`.

**Remediation:** Expand `ModelSpec`/`ModelCapabilities` to include at least `constraints`, `pricing`, `deprecation`, `quantization`, `reasoningEffortOptions`, `maxImages`, `maxVideos`, and media-specific fields. Keep unknown fields accessible via `rawJson`.

**Tests required:** Deserialize a comprehensive swagger-like model fixture and assert all new fields are populated.

**Compatibility impact:** Adding properties with defaults preserves source compatibility; data-class binary compatibility requires care (prefer non-data class or stable API module).

---

## SDK-CORE-04 | P2 | CONFIRMED

**Area:** Model/Capability Semantics  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** 22-23, 43-50  
**Symbol:** `supportsStreaming`, `supportsSystemPrompt`, `supportsTextChat`, `supportsImageGeneration`

**Evidence:**
```kotlin
val supportsStreaming: Boolean = true,
val supportsSystemPrompt: Boolean = true,
```
```kotlin
val supportsTextChat: Boolean
    get() = type.equals("text", ignoreCase = true) || type.equals("code", ignoreCase = true)
val supportsImageGeneration: Boolean
    get() = type.equals("image", ignoreCase = true)
```

**Venice reference:** `swagger.yaml:5239-5242` defines `model_spec.offline` as "Is this model presently offline?" — not a streaming capability flag. `swagger.yaml:6055-6067` defines model `type` enum: `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video`. There is no `code` type in the response enum.

**Expected:** `supportsStreaming` and `supportsSystemPrompt` should be derived from authoritative capability/constraint fields, not hard-coded. `supportsTextChat` should align with the swagger `type` enum.

**Actual:** `supportsSystemPrompt` is always true. `supportsStreaming` is derived from `offline != true` in `CapabilitiesRepository`. `supportsTextChat` treats `"code"` as a valid type, which never appears in `/models` responses. `supportsImageGeneration` ignores `inpaint`/`upscale` types that also produce images.

**Impact:** Capability queries return incorrect or misleading results. UI may claim a model supports streaming/system prompts when it does not.

**Root cause:** Hard-coded defaults and heuristic type checks instead of parsing authoritative capability fields.

**Related occurrences:** `CapabilitiesRepository.kt` lines 66, 67.

**Android/Kotlin reference:** Kotlin data class default property values.

**Remediation:** Parse `supportsSystemPrompt` and `supportsStreaming` from authoritative fields if/when Venice exposes them; otherwise expose them as nullable/unknown rather than `true`. Align `supportsTextChat` and `supportsImageGeneration` with the actual swagger `type` enum.

**Tests required:** Tests with models whose `offline=true` and models of type `inpaint`/`upscale`.

**Compatibility impact:** Changing default values or semantics may alter existing consumer behavior; document as behavior change.

---

## SDK-CORE-05 | P2 | CONFIRMED

**Area:** Error Handling / Resilience  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
catch (_: Exception) {
    return emptyMap()
}
```

**Expected:** Network or parse failures for traits/compatibility should be surfaced to the caller so the app knows discovery is incomplete.

**Actual:** All exceptions are swallowed and empty maps are returned. `fetchLiveCapabilities` succeeds with a catalog missing traits/aliases.

**Impact:** Silent partial failure. Apps may display stale or missing default models and compatibility aliases without knowing the data is incomplete.

**Root cause:** Broad `catch (_: Exception)` returning empty collection.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `try/catch`, coroutine exception transparency.

**Remediation:** Let exceptions propagate, or return a sealed result (`Success`, `Partial(models, traitsError, compatError)`). Do not swallow generic exceptions.

**Tests required:** Tests that simulate 5xx/parse errors for traits/compatibility and assert the exception propagates or is represented in the result.

**Compatibility impact:** API signature change if returning a sealed result; behavioral change if exceptions now propagate.

---

## SDK-CORE-06 | P2 | CONFIRMED

**Area:** Public API / Enum Handling  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** ModelType.kt:21-24, VeniceForgeSdk.kt:100-104  
**Symbol:** `ModelType.fromWireName`, `listModels(apiKey, type: String)`

**Evidence:**
```kotlin
companion object {
    fun fromWireName(wireName: String?): ModelType? {
        if (wireName == null) return null
        return entries.firstOrNull { it.wireName.equals(wireName, ignoreCase = true) }
    }
}
```
```kotlin
@Deprecated("Use typed listModels(apiKey, ModelType?) instead")
suspend fun listModels(apiKey: String, type: String): List<VeniceModel> {
    val modelType = ModelType.fromWireName(type)
    return listModels(apiKey, modelType)
}
```

**Venice reference:** `swagger.yaml:8475-8497` defines the `/models?type=` query parameter with a constrained enum.

**Expected:** An unknown type string should either fail fast or be passed through to the server for validation.

**Actual:** Unknown wire names map to `null`, and `listModels(apiKey, null)` omits the `type` query parameter, returning all models instead of filtering by the requested type.

**Impact:** Callers using the deprecated overload with a typo or future type get silently incorrect results.

**Root cause:** `fromWireName` returns `null` for unknown values, and the caller treats `null` as "no filter".

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin enum lookup.

**Remediation:** In the deprecated overload, if `fromWireName` returns `null`, throw `IllegalArgumentException` or pass the raw string through to the server. Better: remove the deprecated overload.

**Tests required:** Test that an unknown type string does not silently return all models.

**Compatibility impact:** Removing the deprecated overload is a source-breaking change; failing fast on unknown type changes runtime behavior.

---

## SDK-CORE-07 | P2 | CONFIRMED

**Area:** Caching / Performance  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 21-92  
**Symbol:** `fetchLiveCapabilities`

**Evidence:**
```kotlin
class CapabilitiesRepository(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
        val models = sdk.listModels(apiKey, null)
        val traitsMap = fetchTraits(apiKey)
        val compatMap = fetchCompatibility(apiKey)
        ...
    }
}
```

**Expected:** Model catalog data should be cacheable with a configurable TTL to avoid redundant network calls.

**Actual:** Every invocation performs three sequential network requests. There is no cache, no TTL, and no staleness check.

**Impact:** Higher latency, more bandwidth, and unnecessary load on Venice endpoints. `ModelCatalog.refreshedAt` is unused.

**Root cause:** Stateless repository with no caching layer.

**Related occurrences:** `VeniceForgeSdk.listModels`.

**Android/Kotlin reference:** Kotlin coroutines, in-memory caching patterns.

**Remediation:** Add an optional in-memory cache keyed by API key + type with a configurable TTL (e.g., 5 minutes), and expose a `forceRefresh` parameter.

**Tests required:** Tests verifying cache hit, TTL expiry, and force refresh.

**Compatibility impact:** Additive API if TTL/cache are optional.

---

## SDK-CORE-08 | P2 | CONFIRMED

**Area:** Transport / Error Handling  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`  
**Lines:** 49-57  
**Symbol:** `streamChat`

**Evidence:**
```kotlin
call.execute().use { response ->
    if (!response.isSuccessful) {
        val msg = response.body.string()
        trySend(ChatStreamChunk.Error(response.code, msg))
        hasEmittedTerminal = true
        close()
        return@callbackFlow
    }
    ...
}
```

**Expected:** Non-2xx responses should be parsed by the centralized `VeniceForgeSdk.parseHttpError` so callers receive structured `VeniceSdkException` subclasses.

**Actual:** `ChatClient` reads the raw body and emits `ChatStreamChunk.Error(code, message)`, bypassing rate-limit header extraction, error-code parsing, and exception classification.

**Impact:** Streaming callers cannot distinguish rate limits from auth failures from validation errors without parsing the raw message themselves. Rate-limit metadata is lost.

**Root cause:** ChatClient implements its own error path instead of reusing `parseHttpError`.

**Related occurrences:** `ImageClient.kt`, `AudioClient.kt`, `VideoClient.kt` correctly call `sdk.parseHttpError`.

**Android/Kotlin reference:** OkHttp `Response`, Kotlin Flow `callbackFlow`.

**Remediation:** For non-2xx streaming responses, call `sdk.parseHttpError(response)` and emit a terminal error or throw the exception (depending on streaming contract).

**Tests required:** Tests for 429/401/400 streaming responses asserting structured exception/rate-limit metadata.

**Compatibility impact:** Changes the type of errors observed by streaming consumers; document as behavior change.

---

## SDK-CORE-09 | P2 | CONFIRMED

**Area:** Transport / Client Configuration  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 27-29  
**Symbol:** `VeniceForgeSdk` constructor

**Evidence:**
```kotlin
class VeniceForgeSdk(
    private val config: VeniceSdkConfig = VeniceSdkConfig(),
    private val httpClient: OkHttpClient = OkHttpClient(),
)
```

**Expected:** The SDK should either provide sensible default timeouts or expose timeout configuration in `VeniceSdkConfig`.

**Actual:** The default `OkHttpClient` uses OkHttp's built-in 10-second connect/read/write timeouts and creates a new connection pool/dispatcher per instance.

**Impact:** On slow networks, requests may time out unexpectedly. Multiple SDK instances do not share connection pools, reducing efficiency.

**Root cause:** Default constructor delegates to a bare `OkHttpClient()` with no SDK-level configuration.

**Related occurrences:** All feature clients use `sdk.httpClient()`.

**Android/Kotlin reference:** OkHttp `OkHttpClient.Builder`, `connectTimeout`, `readTimeout`, `writeTimeout`.

**Remediation:** Add timeout fields to `VeniceSdkConfig` and apply them in a default `OkHttpClient` builder, or document that callers must supply a configured client.

**Tests required:** Tests verifying custom timeout configuration is honored.

**Compatibility impact:** Additive if config properties have defaults.

---

## SDK-CORE-10 | P2 | CONFIRMED

**Area:** Documentation / Source Accuracy  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt`  
**Lines:** 4-8  
**Symbol:** File header comment

**Evidence:**
```kotlin
/**
 * Complete endpoint-path inventory from Venice Forge's tracked 2026-08-14
 * OpenAPI snapshot (schema 20260814.153445).
 */
```

**Venice reference:** `swagger.yaml` `info.version: 20260814.194349`.

**Expected:** The tracked schema version in code comments should match the bootstrapped upstream source.

**Actual:** Comment references `20260814.153445`, which does not match the current upstream `20260814.194349`.

**Impact:** Misleading documentation; developers may think the SDK is pinned to an older schema.

**Root cause:** Stale comment not updated when the API docs were bootstrapped.

**Related occurrences:** None.

**Android/Kotlin reference:** N/A.

**Remediation:** Update the comment to the current `info.version` and consider deriving it from a build-time constant.

**Tests required:** N/A.

**Compatibility impact:** None.

---

## SDK-CORE-11 | P2 | CONFIRMED

**Area:** Model / Capability Data Types  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** 28-30  
**Symbol:** `availableContextTokens`, `maxContextTokens`, `maxCompletionTokens`

**Evidence:**
```kotlin
val availableContextTokens: Long? = null,
val maxContextTokens: Int? = null,
val maxCompletionTokens: Int? = null,
```

**Venice reference:** `swagger.yaml:4698-4708` defines `availableContextTokens` and `maxCompletionTokens` as `number` (no explicit format, JSON number).

**Expected:** Token counts should use a type that cannot overflow (e.g., `Long?`).

**Actual:** `maxContextTokens` and `maxCompletionTokens` are `Int?`, which can overflow for large context windows (e.g., 2M tokens = 2,000,000 fits in Int, but future values may not). `availableContextTokens` is `Long?` while the derived `maxContextTokens` is `Int?`, creating inconsistency.

**Impact:** Potential integer overflow or precision loss for large models; inconsistent API surface.

**Root cause:** Mixed `Long`/`Int` types for the same semantic values.

**Related occurrences:** `VeniceModel.kt` line 28-30 (`ModelSpec` uses `Long?`).

**Android/Kotlin reference:** Kotlin numeric types.

**Remediation:** Change `maxContextTokens` and `maxCompletionTokens` to `Long?` to match `ModelSpec` and swagger `number` semantics.

**Tests required:** Tests with token values exceeding `Int.MAX_VALUE`.

**Compatibility impact:** Source-breaking for consumers assigning `Int` values; binary-breaking for data class property type change.

---

## SDK-CORE-12 | P2 | CONFIRMED

**Area:** Model Type Enum  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt`  
**Lines:** 6-18  
**Symbol:** `ModelType`

**Evidence:**
```kotlin
enum class ModelType(val wireName: String) {
    ALL("all"), TEXT("text"), IMAGE("image"), VIDEO("video"), AUDIO("audio"),
    TTS("tts"), ASR("asr"), EMBEDDING("embedding"), MUSIC("music"),
    UPSCALE("upscale"), INPAINT("inpaint"), CODE("code");
```

**Venice reference:** `swagger.yaml:6055-6067` defines the model `type` response enum as: `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video`. The `/models?type=` query parameter additionally allows `all` and `code` (`swagger.yaml:8479-8492`).

**Expected:** The SDK enum should distinguish between response-side types and query-only filters.

**Actual:** `CODE` is included as a response-side type, but the swagger `ModelResponse.type` enum does not contain `code`. `ALL` is query-only but included alongside response types.

**Impact:** Consumers may check `model.type == ModelType.CODE` and never match any real model; confusion between query filters and response types.

**Root cause:** Single enum conflates query-parameter values with response values.

**Related occurrences:** `ModelCapabilities.supportsTextChat` treats `"code"` as a valid type.

**Android/Kotlin reference:** Kotlin enum.

**Remediation:** Either remove `CODE` from `ModelType` or document that it is query-only. Consider separate enums for request filters vs. response types.

**Tests required:** Tests verifying `ModelType` round-trips for all swagger response types.

**Compatibility impact:** Removing an enum value is source-breaking.

---

## SDK-CORE-13 | P2 | CONFIRMED

**Area:** Rate Limiting  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 142-145, 279-287  
**Symbol:** `parseHttpError`, `extractRateLimitInfo`

**Evidence:**
```kotlin
val retryAfter = res.header("retry-after")?.toLongOrNull()
    ?: rateLimitInfo.resetRequestsTimestamp
```
```kotlin
fun Response.extractRateLimitInfo(): RateLimitInfo {
    return RateLimitInfo(
        limitRequests = header("x-ratelimit-limit-requests")?.toLongOrNull(),
        ...
        resetRequestsTimestamp = header("x-ratelimit-reset-requests")?.toLongOrNull(),
        ...
        resetTokensSeconds = header("x-ratelimit-reset-tokens")?.toLongOrNull(),
    )
}
```

**Venice reference:** `swagger.yaml` rate-limit headers are documented in the rate-limiting guide; `x-ratelimit-reset-requests` is an epoch timestamp, `x-ratelimit-reset-tokens` is seconds-until-reset.

**Expected:** `retryAfterSeconds` should represent seconds until retry, not an absolute timestamp.

**Actual:** If `retry-after` is absent, `retryAfterSeconds` falls back to `resetRequestsTimestamp`, which is an epoch timestamp (e.g., `1700000030`), not a duration. The field name `resetTokensSeconds` vs `resetRequestsTimestamp` also implies inconsistent units.

**Impact:** Consumers using `retryAfterSeconds` as a duration will compute wildly incorrect retry times when `retry-after` is missing.

**Root cause:** Fallback from a duration header to an absolute timestamp without conversion.

**Related occurrences:** `RateLimitInfo` data class definition.

**Android/Kotlin reference:** Kotlin `Long?`.

**Remediation:** Compute `retryAfterSeconds` as `max(0, resetRequestsTimestamp - now)` when falling back, or keep separate fields for absolute timestamp and duration.

**Tests required:** Tests with missing `retry-after` but present `x-ratelimit-reset-requests`.

**Compatibility impact:** Behavioral change to `RateLimit.retryAfterSeconds`; may require API addition (`retryAfterTimestamp`).

---

## SDK-CORE-14 | P2 | CONFIRMED

**Area:** Coroutine Cancellation  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
catch (_: Exception) {
    return emptyMap()
}
```

**Android/Kotlin reference:** Kotlin `CancellationException` extends `Exception`; coroutine cancellation relies on exceptions propagating.

**Expected:** Coroutine cancellation should propagate to the caller.

**Actual:** `CancellationException` is caught by `catch (_: Exception)` and the function returns `emptyMap()`. The coroutine appears to complete successfully with partial data.

**Impact:** UI/components observing the coroutine cannot distinguish cancellation from success. Partial catalog data may be used as if it were complete.

**Root cause:** Broad exception handler catches cancellation exceptions.

**Related occurrences:** `VeniceForgeSdk.listModels` and feature clients catch `SocketTimeoutException`/`IOException` only, so cancellation propagates there.

**Android/Kotlin reference:** Kotlin coroutines `CancellationException`.

**Remediation:** Catch only `IOException` (and optionally `JsonException`) in network helpers; let `CancellationException` propagate. Alternatively, check `coroutineContext.isActive` before returning empty.

**Tests required:** Test that cancelling `fetchLiveCapabilities` throws `CancellationException` and does not return a catalog.

**Compatibility impact:** Behavioral change; cancellation now propagates as expected.

---

## SDK-CORE-15 | P2 | CONFIRMED

**Area:** Authentication / Wire Conformance  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 60-61  
**Symbol:** `listModels`

**Evidence:**
```kotlin
suspend fun listModels(apiKey: String, type: ModelType? = null): List<VeniceModel> = withContext(Dispatchers.IO) {
    require(apiKey.isNotBlank()) { "apiKey must not be blank" }
```

**Venice reference:** `swagger.yaml:8469-8471` declares `/models` security as both `{}` (anonymous) and `BearerAuth`. The endpoint supports unauthenticated access.

**Expected:** The SDK should allow anonymous model listing, consistent with the API spec.

**Actual:** `require(apiKey.isNotBlank())` rejects empty keys, forcing callers to supply a dummy key for an operation the server permits without auth.

**Impact:** Minor friction and deviation from spec; callers cannot list models without an API key.

**Root cause:** Client-side validation stricter than the API contract.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `require`.

**Remediation:** Remove the blank-key requirement from `listModels`, or add an overload that does not require a key.

**Tests required:** Test that `listModels` succeeds with a blank/null key against a server that allows anonymous access.

**Compatibility impact:** Relaxing validation is source-compatible.

---

## SDK-CORE-16 | P2 | INFERRED

**Area:** Public API Surface / Encapsulation  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 49-51  
**Symbol:** `baseUrl()`, `userAgent()`, `httpClient()`

**Evidence:**
```kotlin
fun baseUrl() = _baseUrl
fun userAgent() = config.userAgent
fun httpClient() = httpClient
```

**Expected:** Internal transport dependencies should not be part of the public SDK surface.

**Actual:** These accessors are public, exposing `HttpUrl`, `OkHttpClient`, and config values to any consumer.

**Impact:** Tight coupling between SDK internals and consumers; future changes to transport (e.g., Ktor) become breaking.

**Root cause:** Feature clients need these values; they were exposed publicly instead of keeping transport internal.

**Related occurrences:** `ChatClient.kt`, `ImageClient.kt`, etc., call these accessors.

**Android/Kotlin reference:** Kotlin visibility modifiers.

**Remediation:** Make `baseUrl()` and `httpClient()` `internal`. If external access is needed, expose a narrower abstraction (e.g., a sealed request builder).

**Tests required:** N/A.

**Compatibility impact:** Reducing visibility is source-breaking for external consumers.

---

## SDK-CORE-17 | P2 | CONFIRMED

**Area:** Exception Hierarchy  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt`  
**Lines:** 57-66  
**Symbol:** `VeniceSdkException.Http`

**Evidence:**
```kotlin
open class Http(
    val statusCode: Int,
    val requestId: String? = null,
    val safeMessage: String? = null,
    val errorCode: String? = null,
) : VeniceSdkException(...)
```

**Expected:** All exception subclasses should be data classes for consistent equality, copy, and destructuring support.

**Actual:** `Http` is an `open class`, not a `data class`. It lacks generated `equals`/`hashCode`/`copy`/`componentN` functions.

**Impact:** Consumers comparing `Http` exceptions by value will get reference equality. The inconsistency complicates the public API contract.

**Root cause:** `Http` was made open to allow extension, but open data classes are not allowed in Kotlin.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin data classes cannot be `open`.

**Remediation:** Make `Http` a `data class` and seal the hierarchy, or provide explicit `equals`/`hashCode`.

**Tests required:** Tests asserting equality of `Http` exceptions with identical fields.

**Compatibility impact:** Making `Http` a data class changes its generated bytecode; may break subclasses if any exist outside the module.

---

## SDK-CORE-18 | P2 | CONFIRMED

**Area:** Transport / Error Handling  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 166-200  
**Symbol:** `parseHttpError`

**Evidence:**
```kotlin
return when (statusCode) {
    429 -> ...
    401, 403 -> ...
    400, 422 -> ...
    in 500..599 -> ...
    else -> VeniceSdkException.Http(...)
}
```

**Venice reference:** Standard HTTP status codes; 404 is not explicitly handled.

**Expected:** 404 Not Found should be classified distinctly or at least include a clear error code.

**Actual:** 404 falls into the generic `Http` bucket. Consumers must inspect `statusCode` themselves.

**Impact:** Slightly worse UX for missing resources (e.g., unknown model slug in future endpoints).

**Root cause:** `when` branch does not cover 404.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `when`.

**Remediation:** Add a `404 -> VeniceSdkException.NotFound(...)` subclass, or document that 404 maps to `Http`.

**Tests required:** Test that 404 produces a distinguishable exception.

**Compatibility impact:** Adding a sealed subclass requires consumers to handle it in exhaustive `when`; source-breaking for exhaustive checks outside the module.

---

## SDK-CORE-19 | P3 | CONFIRMED

**Area:** Test Coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt`  
**Lines:** 6-16  
**Symbol:** `VeniceEndpointsTest`

**Evidence:**
```kotlin
class VeniceEndpointsTest {
    @Test fun canonicalMediaPathsRemainStable() { ... }
    @Test fun parameterizedPathsDoNotAddLeadingSlash() { ... }
}
```

**Expected:** The endpoint inventory should have comprehensive coverage for all constants and parameterized helpers.

**Actual:** Only 5 paths are asserted. Many endpoints (billing, crypto, x402, augment, embeddings, audio/video queue/quote/retrieve) are untested.

**Impact:** Low regression protection if endpoint constants are accidentally changed.

**Root cause:** Minimal test coverage.

**Related occurrences:** None.

**Android/Kotlin reference:** JUnit4.

**Remediation:** Add parameterized tests covering all constants and helper functions.

**Tests required:** New tests.

**Compatibility impact:** None.

---

## SDK-CORE-20 | P3 | CONFIRMED

**Area:** Public API / User Agent  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt`  
**Lines:** 3-6  
**Symbol:** `VeniceSdkConfig`

**Evidence:**
```kotlin
data class VeniceSdkConfig(
    val baseUrl: String = "https://api.venice.ai/api/v1/",
    val userAgent: String = "VeniceForgeAndroid/0.1.0",
)
```

**Expected:** The SDK should allow consumers to identify their app/version in the user agent.

**Actual:** `userAgent` is a fixed string with no built-in way to append app name/version.

**Impact:** Venice server logs cannot distinguish between different apps or versions using the SDK.

**Root cause:** Single hard-coded user agent string.

**Related occurrences:** `VeniceForgeSdk.userAgent()`.

**Android/Kotlin reference:** Android `BuildConfig.VERSION_NAME` (not available in library module by default).

**Remediation:** Accept an optional app identifier/version in `VeniceSdkConfig` and compose the user agent string.

**Tests required:** Tests verifying custom user agent is sent.

**Compatibility impact:** Additive if new fields have defaults.

---

## Positive findings

1. **No production hard-coded model IDs:** All model IDs in `src/main` are runtime-derived; the SDK complies with AGENTS.md rule 1 for production code.
2. **No plaintext credential persistence:** The SDK does not persist API keys; keys are supplied per request.
3. **Injectable HTTP client:** `VeniceForgeSdk` accepts an `OkHttpClient`, enabling test doubles and interceptors.
4. **`ignoreUnknownKeys = true`:** JSON parsers tolerate future swagger additions without crashing.
5. **API key leak test:** `VeniceForgeSdkTest` verifies exception messages do not contain the API key.
6. **Trait fallback tests:** `CapabilitiesRepositoryTest` covers orphan traits and missing-trait fallback behavior.
