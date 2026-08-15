package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Merges the runtime responses of Venice's `/models`, `/models/traits`, and
 * `/models/compatibility_mapping` endpoints into a single typed [ModelCatalog].
 *
 * The repository is intentionally credential-less at rest: callers supply the
 * [apiKey] per fetch and the SDK never persists it.
 */
class CapabilitiesRepository(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
        val models = fetchModels(apiKey)
        val traits = fetchTraits(apiKey)
        val compat = fetchCompatibility(apiKey)

        val traitById = traits.associateBy { it.id.orEmpty() }
        val compatibleById = compat
            .groupBy { it.id.orEmpty() }
            .mapValues { (_, list) -> list.flatMap { it.compatibleWith.orEmpty() }.toSet() }

        val merged = models.map { m ->
            val t = traitById[m.id]
            ModelCapabilities(
                id = m.id,
                name = m.name,
                description = m.description,
                rawJson = m.rawJson,
                supportsImageInput = t?.supportsImageInput ?: false,
                supportsToolCalling = t?.supportsToolCalling ?: false,
                supportsStreaming = t?.supportsStreaming ?: true,
                supportsSystemPrompt = t?.supportsSystemPrompt ?: true,
                maxContextTokens = t?.maxContextTokens,
                inputModalities = t?.inputModalities ?: setOf("text"),
                outputModalities = t?.outputModalities ?: setOf("text"),
                traits = t?.traits ?: emptyMap(),
                compatibleWith = compatibleById[m.id] ?: emptySet(),
            )
        }
        ModelCatalog(models = merged, refreshedAt = System.currentTimeMillis())
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private data class RawModel(
        val id: String,
        val name: String?,
        val description: String?,
        val rawJson: String,
    )

    private data class RawTraits(
        val id: String?,
        val supportsImageInput: Boolean,
        val supportsToolCalling: Boolean,
        val supportsStreaming: Boolean,
        val supportsSystemPrompt: Boolean,
        val maxContextTokens: Int?,
        val inputModalities: Set<String>,
        val outputModalities: Set<String>,
        val traits: Map<String, String>,
    )

    private data class RawCompat(val id: String?, val compatibleWith: List<String>?)

    private suspend fun fetchModels(apiKey: String): List<RawModel> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODELS}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = obj.str("id") ?: return@mapNotNull null
            val spec = obj["model_spec"] as? JsonObject
            RawModel(
                id = id,
                name = spec?.str("name"),
                description = spec?.str("description"),
                rawJson = obj.toString(),
            )
        }
    }

    private suspend fun fetchTraits(apiKey: String): List<RawTraits> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = obj.str("id")
            val modArr = (obj["input_modalities"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }?.toSet() ?: setOf("text")
            val outArr = (obj["output_modalities"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }?.toSet() ?: setOf("text")
            val traitsObj = obj["traits"] as? JsonObject
            val traits = traitsObj?.mapValues { (_, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "" } ?: emptyMap()
            RawTraits(
                id = id,
                supportsImageInput = obj["supports_image_input"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: false,
                supportsToolCalling = obj["supports_tool_calling"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: false,
                supportsStreaming = obj["supports_streaming"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: true,
                supportsSystemPrompt = obj["supports_system_prompt"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: true,
                maxContextTokens = obj["max_context_tokens"]?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() },
                inputModalities = modArr,
                outputModalities = outArr,
                traits = traits,
            )
        }
    }

    private suspend fun fetchCompatibility(apiKey: String): List<RawCompat> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val compatible = (obj["compatible_with"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { it.isString }?.content
            }
            RawCompat(id = obj.str("id"), compatibleWith = compatible)
        }
    }
}
