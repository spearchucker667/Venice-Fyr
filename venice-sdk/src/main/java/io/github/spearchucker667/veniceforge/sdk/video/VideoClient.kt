package io.github.spearchucker667.veniceforge.sdk.video

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException

class VideoClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun queue(apiKey: String, request: QueueVideoRequest): VideoQueueResponse =
        executeJsonRequest(apiKey, VeniceEndpoints.VIDEO_QUEUE, json.encodeToString(QueueVideoRequest.serializer(), request))

    suspend fun complete(apiKey: String, request: CompleteVideoRequest) {
        // We just execute and ignore the response body if successful
        executeRawRequest(apiKey, VeniceEndpoints.VIDEO_COMPLETE, json.encodeToString(CompleteVideoRequest.serializer(), request))
    }

    suspend fun retrieve(apiKey: String, request: RetrieveVideoRequest): VideoRetrieveResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val reqBody = json.encodeToString(RetrieveVideoRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.VIDEO_RETRIEVE).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json, video/mp4")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = try {
            sdk.httpClient().newCall(httpReq).execute()
        } catch (e: SocketTimeoutException) {
            throw VeniceSdkException.Network(e, isTimeout = true)
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e, isTimeout = false)
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            
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
        }
    }

    private suspend inline fun <reified T> executeJsonRequest(apiKey: String, endpoint: String, reqBody: String): T = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = try {
            sdk.httpClient().newCall(httpReq).execute()
        } catch (e: SocketTimeoutException) {
            throw VeniceSdkException.Network(e, isTimeout = true)
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e, isTimeout = false)
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            val bodyStr = res.body?.string().orEmpty()
            runCatching { json.decodeFromString<T>(bodyStr) }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /$endpoint", it) }
        }
    }

    private suspend fun executeRawRequest(apiKey: String, endpoint: String, reqBody: String): Unit = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = try {
            sdk.httpClient().newCall(httpReq).execute()
        } catch (e: SocketTimeoutException) {
            throw VeniceSdkException.Network(e, isTimeout = true)
        } catch (e: IOException) {
            throw VeniceSdkException.Network(e, isTimeout = false)
        }

        response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
        }
    }
}
