# Performance & Concurrency Audit — Venice Fyr Android

**Scope:** `:app`, `:venice-sdk`, `:core:data`, `:core:designsystem`, `:core:security` production sources.  
**Methodology:** Static source review only. No runtime measurement, profiling, or instrumentation was performed; all findings are classified by the strength of static evidence.  
**Status legend:**
- **CONFIRMED** — the code path is present and the impact follows directly from inspection.
- **INFERRED** — the issue is strongly implied by the code but its severity depends on runtime conditions not measured here.

---

## Executive summary

The codebase exhibits a consistent pattern of **synchronous blocking work on the Android main thread**, **unbounded resource growth**, and **missing concurrency guards** in paid/mutating operations. The most severe issues are:

1. **Main-thread crypto and bitmap decoding** in ViewModels and Compose (VM-05, VM-06, APP-UI-007, ARCH-08, ARCH-13).
2. **Duplicate submission races** for chat and image generation (VM-01, VM-04, ARCH-04).
3. **Unbounded image cache** and **undisposed native bitmaps** (ARCH-09, APP-UI-008).
4. **Redundant triple network calls** for model discovery with no cache (SDK-CORE-07).
5. **Non-singleton Room/ViewModel instances** causing repeated initialization and leaked scopes (ARCH-01, ARCH-03).

Because no runtime measurement was performed, the actual ANR frequency, memory pressure, and race windows are **INFERRED** in severity even when the code defect is **CONFIRMED**.

---

## 1. Main-thread blocking work

### PERF-01 | Bitmap decoding on the main thread during composition
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-007, ARCH-08
- **Area:** UI / Performance / ANR risk
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt:180–186`
- **Evidence:**
  ```kotlin
  val decodedBitmap = remember(state.resultImageUri) {
      try {
          state.resultImageUri.path?.let { BitmapFactory.decodeFile(it) }
      } catch (e: Exception) { null }
  }
  ```
  `BitmapFactory.decodeFile` runs synchronously inside `remember` during composition on the main thread.
- **Impact:** Large generated images can freeze the UI and trigger ANRs.
- **Remediation:** Use Coil `AsyncImage` or decode in `produceState`/`LaunchedEffect` with `Dispatchers.IO` and show a placeholder.

### PERF-02 | Base64 image decoding on the main thread in the ViewModel
- **Status:** CONFIRMED
- **Finding IDs:** ARCH-13, VM-06
- **Area:** Image generation / Performance / ANR risk
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:111–114`
- **Evidence:**
  ```kotlin
  val base64 = response.images?.firstOrNull()
  val uri = if (base64 != null) {
      val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
      saveBytesToCache(decodedBytes)
  } else null
  ```
  The decode runs inside `viewModelScope.launch` with no explicit dispatcher, so it executes on the main dispatcher.
- **Impact:** ANR/jank when editing high-resolution images.
- **Remediation:** Wrap `Base64.decode` and `saveBytesToCache` in `withContext(Dispatchers.IO)`.

### PERF-03 | Keystore decryption on the main thread
- **Status:** CONFIRMED
- **Finding IDs:** VM-05
- **Area:** Security / Performance / ANR risk
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt:88`, `android/image/ImageViewModel.kt:56,88`, `android/VeniceForgeApp.kt:125`
- **Evidence:** `apiKeyProvider` is implemented as `{ secureStore.loadApiKey(pid) }`. `SecureSecretStore.loadApiKey` performs AES-GCM decryption via Android Keystore. It is called synchronously from non-suspend ViewModel functions and from `VeniceForgeApp` inside a `LaunchedEffect`, all on the main thread.
- **Impact:** UI jank or ANR; cryptographic operations must not run on the main thread.
- **Remediation:** Make `apiKeyProvider` a `suspend` lambda and call it inside `viewModelScope.launch`, or dispatch to `Dispatchers.IO` before reading the key.

### PERF-04 | CodexPet spritesheet decoding on the main thread
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-008 (related)
- **Area:** Design system / Performance
- **Files:** `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt:81–87`
- **Evidence:** `androidBitmap?.asImageBitmap()` is produced inside `remember(spritesheetRes)` on the main thread. The asset is small, but the pattern is the same as PERF-01.
- **Impact:** Low for the current asset size; native bitmap memory is still allocated on the main thread.
- **Remediation:** Decode on a background dispatcher if the spritesheet grows, or ensure disposal (see RES-01).

---

## 2. Resource leaks and unbounded growth

### RES-01 | `ImageBitmap` native resources never disposed
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-008
- **Area:** Design system / Native memory
- **Files:** `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt:81–87, 103–110`
- **Evidence:** `ImageBitmap` is created inside `remember(spritesheetRes)` but never disposed. `DisposableEffect` is imported but unused.
- **Impact:** Native bitmap memory leak; potential `OutOfMemoryError` if many CodexPet instances are created/destroyed.
- **Remediation:** Wrap in `DisposableEffect(spritesheetRes) { ... onDispose { imageBitmap?.asAndroidBitmap()?.recycle() } }`.

### RES-02 | Generated image cache files are never cleaned up
- **Status:** CONFIRMED
- **Finding IDs:** ARCH-09
- **Area:** Storage / Cache management
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:111–115`
- **Evidence:**
  ```kotlin
  val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
  file.writeBytes(bytes)
  ```
  No deletion, LRU policy, or size cap exists.
