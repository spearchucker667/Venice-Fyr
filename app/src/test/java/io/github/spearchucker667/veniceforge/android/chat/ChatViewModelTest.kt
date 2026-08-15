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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    private class FakeChatClient(private val script: List<ChatStreamChunk>) : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        override fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = flowOf(*script.toTypedArray())
    }

    @Test
    fun `submit writes user message and accumulates assistant chunks`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = ProfileRepository(db.profileDao())
            val profileId = profileRepo.ensureDefault()
            val chat = ChatRepository(db)
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
