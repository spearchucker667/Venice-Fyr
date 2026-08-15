package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }
enum class MessageStatus { PENDING, STREAMING, COMPLETED, FAILED, CANCELLED }

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index(value = ["conversationId", "createdAt"]),
        Index("parentMessageId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val profileId: String,
    val role: MessageRole,
    val parentMessageId: String?,
    val status: MessageStatus,
    val textContent: String,
    val modelId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
