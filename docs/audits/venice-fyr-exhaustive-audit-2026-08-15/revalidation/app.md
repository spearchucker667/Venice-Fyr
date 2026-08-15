# APP / UI / ViewModel Revalidation

**Revalidation scope:** `findings/app-ui.md`, `findings/architecture.md`, and the (non-existent) `app-viewmodels.md` from the 2026-08-15 exhaustive audit.
**Source tree:** `/Users/super_user/Projects/Venice Fyr` @ `ee2cd7a` with the coordinator’s one-line compile fix applied (`import io.github.spearchucker667.veniceforge.sdk.image.ImageClient` in `VeniceForgeSdk.kt`).
**Methodology:** Static source review only; no Gradle commands executed per agent rules.

---

## Missing File Note

`docs/audits/venice-fyr-exhaustive-audit-2026-08-15/findings/app-viewmodels.md` does **not exist** in the audit directory. The ViewModel findings are covered inside `findings/architecture.md` (ARCH-01, ARCH-02, ARCH-04, ARCH-05, ARCH-11, ARCH-12) and `findings/app-ui.md` (APP-UI-001). No separate `VM-01..VM-09` P1 entries were found.

---

## Disposition Summary

| ID | Original Severity | Disposition | Corrected Severity |
|---|---|---|---|
| APP-UI-001 | P1 CONFIRMED | **VALID** | P1 |
| APP-UI-002 | P1 CONFIRMED | **VALID** | P1 |
| APP-UI-003 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-004 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-005 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-006 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-007 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-008 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-009 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-010 | P2 CONFIRMED | **VALID** | P2 |
| APP-UI-011 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-012 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-013 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-014 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-015 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-016 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-017 | P3 INFERRED | **PARTIALLY_VALID** | P3 |
| APP-UI-018 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-019 | P3 CONFIRMED | **VALID** | P3 |
| APP-UI-020 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-01 | P1 CONFIRMED | **VALID** | P1 |
| ARCH-02 | P1 CONFIRMED | **FALSE** | — |
| ARCH-03 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-04 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-05 | P1 CONFIRMED | **VALID** | P1 |
| ARCH-06 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-07 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-08 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-09 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-10 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-11 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-12 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-13 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-14 | P3 CONFIRMED | **VALID** | P3 |
| ARCH-15 | P3 CONFIRMED | **VALID** | P3 |
| ARCH-16 | P2 CONFIRMED | **VALID** | P2 |
| ARCH-17 | P2 CONFIRMED | **VALID** | P2 |

---

## Detailed Findings

### APP-UI-001 | P1 CONFIRMED → **VALID** | P1

**Source evidence:**
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:86-96`
  ```kotlin
  val chatViewModel = remember(profileId) {
      profileId?.let { pid ->
          ChatViewModel(...)
      }
  }
  ```
- `VeniceForgeApp.kt:98-119` constructs `ImageViewModel` the same way.
- `ChatViewModel.kt:46` and `ImageViewModel.kt:24` both extend `androidx.lifecycle.ViewModel`.
- `MainActivity.kt:9-16` never uses `ViewModelProvider` or `viewModel()`.

**Why original is correct:** `remember(profileId)` creates a plain composable-scoped object. The `ViewModel` instances are not registered with a `ViewModelStoreOwner`, so they are not retained across configuration changes and `onCleared()` is never called. On rotation the old instances are dropped while their `viewModelScope` coroutines may continue until the Activity is destroyed.

**Correct remediation:** Obtain ViewModels via `androidx.lifecycle.viewmodel.compose.viewModel()` with a custom `ViewModelProvider.Factory` keyed by `profileId`, or hoist them to a lifecycle-aware owner.

**Tests required:** Rotation/config-change instrumentation test verifying `ChatViewModel`/`ImageViewModel` survive and `onCleared()` is called exactly once when the Activity finishes.

---

### APP-UI-002 | P1 CONFIRMED → **VALID** | P1

**Source evidence:**
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:47`
  ```kotlin
  fun byId(id: String): AppFeature = all.first { it.id == id }
  ```
- `VeniceForgeApp.kt:68-69`
  ```kotlin
  var selectedId by rememberSaveable { mutableStateOf("chat") }
  val selected = remember(selectedId) { FeatureCatalog.byId(selectedId) ?: FeatureCatalog.byId("chat") }
  ```

