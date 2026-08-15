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
    fun `preserves explicit safe_mode false in serialized JSON`() {
        val params = VeniceParameters(
            safeMode = false,
            enableWebSearch = "auto",
            includeVeniceSystemPrompt = true,
        )
        val request = ChatRequest(
            model = "test-model",
            messages = listOf(ChatMessage.user("hello")),
            veniceParameters = params,
        )

        val serialized = json.encodeToString(ChatRequest.serializer(), request)

        assertTrue("Must include safe_mode: false", serialized.contains("\"safe_mode\":false"))
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
}
