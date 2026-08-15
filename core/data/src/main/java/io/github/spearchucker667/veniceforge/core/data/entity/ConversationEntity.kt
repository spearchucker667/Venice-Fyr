package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ConversationKind { STANDARD, CHARACTER }

@Entity(
    tableName = "conversations",
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "updatedAt"]),
        Index("folderId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ConversationFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val modelId: String,
    val kind: ConversationKind,
    val pinned: Boolean,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
)
