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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val modelId: String = "llama-3.3-70b",
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
 * active conversation cannot read or write another profile's state. The
 * streaming job is exposed via [cancel] so SSE consumption stops promptly and
 * downstream network/OkHttp resources are released via the SDK's awaitClose
 * hook.
 */
class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatClient: ChatClient,
    private val apiKeyProvider: () -> String?,
    private val profileId: String,
    initialModelId: String = "llama-3.3-70b",
) : ViewModel() {

    private var conversationId: String? = null
    private var streamJob: Job? = null
    private val _state = MutableStateFlow(ChatUiState(modelId = initialModelId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val convId = chatRepo.createConversation(profileId, initialModelId)
            conversationId = convId
            // Hydrate with persisted messages.
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

    fun submit(text: String) {
        val convId = conversationId ?: return
        val apiKey = apiKeyProvider() ?: run {
            _state.update { it.copy(error = "No API key") }
            return
        }
        val modelId = _state.value.modelId
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
            chatRepo.appendMessage(profileId, convId, userMsg)
            chatRepo.appendMessage(profileId, convId, assistantMsg)

            val req = ChatRequest(
                model = modelId,
                messages = listOf(ChatMessage(role = "user", content = text)),
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
                            _state.update { it.copy(isStreaming = true) }
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
                        is ChatStreamChunk.Open -> Unit  // stream opened; nothing to persist
                    }
                }
            }
        }
    }

    fun cancel() {
        streamJob?.cancel()  // propagates to ChatClient's awaitClose { call.cancel() }
        streamJob = null
        _state.update { it.copy(isStreaming = false) }
    }

    fun observeConversationForView(convId: String): Flow<List<MessageEntity>> =
        chatRepo.observeMessages(profileId, convId)

    private fun toUi(m: MessageEntity): UiMessage =
        UiMessage(id = m.id, role = m.role, text = m.textContent, status = m.status)
}
