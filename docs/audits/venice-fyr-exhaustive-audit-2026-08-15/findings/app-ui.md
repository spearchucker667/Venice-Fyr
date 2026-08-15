# APP Compose UI Audit — Venice Fyr Android

**Auditor scope:** `app/src/main/java/io/github/spearchucker667/veniceforge/android/` (MainActivity.kt, VeniceForgeApp.kt, chat/ChatScreen.kt, image/ImageScreen.kt, ui/ConfigScreen.kt, feature/FeatureCatalog.kt), `core/designsystem/` source/resources, app resources (`values/`, `xml/network_security_config.xml`, drawables), `app/src/main/AndroidManifest.xml`.

**Audit focus:** state hoisting, recomposition/remember/rememberSaveable, LaunchedEffect/DisposableEffect keys, `collectAsStateWithLifecycle`, controls/handlers, unreachable/misleading UI, loading states, stale state, accessibility, hard-coded strings, landscape/overflow/font scaling, theme/contrast, FeatureCatalog truthfulness.

**Methodology:** Static source review only; no Gradle commands executed. Cross-referenced against the in-scope Kotlin/XML files and the ViewModel contracts (`ChatViewModel.kt`, `ImageViewModel.kt`) for UI-wiring verification.

---

## File Ledger

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/MainActivity.kt` | 17 | Y | 1 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt` | 266 | Y | 5 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt` | 123 | Y | 2 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt` | 200 | Y | 4 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` | 173 | Y | 3 |
| `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt` | 48 | Y | 1 |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/CodexPet.kt` | 128 | Y | 2 |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceColors.kt` | 93 | Y | 0 |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceForgeTheme.kt` | 26 | Y | 1 |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceLoadingIndicator.kt` | 86 | Y | 0 |
| `core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceTypography.kt` | 129 | Y | 0 |
| `core/designsystem/build.gradle.kts` | 24 | Y | 0 |
| `core/designsystem/src/main/AndroidManifest.xml` | 1 | Y | 0 |
| `app/src/main/AndroidManifest.xml` | 23 | Y | 0 |
| `app/src/main/res/values/strings.xml` | 14 | Y | 0 |
| `app/src/main/res/values/themes.xml` | 8 | Y | 1 |
| `app/src/main/res/values-v27/themes.xml` | 5 | Y | 0 |
| `app/src/main/res/xml/network_security_config.xml` | 3 | Y | 0 |
| `app/src/main/res/drawable/ic_venice_keys_deep_blue.xml` | 9 | Y | 0 |
| `app/src/main/res/drawable/ic_venice_keys_off_white.xml` | 9 | Y | 0 |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 7 | Y | 0 |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 15 | Y | 0 |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 12 | Y | 0 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 6 | Y | 0 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 6 | Y | 0 |
| `app/src/main/res/mipmap-*/ic_launcher*.png` (binary assets) | — | Y (visual review) | 0 |

**Total scoped files reviewed:** 25 source/resource files (~1,431 lines of text/XML).

---

## Findings

### APP-UI-001 | Severity: P1 | Status: CONFIRMED
**Area:** Lifecycle / Architecture | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Lines:** 86–120 | **Symbol:** `chatViewModel`, `imageViewModel`

**Evidence:**
```kotlin
val chatViewModel = remember(profileId) {
    profileId?.let { pid ->
        ChatViewModel(...)
    }
}
val imageViewModel = remember(profileId) {
    profileId?.let { pid ->
        ImageViewModel(...)
    }
}
```
`ChatViewModel` and `ImageViewModel` extend `androidx.lifecycle.ViewModel` (see `ChatViewModel.kt:46`, `ImageViewModel.kt:24`) and launch coroutines in `viewModelScope`. They are instantiated with plain `remember`, not `viewModel()` or a `ViewModelProvider`.

**Expected:** ViewModels are obtained via `viewModel()` (or `ViewModelProvider`) so they are scoped to a `ViewModelStoreOwner`, survive configuration changes, and are cleared when the owner is destroyed.

**Actual:** `remember` does not survive configuration changes and does not invoke `ViewModel.onCleared()`. On rotation, the old ViewModel instances are dropped but keep running; new instances are created for the new composition. This leaks the old ViewModels and their active streaming coroutines, and all UI state held in the ViewModel is lost across config changes.

**Impact:** Streaming chat/image operations can leak across rotation; user loses conversation state, model selection, and generation results on device rotation.

**Root cause:** ViewModels are treated as plain `@Composable remember` objects instead of lifecycle-aware components.

**Related occurrences:** `ChatScreen.kt:35` receives the leaked/recreated VM; `ImageScreen.kt:42` receives the leaked/recreated VM.

**Venice reference:** N/A (Android lifecycle contract).

**Android/Kotlin reference:** `ViewModel` is designed to be retained by a `ViewModelStore`; `remember` values are destroyed with the composition. See Android docs: "A ViewModel is always created in association with a scope... and stays in memory until the scope is permanently gone."

**Remediation:** Obtain ViewModels via `viewModel()` (or `ViewModelProvider`) keyed by `profileId`, or hoist them to a `ViewModelStoreOwner` (e.g., activity/destination). If a custom factory is needed, pass it to `viewModel(factory = ...)`.

**Tests required:** Rotation/config-change instrumentation test verifying that `ChatViewModel`/`ImageViewModel` survive and `onCleared()` is called exactly once when the Activity finishes.

**Compatibility impact:** Fixing this changes ViewModel lifecycle semantics; any code relying on re-creation on rotation will break.

---

### APP-UI-002 | Severity: P1 | Status: CONFIRMED
**Area:** Navigation / Crash | **Module:** `:app` | **File:** `FeatureCatalog.kt` / `VeniceForgeApp.kt` | **Lines:** `FeatureCatalog.kt:47`, `VeniceForgeApp.kt:68–69` | **Symbol:** `FeatureCatalog.byId`, `selectedId`

**Evidence:**
```kotlin
// FeatureCatalog.kt:47
fun byId(id: String): AppFeature = all.first { it.id == id }

