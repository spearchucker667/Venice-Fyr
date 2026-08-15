# Documentation Drift — Detailed Findings

**Scope:** All documentation files listed in `17-DOCUMENTATION-DRIFT.md`, compared against the production Kotlin/Gradle source tree at `main @ 1da3142`.

**Classification:**
- **CONFIRMED** — directly evidenced by quoted source/spec/test.
- **INFERRED** — strongly implied by available evidence but not directly observed.
- **SUSPECTED** — plausible but needs further verification.
- **UNVERIFIED** — claim could not be checked in this read-only audit.

---

## DOC-01 | P1 | CONFIRMED

**Area:** SDK examples  
**Module:** `:venice-sdk` public API surface  
**File:** `docs/SDK_EXAMPLES.md`  
**Lines:** 71–75  
**Symbol:** `ChatStreamChunk.Delta.text`, `ChatStreamChunk.ToolCallDelta.arguments`

**Evidence:**
- `docs/SDK_EXAMPLES.md:71` reads `chunk.text?.let { print(it) }`.
- `docs/SDK_EXAMPLES.md:75` reads `println("Tool call #${chunk.index}: ${chunk.name} args: ${chunk.arguments}")`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt:7` declares `data class Delta(val index: Int, val textFragment: String?)`.
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt:8–13` declares `ToolCallDelta` with `val argumentsFragment: String?`, not `arguments`.

**Expected:** The example should reference `chunk.textFragment` and `chunk.argumentsFragment` to compile against the current SDK.

**Actual:** The example references `chunk.text` and `chunk.arguments`, which do not exist on the current sealed classes.

**Impact:** The primary SDK usage example will not compile and will mislead consumers integrating `:venice-sdk`.

**Root cause:** The `ChatStreamChunk` API was renamed (`text` → `textFragment`, `arguments` → `argumentsFragment`) after `docs/SDK_EXAMPLES.md` was written; the documentation was not updated.

**Related occurrences:** None found in other docs; `ChatScreen.kt` and `ChatViewModel.kt` use the SDK correctly and do not rely on the example.

**Venice reference:** N/A — Kotlin SDK API naming issue.

**Android/Kotlin reference:** Kotlin data class property names are part of the public API; examples must match them.

**Remediation:** Update `docs/SDK_EXAMPLES.md` lines 71 and 75 to use `textFragment` and `argumentsFragment`. Add a CI/static check that compiles snippets in docs if possible.

**Tests required:** None for docs, but verify the corrected snippet compiles against `:venice-sdk`.

**Compatibility impact:** Correcting the docs is safe; no API change.

---

## DOC-02 | P1 | CONFIRMED

**Area:** Feature parity status  
**Module:** `:app` / `:venice-sdk`  
**Files:** `docs/FEATURE_PARITY_MATRIX.md`, `CHANGELOG.md`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt`  
**Lines:** `docs/FEATURE_PARITY_MATRIX.md:7,10,15,17`; `CHANGELOG.md:17–20`; `FeatureCatalog.kt:23,26,31,33`

**Evidence:**
- `docs/FEATURE_PARITY_MATRIX.md:7` lists `chat` → Android now **Foundation**.
- `docs/FEATURE_PARITY_MATRIX.md:10` lists `image` → Android now **Foundation**.
- `docs/FEATURE_PARITY_MATRIX.md:15` lists `audio` → Android now **Foundation (SDK)**.
- `docs/FEATURE_PARITY_MATRIX.md:17` lists `video` → Android now **Foundation (SDK)**.
- `CHANGELOG.md:17` says "Image Studio foundation"; `CHANGELOG.md:19–20` say `:venice-sdk` Audio and Video clients added.
- `app/src/main/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalog.kt:23` lists `chat` → `AndroidPortStatus.SCAFFOLDED`.
- `FeatureCatalog.kt:26` lists `image` → `SCAFFOLDED`.
- `FeatureCatalog.kt:31` lists `audio` → `SCAFFOLDED`.
- `FeatureCatalog.kt:33` lists `video` → `SCAFFOLDED`.

**Expected:** A single source of truth for parity status. `FeatureCatalog.kt` is the runtime registry used by the app; the parity matrix and changelog should agree with it.

**Actual:** The matrix/changelog claim Foundation-level implementation for chat/image/audio/video, while the in-app feature registry marks them as `SCAFFOLDED`.

**Impact:** Contributors and users receive contradictory signals about which features are implemented. This undermines the parity matrix's authority and risks scope/expectation mismatches.

**Root cause:** The parity matrix and changelog were updated independently of the `FeatureCatalog` enum after milestone work landed.

**Related occurrences:** `FeatureCatalog.kt` statuses for `privacy`, `settings`, and `status` (`FOUNDATION`) agree with the matrix, so the drift is isolated to the generate/media features.

**Venice reference:** N/A.

**Android/Kotlin reference:** N/A.

**Remediation:** Reconcile the four features. If the working chat screen, image screen, and SDK audio/video clients are considered Foundation, update `FeatureCatalog.kt`. If they are still scaffolded, update the matrix and changelog. Document the chosen definition-of-done for each status.

**Tests required:** None; this is a documentation/registry consistency fix.

**Compatibility impact:** None.

---

## DOC-03 | P2 | CONFIRMED

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

## DOC-04 | P2 | CONFIRMED

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

## DOC-05 | P2 | CONFIRMED

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

## DOC-06 | P2 | CONFIRMED

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

## DOC-07 | P3 | CONFIRMED

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

## DOC-08 | P3 | CONFIRMED

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

## DOC-09 | P3 | CONFIRMED

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