**Why original is correct:** `Iterable.first(predicate)` throws `NoSuchElementException` when no element matches. The Elvis fallback `?: FeatureCatalog.byId("chat")` is therefore unreachable dead code. If `rememberSaveable` restores an invalid `selectedId`, the app crashes.

**Correct remediation:** Change `byId` to return `AppFeature?` using `firstOrNull { it.id == id } ?: all.first { it.id == "chat" }`.

**Tests required:** Unit test asserting `byId("unknown")` returns null; instrumentation test restoring an invalid `selectedId` falls back to "chat".

---

### ARCH-01 | P1 CONFIRMED → **VALID** | P1

**Source evidence:** Same as APP-UI-001 (`VeniceForgeApp.kt:86-120`, `MainActivity.kt:9-16`).

**Why original is correct:** The ViewModels are instantiated directly inside `@Composable` with `remember`, bypassing `ViewModelProvider`. They will not survive configuration changes and will not receive proper `onCleared()` callbacks.

**Correct remediation:** Use `viewModel()` with a custom factory, or adopt Hilt/dependency injection scoped to the Activity/destination.

**Tests required:** Rotation/manual config-change test; leak detection; process-death recovery test.

---

### ARCH-02 | P1 CONFIRMED → **FALSE** | —

**Source evidence:**
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt:98-142` (current line numbers)
  ```kotlin
  val userMsg = MessageEntity(...)
  val assistantMsg = MessageEntity(...)

  viewModelScope.launch {
      // Load prior conversation history to construct multi-turn request context
      val priorMessages = chatRepo.observeMessages(profileId, convId).first()
      val contextMessages = priorMessages
          .filter { it.status == MessageStatus.COMPLETED && it.textContent.isNotBlank() }
          .map { entity -> ... }
          .plus(ChatMessage.user(text))

      chatRepo.appendMessage(profileId, convId, userMsg)
      chatRepo.appendMessage(profileId, convId, assistantMsg)
      ...
  }
  ```
- `ChatViewModelTest.kt:172-178`
  ```kotlin
  assertEquals(3, req2.messages.size)
  assertEquals("user", req2.messages[0].role)
  assertEquals("Turn one", req2.messages[0].content)
  assertEquals("assistant", req2.messages[1].role)
  assertEquals("First answer", req2.messages[1].content)
  assertEquals("user", req2.messages[2].role)
  assertEquals("Turn two", req2.messages[2].content)
  ```

**Why original is wrong:** The new `userMsg` and `assistantMsg` are constructed but **not appended** until **after** `priorMessages` is loaded and `contextMessages` is built. Therefore `priorMessages` does not yet contain the new user turn, and `.plus(ChatMessage.user(text))` adds it exactly once. The request for turn 2 contains exactly three messages as the test asserts.

**Correct remediation:** None required; current behavior is correct. Keep the existing `ChatViewModelTest` multi-turn test as regression coverage.

**Tests required:** Existing `ChatViewModelTest.kt:112-182` already verifies the correct behavior.

---

### ARCH-05 | P1 CONFIRMED → **VALID** | P1

**Source evidence:**
- `AGENTS.md` non-negotiable boundary: “Paid/mutating operations require explicit approval and duplicate-submission defenses.”
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt:112-117`
  ```kotlin
  TextButton(
      enabled = input.isNotBlank() && !state.isStreaming && !state.modelId.isNullOrBlank(),
      onClick = {
          viewModel.submit(input)
          input = ""
      },
  )
  ```
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt:145-159`
  ```kotlin
  Button(onClick = { viewModel.generateImage() }, ...)
  Button(onClick = { viewModel.editImage() }, ...)
  ```
- `ChatViewModel.kt:86-187` and `ImageViewModel.kt:52-81` contain no approval gate or idempotency key.

**Why original is correct:** A single tap initiates a billed `/chat/completions`, `/image/generate`, or `/image/edit` request with no confirmation step and no idempotency token. This violates the explicit-approval boundary in `AGENTS.md`.

**Correct remediation:** Introduce an approval coordinator that gates paid/mutating operations; attach idempotency keys to mutating requests where the Venice API supports them.

**Tests required:** UI tests verifying the approval flow; unit tests for idempotency key generation.

---

### APP-UI-003 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `ChatScreen.kt:39`: `val state by viewModel.state.collectAsState()`
- `ImageScreen.kt:47`: `val state by viewModel.uiState.collectAsState()`
- `app/build.gradle.kts:48`: `implementation(libs.androidx.lifecycle.runtime.compose)` provides `collectAsStateWithLifecycle`.

**Correct remediation:** Replace `collectAsState()` with `collectAsStateWithLifecycle()` in both screens.

**Tests required:** Lifecycle transition test verifying collection pauses when the composable is not at least `STARTED`.

---

### APP-UI-004 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `ConfigScreen.kt:49-53`
  ```kotlin
  var apiKey by remember { mutableStateOf("") }
  var status by remember { mutableStateOf("No API key loaded") }
  var loading by remember { mutableStateOf(false) }
  var hasError by remember { mutableStateOf(false) }
  var models by remember { mutableStateOf<List<VeniceModel>>(emptyList()) }
  ```

**Correct remediation:** Move state into a `ConfigViewModel` obtained via `viewModel()`, or use `rememberSaveable` for serializable primitives.

---

### APP-UI-005 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `ChatScreen.kt:40-41`
  ```kotlin
  var input by remember { mutableStateOf("") }
  var modelMenuOpen by remember { mutableStateOf(false) }
  ```

**Correct remediation:** Change `input` and `modelMenuOpen` to `rememberSaveable`.

---

### APP-UI-006 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `MainActivity.kt:12`
  ```kotlin
  VeniceForgeTheme(darkTheme = true) { ... }
  ```
- `VeniceForgeTheme.kt:12` accepts `darkTheme: Boolean = isSystemInDarkTheme()`.

**Correct remediation:** Remove `darkTheme = true` and let `VeniceForgeTheme` default to `isSystemInDarkTheme()`, or persist a manual toggle.

---

### APP-UI-007 / ARCH-08 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `ImageScreen.kt:180-186`
  ```kotlin
  val decodedBitmap = remember(state.resultImageUri) {
      try {
          state.resultImageUri.path?.let { BitmapFactory.decodeFile(it) }
      } catch (e: Exception) { null }
  }
  ```

**Correct remediation:** Decode off the main thread using Coil `AsyncImage`, `produceState`, or `LaunchedEffect(Dispatchers.IO)`.

---

### APP-UI-008 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `CodexPet.kt:9`: `import androidx.compose.runtime.DisposableEffect` (unused).
- `CodexPet.kt:81-87`
  ```kotlin
  val imageBitmap: ImageBitmap? = remember(spritesheetRes) {
      ...
      androidBitmap?.asImageBitmap()
  }
  ```

**Correct remediation:** Wrap the `ImageBitmap` in `DisposableEffect(spritesheetRes) { onDispose { imageBitmap?.asAndroidBitmap()?.recycle() } }`.

---

### APP-UI-009 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `VeniceForgeApp.kt:83-84`
  ```kotlin
  var profileId by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }
  ```
- `VeniceForgeApp.kt:209-219` and `229-232` show `R.string.chat_no_api_key` when `chatViewModel`/`imageViewModel` are null.

**Correct remediation:** Add a loading state and display a loading indicator until `profileId` and key availability are resolved.

---

### APP-UI-010 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `FeatureCatalog.kt:21-45` defines 22 features, 20 marked `SCAFFOLDED`.
- `VeniceForgeApp.kt:234-265` routes every unknown `selectedId` to `FeatureScreen`, which only shows a static placeholder page.

**Correct remediation:** Hide/disable `SCAFFOLDED` features in release builds, add a “Coming soon” badge, or keep them debug-only.

---

### APP-UI-011 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `ImageScreen.kt:72, 85, 110, 129, 136, 150, 158, 178, 195` contain hard-coded English strings (e.g., `"Image Studio"`, `"Model"`, `"Prompt"`, `"Generate"`, `"Edit"`).

**Correct remediation:** Extract all literals to `res/values/strings.xml`.

---

### APP-UI-012 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `ConfigScreen.kt:50, 84, 86, 91, 98, 112, 121, 128, 130, 132, 136, 138, 142, 162, 165, 168` contain hard-coded English strings.

**Correct remediation:** Extract all literals to `res/values/strings.xml`.

---

### APP-UI-013 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `VeniceForgeApp.kt:164, 171, 196, 249, 252, 256, 260-263` contain hard-coded strings.

**Correct remediation:** Extract all literals to `res/values/strings.xml`.

---

### APP-UI-014 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `CodexPet.kt:9` imports `DisposableEffect` but never uses it.

**Correct remediation:** Use `DisposableEffect` to dispose the bitmap (APP-UI-008) or remove the import.

---

### APP-UI-015 | P3 CONFIRMED → **VALID** | P3

**Source evidence:**
- `VeniceForgeApp.kt:159`: `contentDescription = "Official Venice crossed keys"`
- `ConfigScreen.kt:80`: same string.
- `ImageScreen.kt:191`: `contentDescription = "Result Image"`
- `VeniceLoadingIndicator.kt:42`: `contentDescription = message ?: "Loading indicator"`
- `VeniceLoadingIndicator.kt:76`: `contentDescription = message ?: "Status indicator: ${state.name}"`

**Correct remediation:** Move descriptions to `strings.xml`; derive generated-image descriptions from prompt/model where feasible.

---

### APP-UI-016 | P3 CONFIRMED → **VALID** | P3

**Source evidence:**
- `VeniceForgeApp.kt:127` calls `capabilitiesRepo.fetchLiveCapabilities(key)`.
- `ConfigScreen.kt:129` calls `sdk.listModels(apiKey)` directly.

**Correct remediation:** Inject `CapabilitiesRepository` into `ConfigScreen` and call `fetchLiveCapabilities`.

---

### APP-UI-017 | P3 INFERRED → **PARTIALLY_VALID** | P3

**Source evidence:**
- `VeniceForgeApp.kt:193-197`
  ```kotlin
  OutlinedButton(
      onClick = { scope.launch { drawerState.open() } },
      modifier = Modifier.padding(start = 8.dp),
  ) { Text("Menu") }
  ```

**Why partially valid:** Material3 `OutlinedButton` default minimum height is 40dp; the width is text-driven and may fall below 48dp on some configurations. The concern is plausible but unverified at runtime. Applying `Modifier.minimumInteractiveComponentSize()` is a safe, low-risk improvement.

**Correct remediation:** Apply `.minimumInteractiveComponentSize()` or wrap the button in a 48dp box.

---

### APP-UI-018 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `VeniceForgeTheme.kt:21-24` invokes `MaterialTheme` without `shapes`.

**Correct remediation:** Add a `VeniceShapes` object and pass it to `MaterialTheme(shapes = VeniceShapes.Shapes)`.

---

### APP-UI-019 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `VeniceForgeApp.kt:113-115`
  ```kotlin
  val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
  file.writeBytes(bytes)
  android.net.Uri.fromFile(file)
  ```

**Correct remediation:** Migrate to `FileProvider` content URIs, or document and encapsulate internal `file://` usage.

