# 14-ANDROID-LIFECYCLE-STORAGE-AUDIT.md

**Audit scope:** `app`, `venice-sdk`, `core:*` production sources, manifests, and build files.  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `main`/`1da3142`.  
**Venice API source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream `6e69346b`, `info.version 20260814.194349`.  
**Desktop parity mirror:** `.source/Venice_Forge-desktop` (read-only).

---

## 1. Lifecycle Audit

### Activity lifecycle
`MainActivity.kt:9-16` is a minimal `ComponentActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VeniceForgeTheme(darkTheme = true) { VeniceForgeApp() } }
    }
}
```

- `savedInstanceState` is never read or written.
- `onConfigurationChanged` is not overridden; the default Activity recreation path is used.
- No `ViewModel` is obtained via `ViewModelProvider` or `viewModel()`.

### Compose state survival
Only `selectedId` in `VeniceForgeApp.kt:68` is saved across process death:

```kotlin
var selectedId by rememberSaveable { mutableStateOf("chat") }
```

Everything else is ordinary `remember` or `mutableStateOf`:
- `profileId` (`VeniceForgeApp.kt:83`)
- `modelCatalog` (`VeniceForgeApp.kt:122`)
- `ChatViewModel` / `ImageViewModel` state
- `ConfigScreen` text fields and model list

### ViewModel lifecycle
`ChatViewModel` and `ImageViewModel` extend `ViewModel` but are **not** created through the framework. They are instantiated as plain Kotlin objects inside `remember` (`VeniceForgeApp.kt:86-120`). Therefore:
- They are **not** retained across configuration changes.
- `viewModelScope` is not cancelled by the framework on clear; coroutines may leak until the old instance is garbage collected.
- `onCleared()` is never called.

### Configuration-change impact on chat
1. User sends a message; `ChatViewModel.submit` starts collecting the SSE stream.
2. Device rotates; Activity and Composition are destroyed.
3. New `ChatViewModel` is created; `init` loads the most recently updated conversation.
4. The stream job from the old instance continues to run (if still referenced) but no UI observes it.
5. Model selection (`_state.value.modelId`) is reset to `initialModelId`/`null`.

### Process-death impact
Because no state is saved:
- Streaming/generation flags are lost.
- The Room DB may still contain a `PENDING` or `STREAMING` assistant message.
- On restart, `ChatViewModel.init` picks the most recent conversation or creates a new one.
- `ConfigScreen` reverts to empty API key field until `LaunchedEffect` loads it from Keystore.

### Backgrounding
- There is **no foreground service** or `WorkManager` worker to keep a chat stream or generation job alive when the app is backgrounded.
- Coroutines run in `viewModelScope`; they survive backgrounding until the process is killed.
- This is acceptable for short chat requests but not for long video/audio/music generation jobs.

### Findings
- **ARCH-01** (P1): ViewModels not retained across configuration changes. (`VeniceForgeApp.kt:86-120`)
- **ARCH-11** (P2): No `SavedStateHandle` or `savedInstanceState` recovery for in-flight UI state. (`MainActivity.kt:9-16`, `ChatViewModel.kt`, `ImageViewModel.kt`)
- **ARCH-12** (P2): Active conversation selection is recomputed from `updatedAt DESC` and is unstable across recreation. (`ChatViewModel.kt:54-67`)

---

## 2. Storage and Scoped-Storage Audit

### Local databases
`core:data` uses Room (`AppDatabase.kt`). The database file is `venice_forge.db` in the app-private directory.

- Schema version: 1.
- `exportSchema = true`, schemas exported to `core/data/schemas/`.
- No destructive fallback migration configured yet.

### Credential storage
`SecureSecretStore.kt` stores the API key as:
- Plaintext → AES-GCM encryption with a Keystore-backed key.
- Ciphertext + IV → Base64 → `SharedPreferences` (`venice_forge_secure_secrets`).

This satisfies the project rule “No plaintext persistent credentials” and “`:venice-sdk` never persists API keys.”

### Media storage
| Operation | Location | Permission required |
|-----------|----------|---------------------|
| Image input (Photo Picker) | Content URI from system | None |
| Generated/edited image output | `context.cacheDir` | None |
| Audio/video output | Not implemented | N/A |

No external/shared storage is used. No `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_*`, or `MANAGE_EXTERNAL_STORAGE` permissions are declared.

### Cache hygiene
Generated images are written with timestamped filenames:

