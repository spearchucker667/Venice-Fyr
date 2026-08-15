# P3 Findings — Venice Fyr Exhaustive Audit Consolidation

**Severity:** P3
**Total findings after deduplication:** 29

| Status | Count |
|--------|-------|
| CONFIRMED | 27 |
| INFERRED | 2 |

## APP-UI-011 | - `Text("Image Studio", .

**Severity:** P3
**Status:** CONFIRMED
**Area:** Localization
**Module:** :app
**File:** ImageScreen.kt
**Lines:** 72, 85, 110, 129, 136, 150, 158, 178, 195
**Symbol:** various `Text(...)` literals

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

---

## APP-UI-012 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Localization
**Module:** :app
**File:** ConfigScreen.kt
**Lines:** 50, 84, 86, 91, 98, 112, 121, 128, 130, 132, 136, 138, 142, 162, 165, 168
**Symbol:** various `Text(...)` literals

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

---

## APP-UI-013 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Localization
**Module:** :app
**File:** VeniceForgeApp.kt
**Lines:** 164, 171, 196, 249, 252, 256, 260–263
**Symbol:** various `Text(...)` literals

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

---

## APP-UI-014 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Code hygiene
**Module:** :core:designsystem
**File:** CodexPet.kt
**Lines:** 9
**Symbol:** DisposableEffect` import

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

---

## APP-UI-015 | - `VeniceForgeApp.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Accessibility
**Module:** :app` / `:core:designsystem
**File:** ImageScreen.kt` / `VeniceForgeApp.kt` / `ConfigScreen.kt` / `VeniceLoadingIndicator.kt
**Lines:** ImageScreen.kt:191`, `VeniceForgeApp.kt:159`, `ConfigScreen.kt:80`, `VeniceLoadingIndicator.kt:42,76
**Symbol:** contentDescription

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

---

## APP-UI-016 | - `VeniceForgeApp.

**Severity:** P3
**Status:** CONFIRMED
**Area:** API consistency
**Module:** :app
**File:** ConfigScreen.kt` / `VeniceForgeApp.kt
**Lines:** ConfigScreen.kt:129`, `VeniceForgeApp.kt:127
**Symbol:** sdk.listModels`, `capabilitiesRepo.fetchLiveCapabilities

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

---

## APP-UI-017 | ---

**Severity:** P3
**Status:** INFERRED
**Area:** Accessibility / Touch target
**Module:** :app
**File:** VeniceForgeApp.kt
**Lines:** 193–197
**Symbol:** OutlinedButton` in `navigationIcon

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

---

## APP-UI-018 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Theme completeness
**Module:** :core:designsystem
**File:** VeniceForgeTheme.kt
**Lines:** 21–24
**Symbol:** MaterialTheme

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

---

## APP-UI-019 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** File URI exposure
**Module:** :app
**File:** VeniceForgeApp.kt
**Lines:** 115
**Symbol:** Uri.fromFile

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

---

## APP-UI-020 | Top risks:

**Severity:** P3
**Status:** CONFIRMED
**Area:** State persistence
**Module:** :app
**File:** ImageScreen.kt
**Lines:** 76
**Symbol:** expanded

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

---

