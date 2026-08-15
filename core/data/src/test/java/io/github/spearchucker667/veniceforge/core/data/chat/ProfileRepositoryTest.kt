package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val repo = ProfileRepository(db.profileDao())

    @After fun tearDown() { db.close() }

    @Test
    fun `ensureDefault creates default profile when none exists`() = runTest {
        val id = repo.ensureDefault()
        assertEquals(id, db.profileDao().findDefault()?.id)
        assertEquals("default", db.profileDao().findDefault()?.displayName)
    }

    @Test
    fun `ensureDefault is idempotent`() = runTest {
        val first = repo.ensureDefault()
        val second = repo.ensureDefault()
        assertEquals(first, second)
    }
}
