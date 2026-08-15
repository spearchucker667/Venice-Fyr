package io.github.spearchucker667.veniceforge.core.data.repo

import io.github.spearchucker667.veniceforge.core.data.dao.ProfileDao
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity

class ProfileRepository(private val dao: ProfileDao) {
    suspend fun ensureDefault(): String {
        dao.findDefault()?.let { return it.id }
        val now = System.currentTimeMillis()
        val entity = ProfileEntity(
            id = DEFAULT_PROFILE_ID,
            displayName = "default",
            apiKeyAlias = DEFAULT_PROFILE_ID,
            isDefault = true,
            createdAt = now,
            updatedAt = now,
        )
        dao.insertIfAbsent(entity)
        return DEFAULT_PROFILE_ID
    }

    suspend fun findDefault(): String? = dao.findDefault()?.id

    companion object {
        const val DEFAULT_PROFILE_ID = "default"
    }
}
