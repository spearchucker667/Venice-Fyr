package io.github.spearchucker667.veniceforge.sdk.chat

class ChatStreamAccumulator {
    private val text = StringBuilder()
    private val toolCalls = mutableMapOf<Int, MutableToolCall>()
    var finishedReason: String? = null
        private set
    var lastError: ChatStreamChunk.Error? = null
        private set

    fun apply(chunk: ChatStreamChunk) {
        when (chunk) {
            is ChatStreamChunk.Delta -> {
                if (!chunk.textFragment.isNullOrEmpty()) text.append(chunk.textFragment)
            }
            is ChatStreamChunk.ToolCallDelta -> {
                val tc = toolCalls.getOrPut(chunk.index) { MutableToolCall() }
                chunk.callId?.let { tc.id = it }
                chunk.name?.let { tc.name = it }
                if (!chunk.argumentsFragment.isNullOrEmpty()) tc.arguments.append(chunk.argumentsFragment)
            }
            is ChatStreamChunk.Finish -> { finishedReason = chunk.reason }
            is ChatStreamChunk.Error -> { lastError = chunk }
            is ChatStreamChunk.Open -> { /* nothing */ }
        }
    }

    fun snapshot(): AssistantMessage = AssistantMessage(
        text = text.toString(),
        toolCalls = toolCalls.entries
            .sortedBy { it.key }
            .map { (_, v) ->
                ToolCall(v.id, v.name, v.arguments.toString())
            },
        finishedReason = finishedReason,
        error = lastError,
    )

    private data class MutableToolCall(var id: String? = null, var name: String? = null) {
        val arguments = StringBuilder()
    }

    data class AssistantMessage(
        val text: String,
        val toolCalls: List<ToolCall>,
        val finishedReason: String?,
        val error: ChatStreamChunk.Error?,
    )

    data class ToolCall(val id: String?, val name: String?, val argumentsJson: String)
}
