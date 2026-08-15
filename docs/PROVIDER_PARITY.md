# Provider Adapter Parity

The Android port must preserve the desktop provider registry as a separate concern from the Venice-native SDK.

`venice-sdk` remains Venice-specific. Fallback providers belong behind an app/core `ProviderAdapter` interface so Venice API semantics do not become polluted with provider-specific transformations.

## Desktop baseline

| Provider | Desktop status | Implemented features | Model discovery |
|---|---|---|---|
| Venice AI | Available | chat, image, video, audio, embeddings | live |
| Together AI | Available | chat, image | static |
| Groq | Available | chat | static |
| Fireworks AI | Available | chat | static |
| Google Gemini Developer API | Available | chat | static |
| Mistral API | Available | chat | static |
| Anthropic API | Available | chat | static |
| Perplexity API | Available | chat | static |
| Replicate | Deferred | none | none |
| AWS Bedrock | Deferred | none | none |
| Google Vertex AI | Deferred | none | none |
| Azure OpenAI | Deferred | none | none |
| Hugging Face | Deferred | none | none |
| Cohere | Deferred | none | none |

## Android rules

- Preserve the *implemented/deferred* distinction. A deferred provider must not accept a credential or route traffic until its adapter is actually implemented.
- Store each provider credential with a provider+profile-scoped Android Keystore alias.
- Normalize app-level requests into provider adapters; do not make feature screens directly know provider HTTP quirks.
- Provider adapters own request/response/stream transformation only. Feature policy, paid-operation confirmation, redaction, and profile isolation stay above the adapter.
- Add fixture tests for Anthropic/Gemini/etc. streaming deltas before exposing them in UI.
- Preserve the desktop registry as the source of truth for which provider/features are actually available.
- Research providers (Venice/Jina/generic HTTP scrape) are a separate research-provider abstraction from chat/media fallback providers.
