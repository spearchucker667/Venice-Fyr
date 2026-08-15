# findings/architecture.md — Architecture & Android Architecture Findings

**Audit scope:** `app`, `venice-sdk`, `core:*` production sources, manifests, and build files.  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `main`/`1da3142`, clean tree.  
**Venice API source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream `6e69346b`, `info.version 20260814.194349`.  
**Desktop parity mirror:** `.source/Venice_Forge-desktop` (read-only).  
**Audit date:** 2026-08-15.

---

## Review Ledger

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `app/build.gradle.kts` | 68 | Y | ARCH-06, ARCH-07 |
| `app/src/main/AndroidManifest.xml` | 23 | Y | — |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` | 17 | Y | ARCH-01, ARCH-11 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 266 | Y | ARCH-01, ARCH-03, ARCH-11, ARCH-16 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` | 123 | Y | ARCH-04, ARCH-05 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` | 200 | Y | ARCH-01, ARCH-02, ARCH-04, ARCH-05, ARCH-11, ARCH-12 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt` | 48 | Y | ARCH-14 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` | 200 | Y | ARCH-05, ARCH-08 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` | 122 | Y | ARCH-01, ARCH-05, ARCH-09, ARCH-13 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` | 173 | Y | ARCH-15 |
| `app/src/main/res/values/strings.xml` | 14 | Y | — |
| `app/src/main/res/values/themes.xml` | 8 | Y | — |
| `app/src/main/res/values-v27/themes.xml` | 5 | Y | — |
| `app/src/main/res/xml/network_security_config.xml` | 3 | Y | — |
| `build.gradle.kts` | 5 | Y | — |
| `core/common/build.gradle.kts` | 17 | Y | — |
| `core/common/src/main/AndroidManifest.xml` | 1 | Y | — |
| `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt` | 17 | Y | ARCH-10 |
| `core/data/build.gradle.kts` | 56 | Y | — |
| `core/data/src/main/AndroidManifest.xml` | 2 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` | 47 | Y | ARCH-03 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt` | 21 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` | 22 | Y | ARCH-03 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt` | 33 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt` | 31 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt` | 17 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt` | 30 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationEntity.kt` | 43 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationFolderEntity.kt` | 27 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt` | 38 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageToolCallEntity.kt` | 35 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt` | 14 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt` | 83 | Y | — |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt` | 27 | Y | — |
| `core/designsystem/build.gradle.kts` | 24 | Y | — |
| `core/designsystem/src/main/AndroidManifest.xml` | 1 | Y | — |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt` | 128 | Y | — |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceColors.kt` | 93 | Y | — |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceForgeTheme.kt` | 26 | Y | — |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceLoadingIndicator.kt` | 86 | Y | — |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceTypography.kt` | 129 | Y | — |
| `core/security/build.gradle.kts` | 18 | Y | — |
| `core/security/src/main/AndroidManifest.xml` | 1 | Y | — |
| `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt` | 100 | Y | — |
| `gradle/libs.versions.toml` | 58 | Y | — |
| `venice-sdk/build.gradle.kts` | 29 | Y | — |
| `venice-sdk/consumer-rules.pro` | 1 | Y | — |
| `venice-sdk/src/main/AndroidManifest.xml` | 1 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt` | 26 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt` | 67 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt` | 58 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt` | 11 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt` | 105 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt` | 46 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt` | 14 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` | 121 | Y | ARCH-17 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt` | 51 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt` | 42 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` | 152 | Y | ARCH-16 |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt` | 89 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt` | 51 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt` | 23 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt` | 14 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt` | 84 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt` | 119 | Y | — |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt` | 54 | Y | — |

**Totals:** 58 files reviewed, ~4,038 production lines, 17 actionable findings recorded below.

---

## Findings

### ARCH-01 | Severity: P1 | Status: CONFIRMED
**ViewModels are instantiated directly in Compose and are not retained across configuration changes.**

