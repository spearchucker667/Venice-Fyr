# Unresolved Questions — Venice Fyr Android Exhaustive Audit

**Audit date:** 2026-08-15  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `1da3142`  
**Upstream API docs:** `veniceai/api-docs` @ `6e69346b`, `swagger.yaml` `info.version 20260814.194349`

This document collects every question the audit could not resolve through static analysis alone. It includes explicit gaps in runtime verification, items marked **SUSPECTED** or **INFERRED** in the findings files, and behavior of endpoints the SDK does not yet implement.

For each question, the evidence that would resolve it is listed. No item here should be treated as a confirmed defect unless independently verified.

---

## 1. Runtime and release behavior (unverified because the build was broken)

### Q-01 | Does the release build survive R8 minification?
- **Why unresolved:** The repo did not compile (BASELINE-01), so `./gradlew :app:assembleRelease` could not be executed. R8 is enabled (`isMinifyEnabled = true`) but no keep rules exist (BUILD-02, BUILD-05).
- **Evidence needed:** Successful release assembly followed by a smoke test that exercises `@Serializable` deserialization, Room queries, and OkHttp calls.
- **Related findings:** BUILD-02, BUILD-05.

### Q-02 | Does the release APK sign correctly and install?
- **Why unresolved:** No signing config exists (BUILD-03) and the release build could not be produced.
- **Evidence needed:** A signed release APK/AAB built in CI with a test keystore, installed on a device/emulator, and launched successfully.
- **Related findings:** BUILD-03.

### Q-03 | What is the actual cold-start time and ANR rate on a low-end device?
- **Why unresolved:** No instrumentation or profiling was performed. The static audit identified main-thread Keystore decryption (VM-05), bitmap decoding (APP-UI-007), and base64 decoding (VM-06), but cannot quantify their real-world impact.
- **Evidence needed:** Systrace/Perfetto capture on a representative device (e.g., API 26, 4 GB RAM) during first launch, chat submit, and image generation.
- **Related findings:** VM-05, APP-UI-007, VM-06.

---

## 2. Live Venice API behavior (unverified because no API key was used)

### Q-04 | Do the implemented SDK methods actually succeed against the live Venice API?
- **Why unresolved:** All SDK tests use mocked OkHttp responses or inline fixtures. No live call was made with a real API key.
- **Evidence needed:** Integration test suite run against `api.venice.ai` with a valid key, covering `listModels`, `streamChat`, `image/generate`, `image/edit`, `video/queue`, `video/retrieve`, and `audio/speech`.
- **Related findings:** CHAT-01, IMG-01, VID-01..VID-03, AUD-01..AUD-05.

### Q-05 | What are the actual rate-limit headers and retry semantics?
- **Why unresolved:** `RateLimitInfo` parsing was reviewed against documentation, but no live 429 response was observed. The fallback from `retry-after` to `x-ratelimit-reset-requests` may treat an epoch timestamp as a duration (SDK-CORE-13).
- **Evidence needed:** Capture live 429 responses and confirm the units of `x-ratelimit-reset-requests` and `x-ratelimit-reset-tokens`.
- **Related findings:** SDK-CORE-13.

### Q-06 | Does Venice reject `safe_mode` under `/chat/completions` `venice_parameters`?
- **Why unresolved:** The swagger schema for `/chat/completions` does not list `safe_mode` under `venice_parameters`, but the SDK serializes it. Whether Venice validates `additionalProperties` is unknown without a live request (CHAT-06, SUSPECTED).
- **Evidence needed:** Send a chat request with `venice_parameters.safe_mode=false` and record the HTTP status/body.
- **Related findings:** CHAT-06.

### Q-07 | Does `/audio/speech` accept or ignore `safe_mode`?
- **Why unresolved:** `SpeechRequest` includes `safeMode`, but the swagger schema for `/audio/speech` does not list it (AUD-03).
- **Evidence needed:** Send two identical TTS requests, one with `safe_mode=false` and one without, and compare responses/status codes.
- **Related findings:** AUD-03.

### Q-08 | What is the exact behavior of `/video/retrieve` for VPS-backed models?
- **Why unresolved:** The swagger description says VPS-backed queue responses return a `download_url` and that retrieve returns JSON status only, but no live VPS job was polled (VID-03).
- **Evidence needed:** Queue a video generation with a VPS-backed model, poll `/video/retrieve`, and capture the JSON body when `status == COMPLETED`.
- **Related findings:** VID-03.

