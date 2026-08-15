# 19-VALIDATION-RESULTS.md

Baseline validation report for the Venice Fyr Android repository.

**Auditor scope:** BASELINE VALIDATION — run Gradle test/lint/assemble gates and record results.  
**Repo root:** `/Users/super_user/Projects/Venice Fyr`  
**Branch:** `main`  
**Commit:** `1da3142` (clean tree)  
**Date:** 2026-08-15

---

## Environment

| Variable | Value |
|----------|-------|
| `JAVA_HOME` | `/opt/homebrew/opt/openjdk@17` |
| `ANDROID_HOME` | `/opt/homebrew/share/android-commandlinetools` (discovered locally; required because no `local.properties` exists) |
| JDK version | OpenJDK 17.0.20 |
| Gradle version | 9.5.0 |
| Kotlin (Gradle daemon) | 2.3.20 |
| AGP | 9.3.0 |
| Android SDK platforms | android-37.0 |
| Android SDK build-tools | 36.0.0 |

The first validation attempt failed because `ANDROID_HOME` was unset and the project has no `local.properties` file. The second attempt set `ANDROID_HOME` to the command-line tools installation found under `/opt/homebrew/share/android-commandlinetools`.

---

## Commands executed

All commands were run sequentially from the repo root via `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/run-validation.sh`.

| # | Command | Exit code | Status | Time (reported) |
|---|---------|-----------|--------|----------------|
| 1 | `java -version` | 0 | VERIFIED | — |
| 2 | `./gradlew --version` | 0 | VERIFIED | — |
| 3 | `./gradlew test` | 1 | FAILED | 5s |
| 4 | `./gradlew lint` | 1 | FAILED | 5s |
| 5 | `./gradlew :app:assembleDebug` | 1 | FAILED | 4s |
| 6 | `./gradlew :venice-sdk:assembleRelease` | 1 | FAILED | 2s |
| 7 | `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | 0 | VERIFIED | 1s |

**Summary:** 4 of 6 Gradle gates failed. All four failures share the same root cause: `:venice-sdk:compileDebugKotlin` (or `compileReleaseKotlin`) cannot resolve `ImageClient` in `VeniceForgeSdk.kt:34`.

---

## Failure details

### Common failure across `test`, `lint`, `:app:assembleDebug`, and `:venice-sdk:assembleRelease`

```text
e: file:///Users/super_user/Projects/Venice%20Fyr/venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: file:///Users/super_user/Projects/Venice%20Fyr/venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
```

The facade class `VeniceForgeSdk` declares:

```kotlin
// venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:34
fun imageClient(): ImageClient = ImageClient(this)
```

but does not import `io.github.spearchucker667.veniceforge.sdk.image.ImageClient`. The sibling `audioClient()` and `videoClient()` methods use fully-qualified names; `imageClient()` uses the unqualified name without an import. The class exists at:

```text
venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:15
class ImageClient(private val sdk: VeniceForgeSdk)
```

Because the SDK module fails to compile, every gate that transitively needs it also fails.

### Per-command excerpts

#### `./gradlew test`
```text
> Task :venice-sdk:compileDebugKotlin FAILED
e: .../VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: .../VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
BUILD FAILED in 5s
EXIT_CODE_TEST: 1
```

#### `./gradlew lint`
```text
> Task :venice-sdk:compileDebugKotlin FAILED
e: .../VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: .../VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
BUILD FAILED in 5s
EXIT_CODE_LINT: 1
```

#### `./gradlew :app:assembleDebug`
```text
> Task :venice-sdk:compileDebugKotlin FAILED
e: .../VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: .../VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
BUILD FAILED in 4s
EXIT_CODE_APP_DEBUG: 1
```

#### `./gradlew :venice-sdk:assembleRelease`
```text
> Task :venice-sdk:compileReleaseKotlin FAILED
e: .../VeniceForgeSdk.kt:34:24 Unresolved reference 'ImageClient'.
e: .../VeniceForgeSdk.kt:34:38 Unresolved reference 'ImageClient'.
BUILD FAILED in 2s
EXIT_CODE_SDK_RELEASE: 1
```

---

## Dependency report summary

`:app:dependencies --configuration debugRuntimeClasspath` completed successfully. Key runtime dependencies observed:

- Kotlin stdlib 2.3.21
- AndroidX Compose BOM 2026.06.00 (Compose UI 1.11.3, Material3 1.4.0)
- AndroidX Activity 1.13.0, Core 1.18.0, Lifecycle 2.11.0
- AndroidX Room 2.7.0
- AndroidX DataStore 1.2.1
- AndroidX Work 2.11.2
- AndroidX Media3 ExoPlayer 1.10.1
- OkHttp (via `:venice-sdk` and `:core:data`)
- Kotlinx Serialization, Coroutines 1.11.0

No explicit dependency conflicts or duplicate-class warnings were emitted in this report. Gradle emitted an informational note:

```text
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.5.0/userguide/configuration_cache_enabling.html
```

---

## Raw log

Full command output is preserved in:

```text
docs/audits/venice-fyr-exhaustive-audit-2026-08-15/validation-raw.log
```

---

## Conclusion

The repository currently does **not** pass its baseline Gradle gates. The single blocking defect is a missing import for `ImageClient` in `VeniceForgeSdk.kt`. Once that import is added (or the call is fully qualified), the gates should be re-run to confirm no secondary compilation or test failures exist.
