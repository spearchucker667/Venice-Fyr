package io.github.spearchucker667.veniceforge.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Public Android SDK facade. It intentionally does not persist API keys; callers supply
 * credentials per request or through a higher-level credential provider owned by their app.
 */
class VeniceForgeSdk(
    private val config: VeniceSdkConfig = VeniceSdkConfig(),
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = config.baseUrl.toHttpUrl()

    suspend fun listModels(apiKey: String, type: String = "all"): List<VeniceModel> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val url = baseUrl.newBuilder()
            .addPathSegment("models")
            .addQueryParameter("type", type)
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
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e)
        }

        response.use { res ->
            val requestId = res.header("x-request-id") ?: res.header("request-id")
            if (!res.isSuccessful) throw VeniceSdkException.Http(res.code, requestId)
            val body = res.body.string()
            val root = runCatching { json.parseToJsonElement(body).jsonObject }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /models", it) }
            val data = root["data"] as? JsonArray
                ?: throw VeniceSdkException.Protocol("/models response did not contain a data array")
            data.mapNotNull(::parseModel)
        }
    }

    /**
     * Internal helper that issues a raw authenticated GET to a relative Venice path
     * and returns the response body verbatim. Used by [CapabilitiesRepository] to
     * parse endpoints whose shape is not yet promoted to typed SDK methods.
     *
     * Public SDK callers should prefer typed methods on this facade; this helper
     * is intentionally [internal] until individual typed accessors land.
     */
    internal suspend fun getRaw(path: String, apiKey: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val url = baseUrl.newBuilder()
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
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e)
        }

        response.use { res ->
            val requestId = res.header("x-request-id") ?: res.header("request-id")
            if (!res.isSuccessful) throw VeniceSdkException.Http(res.code, requestId)
            res.body.string()
        }
    }

    private fun parseModel(element: kotlinx.serialization.json.JsonElement): VeniceModel? {
        val obj = element as? JsonObject ?: return null
        val id = obj.stringOrNull("id") ?: return null
        val spec = obj["model_spec"] as? JsonObject
        return VeniceModel(
            id = id,
            objectType = obj.stringOrNull("object"),
            created = obj.longOrNull("created"),
            ownedBy = obj.stringOrNull("owned_by"),
            name = spec?.stringOrNull("name"),
            description = spec?.stringOrNull("description"),
            rawJson = obj.toString(),
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.longOrNull(key: String): Long? =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull()
}
