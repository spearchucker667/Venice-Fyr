package io.github.spearchucker667.veniceforge.sdk.audio

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

class AudioClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val jsonMedia = "application/json".toMediaType()

    suspend fun speech(apiKey: String, request: SpeechRequest): ByteArray = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }

        val reqBody = json.encodeToString(SpeechRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.AUDIO_SPEECH).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "audio/*")
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
            res.body?.bytes() ?: throw VeniceSdkException.Protocol("Empty binary response from /audio/speech", null)
        }
    }
}
