package io.github.spearchucker667.veniceforge.sdk

import kotlinx.serialization.Serializable

/**
 * Model representation parsed from Venice GET /models response.
 */
data class VeniceModel(
    val id: String,
    val objectType: String? = "model",
    val created: Long? = null,
    val ownedBy: String? = "venice.ai",
    val type: String? = "text",
    val name: String? = null,
    val description: String? = null,
    val rawJson: String = "",
    val modelSpec: ModelSpec? = null,
)

/**
 * Detailed specification metadata nested inside Venice GET /models response (model_spec).
 */
@Serializable
data class ModelSpec(
    val name: String? = null,
    val description: String? = null,
    val modelSource: String? = null,
    val availableContextTokens: Long? = null,
    val maxCompletionTokens: Long? = null,
    val privacy: String? = null,
    val uncensored: Boolean? = null,
    val offline: Boolean? = null,
    val beta: Boolean? = null,
    val betaModel: Boolean? = null,
    val traits: List<String> = emptyList(),
    val capabilities: ModelCapabilitiesSpec? = null,
)

/**
 * Fine-grained capabilities exposed inside model_spec.capabilities.
 */
@Serializable
data class ModelCapabilitiesSpec(
    val supportsVision: Boolean = false,
    val supportsMultipleImages: Boolean = false,
    val supportsVideoInput: Boolean = false,
    val supportsAudioInput: Boolean = false,
    val supportsFunctionCalling: Boolean = false,
    val supportsWebSearch: Boolean = false,
    val supportsXSearch: Boolean = false,
    val supportsReasoning: Boolean = false,
    val supportsReasoningEffort: Boolean = false,
    val supportsResponseSchema: Boolean = false,
    val supportsLogProbs: Boolean = false,
    val supportsTeeAttestation: Boolean = false,
    val supportsE2EE: Boolean = false,
    val optimizedForCode: Boolean = false,
)