// VeniceForgeApp.kt:68–69
var selectedId by rememberSaveable { mutableStateOf("chat") }
val selected = remember(selectedId) { FeatureCatalog.byId(selectedId) ?: FeatureCatalog.byId("chat") }
```

**Expected:** `byId` should return a nullable `AppFeature?` so the `?: FeatureCatalog.byId("chat")` fallback in `VeniceForgeApp.kt:69` can execute.

**Actual:** `byId` uses `List.first { ... }`, which throws `NoSuchElementException` when `id` is not found. The Elvis-operator fallback is unreachable dead code. If `rememberSaveable` restores an invalid `selectedId` (e.g., from an older app version or tampered state), the app crashes.

**Impact:** Potential startup/restore crash; navigation fallback is a no-op.

**Root cause:** Mismatch between nullable fallback intent and non-nullable throwing implementation.

**Related occurrences:** None in scope; `FeatureCatalog.all.first` pattern is unique here.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `Iterable.first(predicate)` throws `NoSuchElementException` if no element matches.

**Remediation:** Change `FeatureCatalog.byId` to return `AppFeature?` and use `firstOrNull { it.id == id }`.

**Tests required:** Unit test asserting `byId("unknown")` returns null; instrumentation test restoring an invalid `selectedId` falls back to "chat".

**Compatibility impact:** API signature change from non-null to nullable; update `VeniceForgeApp.kt` call site (already uses `?:`, so it becomes valid).

---

### APP-UI-003 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-004 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-005 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-006 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-007 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-008 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-009 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-010 | Severity: P2 | Status: CONFIRMED
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

### APP-UI-011 | Severity: P3 | Status: CONFIRMED
**Area:** Localization | **Module:** `:app` | **File:** `ImageScreen.kt` | **Lines:** 72, 85, 110, 129, 136, 150, 158, 178, 195 | **Symbol:** various `Text(...)` literals

**Evidence:** Hard-coded English strings:
- `Text("Image Studio", ...)` (72)
- `label = { Text("Model") }` (85)
- `label = { Text("Prompt") }` (110)
- `Text(if (state.inputImageUri != null) "Change Image" else "Select Image")` (129)
- `Text("Selected: ${state.inputImageUri?.lastPathSegment}")` (136)
- `Text("Generate")` (150)
- `Text("Edit")` (158)
- `Text("Result:", ...)` (178)
- `Text("Failed to decode image bitmap.", ...)` (195)

**Expected:** All user-visible strings live in `res/values/strings.xml` and are referenced via `stringResource()`.

**Actual:** Strings are inline literals, blocking localization and translation workflows.

**Impact:** Cannot localize Image Studio; inconsistent with `ChatScreen.kt` and `ConfigScreen.kt`, which partially use string resources.

**Root cause:** Developer convenience during scaffolding.

**Related occurrences:** `VeniceForgeApp.kt` (APP-UI-013), `ConfigScreen.kt` (APP-UI-012).

**Venice reference:** N/A.

**Android/Kotlin reference:** Android localization requires translatable strings in resources.

**Remediation:** Extract all literals to `strings.xml` and use `stringResource()`.

**Tests required:** Lint check for hard-coded strings in `ImageScreen.kt`; locale-switch screenshot test.

**Compatibility impact:** None.

---

### APP-UI-012 | Severity: P3 | Status: CONFIRMED
**Area:** Localization | **Module:** `:app` | **File:** `ConfigScreen.kt` | **Lines:** 50, 84, 86, 91, 98, 112, 121, 128, 130, 132, 136, 138, 142, 162, 165, 168 | **Symbol:** various `Text(...)` literals

**Evidence:** Hard-coded English strings for labels, status messages, and button text: "Venice API", "Official Venice.ai API integration...", "Starter functionality...", "Venice API key", "Save", "Remove", "Load models", "No API key loaded", "API key loaded from Android Keystore-backed storage", "Saved to Keystore-backed storage", "API key removed", "Loading /models…", "Loaded ${it.size} models", "Model probe failed: ...", "Model catalog".

**Expected:** All user-facing strings externalized to `strings.xml`.

**Actual:** Inline literals throughout the settings screen.

**Impact:** Cannot localize Config screen; status messages mixed with hard-coded labels.

**Root cause:** Inline literals for rapid prototyping.

**Related occurrences:** `ImageScreen.kt` (APP-UI-011), `VeniceForgeApp.kt` (APP-UI-013).

**Venice reference:** N/A.

**Android/Kotlin reference:** Android `strings.xml` localization contract.

**Remediation:** Extract strings to `strings.xml`; consider a `StringResource` enum or ViewModel-resolved messages if dynamic counts are needed.

**Tests required:** Lint for hard-coded strings; locale tests.

**Compatibility impact:** None.

---

### APP-UI-013 | Severity: P3 | Status: CONFIRMED
**Area:** Localization | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Lines:** 164, 171, 196, 249, 252, 256, 260–263 | **Symbol:** various `Text(...)` literals

**Evidence:** Hard-coded strings: "Venice Forge Android" (164), "Menu" (196), "Android status: ${feature.status}" (249), "Desktop parity target" (252), "Port contract" (256), and the long placeholder explanation (260–263).

**Expected:** Drawer header, button labels, and placeholder headings should be string resources.

**Actual:** Inline literals.

**Impact:** Cannot localize app navigation/placeholder content.

**Root cause:** Inline literals.

**Related occurrences:** `ImageScreen.kt` (APP-UI-011), `ConfigScreen.kt` (APP-UI-012).

**Venice reference:** N/A.

**Android/Kotlin reference:** Android `strings.xml` localization contract.

**Remediation:** Extract to `strings.xml`.

**Tests required:** Lint for hard-coded strings.

**Compatibility impact:** None.

---

### APP-UI-014 | Severity: P3 | Status: CONFIRMED
**Area:** Code hygiene | **Module:** `:core:designsystem` | **File:** `CodexPet.kt` | **Line:** 9 | **Symbol:** `DisposableEffect` import

**Evidence:** `import androidx.compose.runtime.DisposableEffect` is present but never referenced in the file.

**Expected:** No unused imports.

**Actual:** Unused import; also, the very resource that needs disposal (`ImageBitmap`) is not wrapped in `DisposableEffect` (see APP-UI-008).

**Impact:** Minor compile warning; hints at an incomplete implementation.

**Root cause:** Import left after partial implementation.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Either use `DisposableEffect` to dispose the bitmap (APP-UI-008) or remove the import.

**Tests required:** Lint/detekt unused-import check.

**Compatibility impact:** None.

---

### APP-UI-015 | Severity: P3 | Status: CONFIRMED
**Area:** Accessibility | **Module:** `:app` / `:core:designsystem` | **File:** `ImageScreen.kt` / `VeniceForgeApp.kt` / `ConfigScreen.kt` / `VeniceLoadingIndicator.kt` | **Lines:** `ImageScreen.kt:191`, `VeniceForgeApp.kt:159`, `ConfigScreen.kt:80`, `VeniceLoadingIndicator.kt:42,76` | **Symbol:** `contentDescription`

**Evidence:**
- `VeniceForgeApp.kt:159` and `ConfigScreen.kt:80`: `contentDescription = "Official Venice crossed keys"`
- `ImageScreen.kt:191`: `contentDescription = "Result Image"`
- `VeniceLoadingIndicator.kt:42`: `contentDescription = message ?: "Loading indicator"`
- `VeniceLoadingIndicator.kt:76`: `contentDescription = message ?: "Status indicator: ${state.name}"`

**Expected:** Content descriptions should be localizable string resources; result images should describe the content if possible.

**Actual:** Descriptions are hard-coded English strings. The result image description is generic ("Result Image") and does not convey what the image is.

**Impact:** Screen-reader users receive English labels regardless of locale; generated image lacks meaningful description.

**Root cause:** Inline literals for accessibility text.

**Related occurrences:** Same as above.

**Venice reference:** N/A.

**Android/Kotlin reference:** Android accessibility guidelines recommend meaningful, localized content descriptions.

**Remediation:** Move descriptions to `strings.xml`; for generated images, derive a description from the prompt/model if feasible (e.g., "Generated image for: ${prompt}").

**Tests required:** Accessibility scanner / TalkBack traversal test.

**Compatibility impact:** None.

---

### APP-UI-016 | Severity: P3 | Status: CONFIRMED
**Area:** API consistency | **Module:** `:app` | **File:** `ConfigScreen.kt` / `VeniceForgeApp.kt` | **Lines:** `ConfigScreen.kt:129`, `VeniceForgeApp.kt:127` | **Symbol:** `sdk.listModels`, `capabilitiesRepo.fetchLiveCapabilities`

**Evidence:**
- `VeniceForgeApp.kt:127` calls `capabilitiesRepo.fetchLiveCapabilities(key)` to obtain a `ModelCatalog` with capability traits.
- `ConfigScreen.kt:129` calls `sdk.listModels(apiKey)` directly and stores a `List<VeniceModel>`.

**Expected:** Settings screen should use the same capability-discovery path as the rest of the app (`CapabilitiesRepository`) so the user sees the same model metadata and capability filters everywhere.

**Actual:** Two different discovery paths; `ConfigScreen` does not populate `ModelCatalog` and cannot show capability-aware defaults.

**Impact:** Inconsistent model list between Settings and Chat/Image screens; capability metadata (e.g., `supportsTextChat`) is not surfaced in Settings.

**Root cause:** Ad-hoc direct SDK call in Config instead of reusing the shared `CapabilitiesRepository`.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Inject `CapabilitiesRepository` into `ConfigScreen` and call `fetchLiveCapabilities`; display capability traits alongside model names.

**Tests required:** Unit test asserting Settings and VeniceForgeApp use the same repository for model discovery.

**Compatibility impact:** None.

---

### APP-UI-017 | Severity: P3 | Status: INFERRED
**Area:** Accessibility / Touch target | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Lines:** 193–197 | **Symbol:** `OutlinedButton` in `navigationIcon`

**Evidence:**
```kotlin
OutlinedButton(
    onClick = { scope.launch { drawerState.open() } },
    modifier = Modifier.padding(start = 8.dp),
) { Text("Menu") }
```

**Expected:** Touch targets meet the 48x48dp accessibility minimum.

**Actual:** Material3 `OutlinedButton` default minimum height is 40dp; the width is driven by the "Menu" text plus padding, which may be below 48dp on some devices/font scales. No explicit `sizeIn` or `minimumInteractiveComponentSize` is applied.

**Impact:** Users with motor impairments or large font scales may struggle to open the navigation drawer.

**Root cause:** Reliance on default button sizing inside a constrained top-bar slot.

**Related occurrences:** `ChatScreen.kt:47` model picker `OutlinedButton`, `ImageScreen.kt:81` `OutlinedTextField` with trailing icon.

**Venice reference:** N/A.

**Android/Kotlin reference:** Material accessibility guidelines recommend 48dp minimum touch target; Compose provides `Modifier.minimumInteractiveComponentSize()`.

**Remediation:** Apply `.minimumInteractiveComponentSize()` or wrap the button in a 48dp box.

**Tests required:** Accessibility scanner touch-target audit.

**Compatibility impact:** Slightly larger top-bar button; no functional break.

---

### APP-UI-018 | Severity: P3 | Status: CONFIRMED
**Area:** Theme completeness | **Module:** `:core:designsystem` | **File:** `VeniceForgeTheme.kt` | **Lines:** 21–24 | **Symbol:** `MaterialTheme`

**Evidence:**
```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = VeniceTypography.Typography,
    content = content,
)
```

**Expected:** A complete theme should also provide `shapes` to ensure consistent component rounding across the app.

**Actual:** `MaterialTheme` is invoked without `shapes`, falling back to default Material3 shapes. The project defines colors and typography but no shape tokens.

**Impact:** Inconsistent corner radii across custom vs. default components; missed opportunity for brand consistency.

**Root cause:** Shape tokens not defined or wired.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `MaterialTheme` accepts `shapes: Shapes`; default is `Shapes()`.

**Remediation:** Add a `VeniceShapes` object and pass it to `MaterialTheme(shapes = VeniceShapes.Shapes)`.

**Tests required:** Screenshot test comparing component shapes.

**Compatibility impact:** Visual change if shapes differ from defaults.

---

### APP-UI-019 | Severity: P3 | Status: CONFIRMED
**Area:** File URI exposure | **Module:** `:app` | **File:** `VeniceForgeApp.kt` | **Line:** 115 | **Symbol:** `Uri.fromFile`

**Evidence:**
```kotlin
val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
file.writeBytes(bytes)
android.net.Uri.fromFile(file)
```

**Expected:** Use `FileProvider` content URIs for any file that might be shared outside the app; for internal display, ensure it never crosses a `StrictMode` or `FileUriExposedException` boundary.

**Actual:** `Uri.fromFile()` produces a `file://` URI. While the URI is consumed internally in `ImageScreen`, passing it through Compose state or to other components risks accidental exposure. `ImageViewModel.kt:115` uses the same pattern.

