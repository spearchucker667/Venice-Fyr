package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.Serializable

sealed class ChatStreamChunk {
    data class Open(val id: String? = null) : ChatStreamChunk()
    data class Delta(val index: Int, val textFragment: String?) : ChatStreamChunk()
    data class ToolCallDelta(
        val index: Int,
        val callId: String?,
        val name: String?,
        val argumentsFragment: String?,
    ) : ChatStreamChunk()
    data class Finish(val reason: String, val usage: Usage? = null) : ChatStreamChunk()
    data class Error(val code: Int?, val message: String) : ChatStreamChunk()

    @Serializable
    data class Usage(
        val prompt_tokens: Int? = null,
        val completion_tokens: Int? = null,
        val total_tokens: Int? = null,
    )
}
