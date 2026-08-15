package io.github.spearchucker667.veniceforge.core.data.repo

import androidx.room.withTransaction
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationKind
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository.Companion.DEFAULT_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val db: AppDatabase) {
    private val profileDao = db.profileDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()

    suspend fun createConversation(
        profileId: String,
        modelId: String,
        title: String = "New conversation",
        kind: ConversationKind = ConversationKind.STANDARD,
    ): String = db.withTransaction {
        require(profileDao.findById(profileId) != null) { "Unknown profileId: $profileId" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversationDao.insert(
            ConversationEntity(
                id = id,
                profileId = profileId,
                title = title,
                modelId = modelId,
                kind = kind,
                pinned = false,
                folderId = null,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
            )
        )
        id
    }

    suspend fun appendMessage(profileId: String, conversationId: String, message: MessageEntity) {
        require(message.profileId == profileId) { "Message.profileId must equal scoping profileId" }
        require(message.conversationId == conversationId) { "Message.conversationId mismatch" }
        db.withTransaction {
            val conversation = conversationDao.findById(profileId, conversationId)
            require(conversation != null) {
                "Unknown conversationId: $conversationId"
            }
            messageDao.upsert(message)
            conversationDao.update(
                conversation.copy(updatedAt = maxOf(System.currentTimeMillis(), conversation.updatedAt + 1)),
            )
        }
    }

    suspend fun updateAssistantText(
        profileId: String,
        conversationId: String,
        messageId: String,
        text: String,
        status: MessageStatus,
    ) = db.withTransaction {
        val conversation = conversationDao.findById(profileId, conversationId)
        require(conversation != null) { "Unknown conversationId: $conversationId" }
        val now = maxOf(System.currentTimeMillis(), conversation.updatedAt + 1)
        val updated = messageDao.updateTextAndStatus(
            profileId = profileId,
            conversationId = conversationId,
            id = messageId,
            text = text,
            status = status,
            updatedAt = now,
        )
        require(updated == 1) { "Unknown messageId for conversation: $messageId" }
        conversationDao.update(conversation.copy(updatedAt = now))
    }

    fun observeConversations(profileId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeForProfile(profileId)

    fun observeMessages(profileId: String, conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(profileId, conversationId).map { messages ->
            messages.filter { it.profileId == profileId }  // belt and suspenders
        }

    suspend fun deleteConversation(profileId: String, conversationId: String): Boolean =
        db.withTransaction {
            conversationDao.deleteById(profileId, conversationId) > 0
        }
}
