# Venice SDK Code Examples

Practical Kotlin code examples demonstrating the public API of `:venice-sdk`.

---

## 1. Model Discovery & Capability Resolution

### Basic Model Listing
```kotlin
val sdk = VeniceForgeSdk()

// List all text models (GET /models?type=text)
val textModels: List<VeniceModel> = sdk.listModels(apiKey, ModelType.TEXT)

// List all models without type filter (GET /models)
val allModels: List<VeniceModel> = sdk.listModels(apiKey)
```

### Dynamic Capability & Default Resolution
```kotlin
val sdk = VeniceForgeSdk()
val capabilitiesRepo = CapabilitiesRepository(sdk)

// Fetches /models, /models/traits, and /models/compatibility_mapping
val catalog: ModelCatalog = capabilitiesRepo.fetchLiveCapabilities(apiKey)

// Dynamically resolve defaults from live traits with an online, matching-model fallback.
val defaultModelId: String = catalog.defaultTextModelId
    ?: error("No compatible text model available on Venice API")
val defaultImageModelId: String? = catalog.defaultModelIdFor(ModelType.IMAGE)

// Inspect model capabilities
val modelCaps: ModelCapabilities? = catalog.byId(defaultModelId)
val supportsVision: Boolean = modelCaps?.supportsVision ?: false
val supportsTools: Boolean = modelCaps?.supportsToolCalling ?: false
val contextTokens: Long? = modelCaps?.availableContextTokens
```

---

## 2. Multi-Turn Streaming Chat

```kotlin
val sdk = VeniceForgeSdk()
val chatClient = ChatClient(sdk)

// Construct full multi-turn conversation history
val conversation = listOf(
    ChatMessage.user("What is the capital of France?"),
    ChatMessage.assistant("The capital of France is Paris."),
    ChatMessage.user("What is its most famous landmark?"),
)

val request = ChatRequest(
    model = defaultModelId,
    messages = conversation,
    stream = true, // streamChat rejects false; a typed non-streaming API is not implemented yet
    reasoning = ReasoningConfig(enabled = true, effort = ReasoningEffort.HIGH),
    reasoningEffort = ReasoningEffort.HIGH,
    veniceParameters = VeniceParameters(
        enableWebSearch = "auto",
        includeVeniceSystemPrompt = true,
    ),
)

// Collect SSE stream
chatClient.streamChat(apiKey, request).collect { chunk ->
    when (chunk) {
        is ChatStreamChunk.Open -> {
            println("Stream connected")
        }
        is ChatStreamChunk.Delta -> {
            chunk.textFragment?.let { print(it) }
        }
        is ChatStreamChunk.ReasoningDelta -> {
            // Keep provider reasoning separate from the assistant answer.
            println("Reasoning: ${chunk.reasoningFragment}")
        }
        is ChatStreamChunk.ToolCallDelta -> {
            println("Tool call #${chunk.index}: ${chunk.name} args: ${chunk.argumentsFragment}")
        }
        is ChatStreamChunk.Finish -> {
            println("\nStream finished with reason: ${chunk.reason}")
        }
        is ChatStreamChunk.Error -> {
            System.err.println("Stream error (${chunk.code}): ${chunk.message}")
        }
    }
}
```

`streamChat()` cancels its active OkHttp call when collection is cancelled. A stream that ends without either an explicit `finish_reason` or `[DONE]` throws `VeniceSdkException.Protocol`; partial output must not be persisted as a completed reply.

Provider-encrypted or summarized reasoning placeholders are returned unchanged. The current app does not persist or render reasoning history; SDK consumers must make that privacy/UI decision explicitly.

---

## 3. Structured Error Handling

```kotlin
try {
    val models = sdk.listModels(apiKey)
} catch (e: VeniceSdkException.RateLimit) {
    println("Rate limited! Limit: ${e.rateLimitInfo?.limitRequests}, Reset in: ${e.retryAfterSeconds}s")
} catch (e: VeniceSdkException.Authentication) {
    println("Invalid API key or unauthorized: ${e.safeMessage}")
} catch (e: VeniceSdkException.Validation) {
    println("Bad request parameters ($e.statusCode): ${e.safeMessage}")
} catch (e: VeniceSdkException.PaymentRequired) {
    println("Payment or balance required: ${e.safeMessage}")
} catch (e: VeniceSdkException.Server) {
    println("Venice server error ($e.statusCode): ${e.safeMessage}")
} catch (e: VeniceSdkException.Network) {
    println("Network failure: ${e.message} (timeout: ${e.isTimeout})")
} catch (e: VeniceSdkException.Protocol) {
    println("Incomplete or invalid Venice response: ${e.message}")
} catch (e: VeniceSdkException.Cancelled) {
    println("Request was cancelled by caller")
} catch (e: VeniceSdkException) {
    println("Generic SDK error: ${e.message}")
}
```

---

## 4. Binary Image Operations

```kotlin
val images = sdk.imageClient()
val edited: BinaryMediaResult = images.edit(
    apiKey,
    EditImageRequest(
        image = inputDataUrl,
        prompt = "Replace the sky with a sunset",
        model = selectedRuntimeModelId,
        outputFormat = "png",
        safeMode = false, // Image provider field; explicit false is retained.
    ),
)

check(edited.mimeType.startsWith("image/"))
appMediaStore.persist(edited.bytes, edited.mimeType)
```

`edit`, `multiEdit`, and `upscale` return `BinaryMediaResult`; they never return a generated-image JSON envelope.

---

## 5. Queued Audio and Video

```kotlin
val audio = sdk.audioClient()
val quote = audio.quote(apiKey, QuoteAudioRequest(musicModelId, durationSeconds = "60"))
requestExplicitPaidOperationApproval(quote.quote)
val queuedAudio = audio.queue(
    apiKey,
    QueueAudioRequest(musicModelId, "Warm ambient strings", durationSeconds = "60"),
)

when (val result = audio.retrieve(apiKey, RetrieveAudioRequest(queuedAudio.model, queuedAudio.queueId))) {
    is AudioRetrieveResult.Processing -> scheduleBoundedPoll(result.averageExecutionTime)
    is AudioRetrieveResult.CompletedBinary -> persistAudio(result.audio, result.mimeType)
    is AudioRetrieveResult.UnknownStatus -> stopAndSurfaceUnknownStatus(result.status)
}
```

```kotlin
val video = sdk.videoClient()
val queuedVideo = video.queue(
    apiKey,
    QueueVideoRequest(videoModelId, "A canal at dawn", duration = "5s"),
)

// Retain queuedVideo.downloadUrl in durable caller-owned job state.
when (val result = video.retrieve(apiKey, RetrieveVideoRequest(queuedVideo.model, queuedVideo.queueId))) {
    is VideoRetrieveResult.Processing -> scheduleBoundedPoll(result.averageExecutionTime)
    is VideoRetrieveResult.CompletedRemote -> downloadFromRetainedQueueUrl(queuedVideo.downloadUrl)
    is VideoRetrieveResult.CompletedBinary -> persistVideo(result.binaryVideo, result.mimeType)
    is VideoRetrieveResult.UnknownStatus -> stopAndSurfaceUnknownStatus(result.status)
}
```

Queue submission is never generation completion. Production callers must persist job state, bound polling, survive process death, and call `complete` only after successful durable download when automatic deletion was not requested.
