package io.github.spearchucker667.veniceforge.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.spearchucker667.veniceforge.core.data.dao.ConversationDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageToolCallDao
import io.github.spearchucker667.veniceforge.core.data.dao.ProfileDao
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationFolderEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageToolCallEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        ConversationEntity::class,
        ConversationFolderEntity::class,
        MessageEntity::class,
        MessageToolCallEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageToolCallDao(): MessageToolCallDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "venice_forge.db",
            )
                .build()

        const val SCHEMA_VERSION = 1
    }
}
