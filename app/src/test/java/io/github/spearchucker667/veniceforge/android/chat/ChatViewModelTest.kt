package io.github.spearchucker667.veniceforge.android.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import io.github.spearchucker667.veniceforge.sdk.chat.ChatRequest
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamChunk
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
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

    // Map a CoroutineDispatcher onto a Java Executor that schedules tasks on
    // the dispatcher. This lets Room's query/transaction executors run under
    // the runTest scheduler, so `advanceUntilIdle` drains Room writes.
    private fun CoroutineDispatcher.asExecutor(): Executor =
        Executor { command -> dispatch(EmptyCoroutineContext) { command.run() } }

    private var db: AppDatabase? = null

    @After fun tearDown() { db?.close(); db = null }

    private class FakeChatClient(private val script: List<ChatStreamChunk>) : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        override fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = flowOf(*script.toTypedArray())
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
            val client = FakeChatClient(
                listOf(
                    ChatStreamChunk.Open(),
                    ChatStreamChunk.Delta(0, "Hi "),
                    ChatStreamChunk.Delta(0, "there"),
                    ChatStreamChunk.Finish("stop"),
                )
            )
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
            )
            // Drain the init coroutine that creates the conversation.
            advanceUntilIdle()

            vm.submit("Hello")
            advanceUntilIdle()

            // Wait for assistant message to be COMPLETED and accumulated.
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

            // Seed two conversations; the "recent" one already holds a user message so
            // we can detect which conversation the ViewModel picked from the visible state.
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

            val client = FakeChatClient(emptyList())
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
            )
            advanceUntilIdle()

            // Most-recent-by-updatedAt must be the active conversation: the seeded
            // user message should appear in the visible state. If the ViewModel
            // instead mints a fresh conversation, the state stays empty and the
            // timeout fires (or the predicate never matches because the new
            // conversation has no messages).
            val messages = withTimeout(2_000) {
                vm.state.first { it.messages.any { m -> m.text == "prior-prompt" } }.messages
            }
            assertEquals(1, messages.size)
            assertEquals("prior-prompt", messages[0].text)
            assertEquals(MessageRole.USER, messages[0].role)

            // The DB must not have grown: only the two seeded conversations remain.
            val conversations = chat.observeConversations(profileId).first()
            assertEquals(2, conversations.size)
            assertEquals(recent, conversations.first().id)
        } finally {
            Dispatchers.resetMain()
        }
    }
}



