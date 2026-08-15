# 04-ANDROID-ARCHITECTURE-AUDIT.md

**Audit scope:** `app`, `venice-sdk`, `core:*` production sources, manifests, and build files.  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `main`/`1da3142`.  
**Venice API source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream `6e69346b`, `info.version 20260814.194349`.  
**Desktop parity mirror:** `.source/Venice_Forge-desktop` (read-only).

---

## 1. DI, Singletons, and Global State

### Current DI strategy
The app uses a **manual service-locator inside Compose** rather than a framework DI graph:

- `VeniceForgeApp.kt:73` creates `SecureSecretStore(context)`.
- `VeniceForgeApp.kt:74` creates `VeniceForgeSdk()`.
- `VeniceForgeApp.kt:77` creates `DataServices.create(context)`.
- `VeniceForgeApp.kt:80-81` creates `ChatClient(sdk)` and `CapabilitiesRepository(sdk)`.
- `VeniceForgeApp.kt:86-120` creates `ChatViewModel(...)` and `ImageViewModel(...)` directly inside `remember`.

Because these objects are held only in `remember`, they are recreated on every Activity recreation / process restart. This violates the Android `ViewModel` contract and the Room singleton guidance.

### Singleton / hidden global state audit

| Component | Intended scope | Actual scope | Risk |
|-----------|---------------|--------------|------|
| `AppDatabase` | Process singleton | New instance per `DataServices.create` | Multiple Room instances, leaks |
| `DataServices` | Process singleton | New per Composition | Repositories recreated |
| `SecureSecretStore` | Process singleton | New per Composition | Low (same backing store) |
| `VeniceForgeSdk` | Process singleton | New per Composition | New OkHttpClient, no shared pool |
| `FeatureCatalog` | Object singleton | Object singleton | Safe |
| `Redactor` | Object singleton | Object singleton | Safe, but unused |

**No hidden global mutable state** was found beyond the standard Android `SharedPreferences` and Keystore entries.

### Finding: ARCH-01 — ViewModels not retained
See `findings/architecture.md` ARCH-01 for full evidence. Summary: `ChatViewModel` and `ImageViewModel` are instantiated directly in Compose (`VeniceForgeApp.kt:86-120`), so they do **not** survive configuration changes. This breaks streaming, loses model/conversation selection, and can leak `viewModelScope` jobs.

### Finding: ARCH-03 — DataServices/AppDatabase not singleton
See `findings/architecture.md` ARCH-03. Summary: `DataServices.create` builds a new `AppDatabase` each time it is called; `VeniceForgeApp.kt` calls it inside `remember`, causing a new Room instance on every composition.

---

## 2. Network Layer and Serialization

### Network layer count
A single OkHttp stack is used. Each SDK client builds requests inline:

- `VeniceForgeSdk.kt` — `/models`, raw GETs, error parsing.
- `ChatClient.kt` — `/chat/completions` SSE streaming.
- `ImageClient.kt` — `/image/generate`, `/image/edit`, etc.
- `AudioClient.kt` — `/audio/speech`.
- `VideoClient.kt` — `/video/queue`, `/video/retrieve`, `/video/complete`.

There is **no shared request builder, retry interceptor, or logging interceptor** yet. This is acceptable for a starter but will need consolidation as retry/rate-limit logic is added.

### Serializer instances
Each client owns a separate `Json` instance:

- `VeniceForgeSdk`: `Json { ignoreUnknownKeys = true }`
- `ChatClient`: `Json { ignoreUnknownKeys = true }`
- `ImageClient` / `AudioClient` / `VideoClient`: `Json { ignoreUnknownKeys = true; encodeDefaults = false }`
- `CapabilitiesRepository`: `Json { ignoreUnknownKeys = true }`

**Risk:** Inconsistent `encodeDefaults` behavior. If a DTO is shared between clients, the same object could serialize differently. Today the DTOs are client-specific, so this is latent.

### UI-to-transport bypasses
**None detected.** Every network call originates in `:venice-sdk` clients. `:app` never constructs raw OkHttp requests or bypasses the SDK.

### Finding: ARCH-16 — ChatClient ignores SSE usage events
See `findings/architecture.md` ARCH-16. The SSE parser only inspects `choices`; usage events are dropped, so token accounting is impossible for streamed completions.

### Finding: ARCH-17 — CapabilitiesRepository swallows endpoint errors
See `findings/architecture.md` ARCH-17. Failures from `/models/traits` and `/models/compatibility_mapping` return empty maps, hiding network/auth errors from the UI.

---

## 3. Lifecycle, Process Death, and Background Work