### Q-09 | Do the unimplemented endpoints (`/image/background-remove`, `/image/styles`, `/images/generations`, `/video/transcriptions`, `/audio/transcriptions`, queued audio) behave as documented?
- **Why unresolved:** The SDK does not implement these methods, so no local code was exercised against them.
- **Evidence needed:** Manual or scripted calls to each endpoint using the documented request/response shapes.
- **Related findings:** IMG-02, IMG-03, IMG-04, VID-04, AUD-01, AUD-02.

---

## 3. Swagger-versioned behavior of endpoints the SDK does not implement

### Q-10 | What is the current request/response shape for `/image/background-remove`?
- **Why unresolved:** The endpoint constant exists but no SDK method exists. The swagger schema describes JSON and multipart variants returning PNG binary, but the exact field names and validation rules were not exercised.
- **Evidence needed:** Recorded request/response against the live endpoint or an authoritative fixture from `veniceai/api-docs`.
- **Related findings:** IMG-02.

### Q-11 | What fields does `/video/quote` actually require?
- **Why unresolved:** `VideoClient` has no `quote` method. The swagger schema documents `QuoteVideoRequest`, but required-field semantics may differ from the queue request.
- **Evidence needed:** Live `POST /video/quote` calls with minimal and full payloads.
- **Related findings:** VID-02.

### Q-12 | What is the exact multipart format for `/audio/transcriptions`?
- **Why unresolved:** The SDK has no transcription method. The swagger schema describes `file`, `model`, `response_format`, `timestamps`, and `language`, but boundary and encoding requirements are best verified live.
- **Evidence needed:** Successful live transcription request captured with a tool such as mitmproxy or OkHttp logging.
- **Related findings:** AUD-02.

### Q-13 | What does the `/models/traits` response contain for non-text types?
- **Why unresolved:** `CapabilitiesRepository` does not send the `type` query parameter, so only the default `text` traits have been observed statically (SDK-CORE-01).
- **Evidence needed:** Call `/models/traits?type=image`, `audio`, `video`, `embedding` with a valid key and compare trait keys/values.
- **Related findings:** SDK-CORE-01.

---

## 4. Items marked SUSPECTED or INFERRED in the findings

### Q-14 | Is `safe_mode` under `/chat/completions` `venice_parameters` silently ignored or rejected?
- **Status from findings:** SUSPECTED (CHAT-06)
- **Why unresolved:** Schema says it is absent; SDK sends it. Live behavior is unknown.
- **Evidence needed:** See Q-06.

### Q-15 | Are the navigation drawer touch targets below 48 × 48 dp?
- **Status from findings:** INFERRED (APP-UI-017)
- **Why unresolved:** No layout measurement or accessibility scanner was run. The `OutlinedButton` default minimum is 40 dp and width is text-driven.
- **Evidence needed:** Run Android Accessibility Scanner or Compose UI test with `SemanticsMatcher` on the Menu button and model picker.
- **Related findings:** APP-UI-017.

### Q-16 | Does the current Kotlin compilation target match the Java 17 target?
- **Status from findings:** INFERRED (BUILD-09)
- **Why unresolved:** Modules set `compileOptions` to Java 17 but do not set `kotlinOptions.jvmTarget` or `jvmToolchain(17)`. Inferred target is usually correct, but not guaranteed with future toolchain versions.
- **Evidence needed:** Build scan or `javap -v` on compiled `.class` files to confirm major version 61.
- **Related findings:** BUILD-09.

### Q-17 | Should `VeniceForgeSdk.baseUrl()`, `userAgent()`, and `httpClient()` be `internal`?
- **Status from findings:** INFERRED (SDK-CORE-16)
- **Why unresolved:** The accessors are public, but no external consumer was reviewed. The impact depends on whether downstream modules or SDK users depend on them.
- **Evidence needed:** API compatibility report from `./gradlew :venice-sdk:apiCheck` (if configured) or consumer usage search across all modules.
- **Related findings:** SDK-CORE-16.

### Q-18 | Does the `Redactor` regex miss non-`sk`/`vn` Venice key formats?
- **Status from findings:** INFERRED (SEC-08)
- **Why unresolved:** The regex only matches `sk-*` and `vn-*` prefixes. The full set of documented Venice key formats was not confirmed from `venice-api-docs`.
- **Evidence needed:** Review `venice-api-docs` security/auth sections and sample keys; update regex and parameterized tests.
- **Related findings:** SEC-08.

### Q-19 | Is the 32-character SHA-256 truncation in `SecureSecretStore` a practical weakness?
- **Status from findings:** INFERRED (SEC-10)
- **Why unresolved:** Truncation to 128 bits reduces the security margin but collision risk for a small number of profiles is likely negligible. No attack model was evaluated.
- **Evidence needed:** Cryptographic review and migration test confirming whether existing stored keys must be re-encrypted.
- **Related findings:** SEC-10.