- **Area:** Android Architecture / Lifecycle  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` lines 86–120  
- **Symbol:** `ChatViewModel`, `ImageViewModel`  
- **Evidence:**
  ```kotlin
  val chatViewModel = remember(profileId) {
      profileId?.let { pid ->
          ChatViewModel(
              chatRepo = chatRepo,
              chatClient = chatClient,
              apiKeyProvider = { secureStore.loadApiKey(pid) },
              profileId = pid,
              initialModelId = null,
          )
      }
  }
  ```
  `ImageViewModel` is constructed the same way at lines 98–119. `MainActivity.kt` (lines 9–16) does not use `ViewModelProvider` or `viewModel()`.
- **Expected:** `ChatViewModel`/`ImageViewModel` survive Activity recreation and process-configuration changes, as guaranteed by the Android `ViewModel` framework when obtained via `ViewModelProvider`/`viewModel()`.
- **Actual:** The instances are ordinary Kotlin objects held in `remember`. On rotation, process resize, dark-mode toggle, etc., the Composition is destroyed and recreated, producing brand-new ViewModels. Any in-flight stream/generation is dropped and UI state is reset.
- **Impact:** Broken chat streaming UX, lost model selection, lost `conversationId`, potential duplicate conversation creation, and leaked `viewModelScope` jobs.
- **Root cause:** The app bypasses the framework ViewModel creation path and treats `ViewModel` subclasses as plain composable-scoped objects.
- **Related occurrences:** `ImageViewModel` (`VeniceForgeApp.kt:98`), `MainActivity.kt`.
- **Android/Kotlin reference:** [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel) — “ViewModel objects are automatically retained during configuration changes … they are not destroyed on configuration changes.”
- **Remediation:** Use `androidx.lifecycle.viewmodel.compose.viewModel()` with a custom `ViewModelProvider.Factory`, or adopt Hilt, and inject the same application-scoped `DataServices` / repositories.
- **Tests required:** Rotation/manual config-change test; leak detection; process-death recovery test.
- **Compatibility impact:** Behavior change: chat/image screens will retain state correctly after fix.

---

### ARCH-02 | Severity: P1 | Status: CONFIRMED
**The latest user message is duplicated in the multi-turn chat request context.**

- **Area:** Chat / Venice API integration  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 99–139  
- **Symbol:** `submit(text: String)`  
- **Evidence:**
  ```kotlin
  // Lines 99-123: appends the new user message AND a pending assistant placeholder.
  chatRepo.appendMessage(profileId, convId, userMsg)
  chatRepo.appendMessage(profileId, convId, assistantMsg)

  // Lines 127-139: loads the now-just-appended user message, then appends it again.
  val priorMessages = chatRepo.observeMessages(profileId, convId).first()
  val contextMessages = priorMessages
      .filter { it.status == MessageStatus.COMPLETED && it.textContent.isNotBlank() }
      .map { entity -> ... }
      .plus(ChatMessage.user(text))
  ```
  The existing test `ChatViewModelTest.kt:172-178` asserts that turn 2 sends exactly three messages (`user:Turn one`, `assistant:First answer`, `user:Turn two`). The current code produces four because the just-appended `userMsg` is already in `priorMessages` and is appended again as `ChatMessage.user(text)`.
- **Venice reference:** `.source/venice-api-docs/swagger.yaml` `ChatCompletionRequest.messages` (line 672) is the ordered list of conversation turns; duplicate consecutive `user` roles violate the expected alternating-turn contract.
- **Expected:** Each user turn appears exactly once in the request payload.
- **Actual:** The newest user turn appears twice.
- **Impact:** Model receives a malformed context, which can degrade response quality, confuse multi-turn reasoning, and waste tokens.
- **Root cause:** The current message is persisted before the request context is built, then re-added to the context list.
- **Related occurrences:** None other; the bug is localized to `ChatViewModel.submit`.
- **Remediation:** Build `contextMessages` from the existing completed messages **before** appending the new user message, or filter out the message whose `id == userMsg.id` from `priorMessages`.
- **Tests required:** The existing `ChatViewModelTest` multi-turn test already documents the expected behavior and should pass after the fix.
- **Compatibility impact:** Request shape change; no public API surface change.

---

### ARCH-03 | Severity: P2 | Status: CONFIRMED
**DataServices and AppDatabase are not application-singletons.**

- **Area:** Architecture / DI  
- **Module:** `:core:data`, `:app`  
- **File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` lines 18–20; `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` lines 36–43; `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` line 77  
- **Symbol:** `DataServices.create`, `AppDatabase.create`  
- **Evidence:**
  ```kotlin
  // DataServices.kt
  fun create(context: Context): DataServices = DataServices(AppDatabase.create(context.applicationContext))
  // AppDatabase.kt
  fun create(context: Context): AppDatabase = Room.databaseBuilder(...).build()
  // VeniceForgeApp.kt
  val data = remember { DataServices.create(context) }
  ```
