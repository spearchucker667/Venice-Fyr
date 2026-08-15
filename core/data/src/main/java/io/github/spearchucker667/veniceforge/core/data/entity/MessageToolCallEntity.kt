package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ToolCallStatus { PENDING, STREAMING, COMPLETED, FAILED, CANCELLED }

@Entity(
    tableName = "message_tool_calls",
    indices = [
        Index("messageId"),
        Index("toolCallId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageToolCallEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val toolCallId: String,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String?,
    val status: ToolCallStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
