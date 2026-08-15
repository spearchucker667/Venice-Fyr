package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `v1 schema creates all expected tables`() {
        helper.createDatabase(dbName, 1).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.moveToNext()) names.add(cursor.getString(0))
                check("profiles" in names)
                check("conversations" in names)
                check("conversation_folders" in names)
                check("messages" in names)
                check("message_tool_calls" in names)
            }
        }
    }

    @Test
    fun `AppDatabase can open v1`() {
        // Room 2.7.0's RoomDatabase extends neither Closeable nor AutoCloseable,
        // so stdlib `.use {}` doesn't resolve on it; instead open and close directly.
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            dbName,
        ).build().close()
    }
}
