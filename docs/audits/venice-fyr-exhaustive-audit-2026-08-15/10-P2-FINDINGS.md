# P2 Findings — Venice Fyr Exhaustive Audit Consolidation

**Severity:** P2
**Total findings after deduplication:** 97

| Status | Count |
|--------|-------|
| CONFIRMED | 92 |
| INFERRED | 5 |

## APP-UI-003 | `app/build.

**Severity:** P2
**Status:** CONFIRMED
**Area:** State collection lifecycle
**Module:** :app
**File:** ChatScreen.kt` / `ImageScreen.kt
**Lines:** ChatScreen.kt:39`, `ImageScreen.kt:47
**Symbol:** collectAsState

**Area:** State collection lifecycle | **Module:** `:app` | **File:** `ChatScreen.kt` / `ImageScreen.kt` | **Lines:** `ChatScreen.kt:39`, `ImageScreen.kt:47` | **Symbol:** `collectAsState`

**Evidence:**
```kotlin
// ChatScreen.kt:39
val state by viewModel.state.collectAsState()

// ImageScreen.kt:47
val state by viewModel.uiState.collectAsState()
```
`app/build.gradle.kts:48` declares `implementation(libs.androidx.lifecycle.runtime.compose)`, which provides `collectAsStateWithLifecycle`.

**Expected:** Use `collectAsStateWithLifecycle()` so collection pauses when the composable is not at least `Lifecycle.State.STARTED`, reducing background work.

**Actual:** `collectAsState()` keeps collecting while the composition exists, even when the app is in the background or the screen is not active. This wastes battery and can update UI state that is not visible.

**Impact:** Background collection of chat/image state flows; unnecessary CPU/battery use; potential for stale UI updates when returning to foreground.

**Root cause:** Developers used the generic Compose `collectAsState` instead of the lifecycle-aware variant available in the project.

**Related occurrences:** None other in scope; `VeniceForgeApp.kt` uses `produceState` and direct `remember`, not flow collection.

**Venice reference:** N/A.

**Android/Kotlin reference:** `collectAsStateWithLifecycle` is the recommended API for collecting flows in Compose UIs tied to Android lifecycle (Android developer guide "Collect flows in a lifecycle-aware manner").

**Remediation:** Replace `collectAsState()` with `collectAsStateWithLifecycle()` in both screens.

**Tests required:** Add a test verifying collection pauses/resumes with lifecycle state transitions.

**Compatibility impact:** No behavioral breaking change; only positive lifecycle correctness.

---

---

## APP-UI-004 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** State persistence
**Module:** :app
**File:** ConfigScreen.kt
**Lines:** 49–53
**Symbol:** apiKey`, `status`, `loading`, `hasError`, `models

**Area:** State persistence | **Module:** `:app` | **File:** `ConfigScreen.kt` | **Lines:** 49–53 | **Symbol:** `apiKey`, `status`, `loading`, `hasError`, `models`

**Evidence:**
```kotlin
var apiKey by remember { mutableStateOf("") }
var status by remember { mutableStateOf("No API key loaded") }
var loading by remember { mutableStateOf(false) }
var hasError by remember { mutableStateOf(false) }
var models by remember { mutableStateOf<List<VeniceModel>>(emptyList()) }
```

**Expected:** User-entered/transient UI state should survive configuration changes via `rememberSaveable` or be re-derived from a ViewModel.

**Actual:** All five values are held in `remember`, so they are reset on rotation. The `LaunchedEffect(Unit)` reloads the API key from secure storage, but `status`, `loading`, `hasError`, and the fetched `models` list are lost. After rotation the user sees "No API key loaded" again and must re-tap "Load models".

**Impact:** Poor UX on rotation; lost model-discovery results and status messages.

**Root cause:** Use of `remember` for state that should survive config changes.

**Related occurrences:** `ChatScreen.kt:40–41` (input/menu state), `ImageScreen.kt:76` (dropdown expanded), `VeniceForgeApp.kt:83` (`profileId`), `VeniceForgeApp.kt:122` (`modelCatalog`).

**Venice reference:** N/A.

**Android/Kotlin reference:** `rememberSaveable` is the Compose API for surviving process death and configuration changes.

**Remediation:** Convert `apiKey`, `status`, `loading`, `hasError`, and `models` to `rememberSaveable` where serializable, or move them into a `ConfigViewModel` obtained via `viewModel()`.

**Tests required:** Instrumentation rotation test verifying model list and status survive.

**Compatibility impact:** State survives rotation; no negative compatibility impact.

---

---

## APP-UI-005 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** State persistence
**Module:** :app
**File:** ChatScreen.kt
**Lines:** 40–41
**Symbol:** input`, `modelMenuOpen

**Area:** State persistence | **Module:** `:app` | **File:** `ChatScreen.kt` | **Lines:** 40–41 | **Symbol:** `input`, `modelMenuOpen`

**Evidence:**
```kotlin
var input by remember { mutableStateOf("") }
var modelMenuOpen by remember { mutableStateOf(false) }
```

**Expected:** Composer text and dropdown open state should survive configuration changes.

**Actual:** Both are `remember`, so a partially typed message and an open model menu are lost on rotation.

**Impact:** User loses in-progress message text on rotation.

**Root cause:** Use of `remember` instead of `rememberSaveable` for simple serializable UI state.

**Related occurrences:** `ImageScreen.kt:76` (`expanded`), `ConfigScreen.kt:49–53`.

**Venice reference:** N/A.

**Android/Kotlin reference:** `rememberSaveable` persists primitive/String state across config changes.

**Remediation:** Change `input` and `modelMenuOpen` to `rememberSaveable`.

**Tests required:** Rotation test verifying typed text and menu state survive.

**Compatibility impact:** None.

---

---

## APP-UI-006 | `VeniceForgeTheme.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Theme / UX
**Module:** :app
**File:** MainActivity.kt
**Lines:** 12
**Symbol:** VeniceForgeTheme(darkTheme = true)

**Area:** Theme / UX | **Module:** `:app` | **File:** `MainActivity.kt` | **Line:** 12 | **Symbol:** `VeniceForgeTheme(darkTheme = true)`

**Evidence:**
```kotlin
setContent {
    VeniceForgeTheme(darkTheme = true) {
        VeniceForgeApp()
    }
}
```
`VeniceForgeTheme.kt:12` accepts `darkTheme: Boolean = isSystemInDarkTheme()`.

**Expected:** Respect the system dark/light mode setting by default.

**Actual:** `darkTheme = true` is hard-coded, forcing dark mode regardless of system setting. The light color scheme in `VeniceColors.kt:69` is effectively unreachable.

**Impact:** Users who prefer light mode or have battery-saver/auto settings are ignored; accessibility/contrast expectations may be violated.

**Root cause:** Explicit `darkTheme = true` in `MainActivity` overrides theme default.

**Related occurrences:** `VeniceForgeApp.kt:149` uses `isSystemInDarkTheme()` only to choose logo variant, but the app is always dark.

**Venice reference:** N/A.

**Android/Kotlin reference:** `isSystemInDarkTheme()` is the standard Compose API for following system UI mode.

**Remediation:** Remove `darkTheme = true` and let `VeniceForgeTheme` default to `isSystemInDarkTheme()`. If a manual toggle is desired, persist the user choice and pass it here.

**Tests required:** UI test verifying light/dark system setting is reflected.

**Compatibility impact:** Visual change for users on light-mode devices; should be intentional.

---

---

## APP-UI-007 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Performance / ANR risk
**Module:** :app
**File:** ImageScreen.kt
**Lines:** 180–186
**Symbol:** decodedBitmap
**Also reported as:** ARCH-08

**Area:** Performance / ANR risk | **Module:** `:app` | **File:** `ImageScreen.kt` | **Lines:** 180–186 | **Symbol:** `decodedBitmap`

**Evidence:**
```kotlin
val decodedBitmap = remember(state.resultImageUri) {
    try {
        state.resultImageUri.path?.let { BitmapFactory.decodeFile(it) }
    } catch (e: Exception) {
        null
    }
}
```

**Expected:** Bitmap decoding should happen on a background dispatcher (e.g., `Dispatchers.IO`) or via a Coil/AsyncImage-like library.

**Actual:** `BitmapFactory.decodeFile` runs synchronously inside `remember` on the main thread. Large generated images can cause frame drops or ANRs.

**Impact:** Jank or ANR when displaying generated/edited images.

**Root cause:** Synchronous bitmap I/O on the main thread.

**Related occurrences:** `CodexPet.kt:85` decodes a small spritesheet on the main thread; acceptable for that asset size but same pattern.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android strict-mode and performance guidelines require bitmap decoding off the main thread.

**Remediation:** Use Coil (`AsyncImage`) or wrap decoding in `produceState`/`LaunchedEffect` with `Dispatchers.IO` and show a placeholder while loading.

**Tests required:** Performance test with a large image; strict-mode test detecting disk-read on main thread.

**Compatibility impact:** None.

---

---

## APP-UI-008 | `CodexPet.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Resource leak
**Module:** :core:designsystem
**File:** CodexPet.kt
**Lines:** 81–87, 103–110
**Symbol:** imageBitmap`, `DisposableEffect` import

**Area:** Resource leak | **Module:** `:core:designsystem` | **File:** `CodexPet.kt` | **Lines:** 81–87, 103–110 | **Symbol:** `imageBitmap`, `DisposableEffect` import

**Evidence:**
```kotlin
val imageBitmap: ImageBitmap? = remember(spritesheetRes) {
    ...
    androidBitmap?.asImageBitmap()
}
```
`CodexPet.kt:9` imports `DisposableEffect` but it is never used. `ImageBitmap` created from an Android `Bitmap` holds native resources and should be explicitly disposed when no longer needed.

**Expected:** Wrap the `ImageBitmap` in `DisposableEffect(spritesheetRes)` and call `imageBitmap.asAndroidBitmap().recycle()` or `imageBitmap.asAndroidBitmap().prepareToDraw()` cleanup on disposal.

**Actual:** The `ImageBitmap` is created once per `spritesheetRes` and never disposed. While the spritesheet is small, this violates Compose graphics resource hygiene and can leak native bitmap memory if the composable is created/destroyed repeatedly (e.g., many status indicators).

**Impact:** Native bitmap memory leak; potential `OutOfMemoryError` if many CodexPet instances are created.

**Root cause:** Missing `DisposableEffect` disposal for the converted `ImageBitmap`.

**Related occurrences:** None in scope.

**Venice reference:** N/A.

**Android/Kotlin reference:** `ImageBitmap.asAndroidBitmap()` returns a `Bitmap` that must be recycled when the `ImageBitmap` is no longer needed; Compose does not automatically recycle it.

**Remediation:** Replace the unused `DisposableEffect` import with an actual `DisposableEffect(spritesheetRes) { ... onDispose { imageBitmap?.asAndroidBitmap()?.recycle() } }` block, or use `remember(spritesheetRes) { ... }` and dispose in `onDispose`.

**Tests required:** LeakCanary/memory test creating and destroying many `CodexPetRenderer` instances.

**Compatibility impact:** None.

---

---

## APP-UI-009 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Misleading UI / State
**Module:** :app
**File:** VeniceForgeApp.kt
**Lines:** 83–84, 209–219, 229–232
**Symbol:** profileId`, `chatViewModel`/`imageViewModel` null branch

**Area:** Misleading UI / State | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Lines:** 83–84, 209–219, 229–232 | **Symbol:** `profileId`, `chatViewModel`/`imageViewModel` null branch

**Evidence:**
```kotlin
var profileId by remember { mutableStateOf<String?>(null) }
LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }
...
if (vm != null) { ChatScreen(...) } else {
    Column(...) { Text(stringResource(R.string.chat_no_api_key)) }
}
```

**Expected:** While the profile/API key is being loaded, show a loading indicator or "Loading…" message.

**Actual:** `profileId` starts as `null`, so `chatViewModel` and `imageViewModel` are null and the UI shows `R.string.chat_no_api_key` ("No API key saved. Go to Settings…") even if a key exists and is merely being loaded. This is misleading and can send the user to Settings unnecessarily.

**Impact:** Users incorrectly believe their API key is missing during app startup.

**Root cause:** No loading state distinguishes "key loading" from "key missing".

**Related occurrences:** Same string reused for image screen at `VeniceForgeApp.kt:230`.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Add a `isLoadingProfile` boolean and display a loading indicator until `profileId` and key availability are resolved.

**Tests required:** UI test verifying loading state then content state; test with saved key shows no "No API key saved" flash.

**Compatibility impact:** None.

---

---

## APP-UI-010 | `FeatureCatalog.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Feature advertising / UX
**Module:** :app
**File:** FeatureCatalog.kt` / `VeniceForgeApp.kt
**Lines:** FeatureCatalog.kt:21–45`, `VeniceForgeApp.kt:234–265
**Symbol:** FeatureCatalog.all`, `FeatureScreen

**Area:** Feature advertising / UX | **Module:** `:app` | **File:** `FeatureCatalog.kt` / `VeniceForgeApp.kt` | **Lines:** `FeatureCatalog.kt:21–45`, `VeniceForgeApp.kt:234–265` | **Symbol:** `FeatureCatalog.all`, `FeatureScreen`

**Evidence:**
`FeatureCatalog.kt` defines 22 features (Character Chats, History, Media Studio, Audio Studio, Music Studio, Video Studio, etc.), almost all marked `SCAFFOLDED` or `FOUNDATION`. `VeniceForgeApp.kt:234–265` routes every unknown `selectedId` to `FeatureScreen`, which only shows label, status, desktop purpose, and port notes.

**Expected:** Navigation surfaces should accurately reflect what is implemented, or clearly mark placeholders.

**Actual:** The navigation drawer advertises a full desktop parity surface, but tapping any non-chat/image feature shows a static placeholder page. The comment at `VeniceForgeApp.kt:260–263` confirms this is intentional scaffolding, but from the user's perspective the drawer items look actionable.

**Impact:** User confusion; perceived broken app; potential violation of accurate feature representation.

**Root cause:** Complete navigation surface exposed before screens are implemented.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Either (a) hide/disable unimplemented features in release builds, (b) add a "Coming soon" badge/overlay, or (c) keep them in a debug-only catalog. At minimum, disable the `NavigationDrawerItem` for `SCAFFOLDED` features.

**Tests required:** UI test verifying that unimplemented features show a clear placeholder or are disabled.

**Compatibility impact:** Navigation surface reduction if features are hidden; no API compatibility impact.

---

---

## ARCH-03 | DataServices and AppDatabase are not application-singletons.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Architecture / DI
**Module:** :core:data`, `:app
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` lines 18–20; `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` lines 36–43; `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` line 77
**Lines:** 
**Symbol:** DataServices.create`, `AppDatabase.create

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

---

## ARCH-06 | WorkManager is declared as a dependency but is not used for durable background work.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Background work / Architecture
**Module:** :app
**File:** app/build.gradle.kts` line 51
**Lines:** 
**Symbol:** androidx.work.runtime

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

---

## ARCH-07 | DataStore and Media3 are declared but unused in production code.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Architecture / Dependencies
**Module:** :app
**File:** app/build.gradle.kts` lines 50, 52
**Lines:** 
**Symbol:** androidx.datastore.preferences`, `androidx.media3.exoplayer

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

---

## ARCH-09 | Generated image cache files are never cleaned up.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Storage / Lifecycle
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt` lines 111–115
**Lines:** 
**Symbol:** saveBytesToCache

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

---

## ARCH-11 | No process-death recovery for in-flight chat/image UI state.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Lifecycle / State management
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` lines 9–16; `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` line 68; `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`; `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 
**Symbol:** onCreate`, `rememberSaveable`, `SavedStateHandle

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

---

## ARCH-12 | Active conversation selection is not stable across Activity recreation.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Lifecycle / Persistence
**Module:** :app`, `:core:data
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt` lines 54–67
**Lines:** 
**Symbol:** conversationId`, `init

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

---

## AUD-03 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Speech request model incomplete
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt
**Lines:** 7–14
**Symbol:** SpeechRequest

**Area:** Speech request model incomplete  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt`  
**Lines:** 7–14  
**Symbol:** `SpeechRequest`

**Evidence:**
```kotlin
@Serializable
data class SpeechRequest(
    val model: String,
    val input: String,
    val voice: String? = null,
    @SerialName("response_format") val responseFormat: String? = null,
    val speed: Float? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null
)
```

**Spec:** `swagger.yaml` `CreateSpeechRequestSchema` (lines 3355–3467) includes `input`, `language`, `model`, `prompt`, `response_format`, `speed`, `streaming`, `temperature`, `top_p`, `voice`.

**Expected:** SDK model supports `language`, `prompt`, `streaming`, `temperature`, `top_p`.

**Actual:** These fields are missing. Callers cannot request streaming TTS, style prompts, language hints, or sampling controls.

**Impact:** Incorrect Venice integration. Callers cannot use documented TTS features; `safe_mode` is also present in the SDK model but is **not** in the swagger schema for `/audio/speech`.

**Root cause:** Model was written from an incomplete subset of the schema.

**Related occurrences:** `AudioModels.kt:7–14` only.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 3355–3467 (`CreateSpeechRequestSchema`).

**Android/Kotlin reference:** N/A.

**Remediation:** Add missing fields; remove `safe_mode` from `SpeechRequest` unless it is confirmed to be accepted by the server (it is not in swagger).

**Tests required:** Serialization round-trip for all speech fields.

**Compatibility impact:** Removing `safe_mode` is source-incompatible; adding optional fields is additive.

---

---

## AUD-04 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Speech response content type not exposed
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt
**Lines:** 19–45
**Symbol:** speech

**Area:** Speech response content type not exposed  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 19–45  
**Symbol:** `speech`

**Evidence:**
```kotlin
suspend fun speech(apiKey: String, request: SpeechRequest): ByteArray = withContext(Dispatchers.IO) {
    ...
    res.body?.bytes() ?: throw VeniceSdkException.Protocol("Empty binary response from /audio/speech", null)
}
```

**Spec:** `swagger.yaml` `/audio/speech` 200 response (lines 10935–10959) can return `audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, or `audio/wav`.

**Expected:** Caller knows which format was returned (via `Content-Type` header or a wrapper type).

**Actual:** Only `ByteArray` is returned; the `Content-Type` is discarded.

**Impact:** UX/reliability problem. The caller must guess or hard-code the format, which may not match the actual bytes.

**Root cause:** Binary response API returns raw bytes without metadata.

**Related occurrences:** `ImageClient.executeBinaryRequest` has the same limitation (not in scope).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 10935–10959.

**Android/Kotlin reference:** N/A.

**Remediation:** Return a wrapper object containing `bytes` and `contentType`, or add a separate method.

**Tests required:** Mock speech responses with each audio MIME type and assert returned metadata.

**Compatibility impact:** Return-type change; source-incompatible.

---

---

## AUD-05 | --- --- * `docs/reference/VENICE_API_SOURCE_MANIFEST.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Audio speech Accept header too broad
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt
**Lines:** N/A
**Symbol:** Accept