---

### APP-UI-020 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `ImageScreen.kt:76`
  ```kotlin
  var expanded by remember { mutableStateOf(false) }
  ```

**Correct remediation:** Change to `rememberSaveable { mutableStateOf(false) }`.

---

### ARCH-03 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `DataServices.kt:18-20`: `fun create(context: Context): DataServices = DataServices(AppDatabase.create(context.applicationContext))`
- `AppDatabase.kt:36-43`: `fun create(context: Context): AppDatabase = Room.databaseBuilder(...).build()`
- `VeniceForgeApp.kt:77`: `val data = remember { DataServices.create(context) }`

**Correct remediation:** Initialize `DataServices` once in `Application.onCreate` or expose it via a DI singleton.

---

### ARCH-04 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `ChatViewModel.kt:150-162`: `isStreaming` is set to `true` only after the first SSE `Delta`/`ToolCallDelta` chunk.
- `ChatScreen.kt:112-117`: Send button is enabled while `!state.isStreaming`.

**Correct remediation:** Set `isStreaming = true` at the top of `submit()` before launching the network call, and/or guard with an atomic flag.

---

### ARCH-06 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `app/build.gradle.kts:51`: `implementation(libs.androidx.work.runtime)`; no `androidx.work` imports in `app/src/main/java`.