## ARCH-15 | ConfigScreen hardcodes the default profile and does not use DataStore.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Architecture / Settings
**Module:** :app
**File:** app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt` line 47
**Lines:** 
**Symbol:** profileId

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

---

## BUILD-07 | Hardcoded versionCode/versionName

**Severity:** P3
**Status:** CONFIRMED
**Area:** Release versioning
**Module:** :app
**File:** app/build.gradle.kts
**Lines:** 14-15
**Symbol:** versionCode`, `versionName

| Field | Value |
|-------|-------|
| **Area** | Release versioning |
| **Module** | `:app` |
| **File** | `app/build.gradle.kts` |
| **Lines** | 14-15 |
| **Symbol** | `versionCode`, `versionName` |
| **Evidence** | ```kotlin
versionCode = 1
versionName = "0.1.0-alpha.1"
``` |
| **Expected** | Version values should be driven by CI/tag or a version file to avoid manual editing mistakes and to tie artifacts to Git state. |
| **Actual** | Values are hardcoded in the build script. |
| **Impact** | Risk of releasing an artifact with stale version metadata; harder to automate releases. |
| **Root cause** | Early-stage alpha project with manual versioning. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | Standard Android `defaultConfig` versioning. |
| **Remediation** | Read `versionCode`/`versionName` from `version.properties` or Git tag in CI; keep fallback for local builds. |
| **Tests required** | Verify generated APK manifest contains expected version after CI-driven change. |
| **Compatibility impact** | None. |

---

---

## BUILD-12 | `gradle.properties` omits configuration cache

**Severity:** P3
**Status:** CONFIRMED
**Area:** Build performance / reproducibility
**Module:** Root
**File:** gradle.properties
**Lines:** 1-5
**Symbol:** Gradle optimization flags

| Field | Value |
|-------|-------|
| **Area** | Build performance / reproducibility |
| **Module** | Root |
| **File** | `gradle.properties` |
| **Lines** | 1-5 |
| **Symbol** | Gradle optimization flags |
| **Evidence** | ```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true
``` |
| **Expected** | Modern Gradle builds benefit from `org.gradle.configuration-cache=true` and `org.gradle.configuration-cache.parallel=true`. |
| **Actual** | Configuration cache is not enabled. |
| **Impact** | Slower CI builds; missed reproducibility/parallelism gains. |
| **Root cause** | Conservative defaults. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html). |
| **Remediation** | Enable `org.gradle.configuration-cache=true` after validating plugin compatibility. |
| **Tests required** | `./gradlew test` with configuration cache enabled. |
| **Compatibility impact** | None. |

---

*End of `findings/build.md`.*

---

## CHAT-15 | `return_search_results_as_documents` is missing from `VeniceParameters`

**Severity:** P3
**Status:** CONFIRMED
**Area:** Venice-specific parameters
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 75-88
**Symbol:** VeniceParameters

- **Severity:** P3
- **Status:** CONFIRMED
- **Area:** Venice-specific parameters
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 75-88
- **Symbol:** `VeniceParameters`

**Evidence:**

`VeniceParameters` contains `enableWebSearch`, `enableWebScraping`, `enableWebCitations`, `enableXSearch`, `characterSlug`, `includeVeniceSystemPrompt`, `stripThinkingResponse`, `disableThinking`, `enableE2ee`, `includeSearchResultsInStream`, and `safeMode`, but not `return_search_results_as_documents`.

**Spec:** swagger.yaml lines 1523-1527 document `return_search_results_as_documents` under `venice_parameters`.

**Expected:** SDK exposes the parameter.

**Actual:** Parameter is absent.

**Impact:** Callers cannot request search results as a tool-call documents block.

**Root cause:** Incomplete Venice parameters model.

**Related occurrences:** None.

**Venice reference:** swagger.yaml:1523-1527.

**Android/Kotlin reference:** N/A.

**Remediation:** Add `returnSearchResultsAsDocuments: Boolean?` to `VeniceParameters`.

**Tests required:** Serialization test.

**Compatibility impact:** Additive.

---

---

## CHAT-16 | Many optional OpenAI compatible parameters are missing

**Severity:** P3
**Status:** CONFIRMED
**Area:** Request schema completeness
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt
**Lines:** 58-68
**Symbol:** ChatRequest

- **Severity:** P3
- **Status:** CONFIRMED
- **Area:** Request schema completeness
- **Module:** `:venice-sdk`
- **File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- **Lines:** 58-68
- **Symbol:** `ChatRequest`

**Evidence:**

`ChatRequest` omits `frequency_penalty`, `presence_penalty`, `top_k`, `min_p`, `min_temp`, `max_temp`, `seed`, `repetition_penalty`, `stop`, `stop_token_ids`, `prompt_cache_key`, `prompt_cache_retention`, `fallbacks`, `store`, `verbosity`, `text`, `include`, `metadata`, and `user`.

**Spec:** swagger.yaml lines 636-1669.

**Expected:** SDK exposes commonly used optional parameters.

**Actual:** These parameters cannot be sent.

**Impact:** Reduced OpenAI compatibility; advanced sampling, caching, and metadata features unavailable.

**Root cause:** Minimal initial model.

**Related occurrences:** `ChatRequest.kt`.

**Venice reference:** swagger.yaml:636-1669.

**Android/Kotlin reference:** N/A.

**Remediation:** Add the missing nullable parameters to `ChatRequest`.

**Tests required:** Serialization round-trips.

**Compatibility impact:** Additive.

---

## Test Fixture Notes

- `stream-good.sse` is a valid OpenAI-style SSE stream and matches the fields the SDK parses (`id`, `choices`, `delta.content`, `finish_reason`, `[DONE]`). It does not exercise `usage`, `cost`, `reasoning_content`, multiple choices, or metadata.
- `ChatClientTest` fixtures are inline strings; they correctly model tool-call deltas and stream-side error objects, but they do not assert against the swagger schema for request/response fields.
- `VeniceParametersSerializationTest` verifies `safe_mode: false` preservation, which is a test of an unsupported field (see CHAT-06).

---

## DATA-07 | Area: Clarity / Maintainability Module: `core:data`

**Severity:** P3
**Status:** CONFIRMED
**Area:** Clarity / Maintainability
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt
**Lines:** 29–32
**Symbol:** deleteCascade

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt`  
**Lines:** 29–32  
**Symbol:** `deleteCascade`

