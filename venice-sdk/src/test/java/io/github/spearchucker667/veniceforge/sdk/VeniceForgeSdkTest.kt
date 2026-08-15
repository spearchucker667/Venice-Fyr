package io.github.spearchucker667.veniceforge.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class VeniceForgeSdkTest {

    private val jsonMedia = "application/json".toMediaType()

    @Test
    fun `listModels without type parameter omits type query from URL`() = runTest {
        val capturedUrl = AtomicReference<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                capturedUrl.set(chain.request().url.toString())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"object":"list","type":"text","data":[]}""".toResponseBody(jsonMedia))
                    .build()
            })
            .build()

        val sdk = VeniceForgeSdk(httpClient = client)
        sdk.listModels(apiKey = "secret-key-12345", type = null)

        val url = capturedUrl.get()
        assertNotNull(url)
        assertTrue(url.endsWith("/models"))
        assertFalse("URL must not contain type query when omitted", url.contains("type="))
    }

    @Test
    fun `listModels with ModelType IMAGE appends type=image query`() = runTest {
        val capturedUrl = AtomicReference<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                capturedUrl.set(chain.request().url.toString())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"object":"list","type":"image","data":[]}""".toResponseBody(jsonMedia))
                    .build()
            })
            .build()

        val sdk = VeniceForgeSdk(httpClient = client)
        sdk.listModels(apiKey = "secret-key-12345", type = ModelType.IMAGE)

        val url = capturedUrl.get()
        assertNotNull(url)
        assertTrue(url.contains("/models?type=image"))
    }

    @Test
    fun `HTTP 429 returns structured RateLimit exception with headers`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .header("x-request-id", "req-test-429")
                    .header("x-ratelimit-limit-requests", "100")
                    .header("x-ratelimit-remaining-requests", "0")
                    .header("x-ratelimit-reset-requests", "1700000030")
                    .header("retry-after", "30")
                    .body("""{"error":"Rate limit exceeded for tier"}""".toResponseBody(jsonMedia))
                    .build()
            })
            .build()

        val sdk = VeniceForgeSdk(httpClient = client)
        val apiKey = "super-secret-api-key-xyz"

        try {
            sdk.listModels(apiKey)
            org.junit.Assert.fail("Expected VeniceSdkException.RateLimit")
        } catch (e: VeniceSdkException.RateLimit) {
            assertEquals(429, e.statusCode)
            assertEquals("req-test-429", e.requestId)
            assertEquals(30L, e.retryAfterSeconds)
            assertEquals(100L, e.rateLimitInfo?.limitRequests)
            assertEquals(0L, e.rateLimitInfo?.remainingRequests)
            assertFalse("Exception message must never contain API keys", e.message?.contains(apiKey) == true)
        }
    }

    @Test
    fun `HTTP 401 returns structured Authentication exception without leaking key`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .header("x-request-id", "req-auth-fail")
                    .body("""{"error":"Authentication failed - invalid key"}""".toResponseBody(jsonMedia))
                    .build()
            })
            .build()

        val sdk = VeniceForgeSdk(httpClient = client)
        val apiKey = "secret-token-abcdef"

        try {
            sdk.listModels(apiKey)
            org.junit.Assert.fail("Expected VeniceSdkException.Authentication")
        } catch (e: VeniceSdkException.Authentication) {
            assertEquals(401, e.statusCode)
            assertEquals("req-auth-fail", e.requestId)
            assertEquals("Authentication failed - invalid key", e.safeMessage)
            assertFalse("Exception message must never contain API keys", e.message?.contains(apiKey) == true)
        }
    }

    @Test
    fun `HTTP 400 returns structured Validation exception with details`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .header("x-request-id", "req-val-fail")
                    .body("""{"error":"Invalid request parameters","details":{"field":{"_errors":["Required"]}}}""".toResponseBody(jsonMedia))
                    .build()
            })
            .build()

        val sdk = VeniceForgeSdk(httpClient = client)
        try {
            sdk.listModels("test-key")
            org.junit.Assert.fail("Expected VeniceSdkException.Validation")
        } catch (e: VeniceSdkException.Validation) {
            assertEquals(400, e.statusCode)
            assertEquals("req-val-fail", e.requestId)
            assertEquals("Invalid request parameters", e.safeMessage)
            assertNotNull(e.validationDetails)
        }
    }
}