**Correct remediation:** Implement `Worker` classes for queued generation jobs or remove the dependency until needed.

---

### ARCH-07 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `app/build.gradle.kts:50,52`: `implementation(libs.androidx.datastore.preferences)` and `implementation(libs.androidx.media3.exoplayer)`; no production imports of either in `app/src/main/java`.

**Correct remediation:** Implement the planned preferences/playback features or remove the unused dependencies.

---

### ARCH-09 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `VeniceForgeApp.kt:111-117` / `ImageViewModel.kt` cache helper writes timestamped files to `context.cacheDir` with no deletion or LRU policy.

**Correct remediation:** Implement a content-addressed cache with max-size eviction; store metadata in Room.

---

### ARCH-10 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `core/common/src/main/java/io/github/spearchucker667/veniceforge/core/common/Redactor.kt` exists but is only used in its own unit test.

**Correct remediation:** Route all log/diagnostic output through `Redactor.redact()` before emission.

---

### ARCH-11 | P2 CONFIRMED → **VALID** | P2

**Source evidence:**
- `MainActivity.kt:9-16` does not read `savedInstanceState`.
- `VeniceForgeApp.kt:68` only saves `selectedId` via `rememberSaveable`.
- `ChatViewModel`/`ImageViewModel` do not accept `SavedStateHandle`.

