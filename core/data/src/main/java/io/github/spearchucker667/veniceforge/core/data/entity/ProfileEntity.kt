package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val apiKeyAlias: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
