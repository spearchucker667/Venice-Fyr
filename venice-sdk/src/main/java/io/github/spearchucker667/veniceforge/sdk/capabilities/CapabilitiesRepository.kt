package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Merges the runtime responses of Venice's `/models`, `/models/traits`, and
 * `/models/compatibility_mapping` endpoints into a single typed [ModelCatalog].
 *
 * Conforms strictly to the official Venice API specification:
 * - GET /models returns an array of models with `model_spec` metadata.
 * - GET /models/traits returns a key-value object mapping symbolic traits to model IDs.
 * - GET /models/compatibility_mapping returns a key-value object mapping external model aliases to Venice model IDs.
 */
class CapabilitiesRepository(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
        val models = sdk.listModels(apiKey, null)
        val traitsMap = fetchTraits(apiKey)
        val compatMap = fetchCompatibility(apiKey)

        // Invert compatibility mapping: modelId -> Set of aliases mapped to this model
        val compatibleByModelId = mutableMapOf<String, MutableSet<String>>()
        for ((alias, targetModelId) in compatMap) {
            compatibleByModelId.getOrPut(targetModelId) { mutableSetOf() }.add(alias)
        }

        val merged = models.map { m ->
            val spec = m.modelSpec
            val caps = spec?.capabilities

            val supportsVision = caps?.supportsVision ?: false
            val supportsMultipleImages = caps?.supportsMultipleImages ?: false
            val supportsImageInput = supportsVision || supportsMultipleImages

            val inputModalities = mutableSetOf("text")
            if (supportsImageInput) inputModalities.add("image")
            if (caps?.supportsVideoInput == true) inputModalities.add("video")
            if (caps?.supportsAudioInput == true) inputModalities.add("audio")

            val outputModalities = mutableSetOf("text")
            if (m.type.equals("image", ignoreCase = true)) outputModalities.add("image")
            if (m.type.equals("video", ignoreCase = true)) outputModalities.add("video")
            if (m.type.equals("audio", ignoreCase = true) || m.type.equals("tts", ignoreCase = true) || m.type.equals("music", ignoreCase = true)) {
                outputModalities.add("audio")
            }

            ModelCapabilities(
                id = m.id,
                name = m.name ?: m.id,
                description = m.description,
                type = m.type ?: "text",
                rawJson = m.rawJson,
                supportsImageInput = supportsImageInput,
                supportsVision = supportsVision,
                supportsVideoInput = caps?.supportsVideoInput ?: false,
                supportsAudioInput = caps?.supportsAudioInput ?: false,
                supportsToolCalling = caps?.supportsFunctionCalling ?: false,
                supportsStreaming = spec?.offline != true,
                supportsSystemPrompt = true,
                supportsWebSearch = caps?.supportsWebSearch ?: false,
                supportsXSearch = caps?.supportsXSearch ?: false,
                supportsReasoning = caps?.supportsReasoning ?: false,
                supportsResponseSchema = caps?.supportsResponseSchema ?: false,
                availableContextTokens = spec?.availableContextTokens,
                maxContextTokens = spec?.availableContextTokens?.toInt(),
                maxCompletionTokens = spec?.maxCompletionTokens?.toInt(),
                privacy = spec?.privacy,
                uncensored = spec?.uncensored ?: false,
                offline = spec?.offline ?: false,
                inputModalities = inputModalities,
                outputModalities = outputModalities,
                traits = spec?.traits ?: emptyList(),
                compatibleWith = compatibleByModelId[m.id] ?: emptySet(),
                modelSpec = spec,
            )
        }

        ModelCatalog(
            models = merged,
            traits = traitsMap,
            compatibilityMapping = compatMap,
            refreshedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun fetchTraits(apiKey: String): Map<String, String> {
        val raw = try {
            sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)
        } catch (_: Exception) {
            return emptyMap()
        }
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        val data = root["data"] as? JsonObject ?: return emptyMap()
        return data.mapNotNull { (trait, value) ->
            val modelId = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (modelId != null) trait to modelId else null
        }.toMap()
    }

    private suspend fun fetchCompatibility(apiKey: String): Map<String, String> {
        val raw = try {
            sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)
        } catch (_: Exception) {
            return emptyMap()
        }
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyMap()
        val data = root["data"] as? JsonObject ?: return emptyMap()
        return data.mapNotNull { (alias, value) ->
            val targetModelId = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (targetModelId != null) alias to targetModelId else null
        }.toMap()
    }
}
