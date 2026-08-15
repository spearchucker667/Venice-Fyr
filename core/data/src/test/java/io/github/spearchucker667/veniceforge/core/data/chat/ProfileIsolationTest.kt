package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileIsolationTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    @Test
    fun `profile B cannot read profile A's conversations`() = runTest {
        val profileRepo = ProfileRepository(db.profileDao())
        val chat = ChatRepository(db)

        val a = profileRepo.ensureDefault()
        val b = "profile-b"
        profileRepo.run {
            // create a second non-default profile.
            db.profileDao().insert(
                io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity(
                    id = b, displayName = "B",
                    apiKeyAlias = b, isDefault = false,
                    createdAt = 0L, updatedAt = 0L,
                )
            )
        }

        val convA = chat.createConversation(a, "model-x")
        chat.appendMessage(a, convA, msg(convA, a, "secret", "a1"))

        // B cannot observe A's conversation or its messages.
        assertEquals(0, chat.observeMessages(b, convA).first().size)
        assertTrue("B's conversation list must not contain A's conversation",
            chat.observeConversations(b).first().none { it.id == convA })
        assertEquals(1, chat.observeConversations(a).first().size)
        // Sanity: B's direct lookup of A's conversation id returns null.
        assertNull(chat.observeConversations(b).first().firstOrNull { it.id == convA })
    }

    private fun msg(conv: String, profile: String, text: String, id: String) =
        MessageEntity(
            id = id, conversationId = conv, profileId = profile,
            role = MessageRole.USER, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = null, createdAt = 1L, updatedAt = 1L,
        )
}