- **Impact:** Unbounded disk growth until the OS clears cache or the app is uninstalled.
- **Remediation:** Implement a content-addressed cache with max-size eviction; store metadata in Room, not base64 blobs.

### RES-03 | Room and DataServices recreated per composition
- **Status:** CONFIRMED
- **Finding IDs:** ARCH-03
- **Area:** Architecture / DI / Memory
- **Files:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt:18–20`, `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt:36–43`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:77`
- **Evidence:** `val data = remember { DataServices.create(context) }` creates a new `AppDatabase` every time the Composition is recreated (configuration change, process restart, etc.).
- **Impact:** Multiple Room instances pointing at the same database file can cause connection leaks, inconsistent query caching, and higher memory usage.
- **Remediation:** Initialize `DataServices` once in `Application.onCreate` or expose it via a DI singleton.

### RES-04 | ViewModels recreated on configuration changes
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-001, ARCH-01
- **Area:** Lifecycle / Memory / State loss
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:86–120`
- **Evidence:** `ChatViewModel` and `ImageViewModel` are instantiated with plain `remember`, not `viewModel()` or `ViewModelProvider`.
- **Impact:** Old ViewModel instances are dropped but keep running; `viewModelScope` jobs leak, and all UI state is lost on rotation.
- **Remediation:** Obtain ViewModels via `viewModel()` with a custom factory keyed by `profileId`.

---

## 3. Concurrency races and duplicate submissions

### RACE-01 | Rapid taps submit duplicate chat prompts
- **Status:** CONFIRMED
- **Finding IDs:** ARCH-04, VM-01
- **Area:** Chat / Duplicate submission
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt:150–163`, `android/chat/ChatScreen.kt:110–117`
- **Evidence:** `isStreaming` becomes `true` only after the first SSE chunk arrives. Between tapping Send and receiving the first chunk, the button remains enabled and additional taps launch duplicate coroutines. `streamJob` is assigned inside the async block, so a second assignment overwrites the first and leaves an orphan stream.
- **Impact:** Duplicate user messages, duplicate paid API calls, inconsistent conversation context, and loss of cancellation control.
- **Remediation:** Set `isStreaming = true` at the top of `submit()` before launching the network call, and/or guard with an atomic in-flight flag and assign `streamJob` before launching the streaming child.

### RACE-02 | Image generation/edit has no ViewModel-level duplicate guard
- **Status:** CONFIRMED
- **Finding IDs:** VM-04
- **Area:** Image / Duplicate submission
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:52–81, 83–121`
- **Evidence:** `generateImage()` and `editImage()` read `_uiState.value`, validate inputs, then set `isGenerating = true` and launch. There is no check of `state.isGenerating` at the start, and no `Job` reference is kept.
- **Impact:** Duplicate billed image generations/edits; wasted credits; UI state thrashing.
- **Remediation:** Add `if (_uiState.value.isGenerating) return` at the top of both functions; keep a single `Job` reference for cancellation.

### RACE-03 | `CapabilitiesRepository` catches `CancellationException`
- **Status:** CONFIRMED
- **Finding IDs:** SDK-CORE-14
- **Area:** Model discovery / Coroutine cancellation
- **Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt:94–120`
- **Evidence:** `catch (_: Exception)` catches `CancellationException` and returns `emptyMap()`, so the coroutine appears to complete successfully with partial data.
- **Impact:** UI/components cannot distinguish cancellation from success; partial catalog data may be used as complete.
- **Remediation:** Catch only `IOException`/`JsonException`; let `CancellationException` propagate, or check `coroutineContext.isActive` before returning.