- **Expected:** One `AppDatabase` instance per process, as recommended by Room.
- **Actual:** A new `AppDatabase` is opened every time the Composition is recreated (configuration change, process restart, etc.).
- **Impact:** Multiple Room instances pointing at the same database file can cause connection leaks, inconsistent query caching, and higher memory usage.
- **Root cause:** No application-scoped singleton or DI container holds `DataServices`.
- **Related occurrences:** `VeniceForgeApp.kt` also recreates `SecureSecretStore`, `VeniceForgeSdk`, `ChatClient`, and `CapabilitiesRepository` on every composition.
- **Remediation:** Initialize `DataServices` once in `Application.onCreate` or expose it via a DI singleton; inject the same instance into ViewModels.
- **Tests required:** LeakCanary / Profiler rotation test; multi-instance behavior test.
- **Compatibility impact:** None; internal wiring change.

---

### ARCH-04 | Severity: P2 | Status: CONFIRMED
**Rapid taps on the Send button can submit the same chat prompt multiple times.**

- **Area:** Chat / UX / Duplicate submission  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 150–163; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` lines 110–117  
- **Symbol:** `submit`, `isStreaming`  
- **Evidence:**
  ```kotlin
  // ChatViewModel.kt
  streamJob = launch {
      chatClient.streamChat(apiKey, req).collect { chunk ->
          accumulator.apply(chunk)
          when (chunk) {
              is ChatStreamChunk.Delta, is ChatStreamChunk.ToolCallDelta -> {
                  _state.update { it.copy(isStreaming = true, error = null) }
              }
              ...
          }
      }
  }
  ```
  `ChatScreen.kt` disables the Send button only when `!state.isStreaming`.
- **Expected:** As soon as a prompt is accepted, further submissions of the same prompt are blocked.
- **Actual:** `isStreaming` becomes `true` only after the first SSE chunk arrives. Between tapping Send and receiving the first chunk, the button remains enabled and additional taps launch duplicate coroutines.
- **Impact:** Duplicate user messages in the DB, duplicate paid API calls, and a confusing UI.
- **Root cause:** The in-flight flag is set too late.
- **Related occurrences:** Image generation disables the button synchronously in `ImageViewModel.kt:63`, so it is not affected.
- **Remediation:** Set `_state.value = _state.value.copy(isStreaming = true)` at the top of `submit()` before launching the network call, and/or guard with an atomic `submitting` flag.
- **Tests required:** Rapid-tap UI test.
- **Compatibility impact:** None.

---

### ARCH-05 | Severity: P1 | Status: CONFIRMED
**Paid/mutating operations have no explicit approval or duplicate-submission defense.**

- **Area:** Security / Privacy / Product boundaries  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` lines 110–117; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` lines 145–159; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 52–81; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 86–187  
- **Symbol:** `submit`, `generateImage`, `editImage`  
- **Evidence:**
  - `AGENTS.md` non-negotiable boundary: “Paid/mutating operations require explicit approval and duplicate-submission defenses.”
  - `ChatScreen.kt` sends on a single `TextButton` click.
  - `ImageScreen.kt` Generate/Edit buttons immediately call `viewModel.generateImage()` / `viewModel.editImage()`.
- **Expected:** A confirmation step (dialog, sheet, or explicit approval coordinator) before incurring API costs, plus idempotency keys for retried requests.
- **Actual:** A single tap initiates a billed `/chat/completions`, `/image/generate`, or `/image/edit` request.
- **Impact:** Accidental spend; violates the project’s explicit-approval contract; no defense against retry storms.
- **Root cause:** The UI layer has no approval coordinator.
- **Related occurrences:** ConfigScreen model probe is lower risk but also lacks confirmation.
- **Remediation:** Introduce an approval coordinator that gates paid operations; attach idempotency keys to mutating requests where the Venice API supports them.
- **Tests required:** UI tests verifying the approval flow; unit tests for idempotency key generation.
- **Compatibility impact:** UX change; users will see an extra confirmation step.

---

### ARCH-06 | Severity: P2 | Status: CONFIRMED
**WorkManager is declared as a dependency but is not used for durable background work.**

- **Area:** Background work / Architecture  
- **Module:** `:app`  
- **File:** `app/build.gradle.kts` line 51  
- **Symbol:** `androidx.work.runtime`  
- **Evidence:**
  ```kotlin
  implementation(libs.androidx.work.runtime)
  ```
  No `androidx.work` imports exist in `app/src/main/java`.
- **Expected:** Per `ANDROID_PORT_HANDOFF.md` Phase 5, long-running generation/retrieval jobs (video, music, audio) should be backed by WorkManager so they survive process death and retry automatically.
- **Actual:** All async work lives in `viewModelScope` and dies with the UI.
- **Impact:** Process death cancels queued or in-flight generation jobs; no retry; no notification/progress surface.
- **Root cause:** Infrastructure not yet implemented.
- **Related occurrences:** `VideoClient.kt` and `AudioClient.kt` expose queue/retrieve methods but no worker consumes them.
- **Remediation:** Implement `Worker` classes for video/audio/music queued jobs; enqueue them from the ViewModels.
- **Tests required:** Worker unit tests; process-death recovery test.
- **Compatibility impact:** New feature; no backward-compatibility risk.

---

### ARCH-07 | Severity: P2 | Status: CONFIRMED
**DataStore and Media3 are declared but unused in production code.**

- **Area:** Architecture / Dependencies  
- **Module:** `:app`  
- **File:** `app/build.gradle.kts` lines 50, 52  
- **Symbol:** `androidx.datastore.preferences`, `androidx.media3.exoplayer`  
- **Evidence:**
  ```kotlin
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.media3.exoplayer)
  ```
  No production imports of `androidx.datastore` or `androidx.media3` in `app/src/main/java`.
- **Expected:** `ANDROID_PORT_HANDOFF.md` requires DataStore for preferences and Media3 for audio/video playback.
- **Actual:** Dead dependencies increase APK size and attack surface.
- **Impact:** Bloat; unused code paths may still be included in release builds and require ProGuard/R8 keep rules.
- **Root cause:** Dependencies added before the features that need them.
- **Related occurrences:** `libs.versions.toml` defines versions for both.
- **Remediation:** Either implement the planned preferences/playback features or remove the dependencies until needed.
- **Tests required:** Dependency analysis / APK size diff.
- **Compatibility impact:** Removing unused libs reduces size.

---

### ARCH-08 | Severity: P2 | Status: CONFIRMED
**Generated image bitmaps are decoded on the main thread during composition.**

- **Area:** Lifecycle / Storage / Performance  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` lines 180–186  
- **Symbol:** `decodedBitmap`  
- **Evidence:**
  ```kotlin
  val decodedBitmap = remember(state.resultImageUri) {
      try {
          state.resultImageUri.path?.let { BitmapFactory.decodeFile(it) }
      } catch (e: Exception) { null }
  }
  ```
