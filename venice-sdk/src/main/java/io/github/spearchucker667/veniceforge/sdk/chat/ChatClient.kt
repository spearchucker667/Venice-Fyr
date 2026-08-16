package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.core.common.Redactor
import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

open class ChatClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = callbackFlow {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(request.stream) { "streamChat requires ChatRequest.stream=true" }

        val reqBody = json.encodeToString(ChatRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.CHAT_COMPLETIONS).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val call = sdk.httpClient().newCall(httpReq)
        val transportFinished = AtomicBoolean(false)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                transportFinished.set(true)
                if (!call.isCanceled()) {
                    close(e.toSdkNetworkException())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) {
                        val error = sdk.parseHttpError(res)
                        transportFinished.set(true)
                        close(error)
                        return
                    }

                    trySend(ChatStreamChunk.Open())
                    consumeSuccessfulStream(call, res, transportFinished)
                }
            }
        })

        // The producer suspends here instead of blocking in Call.execute()/readLine().
        // Consumer cancellation therefore reaches this handler immediately and closes
        // the transport even while OkHttp's callback thread waits for more SSE data.
        awaitClose {
            if (!transportFinished.get() && !call.isCanceled()) {
                call.cancel()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun ProducerScope<ChatStreamChunk>.consumeSuccessfulStream(
        call: Call,
        response: Response,
        transportFinished: AtomicBoolean,
    ) {
        var hasTerminal = false
        var sawDone = false

        try {
            val parser = SseLineParser(response.body.byteStream().bufferedReader())
            while (!hasTerminal) {
                val payload = parser.nextData() ?: break
                if (payload == "[DONE]") {
                    sawDone = true
                    break
                }

                for (chunk in parseChunks(payload)) {
                    trySend(chunk)
                    if (chunk is ChatStreamChunk.Finish || chunk is ChatStreamChunk.Error) {
                        hasTerminal = true
                        break
                    }
                }
            }

            when {
                hasTerminal -> {
                    transportFinished.set(true)
                    close()
                }
                sawDone -> {
                    // [DONE] is a valid OpenAI-compatible terminal marker even when a
                    // provider omits a finish_reason chunk.
                    trySend(ChatStreamChunk.Finish(reason = "done"))
                    transportFinished.set(true)
                    close()
                }
                else -> {
                    transportFinished.set(true)
                    close(
                        VeniceSdkException.Protocol(
                            "Chat stream ended before finish_reason or [DONE]",
                        ),
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            transportFinished.set(true)
            if (!call.isCanceled()) close(VeniceSdkException.Network(e, isTimeout = true))
        } catch (e: IOException) {
            transportFinished.set(true)
            if (!call.isCanceled()) close(VeniceSdkException.Network(e, isTimeout = false))
        } catch (e: Throwable) {
            transportFinished.set(true)
            if (!call.isCanceled()) {
                close(
                    if (e is VeniceSdkException) e
                    else VeniceSdkException.Protocol("Invalid chat SSE response", e),
                )
            }
        }
    }

    private fun IOException.toSdkNetworkException(): VeniceSdkException.Network =
        VeniceSdkException.Network(this, isTimeout = this is SocketTimeoutException)

    private fun parseChunks(payload: String): List<ChatStreamChunk> {
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return listOf(ChatStreamChunk.Error(null, "Invalid JSON in chat stream event"))

        val choices = obj["choices"]
        if (choices !is JsonArray) {
            val errObj = obj["error"]
            if (errObj is JsonObject) {
                val message = (errObj["message"] as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
                    ?.let(Redactor::redact)
                    ?: "Venice chat stream error"
                val code = (errObj["code"] as? JsonPrimitive)?.content?.toIntOrNull()
                return listOf(ChatStreamChunk.Error(code, message))
            }
            return listOf(ChatStreamChunk.Error(null, "Chat stream event did not contain choices or error"))
        }

        val first = choices.firstOrNull() as? JsonObject
            ?: return listOf(ChatStreamChunk.Error(null, "Chat stream choices were empty or invalid"))
        val index = (first["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
        val delta = first["delta"] as? JsonObject

        val emitted = mutableListOf<ChatStreamChunk>()

        if (delta != null) {
            val reasoning = (delta["reasoning_content"] as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
            if (reasoning != null) {
                emitted.add(ChatStreamChunk.ReasoningDelta(index, reasoning))
            }

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
