package io.github.spearchucker667.veniceforge.sdk.video

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class VideoClientTest {
    private fun client(body: ByteArray, mimeType: String, code: Int = 200): VideoClient {
        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Error")
                .header("x-request-id", "req-video")
                .body(body.toResponseBody(mimeType.toMediaType()))
                .build()
        }
        return VideoClient(VeniceForgeSdk(VeniceSdkConfig(), OkHttpClient.Builder().addInterceptor(interceptor).build()))
    }

    private val request = RetrieveVideoRequest("video-model", "queue-id")

    @Test
    fun `retrieve distinguishes processing and remote completion JSON`() = runBlocking {
        fun payload(status: String) = """{"status":"$status","average_execution_time":100.0,"execution_duration":25.0}""".toByteArray()

        assertTrue(client(payload("PROCESSING"), "application/json").retrieve("key", request) is VideoRetrieveResult.Processing)
        assertTrue(client(payload("COMPLETED"), "application/json").retrieve("key", request) is VideoRetrieveResult.CompletedRemote)
        assertTrue(client(payload("PAUSED"), "application/json").retrieve("key", request) is VideoRetrieveResult.UnknownStatus)
    }

    @Test
    fun `retrieve returns binary video with MIME type`() = runBlocking {
        val expected = byteArrayOf(7, 8, 9)
        val result = client(expected, "video/mp4").retrieve("key", request) as VideoRetrieveResult.CompletedBinary
        assertArrayEquals(expected, result.binaryVideo)
        assertEquals("video/mp4", result.mimeType)
        assertEquals("req-video", result.requestId)
    }

    @Test
    fun `retrieve rejects malformed JSON empty binary and HTTP errors`() = runBlocking {
        suspend fun expectProtocol(body: ByteArray, mime: String) {
            try {
                client(body, mime).retrieve("key", request)
                fail("Expected protocol error")
            } catch (_: VeniceSdkException.Protocol) {
            }
        }
        expectProtocol("{".toByteArray(), "application/json")
        expectProtocol(byteArrayOf(), "video/mp4")

        try {
            client("""{"error":"missing"}""".toByteArray(), "application/json", 404).retrieve("key", request)
            fail("Expected HTTP error")
        } catch (e: VeniceSdkException.Http) {
            assertEquals(404, e.statusCode)
        }
    }

    @Test
    fun `queue quote complete and transcription use current response contracts`() = runBlocking {
        val captured = AtomicReference<okhttp3.Request>()
        val interceptor = Interceptor { chain ->
            captured.set(chain.request())
            val body = when (chain.request().url.encodedPath.substringAfterLast('/')) {
                "queue" -> """{"model":"video-model","queue_id":"q1","download_url":"https://download.example/video"}"""
                "quote" -> """{"quote":1.25}"""
                "complete" -> """{"success":true}"""
                else -> """{"transcript":"hello","lang":"en"}"""
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body.toResponseBody("application/json".toMediaType())).build()
        }
        val video = VideoClient(VeniceForgeSdk(VeniceSdkConfig(), OkHttpClient.Builder().addInterceptor(interceptor).build()))

        val queued = video.queue("key", QueueVideoRequest("video-model", "canal", duration = "5s"))
        assertEquals("q1", queued.queueId)
        assertEquals("https://download.example/video", queued.downloadUrl)
        assertEquals(1.25, video.quote("key", QuoteVideoRequest("video-model", "5s")).quote, 0.0)
        assertTrue(video.complete("key", CompleteVideoRequest("video-model", "q1")).success)
        val transcript = video.transcribe("key", VideoTranscriptionRequest("https://example.com/video")) as VideoTranscriptionResult.Json
        assertEquals("hello", transcript.transcript)
        assertEquals("en", transcript.language)
    }
}
