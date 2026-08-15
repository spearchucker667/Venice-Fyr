package io.github.spearchucker667.veniceforge.sdk.image

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

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
    fun `edit maps request correctly`() = runBlocking {
        val mockResponseJson = """
            {
                "id": "edit-123",
                "images": ["edited-base64"]
            }
        """.trimIndent()

        val requestRef = AtomicReference<okhttp3.Request>()
        val client = createClient(mockResponseJson, requestRef)

        val req = EditImageRequest(
            image = "input-base64",
            prompt = "make it blue",
            model = "edit-model"
        )

        val res = client.edit("test-key", req)

        val recordedRequest = requestRef.get()
        assertEquals("POST", recordedRequest.method)
        assertEquals("https://api.venice.ai/api/v1/image/edit", recordedRequest.url.toString())
        
        assertEquals("edit-123", res.id)
        assertEquals(1, res.images?.size)
    }
}