### Activity / ViewModel lifecycle
- `MainActivity.kt:9-16` is a single-activity Compose app with no `savedInstanceState` handling.
- `VeniceForgeApp.kt:68` only persists `selectedId` via `rememberSaveable`.
- `ChatViewModel` and `ImageViewModel` do not use `SavedStateHandle`.

### Configuration change survival
Because ViewModels are created with `remember`, they are **not** retained across configuration changes. On rotation:
- `ChatViewModel` is recreated.
- `conversationId` is recomputed from the most recent conversation.
- `modelId` resets.
- Any active stream is dropped.

### Process death
No in-flight UI state is saved. After process death:
- Streaming/generation flags are lost.
- The DB may contain `PENDING`/`STREAMING` assistant messages with no recovery logic.
- `ConfigScreen` loses its text fields and loaded model list.

### Background work
- `app/build.gradle.kts:51` declares `androidx.work.runtime`.
- **No WorkManager code exists** in `app/src/main/java`.
- All async work runs in `viewModelScope` and dies with the UI.

This directly conflicts with `ANDROID_PORT_HANDOFF.md` Phase 5, which requires WorkManager-backed durable jobs for video/music/audio generation.

### Services, Receivers, Providers
The only component declared in `app/src/main/AndroidManifest.xml` is `MainActivity`. There are **no** services, broadcast receivers, content providers, or `FileProvider` entries.

### Findings
- **ARCH-01** (P1): ViewModels not retained.
- **ARCH-11** (P2): No process-death recovery for in-flight UI state.
- **ARCH-12** (P2): Active conversation selection not stable across recreation.
- **ARCH-06** (P2): WorkManager declared but unused; no durable background work.

---

## 4. Storage, Scoped Storage, Permissions, and Content URIs

### Permissions
`app/src/main/AndroidManifest.xml` declares only:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

No storage, camera, microphone, or broad file-system permissions are requested. This aligns with the project boundary “No broad storage permissions.”

### Network security
```xml
android:networkSecurityConfig="@xml/network_security_config"
android:usesCleartextTraffic="false"
```
`network_security_config.xml` sets `cleartextTrafficPermitted="false"`. Good.

### Backup
```xml
android:allowBackup="false"
android:fullBackupContent="false"
```
Good: secrets and local DB are not auto-backed up.

### Image input
`ImageScreen.kt:50-55` uses `ActivityResultContracts.PickVisualMedia`, which leverages the system Photo Picker and requires **no storage permission**. Input bytes are read via `context.contentResolver.openInputStream(uri)` in `VeniceForgeApp.kt:105-108`.

### Image output
Generated/edited images are written to `context.cacheDir` (`ImageViewModel.kt:111-115`). The resulting URI is built with `Uri.fromFile(file)`. There is **no `FileProvider`**, which is acceptable because the URI is consumed only inside the same app.

### Cache hygiene
There is **no cleanup policy** for generated image files. Each generation writes a new timestamped file. See `findings/architecture.md` ARCH-09.

### Findings
- **ARCH-08** (P2): `BitmapFactory.decodeFile` runs on the main thread in `ImageScreen.kt:180-186`.
- **ARCH-09** (P2): Generated image cache never cleaned up.
- **ARCH-13** (P2): Image-edit base64 decode runs on the main thread in `ImageViewModel.kt:110-116`.

---

## 5. Declared-but-Unused Jetpack Libraries

| Library | Declared in | Used in production? | Expected use per handoff |
|---------|-------------|---------------------|--------------------------|
| WorkManager | `app/build.gradle.kts:51` | No | Durable generation/retrieval jobs |
| DataStore Preferences | `app/build.gradle.kts:50` | No | Settings/preferences persistence |
| Media3 ExoPlayer | `app/build.gradle.kts:52` | No | Audio/video playback |

See `findings/architecture.md` ARCH-06 and ARCH-07.

---

## 6. Ledger Summary

Full ledger is in `findings/architecture.md`. This audit reviewed 58 production files (~4,038 lines) across `:app`, `:venice-sdk`, and the four `:core` modules.

**Severity summary:**
- P0: 0
- P1: 3 (ARCH-01, ARCH-02, ARCH-05)
- P2: 12
- P3: 2

**Most critical Android-architecture issues:**
1. ARCH-01 — ViewModels not retained across configuration changes.
2. ARCH-02 — Duplicate user message in chat request context.
3. ARCH-05 — Paid/mutating operations lack explicit approval.
4. ARCH-03 — DataServices/AppDatabase not application-singletons.
5. ARCH-06 — WorkManager declared but unused.
6. ARCH-11 — No process-death recovery for in-flight UI state.
7. ARCH-12 — Active conversation selection unstable across recreation.
