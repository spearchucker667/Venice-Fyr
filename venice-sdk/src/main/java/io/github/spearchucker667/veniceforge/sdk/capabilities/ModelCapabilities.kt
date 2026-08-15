package io.github.spearchucker667.veniceforge.sdk.capabilities

/**
 * Typed, runtime-discovered Venice model capabilities. The Venice server defines a
 * superset of these flags and may add more at any time; fields not represented here
 * remain accessible verbatim through [rawJson] so downstream consumers can read them
 * without losing fidelity when the server gains new capability bits.
 */
data class ModelCapabilities(
    val id: String,
    val name: String?,
    val description: String?,
    val rawJson: String,
    val supportsImageInput: Boolean = false,
    val supportsToolCalling: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsSystemPrompt: Boolean = true,
    val maxContextTokens: Int? = null,
    val inputModalities: Set<String> = setOf("text"),
    val outputModalities: Set<String> = setOf("text"),
    val traits: Map<String, String> = emptyMap(),
    val compatibleWith: Set<String> = emptySet(),
)