**Impact:** Potential `FileUriExposedException` if the URI is ever passed to another app or `Intent`; also flagged by security scanners.

**Root cause:** Convenience use of `Uri.fromFile` instead of `FileProvider`.

**Related occurrences:** `ImageViewModel.kt:115` (out of scope but same pattern).

**Venice reference:** N/A.

**Android/Kotlin reference:** Android 7.0+ restricts `file://` URI exposure; `FileProvider` is the recommended API.

**Remediation:** Migrate to `FileProvider` content URIs or, if strictly internal, document and encapsulate the `file://` usage.

**Tests required:** Security lint check for `Uri.fromFile` usage.

**Compatibility impact:** URI scheme change for saved images; update consumers accordingly.

---

### APP-UI-020 | Severity: P3 | Status: CONFIRMED
**Area:** State persistence | **Module:** `:app` | **File:** `ImageScreen.kt` | **Line:** 76 | **Symbol:** `expanded`

**Evidence:**
```kotlin
var expanded by remember { mutableStateOf(false) }
```

**Expected:** Dropdown expansion state should survive configuration changes.

**Actual:** `expanded` uses `remember`, so the model dropdown closes on rotation.

**Impact:** Minor UX friction; related to APP-UI-005 pattern.

**Root cause:** Use of `remember` for serializable UI state.

