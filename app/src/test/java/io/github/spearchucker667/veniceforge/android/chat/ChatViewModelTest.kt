package io.github.spearchucker667.veniceforge.android.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
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
}



