package io.github.spearchucker667.veniceforge.sdk.image

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.io.IOException

class ImageClientTest {

    private fun createClient(mockResponseJson: String, requestRef: AtomicReference<okhttp3.Request>): ImageClient {
        val interceptor = Interceptor { chain ->
            requestRef.set(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockResponseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val sdk = VeniceForgeSdk(
            config = VeniceSdkConfig(baseUrl = "https://api.venice.ai/api/v1/"),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        )
        return ImageClient(sdk)
    }

    @Test
    fun `generate maps request and response correctly`() = runBlocking {
        val mockResponseJson = """
            {
                "id": "gen-123",
                "images": ["base64string1", "base64string2"],
                "timing": {
                    "inferenceDuration": 12.3,
                    "inferencePreprocessingTime": 0.5,
                    "inferenceQueueTime": 1.0,
                    "total": 13.8
                }
            }
        """.trimIndent()

        val requestRef = AtomicReference<okhttp3.Request>()
        val client = createClient(mockResponseJson, requestRef)

        val req = GenerateImageRequest(
            model = "test-model",
            prompt = "A cute cat",
            height = 512,
            width = 512,
            returnBinary = false
        )

        val res = client.generate("test-key", req)

        val recordedRequest = requestRef.get()
        assertEquals("POST", recordedRequest.method)
        assertEquals("https://api.venice.ai/api/v1/image/generate", recordedRequest.url.toString())
        assertEquals("Bearer test-key", recordedRequest.header("Authorization"))

        assertEquals("gen-123", res.id)
        assertEquals(2, res.images?.size)
        assertEquals("base64string1", res.images?.get(0))
        assertNotNull(res.timing)
        assertEquals(12.3, res.timing?.inferenceDuration)
    }

    @Test
    fun `edit maps binary response correctly`() = runBlocking {
        val requestRef = AtomicReference<okhttp3.Request>()
        val expected = byteArrayOf(1, 2, 3)
        val interceptor = Interceptor { chain ->
            requestRef.set(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("x-request-id", "req-edit")
                .body(expected.toResponseBody("image/png".toMediaType()))
                .build()
        }
        val client = ImageClient(
            VeniceForgeSdk(
                config = VeniceSdkConfig(baseUrl = "https://api.venice.ai/api/v1/"),
                httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            ),
        )

        val req = EditImageRequest(
            image = "input-base64",
            prompt = "make it blue",
            model = "edit-model"
        )

        val res = client.edit("test-key", req)

        val recordedRequest = requestRef.get()
        assertEquals("POST", recordedRequest.method)
        assertEquals("https://api.venice.ai/api/v1/image/edit", recordedRequest.url.toString())

        assertArrayEquals(expected, res.bytes)
        assertEquals("image/png", res.mimeType)
        assertEquals("req-edit", res.requestId)
        assertEquals("image/*", recordedRequest.header("Accept"))
    }

    @Test
    fun `multi-edit and upscale map binary responses correctly`() = runBlocking {
        val paths = mutableListOf<String>()
        val expected = byteArrayOf(4, 5, 6)
        val interceptor = Interceptor { chain ->
            paths += chain.request().url.encodedPath
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("x-request-id", "req-binary")
                .header("X-Balance-Remaining", "12.5")
                .body(expected.toResponseBody("image/webp".toMediaType()))
                .build()
        }
        val client = ImageClient(
            VeniceForgeSdk(
                config = VeniceSdkConfig(baseUrl = "https://api.venice.ai/api/v1/"),
                httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            ),
        )

        val multi = client.multiEdit("key", MultiEditImageRequest(images = listOf("one", "two"), prompt = "merge"))
        val upscale = client.upscale("key", UpscaleImageRequest(image = "one"))

        assertEquals(listOf("/api/v1/image/multi-edit", "/api/v1/image/upscale"), paths)
        listOf(multi, upscale).forEach { result ->
            assertArrayEquals(expected, result.bytes)
            assertEquals("image/webp", result.mimeType)
            assertEquals("req-binary", result.requestId)
            assertEquals("12.5", result.balanceRemaining)
        }
    }

    @Test
    fun `binary endpoint rejects empty body`() = runBlocking {
        val requestRef = AtomicReference<okhttp3.Request>()
        val interceptor = Interceptor { chain ->
            requestRef.set(chain.request())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(byteArrayOf().toResponseBody("image/png".toMediaType()))
                .build()
        }
        val client = ImageClient(VeniceForgeSdk(httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()))
        try {
            client.upscale("key", UpscaleImageRequest("input"))
            fail("Expected protocol error")
        } catch (e: VeniceSdkException.Protocol) {
            assertTrue(e.message.orEmpty().contains("Empty binary response"))
        }
    }

    @Test
    fun `cancellation cancels the active binary image call`() = runBlocking {
        val entered = CountDownLatch(1)
        val canceled = AtomicBoolean(false)
        val interceptor = Interceptor { chain ->
            entered.countDown()
            try {
                while (!chain.call().isCanceled()) Thread.sleep(5)
                throw IOException("canceled")
            } finally {
                canceled.set(chain.call().isCanceled())
            }
        }
        val client = ImageClient(VeniceForgeSdk(httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()))
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            client.edit("key", EditImageRequest("input", "prompt"))
        }

        assertTrue(entered.await(3, TimeUnit.SECONDS))
        job.cancelAndJoin()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!canceled.get() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue("OkHttp call was not canceled", canceled.get())
    }
}
