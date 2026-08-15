package io.github.spearchucker667.veniceforge.core.common

/**
 * Central log/diagnostics redaction. Do not log request/response bodies by default.
 * This mirrors the desktop rule that API keys, bearer credentials, and obvious
 * local paths must never appear in diagnostics exports.
 */
object Redactor {
    private val bearer = Regex("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*")
    private val apiKey = Regex("(?i)\\b(?:sk|vn)-[A-Za-z0-9._-]{8,}\\b")
    private val unixPath = Regex("(?<![A-Za-z0-9])/(?:Users|home|data/user/\\d+|storage/emulated/\\d+)(?:/[^\\s]+)+")

    fun redact(value: String): String = value
        .replace(bearer, "Bearer [REDACTED]")
        .replace(apiKey, "[REDACTED_API_KEY]")
        .replace(unixPath, "[REDACTED_LOCAL_PATH]")
}
