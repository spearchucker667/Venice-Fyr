package io.github.spearchucker667.veniceforge.sdk.video

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class VideoClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun queue(apiKey: String, request: QueueVideoRequest): VideoQueueResponse =
        executeJsonRequest(apiKey, VeniceEndpoints.VIDEO_QUEUE, json.encodeToString(QueueVideoRequest.serializer(), request))

    suspend fun complete(apiKey: String, request: CompleteVideoRequest): VideoCompleteResponse =
        executeJsonRequest(apiKey, VeniceEndpoints.VIDEO_COMPLETE, json.encodeToString(CompleteVideoRequest.serializer(), request))

    suspend fun quote(apiKey: String, request: QuoteVideoRequest): VideoQuoteResponse =
        executeJsonRequest(apiKey, VeniceEndpoints.VIDEO_QUOTE, json.encodeToString(QuoteVideoRequest.serializer(), request))

    suspend fun transcribe(apiKey: String, request: VideoTranscriptionRequest): VideoTranscriptionResult {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(request.responseFormat == "json" || request.responseFormat == "text") {
            "responseFormat must be json or text"
        }
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.VIDEO_TRANSCRIPTIONS).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", if (request.responseFormat == "text") "text/plain" else "application/json")
            .header("User-Agent", sdk.userAgent())
            .post(json.encodeToString(VideoTranscriptionRequest.serializer(), request).toRequestBody(jsonMedia))
            .build()
        return sdk.awaitResponse(httpReq).use { res ->
            if (!res.isSuccessful) throw sdk.parseHttpError(res)
            val body = res.body.string()
            if (request.responseFormat == "text") {
                VideoTranscriptionResult.Text(body)
            } else {
                val parsed = runCatching { json.decodeFromString(VideoTranscriptionJsonResponse.serializer(), body) }
                    .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /video/transcriptions", it) }
                VideoTranscriptionResult.Json(parsed.transcript, parsed.language)
            }
        }
    }

    suspend fun retrieve(apiKey: String, request: RetrieveVideoRequest): VideoRetrieveResult {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val reqBody = json.encodeToString(RetrieveVideoRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.VIDEO_RETRIEVE).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json, video/mp4")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = sdk.awaitResponse(httpReq)

        return response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            
            val contentType = res.body.contentType()?.toString().orEmpty()
            if (contentType.contains("application/json")) {
                val bodyStr = res.body.string()
                val statusRes = runCatching { json.decodeFromString(RetrieveVideoResponseStatus.serializer(), bodyStr) }
                    .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /video/retrieve", it) }
                when (statusRes.status) {
                    "PROCESSING" -> VideoRetrieveResult.Processing(
                        statusRes.status,
                        statusRes.averageExecutionTime,
                        statusRes.executionDuration,
                    )
                    "COMPLETED" -> VideoRetrieveResult.CompletedRemote(
                        statusRes.status,
                        statusRes.averageExecutionTime,
                        statusRes.executionDuration,
                    )
                    else -> VideoRetrieveResult.UnknownStatus(
                        statusRes.status,
                        statusRes.averageExecutionTime,
                        statusRes.executionDuration,
                    )
                }
            } else {
                val mimeType = res.body.contentType()?.toString()
                    ?: throw VeniceSdkException.Protocol("Missing Content-Type from /video/retrieve")
                if (!mimeType.startsWith("video/")) {
                    throw VeniceSdkException.Protocol("Unexpected Content-Type from /video/retrieve: $mimeType")
                }
                val bytes = res.body.bytes()
                if (bytes.isEmpty()) throw VeniceSdkException.Protocol("Empty binary response from /video/retrieve")
                VideoRetrieveResult.CompletedBinary(
                    binaryVideo = bytes,
                    mimeType = mimeType,
                    requestId = res.header("x-request-id") ?: res.header("request-id"),
                )
            }
        }
    }

    private suspend inline fun <reified T> executeJsonRequest(apiKey: String, endpoint: String, reqBody: String): T {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = sdk.awaitResponse(httpReq)

        return response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            val bodyStr = res.body.string()
            runCatching { json.decodeFromString<T>(bodyStr) }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /$endpoint", it) }
        }
    }

}
