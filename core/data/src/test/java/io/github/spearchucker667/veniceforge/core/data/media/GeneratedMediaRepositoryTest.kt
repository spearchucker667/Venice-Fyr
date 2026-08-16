package io.github.spearchucker667.veniceforge.core.data.media

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaOperation
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity
import io.github.spearchucker667.veniceforge.core.data.repo.GeneratedMediaRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GeneratedMediaRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = GeneratedMediaRepository(context, db)

    @After
    fun tearDown() {
        db.close()
        File(context.filesDir, "generated-media").deleteRecursively()
    }

    @Test
    fun `persisted image metadata and file survive repository recreation`() = runTest {
        val profileId = ProfileRepository(db.profileDao()).ensureDefault()
        val stored = repository.persistImage(
            profileId = profileId,
            bytes = VALID_PNG,
            operation = GeneratedMediaOperation.GENERATE,
            modelId = "runtime-image-model",
            prompt = "test prompt",
            declaredMimeType = "image/png; charset=binary",
        )

        val recreated = GeneratedMediaRepository(context, db)
        val restoredUri = recreated.observeLatestImageUri(profileId).first()
        assertNotNull(restoredUri)
        assertTrue(requireNotNull(restoredUri?.path).let(::File).isFile)
        assertEquals("image/png", stored.mimeType)
        assertEquals(VALID_PNG.size.toLong(), stored.byteSize)
        assertEquals(stored, recreated.page(profileId, 10, 0).single())
    }

    @Test
    fun `profiles cannot observe each other's generated media`() = runTest {
        val defaultId = ProfileRepository(db.profileDao()).ensureDefault()
        val otherId = "other-profile"
        val now = System.currentTimeMillis()
        db.profileDao().insert(
            ProfileEntity(otherId, "Other", otherId, false, now, now),
        )
        repository.persistImage(
            defaultId,
            VALID_PNG,
            GeneratedMediaOperation.EDIT,
            "runtime-image-model",
            "private prompt",
        )

        assertNull(repository.observeLatestImageUri(otherId).first())
        assertTrue(repository.page(otherId, 10, 0).isEmpty())
    }

    @Test
    fun `invalid media is rejected before metadata is written`() = runTest {
        val profileId = ProfileRepository(db.profileDao()).ensureDefault()
        val failure = runCatching {
            repository.persistImage(
                profileId,
                "not an image".encodeToByteArray(),
                GeneratedMediaOperation.GENERATE,
                "runtime-image-model",
                "prompt",
            )
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(repository.page(profileId, 10, 0).isEmpty())
    }

    @Test
    fun `profile media deletion removes metadata and owned files`() = runTest {
        val profileId = ProfileRepository(db.profileDao()).ensureDefault()
        val media = repository.persistImage(
            profileId,
            VALID_PNG,
            GeneratedMediaOperation.GENERATE,
            "runtime-image-model",
            "prompt",
        )
        val file = File(requireNotNull(requireNotNull(repository.uriFor(media)).path))

        assertEquals(1, repository.deleteForProfile(profileId))
        assertTrue(!file.exists())
        assertTrue(repository.page(profileId, 10, 0).isEmpty())
    }

    companion object {
        private val VALID_PNG = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00,
        )
    }
}
