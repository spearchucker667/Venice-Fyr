package io.github.spearchucker667.veniceforge.sdk.chat

import java.io.BufferedReader

class SseLineParser(private val reader: BufferedReader) {
    fun nextData(): String? {
        val line = reader.readLine() ?: return null
        if (line.isEmpty()) return nextData()
        if (line.startsWith(":")) return nextData()
        if (line.startsWith("data:")) return line.removePrefix("data:").trim()
        // Other SSE fields (event:, id:) are ignored for /chat/completions.
        return nextData()
    }
}
