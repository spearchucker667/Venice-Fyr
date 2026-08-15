package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.ModelSpec

/**
 * Typed, runtime-discovered Venice model capabilities.
 *
 * Derived from the authoritative GET /models response and model_spec metadata,
 * combined with runtime traits and compatibility mappings.
 */
data class ModelCapabilities(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val type: String = "text",
    val rawJson: String = "",
    val supportsImageInput: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsVideoInput: Boolean = false,
    val supportsAudioInput: Boolean = false,
    val supportsToolCalling: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsSystemPrompt: Boolean = true,
    val supportsWebSearch: Boolean = false,
    val supportsXSearch: Boolean = false,
    val supportsReasoning: Boolean = false,
    val supportsResponseSchema: Boolean = false,
    val availableContextTokens: Long? = null,
    val maxContextTokens: Int? = null,
    val maxCompletionTokens: Int? = null,
    val privacy: String? = null,
    val uncensored: Boolean = false,
    val offline: Boolean = false,
    val inputModalities: Set<String> = setOf("text"),
    val outputModalities: Set<String> = setOf("text"),
    val traits: List<String> = emptyList(),
    val compatibleWith: Set<String> = emptySet(),
    val modelSpec: ModelSpec? = null,
) {
    /**
     * True if this model can participate in standard text/code chat completions.
     */
    val supportsTextChat: Boolean
        get() = type.equals("text", ignoreCase = true) || type.equals("code", ignoreCase = true)

    /**
     * True if this model can participate in image generation.
     */
    val supportsImageGeneration: Boolean
        get() = type.equals("image", ignoreCase = true)
}
