package io.github.spearchucker667.veniceforge.sdk

data class VeniceModel(
    val id: String,
    val objectType: String?,
    val created: Long?,
    val ownedBy: String?,
    val name: String?,
    val description: String?,
    val rawJson: String,
)
