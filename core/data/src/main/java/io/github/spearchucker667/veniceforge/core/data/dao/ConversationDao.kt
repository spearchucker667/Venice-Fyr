package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(profileId: String, id: String): Int

    @Query("SELECT * FROM conversations WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun findById(profileId: String, id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeForProfile(profileId: String): Flow<List<ConversationEntity>>

    @Transaction
    suspend fun deleteCascade(profileId: String, id: String) {
        deleteById(profileId, id)
    }
}
