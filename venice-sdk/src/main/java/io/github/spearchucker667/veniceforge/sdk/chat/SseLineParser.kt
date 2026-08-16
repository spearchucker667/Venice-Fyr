package io.github.spearchucker667.veniceforge.sdk.chat

import java.io.BufferedReader

class SseLineParser(private val reader: BufferedReader) {
    fun nextData(): String? {
        val dataLines = mutableListOf<String>()

        while (true) {
            val line = reader.readLine()
            if (line == null) {
                return dataLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
            }
            if (line.isEmpty()) {
                if (dataLines.isNotEmpty()) return dataLines.joinToString("\n")
                continue
            }
            if (line.startsWith(":")) continue

            val separator = line.indexOf(':')
            val field = if (separator >= 0) line.substring(0, separator) else line
            if (field != "data") continue

            val rawValue = if (separator >= 0) line.substring(separator + 1) else ""
            dataLines += rawValue.removePrefix(" ")
        }
    }
}
