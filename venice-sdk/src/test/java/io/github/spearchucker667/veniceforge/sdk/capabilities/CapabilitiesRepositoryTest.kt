package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesRepositoryTest {

    private fun fixture(name: String): String =
        CapabilitiesRepositoryTest::class.java.getResourceAsStream("/fixtures/models-with-capabilities/$name")!!
            .bufferedReader().readText()

    private fun fakeSdk(): io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk {
        val jsonMedia = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.encodedPath
                val body = when {
                    url.endsWith("/" + VeniceEndpoints.MODELS) -> fixture("models.json")
                    url.endsWith("/" + VeniceEndpoints.MODEL_TRAITS) -> fixture("traits.json")
                    url.endsWith("/" + VeniceEndpoints.MODEL_COMPATIBILITY) -> fixture("compatibility.json")
                    else -> "{}"
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody(jsonMedia))
                    .build()
            })
            .build()
        return io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk(
            config = VeniceSdkConfig(),
            httpClient = client,
        )
    }

    @Test
    fun `combines models, traits and compatibility into one catalog`() = runTest {
        val repo = CapabilitiesRepository(fakeSdk())
        val catalog = repo.fetchLiveCapabilities("test-key")
        val llama = catalog.byId("llama-3.3-70b") ?: error("missing llama")
        assertTrue(llama.supportsToolCalling)
        assertTrue(llama.supportsImageInput)
        assertEquals(setOf("text", "image"), llama.inputModalities)
        assertTrue("deepseek-r1" in llama.compatibleWith)
    }
}