**Evidence:**
```kotlin
@Transaction
suspend fun deleteCascade(profileId: String, id: String) {
    deleteById(profileId, id)
}
```
The method name implies it performs manual cascade deletion, but it only calls `deleteById`. The actual cascade is performed by SQLite via the `ForeignKey.CASCADE` on `MessageEntity.conversationId`.

**Expected:** Either delete children manually or rename the method to reflect that it relies on DB-level cascade.

**Actual:** Misleading name; future maintainers may add duplicate deletion logic or assume it is missing.

**Impact:** Maintenance confusion; risk of double deletes or missed cascade if FKs change.

**Root cause:** Naming does not match implementation.

**Related occurrences:** `ChatRepository.deleteConversation` calls `conversationDao.deleteById` directly, not `deleteCascade`.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Transaction`; SQLite FK `ON DELETE CASCADE`.

**Remediation:** Rename to `deleteByProfileAndId` or remove the wrapper and rely on callers using `deleteById`.

**Tests required:** None beyond existing cascade tests.

**Compatibility impact:** Internal API rename only.

---

---

## DATA-13 | Area: API Surface / Module Boundaries Module: `core:data`

**Severity:** P3
**Status:** CONFIRMED
**Area:** API Surface / Module Boundaries
**Module:** core:data
**File:** core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt
**Lines:** 7–16
**Symbol:** DataServices

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt`  
**Lines:** 7–16  
**Symbol:** `DataServices`

**Evidence:**
```kotlin
/**
 * Minimal service-locator that hides Room types from `:app`. `:app` never sees
 * [AppDatabase] or any Room dependency — it only needs a single entry point...
 */
class DataServices private constructor(
    private val db: AppDatabase,
) {
    val chatRepository: ChatRepository by lazy { ChatRepository(db) }
    ...
}
```
`ChatRepository` has a public constructor that takes `AppDatabase` (`core/data/src/main/java/.../repo/ChatRepository.kt` line 14). Because `ChatRepository` is public and exposed via `DataServices`, `:app` can import it and see the Room type in the signature.

**Expected:** Room types should be internal to the module, or the comment should accurately describe the current design.

**Actual:** Room type leakage through `ChatRepository`'s public constructor.

**Impact:** `:app` can inadvertently depend on Room types; breaks the stated abstraction.

**Root cause:** `ChatRepository` constructor is public and accepts `AppDatabase`.

**Related occurrences:** `ChatRepository.kt` line 14.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `internal` visibility; dependency inversion.

**Remediation:** Make `ChatRepository` constructor `internal` and inject DAOs instead of the database.

**Tests required:** Compile-time/API surface test ensuring `:app` cannot reference `AppDatabase`.

**Compatibility impact:** Internal API change.

---

---

