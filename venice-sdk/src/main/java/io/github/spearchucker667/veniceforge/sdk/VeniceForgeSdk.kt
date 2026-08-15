package io.github.spearchucker667.veniceforge.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Public Android SDK facade for interacting with official Venice.ai APIs.
 *
 * It intentionally does not persist API keys; callers supply credentials per request or
 * through a higher-level credential provider owned by their app.
 */
class VeniceForgeSdk(
    private val config: VeniceSdkConfig = VeniceSdkConfig(),
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    /**
     * Entry point for image generation, editing, and upscaling capabilities.
     */
    fun imageClient(): ImageClient = ImageClient(this)

    /**
     * Entry point for audio generation, speech, and voice capabilities.
     */
    fun audioClient(): io.github.spearchucker667.veniceforge.sdk.audio.AudioClient = io.github.spearchucker667.veniceforge.sdk.audio.AudioClient(this)

    /**
     * Entry point for video generation and asynchronous queue capabilities.
     */
    fun videoClient(): io.github.spearchucker667.veniceforge.sdk.video.VideoClient = io.github.spearchucker667.veniceforge.sdk.video.VideoClient(this)

    private val json = Json { ignoreUnknownKeys = true }
    private val _baseUrl = config.baseUrl.toHttpUrl()

    fun baseUrl() = _baseUrl
    fun userAgent() = config.userAgent
    fun httpClient() = httpClient


    /**
     * Lists available models from the Venice API (GET /models).
     *
     * @param apiKey Venice API key (Bearer token).
     * @param type Optional model type filter. If null, the query parameter is omitted per official spec.
     */
    suspend fun listModels(apiKey: String, type: ModelType? = null): List<VeniceModel> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val urlBuilder = _baseUrl.newBuilder().addPathSegment("models")
        if (type != null) {
            urlBuilder.addQueryParameter("type", type.wireName)
        }
        val url = urlBuilder.build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent)
            .get()
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw VeniceSdkException.Network(e, isTimeout = true)
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e, isTimeout = false)
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw parseHttpError(res)
            }
            val body = res.body?.string().orEmpty()
            val root = runCatching { json.parseToJsonElement(body).jsonObject }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /models", it) }
            val data = root["data"] as? JsonArray
                ?: throw VeniceSdkException.Protocol("/models response did not contain a data array")
            data.mapNotNull(::parseModel)
        }
    }

    /**
     * Backwards-compatible overload of [listModels] accepting string type.
     */
    @Deprecated("Use typed listModels(apiKey, ModelType?) instead")
    suspend fun listModels(apiKey: String, type: String): List<VeniceModel> {
        val modelType = ModelType.fromWireName(type)
        return listModels(apiKey, modelType)
    }

    /**
     * Internal helper that issues a raw authenticated GET to a relative Venice path
     * and returns the response body verbatim. Used by [CapabilitiesRepository] to
     * parse endpoints whose shape is not yet promoted to typed SDK methods.
     */
    internal suspend fun getRaw(path: String, apiKey: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val url = _baseUrl.newBuilder()
            .addPathSegments(path.trimStart('/'))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent)
            .get()
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw VeniceSdkException.Network(e, isTimeout = true)
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e, isTimeout = false)
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw parseHttpError(res)
            }
            res.body?.string().orEmpty()
        }
    }

    internal fun parseHttpError(res: Response): VeniceSdkException {
        val statusCode = res.code
        val requestId = res.header("x-request-id") ?: res.header("request-id")
        val rateLimitInfo = res.extractRateLimitInfo()
        val retryAfter = res.header("retry-after")?.toLongOrNull()
            ?: rateLimitInfo.resetRequestsTimestamp

        val body = runCatching { res.body?.string() }.getOrNull()
        var errorCode: String? = null
        var safeMessage = "HTTP $statusCode"
        var details: String? = null

        if (!body.isNullOrBlank()) {
            val jsonRoot = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            if (jsonRoot != null) {
                val errElem = jsonRoot["error"]
                if (errElem is JsonPrimitive && errElem.isString) {
                    safeMessage = errElem.content
                } else if (errElem is JsonObject) {
                    safeMessage = errElem.stringOrNull("message") ?: safeMessage
                    errorCode = errElem.stringOrNull("code")
                }
                details = jsonRoot["details"]?.toString() ?: jsonRoot["issues"]?.toString()
            }
        }

        return when (statusCode) {
            429 -> VeniceSdkException.RateLimit(
                statusCode = statusCode,
                errorCode = errorCode ?: "RATE_LIMIT_EXCEEDED",
                safeMessage = safeMessage,
                requestId = requestId,
                retryAfterSeconds = retryAfter,
                rateLimitInfo = rateLimitInfo,
            )
            401, 403 -> VeniceSdkException.Authentication(
                statusCode = statusCode,
                errorCode = errorCode,
                safeMessage = safeMessage,
                requestId = requestId,
            )
            400, 422 -> VeniceSdkException.Validation(
                statusCode = statusCode,
                errorCode = errorCode,
                safeMessage = safeMessage,
                requestId = requestId,
                validationDetails = details,
            )
            in 500..599 -> VeniceSdkException.Server(
                statusCode = statusCode,
                errorCode = errorCode,
                safeMessage = safeMessage,
                requestId = requestId,
            )
            else -> VeniceSdkException.Http(
                statusCode = statusCode,
                requestId = requestId,
                safeMessage = safeMessage,
                errorCode = errorCode,
            )
        }
    }

    private fun parseModel(element: kotlinx.serialization.json.JsonElement): VeniceModel? {
        val obj = element as? JsonObject ?: return null
        val id = obj.stringOrNull("id") ?: return null
        val specObj = obj["model_spec"] as? JsonObject
        val modelSpec = specObj?.let(::parseModelSpec)

        return VeniceModel(
            id = id,
            objectType = obj.stringOrNull("object"),
            created = obj.longOrNull("created"),
            ownedBy = obj.stringOrNull("owned_by"),
            type = obj.stringOrNull("type") ?: "text",
            name = modelSpec?.name ?: specObj?.stringOrNull("name"),
            description = modelSpec?.description ?: specObj?.stringOrNull("description"),
            rawJson = obj.toString(),
            modelSpec = modelSpec,
        )
    }

    private fun parseModelSpec(spec: JsonObject): ModelSpec {
        val capsObj = spec["capabilities"] as? JsonObject
        val caps = capsObj?.let { c ->
            ModelCapabilitiesSpec(
                supportsVision = c.boolOrFalse("supportsVision"),
                supportsMultipleImages = c.boolOrFalse("supportsMultipleImages"),
                supportsVideoInput = c.boolOrFalse("supportsVideoInput"),
                supportsAudioInput = c.boolOrFalse("supportsAudioInput"),
                supportsFunctionCalling = c.boolOrFalse("supportsFunctionCalling"),
                supportsWebSearch = c.boolOrFalse("supportsWebSearch"),
                supportsXSearch = c.boolOrFalse("supportsXSearch"),
                supportsReasoning = c.boolOrFalse("supportsReasoning"),
                supportsReasoningEffort = c.boolOrFalse("supportsReasoningEffort"),
                supportsResponseSchema = c.boolOrFalse("supportsResponseSchema"),
                supportsLogProbs = c.boolOrFalse("supportsLogProbs"),
                supportsTeeAttestation = c.boolOrFalse("supportsTeeAttestation"),
                supportsE2EE = c.boolOrFalse("supportsE2EE"),
                optimizedForCode = c.boolOrFalse("optimizedForCode"),
            )
        }

        val traitsList = (spec["traits"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
        } ?: emptyList()

        return ModelSpec(
            name = spec.stringOrNull("name"),
            description = spec.stringOrNull("description"),
            modelSource = spec.stringOrNull("modelSource"),
            availableContextTokens = spec.longOrNull("availableContextTokens"),
            maxCompletionTokens = spec.longOrNull("maxCompletionTokens"),
            privacy = spec.stringOrNull("privacy"),
            uncensored = spec.boolOrNull("uncensored"),
            offline = spec.boolOrNull("offline"),
            beta = spec.boolOrNull("beta"),
            betaModel = spec.boolOrNull("betaModel"),
            traits = traitsList,
            capabilities = caps,
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.longOrNull(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonObject.boolOrNull(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.boolOrFalse(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: false
}

/**
 * Extracts rate limit headers from OkHttp Response.
 */
fun Response.extractRateLimitInfo(): RateLimitInfo {
    return RateLimitInfo(
        limitRequests = header("x-ratelimit-limit-requests")?.toLongOrNull(),
        remainingRequests = header("x-ratelimit-remaining-requests")?.toLongOrNull(),
        resetRequestsTimestamp = header("x-ratelimit-reset-requests")?.toLongOrNull(),
        limitTokens = header("x-ratelimit-limit-tokens")?.toLongOrNull(),
        remainingTokens = header("x-ratelimit-remaining-tokens")?.toLongOrNull(),
        resetTokensSeconds = header("x-ratelimit-reset-tokens")?.toLongOrNull(),
    )
}