### RACE-04 | `ChatClient` swallows `CancellationException`
- **Status:** CONFIRMED
- **Finding IDs:** CHAT-11
- **Area:** Streaming / Coroutine cancellation
- **Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt:83–84`
- **Evidence:** `catch (e: CancellationException) { /* ... */ }` swallows the exception; the flow simply completes.
- **Impact:** Downstream collectors cannot distinguish cancellation from normal completion; UI may treat a canceled request as successful.
- **Remediation:** Do not catch `CancellationException`; let `callbackFlow` close naturally, or rethrow after cleanup.

### RACE-05 | `ImageViewModel` reports cancellation as user-facing error
- **Status:** CONFIRMED
- **Finding IDs:** VM-03
- **Area:** Image / Coroutine cancellation
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:65–80, 97–120`
- **Evidence:** Both image operations use `catch (e: Exception)`, which catches `CancellationException` and surfaces it as `error = e.message`.
- **Impact:** Normal scope cancellation (ViewModel cleared, user navigates away) is displayed as an error.
- **Remediation:** Catch `CancellationException` explicitly and re-throw it; catch only non-cancellation exceptions for UI error state.

---

## 4. Redundant and background work

### NET-01 | Triple sequential network calls for model catalog with no cache
- **Status:** CONFIRMED
- **Finding IDs:** SDK-CORE-07
- **Area:** Model discovery / Network efficiency
- **Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt:21–92`
- **Evidence:**
  ```kotlin
  suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
      val models = sdk.listModels(apiKey, null)
      val traitsMap = fetchTraits(apiKey)
      val compatMap = fetchCompatibility(apiKey)
      ...
  }
  ```
  Every invocation performs three sequential network requests with no cache, TTL, or staleness check.
- **Impact:** Higher latency, more bandwidth, and unnecessary load on Venice endpoints. `ModelCatalog.refreshedAt` is unused.
- **Remediation:** Add an in-memory cache keyed by API key + type with a configurable TTL (e.g., 5 minutes) and expose a `forceRefresh` parameter.

### NET-02 | Flows collected with `collectAsState` instead of lifecycle-aware variant
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-003
- **Area:** Compose / Background work
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt:39`, `android/image/ImageScreen.kt:47`
- **Evidence:** `collectAsState()` is used although `androidx.lifecycle.runtime.compose` is available and provides `collectAsStateWithLifecycle()`.
- **Impact:** Flows keep collecting while the composition exists, even when the app is in the background, wasting CPU/battery.
- **Remediation:** Replace `collectAsState()` with `collectAsStateWithLifecycle()`.

### NET-03 | WorkManager declared but unused for durable background work
- **Status:** CONFIRMED
- **Finding IDs:** ARCH-06
- **Area:** Background work / Lifecycle
- **Files:** `app/build.gradle.kts:51`
- **Evidence:** `implementation(libs.androidx.work.runtime)` is declared but no `androidx.work` imports exist in `app/src/main/java`.
- **Impact:** Process death cancels queued or in-flight generation jobs; no retry; no notification/progress surface.
- **Remediation:** Implement `Worker` classes for video/audio/music queued jobs; enqueue them from ViewModels. (This is architecture, not pure performance, but it directly affects background durability.)

---

## 5. Additional concurrency/performance observations

### OBS-01 | `FeatureCatalog.byId` throws on unknown IDs
- **Status:** CONFIRMED
- **Finding IDs:** APP-UI-002, ARCH-14, VM-15
- **Area:** Navigation / Crash
- **Files:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:47`
- **Evidence:** `fun byId(id: String): AppFeature = all.first { it.id == id }` throws `NoSuchElementException` for unknown IDs.
- **Performance impact:** Startup crash on restore if `rememberSaveable` holds a stale feature ID; not a hot-path performance issue but a lifecycle/robustness concern.
- **Remediation:** Use `firstOrNull` and return `AppFeature?`.

### OBS-02 | Multipart uploads unsupported for image/media endpoints
- **Status:** CONFIRMED
- **Finding IDs:** IMG-05, AUD-04
- **Area:** Media upload / Memory
- **Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:36–44, 65–74`
- **Evidence:** All image endpoints send `Content-Type: application/json` only; callers must base64-encode files.
- **Performance impact:** Increased payload size and CPU/memory cost on Android; large images may hit memory limits.
- **Remediation:** Add multipart upload variants using `MultipartBody.Builder`.