## DOC-07 | - `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Candidate GitHub docs pack / branding
**Module:** :app` UI
**File:** docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt
**Lines:** docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md:87`; `ConfigScreen.kt:75–89`; `VeniceForgeApp.kt:154–167

**Area:** Candidate GitHub docs pack / branding  
**Module:** `:app` UI  
**Files:** `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt`  
**Lines:** `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md:87`; `ConfigScreen.kt:75–89`; `VeniceForgeApp.kt:154–167`

**Evidence:**
- `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md:87` states: "In-app About, Settings, and Model Discovery screens feature the unmodified Venice wordmark and Built in Venice badge".
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt:75–89` uses `R.drawable.ic_venice_keys_off_white` / `ic_venice_keys_deep_blue` (crossed keys), not a wordmark or Built in Venice badge.
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt:154–167` uses the same crossed-keys drawable in the navigation drawer.
- No `About` screen exists in `app/src/main/java/.../android/`.
- No `Built in Venice` badge asset is referenced in the app.

**Expected:** Candidate documentation should describe the actual current app surfaces.

**Actual:** The candidate pack branding doc describes wordmark/badge placements that are not present.

**Impact:** If the candidate pack is adopted without reconciliation, it would introduce stale branding guidance.

**Root cause:** The candidate pack was generated against an earlier repository state and not reconciled with the current UI.

**Related occurrences:** Root `docs/BRANDING.md:87` correctly describes the crossed-keys usage in the navigation drawer and Settings/Config screens.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** If the pack is ever adopted, update its `docs/BRANDING.md` to match the current in-app attribution (crossed keys only) or implement the described surfaces first.

**Tests required:** None.

**Compatibility impact:** None unless the pack is merged as-is.

---

---

## DOC-08 | - `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Review artifact provenance
**Module:** N/A
**File:** docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md
**Lines:** 5–6, 30–34

**Area:** Review artifact provenance  
**Module:** N/A  
**File:** `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md`  
**Lines:** 5–6, 30–34

**Evidence:**
- `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md:5–6` says "Current HEAD at Review: `02ae314`".
- `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md:30–34` repeats the commit `02ae314` and branch `main`.
- `git rev-parse --short HEAD` in the repository root returns `1da3142`.

**Expected:** A review report should identify the HEAD it actually reviewed.

**Actual:** The report is pinned to `02ae314`, which is not the current HEAD.

**Impact:** Provenance mismatch; readers cannot trust that the review reflects the current tree.

**Root cause:** The review was performed against an earlier commit and the report was not updated.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Update the report header to `1da3142` and re-verify the claims, or add a clear "reviewed at historical commit" note.

**Tests required:** None.

**Compatibility impact:** None.

---

---

## DOC-09 | - Root `CHANGELOG.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Candidate GitHub docs pack / changelog
**Module:** N/A
**File:** docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md`, `CHANGELOG.md
**Lines:** docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md:11–16`; `CHANGELOG.md:17–20

**Area:** Candidate GitHub docs pack / changelog  
**Module:** N/A  
**Files:** `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md`, `CHANGELOG.md`  
**Lines:** `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md:11–16`; `CHANGELOG.md:17–20`

**Evidence:**
- Root `CHANGELOG.md:17–20` lists "Image Studio foundation", `:venice-sdk` Image client, Audio client, and Video client under [Unreleased].
- `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md:11–16` only lists "Native Android port foundation", module boundaries, Room foundation, and documentation workflow — omitting the image/audio/video clients.

**Expected:** A candidate docs pack changelog should not silently drop recent user-facing changes.

**Actual:** The candidate changelog is missing the image/audio/video SDK work that is present in the root changelog.

**Impact:** Adopting the pack would revert the changelog to an older state.

**Root cause:** The pack was generated before the image/audio/video clients were added to the changelog.

**Related occurrences:** See DOC-07.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Merge/reconcile the candidate changelog with the root `CHANGELOG.md` before adoption.

**Tests required:** None.

**Compatibility impact:** None unless the pack is merged as-is.

---

## Notes on non-findings

The following high-level documentation claims were checked and found to be consistent with the current code:

