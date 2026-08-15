package io.github.spearchucker667.veniceforge.android.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import io.github.spearchucker667.veniceforge.sdk.chat.ChatMessage
import io.github.spearchucker667.veniceforge.sdk.chat.ChatRequest
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamAccumulator
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamChunk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val modelId: String? = null,
    val isStreaming: Boolean = false,
    val error: String? = null,
)

data class UiMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val status: MessageStatus,
)

/**
 * Orchestrates a single conversation against [ChatClient] with persistence via
 * [ChatRepository]. Keeps every persistence call scoped to [profileId] so the
 * active conversation cannot read or write another profile's state.
 *
 * Implements full multi-turn conversation context, dynamic model selection,
 * and cooperative cancellation.
 */
class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatClient: ChatClient,
    private val apiKeyProvider: () -> String?,
    private val profileId: String,
    initialModelId: String? = null,
) : ViewModel() {

    private var conversationId: String? = null
    private var streamJob: Job? = null
    private val _state = MutableStateFlow(ChatUiState(modelId = initialModelId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = chatRepo.observeConversations(profileId).first()
            val convId = if (existing.isNotEmpty()) {
                existing.first().id
            } else {
                chatRepo.createConversation(profileId, initialModelId ?: "")
            }
            conversationId = convId
            chatRepo.observeMessages(profileId, convId).collect { msgs ->
                _state.update {
                    it.copy(messages = msgs.map(::toUi))
                }
            }
        }
    }

    fun setModel(modelId: String) {
        _state.update { it.copy(modelId = modelId) }
    }

    fun setDefaultModelIfUnset(defaultModelId: String) {
        _state.update {
            if (it.modelId.isNullOrBlank()) it.copy(modelId = defaultModelId) else it
        }
    }

    fun submit(text: String) {
        val convId = conversationId ?: return
        val apiKey = apiKeyProvider() ?: run {
            _state.update { it.copy(error = "No API key loaded") }
            return
        }
        val modelId = _state.value.modelId
        if (modelId.isNullOrBlank()) {
            _state.update { it.copy(error = "No model selected. Please select a model.") }
            return
        }

        val now = System.currentTimeMillis()
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            profileId = profileId,
            role = MessageRole.USER,
            parentMessageId = null,
            status = MessageStatus.COMPLETED,
            textContent = text,
            modelId = null,
            createdAt = now,
            updatedAt = now,
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantMsg = MessageEntity(
            id = assistantId,
            conversationId = convId,
            profileId = profileId,
            role = MessageRole.ASSISTANT,
            parentMessageId = userMsg.id,
            status = MessageStatus.PENDING,
            textContent = "",
            modelId = modelId,
            createdAt = now,
            updatedAt = now,
        )

        viewModelScope.launch {
            // Load prior conversation history to construct multi-turn request context
            val priorMessages = chatRepo.observeMessages(profileId, convId).first()
            val contextMessages = priorMessages
                .filter { it.status == MessageStatus.COMPLETED && it.textContent.isNotBlank() }
                .map { entity ->
                    val roleStr = when (entity.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                        MessageRole.TOOL -> "tool"
                    }
                    ChatMessage(role = roleStr, content = entity.textContent)
                }
                .plus(ChatMessage.user(text))

            chatRepo.appendMessage(profileId, convId, userMsg)
            chatRepo.appendMessage(profileId, convId, assistantMsg)

            val req = ChatRequest(
                model = modelId,
                messages = contextMessages,
                stream = true,
            )

            streamJob = launch {
                val accumulator = ChatStreamAccumulator()
                chatClient.streamChat(apiKey, req).collect { chunk ->
                    accumulator.apply(chunk)
                    when (chunk) {
                        is ChatStreamChunk.Delta, is ChatStreamChunk.ToolCallDelta -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.STREAMING,
                            )
                            _state.update { it.copy(isStreaming = true, error = null) }
                        }
                        is ChatStreamChunk.Finish -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.COMPLETED,
                            )
                            _state.update { it.copy(isStreaming = false) }
                        }
                        is ChatStreamChunk.Error -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.FAILED,
                            )
                            _state.update { it.copy(isStreaming = false, error = chunk.message) }
                        }
                        is ChatStreamChunk.Open -> Unit
                    }
                }
            }
        }
    }

    fun cancel() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isStreaming = false) }
    }

    fun observeConversationForView(convId: String): Flow<List<MessageEntity>> =
        chatRepo.observeMessages(profileId, convId)

    private fun toUi(m: MessageEntity): UiMessage =
        UiMessage(id = m.id, role = m.role, text = m.textContent, status = m.status)
}