**Area:** Audio speech Accept header too broad  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Line:** 26  
**Symbol:** `Accept`

**Evidence:**
```kotlin
.header("Accept", "audio/*")
```

**Spec:** `swagger.yaml` `/audio/speech` lists specific audio MIME types (`audio/aac`, `audio/flac`, `audio/mpeg`, `audio/opus`, `audio/pcm`, `audio/wav`).

**Expected:** SDK sends an `Accept` header that reflects the requested `response_format` or the model's supported formats.

**Actual:** Wildcard `audio/*` is sent.

**Impact:** Reliability/UX problem. Server may return a default format the caller did not request; caller cannot distinguish format.

**Root cause:** Simplified header.

**Related occurrences:** `AudioClient.kt:26` only.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 10935–10959.

**Android/Kotlin reference:** N/A.

**Remediation:** Set `Accept` based on `request.responseFormat` (mapped to MIME type), or omit and rely on `response_format` body field.

**Tests required:** Verify `Accept` header in mocked request.

**Compatibility impact:** None.

---

### X-01 | P2 | CONFIRMED
**Area:** Missing unit tests for video and audio clients  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/...`  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:** `venice-sdk/src/test` contains `ImageClientTest.kt`, `ChatClientTest.kt`, etc., but no `VideoClientTest.kt` or `AudioClientTest.kt`.

**Expected:** Each public SDK client has unit tests covering success paths, error paths, and serialization.

**Actual:** No tests exist for video or audio clients.

**Impact:** Architecture/test problem. The gaps documented in this audit are not caught by CI.

**Root cause:** Tests were not written.

**Related occurrences:** Entire `sdk/video/` and `sdk/audio/` packages.

**Venice reference:** N/A.

**Android/Kotlin reference:** JUnit4 / `kotlinx.coroutines.test.runTest`.

**Remediation:** Add `VideoClientTest.kt` and `AudioClientTest.kt` with `MockWebServer` or interceptor-based mocks.

**Tests required:** N/A (this finding is about missing tests).

**Compatibility impact:** None.

---

### X-02 | P3 | CONFIRMED
**Area:** Documentation drift between manifest and changelog  
**Module:** docs  
**File:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md`, `CHANGELOG.md`  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:**
* `docs/reference/VENICE_API_SOURCE_MANIFEST.md` lines 37–38 list Video and Audio & Music as "Planned" with status "Pending Milestone 3".
* `CHANGELOG.md` line 19 claims: ":venice-sdk Audio client (`AudioClient`) with `/audio/speech` direct binary stream support" and line 20 claims ":venice-sdk Video client (`VideoClient`) with `/video/queue`, `/video/retrieve`, `/video/complete`, and dynamic Content-Type stream/status discriminator."

**Expected:** Source manifest reflects implemented clients and remaining gaps.

**Actual:** Manifest contradicts changelog.

**Impact:** Docs/minor problem. Misleading project status; other agents may plan around outdated information.

**Root cause:** Manifest not updated after implementation.

**Related occurrences:** N/A.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Update `VENICE_API_SOURCE_MANIFEST.md` to show implemented endpoints and missing endpoints; update parity matrix.

**Tests required:** N/A.

**Compatibility impact:** None.

---

## Cross-Cutting Observations

1. **No x402 / SIWX auth path.** All clients send `Authorization: Bearer $apiKey`. The swagger endpoints also accept `siwx` security, but the SDK does not expose an x402 credential provider. This is consistent with the rest of the SDK (chat, image) and is noted here for completeness, not as a unique defect.
2. **Duplicate `Json` instances.** `VideoClient` and `AudioClient` each create a private `Json { ignoreUnknownKeys = true; encodeDefaults = false }`. This matches `ImageClient` and is stylistically consistent, but a shared SDK serializer would reduce duplication.
3. **No client-side media-size validation.** The SDK does not validate prompt length, file size, or reference URL count before sending. Server-side validation handles this, but earlier failure would improve UX.
4. **AGENTS.md safe_mode rule.** The project rule says "Preserve explicit `safe_mode=false` when selected." `QueueVideoRequest.safeMode` is nullable, so explicit `false` is preserved in serialization (`encodeDefaults = false` means the field is omitted when `null`, but `false` is encoded). This is correct for video. However, `SpeechRequest.safeMode` should not exist per swagger.

---

## Severity Summary

| Severity | Count | Finding IDs |
|----------|-------|-------------|
| P0 | 0 | — |
| P1 | 6 | VID-01, VID-02, VID-03, VID-04, AUD-01, AUD-02 |
| P2 | 7 | VID-05, VID-06, VID-07, AUD-03, AUD-04, AUD-05, X-01 |
| P3 | 1 | X-02 |

**Total findings:** 14.

---

## BUILD-04 | Declared but unused dependencies bloat `:app` and catalog

**Severity:** P2
**Status:** CONFIRMED
**Area:** Dependency hygiene
**Module:** :app` / version catalog
**File:** app/build.gradle.kts`, `gradle/libs.versions.toml
**Lines:** app/build.gradle.kts:50-53`; `gradle/libs.versions.toml:27-29
**Symbol:** androidx.datastore.preferences`, `androidx.work.runtime`, `androidx.media3.exoplayer`, `androidx.lifecycle.runtime.compose`, `androidx.test.ext:junit`, `androidx.sqlite

| Field | Value |
|-------|-------|
| **Area** | Dependency hygiene |
| **Module** | `:app` / version catalog |
| **File** | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| **Lines** | `app/build.gradle.kts:50-53`; `gradle/libs.versions.toml:27-29` |
| **Symbol** | `androidx.datastore.preferences`, `androidx.work.runtime`, `androidx.media3.exoplayer`, `androidx.lifecycle.runtime.compose`, `androidx.test.ext:junit`, `androidx.sqlite` |
| **Evidence** | Declared in `:app`:<br>```kotlin
implementation(libs.androidx.datastore.preferences)
implementation(libs.androidx.work.runtime)
implementation(libs.androidx.media3.exoplayer)
```<br>Grep for actual imports/usage in `app/src` yields no `DataStore`, `WorkManager`, `ExoPlayer`, or `collectAsStateWithLifecycle` usage. `androidx.test.ext:junit` is declared but there are no `androidTest` sources. `androidx.sqlite` is declared in `:app` test but no direct import found; Room already pulls the needed sqlite artifact transitively via `:core:data`. |
| **Expected** | Only dependencies actually used by the module should be declared. |
| **Actual** | Six dependencies are declared without corresponding source usage in `:app`. |
| **Impact** | Larger APK/AAR, longer build times, wider dependency attack surface, misleading signal about implemented features. |
| **Root cause** | Dependencies added prospectively for planned features but not yet used. |
| **Related occurrences** | `okhttp-logging` and `media3-ui-compose` are also cataloged but unused anywhere (BUILD-06). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | Gradle dependency declarations should match source imports. |
| **Remediation** | Remove unused `implementation`/`testImplementation` lines from `:app`. Keep catalog entries only if a module uses them. |
| **Tests required** | `./gradlew :app:assembleDebug` and `./gradlew :app:test` must still pass after removal. |
| **Compatibility impact** | Removing unused deps is safe; may reduce transitive count. |

---

---

## BUILD-06 | No dependency verification or lock files

**Severity:** P2
**Status:** CONFIRMED
**Area:** Supply chain / reproducibility
**Module:** Root
**File:** N/A
**Lines:** N/A
**Symbol:** Dependency verification / locking

| Field | Value |
|-------|-------|
| **Area** | Supply chain / reproducibility |
| **Module** | Root |
| **File** | N/A |
| **Lines** | N/A |
| **Symbol** | Dependency verification / locking |
| **Evidence** | `find . -maxdepth 3` for `verification-metadata.xml`, `*.lockfile`, or `gradle.lockfile` returns nothing. `settings.gradle.kts` uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS` but no content filtering or checksum verification. |
| **Expected** | Production projects should lock dependency versions and/or verify artifact checksums to detect supply-chain tampering and ensure reproducible builds. |
| **Actual** | No lock files or verification metadata. Build depends on mutable remote repository state. |
| **Impact** | A compromised Maven Central/Google artifact or a transient dependency upgrade can silently change build outputs. Reproducibility is not guaranteed across machines/time. |
| **Root cause** | Dependency verification/locking not configured. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html); [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html). |
| **Remediation** | Generate `gradle/verification-metadata.xml` or enable per-configuration dependency locking and commit lockfiles. |
| **Tests required** | Verify CI build succeeds with verification metadata; verify a tampered artifact fails the build. |
| **Compatibility impact** | None; improves supply-chain integrity. |

---

---

## BUILD-08 | `testInstrumentationRunner` configured but no instrumentation tests exist

**Severity:** P2
**Status:** CONFIRMED
**Area:** Test configuration hygiene
**Module:** :core:data
**File:** core/data/build.gradle.kts
**Lines:** 13
**Symbol:** testInstrumentationRunner

| Field | Value |
|-------|-------|
| **Area** | Test configuration hygiene |
| **Module** | `:core:data` |
| **File** | `core/data/build.gradle.kts` |
| **Lines** | 13 |
| **Symbol** | `testInstrumentationRunner` |
| **Evidence** | `core/data/build.gradle.kts:13`:<br>```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```<br>`find . -type d -name androidTest` returns no directories. |
| **Expected** | `testInstrumentationRunner` is only meaningful for `androidTest` sources. If none exist, the property is misleading noise. |
| **Actual** | Runner declared without corresponding `androidTest` sources. |
| **Impact** | Minor confusion; no functional impact unless someone assumes instrumentation tests exist. |
| **Root cause** | Copy-paste from template. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | `testInstrumentationRunner` applies to `androidTest` variant only. |
| **Remediation** | Remove `testInstrumentationRunner` until `androidTest` sources are added. |
| **Tests required** | N/A |
| **Compatibility impact** | None. |

---

---

## BUILD-09 | No explicit Kotlin JVM target / toolchain

**Severity:** P2
**Status:** INFERRED
**Area:** Kotlin compilation target
**Module:** All modules
**File:** */build.gradle.kts
**Lines:** compileOptions` blocks
**Symbol:** sourceCompatibility`/`targetCompatibility

| Field | Value |
|-------|-------|
| **Area** | Kotlin compilation target |
| **Module** | All modules |
| **File** | `*/build.gradle.kts` |
| **Lines** | `compileOptions` blocks |
| **Symbol** | `sourceCompatibility`/`targetCompatibility` |
| **Evidence** | Every module sets Java 17 compatibility via `compileOptions`, but none set `kotlinOptions.jvmTarget` or `jvmToolchain(17)`. |
| **Expected** | Kotlin compilation target should be explicitly aligned with Java target, especially when using future toolchain versions. |
| **Actual** | Relies on AGP/Kotlin plugin inferring the target from `compileOptions`. |
| **Impact** | Inferred target is usually correct, but with AGP 9 / Kotlin 2.3.x edge cases, explicit configuration prevents mismatches. |
| **Root cause** | Minimal build configuration. |
| **Related occurrences** | All six module build files. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Kotlin Gradle plugin JVM target](https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support). |
| **Remediation** | Add `kotlin { jvmToolchain(17) }` or `kotlinOptions.jvmTarget = "17"` to each module. |
| **Tests required** | `./gradlew test` after change. |
| **Compatibility impact** | None if target stays 17. |

---

---

## BUILD-10 | `okhttp logging interceptor` cataloged but unused

**Severity:** P2
**Status:** CONFIRMED
**Area:** Dependency hygiene
**Module:** Version catalog / `:venice-sdk
**File:** gradle/libs.versions.toml
**Lines:** 38
**Symbol:** okhttp-logging

| Field | Value |
|-------|-------|
| **Area** | Dependency hygiene |
| **Module** | Version catalog / `:venice-sdk` |
| **File** | `gradle/libs.versions.toml` |
| **Lines** | 38 |
| **Symbol** | `okhttp-logging` |
| **Evidence** | `gradle/libs.versions.toml:38`:<br>```toml
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor" }
```<br>Grep for `HttpLoggingInterceptor` or `logging-interceptor` across `*.kt` returns no matches. |
| **Expected** | Catalog entries should correspond to actual usage. |
| **Actual** | Declared but never referenced in any module. |
| **Impact** | Misleading catalog; slightly larger resolved graph if ever pulled transitively. |
| **Root cause** | Added prospectively for debug logging but not wired. |
| **Related occurrences** | `media3-ui-compose` is similarly cataloged but unused. |
| **Venice reference** | AGENTS.md prohibits raw API-key/prompt/response logging; any future logging interceptor must be debug-only and redacting. |
| **Android/Kotlin reference** | N/A |
| **Remediation** | Remove `okhttp-logging` from catalog until needed; if added later, restrict to `debugImplementation` and redact secrets. |
| **Tests required** | N/A |
| **Compatibility impact** | None. |

---

---

## BUILD-11 | `media3 ui compose` cataloged but unused

**Severity:** P2
**Status:** CONFIRMED
**Area:** Dependency hygiene
**Module:** Version catalog
**File:** gradle/libs.versions.toml
**Lines:** 29
**Symbol:** androidx-media3-ui-compose

| Field | Value |
|-------|-------|
| **Area** | Dependency hygiene |
| **Module** | Version catalog |
| **File** | `gradle/libs.versions.toml` |
| **Lines** | 29 |
| **Symbol** | `androidx-media3-ui-compose` |
| **Evidence** | `gradle/libs.versions.toml:29`:<br>```toml
androidx-media3-ui-compose = { module = "androidx.media3:media3-ui-compose", version.ref = "media3" }
```<br>No module references this library. |
| **Expected** | Catalog entries should be used by at least one module. |
| **Actual** | Declared but never consumed. |
| **Impact** | Catalog bloat; risk of future version drift. |
| **Root cause** | Added prospectively for media playback UI. |
| **Related occurrences** | `okhttp-logging` (BUILD-10). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | N/A |
| **Remediation** | Remove `androidx-media3-ui-compose` from catalog. Re-add when a media player screen is implemented. |
| **Tests required** | N/A |
| **Compatibility impact** | None. |

---

---

## CHAT-08 | Streaming parser ignores most response metadata: `id`, `created`, `model`, `object`, `usage`, `cost`, multiple choices, reasoning, logprobs

**Severity:** P2
**Status:** CONFIRMED
**Area:** Streaming response parsing
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 100-151
**Symbol:** parseChunks

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming response parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 100-151
- **Symbol:** `parseChunks`

**Evidence:**

`parseChunks` only reads `choices`, `choices[0].index`, `choices[0].delta.content`, `choices[0].delta.tool_calls`, and `choices[0].finish_reason`. It never inspects `id`, `created`, `model`, `object`, `usage`, `cost`, `logprobs`, `choices[0].delta.role`, `choices[0].delta.reasoning_content`, or additional choices.

**Spec:** swagger.yaml non-streaming response (lines 6256-6780) documents all of these fields. The reasoning-models guide (lines 54-100) documents `reasoning_content` in streaming deltas. The function-calling guide documents `tool_calls`.

**Expected:** The SDK surfaces metadata and optional fields that callers need for logging, billing, token accounting, reasoning display, and multi-choice generation.

**Actual:** These fields are silently dropped.

**Impact:** Incomplete observability; callers cannot show model name, token usage, cost, or reasoning content from streams.

**Root cause:** Minimal chunk model (`ChatStreamChunk`) and parser.

**Related occurrences:** `ChatStreamChunk.kt:5-23` defines `Usage` but it is never populated; `ChatClient.kt:77-78` synthesizes a `Finish` without usage.

**Venice reference:** swagger.yaml:6256-6780; `guides/features/reasoning-models.mdx`:54-100.

**Android/Kotlin reference:** N/A.

**Remediation:** Extend `ChatStreamChunk` with metadata fields; parse them from each SSE payload; emit usage when present.

**Tests required:** Fixtures containing `id`, `created`, `model`, `usage`, `cost`, `reasoning_content`, and multiple choices.

**Compatibility impact:** Additive to `ChatStreamChunk` sealed subtypes.

---

---

## CHAT-09 | SSE parser does not accumulate multi line `data:` fields and ignores `event:`/`id:`/`retry:` fields

**Severity:** P2
**Status:** CONFIRMED
**Area:** SSE wire parsing
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt
**Lines:** 6-13
**Symbol:** SseLineParser.nextData

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** SSE wire parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt`
- **Lines:** 6-13
- **Symbol:** `SseLineParser.nextData`

**Evidence:**

```kotlin
fun nextData(): String? {
    val line = reader.readLine() ?: return null
    if (line.isEmpty()) return nextData()
    if (line.startsWith(":")) return nextData()
    if (line.startsWith("data:")) return line.removePrefix("data:").trim()
    return nextData()
}
```

**Spec:** The HTML Standard for Server-Sent Events requires that consecutive `data:` lines be concatenated with `\n` to form one event's data; `event:`, `id:`, and `retry:` fields are also part of the event semantics.

**Expected:** Multi-line data values are reconstructed; event metadata is available if needed.

**Actual:** Only the first `data:` line is returned; other fields are discarded. If Venice ever emits a multi-line data event, parsing will break.

**Impact:** Fragile SSE handling; potential JSON parse failures on multi-line payloads.

**Root cause:** Line-oriented shortcut instead of full SSE event accumulation.

**Related occurrences:** `SseLineParserTest.kt:11-24` only tests single-line data.

**Venice reference:** N/A (SSE is a web standard; swagger does not define SSE framing).

**Android/Kotlin reference:** HTML Living Standard § Server-Sent Events.

**Remediation:** Implement an event accumulator that buffers lines until a blank line, then returns the concatenated data and any event/id metadata.

**Tests required:** Multi-line data fixture; comment interleaving; event/id fields.

**Compatibility impact:** Internal parser change; external API unchanged.

---

---

## CHAT-10 | Synthetic `Finish("stop")` is emitted when the server provides no terminal chunk

**Severity:** P2
**Status:** CONFIRMED
**Area:** Streaming lifecycle
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 76-80
**Symbol:** streamChat` terminal enforcement

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming lifecycle
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 76-80
- **Symbol:** `streamChat` terminal enforcement

**Evidence:**

```kotlin
if (!hasEmittedTerminal) {
    trySend(ChatStreamChunk.Finish(reason = "stop"))
    hasEmittedTerminal = true
}
```

**Spec:** `finish_reason` is nullable and can be `stop`, `length`, or `tool_calls`; a missing final chunk indicates a truncated or interrupted stream, not a successful stop.

**Expected:** If the stream ends without a terminal chunk, the SDK should emit an error or leave the stream incomplete, preserving the actual server state.

**Actual:** The SDK fabricates a `stop` finish reason, hiding incomplete responses.

**Impact:** Consumers believe a response completed normally when it may have been cut off by network or server issues.

**Root cause:** Test-driven assumption that every stream must end with exactly one finish event.

