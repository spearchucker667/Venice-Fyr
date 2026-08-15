package io.github.spearchucker667.veniceforge.sdk.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class SseLineParserTest {
    @Test
    fun `parses data lines and skips comments`() {
        val input = """
            : heartbeat
            data: {"id":"abc"}

            data: {"delta":"hi"}
            data: [DONE]
        """.trimIndent()
        val reader = BufferedReader(StringReader(input))
        val parser = SseLineParser(reader)
        assertEquals("{\"id\":\"abc\"}", parser.nextData())
        assertEquals("{\"delta\":\"hi\"}", parser.nextData())
        assertEquals("[DONE]", parser.nextData())
        assertNull(parser.nextData())
    }

    @Test
    fun `yields null on blank stream when done`() {
        val parser = SseLineParser(BufferedReader(StringReader("")))
        assertNull(parser.nextData())
    }
}
