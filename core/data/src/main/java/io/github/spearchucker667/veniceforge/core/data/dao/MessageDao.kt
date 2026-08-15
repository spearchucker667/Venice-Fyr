package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET textContent = :text, status = :status, updatedAt = :updatedAt WHERE id = :id AND profileId = :profileId")
    suspend fun updateTextAndStatus(profileId: String, id: String, text: String, status: MessageStatus, updatedAt: Long): Int

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND profileId = :profileId ORDER BY createdAt ASC")
    fun observeForConversation(profileId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(profileId: String, id: String): Int
}
