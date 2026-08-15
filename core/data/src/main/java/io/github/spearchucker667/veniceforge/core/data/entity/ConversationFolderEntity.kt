package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_folders",
    indices = [Index("profileId"), Index(value = ["profileId", "sortOrder"])],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ConversationFolderEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