**Related occurrences:** `ChatClientTest.kt:68-85` asserts exactly one finish event.

**Venice reference:** swagger.yaml:6261-6268 (finish_reason enum).

**Android/Kotlin reference:** N/A.

**Remediation:** Remove synthetic `stop`; emit an `Error` or incomplete terminal state when no terminal chunk is received.

**Tests required:** Test for truncated stream behavior.

**Compatibility impact:** Changes stream termination contract; consumers may now receive `Error` instead of synthetic `Finish`.

---

---

## CHAT-11 | `CancellationException` is swallowed without surfacing cancellation to the consumer

**Severity:** P2
**Status:** CONFIRMED
**Area:** Coroutine lifecycle
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt
**Lines:** 83-84
**Symbol:** streamChat` catch block

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Coroutine lifecycle
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- **Lines:** 83-84
- **Symbol:** `streamChat` catch block

**Evidence:**

```kotlin
} catch (e: CancellationException) {
    // Cancellation terminates the flow promptly; invokeOnCompletion cancels the OkHttp Call
}
```

**Expected:** In Kotlin coroutines/Flow, cancellation should propagate via `CancellationException` so downstream collectors can react.

**Actual:** The exception is caught and swallowed; the flow simply completes. Downstream code cannot distinguish cancellation from normal completion.

**Impact:** UI/state machines may incorrectly treat a canceled request as successfully completed.

**Root cause:** Swallowing cancellation to avoid emitting an error chunk.

**Related occurrences:** `ChatClientTest.kt:141-219` tests that the OkHttp call is canceled, but does not assert cancellation signaling.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin coroutines `Flow` cancellation semantics; `callbackFlow` should rethrow cancellation.

**Remediation:** Do not catch `CancellationException`; let `callbackFlow` close it naturally, or rethrow it after cleanup.

**Tests required:** Assert that cancellation propagates to the collector.

**Compatibility impact:** Behavioral change; callers relying on silent completion will see cancellation signals.

---

---

## CHAT-12 | `ChatStreamAccumulator` does not validate reconstructed tool call argument JSON

**Severity:** P2
**Status:** INFERRED
**Area:** Tool-call accumulation
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt
**Lines:** 17-20
**Symbol:** ChatStreamAccumulator.apply` (ToolCallDelta branch)

- **Severity:** P2
- **Status:** INFERRED
- **Area:** Tool-call accumulation
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt`
- **Lines:** 17-20
- **Symbol:** `ChatStreamAccumulator.apply` (ToolCallDelta branch)

**Evidence:**

```kotlin
is ChatStreamChunk.ToolCallDelta -> {
    val tc = toolCalls.getOrPut(chunk.index) { MutableToolCall() }
    chunk.callId?.let { tc.id = it }
    chunk.name?.let { tc.name = it }
    if (!chunk.argumentsFragment.isNullOrEmpty()) tc.arguments.append(chunk.argumentsFragment)
}
```

**Spec:** The API streams `function.arguments` as partial JSON strings; the final concatenation should be valid JSON.

**Expected:** The accumulator either validates the final JSON or exposes a parse helper so callers know when the arguments are incomplete/invalid.

**Actual:** Fragments are concatenated blindly; `snapshot()` returns a raw string with no validity check.

**Impact:** Callers may attempt to parse malformed JSON and crash; incomplete streams produce partial JSON without indication.

**Root cause:** No validation step in accumulation.

**Related occurrences:** `ChatStreamAccumulatorTest.kt:18-29` tests concatenation but not JSON validity.

**Venice reference:** swagger.yaml:1622-1669; `guides/features/function-calling.mdx`:124-150.

**Android/Kotlin reference:** N/A.

**Remediation:** Add an optional JSON validation in `snapshot()` and surface an error/flag if the arguments are not valid JSON.

**Tests required:** Test with incomplete and malformed argument fragments.

**Compatibility impact:** Additive if exposed as a new property.

---

---

## CHAT-13 | `ChatStreamChunk.Finish.usage` is defined but never populated from the stream

**Severity:** P2
**Status:** CONFIRMED
**Area:** Streaming response parsing
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt
**Lines:** 14
**Symbol:** ChatStreamChunk.Finish
**Also reported as:** ARCH-16

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Streaming response parsing
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt`
- **Lines:** 14
- **Symbol:** `ChatStreamChunk.Finish`

**Evidence:**

```kotlin
data class Finish(val reason: String, val usage: Usage? = null) : ChatStreamChunk()
```

No code path in `ChatClient.parseChunks` creates a `Finish` with a non-null `usage`.

**Spec:** When `stream_options.include_usage` is requested, the final SSE chunk contains a top-level `usage` object (OpenAI-compatible streaming behavior).

**Expected:** Usage is parsed from the final chunk and included in the terminal `Finish` event.

**Actual:** `usage` is always `null`.

**Impact:** Token accounting and cost display cannot be derived from the stream.

**Root cause:** Parser ignores the `usage` field.

**Related occurrences:** `ChatClient.kt:77-78`.

**Venice reference:** swagger.yaml:1377-1382 (`stream_options.include_usage`); swagger.yaml:6540-6587 (`usage` schema).

**Android/Kotlin reference:** N/A.

**Remediation:** Parse `usage` from the SSE payload and pass it to `ChatStreamChunk.Finish`.

**Tests required:** Fixture with final `usage` chunk.

**Compatibility impact:** Additive.

---

---

## CHAT-14 | `developer` role and `reasoning_content`/`reasoning_details`/`thought_signature` are unsupported

**Severity:** P2
**Status:** CONFIRMED
**Area:** Request/response schema
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 13-26
**Symbol:** ChatMessage

- **Severity:** P2
- **Status:** CONFIRMED
- **Area:** Request/response schema
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 13-26
- **Symbol:** `ChatMessage`

**Evidence:**

`ChatMessage` has no `reasoning_content`, `reasoning_details`, or `thought_signature` fields, and its `role` is an unconstrained `String` with helper constructors only for `user`, `assistant`, `system`, and `tool`.

**Spec:** swagger.yaml lines 1171-1236 define the `developer` message role; lines 1027-1065 and 6375-6414 define reasoning fields for assistant messages.

**Expected:** SDK supports `developer` messages and round-trips reasoning metadata for models that require it (e.g., Gemini thought signatures).

**Actual:** These fields are dropped on serialization/deserialization.

**Impact:** Breaks reasoning-model workflows and multi-turn tool-call conversations that require thought signatures.

**Root cause:** Message model is missing fields added for reasoning models.

**Related occurrences:** `ChatClient.kt:117-139` also ignores `delta.reasoning_content`.

**Venice reference:** swagger.yaml:1027-1065, 1171-1236, 6375-6414; `guides/features/reasoning-models.mdx`:14-100.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `reasoning_content`, `reasoning_details`, `thought_signature`, and a `developer` helper to `ChatMessage`.

**Tests required:** Round-trip serialization for reasoning fields and developer role.

**Compatibility impact:** Additive.

---

---

