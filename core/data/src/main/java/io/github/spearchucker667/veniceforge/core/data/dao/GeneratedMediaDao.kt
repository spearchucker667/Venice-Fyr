package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedMediaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(media: GeneratedMediaEntity)

    @Query("SELECT * FROM generated_media WHERE profileId = :profileId ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun observeLatest(profileId: String): Flow<GeneratedMediaEntity?>

    @Query("SELECT * FROM generated_media WHERE profileId = :profileId ORDER BY createdAt DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun page(profileId: String, limit: Int, offset: Int): List<GeneratedMediaEntity>

    @Query("DELETE FROM generated_media WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String): Int
}
