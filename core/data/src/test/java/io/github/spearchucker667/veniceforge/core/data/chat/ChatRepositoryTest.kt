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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    @Test
    fun `create conversation, append messages, observe`() = runTest {
        val profileRepo = ProfileRepository(db.profileDao())
        val profileId = profileRepo.ensureDefault()
        val chat = ChatRepository(db)

        val conversationId = chat.createConversation(profileId, "llama-3.3-70b")
        chat.appendMessage(profileId, conversationId, userMessage(conversationId, profileId, "Hi"))
        chat.appendMessage(profileId, conversationId, assistantMessage(conversationId, profileId, "Hello!"))

        val messages = chat.observeMessages(profileId, conversationId).first()
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].textContent)
        assertEquals(MessageStatus.COMPLETED, messages[1].status)
    }

    private fun userMessage(conv: String, profile: String, text: String) =
        MessageEntity(
            id = "u1", conversationId = conv, profileId = profile,
            role = MessageRole.USER, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = null, createdAt = 1L, updatedAt = 1L,
        )

    private fun assistantMessage(conv: String, profile: String, text: String) =
        MessageEntity(
            id = "a1", conversationId = conv, profileId = profile,
            role = MessageRole.ASSISTANT, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = "llama-3.3-70b", createdAt = 2L, updatedAt = 2L,
        )
}