## DATA-01 | Area: Schema / Referential Integrity Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Schema / Referential Integrity
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt
**Lines:** 12–37
**Symbol:** parentMessageId

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt`  
**Lines:** 12–37  
**Symbol:** `parentMessageId`

**Evidence:**
```kotlin
@Entity(
    tableName = "messages",
    indices = [ ..., Index("parentMessageId"), ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    ...
    val parentMessageId: String?,
    ...
)
```
`parentMessageId` is indexed but has no `ForeignKey` referencing `MessageEntity.id`. The exported schema confirms a plain nullable TEXT column with no FK.

**Expected:** A self-referencing `ForeignKey` on `parentMessageId` with `onDelete = CASCADE` or `SET_NULL`.

**Actual:** Deleting a parent message leaves child messages with a dangling `parentMessageId`.

**Impact:** Threaded UI may reference deleted messages; referential integrity is degraded.

**Root cause:** Missing `ForeignKey` annotation for the self-reference.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `ForeignKey` constraints; SQLite `ON DELETE` actions.

**Remediation:** Add a self-referencing `ForeignKey` with an appropriate `onDelete` action; bump schema version and supply a migration.

**Tests required:** Unit test that deletes a parent message and asserts children are cascaded or nulled.

**Compatibility impact:** Schema change requires migration.

---

---

## DATA-02 | Area: Schema / Profile Isolation Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Schema / Profile Isolation
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt
**Lines:** 12–37
**Symbol:** profileId

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt`  
**Lines:** 12–37  
**Symbol:** `profileId`

**Evidence:** `MessageEntity.profileId` is a plain non-null TEXT column with no `ForeignKey` to `profiles(id)`. The repository (`ChatRepository.appendMessage`) validates `message.profileId` at runtime, but the database does not enforce it.

**Expected:** `profileId` should reference `ProfileEntity.id` or, at minimum, a `CHECK` constraint ensuring it matches the parent conversation's `profileId`.

**Actual:** A message can be inserted with a `profileId` that does not match its conversation's `profileId` if any caller bypasses `ChatRepository`.

**Impact:** Cross-profile data inconsistency; DAO-level isolation relies on caller discipline rather than schema guarantees.

**Root cause:** Missing FK/CHECK on `MessageEntity.profileId`.

**Related occurrences:** `ConversationEntity.profileId` and `ConversationFolderEntity.profileId` have FKs; `MessageEntity` is inconsistent.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `ForeignKey`; SQLite referential integrity.

**Remediation:** Add `ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = CASCADE)`.

**Tests required:** Migration/schema test; negative test inserting a message with invalid `profileId`.

**Compatibility impact:** Schema change requires migration.

---

---

## DATA-06 | Area: Profile/Conversation Isolation Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Profile/Conversation Isolation
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt
**Lines:** 56–69
**Symbol:** updateAssistantText

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 56–69  
**Symbol:** `updateAssistantText`

**Evidence:** `updateAssistantText` accepts `profileId` and `messageId` but not `conversationId`. The DAO query is:
```kotlin
@Query("UPDATE messages SET ... WHERE id = :id AND profileId = :profileId")
```
It filters by `profileId` + `id` only.

**Expected:** The repository should verify the message belongs to the conversation the caller intends to update.

**Actual:** A message in another conversation (same profile) can be updated if its `messageId` is supplied.

**Impact:** Cross-conversation message mutation/data corruption if the UI misidentifies a message ID.

**Root cause:** Missing `conversationId` parameter and validation.

**Related occurrences:** `MessageDao.updateTextAndStatus` line 23.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Query` scoping.

**Remediation:** Add `conversationId` to the method signature and include it in the WHERE clause (or load the message and verify `conversationId`).

**Tests required:** Negative test updating a message in the wrong conversation.

**Compatibility impact:** API signature change for callers.

---

---

## DATA-08 | Area: Profile Isolation / Tool Calls Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Profile Isolation / Tool Calls
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt
**Lines:** 15–16
**Symbol:** observeForMessage

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt`  
**Lines:** 15–16  
**Symbol:** `observeForMessage`

**Evidence:**
```kotlin
@Query("SELECT * FROM message_tool_calls WHERE messageId = :messageId ORDER BY createdAt ASC")
fun observeForMessage(messageId: String): Flow<List<MessageToolCallEntity>>
```
The query filters only by `messageId`; there is no `profileId` join to `messages`.

**Expected:** Tool-call observation should be scoped by profile (e.g., `JOIN messages ON ... WHERE messages.profileId = :profileId`).

**Actual:** Any caller with a valid `messageId` can observe tool calls for that message, regardless of profile ownership.

**Impact:** Potential cross-profile tool-call content leak if message IDs are exposed or guessed.

**Root cause:** DAO query lacks profile scoping; there is no repository wrapper enforcing it either.

**Related occurrences:** `MessageToolCallDao.upsert` also lacks profile/conversation validation.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Query` joins; SQLite foreign keys.

**Remediation:** Add a `profileId` parameter and join through `messages`, or expose tool-call operations only through a profile-scoped repository.

**Tests required:** Profile isolation test for tool calls.

**Compatibility impact:** DAO signature change.

---

---

## DATA-10 | Area: Corruption Handling Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Corruption Handling
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt
**Lines:** 36–43
**Symbol:** create

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 36–43  
**Symbol:** `create`

**Evidence:** `Room.databaseBuilder` is built without `fallbackToDestructiveFromCorruption()` or any corruption listener. Room's default behavior is to throw an `IllegalStateException` when the integrity check fails.

**Expected:** A corruption recovery strategy (e.g., destructive fallback with data-loss logging, or a backup restore path).

**Actual:** Corruption causes a hard crash and renders the app unusable until the user clears app data.

**Impact:** Data loss / app bricking on disk corruption.

**Root cause:** No corruption handler configured.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.Builder.fallbackToDestructiveFromCorruption()`; `DefaultDatabaseErrorHandler`.

**Remediation:** Register a corruption callback that logs a redacted event and triggers destructive fallback or backup restore.

**Tests required:** Corruption simulation test.

**Compatibility impact:** Behavior change on corruption only.

---

---

## DATA-11 | Area: Migrations Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Migrations
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt
**Lines:** 18–46
**Symbol:** AppDatabase

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 18–46  
**Symbol:** `AppDatabase`

**Evidence:**
- `version = 1`.
- Builder does not call `addMigrations()` or `fallbackToDestructiveMigration()`.
- `MigrationTest` only exercises version 1.

**Expected:** A migration strategy for future schema changes (either explicit `Migration` objects or an intentional destructive fallback).

**Actual:** Any version bump without a supplied `Migration` will cause Room to throw `IllegalStateException` on app launch.

**Impact:** Future schema evolution is blocked unless migrations are added before shipping a new version.

**Root cause:** No migration fallback configured; migration test coverage is minimal.

**Related occurrences:** `docs/superpowers/plans/2026-08-15-android-port-milestone-1.md` line 821 mentions `fallbackToDestructiveMigrationOnDowngrade(true)` in plans, but production code lacks it.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room migrations; `RoomDatabase.Builder.fallbackToDestructiveMigration()`.

**Remediation:** Add `fallbackToDestructiveMigration()` (with product approval for data loss) or commit to writing explicit migrations and keep `MigrationTest` current.

**Tests required:** Round-trip migration test for every future schema version.

**Compatibility impact:** Affects future upgrades.

---

---

## DATA-12 | Area: Schema Robustness Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Schema Robustness
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt
**Lines:** 9–20
**Symbol:** enum converters

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt`  
**Lines:** 9–20  
**Symbol:** enum converters

**Evidence:**
```kotlin
@TypeConverter fun toMessageRole(v: String?): MessageRole? = v?.let { MessageRole.valueOf(it) }
```
All four enum converters use `Enum.valueOf()` directly.

**Expected:** Graceful handling of unknown enum values (e.g., default to a safe value or `null`) to avoid crashes when an enum is renamed or a stale row is read.

**Actual:** Any enum name not present in the current code will throw `IllegalArgumentException` at read time.

**Impact:** App crash on schema drift, downgrades, or corrupted enum strings.

**Root cause:** `valueOf` used without a fallback.

**Related occurrences:** `MessageRole`, `MessageStatus`, `ConversationKind`, `ToolCallStatus` converters.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `Enum.valueOf`; Room `@TypeConverter`.

**Remediation:** Replace `valueOf` with `enumValues<T>().find { it.name == v }` and define an explicit unknown/default mapping.

**Tests required:** Unit tests reading each enum column with an unrecognized string.

**Compatibility impact:** Behavior change for invalid enum data.

---

---

## DATA-14 | Area: Profile Integrity Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Profile Integrity
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt
**Lines:** 19–26
**Symbol:** findDefault` / `deleteById

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt`  
**Lines:** 19–26  
**Symbol:** `findDefault` / `deleteById`

**Evidence:**
```kotlin
@Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
suspend fun findDefault(): ProfileEntity?
```
There is no unique index or constraint on `isDefault = 1`. `ProfileDao.deleteById(id)` has no guard against deleting the default profile.

**Expected:** Exactly one default profile at any time; deletion of the default profile should be prevented or should trigger default reassignment.

**Actual:** Multiple rows can have `isDefault = 1`; `findDefault` returns an arbitrary one. The default profile can be deleted without recourse.

**Impact:** Inconsistent default-profile selection; app may lose the active profile.

**Root cause:** Schema and DAO do not enforce singleton default semantics.

**Related occurrences:** `ProfileEntity.kt` lines 6–13; `ProfileRepository.kt` lines 7–20.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Index(unique = true)` with partial index; SQLite triggers.

**Remediation:**
- Add a migration that adds a unique partial index on `isDefault = 1` (or enforce in code).
- Guard `deleteById` to prevent deleting the last default profile, or reassign default before deletion.

**Tests required:** Test that two defaults cannot exist; test deletion of default profile.

**Compatibility impact:** Schema change requires migration.

---

---

## DATA-15 | Area: Test Coverage / Migrations Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Test Coverage / Migrations
**Module:** core:data
**File:** core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt
**Lines:** 26–50
**Symbol:** MigrationTest

**File:** `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt`  
**Lines:** 26–50  
**Symbol:** `MigrationTest`

**Evidence:** The test suite contains only:
1. `v1 schema creates all expected tables` — checks `sqlite_master` table names.
2. `AppDatabase can open v1` — opens and closes the DB.

It does not validate:
- Foreign keys or `ON DELETE` actions.
- Index definitions.
- Column nullability / types.
- Destructive fallback behavior.

**Expected:** Migration tests should verify schema integrity and future migration paths.

**Actual:** Only table existence is checked.

**Impact:** Schema regressions (missing FKs, indices, columns) can slip through.

**Root cause:** Minimal test assertions.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `MigrationTestHelper`; `PRAGMA foreign_key_check`; `PRAGMA index_list`.

**Remediation:** Add assertions for FKs, indices, and run `PRAGMA foreign_key_check` after each migration.

**Tests required:** Expand `MigrationTest`.

**Compatibility impact:** None.

---

---

## DATA-16 | Area: Import / Export / Backup Module: `core:data`

**Severity:** P2
**Status:** CONFIRMED
**Area:** Import / Export / Backup
**Module:** core:data
**File:** core/data` module (no relevant source file)
**Lines:** N/A
**Symbol:** N/A

**File:** `core/data` module (no relevant source file)  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:**
- `core/data` contains no import, export, backup, or sync APIs.
- `app/src/main/AndroidManifest.xml` lines 5–6 sets `android:allowBackup="false"` and `android:fullBackupContent="false"`.
- `docs/FEATURE_PARITY_MATRIX.md` lists `history` target as "Room persistence, folders, lock/import/export/recovery" and `privacy` target as "Storage inventory, encrypted `.vfbackup`, sync folder, purge/maintenance".

**Expected:** Core data layer should expose redacted export/import primitives (e.g., `.vfbackup`) backed by SAF/content URIs.

**Actual:** No persistence-level support for backup/export/import exists.

**Impact:** Feature-parity claims for history/privacy are not met; users cannot back up or migrate conversations.

**Root cause:** Not implemented in current milestone.

**Related occurrences:** `docs/FEATURE_PARITY_MATRIX.md` lines 9, 27, 43.

**Venice reference:** N/A.

**Android/Kotlin reference:** Storage Access Framework (SAF); `BackupAgent`; encrypted archives.

**Remediation:** Design and implement a profile-scoped, encrypted backup/export API in `core:data`.

**Tests required:** Round-trip backup/restore tests; profile isolation tests after restore.

**Compatibility impact:** New feature; no backward-compatibility risk yet.

---

## Areas checked with no confirmed findings

- **Flow emissions on wrong dispatchers:** All `Flow` sources are Room DAO flows or a single `.map` that filters by `profileId`. Room emits on its query executor; no `Dispatchers.Main` emission or `flowOn` mismatch was identified.
- **Plaintext credentials in `core:data`:** `ProfileEntity` stores only `apiKeyAlias`; actual API keys are handled by `core:security` (`SecureSecretStore`).
- **Telemetry / logging:** No logging of prompts, responses, or DB contents was found in the reviewed files.

---

## Summary counts

| Severity | Count |
|---|---|
| P0 | 0 |
| P1 | 5 |
| P2 | 9 |
| P3 | 2 |
| **Total** | **16** |

---

## DOC-03 | - `README.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Source baseline / API snapshot tracking
**Module:** Repository-wide
**File:** README.md`, `SOURCE_BASELINE.md`, `docs/reference/VENICE_API_SOURCE_MANIFEST.md
**Lines:** README.md:114`; `SOURCE_BASELINE.md:9`; `VENICE_API_SOURCE_MANIFEST.md:9

**Area:** Source baseline / API snapshot tracking  
**Module:** Repository-wide  
**Files:** `README.md`, `SOURCE_BASELINE.md`, `docs/reference/VENICE_API_SOURCE_MANIFEST.md`  
**Lines:** `README.md:114`; `SOURCE_BASELINE.md:9`; `VENICE_API_SOURCE_MANIFEST.md:9`

**Evidence:**
- `README.md:114` states: `Venice OpenAPI schema version: 20260814.153445`.
- `SOURCE_BASELINE.md:9` states: `Venice OpenAPI schema version: 20260814.194349`.
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:9` states: `OpenAPI Schema Version: 20260814.194349`.
- The repository facts provided to auditors list upstream HEAD `6e69346b` and `info.version 20260814.194349`.

**Expected:** The README baseline should match the authoritative tracked upstream version recorded in `SOURCE_BASELINE.md` and the source manifest.

**Actual:** README cites an older schema version (`20260814.153445`) that matches the desktop archive snapshot, not the current `veniceai/api-docs` HEAD.

**Impact:** New contributors may bootstrap against the wrong API snapshot and implement against stale endpoint semantics.

**Root cause:** README copied the desktop archive's embedded OpenAPI version instead of the refreshed upstream mirror version.

**Related occurrences:** `docs/VENICE_API_PORT_MATRIX.md:3` also says "Generated from the desktop tracked OpenAPI snapshot `20260814.153445`", which is consistent with the README but stale relative to the current upstream baseline.

**Venice reference:** `veniceai/api-docs` `swagger.yaml` `info.version` is the authoritative version string; current value is `20260814.194349` per `SOURCE_BASELINE.md` and the provided repository facts.

**Android/Kotlin reference:** N/A.

**Remediation:** Update `README.md:114` to `20260814.194349` and refresh `docs/VENICE_API_PORT_MATRIX.md:3` to match. Re-run the bootstrap script and re-verify the version string.

**Tests required:** None.

**Compatibility impact:** None.

---

---

## DOC-04 | - `docs/VENICE_API_PORT_MATRIX.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Venice API port matrix
**Module:** :venice-sdk
**File:** docs/VENICE_API_PORT_MATRIX.md`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt
**Lines:** VENICE_API_PORT_MATRIX.md:17–18`; `CapabilitiesRepository.kt:21–120

**Area:** Venice API port matrix  
**Module:** `:venice-sdk`  
**Files:** `docs/VENICE_API_PORT_MATRIX.md`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** `VENICE_API_PORT_MATRIX.md:17–18`; `CapabilitiesRepository.kt:21–120`

**Evidence:**
- `docs/VENICE_API_PORT_MATRIX.md:17–18` lists `/models/traits` and `/models/compatibility_mapping` with status **Planned typed SDK method**.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt:21` declares `class CapabilitiesRepository(private val sdk: VeniceForgeSdk)`.
- `CapabilitiesRepository.kt:94–119` implements `fetchTraits(...)` and `fetchCompatibility(...)`, calling `sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)` and `sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)` and parsing the JSON into typed `Map<String, String>`.
- `CapabilitiesRepository.kt:86–91` merges the results into a typed `ModelCatalog`.

**Expected:** The port matrix should reflect implemented endpoints.

**Actual:** The matrix still lists the traits and compatibility endpoints as planned, even though `CapabilitiesRepository` already consumes them.

**Impact:** The roadmap understates current SDK capability coverage and may cause duplicate implementation work.

**Root cause:** The matrix was generated before `CapabilitiesRepository` was implemented and not updated.

**Related occurrences:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md:31–32` correctly marks traits and compatibility as **VERIFIED**, so the matrix is internally inconsistent with the manifest.

**Venice reference:** `swagger.yaml` defines `GET /models/traits` and `GET /models/compatibility_mapping`; the parsing in `CapabilitiesRepository` matches the dictionary shape described in `VENICE_API_SOURCE_MANIFEST.md:53–57`.

**Android/Kotlin reference:** N/A.

**Remediation:** Update `docs/VENICE_API_PORT_MATRIX.md:17–18` to **Foundation** or **Implemented** for `/models/traits` and `/models/compatibility_mapping`, consistent with the source manifest.

**Tests required:** None.

**Compatibility impact:** None.

---

---

## DOC-05 | - `docs/reference/VENICE_API_SOURCE_MANIFEST.

**Severity:** P2
**Status:** CONFIRMED
**Area:** API source manifest
**Module:** :venice-sdk
**File:** docs/reference/VENICE_API_SOURCE_MANIFEST.md`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** VENICE_API_SOURCE_MANIFEST.md:36–38`; `ImageClient.kt:15–91`; `AudioClient.kt:15–45`; `VideoClient.kt:15–119

**Area:** API source manifest  
**Module:** `:venice-sdk`  
**Files:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** `VENICE_API_SOURCE_MANIFEST.md:36–38`; `ImageClient.kt:15–91`; `AudioClient.kt:15–45`; `VideoClient.kt:15–119`

**Evidence:**
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:36` says Images → **Planned :venice-sdk image service**.
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:37` says Video → **Planned :venice-sdk video service**.
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:38` says Audio & Music → **Planned :venice-sdk audio service**.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:15` declares `class ImageClient(private val sdk: VeniceForgeSdk)` with `generate`, `upscale`, `edit`, and `multiEdit` methods.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt:15` declares `class AudioClient(private val sdk: VeniceForgeSdk)` with `speech(...)`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt:15` declares `class VideoClient(private val sdk: VeniceForgeSdk)` with `queue`, `complete`, and `retrieve`.

**Expected:** The source manifest should describe the current SDK surface.

**Actual:** The manifest lists image, audio, and video SDK services as planned, but typed clients already exist.

**Impact:** The source-of-truth manifest understates implementation progress and contradicts `CHANGELOG.md:17–20`.

**Root cause:** Manifest was written before the image/audio/video clients landed.

**Related occurrences:** `docs/VENICE_API_PORT_MATRIX.md` also lists all image/audio/video endpoints as Planned; see DOC-04 context.

**Venice reference:** `swagger.yaml` defines the relevant endpoints; the request/response models in `ImageModels.kt`, `AudioModels.kt`, and `VideoModels.kt` align with the wire shapes.

**Android/Kotlin reference:** N/A.

**Remediation:** Update the manifest rows for Images, Audio & Music, and Video to reflect the implemented SDK clients and current test coverage. If UI/queue state-machine work remains, split the status into SDK vs. UI.

**Tests required:** Verify the manifest rows match the actual client classes and tests.

**Compatibility impact:** None.

---

---

## DOC-06 | - `docs/reference/VENICE_API_SOURCE_MANIFEST.

**Severity:** P2
**Status:** CONFIRMED
**Area:** API source manifest / test coverage claims
**Module:** :venice-sdk` / `:app` tests
**File:** docs/reference/VENICE_API_SOURCE_MANIFEST.md
**Lines:** 30, 35

**Area:** API source manifest / test coverage claims  
**Module:** `:venice-sdk` / `:app` tests  
**File:** `docs/reference/VENICE_API_SOURCE_MANIFEST.md`  
**Lines:** 30, 35

**Evidence:**
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:30` lists verification as `VeniceForgeSdkTest.kt`, `ModelCatalogTest.kt` for the Models surface.
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md:35` lists verification as `VeniceSdkExceptionTest.kt` for Errors & Rate Limits.
- Repository glob search for `**/VeniceSdkExceptionTest.kt` returned no matches.
- Repository glob search for `**/ModelCatalogTest.kt` returned no matches.
- Existing tests observed: `VeniceForgeSdkTest.kt`, `CapabilitiesRepositoryTest.kt`, `ChatClientTest.kt`, `SseLineParserTest.kt`, `ChatStreamAccumulatorTest.kt`, `VeniceParametersSerializationTest.kt`, `VeniceEndpointsTest.kt`, `ImageClientTest.kt`.

**Expected:** Referenced test files should exist.

**Actual:** Two test files named in the manifest do not exist in the repository.

**Impact:** The manifest falsely claims test coverage, which can mislead reviewers and release gates.

**Root cause:** The manifest copied planned test names that were never created, or tests were renamed (e.g., `CapabilitiesRepositoryTest` may have replaced `ModelCatalogTest`).

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Either create the missing tests or update the manifest to reference the actual test files (`CapabilitiesRepositoryTest.kt` for model/traits/compatibility, and add an exception-hierarchy test file or remove the non-existent reference).

**Tests required:** Add or rename tests to match the manifest, or vice versa.

**Compatibility impact:** None.

---

---

## IMG-02 | - `VeniceEndpoints.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Endpoint coverage / Missing feature
**Module:** :venice-sdk
**File:** VeniceEndpoints.kt`, `ImageClient.kt
**Lines:** VeniceEndpoints.kt:20`, `ImageClient.kt:1-91
**Symbol:** IMAGE_BACKGROUND_REMOVE

**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:20`, `ImageClient.kt:1-91`  
**Symbol:** `IMAGE_BACKGROUND_REMOVE`

**Evidence:**
- `VeniceEndpoints.kt:20` declares `IMAGE_BACKGROUND_REMOVE = "image/background-remove"`.
- `ImageClient.kt` has no method using this constant.
- Swagger `/image/background-remove` (lines 8330–8463) is a fully documented POST endpoint accepting JSON or multipart and returning a PNG binary.

**Expected:** SDK exposes a method for background removal.

**Actual:** Constant exists but no public API method exists.

**Impact:** Consumers cannot use background removal through the SDK.

**Root cause:** Endpoint constant added without corresponding client method.

**Related occurrences:** `VeniceEndpoints.kt:15-16` (`IMAGE_GENERATIONS_COMPAT`, `IMAGE_STYLES`) also lack client methods (see IMG-03, IMG-04).

**Venice reference:** `swagger.yaml:/paths/image/background-remove/post`.

**Remediation:** Add `backgroundRemove(apiKey, BackgroundRemoveImageRequest): ByteArray` method. Support both JSON (`image` base64 / `image_url`) and multipart file upload variants.

**Tests required:** Unit test for background-remove returning PNG bytes; verify request body shape.

---

---

## IMG-03 | - `VeniceEndpoints.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Endpoint coverage / Missing feature
**Module:** :venice-sdk
**File:** VeniceEndpoints.kt`, `ImageClient.kt
**Lines:** VeniceEndpoints.kt:16
**Symbol:** IMAGE_STYLES

**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:16`  
**Symbol:** `IMAGE_STYLES`

**Evidence:**
- `VeniceEndpoints.kt:16` declares `IMAGE_STYLES = "image/styles"`.
- `ImageClient.kt` has no method for this endpoint.
- Swagger `/image/styles` (lines 7668–7714) is a GET endpoint returning `{ "data": [...], "object": "list" }`.

**Expected:** SDK exposes a method to list available image styles.

**Actual:** No method exists.

**Impact:** Consumers must call the endpoint manually via `VeniceForgeSdk.getRaw`; no typed response model exists.

**Root cause:** Endpoint constant added without corresponding client method or response model.

**Venice reference:** `swagger.yaml:/paths/image/styles/get`.

**Remediation:** Add `styles()` method returning a typed `ImageStylesResponse(data: List<String>, object: String?)`.

**Tests required:** Unit test for `/image/styles` parsing the example response.

---

---

## IMG-04 | - `VeniceEndpoints.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Endpoint coverage / Missing feature
**Module:** :venice-sdk
**File:** VeniceEndpoints.kt`, `ImageClient.kt
**Lines:** VeniceEndpoints.kt:15
**Symbol:** IMAGE_GENERATIONS_COMPAT

**Area:** Endpoint coverage / Missing feature  
**Module:** `:venice-sdk`  
**File:** `VeniceEndpoints.kt`, `ImageClient.kt`  
**Lines:** `VeniceEndpoints.kt:15`  
**Symbol:** `IMAGE_GENERATIONS_COMPAT`

**Evidence:**
- `VeniceEndpoints.kt:15` declares `IMAGE_GENERATIONS_COMPAT = "images/generations"`.
- `ImageClient.kt` has no method for this endpoint.
- Swagger `/images/generations` (lines 7455–7667) is the OpenAI-compatible image generation endpoint returning `{ created, data: [{ b64_json | url }] }`.

**Expected:** SDK exposes an OpenAI-compatible image generation method.

**Actual:** No method exists.

**Impact:** Consumers targeting OpenAI-compatible tooling cannot use this endpoint through the SDK.

**Root cause:** Endpoint constant added without corresponding client method or response model.

**Venice reference:** `swagger.yaml:/paths/images/generations/post`.

**Remediation:** Add `simpleGenerate(apiKey, SimpleGenerateImageRequest): SimpleGenerateImageResponse` with the OpenAI-shaped response.

**Tests required:** Unit test for `/images/generations` request/response serialization.

---

---

## IMG-05 | - Both request paths in `ImageClient.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Request encoding / File upload
**Module:** :venice-sdk
**File:** ImageClient.kt
**Lines:** 36–44, 65–74
**Symbol:** executeRequest`, `executeBinaryRequest

**Area:** Request encoding / File upload  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 36–44, 65–74  
**Symbol:** `executeRequest`, `executeBinaryRequest`

**Evidence:**
- Both request paths in `ImageClient.kt` set `Content-Type: application/json` via `reqBody.toRequestBody(jsonMedia)` (`ImageClient.kt:44`, `ImageClient.kt:73`).
- Swagger `/image/upscale` (lines 7729–7736) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/edit` (lines 7867–7898) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/multi-edit` (lines 8122–8155) accepts both `application/json` and `multipart/form-data`.
- Swagger `/image/background-remove` (lines 8345–8352) accepts both `application/json` and `multipart/form-data`.

**Expected:** SDK supports multipart/form-data file uploads for endpoints that accept them.

**Actual:** SDK only sends JSON. Callers must base64-encode files, increasing payload size and CPU cost.

**Impact:** File upload workflows are unsupported; large images may hit memory/performance limits on Android.

**Root cause:** Single JSON-only request path; no multipart builder.

**Venice reference:** `swagger.yaml:/paths/*/post/requestBody/content/multipart/form-data` for image endpoints.

**Android/Kotlin reference:** OkHttp `MultipartBody.Builder` is the standard API for multipart uploads.

**Remediation:** Add multipart upload variants for `upscale`, `edit`, `multiEdit`, and `backgroundRemove` that accept `okhttp3.RequestBody` or file paths/URIs.

**Tests required:** Unit tests verifying multipart body parts and `Content-Type: multipart/form-data` boundary.

---

---

## IMG-06 | - `ImageModels.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Response model completeness
**Module:** :venice-sdk
**File:** ImageModels.kt
**Lines:** 79–84
**Symbol:** GenerateImageResponse

**Area:** Response model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 79–84  
**Symbol:** `GenerateImageResponse`

**Evidence:**
- `ImageModels.kt:79-84` defines `GenerateImageResponse(id, images, timing)` with all fields nullable.
- Swagger `/image/generate` 200 response schema (lines 7319–7356) defines `id`, `images`, and `timing` as `required`; it also defines a `request` field (nullable object) that echoes the original request.

**Expected:** `GenerateImageResponse` should match the swagger response shape: required `id`, `images`, `timing`; optional `request`.

**Actual:** All fields are nullable and `request` is missing.

**Impact:** Callers cannot access the echoed request, and nullability is wider than the contract guarantees. The SDK silently drops `request` data.

**Root cause:** Response model not updated to match current swagger.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/content/application/json/schema`.

**Remediation:** Add `request: JsonElement?` (or a typed request echo) to `GenerateImageResponse`; make `id`, `images`, `timing` non-nullable.

**Tests required:** Deserialize a generate response containing `request`; assert `request` is accessible.

**Compatibility impact:** Making fields non-nullable is a breaking change for Kotlin callers; consider deprecation path.

---

---

## IMG-07 | - `ImageModels.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Request model completeness
**Module:** :venice-sdk
**File:** ImageModels.kt
**Lines:** 13–35
**Symbol:** GenerateImageRequest

**Area:** Request model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 13–35  
**Symbol:** `GenerateImageRequest`

**Evidence:**
- `ImageModels.kt:13-35` includes `model`, `prompt`, `negativePrompt`, `stylePreset`, `height`, `width`, `steps`, `cfgScale`, `seed`, `safeMode`, `returnBinary`, `hideWatermark`, `format`, `variants`, `aspectRatio`, `resolution`, `quality`, `enableWebSearch`, `disablePromptOptimizationThinking`, `enhancePrompt`, `styleReferences`.
- Swagger `GenerateImageRequest` (lines 2583–2777) additionally defines:
  - `embed_exif_metadata` (boolean, default false)
  - `inpaint` (deprecated, nullable)
  - `lora_strength` (integer, 0–100)

**Expected:** SDK request model exposes all non-deprecated swagger fields.

**Actual:** `embed_exif_metadata` and `lora_strength` are missing; `inpaint` is also missing (deprecated but still in schema).

**Impact:** Callers cannot control EXIF embedding or LoRA strength for supported models.

**Root cause:** Request model not kept in sync with swagger.

**Venice reference:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/embed_exif_metadata`, `/components/schemas/GenerateImageRequest/properties/lora_strength`.

**Remediation:** Add `embedExifMetadata: Boolean?`, `loraStrength: Int?`, and optionally `inpaint` (marked deprecated) to `GenerateImageRequest`.

**Tests required:** Serialization test verifying new fields map to correct wire names.

---

---

## IMG-08 | - `ImageModels.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Request model completeness
**Module:** :venice-sdk
**File:** ImageModels.kt
**Lines:** 45–55
**Symbol:** EditImageRequest

**Area:** Request model completeness  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 45–55  
**Symbol:** `EditImageRequest`

**Evidence:**
- `ImageModels.kt:45-55` defines `EditImageRequest(image, prompt, model, aspectRatio, resolution, outputFormat, disablePromptOptimizationThinking, enhancePrompt, safeMode)`.
- Swagger `EditImageRequest` (lines 2933–3027) includes `modelId` (deprecated) in addition to `model`.

**Expected:** SDK request model includes deprecated `modelId` for backwards compatibility.

**Actual:** `modelId` is missing.

**Impact:** Existing code or docs referencing `modelId` cannot be used with the SDK.

**Root cause:** Request model not kept in sync with swagger.

**Venice reference:** `swagger.yaml:/components/schemas/EditImageRequest/properties/modelId`.

**Remediation:** Add `@SerialName("modelId") val modelId: String? = null` to `EditImageRequest`.

**Tests required:** Serialization test verifying `modelId` is sent when provided.

---

---

## IMG-09 | - `VeniceForgeSdk.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Error handling / Paid operations
**Module:** :venice-sdk
**File:** VeniceForgeSdk.kt
**Lines:** 166–200
**Symbol:** parseHttpError

**Area:** Error handling / Paid operations  
**Module:** `:venice-sdk`  
**File:** `VeniceForgeSdk.kt`  
**Lines:** 166–200  
**Symbol:** `parseHttpError`

**Evidence:**
- `VeniceForgeSdk.kt:166-200` maps 401/403 to `Authentication`, 400/422 to `Validation`, 500–599 to `Server`, 429 to `RateLimit`, and everything else to generic `Http`.
- Swagger image endpoints return `402` for insufficient balance / x402 payment required and `415` for invalid content-type.
- `415` responses use `StandardError`; `402` responses may be `StandardError` or `X402InferencePaymentRequired`.

**Expected:** SDK surfaces `402 Payment Required` as a distinct exception type so callers can detect insufficient balance.

**Actual:** `402` falls into the generic `Http` exception; callers cannot easily distinguish payment failures from other errors.

**Impact:** Paid/mutating image operations cannot gracefully guide users to top-up; AGENTS.md requires explicit approval for paid operations, but the SDK does not expose the signal.

**Root cause:** Exception mapping does not account for image-endpoint-specific 402 semantics.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/402`, `/paths/image/edit/post/responses/402`, etc.

**Remediation:** Add `VeniceSdkException.PaymentRequired` (or extend `Http` with a dedicated subtype) for HTTP 402, parsing `X402InferencePaymentRequired` fields.

**Tests required:** Unit tests for 402 responses on image endpoints.

---

---

## IMG-10 | - `ImageClient.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Response metadata / Information loss
**Module:** :venice-sdk
**File:** ImageClient.kt
**Lines:** 36–63
**Symbol:** executeRequest

**Area:** Response metadata / Information loss  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 36–63  
**Symbol:** `executeRequest`

**Evidence:**
- `ImageClient.kt:36-63` reads only the response body string; no headers are captured.
- Swagger `/image/generate` 200 response defines headers: `x-venice-is-blurred`, `x-venice-is-content-violation`, `x-venice-model-deprecation-warning`, `x-venice-model-deprecation-date`, `x-venice-deprecated`, `x-venice-deprecated-replacement`, `X-Balance-Remaining`.
- Swagger `/image/edit` and `/image/multi-edit` define `x-venice-is-content-violation`, `x-venice-model-id`, `x-venice-model-name`, and deprecation headers.

**Expected:** SDK exposes relevant Venice response headers to callers.

**Actual:** All Venice-specific headers are discarded.

**Impact:** Callers cannot detect blurred images, content violations, model deprecation, or remaining x402 balance. This violates the principle of preserving upstream semantics.

**Root cause:** `executeRequest` only returns the deserialized body, not header metadata.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/headers`, `/paths/image/edit/post/responses/200/headers`.

**Remediation:** Return a wrapper object (e.g., `ImageGenerationResult`) containing `body: GenerateImageResponse` and `headers: ImageResponseHeaders`, or expose headers via a callback.

**Compatibility impact:** Breaking API change; can be offered as an additional method while deprecating the old one.

---

---

## IMG-11 | - `ImageClient.

**Severity:** P2
**Status:** CONFIRMED
**Area:** API footgun / Binary vs JSON
**Module:** :venice-sdk
**File:** ImageClient.kt
**Lines:** 19–25
**Symbol:** generate`, `generateBinary

**Area:** API footgun / Binary vs JSON  
**Module:** `:venice-sdk`  
**File:** `ImageClient.kt`  
**Lines:** 19–25  
**Symbol:** `generate`, `generateBinary`

**Evidence:**
- `ImageClient.kt:19-20` `generate()` always sends `Accept: application/json` and parses JSON.
- `ImageClient.kt:22-25` `generateBinary()` requires `returnBinary == true` and sends `Accept: image/*`.
- Swagger `/image/generate` returns binary when `return_binary=true`.

**Expected:** If a caller accidentally calls `generate()` with `returnBinary=true`, the SDK should either reject it or route to binary handling.

**Actual:** `generate()` does not validate `returnBinary`; it will request JSON and then fail to parse binary image bytes.

**Impact:** Easy-to-make caller error results in a confusing `Protocol` exception instead of a clear validation error or correct binary response.

**Root cause:** Two separate methods without guardrails; `generate()` is not binary-safe.

**Venice reference:** `swagger.yaml:/paths/image/generate/post/responses/200/content/image/*`.

**Remediation:** Either merge `generate`/`generateBinary` into a single method that inspects `returnBinary`, or add an explicit check in `generate()` throwing `IllegalArgumentException` when `returnBinary == true`.

**Tests required:** Unit test asserting `generate()` with `returnBinary=true` fails fast with a clear exception.

---

---

## SDK-CORE-03 | `ModelSpec` contains only: `name`, `description`, `modelSource`, `availableContextTokens`, `maxCompletionTokens`, `privacy`, `uncensored`, `offline`, `beta`, `betaModel`, `traits`, `capabilities`.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Model/Capability Discovery
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt
**Lines:** VeniceModel.kt:8-58, ModelCapabilities.kt:11-38
**Symbol:** VeniceModel`, `ModelSpec`, `ModelCapabilitiesSpec`, `ModelCapabilities

**Area:** Model/Capability Discovery  
**Module:** `:venice-sdk`  
**Files:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceModel.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** VeniceModel.kt:8-58, ModelCapabilities.kt:11-38  
**Symbol:** `VeniceModel`, `ModelSpec`, `ModelCapabilitiesSpec`, `ModelCapabilities`

**Evidence:**
`ModelSpec` contains only: `name`, `description`, `modelSource`, `availableContextTokens`, `maxCompletionTokens`, `privacy`, `uncensored`, `offline`, `beta`, `betaModel`, `traits`, `capabilities`.
`ModelCapabilities` omits constraints, pricing, deprecation, quantization, reasoning effort options, max images/videos, media-specific fields, etc.

**Venice reference:** `swagger.yaml:4659-6075` defines `ModelResponse` and `model_spec` with fields such as `context_length`, `discount_to_user`, `constraints`, `deprecation`, `regionRestrictions`, `pricing`, `capabilities.quantization`, `capabilities.reasoningEffortOptions`, `capabilities.maxImages`, `capabilities.maxVideos`, `embeddingDimensions`, `voices`, `voice_cloning`, `supported_formats`, etc.

**Expected:** The SDK should expose enough model metadata for callers to make capability-aware decisions and validate request parameters.

**Actual:** A large subset of authoritative model metadata is discarded during parsing.

**Impact:** Apps cannot enforce per-model constraints (aspect ratios, resolutions, max images, max videos), display pricing, or reason about deprecation/replacement. UI may offer options that the selected model does not support.

**Root cause:** Minimalistic parser/data model that only covers chat-relevant fields.

**Related occurrences:** `VeniceForgeSdk.parseModelSpec` lines 222-261.

**Android/Kotlin reference:** Kotlinx Serialization `Json { ignoreUnknownKeys = true }`.

**Remediation:** Expand `ModelSpec`/`ModelCapabilities` to include at least `constraints`, `pricing`, `deprecation`, `quantization`, `reasoningEffortOptions`, `maxImages`, `maxVideos`, and media-specific fields. Keep unknown fields accessible via `rawJson`.

**Tests required:** Deserialize a comprehensive swagger-like model fixture and assert all new fields are populated.

**Compatibility impact:** Adding properties with defaults preserves source compatibility; data-class binary compatibility requires care (prefer non-data class or stable API module).

---

---

## SDK-CORE-04 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Model/Capability Semantics
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt
**Lines:** 22-23, 43-50
**Symbol:** supportsStreaming`, `supportsSystemPrompt`, `supportsTextChat`, `supportsImageGeneration

**Area:** Model/Capability Semantics  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** 22-23, 43-50  
**Symbol:** `supportsStreaming`, `supportsSystemPrompt`, `supportsTextChat`, `supportsImageGeneration`

**Evidence:**
```kotlin
val supportsStreaming: Boolean = true,
val supportsSystemPrompt: Boolean = true,
```
```kotlin
val supportsTextChat: Boolean
    get() = type.equals("text", ignoreCase = true) || type.equals("code", ignoreCase = true)
val supportsImageGeneration: Boolean
    get() = type.equals("image", ignoreCase = true)
```

**Venice reference:** `swagger.yaml:5239-5242` defines `model_spec.offline` as "Is this model presently offline?" — not a streaming capability flag. `swagger.yaml:6055-6067` defines model `type` enum: `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video`. There is no `code` type in the response enum.

**Expected:** `supportsStreaming` and `supportsSystemPrompt` should be derived from authoritative capability/constraint fields, not hard-coded. `supportsTextChat` should align with the swagger `type` enum.

**Actual:** `supportsSystemPrompt` is always true. `supportsStreaming` is derived from `offline != true` in `CapabilitiesRepository`. `supportsTextChat` treats `"code"` as a valid type, which never appears in `/models` responses. `supportsImageGeneration` ignores `inpaint`/`upscale` types that also produce images.

**Impact:** Capability queries return incorrect or misleading results. UI may claim a model supports streaming/system prompts when it does not.

**Root cause:** Hard-coded defaults and heuristic type checks instead of parsing authoritative capability fields.

**Related occurrences:** `CapabilitiesRepository.kt` lines 66, 67.

**Android/Kotlin reference:** Kotlin data class default property values.

**Remediation:** Parse `supportsSystemPrompt` and `supportsStreaming` from authoritative fields if/when Venice exposes them; otherwise expose them as nullable/unknown rather than `true`. Align `supportsTextChat` and `supportsImageGeneration` with the actual swagger `type` enum.

**Tests required:** Tests with models whose `offline=true` and models of type `inpaint`/`upscale`.

**Compatibility impact:** Changing default values or semantics may alter existing consumer behavior; document as behavior change.

---

---

## SDK-CORE-05 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Error Handling / Resilience
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt
**Lines:** 94-120
**Symbol:** fetchTraits`, `fetchCompatibility
**Also reported as:** ARCH-17

**Area:** Error Handling / Resilience  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
catch (_: Exception) {
    return emptyMap()
}
```

**Expected:** Network or parse failures for traits/compatibility should be surfaced to the caller so the app knows discovery is incomplete.

**Actual:** All exceptions are swallowed and empty maps are returned. `fetchLiveCapabilities` succeeds with a catalog missing traits/aliases.

**Impact:** Silent partial failure. Apps may display stale or missing default models and compatibility aliases without knowing the data is incomplete.

**Root cause:** Broad `catch (_: Exception)` returning empty collection.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `try/catch`, coroutine exception transparency.

**Remediation:** Let exceptions propagate, or return a sealed result (`Success`, `Partial(models, traitsError, compatError)`). Do not swallow generic exceptions.

**Tests required:** Tests that simulate 5xx/parse errors for traits/compatibility and assert the exception propagates or is represented in the result.

**Compatibility impact:** API signature change if returning a sealed result; behavioral change if exceptions now propagate.

---

---

## SDK-CORE-06 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Public API / Enum Handling
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** ModelType.kt:21-24, VeniceForgeSdk.kt:100-104
**Symbol:** ModelType.fromWireName`, `listModels(apiKey, type: String)

**Area:** Public API / Enum Handling  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt`, `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** ModelType.kt:21-24, VeniceForgeSdk.kt:100-104  
**Symbol:** `ModelType.fromWireName`, `listModels(apiKey, type: String)`

**Evidence:**
```kotlin
companion object {
    fun fromWireName(wireName: String?): ModelType? {
        if (wireName == null) return null
        return entries.firstOrNull { it.wireName.equals(wireName, ignoreCase = true) }
    }
}
```
```kotlin
@Deprecated("Use typed listModels(apiKey, ModelType?) instead")
suspend fun listModels(apiKey: String, type: String): List<VeniceModel> {
    val modelType = ModelType.fromWireName(type)
    return listModels(apiKey, modelType)
}
```

**Venice reference:** `swagger.yaml:8475-8497` defines the `/models?type=` query parameter with a constrained enum.

**Expected:** An unknown type string should either fail fast or be passed through to the server for validation.

**Actual:** Unknown wire names map to `null`, and `listModels(apiKey, null)` omits the `type` query parameter, returning all models instead of filtering by the requested type.

**Impact:** Callers using the deprecated overload with a typo or future type get silently incorrect results.

**Root cause:** `fromWireName` returns `null` for unknown values, and the caller treats `null` as "no filter".

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin enum lookup.

**Remediation:** In the deprecated overload, if `fromWireName` returns `null`, throw `IllegalArgumentException` or pass the raw string through to the server. Better: remove the deprecated overload.

**Tests required:** Test that an unknown type string does not silently return all models.

**Compatibility impact:** Removing the deprecated overload is a source-breaking change; failing fast on unknown type changes runtime behavior.

---

---

## SDK-CORE-07 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Caching / Performance
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt
**Lines:** 21-92
**Symbol:** fetchLiveCapabilities

**Area:** Caching / Performance  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 21-92  
**Symbol:** `fetchLiveCapabilities`

**Evidence:**
```kotlin
class CapabilitiesRepository(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
        val models = sdk.listModels(apiKey, null)
        val traitsMap = fetchTraits(apiKey)
        val compatMap = fetchCompatibility(apiKey)
        ...
    }
}
```

**Expected:** Model catalog data should be cacheable with a configurable TTL to avoid redundant network calls.

**Actual:** Every invocation performs three sequential network requests. There is no cache, no TTL, and no staleness check.

**Impact:** Higher latency, more bandwidth, and unnecessary load on Venice endpoints. `ModelCatalog.refreshedAt` is unused.

**Root cause:** Stateless repository with no caching layer.

**Related occurrences:** `VeniceForgeSdk.listModels`.

**Android/Kotlin reference:** Kotlin coroutines, in-memory caching patterns.

**Remediation:** Add an optional in-memory cache keyed by API key + type with a configurable TTL (e.g., 5 minutes), and expose a `forceRefresh` parameter.

**Tests required:** Tests verifying cache hit, TTL expiry, and force refresh.

**Compatibility impact:** Additive API if TTL/cache are optional.

---

---

## SDK-CORE-09 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Transport / Client Configuration
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** 27-29
**Symbol:** VeniceForgeSdk` constructor

