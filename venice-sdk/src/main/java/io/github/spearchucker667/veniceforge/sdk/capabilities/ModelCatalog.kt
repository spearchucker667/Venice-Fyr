package io.github.spearchucker667.veniceforge.sdk.capabilities

/**
 * Snapshot of the merged Venice model catalog as observed by
 * [CapabilitiesRepository.fetchLiveCapabilities]. [refreshedAt] is recorded by the
 * caller at fetch time so UI layers can decide when to re-trigger discovery.
 */
data class ModelCatalog(
    val models: List<ModelCapabilities>,
    val refreshedAt: Long,
) {
    fun byId(id: String): ModelCapabilities? = models.firstOrNull { it.id == id }
}
