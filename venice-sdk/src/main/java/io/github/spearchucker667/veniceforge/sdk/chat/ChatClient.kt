package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.yield
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
        val cancelHandle = coroutineContext.job.invokeOnCompletion { cause ->
            if (cause is CancellationException && !call.isCanceled()) {
                runCatching { call.cancel() }
            }
        }
        trySend(ChatStreamChunk.Open()) // signal stream opened

        var hasEmittedTerminal = false

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = response.body.string()
                    trySend(ChatStreamChunk.Error(response.code, msg))
                    hasEmittedTerminal = true
                    close()
                    return@callbackFlow
                }
                val source = response.body.byteStream()
                val parser = SseLineParser(source.bufferedReader())
                while (true) {
                    coroutineContext.ensureActive()
                    yield()
                    val payload = parser.nextData() ?: break
                    if (payload == "[DONE]") break

                    val chunks = parseChunks(payload)
                    for (chunk in chunks) {
                        trySend(chunk)
                        if (chunk is ChatStreamChunk.Finish) {
                            hasEmittedTerminal = true
                        } else if (chunk is ChatStreamChunk.Error) {
                            hasEmittedTerminal = true
                        }
                    }
                }
                // Enforce exactly one terminal completion event per successful stream
                if (!hasEmittedTerminal) {
                    trySend(ChatStreamChunk.Finish(reason = "stop"))
                    hasEmittedTerminal = true
                }
                close()
            }
        } catch (e: CancellationException) {
            // Cancellation terminates the flow promptly; invokeOnCompletion cancels the OkHttp Call
        } catch (e: Throwable) {
            if (!call.isCanceled() && !hasEmittedTerminal) {
                trySend(ChatStreamChunk.Error(code = null, message = e.message ?: e::class.simpleName.orEmpty()))
            }
            close(e)
        }

        awaitClose {
            cancelHandle.dispose()
            if (!call.isCanceled()) {
                runCatching { call.cancel() }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseChunks(payload: String): List<ChatStreamChunk> {
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return listOf(ChatStreamChunk.Error(null, "invalid SSE JSON: $payload"))

        val choices = obj["choices"]
        if (choices !is JsonArray) {
            val errObj = obj["error"]
            if (errObj is JsonObject) {
                val msg = (errObj["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "stream error"
                val code = (errObj["code"] as? JsonPrimitive)?.content?.toIntOrNull()
                return listOf(ChatStreamChunk.Error(code, msg))
            }
            return listOf(ChatStreamChunk.Error(null, payload))
        }

        val first = choices.firstOrNull() as? JsonObject ?: return listOf(ChatStreamChunk.Error(null, payload))
        val index = (first["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
        val delta = first["delta"] as? JsonObject

        val emitted = mutableListOf<ChatStreamChunk>()

        if (delta != null) {
            val toolCalls = delta["tool_calls"] as? JsonArray
            if (toolCalls != null && toolCalls.isNotEmpty()) {
                for (tcElem in toolCalls) {
                    val tc = tcElem as? JsonObject ?: continue
                    val tcIndex = (tc["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: index
                    val id = (tc["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                    val fn = tc["function"] as? JsonObject
                    val name = (fn?.get("name") as? JsonPrimitive)?.takeIf { it.isString }?.content
                    val args = (fn?.get("arguments") as? JsonPrimitive)?.takeIf { it.isString }?.content
                    emitted.add(ChatStreamChunk.ToolCallDelta(tcIndex, id, name, args))
                }
            }

            val text = (delta["content"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (text != null) {
                emitted.add(ChatStreamChunk.Delta(index, text))
            }
        }

        val finishReason = (first["finish_reason"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (finishReason != null) {
            emitted.add(ChatStreamChunk.Finish(reason = finishReason))
        }

        if (emitted.isEmpty()) {
            return listOf(ChatStreamChunk.Delta(index, null))
        }

        return emitted
    }
}
