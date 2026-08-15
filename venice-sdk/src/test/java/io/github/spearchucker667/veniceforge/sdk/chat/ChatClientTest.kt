package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

    @Test
    fun `emits exactly one terminal finish event when finish_reason is followed by DONE`() = runTest {
        val sse = """
            data: {"choices":[{"index":0,"delta":{"content":"Hello"}}]}
            data: {"choices":[{"index":0,"finish_reason":"stop"}]}
            data: [DONE]
        """.trimIndent() + "\n\n"

        val chunks = client(sse)
            .streamChat(
                apiKey = "test-key",
                request = ChatRequest(model = "test-model", messages = listOf(ChatMessage.user("hi"))),
            )
            .toList()

        val finishEvents = chunks.filterIsInstance<ChatStreamChunk.Finish>()
        assertEquals(1, finishEvents.size)
        assertEquals("stop", finishEvents.first().reason)
    }

    @Test
    fun `parses and preserves multiple tool calls in a single SSE chunk`() = runTest {
        val sse = """
            data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"name":"get_weather","arguments":"{\"loc\":"}},{"index":1,"id":"call_2","function":{"name":"get_time","arguments":"{}"}}]}}]}
            data: {"choices":[{"index":0,"finish_reason":"tool_calls"}]}
            data: [DONE]
        """.trimIndent() + "\n\n"

        val chunks = client(sse)
            .streamChat(
                apiKey = "test-key",
                request = ChatRequest(model = "test-model", messages = listOf(ChatMessage.user("hi"))),
            )
            .toList()

        val toolCalls = chunks.filterIsInstance<ChatStreamChunk.ToolCallDelta>()
        assertEquals(2, toolCalls.size)
        assertEquals(0, toolCalls[0].index)
        assertEquals("call_1", toolCalls[0].callId)
        assertEquals("get_weather", toolCalls[0].name)
        assertEquals("{\"loc\":", toolCalls[0].argumentsFragment)

        assertEquals(1, toolCalls[1].index)
        assertEquals("call_2", toolCalls[1].callId)
        assertEquals("get_time", toolCalls[1].name)
        assertEquals("{}", toolCalls[1].argumentsFragment)

        val finishEvents = chunks.filterIsInstance<ChatStreamChunk.Finish>()
        assertEquals(1, finishEvents.size)
        assertEquals("tool_calls", finishEvents.first().reason)
    }

    @Test
    fun `stream-side provider error emits Error chunk without duplicate success finish`() = runTest {
        val sse = """
            data: {"error":{"message":"Model overloaded","code":429}}
        """.trimIndent() + "\n\n"

        val chunks = client(sse)
            .streamChat(
                apiKey = "test-key",
                request = ChatRequest(model = "test-model", messages = listOf(ChatMessage.user("hi"))),
            )
            .toList()

        val errors = chunks.filterIsInstance<ChatStreamChunk.Error>()
        assertEquals(1, errors.size)
        assertEquals("Model overloaded", errors.first().message)
        assertEquals(429, errors.first().code)

        val finishEvents = chunks.filterIsInstance<ChatStreamChunk.Finish>()
        assertEquals(0, finishEvents.size)
    }

    @Test
    fun `cancel propagates to OkHttp call within deadline`() = runTest {
        val callRef = AtomicReference<Call?>(null)
        val chunk1 = "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n".toByteArray()
        val delivered = AtomicBoolean(false)
        val sseMime = "text/event-stream".toMediaType()

        val stream = object : Source {
            private val delegate = Buffer()
            override fun timeout(): Timeout = Timeout.NONE
            override fun close() {}
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (delivered.compareAndSet(false, true)) {
                    sink.write(chunk1)
                    return chunk1.size.toLong()
                }
                try {
                    Thread.sleep(60_000)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("interrupted", ie)
                }
                return -1L
            }
        }

        val body: ResponseBody = object : ResponseBody() {
            override fun contentType() = sseMime
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = stream.buffer()
        }

        val ok = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                callRef.set(chain.call())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body)
                    .build()
            })
            .build()
        val sdk = VeniceForgeSdk(config = VeniceSdkConfig(), httpClient = ok)
        val client = ChatClient(sdk)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            try {
                client.streamChat(
                    apiKey = "test-key",
                    request = ChatRequest(
                        model = "test-model",
                        messages = listOf(ChatMessage(role = "user", content = "hi")),
                    ),
                ).collect { /* drop */ }
            } catch (e: CancellationException) {
                // expected once the flow is canceled
            }
        }
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            val c = callRef.get()
            if (c != null && c.isCanceled()) break
            Thread.sleep(20)
        }

        val captured: Call? = callRef.get()
        assertNotNull("OkHttp call should have been captured", captured)
        assertTrue(
            "OkHttp call must be canceled once the consumer cancels the flow",
            captured!!.isCanceled(),
        )
    }
}