**Related occurrences:** `ChatScreen.kt:41` (`modelMenuOpen`), `ConfigScreen.kt:49–53`.

**Venice reference:** N/A.

**Android/Kotlin reference:** `rememberSaveable` for config-change survival.

**Remediation:** Change to `rememberSaveable { mutableStateOf(false) }`.

**Tests required:** Rotation test verifying dropdown state.

**Compatibility impact:** None.

---

## Summary

| Severity | Count | Finding IDs |
|----------|-------|-------------|
| P0 | 0 | — |
| P1 | 2 | APP-UI-001, APP-UI-002 |
| P2 | 8 | APP-UI-003, APP-UI-004, APP-UI-005, APP-UI-006, APP-UI-007, APP-UI-008, APP-UI-009, APP-UI-010 |
| P3 | 10 | APP-UI-011, APP-UI-012, APP-UI-013, APP-UI-014, APP-UI-015, APP-UI-016, APP-UI-017, APP-UI-018, APP-UI-019, APP-UI-020 |
| **Total** | **20** | — |

**Top risks:**
1. **APP-UI-001** — ViewModels created with `remember` leak and lose state on rotation (P1).
2. **APP-UI-002** — `FeatureCatalog.byId` throws, making the navigation fallback dead code and risking a restore crash (P1).
3. **APP-UI-003** — Flows collected with `collectAsState` instead of `collectAsStateWithLifecycle` waste background battery (P2).
4. **APP-UI-008** — `CodexPet` `ImageBitmap` never disposed, leaking native bitmap memory (P2).
5. **APP-UI-009** — "No API key saved" shown while profile/key is still loading, misleading users (P2).
6. **APP-UI-010** — Navigation drawer advertises ~20 unimplemented features as selectable items (P2).
7. **APP-UI-006** — Theme hard-coded to dark mode; system light-mode setting ignored (P2).
8. **APP-UI-007** — Generated image bitmap decoded synchronously on main thread (P2).

No P0 findings (credential exposure, data loss, release-blocking security compromise) were identified in the scoped Compose UI layer.