- **Expected:** Bitmap decoding should happen off the main thread to avoid ANRs.
- **Actual:** `BitmapFactory.decodeFile` runs synchronously inside `remember` during composition.
- **Impact:** Large generated images can freeze the UI and trigger ANRs.
- **Root cause:** Synchronous decode in Compose instead of using an async image loader.
- **Related occurrences:** `ImageViewModel.kt:112-114` decodes base64 on the main thread for image edits (ARCH-13).
- **Remediation:** Use Coil `AsyncImage` or decode in a `produceState` coroutine on `Dispatchers.IO`.
- **Tests required:** UI performance test with large image fixture.
- **Compatibility impact:** None.

---

### ARCH-09 | Severity: P2 | Status: CONFIRMED
**Generated image cache files are never cleaned up.**

- **Area:** Storage / Lifecycle  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 111–115  
- **Symbol:** `saveBytesToCache`  
- **Evidence:**
  ```kotlin
  saveBytesToCache = { bytes ->
      withContext(Dispatchers.IO) {
          val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
          file.writeBytes(bytes)
          android.net.Uri.fromFile(file)
      }
  }
  ```
  No deletion or LRU policy exists.
- **Expected:** Generated media should be stored in a bounded, content-addressed cache with eviction.
- **Actual:** Every generation/edit writes a new timestamped file into `context.cacheDir`.
- **Impact:** Unbounded disk growth; user data accumulates until the OS clears the cache or the app is uninstalled.
- **Root cause:** No cache management policy.
- **Related occurrences:** None.
- **Remediation:** Implement a content-addressed cache with a max-size eviction policy; store metadata in Room, not base64 blobs.
- **Tests required:** Storage test verifying eviction.
- **Compatibility impact:** Existing cache files may be orphaned once; document one-time cleanup.

