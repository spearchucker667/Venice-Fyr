package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.ModelType

/**
 * Snapshot of the merged Venice model catalog as observed by
 * [CapabilitiesRepository.fetchLiveCapabilities].
 *
 * Exposes dynamic discovery resolution for default models, traits, and compatibility aliases
 * without hard-coding static model allowlists.
 */
data class ModelCatalog(
    val models: List<ModelCapabilities>,
    val traits: Map<String, String> = emptyMap(),
    val compatibilityMapping: Map<String, String> = emptyMap(),
    val refreshedAt: Long,
) {
    /**
     * Dynamically resolves the default chat model ID from runtime traits ("default", "text:default")
     * or falls back to the first available text chat model in the catalog.
     */
    val defaultTextModelId: String?
        get() = defaultModelIdFor(ModelType.TEXT)

    /**
     * Resolve a usable runtime default for [type]. Trait targets must exist in the current model
     * catalog, match the requested modality, and be online; stale discovery aliases are ignored.
     */
    fun defaultModelIdFor(type: ModelType): String? {
        val candidates = buildList {
            traits["${type.wireName}:default"]?.let(::add)
            if (type == ModelType.TEXT) traits["default"]?.let(::add)
        }
        val matchesType: (ModelCapabilities) -> Boolean = { model ->
            if (type == ModelType.TEXT) {
                model.supportsTextChat
            } else {
                model.type.equals(type.wireName, ignoreCase = true)
            }
        }
        return candidates.firstNotNullOfOrNull { id ->
            byId(id)?.takeIf { matchesType(it) && !it.offline }?.id
        } ?: models.firstOrNull { matchesType(it) && !it.offline }?.id
    }

    /**
     * Find model capabilities by exact model ID.
     */
    fun byId(id: String): ModelCapabilities? = models.firstOrNull { it.id == id }

    /**
     * Resolve model capabilities for a symbolic trait (e.g. "default", "fastest", "uncensored").
     */
    fun modelForTrait(trait: String): ModelCapabilities? =
        traits[trait]?.let(::byId)

    /**
     * Resolve model capabilities for a compatibility alias (e.g. "gpt-4o", "claude-3-5-sonnet").
     */
    fun modelForAlias(alias: String): ModelCapabilities? =
        compatibilityMapping[alias]?.let(::byId)
}
