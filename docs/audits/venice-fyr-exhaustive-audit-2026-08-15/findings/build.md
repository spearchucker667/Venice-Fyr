# Build / Gradle / CI / Release Findings

**Scope:** BUILD / GRADLE / CI / RELEASE  
**Audit date:** 2026-08-15  
**Repository:** Venice-Fyr @ main (`1da3142`)

---

## BUILD-01 | P0 | CONFIRMED — No CI/CD workflows exist

| Field | Value |
|-------|-------|
| **Area** | CI/CD |
| **Module** | `.github/` |
| **File** | `.github/workflows/*` |
| **Lines** | N/A (directory absent) |
| **Symbol** | GitHub Actions workflows |
| **Evidence** | `find .github -type f` returns only `CODEOWNERS`, `PULL_REQUEST_TEMPLATE.md`, and issue templates. There is no `.github/workflows/` directory. `docs/RELEASE_CHECKLIST.md:16-19` lists manual `./gradlew` commands as the release gate. |
| **Expected** | A repository shipping a native Android client + reusable SDK should have automated PR and release workflows (test, lint, assembleDebug, assembleRelease, wrapper validation). |
| **Actual** | All validation is manual. No workflow files exist. |
| **Impact** | Every release relies on a developer running the exact right commands locally. Regressions are not caught automatically; no wrapper-validation gate; no artifact provenance. |
| **Root cause** | CI automation was not implemented. `docs/RELEASE_CHECKLIST.md:3` explicitly states it is "a review checklist, not proof that release automation/signing already exists." |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | GitHub Actions documentation for Android CI. |
| **Remediation** | Create `.github/workflows/pr.yml` running `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` on JDK 17, plus `gradle/wrapper-validation-action`. Add a release workflow that signs artifacts via GitHub secrets. |
| **Tests required** | Verify workflow runs green on a PR branch; verify wrapper validation catches tampering. |
| **Compatibility impact** | None directly; improves release reliability. |

---

## BUILD-02 | P1 | CONFIRMED — Release build enables R8 but provides no keep rules

