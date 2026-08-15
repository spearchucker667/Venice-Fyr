package io.github.spearchucker667.veniceforge.android.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * cooperative cancellation, and duplicate-submission guards.
 */
class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatClient: ChatClient,
    private val apiKeyProvider: suspend () -> String?,
    savedStateHandle: SavedStateHandle,
    initialModelId: String? = null,
) : ViewModel() {

    private val profileId: String = savedStateHandle[KEY_PROFILE_ID]
        ?: throw IllegalStateException("profileId must be provided in SavedStateHandle")

    private var conversationId: String? = savedStateHandle[KEY_CONVERSATION_ID]
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
            savedStateHandle[KEY_CONVERSATION_ID] = convId
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
        val modelId = _state.value.modelId
        if (modelId.isNullOrBlank()) {
            _state.update { it.copy(error = "No model selected. Please select a model.") }
            return
        }

        // Duplicate-submission guard at the ViewModel level.
        if (streamJob?.isActive == true) return

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

        streamJob = viewModelScope.launch {
            _state.update { it.copy(isStreaming = true, error = null) }

            val accumulator = ChatStreamAccumulator()
            var assistantPersisted = false
            try {
                val apiKey = apiKeyProvider()
                if (apiKey.isNullOrBlank()) {
                    _state.update { it.copy(error = "No API key loaded") }
                    return@launch
                }

                // Load prior conversation history to construct multi-turn request context.
                // The new user turn is appended *after* history is loaded, so it appears
                // exactly once in the request context (ARCH-02 regression guard).
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
                assistantPersisted = true

                val req = ChatRequest(
                    model = modelId,
                    messages = contextMessages,
                    stream = true,
                )

                chatClient.streamChat(apiKey, req).collect { chunk ->
                    accumulator.apply(chunk)
                    when (chunk) {
                        is ChatStreamChunk.Delta, is ChatStreamChunk.ToolCallDelta -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                conversationId = convId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.STREAMING,
                            )
                        }
                        is ChatStreamChunk.Finish -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                conversationId = convId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.COMPLETED,
                            )
                        }
                        is ChatStreamChunk.Error -> {
                            chatRepo.updateAssistantText(
                                profileId = profileId,
                                conversationId = convId,
                                messageId = assistantId,
                                text = accumulator.snapshot().text,
                                status = MessageStatus.FAILED,
                            )
                            _state.update { it.copy(error = chunk.message) }
                        }
                        is ChatStreamChunk.Open -> Unit
                    }
                }
            } catch (e: CancellationException) {
                if (assistantPersisted) {
                    withContext(NonCancellable) {
                        chatRepo.updateAssistantText(
                            profileId = profileId,
                            conversationId = convId,
                            messageId = assistantId,
                            text = accumulator.snapshot().text,
                            status = MessageStatus.CANCELLED,
                        )
                    }
                }
                throw e
            } catch (e: Exception) {
                if (assistantPersisted) {
                    chatRepo.updateAssistantText(
                        profileId = profileId,
                        conversationId = convId,
                        messageId = assistantId,
                        text = accumulator.snapshot().text,
                        status = MessageStatus.FAILED,
                    )
                }
                _state.update { it.copy(error = e.message ?: "Unknown error") }
            } finally {
                _state.update { it.copy(isStreaming = false) }
            }
        }
    }

    fun cancel() {
        streamJob?.cancel()
    }

    fun observeConversationForView(convId: String): Flow<List<MessageEntity>> =
        chatRepo.observeMessages(profileId, convId)

    private fun toUi(m: MessageEntity): UiMessage =
        UiMessage(id = m.id, role = m.role, text = m.textContent, status = m.status)

    companion object {
        const val KEY_PROFILE_ID = "profileId"
        const val KEY_CONVERSATION_ID = "conversationId"
    }
}

class ChatViewModelFactory(
    private val chatRepo: ChatRepository,
    private val chatClient: ChatClient,
    private val apiKeyProvider: suspend () -> String?,
    private val profileId: String,
    private val initialModelId: String? = null,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(
            chatRepo = chatRepo,
            chatClient = chatClient,
            apiKeyProvider = apiKeyProvider,
            savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
            initialModelId = initialModelId,
        ) as T
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        savedStateHandle[ChatViewModel.KEY_PROFILE_ID] = profileId
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(
            chatRepo = chatRepo,
            chatClient = chatClient,
            apiKeyProvider = apiKeyProvider,
            savedStateHandle = savedStateHandle,
            initialModelId = initialModelId,
        ) as T
    }
}
