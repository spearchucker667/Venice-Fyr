# findings/baseline.md

Baseline validation findings for the Venice Fyr Android repository.

**Auditor scope:** BASELINE VALIDATION  
**Commit:** `1da3142` (clean tree)  
**Date:** 2026-08-15

---

## Ledger

| Path | Lines | Reviewed | Findings | Notes |
|------|-------|----------|----------|-------|
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/run-validation.sh` | 52 | Y | 0 | Auditor's own command script. |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/validation-raw.log` | 1,703 | Y | 4 | Raw Gradle output; four gate failures recorded. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt` | 288 | Y | 1 | Missing import for `ImageClient`. |
| `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt` | 91 | Y | 0 | Confirms target class exists in `sdk.image` package. |

---

## Findings

### BASELINE-01 | Missing `ImageClient` import breaks every Gradle gate

**Severity:** P1  
**Status:** CONFIRMED  
**Area:** Build / SDK public API  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`  
**Lines:** 34  
**Symbol:** `ImageClient`

**Evidence (code):**

```kotlin
// venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:31-34
/**
 * Entry point for image generation, editing, and upscaling capabilities.
 */
fun imageClient(): ImageClient = ImageClient(this)
```

No `import io.github.spearchucker667.veniceforge.sdk.image.ImageClient` is present in the file. In contrast, the adjacent methods fully qualify their return types:

```kotlin
// venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:39
fun audioClient(): io.github.spearchucker667.veniceforge.sdk.audio.AudioClient = io.github.spearchucker667.veniceforge.sdk.audio.AudioClient(this)

// venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:44
fun videoClient(): io.github.spearchucker667.veniceforge.sdk.video.VideoClient = io.github.spearchucker667.veniceforge.sdk.video.VideoClient(this)
```

The target class exists in the expected package:

```kotlin
// venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:15
class ImageClient(private val sdk: VeniceForgeSdk) {
```

**Evidence (build):**

```text
e: file:///Users/super_user/Projects/Venice%20Fyr/venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: file:///Users/super_user/Projects/Venice%20Fyr/venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
```

This error appears in the output of `./gradlew test`, `./gradlew lint`, `./gradlew :app:assembleDebug`, and `./gradlew :venice-sdk:assembleRelease`.

**Expected:** `:venice-sdk` compiles successfully in debug and release variants.  
**Actual:** `:venice-sdk:compileDebugKotlin` and `:venice-sdk:compileReleaseKotlin` fail with `Unresolved reference 'ImageClient'`, causing all dependent Gradle gates to fail.  
**Impact:**
- Unit tests cannot run.
- Lint cannot run.
- Debug APK cannot be built.
- Release AAR for `:venice-sdk` cannot be built.
- The image-generation public API entry point is unusable.

**Root cause:** A missing import/fully-qualified reference for `ImageClient` in `VeniceForgeSdk.kt`. The author likely intended consistency with `audioClient()`/`videoClient()` (fully qualified) but left `imageClient()` unqualified without an import.

**Related occurrences:**
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34` — the only source occurrence of the unqualified reference.
- The class is referenced correctly elsewhere:
  - `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:15` (definition)
  - `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt:17,19,35` (tests)
  - `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt:8,25` (app usage, currently blocked by the SDK compile failure)

**Venice reference:** N/A — this is a Kotlin compilation issue, not a wire-contract issue.  
**Android/Kotlin reference:** Kotlin requires imported or same-package types; `ImageClient` is in `io.github.spearchucker667.veniceforge.sdk.image`, while `VeniceForgeSdk` is in `io.github.spearchucker667.veniceforge.sdk`.  
**Remediation:** Add `import io.github.spearchucker667.veniceforge.sdk.image.ImageClient` to `VeniceForgeSdk.kt`, or change line 34 to use the fully-qualified form for consistency:

```kotlin
fun imageClient(): io.github.spearchucker667.veniceforge.sdk.image.ImageClient = io.github.spearchucker667.veniceforge.sdk.image.ImageClient(this)
```

**Tests required:** Re-run `./gradlew test`, `./gradlew lint`, `./gradlew :app:assembleDebug`, and `./gradlew :venice-sdk:assembleRelease` to confirm all gates pass. Existing `ImageClientTest` should be exercised once compilation is restored.  
**Compatibility impact:** Fixing the import is a source-only change with no public API or behavioral change.