- **Module layout:** `README.md`, `GETTING_STARTED.md`, `DEVELOPMENT_GUIDE.md`, and `SDK_GUIDE.md` correctly describe `:app`, `:venice-sdk`, `:core:common`, `:core:security`, `:core:designsystem`, and `:core:data`.
- **Toolchain:** Documented versions for AGP 9.3.0, Gradle 9.5.0, Kotlin 2.3.21, compile/targetSdk 37, minSdk 26, Compose BOM 2026.06.00, OkHttp 5.3.0 match `gradle/libs.versions.toml`.
- **Security boundaries:** `SECURITY.md`, `PRIVACY.md`, and `docs/SECURITY_AND_STORAGE_CONTRACT.md` align with `AndroidManifest.xml` (only `INTERNET`, `usesCleartextTraffic="false"`, `allowBackup="false"`), `network_security_config.xml`, `SecureSecretStore.kt` (Keystore AES-GCM), and the absence of telemetry/analytics SDKs in dependencies.
- **SDK credential boundary:** `:venice-sdk` has no persistence code; `SecureSecretStore` lives in `:core:security` and is consumed by `:app`.
- **Branding assets:** `docs/BRANDING.md` asset paths, color tokens, typography fallbacks, launcher icon XML files, Codex Pet spritesheet, and animation contract match the implementation in `core/designsystem` and `app/src/main/res`.

---

## HYGIENE-01 | Area: Repo hygiene / test fixtures Module: `:venice sdk` File: `venice sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` Lines: 39, 64, 91, 122 Symbol: hardcoded API key strings

**Severity:** P3
**Status:** CONFIRMED
**Area:** Repo hygiene / test fixtures
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt
**Lines:** 39, 64, 91, 122
**Symbol:** hardcoded API key strings

**Evidence:**
- Line 39: `sdk.listModels(apiKey = "secret-key-12345", type = null)`
- Line 64: `sdk.listModels(apiKey = "secret-key-12345", type = ModelType.IMAGE)`
- Line 91: `val apiKey = "super-secret-api-key-xyz"`
- Line 122: `val apiKey = "secret-token-abcdef"`

**Expected:** Test fixtures should use obviously non-secret placeholders (e.g., `"test-api-key"`) or load values from test-only environment/configuration, and should not resemble real credential patterns.

**Actual:** Four test methods embed strings that match secret-like regexes (`secret-key-*`, `super-secret-api-key-*`, `secret-token-*`). These are not real credentials, but they trigger secret scanners and set a poor precedent.

**Impact:** Low — false positives in secret scanning; potential for copy-paste into production if not reviewed.

**Root cause:** Convenience test data without a standardized placeholder convention.

**Related occurrences:** None elsewhere in tracked files.

**Venice reference:** `AGENTS.md` Fixture Rule (`api-docs` fixtures must model authoritative schema); these are SDK unit tests, not API fixtures, but the same discipline applies.

**Android/Kotlin reference:** N/A.

**Remediation:** Replace with a single test constant such as `TEST_API_KEY = "venice-sdk-test-key"` that does not match secret regexes.

**Tests required:** None (test-only change).

**Compatibility impact:** None.

---

---

## HYGIENE-02 | Area: Repo hygiene / .gitignore Module: root File: `.gitignore` Lines: 1 19 Symbol: `.gitignore` patterns

**Severity:** P3
**Status:** CONFIRMED
**Area:** Repo hygiene / .gitignore
**Module:** root
**File:** .gitignore
**Lines:** 1-19
**Symbol:** .gitignore` patterns

**Evidence:**
- `.gitignore` covers `.gradle/`, `.gradle-bootstrap/`, `.idea/`, `**/build/`, `.local/`, `.source/`, `*.apk`, `*.aab`, `*.aar`, `*.jks`, etc.
- Working tree contains `.kotlin/` and `.superpowers/` directories that are not ignored and not tracked.

**Expected:** Generated/tooling directories should be ignored to prevent accidental commits.

**Actual:** `.kotlin/` (Kotlin compiler daemon output) and `.superpowers/` (project skill/plugin workspace) are not listed in `.gitignore`.

**Impact:** Low — currently not tracked, but a future contributor could accidentally add them.

**Root cause:** `.gitignore` created before these directories were introduced.

**Related occurrences:** None tracked.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin/Gradle convention ignores `.kotlin/`.

**Remediation:** Add `.kotlin/` and `.superpowers/` to root `.gitignore`.

**Tests required:** None.

**Compatibility impact:** None.

---

## Build-output / ignored-file scan

No tracked files matched build-output or environment-sensitive patterns (`.gradle/`, `**/build/`, `*.apk`, `*.aab`, `*.aar`, `*.jks`, `*.keystore`, `local.properties`, `.idea/`, `*.iml`, `.DS_Store`, `.log`).


## Secret-like string scan

The following lines matched secret-like patterns. Context must be reviewed to distinguish placeholders/docs from real credentials.

| File | Line | Pattern | Evidence |
|------|------|---------|----------|
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 39 | api_key assignment | `sdk.listModels(apiKey = "secret-key-12345", type = null)` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 64 | api_key assignment | `sdk.listModels(apiKey = "secret-key-12345", type = ModelType.IMAGE)` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 91 | api_key assignment | `val apiKey = "super-secret-api-key-xyz"` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 122 | api_key assignment | `val apiKey = "secret-token-abcdef"` |

## Ignored-but-tracked files

No tracked files are also matched by `.gitignore` patterns.


## .gitignore adequacy

The root `.gitignore` (19 lines) covers: `.gradle/`, `.gradle-bootstrap/`, `.idea/`, `*.iml`, `local.properties`, `.DS_Store`, `/build/`, `**/build/`, `.externalNativeBuild/`, `.cxx/`, `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `*.apk`, `*.aab`, `*.aar`, `.local/`, `.source/`.

