package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.ModelType
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class CapabilitiesRepositoryTest {

    private fun fixture(name: String): String =
        CapabilitiesRepositoryTest::class.java.getResourceAsStream("/fixtures/models-with-capabilities/$name")!!
            .bufferedReader().readText()

    private fun fakeSdk(
        modelsJson: String = fixture("models.json"),
        traitsJson: String = fixture("traits.json"),
        compatJson: String = fixture("compatibility.json"),
    ): io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk {
        val jsonMedia = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.encodedPath
                val body = when {
                    url.endsWith("/" + VeniceEndpoints.MODELS) -> modelsJson
                    url.endsWith("/" + VeniceEndpoints.MODEL_TRAITS) -> traitsJson
                    url.endsWith("/" + VeniceEndpoints.MODEL_COMPATIBILITY) -> compatJson
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

        // Assert dynamic default model resolution
        assertEquals("llama-3.3-70b", catalog.defaultTextModelId)

        // Assert model capabilities parsed from model_spec
        val llama = catalog.byId("llama-3.3-70b") ?: error("missing llama")
        assertTrue(llama.supportsToolCalling)
        assertTrue(llama.supportsImageInput)
        assertTrue(llama.supportsVision)
        assertTrue(llama.supportsWebSearch)
        assertEquals(setOf("text", "image"), llama.inputModalities)
        assertTrue("gpt-4o" in llama.compatibleWith)
        assertTrue("claude-3-5-sonnet" in llama.compatibleWith)

        // Assert trait resolution
        assertEquals("llama-3.3-70b", catalog.modelForTrait("default")?.id)
        assertEquals("deepseek-r1", catalog.modelForTrait("reasoning")?.id)

        // Assert alias resolution
        assertEquals("llama-3.3-70b", catalog.modelForAlias("gpt-4o")?.id)
        assertEquals("deepseek-r1", catalog.modelForAlias("deepseek-reasoner")?.id)
    }

    @Test
    fun `missing trait falls back to first available text model`() = runTest {
        val emptyTraits = """{"object":"list","type":"text","data":{}}"""
        val repo = CapabilitiesRepository(fakeSdk(traitsJson = emptyTraits))
        val catalog = repo.fetchLiveCapabilities("test-key")

        assertEquals("llama-3.3-70b", catalog.defaultTextModelId)
        assertNull(catalog.modelForTrait("default"))
    }

    @Test
    fun `trait referencing non-existent model is handled safely`() = runTest {
        val orphanTrait = """{"object":"list","type":"text","data":{"default":"non-existent-model"}}"""
        val repo = CapabilitiesRepository(fakeSdk(traitsJson = orphanTrait))
        val catalog = repo.fetchLiveCapabilities("test-key")

        assertEquals("llama-3.3-70b", catalog.defaultTextModelId)
        assertNull(catalog.modelForTrait("default"))
        assertNotNull(catalog.byId("llama-3.3-70b"))
    }

    @Test
    fun `propagates discovered modality to traits and compatibility queries`() = runTest {
        val queries = CopyOnWriteArrayList<String>()
        val models = """{"object":"list","type":"all","data":[{"id":"image-model","type":"image","model_spec":{"name":"Image"}}]}"""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            queries += chain.request().url.toString()
            val body = when {
                chain.request().url.encodedPath.endsWith("/models/traits") -> """{"data":{"default":"image-model"}}"""
                chain.request().url.encodedPath.endsWith("/models/compatibility_mapping") -> """{"data":{"image-alias":"image-model"}}"""
                else -> models
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val catalog = CapabilitiesRepository(
            io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk(VeniceSdkConfig(), client),
        ).fetchLiveCapabilities("key")

        assertEquals("image-model", catalog.modelForTrait("image:default")?.id)
        assertEquals("image-model", catalog.defaultModelIdFor(ModelType.IMAGE))
        assertEquals("image-model", catalog.modelForAlias("image:image-alias")?.id)
        assertTrue(queries.any { it.endsWith("models/traits?type=image") })
        assertTrue(queries.any { it.endsWith("models/compatibility_mapping?type=image") })
    }
}
