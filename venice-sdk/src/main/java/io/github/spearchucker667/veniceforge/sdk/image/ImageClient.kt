package io.github.spearchucker667.veniceforge.sdk.image

import io.github.spearchucker667.veniceforge.sdk.BinaryMediaResult
import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ImageClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun generate(apiKey: String, request: GenerateImageRequest): GenerateImageResponse =
        executeRequest(apiKey, VeniceEndpoints.IMAGE_GENERATE, json.encodeToString(GenerateImageRequest.serializer(), request))

    suspend fun generateBinary(apiKey: String, request: GenerateImageRequest): ByteArray {
        require(request.returnBinary == true) { "returnBinary must be true for generateBinary" }
        return executeBinaryRequest(apiKey, VeniceEndpoints.IMAGE_GENERATE, json.encodeToString(GenerateImageRequest.serializer(), request)).bytes
    }

    suspend fun upscale(apiKey: String, request: UpscaleImageRequest): BinaryMediaResult =
        executeBinaryRequest(apiKey, VeniceEndpoints.IMAGE_UPSCALE, json.encodeToString(UpscaleImageRequest.serializer(), request))

    suspend fun edit(apiKey: String, request: EditImageRequest): BinaryMediaResult =
        executeBinaryRequest(apiKey, VeniceEndpoints.IMAGE_EDIT, json.encodeToString(EditImageRequest.serializer(), request))

    suspend fun multiEdit(apiKey: String, request: MultiEditImageRequest): BinaryMediaResult =
        executeBinaryRequest(apiKey, VeniceEndpoints.IMAGE_MULTI_EDIT, json.encodeToString(MultiEditImageRequest.serializer(), request))

    private suspend fun executeRequest(apiKey: String, endpoint: String, reqBody: String): GenerateImageResponse {
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
            val body = res.body.string()
            runCatching { json.decodeFromString(GenerateImageResponse.serializer(), body) }
                .getOrElse { throw VeniceSdkException.Protocol("Invalid JSON from /$endpoint", it) }
        }
    }

    private suspend fun executeBinaryRequest(apiKey: String, endpoint: String, reqBody: String): BinaryMediaResult {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(endpoint).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "image/*")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val response = sdk.awaitResponse(httpReq)

        return response.use { res ->
            if (!res.isSuccessful) {
                throw sdk.parseHttpError(res)
            }
            val mimeType = res.body.contentType()?.toString()
                ?: throw VeniceSdkException.Protocol("Missing Content-Type from /$endpoint")
            if (!mimeType.startsWith("image/")) {
                throw VeniceSdkException.Protocol("Unexpected Content-Type from /$endpoint: $mimeType")
            }
            val bytes = res.body.bytes()
            if (bytes.isEmpty()) {
                throw VeniceSdkException.Protocol("Empty binary response from /$endpoint")
            }
            BinaryMediaResult(
                bytes = bytes,
                mimeType = mimeType,
                requestId = res.header("x-request-id") ?: res.header("request-id"),
                balanceRemaining = res.header("X-Balance-Remaining"),
            )
        }
    }
}
