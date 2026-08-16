package io.github.spearchucker667.veniceforge.android.chat

import androidx.room.Room
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import io.github.spearchucker667.veniceforge.sdk.chat.ChatRequest
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamChunk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private fun CoroutineDispatcher.asExecutor(): Executor =
        Executor { command -> dispatch(EmptyCoroutineContext) { command.run() } }

    private var db: AppDatabase? = null

    @After fun tearDown() { db?.close(); db = null }

    private class RecordingChatClient(private val responses: List<List<ChatStreamChunk>>) : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        val recordedRequests = mutableListOf<ChatRequest>()
        private var callCount = 0

        override fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> {
            recordedRequests.add(request)
            val script = if (callCount < responses.size) responses[callCount] else emptyList()
            callCount++
            return flowOf(*script.toTypedArray())
        }
    }

    private class SuspendingChatClient : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        var callCount = 0

        override fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = flow {
            callCount++
            emit(ChatStreamChunk.Open())
            awaitCancellation()
        }
    }

    private class FailingChatClient : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        override fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = flow {
            throw IllegalStateException("stream failed")
        }
    }

    @Test
    fun `submit writes user message and accumulates assistant chunks`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileRepo = ProfileRepository(syncedDb.profileDao())
            val profileId = profileRepo.ensureDefault()
            val chat = ChatRepository(syncedDb)
            val client = RecordingChatClient(
                listOf(
                    listOf(
                        ChatStreamChunk.Open(),
                        ChatStreamChunk.Delta(0, "Hi "),
                        ChatStreamChunk.Delta(0, "there"),
                        ChatStreamChunk.Finish("stop"),
                    )
                )
            )
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "test-text-model",
            )
            advanceUntilIdle()

            vm.submit("Hello")
            advanceUntilIdle()

            val finalState = vm.state.first { !it.isStreaming && it.messages.size >= 2 }
            val user = finalState.messages.first { it.role == MessageRole.USER }
            val assistant = finalState.messages.first { it.role == MessageRole.ASSISTANT }

            assertEquals("Hello", user.text)
            assertEquals("Hi there", assistant.text)
            assertEquals(MessageStatus.COMPLETED, assistant.status)
            assertEquals("test-text-model", finalState.modelId)
            assertNull(finalState.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `multi-turn chat constructs request with complete prior conversation context`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileRepo = ProfileRepository(syncedDb.profileDao())
            val profileId = profileRepo.ensureDefault()
            val chat = ChatRepository(syncedDb)

            val client = RecordingChatClient(
                listOf(
                    // Response for Turn 1
                    listOf(
                        ChatStreamChunk.Open(),
                        ChatStreamChunk.Delta(0, "First answer"),
                        ChatStreamChunk.Finish("stop"),
                    ),
                    // Response for Turn 2
                    listOf(
                        ChatStreamChunk.Open(),
                        ChatStreamChunk.Delta(0, "Second answer"),
                        ChatStreamChunk.Finish("stop"),
                    )
                )
            )

            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "test-text-model",
            )
            advanceUntilIdle()

            // Turn 1
            vm.submit("Turn one")
            advanceUntilIdle()

            assertEquals(1, client.recordedRequests.size)
            val req1 = client.recordedRequests[0]
            assertEquals(1, req1.messages.size)
            assertEquals("user", req1.messages[0].role)
            assertEquals("Turn one", req1.messages[0].content)

            // Turn 2
            vm.submit("Turn two")
            advanceUntilIdle()

            assertEquals(2, client.recordedRequests.size)
            val req2 = client.recordedRequests[1]
            // Must contain: user(Turn one), assistant(First answer), user(Turn two)
            assertEquals(3, req2.messages.size)
            assertEquals("user", req2.messages[0].role)
            assertEquals("Turn one", req2.messages[0].content)
            assertEquals("assistant", req2.messages[1].role)
            assertEquals("First answer", req2.messages[1].content)
            assertEquals("user", req2.messages[2].role)
            assertEquals("Turn two", req2.messages[2].content)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `init picks most recent existing conversation instead of creating new one`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileRepo = ProfileRepository(syncedDb.profileDao())
            val profileId = profileRepo.ensureDefault()
            val chat = ChatRepository(syncedDb)
            val convDao = syncedDb.conversationDao()

            val older = chat.createConversation(profileId, "test-text-model", title = "older")
            convDao.update(
                convDao.findById(profileId, older)!!.copy(updatedAt = 1_000L, lastOpenedAt = 1_000L),
            )
            val recent = chat.createConversation(profileId, "test-text-model", title = "recent")
            convDao.update(
                convDao.findById(profileId, recent)!!.copy(updatedAt = 5_000L, lastOpenedAt = 5_000L),
            )
            chat.appendMessage(
                profileId,
                recent,
                MessageEntity(
                    id = "seed-msg-1", conversationId = recent, profileId = profileId,
                    role = MessageRole.USER, parentMessageId = null,
                    status = MessageStatus.COMPLETED, textContent = "prior-prompt",
                    modelId = null, createdAt = 5_000L, updatedAt = 5_000L,
                ),
            )

            val client = RecordingChatClient(emptyList())
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "test-text-model",
            )
            advanceUntilIdle()

            val messages = withTimeout(2_000) {
                vm.state.first { it.messages.any { m -> m.text == "prior-prompt" } }.messages
            }
            assertEquals(1, messages.size)
            assertEquals("prior-prompt", messages[0].text)
            assertEquals(MessageRole.USER, messages[0].role)

            val conversations = chat.observeConversations(profileId).first()
            assertEquals(2, conversations.size)
            assertEquals(recent, conversations.first().id)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `model selection is persisted on the active conversation`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileId = ProfileRepository(syncedDb.profileDao()).ensureDefault()
            val chat = ChatRepository(syncedDb)
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = RecordingChatClient(emptyList()),
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "initial-model",
            )
            advanceUntilIdle()

            vm.setModel("  selected-live-model  ")
            advanceUntilIdle()

            val conversation = chat.observeConversations(profileId).first().single()
            assertEquals("selected-live-model", vm.state.value.modelId)
            assertEquals("selected-live-model", conversation.modelId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `restart restores the latest conversation model before applying catalog default`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileId = ProfileRepository(syncedDb.profileDao()).ensureDefault()
            val chat = ChatRepository(syncedDb)
            val firstVm = ChatViewModel(
                chatRepo = chat,
                chatClient = RecordingChatClient(emptyList()),
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "catalog-default",
            )
            advanceUntilIdle()
            firstVm.setModel("persisted-model")
            advanceUntilIdle()

            val restartedVm = ChatViewModel(
                chatRepo = chat,
                chatClient = RecordingChatClient(emptyList()),
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = null,
            )
            restartedVm.setDefaultModelIfUnset("different-catalog-default")
            advanceUntilIdle()

            assertEquals("persisted-model", restartedVm.state.value.modelId)
            assertEquals(
                "persisted-model",
                chat.observeConversations(profileId).first().single().modelId,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `duplicate submit is ignored while stream is active and cancellation resets state`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileId = ProfileRepository(syncedDb.profileDao()).ensureDefault()
            val client = SuspendingChatClient()
            val vm = ChatViewModel(
                chatRepo = ChatRepository(syncedDb),
                chatClient = client,
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "test-model",
            )
            advanceUntilIdle()

            vm.submit("first")
            advanceUntilIdle()
            assertTrue(vm.state.value.isStreaming)

            vm.submit("duplicate")
            advanceUntilIdle()
            assertEquals(1, client.callCount)

            vm.cancel()
            advanceUntilIdle()
            assertFalse(vm.state.value.isStreaming)
            assertEquals(
                MessageStatus.CANCELLED,
                vm.state.value.messages.first { it.role == MessageRole.ASSISTANT }.status,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `stream exception marks assistant failed and resets streaming state`() = runTest {
        val roomDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(roomDispatcher)
        try {
            val syncedDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .setQueryExecutor(roomDispatcher.asExecutor())
                .setTransactionExecutor(roomDispatcher.asExecutor())
                .build()
            db = syncedDb
            val profileId = ProfileRepository(syncedDb.profileDao()).ensureDefault()
            val vm = ChatViewModel(
                chatRepo = ChatRepository(syncedDb),
                chatClient = FailingChatClient(),
                apiKeyProvider = { "test-key" },
                savedStateHandle = SavedStateHandle(mapOf(ChatViewModel.KEY_PROFILE_ID to profileId)),
                initialModelId = "test-model",
            )
            advanceUntilIdle()

            vm.submit("hello")
            advanceUntilIdle()

            assertFalse(vm.state.value.isStreaming)
            assertEquals("stream failed", vm.state.value.error)
            assertEquals(
                MessageStatus.FAILED,
                vm.state.value.messages.first { it.role == MessageRole.ASSISTANT }.status,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}