Notable omissions observed in working tree (not tracked): `.kotlin/` (Kotlin compiler daemon output) and `.superpowers/` (OMK managed-skill workspace) are not ignored. `.gradle-bootstrap/` is already covered. If these directories contain generated artifacts, they should be added to `.gitignore`. Currently they are not tracked, so no immediate hygiene defect.

---

## IMG-12 | - Swagger `GenerateImageRequest` defines `height`/`width` constraints: `minimum: 0`, `exclusiveMinimum: true`, `maximum: 1280` (lines 2609–2615, 2734–2740).

**Severity:** P3
**Status:** CONFIRMED
**Area:** Request validation / Model constraints
**Module:** :venice-sdk
**File:** ImageModels.kt
**Lines:** 13–35
**Symbol:** GenerateImageRequest

**Area:** Request validation / Model constraints  
**Module:** `:venice-sdk`  
**File:** `ImageModels.kt`  
**Lines:** 13–35  
**Symbol:** `GenerateImageRequest`

**Evidence:**
- Swagger `GenerateImageRequest` defines `height`/`width` constraints: `minimum: 0`, `exclusiveMinimum: true`, `maximum: 1280` (lines 2609–2615, 2734–2740).
- Swagger defines `cfg_scale` `minimum: 0`, `exclusiveMinimum: true`, `maximum: 20` (lines 2586–2592).
- SDK does not validate these constraints before sending.

**Expected:** SDK optionally validates numeric constraints client-side to fail fast.

**Actual:** Invalid values are sent to the server, resulting in 400 errors.

**Impact:** Minor UX/cost issue; every invalid request consumes a network round-trip.

**Root cause:** No validation layer in request models.

**Venice reference:** `swagger.yaml:/components/schemas/GenerateImageRequest/properties/height`.

**Remediation:** Add lightweight validation in `GenerateImageRequest` init block or in `ImageClient.generate()`.

**Tests required:** Unit tests for out-of-range width/height/cfg_scale.

---

---

## IMG-13 | - `ImageClientTest.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Test coverage
**Module:** :venice-sdk
**File:** ImageClientTest.kt
**Lines:** 1–105
**Symbol:** ImageClientTest

**Area:** Test coverage  
**Module:** `:venice-sdk`  
**File:** `ImageClientTest.kt`  
**Lines:** 1–105  
**Symbol:** `ImageClientTest`

**Evidence:**
- `ImageClientTest.kt` contains only two tests: `generate maps request and response correctly` (lines 38–76) and `edit maps request correctly` (lines 78–104).
- No tests for `upscale`, `multiEdit`, `generateBinary`, error responses, `safe_mode` serialization, multipart, or response headers.

**Expected:** Each public image method and major response path has unit coverage.

**Actual:** Coverage is minimal; several methods have no tests.

**Impact:** Defects like IMG-01 (binary response mismatch) were not caught by tests.

**Root cause:** Test suite not expanded alongside SDK surface.

