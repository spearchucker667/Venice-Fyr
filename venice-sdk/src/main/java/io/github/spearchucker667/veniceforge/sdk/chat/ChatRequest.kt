package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    @SerialName("name") val name: String? = null,
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
    val parameters: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val tools: List<ToolSpec>? = null,
    @SerialName("venice_parameters") val veniceParameters: VeniceParameters? = null,
)

@Serializable
data class VeniceParameters(
    val enable_web_search: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null,
    // Preserve explicit safe_mode=false when caller passed it. Never drop.
)