### OBS-03 | No concurrency or lifecycle stress tests exist
- **Status:** CONFIRMED
- **Finding IDs:** TEST-MISSING-20
- **Area:** Test coverage
- **Evidence:** All existing tests are sequential. No tests cover concurrent stream collection, rapid `submit()`/`cancel()` cycles, repository operations under concurrent flows, or process-death recovery.
- **Impact:** The races and leaks identified above are not caught by CI.
- **Remediation:** Add concurrency and lifecycle stress tests using `kotlinx-coroutines-test` and Robolectric/instrumented recreation tests.

---

## Summary table

| ID | Theme | Status | Primary file | Remediation owner |
|----|-------|--------|--------------|-------------------|
| PERF-01 | Main-thread bitmap decode | CONFIRMED | `ImageScreen.kt:180` | WP-11 / WP-13 |
| PERF-02 | Main-thread base64 decode | CONFIRMED | `ImageViewModel.kt:111` | WP-13 |
| PERF-03 | Main-thread Keystore decrypt | CONFIRMED | `ChatViewModel.kt:88` | WP-08 / WP-13 |
| PERF-04 | Main-thread spritesheet decode | CONFIRMED | `CodexPet.kt:81` | WP-11 |
| RES-01 | Undisposed `ImageBitmap` | CONFIRMED | `CodexPet.kt:81` | WP-11 |
| RES-02 | Unbounded image cache | CONFIRMED | `ImageViewModel.kt:111` | WP-12 |
| RES-03 | Non-singleton Room/DataServices | CONFIRMED | `DataServices.kt:18` | WP-09 |
| RES-04 | `remember`-created ViewModels | CONFIRMED | `VeniceForgeApp.kt:86` | WP-08 |
| RACE-01 | Duplicate chat submissions | CONFIRMED | `ChatViewModel.kt:150` | WP-08 |
| RACE-02 | Duplicate image submissions | CONFIRMED | `ImageViewModel.kt:52` | WP-08 |
| RACE-03 | Swallowed cancellation in catalog | CONFIRMED | `CapabilitiesRepository.kt:94` | WP-06 |
| RACE-04 | Swallowed cancellation in chat stream | CONFIRMED | `ChatClient.kt:83` | WP-03 |
| RACE-05 | Cancellation shown as image error | CONFIRMED | `ImageViewModel.kt:65` | WP-08 |
| NET-01 | Triple uncached model calls | CONFIRMED | `CapabilitiesRepository.kt:21` | WP-06 / WP-13 |
| NET-02 | Non-lifecycle flow collection | CONFIRMED | `ChatScreen.kt:39` | WP-11 |
| NET-03 | WorkManager unused | CONFIRMED | `app/build.gradle.kts:51` | WP-12 |
| OBS-01 | Feature catalog throws | CONFIRMED | `FeatureCatalog.kt:47` | WP-08 |
| OBS-02 | JSON-only media uploads | CONFIRMED | `ImageClient.kt:36` | WP-04 |
| OBS-03 | Missing stress tests | CONFIRMED | test suites | WP-14 |

## Recommended validation (no runtime measurement performed)

Because this audit was static only, the following runtime checks should be performed after remediation:

1. **StrictMode** disk-read/network-on-main detection during image generation/edit and chat submit.
2. **Systrace/Profiler** verification that `BitmapFactory.decodeFile`, `Base64.decode`, and `SecureSecretStore.loadApiKey` do not run on `android.ui` thread.
3. **LeakCanary** run with repeated CodexPet create/destroy and image generation cycles.
4. **Stress test:** 100 rapid `ChatViewModel.submit()` taps and `ImageViewModel.generateImage()` taps; assert exactly one network request and one persisted message.
5. **Cache size audit:** generate images until cache cap; assert eviction and no unbounded growth.
6. **Network call counting:** invoke model discovery repeatedly; assert triple call only on first invocation and cache hit within TTL.
7. **Process-death / rotation** profiler snapshot: assert ViewModel instance count does not grow and `AppDatabase` instances remain at one per process.