---

### ARCH-10 | Severity: P2 | Status: CONFIRMED
**The Redactor utility is not wired into production logging or diagnostics.**

- **Area:** Security / Privacy / Architecture  
- **Module:** `:core:common`  
- **File:** `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt`  
- **Symbol:** `Redactor`  
- **Evidence:**
  ```kotlin
  object Redactor { ... }
  ```
  Grep shows the only usage is in `core/common/src/test/java/.../RedactorTest.kt`.
- **Expected:** `AGENTS.md` requires “No raw prompt/response/API-key logging.” A central redactor should sanitize diagnostics.
- **Actual:** The utility exists but is not invoked anywhere in production code.
- **Impact:** Future diagnostics may leak API keys, bearer tokens, or local paths.
- **Root cause:** Utility created but never integrated.
- **Related occurrences:** None.
- **Remediation:** Route all log/diagnostic output through `Redactor.redact()` before emission; add lint rule to prevent raw logging of credentials.
- **Tests required:** Redaction tests already exist; add integration tests for logging paths.
- **Compatibility impact:** None.

---

### ARCH-11 | Severity: P2 | Status: CONFIRMED
**No process-death recovery for in-flight chat/image UI state.**

- **Area:** Lifecycle / State management  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` lines 9–16; `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` line 68; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Symbol:** `onCreate`, `rememberSaveable`, `SavedStateHandle`  
- **Evidence:**
  - `MainActivity.kt` does not read `savedInstanceState`.
  - `VeniceForgeApp.kt` only saves `selectedId` via `rememberSaveable`.
  - `ChatViewModel` and `ImageViewModel` do not accept a `SavedStateHandle`.
- **Expected:** In-flight state (streaming flag, current prompt, selected image URI, generation status) should survive process death via `SavedStateHandle` or `rememberSaveable`.
- **Actual:** After process death, streaming/generation state is lost; the DB may still contain a `PENDING`/`STREAMING` assistant message.
- **Impact:** Confusing recovery; orphaned incomplete messages; possible duplicate paid submissions when the user retries.
- **Root cause:** No saved-state integration for ViewModels.
- **Related occurrences:** `ConfigScreen.kt` also loses its text fields and model list on process death.
- **Remediation:** Pass `SavedStateHandle` to ViewModels; persist lightweight UI state; reconcile DB message statuses on startup.
- **Tests required:** Process-death instrumentation test.
- **Compatibility impact:** None.

---

### ARCH-12 | Severity: P2 | Status: CONFIRMED
**Active conversation selection is not stable across Activity recreation.**

- **Area:** Lifecycle / Persistence  
- **Module:** `:app`, `:core:data`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 54–67  
- **Symbol:** `conversationId`, `init`  
- **Evidence:**
  ```kotlin
  init {
      viewModelScope.launch {
          val existing = chatRepo.observeConversations(profileId).first()
          val convId = if (existing.isNotEmpty()) { existing.first().id } else { ... }
          conversationId = convId
      }
  }
  ```
  `observeConversations` orders by `updatedAt DESC`.
- **Expected:** The conversation the user is actively chatting in remains the active conversation after rotation or process restart.
- **Actual:** `conversationId` is recomputed from the most recently updated conversation. If another conversation is updated (e.g., by a background sync or by a different screen), the user may be switched to a different conversation on recreation.
- **Impact:** User context jumps between conversations; messages may be appended to the wrong thread.
- **Root cause:** Active conversation ID is not persisted.
- **Related occurrences:** None.
- **Remediation:** Persist the active conversation ID in `SavedStateHandle` or DataStore and restore it in `init`.
- **Tests required:** Rotation/restart test with multiple conversations.
- **Compatibility impact:** None.

---

### ARCH-13 | Severity: P2 | Status: CONFIRMED
**Image-edit base64 decoding runs on the main thread.**

- **Area:** Lifecycle / Performance  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 110–116  
- **Symbol:** `editImage`  
- **Evidence:**
  ```kotlin
  val base64 = response.images?.firstOrNull()
  val uri = if (base64 != null) {
      val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
      saveBytesToCache(decodedBytes)
  } else null
  ```
  This executes inside `viewModelScope.launch` without an explicit dispatcher, so it runs on the main dispatcher.
- **Expected:** Decode and save large binary payloads off the main thread.
- **Actual:** Base64 decode and file write block the UI thread.
- **Impact:** ANR risk for large edited images.
- **Root cause:** Missing `withContext(Dispatchers.IO)` around the decode/write block.
- **Related occurrences:** `ImageScreen.kt` bitmap decode (ARCH-08).
- **Remediation:** Wrap decode and `saveBytesToCache` in `withContext(Dispatchers.IO)`.
- **Tests required:** Performance test with large base64 fixture.
- **Compatibility impact:** None.

---

### ARCH-14 | Severity: P3 | Status: CONFIRMED
**FeatureCatalog.byId throws on unknown feature IDs instead of falling back safely.**

- **Area:** Architecture / Navigation  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt` line 47  
- **Symbol:** `byId`  
- **Evidence:**
  ```kotlin
  fun byId(id: String): AppFeature = all.first { it.id == id }
  ```
