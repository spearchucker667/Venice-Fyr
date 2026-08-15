package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.spearchucker667.veniceforge.core.data.entity.MessageToolCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageToolCallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(call: MessageToolCallEntity)

    @Query("SELECT * FROM message_tool_calls WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun observeForMessage(messageId: String): Flow<List<MessageToolCallEntity>>
}
