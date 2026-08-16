package io.github.spearchucker667.veniceforge.sdk.audio

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.MultipartBody
import java.util.concurrent.atomic.AtomicReference

class AudioClientTest {
    private fun client(requestRef: AtomicReference<okhttp3.Request> = AtomicReference(), responder: (okhttp3.Request) -> Pair<String, ByteArray>): AudioClient {
        val interceptor = Interceptor { chain ->
            requestRef.set(chain.request())
            val (mime, body) = responder(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody(mime.toMediaType()))
                .build()
        }
        return AudioClient(VeniceForgeSdk(VeniceSdkConfig(), OkHttpClient.Builder().addInterceptor(interceptor).build()))
    }

    @Test
    fun `queue quote and complete use typed contracts`() = runBlocking {
        val requestRef = AtomicReference<okhttp3.Request>()
        val audio = client(requestRef) { request ->
            val json = when (request.url.encodedPath.substringAfterLast('/')) {
                "queue" -> """{"model":"music","queue_id":"q1","status":"QUEUED"}"""
                "quote" -> """{"quote":0.75}"""
                else -> """{"success":true}"""
            }
            "application/json" to json.toByteArray()
        }

        assertEquals("q1", audio.queue("key", QueueAudioRequest("music", "warm ambient", durationSeconds = "60")).queueId)
        assertEquals(0.75, audio.quote("key", QuoteAudioRequest("music", durationSeconds = "60")).quote, 0.0)
        assertTrue(audio.complete("key", CompleteAudioRequest("music", "q1")).success)
    }

    @Test
    fun `retrieve distinguishes processing and binary completion`() = runBlocking {
        val processing = client { "application/json" to """{"status":"PROCESSING","average_execution_time":20000,"execution_duration":5200}""".toByteArray() }
            .retrieve("key", RetrieveAudioRequest("music", "q1"))
        assertTrue(processing is AudioRetrieveResult.Processing)

        val expected = byteArrayOf(1, 4, 9)
        val completed = client { "audio/mpeg" to expected }
            .retrieve("key", RetrieveAudioRequest("music", "q1")) as AudioRetrieveResult.CompletedBinary
        assertArrayEquals(expected, completed.audio)
        assertEquals("audio/mpeg", completed.mimeType)
    }

    @Test
    fun `transcription and voice cloning use multipart file uploads`() = runBlocking {
        val requestRef = AtomicReference<okhttp3.Request>()
        val audio = client(requestRef) { request ->
            val response = if (request.url.encodedPath.endsWith("/voices")) {
                """{"id":"vv_test"}"""
            } else {
                """{"text":"hello","duration":1.5}"""
            }
            "application/json" to response.toByteArray()
        }
        val file = AudioFileUpload("voice.wav", "audio/wav", byteArrayOf(1, 2, 3))

        val transcript = audio.transcribe("key", AudioTranscriptionRequest(file, model = "openai/whisper-large-v3")) as AudioTranscriptionResult.Json
        assertEquals("hello", transcript.text)
        assertTrue(requestRef.get().body is MultipartBody)

        val voice = audio.cloneVoice("key", CloneVoiceRequest(file, "tts-chatterbox-hd"))
        assertEquals("vv_test", voice.id)
        assertNull(voice.model)
        assertTrue(requestRef.get().body is MultipartBody)
    }
}
