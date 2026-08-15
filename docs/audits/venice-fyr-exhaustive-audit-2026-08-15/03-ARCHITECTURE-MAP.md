# 03-ARCHITECTURE-MAP.md — Venice Fyr Android Architecture Map

**Audit scope:** `app`, `venice-sdk`, `core:*` production sources, manifests, and build files.  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `main`/`1da3142`.  
**Venice API source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream `6e69346b`, `info.version 20260814.194349`.  
**Desktop parity mirror:** `.source/Venice_Forge-desktop` (read-only).

---

## 1. Module Boundaries & Dependency Direction

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  :app                                                                       │
│  Android UI (Compose) + ViewModels + feature catalog + navigation scaffold  │
│  Depends on: :venice-sdk, :core:common, :core:security, :core:data,       │
│              :core:designsystem                                             │
└──────┬──────────────────────────────────────────────────────────────────────┘
       │
       │   public Venice API client facade
       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  :venice-sdk                                                                │
│  Typed HTTP clients for chat, image, audio, video; model discovery;         │
│  request/response DTOs; SSE streaming; structured exceptions.               │
│  Depends on: :core:common, OkHttp, Kotlinx Serialization, Coroutines.       │
└──────┬──────────────────────────────────────────────────────────────────────┘
       │
       │   shared primitives / redaction / exceptions
       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  :core:common                                                               │
│  Redactor, shared exception primitives, future cross-module utilities.      │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────────┐
│  :core:security      │  │  :core:data          │  │  :core:designsystem      │
│  SecureSecretStore   │  │  Room DB, DAOs,      │  │  Compose theme, tokens,  │
│  (Keystore-backed)   │  │  repositories        │  │  CodexPet, indicators    │
└──────────────────────┘  └──────────────────────┘  └──────────────────────────┘
```

**Dependency rules (from `ANDROID_PORT_HANDOFF.md`):**
- `:venice-sdk` is public and reusable; it must not persist credentials or contain UI.
- `:core:security` owns Keystore/profile credential handling.
- `:core:data` owns Room schema/repositories/migrations.
- `:core:designsystem` owns Compose tokens/components/themes.
- `:core:common` owns redaction/errors/shared primitives.

**Observed violations / gaps:**
- `:app` currently mixes DI, ViewModel creation, and UI in one place (`VeniceForgeApp.kt`).
- `:core:data` is used, but there is no `:core:jobs` or `:core:media` module yet, so background work and media handling live inside `:app`.
- `:venice-sdk` correctly depends only on `:core:common`.

---

## 2. Data & Execution Flows

### 2.1 API-Key Loading

```
User types key in ConfigScreen
        │
        ▼
SecureSecretStore.saveApiKey(profileId, apiKey)
        │
        ▼
AES-GCM key in Android Keystore ──► encrypts plaintext
        │
        ▼
Base64(iv + ciphertext) written to SharedPreferences
```

- `SecureSecretStore` is constructed in `VeniceForgeApp.kt:73` and passed down to `ConfigScreen`, `ChatViewModel`, and `ImageViewModel` as a lambda `apiKeyProvider`.
- The SDK itself never persists keys (`VeniceForgeSdk.kt` comment lines 21–25).

### 2.2 Chat Streaming

```
ChatScreen.submit(text)
        │
        ▼
ChatViewModel.submit(text)
        │
        ├──► persists user message + pending assistant placeholder in Room
        │
        ├──► loads prior completed messages from Room
        │
        ├──► builds ChatRequest(messages, stream=true)
        │
        ▼
ChatClient.streamChat(apiKey, request)
        │
        ├──► OkHttp POST /chat/completions (Accept: text/event-stream)
        │
        ├──► SseLineParser reads lines
        │
        ├──► parseChunks emits Delta / ToolCallDelta / Finish / Error
        │
        ▼
ChatViewModel.collects chunks
        │
        ├──► ChatStreamAccumulator accumulates text/tool-calls
        │
        └──► updates assistant message text/status in Room + _state UI
```

- The stream runs on `Dispatchers.IO` via `callbackFlow.flowOn(Dispatchers.IO)`.
- Cancellation propagates from the consumer coroutine to the OkHttp `Call` (`ChatClient.kt:40–44`).

### 2.3 Image Generation / Edit

```
ImageScreen (Photo Picker or prompt)
        │
        ▼
ImageViewModel.generateImage() / editImage()
        │
        ├──► builds GenerateImageRequest / EditImageRequest
        │
        ▼
ImageClient.generateBinary() / edit()
        │
        ├──► POST /image/generate or /image/edit
        │
        ▼
ImageViewModel.saveBytesToCache(bytes)
        │
        ▼
context.cacheDir file + Uri.fromFile(file)
        │
        ▼
ImageScreen displays bitmap via BitmapFactory.decodeFile(path)
```

- Input images come through `ActivityResultContracts.PickVisualMedia` (no storage permission required).
- Generated images are written to the app-private cache directory.

### 2.4 Model Discovery

```
VeniceForgeApp LaunchedEffect(profileId)
        │
        ▼
CapabilitiesRepository.fetchLiveCapabilities(apiKey)
        │
        ├──► GET /models
        │
        ├──► GET /models/traits
        │
        ├──► GET /models/compatibility_mapping
        │
        ▼
ModelCatalog
        │
        ├──► defaultTextModelId resolved from traits or first text model
        │
        └──► filtered model lists passed to ChatScreen / ImageScreen
