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
                // Stall the body so the only way out is via cooperative cancellation:
                // the producer must observe the consumer's cancel and forward it into
                // OkHttp call.cancel() through awaitClose.
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
            // Swallow CancellationException: cancel() throws and we expect it.
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
        // Give the producer a chance to read the first chunk and enter the next iteration
        // before we cancel; the inter-iteration suspension point is what delivers the
        // cancellation back into awaitClose.
        advanceUntilIdle()

        job.cancel()
        advanceUntilIdle()

        // Real-time poll for cancellation to propagate: awaitClose fires only after the
        // producer observes the consumer's cancellation, then call.cancel() runs.
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
