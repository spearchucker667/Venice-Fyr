package io.github.spearchucker667.veniceforge.core.data

import android.content.Context
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.core.data.repo.GeneratedMediaRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository

/**
 * Minimal service-locator that hides Room types from `:app`. `:app` never sees
 * [AppDatabase] or any Room dependency — it only needs a single entry point that
 * returns the repositories it actually consumes.
 */
class DataServices private constructor(
    private val context: Context,
    private val db: AppDatabase,
) {
    val chatRepository: ChatRepository by lazy { ChatRepository(db) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(db.profileDao()) }
    val generatedMediaRepository: GeneratedMediaRepository by lazy { GeneratedMediaRepository(context, db) }

    companion object {
        fun create(context: Context): DataServices =
            DataServices(context.applicationContext, AppDatabase.create(context.applicationContext))
    }
}