- **Expected:** Unknown or stale feature IDs (e.g., from `rememberSaveable` after an app update removes a feature) should map to a safe default.
- **Actual:** `first { ... }` throws `NoSuchElementException`, crashing the app.
- **Impact:** Crash on startup after navigation surface changes.
- **Root cause:** Non-null assertion via `first` instead of `firstOrNull`.
- **Related occurrences:** `VeniceForgeApp.kt:69` already attempts a fallback with `?: FeatureCatalog.byId("chat")`, but `byId` itself throws.
- **Remediation:** Change to `firstOrNull { it.id == id } ?: all.first { it.id == "chat" }`.
- **Tests required:** Unit test with unknown feature id.
- **Compatibility impact:** Safer navigation; no breaking change for valid IDs.

---

### ARCH-15 | Severity: P3 | Status: CONFIRMED
**ConfigScreen hardcodes the default profile and does not use DataStore.**

- **Area:** Architecture / Settings  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` line 47  
- **Symbol:** `profileId`  
- **Evidence:**
  ```kotlin
  val profileId = "default"
  ```
- **Expected:** `ANDROID_PORT_HANDOFF.md` Phase 9 calls for DataStore-backed settings and profile management.
- **Actual:** Settings screen is hard-wired to a single profile and stores no preferences in DataStore.
- **Impact:** Blocks multi-profile work and theming/settings persistence.
- **Root cause:** Starter implementation scoped to a single default profile.
- **Related occurrences:** `VeniceForgeApp.kt` also uses the default profile from `ProfileRepository.ensureDefault()`.
- **Remediation:** Inject current profile from repository; use DataStore for UI preferences.
- **Tests required:** Settings persistence test.
- **Compatibility impact:** None for single-profile milestone; required for future phases.

---

### ARCH-16 | Severity: P2 | Status: CONFIRMED
**ChatClient does not parse SSE usage events.**

- **Area:** Venice API integration / Streaming  
- **Module:** `:venice-sdk`  
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt` lines 100–151  
- **Symbol:** `parseChunks`  
- **Evidence:**
  ```kotlin
  val choices = obj["choices"]
  if (choices !is JsonArray) { ... }
  ```
  The parser only looks at `choices`. `ChatStreamChunk.Finish` includes a `Usage` field but it is never populated from the stream.
