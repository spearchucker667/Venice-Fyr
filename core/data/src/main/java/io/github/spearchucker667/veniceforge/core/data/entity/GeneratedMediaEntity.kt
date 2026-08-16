package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GeneratedMediaKind { IMAGE, VIDEO, AUDIO }
enum class GeneratedMediaOperation { GENERATE, EDIT }

@Entity(
    tableName = "generated_media",
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "createdAt"]),
        Index("sha256"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GeneratedMediaEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val kind: GeneratedMediaKind,
    val operation: GeneratedMediaOperation,
    val mimeType: String,
    val sha256: String,
    val relativePath: String,
    val byteSize: Long,
    val modelId: String,
    val prompt: String,
    val createdAt: Long,
)