**Area:** Transport / Client Configuration  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 27-29  
**Symbol:** `VeniceForgeSdk` constructor

**Evidence:**
```kotlin
class VeniceForgeSdk(
    private val config: VeniceSdkConfig = VeniceSdkConfig(),
    private val httpClient: OkHttpClient = OkHttpClient(),
)
```

**Expected:** The SDK should either provide sensible default timeouts or expose timeout configuration in `VeniceSdkConfig`.

**Actual:** The default `OkHttpClient` uses OkHttp's built-in 10-second connect/read/write timeouts and creates a new connection pool/dispatcher per instance.

**Impact:** On slow networks, requests may time out unexpectedly. Multiple SDK instances do not share connection pools, reducing efficiency.

**Root cause:** Default constructor delegates to a bare `OkHttpClient()` with no SDK-level configuration.

**Related occurrences:** All feature clients use `sdk.httpClient()`.

**Android/Kotlin reference:** OkHttp `OkHttpClient.Builder`, `connectTimeout`, `readTimeout`, `writeTimeout`.

**Remediation:** Add timeout fields to `VeniceSdkConfig` and apply them in a default `OkHttpClient` builder, or document that callers must supply a configured client.

**Tests required:** Tests verifying custom timeout configuration is honored.

**Compatibility impact:** Additive if config properties have defaults.

---

---

## SDK-CORE-10 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Documentation / Source Accuracy
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt
**Lines:** 4-8
**Symbol:** File header comment

**Area:** Documentation / Source Accuracy  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpoints.kt`  
**Lines:** 4-8  
**Symbol:** File header comment

**Evidence:**
```kotlin
/**
 * Complete endpoint-path inventory from Venice Forge's tracked 2026-08-14
 * OpenAPI snapshot (schema 20260814.153445).
 */
```

**Venice reference:** `swagger.yaml` `info.version: 20260814.194349`.

**Expected:** The tracked schema version in code comments should match the bootstrapped upstream source.

**Actual:** Comment references `20260814.153445`, which does not match the current upstream `20260814.194349`.

**Impact:** Misleading documentation; developers may think the SDK is pinned to an older schema.

**Root cause:** Stale comment not updated when the API docs were bootstrapped.

**Related occurrences:** None.

**Android/Kotlin reference:** N/A.

**Remediation:** Update the comment to the current `info.version` and consider deriving it from a build-time constant.

**Tests required:** N/A.

**Compatibility impact:** None.

---

---

## SDK-CORE-11 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Model / Capability Data Types
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt
**Lines:** 28-30
**Symbol:** availableContextTokens`, `maxContextTokens`, `maxCompletionTokens

**Area:** Model / Capability Data Types  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`  
**Lines:** 28-30  
**Symbol:** `availableContextTokens`, `maxContextTokens`, `maxCompletionTokens`

**Evidence:**
```kotlin
val availableContextTokens: Long? = null,
val maxContextTokens: Int? = null,
val maxCompletionTokens: Int? = null,
```

**Venice reference:** `swagger.yaml:4698-4708` defines `availableContextTokens` and `maxCompletionTokens` as `number` (no explicit format, JSON number).

**Expected:** Token counts should use a type that cannot overflow (e.g., `Long?`).

**Actual:** `maxContextTokens` and `maxCompletionTokens` are `Int?`, which can overflow for large context windows (e.g., 2M tokens = 2,000,000 fits in Int, but future values may not). `availableContextTokens` is `Long?` while the derived `maxContextTokens` is `Int?`, creating inconsistency.

**Impact:** Potential integer overflow or precision loss for large models; inconsistent API surface.