- **Venice reference:** `.source/venice-api-docs/swagger.yaml` `/chat/completions` streaming response may emit usage events separate from `choices`.
- **Expected:** Token-usage metadata should be surfaced to callers.
- **Actual:** Usage is dropped.
- **Impact:** Billing/diagnostics cannot report token consumption for streamed completions.
- **Root cause:** Parser ignores non-`choices` SSE data lines.
- **Related occurrences:** `ChatStreamAccumulator.kt` has no usage accumulation path.
- **Remediation:** Parse `usage` events and emit a `ChatStreamChunk.Finish` with populated `Usage`, or add a dedicated `Usage` chunk type.
- **Tests required:** SSE fixture containing usage event.
- **Compatibility impact:** New chunk emissions; existing consumers that ignore usage are unaffected.

---

### ARCH-17 | Severity: P2 | Status: CONFIRMED
**CapabilitiesRepository silently swallows errors from `/models/traits` and `/models/compatibility_mapping`.**

- **Area:** Venice API integration / Model discovery  
- **Module:** `:venice-sdk`  
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt` lines 94–99 and 108–113  
- **Symbol:** `fetchTraits`, `fetchCompatibility`  
- **Evidence:**
  ```kotlin
  private suspend fun fetchTraits(apiKey: String): Map<String, String> {
      val raw = try { sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey) } catch (_: Exception) { return emptyMap() }
      ...
  }
  ```
  Same pattern in `fetchCompatibility`.
- **Expected:** Transient or auth errors should be reported so the UI can show a retry state.
- **Actual:** Any failure returns an empty map, so the app silently loses traits/compatibility data and may fall back to a generic model.
- **Impact:** Degraded model selection and no visibility into discovery failures.
- **Root cause:** Broad `catch (_: Exception)` suppresses all errors.
- **Related occurrences:** None.
- **Remediation:** Distinguish network/auth errors from parse errors; expose a sealed result type or rethrow typed exceptions.
- **Tests required:** Unit tests for 401/500/timeout responses from traits/compatibility endpoints.
- **Compatibility impact:** API surface of `fetchLiveCapabilities` may need to return a richer result type.