**Venice reference:** N/A.

**Remediation:** Add tests for all public methods, error paths, binary responses, and serialization edge cases.

**Tests required:** See remediation items for IMG-01 through IMG-12.

---

## Non-findings (verified correct)

### safe_mode=false preservation
**Status:** CONFIRMED correct.  
`GenerateImageRequest.safeMode: Boolean? = null` (`ImageModels.kt:23`) uses a nullable Boolean with default `null`. With `Json { encodeDefaults = false }` (`ImageClient.kt:16`), kotlinx.serialization omits only values equal to the declared default (`null`). An explicit `false` value is not equal to `null`, so it is serialized as `"safe_mode":false`. The same logic applies to `EditImageRequest.safeMode` and `MultiEditImageRequest.safeMode`. This satisfies AGENTS.md "Preserve explicit safe_mode=false when selected."

### `/image/generate` JSON path
**Status:** CONFIRMED correct.  
`generate()` sends `Accept: application/json` and parses `GenerateImageResponse`, matching swagger `/image/generate` when `return_binary=false`.

### `/image/generate` binary path
**Status:** CONFIRMED correct.  
`generateBinary()` sends `Accept: image/*` and returns `ByteArray`, matching swagger when `return_binary=true`.

---

## End of findings

---

## SDK-CORE-19 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Test Coverage
**Module:** :venice-sdk
**File:** venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt
**Lines:** 6-16
**Symbol:** VeniceEndpointsTest

**Area:** Test Coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt`  
**Lines:** 6-16  
**Symbol:** `VeniceEndpointsTest`

**Evidence:**
```kotlin
class VeniceEndpointsTest {
    @Test fun canonicalMediaPathsRemainStable() { ... }
    @Test fun parameterizedPathsDoNotAddLeadingSlash() { ... }
}
```

**Expected:** The endpoint inventory should have comprehensive coverage for all constants and parameterized helpers.

**Actual:** Only 5 paths are asserted. Many endpoints (billing, crypto, x402, augment, embeddings, audio/video queue/quote/retrieve) are untested.

**Impact:** Low regression protection if endpoint constants are accidentally changed.

**Root cause:** Minimal test coverage.

**Related occurrences:** None.

**Android/Kotlin reference:** JUnit4.

**Remediation:** Add parameterized tests covering all constants and helper functions.

**Tests required:** New tests.

**Compatibility impact:** None.

---

---

## SDK-CORE-20 | --- 1.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Public API / User Agent
**Module:** :venice-sdk
**File:** venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt
**Lines:** 3-6
**Symbol:** VeniceSdkConfig

**Area:** Public API / User Agent  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceSdkConfig.kt`  
**Lines:** 3-6  
**Symbol:** `VeniceSdkConfig`

**Evidence:**
```kotlin
data class VeniceSdkConfig(
    val baseUrl: String = "https://api.venice.ai/api/v1/",
    val userAgent: String = "VeniceForgeAndroid/0.1.0",
)
```

**Expected:** The SDK should allow consumers to identify their app/version in the user agent.

**Actual:** `userAgent` is a fixed string with no built-in way to append app name/version.

**Impact:** Venice server logs cannot distinguish between different apps or versions using the SDK.

**Root cause:** Single hard-coded user agent string.

**Related occurrences:** `VeniceForgeSdk.userAgent()`.

**Android/Kotlin reference:** Android `BuildConfig.VERSION_NAME` (not available in library module by default).

**Remediation:** Accept an optional app identifier/version in `VeniceSdkConfig` and compose the user agent string.

**Tests required:** Tests verifying custom user agent is sent.

**Compatibility impact:** Additive if new fields have defaults.

---

## Positive findings

1. **No production hard-coded model IDs:** All model IDs in `src/main` are runtime-derived; the SDK complies with AGENTS.md rule 1 for production code.
2. **No plaintext credential persistence:** The SDK does not persist API keys; keys are supplied per request.
3. **Injectable HTTP client:** `VeniceForgeSdk` accepts an `OkHttpClient`, enabling test doubles and interceptors.
4. **`ignoreUnknownKeys = true`:** JSON parsers tolerate future swagger additions without crashing.
5. **API key leak test:** `VeniceForgeSdkTest` verifies exception messages do not contain the API key.
6. **Trait fallback tests:** `CapabilitiesRepositoryTest` covers orphan traits and missing-trait fallback behavior.

