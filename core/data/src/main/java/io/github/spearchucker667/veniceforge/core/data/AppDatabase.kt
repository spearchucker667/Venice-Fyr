package io.github.spearchucker667.veniceforge.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.spearchucker667.veniceforge.core.data.dao.ConversationDao
import io.github.spearchucker667.veniceforge.core.data.dao.GeneratedMediaDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageToolCallDao
import io.github.spearchucker667.veniceforge.core.data.dao.ProfileDao
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationFolderEntity
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaEntity
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
        GeneratedMediaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageToolCallDao(): MessageToolCallDao
    abstract fun generatedMediaDao(): GeneratedMediaDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "venice_forge.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `generated_media` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `kind` TEXT NOT NULL, `operation` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sha256` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `byteSize` INTEGER NOT NULL, `modelId` TEXT NOT NULL, `prompt` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_media_profileId` ON `generated_media` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_media_profileId_createdAt` ON `generated_media` (`profileId`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_media_sha256` ON `generated_media` (`sha256`)")
            }
        }

        const val SCHEMA_VERSION = 2
    }
}