### Q-20 | Does image generation default to `safe_mode=true` and blur unexpected content?
- **Status from findings:** INFERRED (VM-14)
- **Why unresolved:** The ViewModel omits `safeMode`, and the swagger default is `true`. The actual API behavior for omitted `safe_mode` was not verified live.
- **Evidence needed:** Generate an image without explicit `safe_mode` and inspect response headers/body; compare with `safe_mode=false`.
- **Related findings:** VM-14.

---

## 5. Device/emulator and instrumentation gaps

### Q-21 | Do ViewModels survive configuration change after the `remember` fix?
- **Why unresolved:** No instrumentation test or emulator run was performed. The fix (use `viewModel()`) is standard, but the custom factory keyed by `profileId` needs validation.
- **Evidence needed:** Instrumented rotation test asserting the same `ChatViewModel`/`ImageViewModel` instance and `onCleared()` called exactly once on Activity finish.
- **Related findings:** APP-UI-001, ARCH-01.

### Q-22 | Does process-death recovery restore the active conversation and generated image URI?
- **Why unresolved:** No `SavedStateHandle` integration exists yet, and no process-death instrumentation test was run (VM-07, VM-08, VM-13).
- **Evidence needed:** Instrumented test using `recreate()` or system-initiated process death; assert restored `conversationId` and `resultImageUri`.
- **Related findings:** VM-07, VM-08, VM-13.

### Q-23 | Does the app request or consume excessive background battery?
- **Why unresolved:** `collectAsState` is used instead of `collectAsStateWithLifecycle` (APP-UI-003), but actual background power draw was not measured.
- **Evidence needed:** Android Studio Energy Profiler or `dumpsys batterystats` after backgrounding the app during an active stream.
- **Related findings:** APP-UI-003.

### Q-24 | Is the Room database actually corrupted or readable on a rooted device?
- **Why unresolved:** The database is plaintext (DATA-09), but no extraction or encryption verification was performed.
- **Evidence needed:** Pull `venice_forge.db` from a rooted/emulated device and inspect message content; after encryption migration, confirm ciphertext.
- **Related findings:** DATA-09.

### Q-25 | Do strict-mode disk-read/network-on-main violations fire?
- **Why unresolved:** StrictMode was not enabled during the audit. The static findings predict violations, but runtime confirmation is absent.
- **Evidence needed:** Enable `StrictMode.ThreadPolicy` with `detectDiskReads()` and `detectNetwork()` during chat submit and image generation.
- **Related findings:** VM-05, VM-06, APP-UI-007, ARCH-08, ARCH-13.

---

## 6. Security and privacy behavior not exercised

### Q-26 | Can a memory dump or screenshot capture the API key from `ConfigScreen` state?
- **Why unresolved:** The key is held in Compose `mutableStateOf` (SEC-05). No memory analysis or screenshot policy test was run.
- **Evidence needed:** Heap dump inspection and `FLAG_SECURE` window screenshot test.
- **Related findings:** SEC-05.

### Q-27 | Does `SecureSecretStore` behave correctly across biometric/lock-screen changes?
- **Why unresolved:** Keystore keys can be invalidated by biometric enrollment or lock-screen changes. No test simulated this.
- **Evidence needed:** Robolectric or device test that rotates biometric enrollment and attempts decryption.
- **Related findings:** SEC-04.

### Q-28 | Are there any production log sinks that bypass `Redactor`?
- **Why unresolved:** `Redactor` is unwired (SEC-01). No runtime logging audit was performed because the app has no explicit `Log.` calls, but future diagnostics or third-party libraries could leak data.
- **Evidence needed:** Static and runtime audit of all logging/diagnostic paths after `Redactor` integration.
- **Related findings:** SEC-01.

---

## Top 5 unresolved questions

1. **Release/R8 survival (Q-01):** The build did not compile, so no release artifact or minified smoke test could be produced. This blocks any distribution.
2. **Live chat `safe_mode` semantics (Q-06):** The SDK serializes `safe_mode` under `/chat/completions` `venice_parameters`, but the schema does not list it. A live request is needed to know if Venice ignores or rejects it.
3. **VPS-backed video retrieve behavior (Q-08):** The current retrieve logic cannot represent a completed VPS job because it only branches on `Content-Type`, not `status` and `download_url`.
4. **Main-thread crypto/ANR rate (Q-03):** Keystore decryption and bitmap/base64 decoding run on the main thread, but actual ANR frequency on real devices is unknown.
5. **Process-death recovery (Q-22):** No `SavedStateHandle` integration or instrumentation testing was done, so recovery of active conversation and generated media after process death is unverified.