**Root cause:** Mixed `Long`/`Int` types for the same semantic values.

**Related occurrences:** `VeniceModel.kt` line 28-30 (`ModelSpec` uses `Long?`).

**Android/Kotlin reference:** Kotlin numeric types.

**Remediation:** Change `maxContextTokens` and `maxCompletionTokens` to `Long?` to match `ModelSpec` and swagger `number` semantics.

**Tests required:** Tests with token values exceeding `Int.MAX_VALUE`.

**Compatibility impact:** Source-breaking for consumers assigning `Int` values; binary-breaking for data class property type change.

---

---

## SDK-CORE-12 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Model Type Enum
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt
**Lines:** 6-18
**Symbol:** ModelType

**Area:** Model Type Enum  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/ModelType.kt`  
**Lines:** 6-18  
**Symbol:** `ModelType`

**Evidence:**
```kotlin
enum class ModelType(val wireName: String) {
    ALL("all"), TEXT("text"), IMAGE("image"), VIDEO("video"), AUDIO("audio"),
    TTS("tts"), ASR("asr"), EMBEDDING("embedding"), MUSIC("music"),
    UPSCALE("upscale"), INPAINT("inpaint"), CODE("code");
```

**Venice reference:** `swagger.yaml:6055-6067` defines the model `type` response enum as: `asr`, `embedding`, `image`, `music`, `text`, `tts`, `upscale`, `inpaint`, `video`. The `/models?type=` query parameter additionally allows `all` and `code` (`swagger.yaml:8479-8492`).

**Expected:** The SDK enum should distinguish between response-side types and query-only filters.

**Actual:** `CODE` is included as a response-side type, but the swagger `ModelResponse.type` enum does not contain `code`. `ALL` is query-only but included alongside response types.

**Impact:** Consumers may check `model.type == ModelType.CODE` and never match any real model; confusion between query filters and response types.

**Root cause:** Single enum conflates query-parameter values with response values.

**Related occurrences:** `ModelCapabilities.supportsTextChat` treats `"code"` as a valid type.

**Android/Kotlin reference:** Kotlin enum.

**Remediation:** Either remove `CODE` from `ModelType` or document that it is query-only. Consider separate enums for request filters vs. response types.

**Tests required:** Tests verifying `ModelType` round-trips for all swagger response types.

**Compatibility impact:** Removing an enum value is source-breaking.

---

---

## SDK-CORE-13 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Rate Limiting
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** 142-145, 279-287
**Symbol:** parseHttpError`, `extractRateLimitInfo

**Area:** Rate Limiting  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 142-145, 279-287  
**Symbol:** `parseHttpError`, `extractRateLimitInfo`

**Evidence:**
```kotlin
val retryAfter = res.header("retry-after")?.toLongOrNull()
    ?: rateLimitInfo.resetRequestsTimestamp
```
```kotlin
fun Response.extractRateLimitInfo(): RateLimitInfo {
    return RateLimitInfo(
        limitRequests = header("x-ratelimit-limit-requests")?.toLongOrNull(),
        ...
        resetRequestsTimestamp = header("x-ratelimit-reset-requests")?.toLongOrNull(),
        ...
        resetTokensSeconds = header("x-ratelimit-reset-tokens")?.toLongOrNull(),
    )
}
```

**Venice reference:** `swagger.yaml` rate-limit headers are documented in the rate-limiting guide; `x-ratelimit-reset-requests` is an epoch timestamp, `x-ratelimit-reset-tokens` is seconds-until-reset.

**Expected:** `retryAfterSeconds` should represent seconds until retry, not an absolute timestamp.

**Actual:** If `retry-after` is absent, `retryAfterSeconds` falls back to `resetRequestsTimestamp`, which is an epoch timestamp (e.g., `1700000030`), not a duration. The field name `resetTokensSeconds` vs `resetRequestsTimestamp` also implies inconsistent units.

**Impact:** Consumers using `retryAfterSeconds` as a duration will compute wildly incorrect retry times when `retry-after` is missing.

**Root cause:** Fallback from a duration header to an absolute timestamp without conversion.

**Related occurrences:** `RateLimitInfo` data class definition.

**Android/Kotlin reference:** Kotlin `Long?`.

**Remediation:** Compute `retryAfterSeconds` as `max(0, resetRequestsTimestamp - now)` when falling back, or keep separate fields for absolute timestamp and duration.

**Tests required:** Tests with missing `retry-after` but present `x-ratelimit-reset-requests`.

**Compatibility impact:** Behavioral change to `RateLimit.retryAfterSeconds`; may require API addition (`retryAfterTimestamp`).

---

---

## SDK-CORE-14 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Coroutine Cancellation
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt
**Lines:** 94-120
**Symbol:** fetchTraits`, `fetchCompatibility

**Area:** Coroutine Cancellation  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`  
**Lines:** 94-120  
**Symbol:** `fetchTraits`, `fetchCompatibility`

**Evidence:**
```kotlin
catch (_: Exception) {
    return emptyMap()
}
```

**Android/Kotlin reference:** Kotlin `CancellationException` extends `Exception`; coroutine cancellation relies on exceptions propagating.

**Expected:** Coroutine cancellation should propagate to the caller.

**Actual:** `CancellationException` is caught by `catch (_: Exception)` and the function returns `emptyMap()`. The coroutine appears to complete successfully with partial data.

**Impact:** UI/components observing the coroutine cannot distinguish cancellation from success. Partial catalog data may be used as if it were complete.

**Root cause:** Broad exception handler catches cancellation exceptions.

**Related occurrences:** `VeniceForgeSdk.listModels` and feature clients catch `SocketTimeoutException`/`IOException` only, so cancellation propagates there.

**Android/Kotlin reference:** Kotlin coroutines `CancellationException`.

**Remediation:** Catch only `IOException` (and optionally `JsonException`) in network helpers; let `CancellationException` propagate. Alternatively, check `coroutineContext.isActive` before returning empty.

**Tests required:** Test that cancelling `fetchLiveCapabilities` throws `CancellationException` and does not return a catalog.

**Compatibility impact:** Behavioral change; cancellation now propagates as expected.

---

---

## SDK-CORE-15 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Authentication / Wire Conformance
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** 60-61
**Symbol:** listModels

**Area:** Authentication / Wire Conformance  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 60-61  
**Symbol:** `listModels`

**Evidence:**
```kotlin
suspend fun listModels(apiKey: String, type: ModelType? = null): List<VeniceModel> = withContext(Dispatchers.IO) {
    require(apiKey.isNotBlank()) { "apiKey must not be blank" }
```

**Venice reference:** `swagger.yaml:8469-8471` declares `/models` security as both `{}` (anonymous) and `BearerAuth`. The endpoint supports unauthenticated access.

**Expected:** The SDK should allow anonymous model listing, consistent with the API spec.

**Actual:** `require(apiKey.isNotBlank())` rejects empty keys, forcing callers to supply a dummy key for an operation the server permits without auth.

**Impact:** Minor friction and deviation from spec; callers cannot list models without an API key.

**Root cause:** Client-side validation stricter than the API contract.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `require`.

**Remediation:** Remove the blank-key requirement from `listModels`, or add an overload that does not require a key.

**Tests required:** Test that `listModels` succeeds with a blank/null key against a server that allows anonymous access.

**Compatibility impact:** Relaxing validation is source-compatible.

---

---

## SDK-CORE-16 | ---

**Severity:** P2
**Status:** INFERRED
**Area:** Public API Surface / Encapsulation
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** 49-51
**Symbol:** baseUrl()`, `userAgent()`, `httpClient()

**Area:** Public API Surface / Encapsulation  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 49-51  
**Symbol:** `baseUrl()`, `userAgent()`, `httpClient()`

**Evidence:**
```kotlin
fun baseUrl() = _baseUrl
fun userAgent() = config.userAgent
fun httpClient() = httpClient
```

**Expected:** Internal transport dependencies should not be part of the public SDK surface.

**Actual:** These accessors are public, exposing `HttpUrl`, `OkHttpClient`, and config values to any consumer.

**Impact:** Tight coupling between SDK internals and consumers; future changes to transport (e.g., Ktor) become breaking.

**Root cause:** Feature clients need these values; they were exposed publicly instead of keeping transport internal.

**Related occurrences:** `ChatClient.kt`, `ImageClient.kt`, etc., call these accessors.

**Android/Kotlin reference:** Kotlin visibility modifiers.

**Remediation:** Make `baseUrl()` and `httpClient()` `internal`. If external access is needed, expose a narrower abstraction (e.g., a sealed request builder).

**Tests required:** N/A.

**Compatibility impact:** Reducing visibility is source-breaking for external consumers.

---

---

## SDK-CORE-17 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Exception Hierarchy
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt
**Lines:** 57-66
**Symbol:** VeniceSdkException.Http

**Area:** Exception Hierarchy  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkException.kt`  
**Lines:** 57-66  
**Symbol:** `VeniceSdkException.Http`

**Evidence:**
```kotlin
open class Http(
    val statusCode: Int,
    val requestId: String? = null,
    val safeMessage: String? = null,
    val errorCode: String? = null,
) : VeniceSdkException(...)
```

**Expected:** All exception subclasses should be data classes for consistent equality, copy, and destructuring support.

**Actual:** `Http` is an `open class`, not a `data class`. It lacks generated `equals`/`hashCode`/`copy`/`componentN` functions.

**Impact:** Consumers comparing `Http` exceptions by value will get reference equality. The inconsistency complicates the public API contract.

**Root cause:** `Http` was made open to allow extension, but open data classes are not allowed in Kotlin.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin data classes cannot be `open`.

**Remediation:** Make `Http` a `data class` and seal the hierarchy, or provide explicit `equals`/`hashCode`.

**Tests required:** Tests asserting equality of `Http` exceptions with identical fields.

**Compatibility impact:** Making `Http` a data class changes its generated bytecode; may break subclasses if any exist outside the module.

---

---

## SDK-CORE-18 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Transport / Error Handling
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt
**Lines:** 166-200
**Symbol:** parseHttpError

**Area:** Transport / Error Handling  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 166-200  
**Symbol:** `parseHttpError`

**Evidence:**
```kotlin
return when (statusCode) {
    429 -> ...
    401, 403 -> ...
    400, 422 -> ...
    in 500..599 -> ...
    else -> VeniceSdkException.Http(...)
}
```

**Venice reference:** Standard HTTP status codes; 404 is not explicitly handled.

**Expected:** 404 Not Found should be classified distinctly or at least include a clear error code.

**Actual:** 404 falls into the generic `Http` bucket. Consumers must inspect `statusCode` themselves.

**Impact:** Slightly worse UX for missing resources (e.g., unknown model slug in future endpoints).

**Root cause:** `when` branch does not cover 404.

**Related occurrences:** None.

**Android/Kotlin reference:** Kotlin `when`.

**Remediation:** Add a `404 -> VeniceSdkException.NotFound(...)` subclass, or document that 404 maps to `Http`.

**Tests required:** Test that 404 produces a distinguishable exception.

**Compatibility impact:** Adding a sealed subclass requires consumers to handle it in exhaustive `when`; source-breaking for exhaustive checks outside the module.

---

---

## SEC-03 | SecureSecretStore has no unit tests

**Severity:** P2
**Status:** CONFIRMED
**Area:** Credential persistence / Test coverage
**Module:** :core:security
**File:** core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt
**Lines:** 1–100
**Symbol:** SecureSecretStore

**ID:** SEC-03 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Credential persistence / Test coverage | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 1–100  
**Symbol:** `SecureSecretStore`

**Evidence:**
- `core/security/src/**/*.kt` glob returns only the main `SecureSecretStore.kt`.
- `core/security/build.gradle.kts:17` declares `testImplementation(libs.junit)` but no test sources exist.

**Expected:** A security-critical component that manages API keys should have unit tests covering save/load/delete, corruption handling, key-rotation/upgrade scenarios, and Keystore unavailability.

**Actual:** No tests exist for `SecureSecretStore`.

**Impact:** Regressions in Keystore-backed encryption, IV handling, or corruption logic could go undetected. The component is also difficult to validate on Robolectric because Android Keystore is hardware-backed in many test environments.

**Root cause:** `:core:security` module was created without a corresponding test source set.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android Keystore behavior is vendor-specific and best verified with tests.

**Remediation:**
- Add `SecureSecretStoreTest` using a fake `KeyStore` provider or Robolectric + Android Keystore shadow where available.
- Cover: round-trip save/load, delete, tampered ciphertext, wrong IV, blank inputs, and multiple profiles.

**Tests required:** New `SecureSecretStoreTest`.

**Compatibility impact:** None.

---

---

## SEC-04 | SecureSecretStore deletes ciphertext on any decryption failure

**Severity:** P2
**Status:** CONFIRMED
**Area:** Credential persistence / Availability
**Module:** :core:security
**File:** core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt
**Lines:** 38–55
**Symbol:** loadApiKey

**ID:** SEC-04 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Credential persistence / Availability | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 38–55  
**Symbol:** `loadApiKey`

**Evidence:**
- `SecureSecretStore.kt:50–53`:
  ```kotlin
  }.getOrElse {
      // Treat undecryptable/corrupt ciphertext as unavailable and remove it.
      prefs.edit().remove(prefKey(profileId)).apply()
      null
  }
  ```

**Expected:** Decryption failures should return `null` without mutating stored ciphertext, or at minimum require user confirmation before deleting the only copy of an encrypted secret.

**Actual:** Any exception during decryption (corruption, Keystore key invalidation after biometric/lock-screen change, vendor Keystore bug, tampering) silently deletes the stored ciphertext.

**Impact:** A malicious or buggy caller, a flaky Keystore, or a device migration can permanently destroy the user's stored API key with no audit trail and no way to recover. This is a denial-of-availability vulnerability for credentials.

**Root cause:** The recovery path conflates "return null" with "delete stored data".

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android Keystore keys can be invalidated by lock-screen changes, biometric enrollment changes, or device migration; see `KeyGenParameterSpec.Builder.setInvalidatedByBiometricEnrollment` and related semantics.

**Remediation:**
- Remove the `prefs.edit().remove(...)` call from the catch block; return `null` and surface a distinct error to the UI.
- If deletion is desired, expose a separate `deleteApiKey` call and require explicit user action.

**Tests required:**
- Unit test that a decryption failure does not remove the stored preference entry.

**Compatibility impact:** Changes observable behavior: failed loads will no longer erase keys. This is the intended safer behavior.

---

---

## SEC-05 | API key held in Compose managed mutable state

**Severity:** P2
**Status:** CONFIRMED
**Area:** UI / Memory exposure
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt
**Lines:** 49, 59, 95–96, 108
**Symbol:** apiKey` state

**ID:** SEC-05 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** UI / Memory exposure | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt`  
**Lines:** 49, 59, 95–96, 108  
**Symbol:** `apiKey` state

**Evidence:**
- `ConfigScreen.kt:49`: `var apiKey by remember { mutableStateOf("") }`
- `ConfigScreen.kt:59`: `apiKey = existing` (loaded decrypted key copied into state)
- `ConfigScreen.kt:95–96`: `OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, ...)`
- `ConfigScreen.kt:108`: `secureStore.saveApiKey(profileId, apiKey)`

**Expected:** Sensitive credentials should be cleared from memory as soon as they are persisted, and should not be retained in UI state longer than necessary.

**Actual:** The decrypted API key lives in a Compose `mutableStateOf` for the lifetime of the screen composition. It survives configuration changes and is present in the composition's snapshot state.

**Impact:** The key is exposed in memory for the duration of the Settings screen, increasing the window for memory dumps, screenshots, and process-level attacks. It also means the key is present in Compose tooling/state inspection.

**Root cause:** Direct two-way binding of the secret text field to a plain `String` state.

**Related occurrences:** None; other screens use `apiKeyProvider: () -> String?` and do not retain the key in state.

**Venice reference:** N/A.

**Android/Kotlin reference:** Compose `TextFieldValue`/state is held in the composition; see Android security best practice to minimize secret lifetime in memory.

**Remediation:**
- Use a sealed input state (e.g., `Boolean hasKey` + `TextFieldValue` for transient entry) and clear the input buffer immediately after save.
- Load the existing key only to determine whether a key is present, not to populate the text field.

**Tests required:**
- UI test asserting the input buffer is cleared after "Save".

**Compatibility impact:** None; internal UI-state change.

---

---

## SEC-06 | No duplicate submission/idempotency defenses for paid/mutating operations

**Severity:** P2
**Status:** CONFIRMED
**Area:** Paid/mutating operations
**Module:** :app`, `:venice-sdk
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121
**Symbol:** submit`, `generateImage`, `editImage

**ID:** SEC-06 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Paid/mutating operations | **Module:** `:app`, `:venice-sdk`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
**Lines:** `ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121`  
**Symbol:** `submit`, `generateImage`, `editImage`

**Evidence:**
- `ChatViewModel.kt:86–187`: `submit(text)` launches a new coroutine and chat stream without any idempotency key or debounce.
- `ImageViewModel.kt:52–121`: `generateImage()` / `editImage()` immediately call `imageClient.generateBinary(...)` / `imageClient.edit(...)` on every UI trigger.
- `AGENTS.md`: "Paid/mutating operations require explicit approval and duplicate-submission defenses."

**Expected:** Mutating/paid operations (chat completion, image generation, video queue) should include an idempotency key and/or debounce to prevent accidental double billing from rapid taps, configuration changes, or retry logic.

**Actual:** No idempotency keys, no debounce, and no request deduplication. Each tap creates a new paid request.

**Impact:** Users can be billed multiple times for the same operation due to accidental double-tap, process death/recreation, or aggressive retry.

**Root cause:** UI/ViewModel layer was scaffolded without transaction semantics.

**Related occurrences:**
- `VideoClient.kt:19–118` exposes queue/complete/retrieve methods with no idempotency parameter.
- `AudioClient.kt:19–45` exposes `speech` with no idempotency parameter.

**Venice reference:** `swagger.yaml` supports `idempotency_key` style headers for some endpoints; verify per endpoint.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Generate a stable idempotency key per user action (e.g., UUID + action hash) and send it in the `Idempotency-Key` header where Venice supports it.
- Disable action buttons while a request is in flight.

**Tests required:**
- Unit test that rapid double-tap results in exactly one network request.
- Unit test that idempotency key is stable across configuration change.

**Compatibility impact:** May require adding an `idempotencyKey` parameter to SDK methods; additive if defaulted.

---

---

## SEC-07 | No explicit user approval before paid/mutating operations

**Severity:** P2
**Status:** CONFIRMED
**Area:** Paid/mutating operations / UX
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121
**Symbol:** submit`, `generateImage`, `editImage

**ID:** SEC-07 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Paid/mutating operations / UX | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
**Lines:** `ChatViewModel.kt:86–187`, `ImageViewModel.kt:52–121`  
**Symbol:** `submit`, `generateImage`, `editImage`

**Evidence:**
- `ChatViewModel.kt:86–187`: `submit(text)` sends the request immediately after model validation.
- `ImageViewModel.kt:52–121`: `generateImage()` / `editImage()` send requests immediately after input validation.
- `AGENTS.md`: "Paid/mutating operations require explicit approval and duplicate-submission defenses."

**Expected:** Before any operation that consumes API credits (chat, image, audio, video), the user should confirm the action, especially when costs may be high (image generation, video queue).

**Actual:** No confirmation dialog or explicit approval step exists.

**Impact:** Accidental submissions can incur real cost; violates the project's stated approval boundary.

**Root cause:** Feature scaffolding omitted confirmation UX.

**Related occurrences:**
- `FeatureCatalog.kt:39` notes "confirmations must remain explicit for mutating/paid operations" for future Workflows feature, but current Generate/Chat features lack them.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Add a confirmation dialog for image/video/audio generation and for the first chat submission in a session.
- Persist a user preference to skip confirmations for low-cost actions if desired.

**Tests required:**
- UI test that confirmation dialog is shown and can be confirmed/cancelled.

**Compatibility impact:** None; UX addition.

---

---

## SEC-08 | Redactor regex may miss non sk/vn Venice key formats

**Severity:** P2
**Status:** INFERRED
**Area:** Logging / Secret redaction
**Module:** :core:common
**File:** core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt
**Lines:** 10
**Symbol:** apiKey` regex

