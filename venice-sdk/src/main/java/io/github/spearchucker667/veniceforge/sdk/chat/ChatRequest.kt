package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Message object within a Venice /chat/completions request or response.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    @SerialName("reasoning_details") val reasoningDetails: JsonElement? = null,
    @SerialName("thought_signature") val thoughtSignature: String? = null,
) {
    companion object {
        fun user(content: String, name: String? = null) = ChatMessage("user", content, name)
        fun assistant(content: String?, toolCalls: List<ToolCall>? = null) = ChatMessage("assistant", content, null, toolCalls)
        fun system(content: String, name: String? = null) = ChatMessage("system", content, name)
        fun tool(toolCallId: String, content: String) = ChatMessage(role = "tool", content = content, toolCallId = toolCallId)
    }
}

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: ToolFunction,
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null,
)

@Serializable
enum class ReasoningEffort {
    @SerialName("none") NONE,
    @SerialName("minimal") MINIMAL,
    @SerialName("low") LOW,
    @SerialName("medium") MEDIUM,
    @SerialName("high") HIGH,
    @SerialName("xhigh") XHIGH,
    @SerialName("max") MAX,
}

@Serializable
enum class ReasoningSummary {
    @SerialName("auto") AUTO,
    @SerialName("concise") CONCISE,
    @SerialName("detailed") DETAILED,
}

@Serializable
data class ReasoningConfig(
    val enabled: Boolean? = null,
    val effort: ReasoningEffort? = null,
    val summary: ReasoningSummary? = null,
)

/**
 * Request payload for POST /chat/completions.
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null,
    val reasoning: ReasoningConfig? = null,
    @SerialName("reasoning_effort") val reasoningEffort: ReasoningEffort? = null,
    val tools: List<ToolSpec>? = null,
    @SerialName("venice_parameters") val veniceParameters: VeniceParameters? = null,
)

/**
 * Venice-specific operational parameters passed inside `venice_parameters`.
 */
@Serializable
data class VeniceParameters(
    @SerialName("enable_web_search") val enableWebSearch: String? = null, // "auto", "on", "off"
    @SerialName("enable_web_scraping") val enableWebScraping: Boolean? = null,
    @SerialName("enable_web_citations") val enableWebCitations: Boolean? = null,
    @SerialName("enable_x_search") val enableXSearch: Boolean? = null,
    @SerialName("character_slug") val characterSlug: String? = null,
    @SerialName("include_venice_system_prompt") val includeVeniceSystemPrompt: Boolean? = null,
    @SerialName("strip_thinking_response") val stripThinkingResponse: Boolean? = null,
    @SerialName("disable_thinking") val disableThinking: Boolean? = null,
    @SerialName("include_search_results_in_stream") val includeSearchResultsInStream: Boolean? = null,
)
