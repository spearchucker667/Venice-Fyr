package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

open class ChatClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = callbackFlow {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val reqBody = json.encodeToString(ChatRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.CHAT_COMPLETIONS).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val call = sdk.httpClient().newCall(httpReq)
        val cancellationHook = trySend(ChatStreamChunk.Open()).isSuccess  // signal stream opened

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = response.body?.string().orEmpty()
                    trySend(ChatStreamChunk.Error(response.code, msg)).isSuccess
                    close()
                    return@callbackFlow
                }
                val source = response.body?.byteStream()
                    ?: throw VeniceSdkException.Protocol("Empty response body")
                val parser = SseLineParser(source.bufferedReader())
                while (true) {
                    val payload = parser.nextData() ?: break
                    if (payload == "[DONE]") break
                    val chunk = parseChunk(payload)
                    trySend(chunk).isSuccess
                }
                trySend(ChatStreamChunk.Finish(reason = "stop"))
                close()
            }
        } catch (e: Throwable) {
            trySend(ChatStreamChunk.Error(code = null, message = e.message ?: e::class.simpleName.orEmpty()))
            close(e)
        }

        awaitClose {
            if (!call.isCanceled()) runCatching { call.cancel() }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseChunk(payload: String): ChatStreamChunk {
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return ChatStreamChunk.Error(null, "invalid SSE JSON: $payload")
        val choices = obj["choices"]
        if (choices !is JsonArray) {
            val errObj = obj["error"]
            if (errObj is JsonObject) {
                val msg = (errObj["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "stream error"
                val code = (errObj["code"] as? JsonPrimitive)?.content?.toIntOrNull()
                return ChatStreamChunk.Error(code, msg)
            }
            return ChatStreamChunk.Error(null, payload)
        }
        val first = choices.firstOrNull() as? JsonObject ?: return ChatStreamChunk.Error(null, payload)
        val index = (first["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
        val delta = first["delta"] as? JsonObject
        if (delta != null) {
            val text = (delta["content"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val toolCalls = delta["tool_calls"] as? JsonArray
            if (toolCalls != null && toolCalls.isNotEmpty()) {
                // Single tool_call deltas from the desktop / Venice `/chat/completions` look like:
                // { "index": 0, "id": "...", "function": { "name": "...", "arguments": "..." } }
                val tc = toolCalls.first() as JsonObject
                val tcIndex = (tc["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: index
                val id = (tc["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                val fn = tc["function"] as? JsonObject
                val name = (fn?.get("name") as? JsonPrimitive)?.takeIf { it.isString }?.content
                val args = (fn?.get("arguments") as? JsonPrimitive)?.takeIf { it.isString }?.content
                return ChatStreamChunk.ToolCallDelta(tcIndex, id, name, args)
            }
            return ChatStreamChunk.Delta(index, text)
        }
        val finishReason = (first["finish_reason"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (finishReason != null) {
            return ChatStreamChunk.Finish(reason = finishReason)
        }
        return ChatStreamChunk.Error(null, "unhandled SSE payload: $payload")
    }
}