**ID:** SEC-08 | **Severity:** P2 | **Status:** INFERRED | **Area:** Logging / Secret redaction | **Module:** `:core:common`

**File:** `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt`  
**Lines:** 10  
**Symbol:** `apiKey` regex

**Evidence:**
- `Redactor.kt:10`: `private val apiKey = Regex("(?i)\\b(?:sk|vn)-[A-Za-z0-9._-]{8,}\\b")`
- Venice API documentation and desktop source may use other prefixes or bare token formats.

**Expected:** Redaction patterns should match all documented Venice API key formats.

**Actual:** Only keys beginning with `sk-` or `vn-` are matched. A Venice key using a different prefix or a raw alphanumeric token would not be redacted.

**Impact:** If `Redactor` is ever wired into production logs (see SEC-01), non-matching keys will leak.

**Root cause:** Regex derived from common OpenAI/Venice prefixes without confirming the full Venice key alphabet.

**Related occurrences:** None.

**Venice reference:** Verify against `swagger.yaml` `securitySchemes` and `api-keys` guide.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Confirm the exact Venice API key format(s) from `venice-api-docs` and update the regex.
- Consider redacting the entire `Authorization` header value as a fallback.

**Tests required:**
- Unit tests with representative real Venice key shapes.

**Compatibility impact:** None.

---

---

## SEC-09 | ViewModels surface raw exception messages in UI state

**Severity:** P2
**Status:** CONFIRMED
**Area:** Error handling / UI
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt
**Lines:** ImageViewModel.kt:78,118`, `ChatViewModel.kt:180
**Symbol:** error = e.message

**ID:** SEC-09 | **Severity:** P2 | **Status:** CONFIRMED | **Area:** Error handling / UI | **Module:** `:app`

**File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
**Lines:** `ImageViewModel.kt:78,118`, `ChatViewModel.kt:180`  
**Symbol:** `error = e.message`

**Evidence:**
- `ImageViewModel.kt:78`: `_uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }`
- `ImageViewModel.kt:118`: same pattern.
- `ChatViewModel.kt:180`: `_state.update { it.copy(isStreaming = false, error = chunk.message) }`

**Expected:** Error messages displayed to users should be sanitized and not expose internal details, network paths, or potentially reflected secrets.

**Actual:** Raw exception messages from the SDK/network layer are displayed verbatim in the UI.

**Impact:** If any downstream exception message contains sensitive data (e.g., a proxy error, a reflected header, or a malformed SSE payload per SEC-02), it will be shown to the user and be eligible for screenshots/accessibility logs.

**Root cause:** Direct mapping of `Throwable.message` / `ChatStreamChunk.Error.message` to UI state without sanitization.

**Related occurrences:**
- `ConfigScreen.kt:136`: `status = "Model probe failed: ${it.message ?: it::class.simpleName}"`

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Map exceptions to user-safe, localized error strings.
- If detailed messages are needed for support, route them through `Redactor` and keep them out of UI state.

**Tests required:**
- Unit test that a synthetic exception message containing an API key is not displayed in UI state.

**Compatibility impact:** User-facing error strings change; positive UX improvement.

---

---

## TEST-FIXTURE-11 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Fixture encodes hardcoded trait mapping
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt
**Lines:** 54–100
**Symbol:** catalog.defaultTextModelId`, `catalog.modelForTrait`, `catalog.modelForAlias

**Area:** Fixture encodes hardcoded trait mapping  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt`  
**Lines:** 54–100  
**Symbol:** `catalog.defaultTextModelId`, `catalog.modelForTrait`, `catalog.modelForAlias`  
**Evidence:** The test asserts `catalog.defaultTextModelId == "llama-3.3-70b"` and `modelForTrait("reasoning")?.id == "deepseek-r1"` based on fixture values. This couples trait-resolution logic to specific current model IDs.  
**Expected:** Trait resolution tested structurally (e.g., "default trait returns the model ID the fixture maps to it") without naming real models.  
**Actual:** Test names real models and will fail when the catalog rotates.  
**Impact:** Maintenance burden and false-positive failures when Venice updates default models.  
**Root cause:** Hardcoded fixture values used in assertions.  
**Related occurrences:** `models.json`, `traits.json`, `compatibility.json` fixtures.  
**Venice reference:** AGENTS.md Model Rule.  
**Android/Kotlin reference:** `CapabilitiesRepository.kt:24–92`.  
**Remediation:** Use synthetic model IDs in fixtures and assert structural behavior only (e.g., `modelForTrait("default") == traits.data["default"]`).  
**Tests required:** Refactor `CapabilitiesRepositoryTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-07 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Incomplete image client coverage
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt
**Lines:** 38–104
**Symbol:** ImageClient.generate`, `ImageClient.edit

**Area:** Incomplete image client coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt`  
**Lines:** 38–104  
**Symbol:** `ImageClient.generate`, `ImageClient.edit`  
**Evidence:** Only `generate()` and `edit()` are tested. `ImageClient` also exposes `upscale()`, `multiEdit()`, `generateBinary()` (`ImageClient.kt:22–34`). No HTTP 4xx/5xx, timeout, or malformed JSON paths are tested.  
**Expected:** All public client methods and error paths tested.  
**Actual:** ~29% public-method coverage; no error-path coverage.  
**Impact:** Paid/mutating image operations (upscale, multi-edit, binary generation) can regress silently.  
**Root cause:** Incomplete test suite.  
**Related occurrences:** `ImageClient.kt:19–90`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:7221–7454` (`/image/generate`), `7848–8463` (`/image/edit`), `2897–2929` (`UpscaleImageRequest`), `3031+` (`MultiEditImageRequest`).  
**Android/Kotlin reference:** `ImageClient.kt:19–90`.  
**Remediation:** Add tests for `upscale()`, `multiEdit()`, `generateBinary()`, and error paths (400, 401, 429, 500, timeout, malformed JSON).  
**Tests required:** Expand `ImageClientTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-08 | - HTTP non-200 response path (`ChatClient.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Incomplete chat streaming coverage
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt
**Lines:** 54–219
**Symbol:** ChatClient.streamChat

**Area:** Incomplete chat streaming coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt`  
**Lines:** 54–219  
**Symbol:** `ChatClient.streamChat`  
**Evidence:** Tests cover: good SSE stream, single finish event, multi-tool-call chunk, SSE-side error, cancellation. Missing:
- HTTP non-200 response path (`ChatClient.kt:51–56`)
- Invalid JSON inside an SSE `data:` line (`ChatClient.kt:101–102`)
- Multiple choices (`n > 1`)
- Empty `delta` object
- `[DONE]` appearing after `finish_reason`
- Missing `choices` array (error object path at `ChatClient.kt:106–110`)

**Expected:** Malformed-response and non-stream error cases tested.  
**Actual:** Only in-stream error cases tested.  
**Impact:** SSE parsing edge cases and HTTP error mapping are unverified.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `ChatClient.kt:50–90`, `ChatClient.kt:100–113`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:6188–6869` (`/chat/completions`).  
**Android/Kotlin reference:** `ChatClient.kt:28–152`.  
**Remediation:** Add tests for HTTP 400/500 responses, invalid SSE JSON, multiple choices, and missing `choices`.  
**Tests required:** Expand `ChatClientTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-09 | - Lines that do not start with `data:` (e.

**Severity:** P2
**Status:** CONFIRMED
**Area:** SSE parser edge cases
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt
**Lines:** 9–31
**Symbol:** SseLineParser.nextData

**Area:** SSE parser edge cases  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt`  
**Lines:** 9–31  
**Symbol:** `SseLineParser.nextData`  
**Evidence:** Tests only: comment lines skipped, blank stream returns null. Missing:
- Lines that do not start with `data:` (e.g., `event:`, `id:`)
- Lines with empty `data:` value
- Whitespace-only lines
- Mixed field lines
- Malformed lines that could cause infinite recursion (`SseLineParser.kt:8`)

**Expected:** Parser edge cases tested.  
**Actual:** Minimal coverage.  
**Impact:** Malformed SSE lines could be silently mishandled or cause stack overflow.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `SseLineParser.kt:5–13`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `SseLineParser.kt:5–13`.  
**Remediation:** Add malformed-line tests and verify recursion safety.  
**Tests required:** Expand `SseLineParserTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-10 | - Interleaved text and tool-call deltas - Multiple tool-call indices - `Finish` after `Error` - Empty/null text fragment

**Severity:** P2
**Status:** CONFIRMED
**Area:** Stream accumulator edge cases
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt
**Lines:** 7–45
**Symbol:** ChatStreamAccumulator.apply`, `ChatStreamAccumulator.snapshot

**Area:** Stream accumulator edge cases  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt`  
**Lines:** 7–45  
**Symbol:** `ChatStreamAccumulator.apply`, `ChatStreamAccumulator.snapshot`  
**Evidence:** Tests cover: text accumulation, tool-call fragments across deltas, finish reason, error without clobbering text. Missing:
- Interleaved text and tool-call deltas
- Multiple tool-call indices
- `Finish` after `Error`
- Empty/null text fragments
- Tool-call delta with only `argumentsFragment`

**Expected:** Accumulator edge cases tested.  
**Actual:** Basic coverage only.  
**Impact:** Tool-call reconstruction bugs in complex streams go undetected.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `ChatStreamAccumulator.kt:11–26`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `ChatStreamAccumulator.kt:11–26`.  
**Remediation:** Add interleaved text/tool-call and multi-index tool-call tests.  
**Tests required:** Expand `ChatStreamAccumulatorTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-12 | - `SocketTimeoutException` / `IOException` → `VeniceSdkException.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Incomplete SDK facade coverage
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt
**Lines:** 22–160
**Symbol:** VeniceForgeSdk.listModels`, `VeniceForgeSdk.parseHttpError`, `VeniceForgeSdk.getRaw

**Area:** Incomplete SDK facade coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt`  
**Lines:** 22–160  
**Symbol:** `VeniceForgeSdk.listModels`, `VeniceForgeSdk.parseHttpError`, `VeniceForgeSdk.getRaw`  
**Evidence:** Tests cover URL building for `listModels` and HTTP 429/401/400 errors. Missing:
- `SocketTimeoutException` / `IOException` → `VeniceSdkException.Network` (`VeniceForgeSdk.kt:78–82`, `125–130`)
- HTTP 500/503 server errors
- Actual `/models` response parsing with a realistic fixture
- `getRaw` helper used by `CapabilitiesRepository`
- `ModelType` values other than `IMAGE`

**Expected:** SDK facade error and network paths tested.  
**Actual:** Selective HTTP error coverage only.  
**Impact:** Network and server-error handling untested.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `VeniceForgeSdk.kt:60–138`, `VeniceForgeSdk.kt:140–201`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:8464–8543` (`/models`).  
**Android/Kotlin reference:** `VeniceForgeSdk.kt:60–201`.  
**Remediation:** Add timeout/IO, 500/503, and realistic `/models` parsing tests.  
**Tests required:** Expand `VeniceForgeSdkTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-13 | - `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Incomplete repository coverage
**Module:** :core:data
**File:** - `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` (lines 28–42)
**Lines:** 
**Symbol:** ChatRepository.createConversation`, `ChatRepository.deleteConversation`, `ProfileRepository.findDefault

**Area:** Incomplete repository coverage  
**Module:** `:core:data`  
**Files:**
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` (lines 28–42)
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` (lines 25–37)

**Symbol:** `ChatRepository.createConversation`, `ChatRepository.deleteConversation`, `ProfileRepository.findDefault`  
**Evidence:**
- `ChatRepositoryTest` only tests create/append/observe. It does **not** test `deleteConversation`, transaction failure, `ConversationKind`, or `observeMessages` profile isolation beyond the dedicated `ProfileIsolationTest`.
- `ProfileRepositoryTest` only tests `ensureDefault` idempotency. It does **not** test `findDefault` or non-default profiles.

**Expected:** Repository contract fully tested.  
**Actual:** Partial coverage.  
**Impact:** Data-layer regressions in deletion and profile handling go undetected.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `ChatRepository.kt:19–83`, `ProfileRepository.kt:6–27`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Room repository patterns.  
**Remediation:** Add tests for `deleteConversation`, `ConversationKind`, `findDefault`, and non-default profile creation.  
**Tests required:** Expand existing tests.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-14 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Migration paths not tested
**Module:** :core:data
**File:** core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt
**Lines:** 26–50
**Symbol:** MigrationTest.v1 schema creates all expected tables`, `AppDatabase can open v1

**Area:** Migration paths not tested  
**Module:** `:core:data`  
**File:** `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt`  
**Lines:** 26–50  
**Symbol:** `MigrationTest.v1 schema creates all expected tables`, `AppDatabase can open v1`  
**Evidence:** The two tests create a v1 database and verify table names, then open it with Room. There are no tests that migrate from an earlier schema version to v1, or from v1 to a future version.  
**Expected:** Migration paths between schema versions tested.  
**Actual:** Only schema creation tested.  
**Impact:** Database migration regressions (data loss, missing columns) are not caught.  
**Root cause:** Single-version testing.  
**Related occurrences:** `AppDatabase` migrations.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `MigrationTestHelper` docs.  
**Remediation:** Add migration tests from any prior schema versions to v1, and document expected migration behavior.  
**Tests required:** Expand `MigrationTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-17 | - `safe_mode=true` serialization - Full `ChatRequest` round-trip with `venice_parameters` - Deserialization of response

**Severity:** P2
**Status:** CONFIRMED
**Area:** Venice parameters serialization incomplete
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt
**Lines:** 16–77
**Symbol:** VeniceParameters` serialization

**Area:** Venice parameters serialization incomplete  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt`  
**Lines:** 16–77  
**Symbol:** `VeniceParameters` serialization  
**Evidence:** Tests cover: explicit `safe_mode=false`, omitted fields, deserialization of assistant message with tool calls. Missing:
- `safe_mode=true` serialization
- Full `ChatRequest` round-trip with `venice_parameters`
- Deserialization of response `venice_parameters`
- Invalid enum values for `enable_web_search`

**Expected:** Venice parameters fully tested.  
**Actual:** Partial serialization tests.  
**Impact:** Regressions in `safe_mode` preservation and request shape are possible.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `ChatRequest.kt:67–68`, `VeniceParameters` in `ChatRequest.kt:75–89`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:1464–1543` (`venice_parameters`).  
**Android/Kotlin reference:** `ChatRequest.kt:57–89`.  
**Remediation:** Add round-trip and `safe_mode=true` tests; add invalid enum test.  
**Tests required:** Expand `VeniceParametersSerializationTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-20 | - `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.

**Severity:** P2
**Status:** CONFIRMED
**Area:** No concurrency or lifecycle stress tests
**Module:** :app`, `:venice-sdk
**File:** - `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt
**Lines:** 
**Symbol:** Streaming and repository concurrency

**Area:** No concurrency or lifecycle stress tests  
**Module:** `:app`, `:venice-sdk`  
**Files:**
- `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt`
- `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt`

**Symbol:** Streaming and repository concurrency  
**Evidence:** All tests are sequential. There are no tests for:
- Concurrent stream collection
- Rapid `submit()` / `cancel()` cycles
- Repository operations under concurrent flows
- `ChatViewModel` recreated after process death

**Expected:** Concurrent and lifecycle behavior tested.  
**Actual:** Sequential tests only.  
**Impact:** Race conditions in streaming and persistence are not caught.  
**Root cause:** Missing concurrency tests.  
**Related occurrences:** `ChatClient.kt:28–98`, `ChatViewModel.kt:86–193`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Kotlin coroutines `callbackFlow`, `StateFlow`.  
**Remediation:** Add concurrency and lifecycle stress tests.  
**Tests required:** New tests.  
**Compatibility impact:** Low.

---

## 3. Coverage map: important production behavior → tests

| Production behavior | Production code | Test coverage | Gap |
|---------------------|-----------------|---------------|-----|
| Chat streaming SSE parsing | `ChatClient.kt:28–152` | `ChatClientTest.kt` (partial) | HTTP non-200, malformed SSE, multiple choices |
| Chat stream accumulation | `ChatStreamAccumulator.kt:11–26` | `ChatStreamAccumulatorTest.kt` (partial) | Interleaved text/tool calls, multi-index tool calls |
| SSE line parsing | `SseLineParser.kt:5–13` | `SseLineParserTest.kt` (partial) | Malformed lines, non-`data:` fields |
| Venice chat parameters | `ChatRequest.kt:57–89` | `VeniceParametersSerializationTest.kt` (partial) | `safe_mode=true`, full request round-trip |
| Image generation/edit/upscale/multi-edit/binary | `ImageClient.kt:19–90` | `ImageClientTest.kt` (partial) | `upscale`, `multiEdit`, `generateBinary`, errors |
| Audio TTS | `AudioClient.kt:19–45` | None | All paths missing |
| Video queue/retrieve/complete | `VideoClient.kt:19–63` | None | All paths missing |
| Model discovery & capabilities | `VeniceForgeSdk.kt:60–95`, `CapabilitiesRepository.kt:24–92` | `VeniceForgeSdkTest.kt`, `CapabilitiesRepositoryTest.kt` (partial) | Network errors, realistic `/models` parsing, hardcoded model IDs |
| SDK error mapping | `VeniceForgeSdk.kt:140–201` | `VeniceForgeSdkTest.kt` (partial) | 500/503, timeout/IO exceptions |
| Chat ViewModel lifecycle | `ChatViewModel.kt:86–193` | `ChatViewModelTest.kt` (partial) | Errors, cancellation, missing key/model, tool calls |
| Message persistence | `ChatRepository.kt:19–83` | `ChatRepositoryTest.kt` (partial) | Delete, `ConversationKind`, transaction failure |
| Profile persistence | `ProfileRepository.kt:6–27` | `ProfileRepositoryTest.kt` (partial) | `findDefault`, non-default profiles |
| Database migrations | `AppDatabase` migrations | `MigrationTest.kt` (partial) | Migration paths between versions |
| Diagnostics redaction | `Redactor.kt:8–16` | `RedactorTest.kt` (partial) | Edge cases |
| Feature catalog | `FeatureCatalog.kt:21–47` | `FeatureCatalogTest.kt` (partial) | Feature semantics |

---

## 4. Summary statistics

- **P0:** 0
- **P1:** 7  
  (TEST-FIXTURE-01, TEST-HARDCODE-02, TEST-COVERAGE-03, TEST-COVERAGE-04, TEST-MISSING-06, TEST-MISSING-18, TEST-MISSING-19)
- **P2:** 11  
  (TEST-MOCK-05, TEST-MISSING-07, TEST-MISSING-08, TEST-MISSING-09, TEST-MISSING-10, TEST-FIXTURE-11, TEST-MISSING-12, TEST-MISSING-13, TEST-MISSING-14, TEST-MISSING-17, TEST-MISSING-20)
- **P3:** 2  
  (TEST-MISSING-15, TEST-MISSING-16)

**Most critical gaps:** missing `AudioClient` and `VideoClient` tests, `models.json` fixture inventing swagger-undefined fields, and hardcoded model IDs violating the project Model Rule.

---

## TEST-MOCK-05 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Heavy mocking hides real integration
**Module:** :app
**File:** app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt
**Lines:** 46–56
**Symbol:** RecordingChatClient

**Area:** Heavy mocking hides real integration  
**Module:** `:app`  
**File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`  
**Lines:** 46–56  
**Symbol:** `RecordingChatClient`  
**Evidence:** `ChatViewModelTest` uses a hand-rolled `RecordingChatClient` that subclasses `ChatClient` and overrides `streamChat` to emit a static `Flow`. It never exercises real `ChatClient` SSE parsing, HTTP error handling, cancellation propagation, or `ChatStreamAccumulator` behavior.  
**Expected:** ViewModel tests either use real SDK components with a fake OkHttp stack, or are narrowly scoped to pure ViewModel state.  
**Actual:** The test is an integration-shaped test that replaces the most error-prone collaborator with a stub.  
**Impact:** Bugs in `ChatClient` → `ChatViewModel` integration (e.g., wrong chunk type handling, exception leaks, status mapping) will not be caught here.  
**Root cause:** Test fake is too coarse.  
**Related occurrences:** `ChatClientTest.kt` tests `ChatClient` in isolation.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `ChatViewModel.kt:86–193`; `ChatClient.kt:28–98`.  
**Remediation:** Add at least one integration test using the real `ChatClient` with a fake OkHttp `Interceptor`, or split into small ViewModel state tests that don't touch `ChatClient`.  
**Tests required:** New integration test or refactored unit tests.  
**Compatibility impact:** Low.

