# Architecture State

The module boundaries remain sound: Compose app → app ViewModels → `:venice-sdk` / `:core:data`; Room is app-owned; Keystore-backed credentials remain in `:core:security`; the SDK accepts but never persists API keys.

Chat uses framework-owned ViewModels, `SavedStateHandle` for profile/conversation identity, Room-backed history, and an asynchronous OkHttp SSE call. Image uses a framework-owned ViewModel but retains selected/input/result state only in memory and writes generated media to cache. Audio/video APIs are SDK-only and have no durable Android job layer.

Only `settings`, `chat`, and `image` have dedicated root screens. Every other stable feature ID routes to `FeatureScreen`. This is truthfully labeled alpha/scaffolded and remains the dominant product-completeness gap.