```

- Model IDs and capabilities are discovered at runtime; no hard-coded catalog is shipped in production code.
- `ModelCatalog.defaultTextModelId` falls back to the first available text model, satisfying the “never hard-code a current Venice model catalog” rule.

### 2.5 Persistence

```
AppDatabase (Room)
        │
        ├──► ProfileEntity / ProfileDao
        ├──► ConversationEntity / ConversationDao
        ├──► ConversationFolderEntity
        ├──► MessageEntity / MessageDao
        └──► MessageToolCallEntity / MessageToolCallDao
        │
        ▼
ChatRepository / ProfileRepository
        │
        ▼
DataServices (service locator)
        │
        ▼
:app ViewModels
```

- `DataServices.create(context)` builds the Room database and exposes repositories.
- All repository methods are profile-scoped (`profileId`) to enforce isolation.

---

## 3. DI Strategy

**Current strategy:** manual service-locator inside Compose.

- `VeniceForgeApp.kt` creates:
  - `SecureSecretStore(context)`
  - `VeniceForgeSdk()`
  - `DataServices.create(context)`
  - `ChatClient(sdk)`
  - `CapabilitiesRepository(sdk)`
- These objects are held in `remember`, so they survive recompositions but **not** Activity recreation or process death.
- ViewModels (`ChatViewModel`, `ImageViewModel`) are also created directly in `remember`, so they are **not** retained by the framework.

**Implications:**
- No singleton application-scoped objects.
- No dependency-injection graph (Hilt/Koin/manual factory).
- ViewModels are not lifecycle-aware in the Android framework sense.

**Recommended direction:**
- Adopt `androidx.lifecycle.viewmodel.compose.viewModel()` with a custom `ViewModelProvider.Factory`.
- Hold `DataServices` as an application singleton (e.g., in `Application.onCreate` or via DI).
- Keep `:venice-sdk` clients stateless and inject them into ViewModels.

See `findings/architecture.md` ARCH-01, ARCH-03, ARCH-11 for detailed findings.

---

## 4. Singleton / Hidden Global State

| Component | Singleton? | Evidence | Risk |
|-----------|-----------|----------|------|
| `AppDatabase` | No | `DataServices.create` builds a new instance each call | Multiple Room instances |
| `DataServices` | No | Created in `remember` | Recreated on config change |
| `SecureSecretStore` | No | Created in `remember` | Harmless (same prefs/keystore aliases) |
| `VeniceForgeSdk` | No | Created in `remember` | New OkHttpClient per composition |
| `ChatClient` / `ImageClient` / `AudioClient` / `VideoClient` | Stateless | Created on demand | Low |
| `CapabilitiesRepository` | No | Created in `remember` | Re-fetches catalog on recreation |
| `FeatureCatalog` | Object | `object FeatureCatalog` | Safe read-only catalog |
| `Redactor` | Object | `object Redactor` | Safe, but unused |

**Hidden global state:** none detected beyond the Android framework `SharedPreferences` and Keystore entries used by `SecureSecretStore`.

---

## 5. Duplicate Network Layers / Serializers

**Network layer:** OkHttp is the single HTTP engine. Each SDK client creates its own `Json` instance:

| Client | Json config | Notes |
|--------|-------------|-------|
| `VeniceForgeSdk` | `Json { ignoreUnknownKeys = true }` | Used for `/models` and error parsing |
| `ChatClient` | `Json { ignoreUnknownKeys = true }` | Used for request/response serialization |
| `ImageClient` | `Json { ignoreUnknownKeys = true; encodeDefaults = false }` | Omits null fields |
| `AudioClient` | `Json { ignoreUnknownKeys = true; encodeDefaults = false }` | |
| `VideoClient` | `Json { ignoreUnknownKeys = true; encodeDefaults = false }` | |
| `CapabilitiesRepository` | `Json { ignoreUnknownKeys = true }` | |

**Observations:**
- The duplication is not a functional bug today because each client’s DTOs are independent.
- However, the inconsistent `encodeDefaults` flag means a future shared DTO could serialize differently depending on which client uses it.
- There is no central OkHttp interceptor for logging, retry, or auth; each request builds headers inline. This is acceptable for a starter but will become a maintenance burden as retries/rate-limits are added.

**No UI-to-transport bypasses detected.** The `:app` module always goes through `:venice-sdk` clients; it does not construct raw OkHttp requests or bypass the SDK.

---

## 6. Ledger Summary

See `findings/architecture.md` for the complete review ledger. High-level counts:

- **Files reviewed:** 58 production source/manifest/build/resource files (~4,038 lines).
- **Modules covered:** `:app`, `:venice-sdk`, `:core:common`, `:core:security`, `:core:data`, `:core:designsystem`.
- **Actionable architecture findings:** 17 (full list in `findings/architecture.md`).
- **Severity distribution:** P0: 0, P1: 3, P2: 12, P3: 2.

---

## 7. Architecture Strengths

1. **Module boundaries are mostly respected:** `:venice-sdk` has no UI or credential persistence; `:core:security` owns secrets; `:core:data` owns Room.
2. **Runtime model discovery:** No hard-coded model catalog; capabilities come from `/models`, `/models/traits`, `/models/compatibility_mapping`.
3. **Secure API-key storage:** Keystore-backed AES-GCM with ciphertext in SharedPreferences, no plaintext persistence.
4. **Profile-scoped repositories:** DAO queries include `profileId` to enforce isolation.
5. **Streaming design:** `callbackFlow` + `flowOn(Dispatchers.IO)` with cancellation propagation is a sound pattern.
6. **No WebView wrapper, no telemetry, no broad storage permissions:** Aligns with project boundaries.
