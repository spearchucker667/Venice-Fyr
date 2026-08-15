package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatClientTest {
    private fun load(name: String): String =
        ChatClientTest::class.java.getResourceAsStream("/fixtures/chat-stream/$name")!!
            .bufferedReader().readText()

    private fun client(payload: String): ChatClient {
        val sseMime = "text/event-stream".toMediaType()
        val ok = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(payload.toResponseBody(sseMime))
                    .build()
            })
            .build()
        val sdk = VeniceForgeSdk(config = VeniceSdkConfig(), httpClient = ok)
        return ChatClient(sdk)
    }

    @Test
    fun `streams chat completions into chunks`() = runTest {
        val chunks = client(load("stream-good.sse"))
            .streamChat(
                apiKey = "test-key",
                request = ChatRequest(model = "llama-3.3-70b", messages = listOf(ChatMessage(role = "user", content = "hi"))),
            )
            .toList()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.any { it is ChatStreamChunk.Delta })
        assertTrue(chunks.last() is ChatStreamChunk.Finish)
    }
}
