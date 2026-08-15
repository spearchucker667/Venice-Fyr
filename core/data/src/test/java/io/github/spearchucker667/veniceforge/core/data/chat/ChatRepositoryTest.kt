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
import org.junit.Assert.assertTrue
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

        val conversationId = chat.createConversation(profileId, "test-text-model")
        val createdAt = requireNotNull(db.conversationDao().findById(profileId, conversationId)).updatedAt
        chat.appendMessage(profileId, conversationId, userMessage(conversationId, profileId, "Hi"))
        chat.appendMessage(profileId, conversationId, assistantMessage(conversationId, profileId, "Hello!"))

        val messages = chat.observeMessages(profileId, conversationId).first()
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].textContent)
        assertEquals(MessageStatus.COMPLETED, messages[1].status)
        assertTrue(requireNotNull(db.conversationDao().findById(profileId, conversationId)).updatedAt > createdAt)
    }

    @Test
    fun `assistant update is scoped to conversation and advances ordering timestamp`() = runTest {
        val profileId = ProfileRepository(db.profileDao()).ensureDefault()
        val chat = ChatRepository(db)
        val firstConversation = chat.createConversation(profileId, "test-text-model")
        val secondConversation = chat.createConversation(profileId, "test-text-model")
        chat.appendMessage(
            profileId,
            firstConversation,
            assistantMessage(firstConversation, profileId, "pending"),
        )
        val before = requireNotNull(
            db.conversationDao().findById(profileId, firstConversation),
        ).updatedAt

        val wrongConversationResult = runCatching {
            chat.updateAssistantText(
                profileId = profileId,
                conversationId = secondConversation,
                messageId = "a1",
                text = "corrupt",
                status = MessageStatus.COMPLETED,
            )
        }
        assertTrue(wrongConversationResult.isFailure)

        chat.updateAssistantText(
            profileId = profileId,
            conversationId = firstConversation,
            messageId = "a1",
            text = "complete",
            status = MessageStatus.COMPLETED,
        )
        assertEquals("complete", chat.observeMessages(profileId, firstConversation).first().single().textContent)
        assertTrue(requireNotNull(db.conversationDao().findById(profileId, firstConversation)).updatedAt > before)
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
            modelId = "test-text-model", createdAt = 2L, updatedAt = 2L,
        )
}
