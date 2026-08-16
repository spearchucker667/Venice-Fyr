package io.github.spearchucker667.veniceforge.sdk.audio

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AudioClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun speech(apiKey: String, request: SpeechRequest): ByteArray {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val reqBody = json.encodeToString(SpeechRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.AUDIO_SPEECH).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "audio/*")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = sdk.awaitResponse(httpReq)

        return response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            val bytes = res.body.bytes()
            if (bytes.isEmpty()) throw VeniceSdkException.Protocol("Empty binary response from /audio/speech")
            bytes
        }
    }

    suspend fun queue(apiKey: String, request: QueueAudioRequest): AudioQueueResponse =
        executeJson(apiKey, VeniceEndpoints.AUDIO_QUEUE, QueueAudioRequest.serializer(), request, AudioQueueResponse.serializer())

    suspend fun quote(apiKey: String, request: QuoteAudioRequest): AudioQuoteResponse =
        executeJson(apiKey, VeniceEndpoints.AUDIO_QUOTE, QuoteAudioRequest.serializer(), request, AudioQuoteResponse.serializer())

    suspend fun complete(apiKey: String, request: CompleteAudioRequest): AudioCompleteResponse =
        executeJson(apiKey, VeniceEndpoints.AUDIO_COMPLETE, CompleteAudioRequest.serializer(), request, AudioCompleteResponse.serializer())

    suspend fun retrieve(apiKey: String, request: RetrieveAudioRequest): AudioRetrieveResult {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val httpReq = authenticatedPost(
            apiKey = apiKey,
            endpoint = VeniceEndpoints.AUDIO_RETRIEVE,
            accept = "application/json, audio/mpeg, audio/wav, audio/flac",
            body = json.encodeToString(RetrieveAudioRequest.serializer(), request),
        )

        sdk.awaitResponse(httpReq).use { res ->
            if (!res.isSuccessful) throw sdk.parseHttpError(res)
            val mimeType = res.body.contentType()?.toString()
                ?: throw VeniceSdkException.Protocol("Missing Content-Type from /audio/retrieve")
            if (mimeType.startsWith("application/json")) {
                val status = runCatching {
                    json.decodeFromString(RetrieveAudioResponseStatus.serializer(), res.body.string())
                }.getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /audio/retrieve", it) }
                return if (status.status == "PROCESSING") {
                    AudioRetrieveResult.Processing(
                        status.status,
                        status.averageExecutionTime,
                        status.executionDuration,
                    )
                } else {
                    AudioRetrieveResult.UnknownStatus(
                        status.status,
                        status.averageExecutionTime,
                        status.executionDuration,
                    )
                }
            }
            if (!mimeType.startsWith("audio/")) {
                throw VeniceSdkException.Protocol("Unexpected Content-Type from /audio/retrieve: $mimeType")
            }
            val bytes = res.body.bytes()
            if (bytes.isEmpty()) throw VeniceSdkException.Protocol("Empty binary response from /audio/retrieve")
            return AudioRetrieveResult.CompletedBinary(
                audio = bytes,
                mimeType = mimeType,
                requestId = res.header("x-request-id") ?: res.header("request-id"),
            )
        }
    }

    suspend fun transcribe(apiKey: String, request: AudioTranscriptionRequest): AudioTranscriptionResult {
        require(request.responseFormat == "json" || request.responseFormat == "text") {
            "responseFormat must be json or text"
        }
        val response = executeMultipart(
            apiKey = apiKey,
            endpoint = VeniceEndpoints.AUDIO_TRANSCRIPTIONS,
            accept = if (request.responseFormat == "text") "text/plain" else "application/json",
            file = request.file,
            fields = buildMap {
                request.model?.let { put("model", it) }
                put("response_format", request.responseFormat)
                put("timestamps", request.timestamps.toString())
                request.language?.let { put("language", it) }
            },
        )
        return response.use { res ->
            val body = res.body.string()
            if (request.responseFormat == "text") {
                AudioTranscriptionResult.Text(body)
            } else {
                val parsed = runCatching { json.decodeFromString(AudioTranscriptionJsonResponse.serializer(), body) }
                    .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /audio/transcriptions", it) }
                AudioTranscriptionResult.Json(parsed.text, parsed.duration, parsed.timestamps)
            }
        }
    }

    suspend fun cloneVoice(apiKey: String, request: CloneVoiceRequest): ClonedVoiceResponse {
        val response = executeMultipart(
            apiKey = apiKey,
            endpoint = VeniceEndpoints.AUDIO_VOICES,
            accept = "application/json",
            file = request.file,
            fields = request.model?.let { mapOf("model" to it) }.orEmpty(),
        )
        return response.use { res ->
            runCatching { json.decodeFromString(ClonedVoiceResponse.serializer(), res.body.string()) }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /audio/voices", it) }
        }
    }

    private fun authenticatedPost(apiKey: String, endpoint: String, accept: String, body: String): Request =
        Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", accept)
            .header("User-Agent", sdk.userAgent())
            .post(body.toRequestBody(jsonMedia))
            .build()

    private suspend fun <RequestT, ResponseT> executeJson(
        apiKey: String,
        endpoint: String,
        requestSerializer: kotlinx.serialization.KSerializer<RequestT>,
        request: RequestT,
        responseSerializer: kotlinx.serialization.KSerializer<ResponseT>,
    ): ResponseT {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val httpReq = authenticatedPost(
            apiKey,
            endpoint,
            "application/json",
            json.encodeToString(requestSerializer, request),
        )
        sdk.awaitResponse(httpReq).use { res ->
            if (!res.isSuccessful) throw sdk.parseHttpError(res)
            return runCatching { json.decodeFromString(responseSerializer, res.body.string()) }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /$endpoint", it) }
        }
    }

    private suspend fun executeMultipart(
        apiKey: String,
        endpoint: String,
        accept: String,
        file: AudioFileUpload,
        fields: Map<String, String>,
    ): okhttp3.Response {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(file.fileName.isNotBlank()) { "fileName must not be blank" }
        require(file.bytes.isNotEmpty()) { "audio file must not be empty" }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.fileName, file.bytes.toRequestBody(file.mimeType.toMediaType()))
            .apply { fields.forEach { (name, value) -> addFormDataPart(name, value) } }
            .build()
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", accept)
            .header("User-Agent", sdk.userAgent())
            .post(multipart)
            .build()
        val response = sdk.awaitResponse(httpReq)
        if (!response.isSuccessful) {
            response.use { throw sdk.parseHttpError(it) }
        }
        return response
    }
}
