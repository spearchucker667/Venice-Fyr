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

// Dynamically resolve default chat model ID (traits["default"] -> first text model -> fallback)
val defaultModelId: String = catalog.defaultTextModelId
    ?: error("No compatible text model available on Venice API")

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
    stream = true,
    veniceParameters = VeniceParameters(
        enableWebSearch = "auto",
        includeVeniceSystemPrompt = true,
        safeMode = false, // Explicit false is preserved in wire payload
    ),
)

// Collect SSE stream
chatClient.streamChat(apiKey, request).collect { chunk ->
    when (chunk) {
        is ChatStreamChunk.Open -> {
            println("Stream connected")
        }
        is ChatStreamChunk.Delta -> {
            chunk.text?.let { print(it) }
        }
        is ChatStreamChunk.ToolCallDelta -> {
            println("Tool call #${chunk.index}: ${chunk.name} args: ${chunk.arguments}")
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
} catch (e: VeniceSdkException.Server) {
    println("Venice server error ($e.statusCode): ${e.safeMessage}")
} catch (e: VeniceSdkException.Network) {
    println("Network failure: ${e.message} (timeout: ${e.isTimeout})")
} catch (e: VeniceSdkException.Cancelled) {
    println("Request was cancelled by caller")
} catch (e: VeniceSdkException) {
    println("Generic SDK error: ${e.message}")
}
```
