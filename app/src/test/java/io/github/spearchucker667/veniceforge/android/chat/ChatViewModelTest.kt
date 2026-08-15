package io.github.spearchucker667.veniceforge.android.chat

import androidx.room.Room
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
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
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
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
            assertEquals("llama-3.3-70b", finalState.modelId)
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
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
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

            val older = chat.createConversation(profileId, "llama-3.3-70b", title = "older")
            convDao.update(
                convDao.findById(profileId, older)!!.copy(updatedAt = 1_000L, lastOpenedAt = 1_000L),
            )
            val recent = chat.createConversation(profileId, "llama-3.3-70b", title = "recent")
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
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
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
}