---

---

## VID-05 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Video retrieve status not typed
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt
**Lines:** 36, 49
**Symbol:** status

**Area:** Video retrieve status not typed  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt`  
**Lines:** 36, 49  
**Symbol:** `status`

**Evidence:**
```kotlin
data class RetrieveVideoResponseStatus(
    val status: String,
    ...
)
```

**Spec:** `swagger.yaml` `/video/retrieve` JSON response (line 11865) defines `status` enum: `PROCESSING`, `COMPLETED`.

**Expected:** Strongly-typed enum for video retrieve status.

**Actual:** Raw `String`.

**Impact:** Reliability/architecture problem. Callers can pass or receive invalid statuses; unknown statuses are not handled explicitly.

**Root cause:** Schema enum not modeled.

**Related occurrences:** `VideoModels.kt:49` (`Processing.status`).

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11865–11870.

**Android/Kotlin reference:** N/A.

**Remediation:** Introduce `VideoStatus` enum (`PROCESSING`, `COMPLETED`) and use it in response/result models.

**Tests required:** Serialization round-trip tests for each enum value.

**Compatibility impact:** Source-incompatible for callers reading `status` as `String`.

---

---

## VID-06 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** Video complete response ignored
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 22–25
**Symbol:** complete

**Area:** Video complete response ignored  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 22–25  
**Symbol:** `complete`

**Evidence:**
```kotlin
suspend fun complete(apiKey: String, request: CompleteVideoRequest) {
    // We just execute and ignore the response body if successful
    executeRawRequest(apiKey, VeniceEndpoints.VIDEO_COMPLETE, json.encodeToString(CompleteVideoRequest.serializer(), request))
}
```

**Spec:** `swagger.yaml` `/video/complete` 200 response (lines 11484–11504) returns `{ "success": boolean }` and documents: "A success value of false indicates cleanup did not complete and can be retried later."

**Expected:** SDK parses and returns the `success` flag so callers know whether cleanup succeeded.

**Actual:** SDK discards the body and returns `Unit`. A `success: false` response is treated as success.

**Impact:** Reliability problem. Failed cleanup goes unnoticed; media may remain in storage and continue to incur cost.

**Root cause:** `executeRawRequest` intentionally ignores successful response bodies.

**Related occurrences:** Same pattern would affect `/audio/complete` if implemented.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` lines 11484–11504.

**Android/Kotlin reference:** N/A.

**Remediation:** Parse the JSON body and return `Boolean` (or a typed response) from `complete`.

**Tests required:** Mock complete responses with `success: true` and `success: false`.

**Compatibility impact:** Return-type change from `Unit` to `Boolean`; source-incompatible.

---

---

## VID-07 | ---

**Severity:** P2
**Status:** CONFIRMED
**Area:** No queued-job polling helper
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt
**Lines:** 15–118
**Symbol:** VideoClient

**Area:** No queued-job polling helper  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 15–118  
**Symbol:** `VideoClient`

**Evidence:** `VideoClient` exposes only single-shot `retrieve`. There is no method to poll until completion, no timeout, no interval, and no cancellation hook beyond the coroutine.

**Spec:** `swagger.yaml` describes `/video/queue` returning a `queue_id` and `/video/retrieve` returning `PROCESSING`/`COMPLETED` status. The project's `docs/API_INTEGRATION_GUIDE.md` line 47 says: "If queued (e.g. video/music), implement polling state machines."

**Expected:** SDK provides a polling helper (e.g., `pollUntilCompleted`) with configurable max attempts, interval, and timeout, and emits status updates.

**Actual:** No polling helper exists. Each caller must implement its own loop.

**Impact:** Reliability/architecture problem. Inconsistent polling behavior across apps; risk of infinite loops, excessive API calls, and poor UX.

**Root cause:** Queued-job lifecycle abstraction not built.

**Related occurrences:** Same gap applies to audio/music queue if/when implemented.

**Venice reference:** `.source/venice-api-docs/swagger.yaml` `/video/queue`, `/video/retrieve`; `docs/API_INTEGRATION_GUIDE.md` line 47.

**Android/Kotlin reference:** `kotlinx.coroutines` `delay`, `withTimeout`, `Job.isActive`.

**Remediation:** Add a `pollForResult` suspend function with bounded retries, exponential back-off, and cooperative cancellation checks.

**Tests required:** Mock polling sequence (PROCESSING → COMPLETED), timeout path, cancellation path.

**Compatibility impact:** New API surface; additive.

---

---

## VM-08 | Generated image URI is lost on process death; no SavedState restoration.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Image / Lifecycle / Process death
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 19, 31–32
**Symbol:** resultImageUri`, `_uiState

**Generated image URI is lost on process death; no SavedState restoration.**

- **Area:** Image / Lifecycle / Process death  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 19, 31–32  
- **Symbol:** `resultImageUri`, `_uiState`

The result image URI is held only in `ImageUiState` and the backing `MutableStateFlow`. On process death, the URI is lost and the user must regenerate (and repay) the image.

**Evidence:**
```kotlin
// ImageViewModel.kt:15-22
data class ImageUiState(
    ...
    val resultImageUri: Uri? = null,
    ...
)
```

**Expected:** Result URI and in-flight request state survive process death via `SavedStateHandle` or persisted job state.  
**Actual:** Result is transient.  
**Impact:** User loses paid/generated media after process death.  
**Root cause:** No saved-state or durable job persistence.  
**Related occurrences:** `ChatViewModel` conversation/stream state (VM-07).  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `SavedStateHandle`; `rememberSaveable` for Compose UI state.  
**Remediation:** Persist `resultImageUri` and request parameters in `SavedStateHandle`; use WorkManager for generation/edit jobs.  
**Tests required:** Process-death recreation test asserting restored URI.  
**Compatibility impact:** Adds `SavedStateHandle`; behavior change on restore.

---

---

## VM-09 | User cancellation does not mark the assistant message `CANCELLED` in Room.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Chat / Cancellation
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt
**Lines:** 189–193
**Symbol:** cancel()

**User cancellation does not mark the assistant message `CANCELLED` in Room.**

- **Area:** Chat / Cancellation  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`  
- **Lines:** 189–193  
- **Symbol:** `cancel()`

`cancel()` cancels `streamJob` and updates `_state.isStreaming = false`, but it never updates the assistant message's `MessageStatus` in the database. The message remains `PENDING` (if cancelled before first delta) or `STREAMING` (if cancelled mid-stream).

**Evidence:**
```kotlin
// ChatViewModel.kt:189-193
fun cancel() {
    streamJob?.cancel()
    streamJob = null
    _state.update { it.copy(isStreaming = false) }
}
```

**Expected:** On cancellation, the assistant message is updated to `MessageStatus.CANCELLED`.  
**Actual:** Message stays `PENDING`/`STREAMING` in Room.  
**Impact:** Conversation history shows incomplete messages as in-progress forever.  
**Root cause:** `cancel()` only updates UI state, not persistence.  
**Related occurrences:** N/A.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Room entity status semantics.  
**Remediation:** In `cancel()`, call `chatRepo.updateAssistantText(..., status = MessageStatus.CANCELLED)` using the last known assistant ID.  
**Tests required:** Unit test cancelling mid-stream and asserting assistant status is `CANCELLED`.  
**Compatibility impact:** Fixes history accuracy; no breaking change.

---

---

## VM-10 | Image error messages expose raw exception text instead of safe, user-facing messages.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Image / Error truthfulness
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 77–78, 117–118
**Symbol:** generateImage()`, `editImage()

**Image error messages expose raw exception text instead of safe, user-facing messages.**

- **Area:** Image / Error truthfulness  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 77–78, 117–118  
- **Symbol:** `generateImage()`, `editImage()`

Both functions set `error = e.message ?: "Unknown error"`. `VeniceSdkException` messages contain technical detail such as HTTP status and request IDs (see `VeniceSdkException.kt:21,31,42,52,63,75`). While these messages do not leak credentials, they are not user-friendly and may include internal request IDs or stack-trace-like strings for unexpected exceptions.

**Evidence:**
```kotlin
// ImageViewModel.kt:77-78
catch (e: Exception) {
    _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
}
```

**Expected:** Map SDK exceptions to localized, user-facing strings (e.g., "Network error. Please try again.", "Invalid API key.").  
**Actual:** Raw exception messages displayed.  
**Impact:** Poor UX; potential leakage of internal request IDs.  
**Root cause:** Direct use of `Throwable.message` as UI text.  
**Related occurrences:** `ChatViewModel` surfaces stream chunk errors directly (`ChatStreamChunk.Error.message`), which are server-provided and generally safe.  
**Venice reference:** `VeniceSdkException` hierarchy in `venice-sdk`.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Pattern-match `e` to `VeniceSdkException` subtypes and produce localized strings; fall back to a generic message.  
**Tests required:** Unit tests for each `VeniceSdkException` subtype asserting a known user-facing error string.  
**Compatibility impact:** UI text change.

---

---

## VM-11 | No explicit retry logic is good, but billable image operations have no idempotency or duplicate-submission defense.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Image / Billing / Reliability
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 52–121
**Symbol:** generateImage()`, `editImage()

**No explicit retry logic is good, but billable image operations have no idempotency or duplicate-submission defense.**

- **Area:** Image / Billing / Reliability  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 52–121  
- **Symbol:** `generateImage()`, `editImage()`

The ViewModel does not retry failed requests, which is correct for billable endpoints. However, there is also no idempotency key or client-generated request ID passed to Venice, and no local deduplication. A network timeout that the SDK reports as failure may actually have been processed by Venice, so a manual user retry could bill twice for the same prompt.

**Evidence:**
```kotlin
// ImageViewModel.kt:67-74
val req = GenerateImageRequest(
    model = model,
    prompt = prompt,
    height = 512,
    width = 512,
    returnBinary = true
)
val bytes = imageClient.generateBinary(apiKey, req)
```

**Expected:** For paid generation, either (a) use a Venice-supported idempotency mechanism, or (b) persist a pending job and reconcile before retrying.  
**Actual:** Each tap is a new, independent billed request.  
**Impact:** Potential double billing on user retry after ambiguous failures.  
**Root cause:** No request-scoped idempotency token or durable job tracking.  
**Related occurrences:** `ChatViewModel` streaming is less idempotent by nature; image generation is the primary concern.  
**Venice reference:** `swagger.yaml` image request schemas do not expose an idempotency-key field.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Document the limitation; consider a "pending generation" queue with server-side reconciliation if Venice later supports idempotency keys; at minimum, disable the generate button from the moment the request starts until completion/error.  
**Tests required:** N/A (requires Venice API support).  
**Compatibility impact:** None today.

---

---

## VM-12 | `ChatViewModelTest` does not exercise cancellation, errors, rapid submissions, or process-death scenarios.

**Severity:** P2
**Status:** CONFIRMED
**Area:** Chat / Test coverage
**Module:** :app` (test)
**File:** app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt
**Lines:** 58–245
**Symbol:** all test methods
**Also reported as:** TEST-MISSING-06

**`ChatViewModelTest` does not exercise cancellation, errors, rapid submissions, or process-death scenarios.**

- **Area:** Chat / Test coverage  
- **Module:** `:app` (test)  
- **File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`  
- **Lines:** 58–245  
- **Symbol:** all test methods

The existing tests verify happy-path streaming, multi-turn context, and conversation selection. They do not cover:
- Cancelling a stream and asserting `CANCELLED` status.
- A `ChatClient` that throws an exception.
- Two rapid `submit()` calls.
- Process recreation / `SavedStateHandle` behavior.

**Evidence:**
```kotlin
// ChatViewModelTest.kt:58-245
@Test fun `submit writes user message and accumulates assistant chunks`() = runTest { ... }
@Test fun `multi-turn chat constructs request with complete prior conversation context`() = runTest { ... }
@Test fun `init picks most recent existing conversation instead of creating new one`() = runTest { ... }
```

**Expected:** Tests for failure modes and concurrency guards.  
**Actual:** Only happy-path coverage.  
**Impact:** Regressions in cancellation, error handling, and duplicate submission are likely to go undetected.  
**Root cause:** Test plan focused on success paths.  
**Related occurrences:** No `ImageViewModelTest` exists at all.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `kotlinx-coroutines-test` documentation.  
**Remediation:** Add tests for VM-01, VM-02, VM-09, and VM-07. Create `ImageViewModelTest` covering VM-03, VM-04, VM-10.  
**Tests required:** See individual findings.  
**Compatibility impact:** N/A.

---

---

## VM-13 | `VeniceForgeApp` holds `profileId` in non-saved `mutableStateOf`, causing ViewModels to be null briefly after process death.

**Severity:** P2
**Status:** CONFIRMED
**Area:** App wiring / Process death
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt
**Lines:** 83–96, 122–135
**Symbol:** profileId`, `chatViewModel`, `imageViewModel

**`VeniceForgeApp` holds `profileId` in non-saved `mutableStateOf`, causing ViewModels to be null briefly after process death.**

- **Area:** App wiring / Process death  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt`  
- **Lines:** 83–96, 122–135  
- **Symbol:** `profileId`, `chatViewModel`, `imageViewModel`

```kotlin
var profileId by remember { mutableStateOf<String?>(null) }
LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }
```

`profileId` is not `rememberSaveable`. After process death and restoration, `profileId` is `null` until `profileRepo.ensureDefault()` completes. During that window `chatViewModel` and `imageViewModel` are `null`, so the UI shows the "No API key loaded" placeholder even if a key exists. The actual data is safe in Room/Keystore, but the transient UX is degraded.

**Expected:** `profileId` survives process death via `rememberSaveable` or is restored from a durable source before first composition.  
**Actual:** Brief null-profile flash after process death.  
**Impact:** UI flicker / misleading "no API key" message.  
**Root cause:** Compose state not saved across process death.  
**Related occurrences:** VM-07, VM-08.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `rememberSaveable` docs.  
**Remediation:** Change to `rememberSaveable { mutableStateOf<String?>(null) }` or read the default profile ID synchronously from a saved-state source.  
**Tests required:** Process-death recreation test asserting ViewModels are non-null immediately after restore.  
**Compatibility impact:** None.

---

---

## VM-14 | Image generation/edit requests omit `safe_mode`, defaulting to the API default of `true`.

**Severity:** P2
**Status:** INFERRED
**Area:** Image / Venice API semantics
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt
**Lines:** 67–73, 105–109
**Symbol:** GenerateImageRequest`, `EditImageRequest

**Image generation/edit requests omit `safe_mode`, defaulting to the API default of `true`.**

- **Area:** Image / Venice API semantics  
- **Module:** `:app`  
- **File:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`  
- **Lines:** 67–73, 105–109  
- **Symbol:** `GenerateImageRequest`, `EditImageRequest`

`GenerateImageRequest` and `EditImageRequest` are constructed without `safeMode`. Per `swagger.yaml`:

> `safe_mode`: type boolean, default true, description "Whether to use safe mode. If enabled, this will blur images that are classified as having adult content." (`swagger.yaml:2666-2671`, `3017-3022`, `3108-3113`, `3206-3211`)

Project `AGENTS.md` states: "Preserve explicit `safe_mode=false` when selected." There is currently no UI or ViewModel state for `safe_mode`, so the effective value is the API default `true`.

**Expected:** Either explicit UI control for `safe_mode` or an explicit default chosen by the app and passed in requests.  
**Actual:** `safe_mode` omitted; API default `true` applied.  
**Impact:** Unexpected content filtering/blurring for a Venice client; violates project rule if a future setting is ignored.  
**Root cause:** No `safe_mode` field in UI or ViewModel.  
**Related occurrences:** `ChatRequest.VeniceParameters.safeMode` is also not set, but chat `safe_mode` semantics differ and are not covered by the same AGENTS rule.  
**Venice reference:** `swagger.yaml` image request `safe_mode` fields.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Add `safeMode` to `ImageUiState`, expose a toggle, and pass it to requests.  
**Tests required:** Unit test asserting `safeMode` is serialized when set.  
**Compatibility impact:** Behavior change if default is switched to `false`.

---

---
