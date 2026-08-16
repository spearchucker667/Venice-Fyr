package io.github.spearchucker667.veniceforge.sdk.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatStreamAccumulatorTest {
    @Test
    fun `accumulates text deltas into single string`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Delta(0, "Hello"))
        acc.apply(ChatStreamChunk.Delta(0, ", "))
        acc.apply(ChatStreamChunk.Delta(0, "world!"))
        assertEquals("Hello, world!", acc.snapshot().text)
    }

    @Test
    fun `accumulates reasoning separately from answer text`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.ReasoningDelta(0, "think "))
        acc.apply(ChatStreamChunk.ReasoningDelta(0, "carefully"))
        acc.apply(ChatStreamChunk.Delta(0, "answer"))

        assertEquals("think carefully", acc.snapshot().reasoning)
        assertEquals("answer", acc.snapshot().text)
    }

    @Test
    fun `reconstructs tool call across fragmented deltas`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.ToolCallDelta(0, "call_1", "web_search", null))
        acc.apply(ChatStreamChunk.ToolCallDelta(0, null, null, "{\"q\":\"\""))
        acc.apply(ChatStreamChunk.ToolCallDelta(0, null, null, "}"))

        val snap = acc.snapshot()
        assertEquals(1, snap.toolCalls.size)
        assertEquals("call_1", snap.toolCalls[0].id)
        assertEquals("web_search", snap.toolCalls[0].name)
        assertEquals("{\"q\":\"\"}", snap.toolCalls[0].argumentsJson)
    }

    @Test
    fun `finish chunk sets finished reason`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Finish(reason = "stop"))
        assertEquals("stop", acc.finishedReason)
    }

    @Test
    fun `error chunk sets last error but does not clobber text`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Delta(0, "partial"))
        acc.apply(ChatStreamChunk.Error(429, "rate limited"))
        assertEquals("partial", acc.snapshot().text)
        assertEquals(429, acc.lastError?.code)
    }
}