**Correct remediation:** Pass `SavedStateHandle` to ViewModels; persist lightweight UI state; reconcile DB message statuses on startup.

---

### ARCH-12 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `ChatViewModel.kt:59-67` recomputes `conversationId` from the most recently updated conversation in `init`.

**Correct remediation:** Persist the active conversation ID in `SavedStateHandle` or DataStore and restore it in `init`.

---

### ARCH-13 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `ImageViewModel.kt:97-120` runs `editImage` in `viewModelScope.launch` (main dispatcher); base64 decode at line 113 and `saveBytesToCache` at line 114 execute on the main thread (only `saveBytesToCache` internally dispatches to IO, but the decode does not).

**Correct remediation:** Wrap the entire decode/write block in `withContext(Dispatchers.IO)`.

---

### ARCH-14 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** Same as APP-UI-002 (`FeatureCatalog.kt:47`).

**Correct remediation:** Change to `firstOrNull { it.id == id } ?: all.first { it.id == "chat" }`.

---

### ARCH-15 | P3 CONFIRMED → **VALID** | P3

**Source evidence:** `ConfigScreen.kt:47`: `val profileId = "default"`.

**Correct remediation:** Inject current profile from repository; use DataStore for UI preferences.

---

### ARCH-16 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt:100-151` only parses `choices`; `usage` events are ignored.

**Correct remediation:** Parse `usage` events and emit a `ChatStreamChunk.Finish` with populated `Usage`, or add a dedicated `Usage` chunk type.

---

### ARCH-17 | P2 CONFIRMED → **VALID** | P2

**Source evidence:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt:94-99` and `108-113` catch all exceptions and return empty maps.

**Correct remediation:** Distinguish network/auth errors from parse errors; expose a sealed result type or rethrow typed exceptions.

---

## Key Evidence Quotes (max 12 lines)

1. `VeniceForgeApp.kt:86-96` — `chatViewModel` created with `remember(profileId)`, not `viewModel()`.
2. `FeatureCatalog.kt:47` — `fun byId(id: String): AppFeature = all.first { it.id == id }` throws on unknown IDs.
3. `ChatViewModel.kt:127-142` — `priorMessages` loaded **before** `appendMessage(userMsg)`, proving ARCH-02 is false.
4. `ChatScreen.kt:112-117` — Send button directly calls `viewModel.submit(input)` with no approval gate.
5. `ImageScreen.kt:145-159` — Generate/Edit buttons directly call paid operations.
6. `MainActivity.kt:12` — `VeniceForgeTheme(darkTheme = true)` hard-codes dark mode.
7. `ImageScreen.kt:180-186` — `BitmapFactory.decodeFile` runs synchronously in `remember`.
8. `CodexPet.kt:81-87` — `ImageBitmap` created but never disposed.
9. `VeniceForgeApp.kt:83-84` — `profileId` starts null; UI shows “No API key saved” during load.
10. `ConfigScreen.kt:49-53` — `remember` used for state that should survive config changes.
11. `ChatViewModel.kt:150-162` — `isStreaming` set only after first SSE chunk arrives.
12. `AGENTS.md` — “Paid/mutating operations require explicit approval and duplicate-submission defenses.”