| Field | Value |
|-------|-------|
| **Area** | Release build / R8 |
| **Module** | `:app` |
| **File** | `app/build.gradle.kts` |
| **Lines** | 26-29 |
| **Symbol** | `release { isMinifyEnabled = true; isShrinkResources = true }` |
| **Evidence** | ```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```<br>No `proguardFiles(...)` or `consumerProguardFiles(...)` is present in any `build.gradle.kts`. |
| **Expected** | When `isMinifyEnabled = true`, the build must include ProGuard/R8 keep rules for reflection/serialization (kotlinx.serialization), Room entities, and any libraries that require them. |
| **Actual** | Release type has minification and resource shrinking enabled, but relies solely on default Android rules. |
| **Impact** | Runtime crashes in release builds: kotlinx.serialization needs `@Serializable` classes kept; Room needs `@Entity`/`@Dao` classes kept and constructors preserved; OkHttp/retrofit-style reflection may break. |
| **Root cause** | Keep rules were never authored. |
| **Related occurrences** | `venice-sdk/consumer-rules.pro` is also empty (BUILD-05). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [kotlinx.serialization ProGuard rules](https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro); [Room ProGuard rules](https://developer.android.com/training/data-storage/room). |
| **Remediation** | Add `proguardFiles(getDefaultProguardFile(...), "proguard-rules.pro")` to `:app` and author rules for serialization, Room, OkHttp, and Compose. |
| **Tests required** | Run `./gradlew :app:assembleRelease` and execute release UI tests / serialization round-trips / Room queries. |
| **Compatibility impact** | Release APK behavior will differ from debug; must verify before any release. |

---

## BUILD-03 | P1 | CONFIRMED — No release signing configuration

| Field | Value |
|-------|-------|
| **Area** | Release signing |
| **Module** | `:app` |
| **File** | `app/build.gradle.kts` |
| **Lines** | 26-29 |
| **Symbol** | `release` build type |
| **Evidence** | The only release configuration is:<br>```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
}
```<br>No `signingConfigs` block, no `signingConfig = signingConfigs.getByName(...)` assignment. |
| **Expected** | Release builds must be signed with a keystore whose credentials are supplied via environment variables or a secure CI secret store, never committed. |
| **Actual** | Release build is unsigned; `docs/RELEASE_CHECKLIST.md:44-51` lists signing items as unchecked manual tasks. |
| **Impact** | Cannot distribute a release APK/AAB through Google Play or sideloading without ad-hoc signing. Any manual signing is error-prone and risks key exposure. |
| **Root cause** | Signing configuration not implemented. |
| **Related occurrences** | None. |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Sign your app](https://developer.android.com/studio/publish/app-signing). |
| **Remediation** | Add a `release` signing config reading `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` from environment; configure CI to inject secrets. |
| **Tests required** | Run `./gradlew :app:assembleRelease` in CI and verify APK is signed with expected certificate. |
| **Compatibility impact** | None. |

---

## BUILD-04 | P2 | CONFIRMED — Declared-but-unused dependencies bloat `:app` and catalog

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

## BUILD-05 | P1 | CONFIRMED — `:venice-sdk` consumer ProGuard rules are empty

| Field | Value |
|-------|-------|
| **Area** | SDK release / consumer keep rules |
| **Module** | `:venice-sdk` |
| **File** | `venice-sdk/build.gradle.kts`, `venice-sdk/consumer-rules.pro` |
| **Lines** | `venice-sdk/build.gradle.kts:12`; `consumer-rules.pro:1` |
| **Symbol** | `consumerProguardFiles("consumer-rules.pro")` |
| **Evidence** | `venice-sdk/build.gradle.kts:12` references `consumer-rules.pro`. The file contains only:<br>```
# Venice Forge SDK currently requires no consumer-specific keep rules.
```<br>Meanwhile `venice-sdk/src/main` contains many `@Serializable` data classes (e.g., `VeniceModel.kt:23`, `ChatRequest.kt`, `ImageModels.kt`, `AudioModels.kt`, `VideoModels.kt`). |
| **Expected** | A reusable SDK that exposes `@Serializable` models and OkHttp clients must ship consumer keep rules so consuming apps do not strip required classes. |
| **Actual** | Consumer rules file is empty; SDK consumers will rely on their own (possibly missing) rules. |
| **Impact** | Any consuming app with R8 enabled will likely crash at runtime when serializing/deserializing Venice API payloads or reflectively instantiating models. |
| **Root cause** | Keep rules were assumed unnecessary. |
| **Related occurrences** | `:app` release type also lacks keep rules (BUILD-02). |
| **Venice reference** | N/A |
| **Android/Kotlin reference** | [Library consumer ProGuard rules](https://developer.android.com/studio/projects/android-library#Considerations); kotlinx.serialization keep rules. |
| **Remediation** | Populate `consumer-rules.pro` with keep rules for `@Serializable` classes, companion objects, and any classes accessed via reflection. |
| **Tests required** | Build a minimal consumer app with `isMinifyEnabled = true` and verify Venice SDK serialization round-trips. |
| **Compatibility impact** | Corrects a latent runtime incompatibility for SDK consumers. |

---

## BUILD-06 | P2 | CONFIRMED — No dependency verification or lock files

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

## BUILD-07 | P3 | CONFIRMED — Hardcoded versionCode/versionName

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

## BUILD-08 | P2 | CONFIRMED — `testInstrumentationRunner` configured but no instrumentation tests exist

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

## BUILD-09 | P2 | INFERRED — No explicit Kotlin JVM target / toolchain

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

## BUILD-10 | P2 | CONFIRMED — `okhttp-logging-interceptor` cataloged but unused

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

## BUILD-11 | P2 | CONFIRMED — `media3-ui-compose` cataloged but unused

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

## BUILD-12 | P3 | CONFIRMED — `gradle.properties` omits configuration cache

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