---

## SEC-10 | SecureSecretStore uses truncated digest for alias derivation

**Severity:** P3
**Status:** INFERRED
**Area:** Credential persistence / Cryptography
**Module:** :core:security
**File:** core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt
**Lines:** 85–91
**Symbol:** alias`, `prefKey`, `digest

**ID:** SEC-10 | **Severity:** P3 | **Status:** INFERRED | **Area:** Credential persistence / Cryptography | **Module:** `:core:security`

**File:** `core/security/src/main/java/io/github/spearchucker667/veniceforge/core/security/SecureSecretStore.kt`  
**Lines:** 85–91  
**Symbol:** `alias`, `prefKey`, `digest`

**Evidence:**
- `SecureSecretStore.kt:88–91`:
  ```kotlin
  private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
      .digest(value.toByteArray(StandardCharsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
      .take(32)
  ```

**Expected:** Keystore alias and preference key should uniquely identify the profile without unnecessary collision risk.

**Actual:** The full 64-character SHA-256 hex digest is truncated to 32 characters (128 bits). While still collision-resistant for a small number of profiles, it is weaker than the full digest and unnecessary.

**Impact:** Negligible in practice for this app, but it reduces the security margin and deviates from standard practice of using the full hash.

**Root cause:** Explicit `.take(32)` truncation.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:**
- Use the full 64-character SHA-256 hex digest, or switch to a deterministic encoding of the profile ID with appropriate length.
- Note: changing alias derivation will invalidate existing stored keys; migration logic is required.

**Tests required:**
- Unit test that alias/prefKey are stable and unique per profile ID.

**Compatibility impact:** Changing alias derivation requires a migration path for existing users.

---

## Positive Findings

---

## TEST-MISSING-15 | - Partial redaction (e.

**Severity:** P3
**Status:** CONFIRMED
**Area:** Minimal redaction coverage
**Module:** :core:common
**File:** core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt
**Lines:** 7–15
**Symbol:** Redactor.redact

**Area:** Minimal redaction coverage  
**Module:** `:core:common`  
**File:** `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt`  
**Lines:** 7–15  
**Symbol:** `Redactor.redact`  
**Evidence:** One test case covers Bearer token, API key, and Unix path. Missing:
- Partial redaction (e.g., `Bearer vn-xxx`)
- Keys without `sk`/`vn` prefix
- Windows-style paths
- Empty/null strings
- Already-redacted strings

**Expected:** Redaction rules thoroughly tested.  
**Actual:** Single happy-path case.  
**Impact:** Edge-case leaks may not be caught.  
**Root cause:** Minimal test.  
**Related occurrences:** `Redactor.kt:8–16`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `Redactor.kt:8–16`.  
**Remediation:** Add parameterized tests for edge cases.  
**Tests required:** Expand `RedactorTest`.  
**Compatibility impact:** Low.

---

---

## TEST-MISSING-16 | ---

**Severity:** P3
**Status:** CONFIRMED
**Area:** Feature catalog semantics not tested
**Module:** :app
**File:** app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt
**Lines:** 7–11
**Symbol:** FeatureCatalog.all`, `FeatureCatalog.byId

**Area:** Feature catalog semantics not tested  
**Module:** `:app`  
**File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt`  
**Lines:** 7–11  
**Symbol:** `FeatureCatalog.all`, `FeatureCatalog.byId`  
**Evidence:** The single test asserts `all.size == 22` and that IDs are unique. It does not verify any feature statuses, groups, labels, or the `byId` lookup behavior.  
**Expected:** Feature catalog semantics tested.  
**Actual:** Trivial count test.  
**Impact:** Catalog regressions (wrong status, missing feature, duplicate ID) are not caught beyond count.  
**Root cause:** Minimal test.  
**Related occurrences:** `FeatureCatalog.kt:21–47`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `FeatureCatalog.kt:21–47`.  
**Remediation:** Add tests for required fields, group distribution, and `byId` lookup.  
**Tests required:** Expand `FeatureCatalogTest`.  
**Compatibility impact:** Low.

---

---