```kotlin
val file = java.io.File(context.cacheDir, "venice_image_${System.currentTimeMillis()}.png")
```

There is **no eviction policy**. The cache will grow unbounded until the OS clears it or the user uninstalls the app.

### Content-URI handling
- **Input:** `ImageScreen.kt:50-55` uses `PickVisualMedia` and passes the URI to `ImageViewModel.onImageSelected`. The actual read is done via `context.contentResolver.openInputStream(uri)` in `VeniceForgeApp.kt:105-108`.
- **Output:** The app uses `Uri.fromFile(file)` for cache files. There is **no `FileProvider`** because the URI is consumed only inside the same app.

### FileProvider / sharing
No `FileProvider` is declared. If future features need to share generated media with other apps, a `FileProvider` must be added.

### Findings
- **ARCH-09** (P2): Generated image cache files are never cleaned up. (`ImageViewModel.kt:111-115`)
- **ARCH-08** (P2): Result bitmaps are decoded on the main thread. (`ImageScreen.kt:180-186`)
- **ARCH-13** (P2): Image-edit base64 decode runs on the main thread. (`ImageViewModel.kt:110-116`)

---

## 3. Background Work, Services, Receivers, and Providers

### WorkManager
- Declared in `app/build.gradle.kts:51` as `implementation(libs.androidx.work.runtime)`.
- **Zero production references** to `androidx.work` in `app/src/main/java`.
- No `Worker`, `WorkRequest`, or `WorkManager` configuration exists.

This conflicts with `ANDROID_PORT_HANDOFF.md` Phase 5, which requires WorkManager-backed durable jobs for video/music/audio generation and retrieval.

### Services
No `Service`, `JobService`, `ForegroundService`, or `MediaBrowserService` is declared in `app/src/main/AndroidManifest.xml` or implemented in code.

### BroadcastReceivers
No receivers are declared or implemented.

### ContentProviders
No content providers are declared. There is no `FileProvider`.

### JobScheduler / AlarmManager
No usage detected.

### Findings
- **ARCH-06** (P2): WorkManager is declared but unused; no durable background work infrastructure exists. (`app/build.gradle.kts:51`)

---

## 4. Permissions and Security Posture

### Manifest permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Only `INTERNET` is requested. This is correct for a privacy-first app that uses the Photo Picker and app-private cache.

### Network security
```xml
android:networkSecurityConfig="@xml/network_security_config"
android:usesCleartextTraffic="false"
```
`network_security_config.xml` denies cleartext traffic. Good.

### Backup
```xml
android:allowBackup="false"
android:fullBackupContent="false"
```
Good: prevents auto-backup of the Room DB and SharedPreferences.

---

## 5. Ledger Summary

Full ledger is in `findings/architecture.md`. This lifecycle/storage audit reviewed 58 production files (~4,038 lines).

**Lifecycle/storage-specific findings:**
- ARCH-01 (P1): ViewModels not retained.
- ARCH-06 (P2): WorkManager declared but unused.
- ARCH-08 (P2): Main-thread bitmap decode.
- ARCH-09 (P2): Unbounded image cache.
- ARCH-11 (P2): No process-death recovery.
- ARCH-12 (P2): Unstable active conversation selection.
- ARCH-13 (P2): Main-thread base64 decode for image edits.

**Severity summary:**
- P0: 0
- P1: 1
- P2: 6
- P3: 0

---

## 6. Recommendations

1. **Adopt framework ViewModels:** Use `viewModel()` with a factory and `SavedStateHandle` so that UI state survives configuration changes and process death.
2. **Persist active conversation ID:** Store the current conversation ID in `SavedStateHandle` or DataStore and restore it in `ChatViewModel.init`.
3. **Reconcile DB on startup:** On `Application.onCreate` or `ChatViewModel.init`, mark any `PENDING`/`STREAMING` messages as `FAILED` or `CANCELLED` so that stale in-flight state does not confuse the user.
4. **Implement WorkManager workers:** For video/audio/music queued jobs, create `Worker` classes that survive process death and retry.
5. **Add cache management:** Implement a bounded, content-addressed cache for generated media with eviction.
6. **Move heavy decode off main thread:** Use Coil `AsyncImage` or `produceState` with `Dispatchers.IO` for generated/edited images.
7. **Add `FileProvider` when sharing media:** If future features share URIs with other apps, declare a `FileProvider` in the manifest.
