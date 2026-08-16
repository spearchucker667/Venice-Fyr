package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeniceParametersSerializationTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `serializes documented Venice chat parameters without provider image safety fields`() {
        val params = VeniceParameters(
            enableWebSearch = "auto",
            includeVeniceSystemPrompt = true,
        )
        val request = ChatRequest(
            model = "test-model",
            messages = listOf(ChatMessage.user("hello")),
            veniceParameters = params,
        )

        val serialized = json.encodeToString(ChatRequest.serializer(), request)

        assertFalse("Chat requests must not include image-only safe_mode", serialized.contains("safe_mode"))
        assertTrue("Must include enable_web_search: auto", serialized.contains("\"enable_web_search\":\"auto\""))
        assertTrue("Must include include_venice_system_prompt: true", serialized.contains("\"include_venice_system_prompt\":true"))
    }

    @Test
    fun `omits unset venice_parameters fields when null`() {
        val jsonClean = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        val params = VeniceParameters(
            enableWebSearch = "on",
        )
        val serialized = jsonClean.encodeToString(VeniceParameters.serializer(), params)

        assertTrue(serialized.contains("\"enable_web_search\":\"on\""))
        assertFalse("Must not serialize unset character_slug", serialized.contains("character_slug"))
        assertFalse("Must not serialize unset disable_thinking", serialized.contains("disable_thinking"))
    }

    @Test
    fun `deserializes chat message with tool calls`() {
        val jsonStr = """
            {
              "role": "assistant",
              "content": null,
              "tool_calls": [
                {
                  "id": "call_abc123",
                  "type": "function",
                  "function": {
                    "name": "get_current_weather",
                    "arguments": "{\"location\":\"San Francisco\"}"
                  }
                }
              ]
            }
        """.trimIndent()

        val msg = json.decodeFromString(ChatMessage.serializer(), jsonStr)
        assertEquals("assistant", msg.role)
        assertEquals(null, msg.content)
        assertEquals(1, msg.toolCalls?.size)
        assertEquals("call_abc123", msg.toolCalls?.first()?.id)
        assertEquals("get_current_weather", msg.toolCalls?.first()?.function?.name)
    }

    @Test
    fun `serializes typed reasoning controls and preserves reasoning fields`() {
        val request = ChatRequest(
            model = "reasoning-model",
            messages = listOf(
                ChatMessage(
                    role = "assistant",
                    content = "answer",
                    reasoningContent = "[Some reasoning content is encrypted]",
                ),
            ),
            reasoning = ReasoningConfig(enabled = true, effort = ReasoningEffort.HIGH, summary = ReasoningSummary.CONCISE),
            reasoningEffort = ReasoningEffort.XHIGH,
        )

        val serialized = json.encodeToString(ChatRequest.serializer(), request)
        assertTrue(serialized.contains("\"reasoning\":{\"enabled\":true,\"effort\":\"high\",\"summary\":\"concise\"}"))
        assertTrue(serialized.contains("\"reasoning_effort\":\"xhigh\""))
        assertTrue(serialized.contains("\"reasoning_content\":\"[Some reasoning content is encrypted]\""))
    }
}
