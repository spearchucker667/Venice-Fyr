# Build / Gradle / CI / Release Audit

**Auditor scope:** BUILD / GRADLE / CI / RELEASE  
**Audit date:** 2026-08-15  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `1da3142` (main), clean tree  
**Toolchain under review:** Gradle 9.5.0, AGP 9.3.0, Kotlin 2.3.21, KSP 2.3.11, JDK 17  
**Output files:** `16-CI-RELEASE-AUDIT.md`, `findings/build.md`

---

## 1. Executive Summary

The build system is a minimal but functional Gradle 9.5.0 + AGP 9.3.0 + Kotlin 2.3.21 setup with a version catalog and six modules. The toolchain versions are mutually compatible per public AGP/Gradle/KSP documentation, and the wrapper is correctly pinned. However, **release engineering is effectively absent**: there are no CI workflows, no release signing configuration, no ProGuard/R8 keep rules for serialization or Room, and the `:venice-sdk` consumer ProGuard file is empty. Several declared dependencies are unused, bloating the dependency surface and APK/AAR. These gaps make the current configuration unsuitable for a releasable artifact without significant hardening.

---

## 2. Ledger of Reviewed Files

| Path | Lines | Reviewed | Findings | Notes |
|------|-------|----------|----------|-------|
| `build.gradle.kts` | 5 | Y | 0 | Root plugin aliases only. |
| `settings.gradle.kts` | 23 | Y | 1 | Centralized repos, FAIL_ON_PROJECT_REPOS. |
| `gradle.properties` | 5 | Y | 1 | Basic JVM args/parallelism/caching. |
| `gradle/libs.versions.toml` | 58 | Y | 3 | Version catalog; several unused entries. |
| `gradle/wrapper/gradle-wrapper.properties` | 9 | Y | 0 | Pinned to 9.5.0-bin; validation enabled. |
| `app/build.gradle.kts` | 68 | Y | 6 | Release type lacks signing/keep rules; unused deps. |
| `venice-sdk/build.gradle.kts` | 29 | Y | 2 | Empty consumer ProGuard; no keep rules. |
| `venice-sdk/consumer-rules.pro` | 1 | Y | 1 | Contains only a comment. |
| `core/common/build.gradle.kts` | 17 | Y | 0 | Minimal library module. |
| `core/security/build.gradle.kts` | 18 | Y | 0 | Minimal library module. |
| `core/designsystem/build.gradle.kts` | 24 | Y | 0 | Compose library module. |
| `core/data/build.gradle.kts` | 56 | Y | 2 | Room/KSP configured; testInstrumentationRunner unused. |
| `scripts/bootstrap-desktop-source.sh` | 68 | Y | 0 | Robust read-only desktop mirror bootstrap. |
| `scripts/bootstrap-venice-api-docs.sh` | 77 | Y | 0 | Robust API docs bootstrap; records Swagger version. |
| `scripts/bootstrap-wrapper.sh` | 31 | Y | 0 | Generates wrapper from cached Gradle distribution. |
| `.github/CODEOWNERS` | 4 | Y | 0 | Single maintainer code ownership. |
| `.github/PULL_REQUEST_TEMPLATE.md` | 57 | Y | 0 | Comprehensive PR checklist. |
| `.github/workflows/*` | — | Y | 1 | **Directory does not exist.** |

**Total files reviewed:** 17 files + 1 missing workflow directory.  
**Total lines reviewed:** ~462 lines of build/script/config source.

---

## 3. Compatibility Verification

### 3.1 AGP / Gradle / Kotlin / KSP cluster

| Component | Declared | Compatibility evidence | Status |
|-----------|----------|----------------------|--------|
| AGP | 9.3.0 | AGP 9.3 requires Gradle ≥ 9.5.0 and JDK 17; latest stable per Android Developer docs/community release notes in 2026. | Compatible cluster |
| Gradle | 9.5.0 | Minimum required by AGP 9.3.0. | Compatible cluster |
| Kotlin | 2.3.21 | KGP 2.3.x line matches AGP 9 / Gradle 9.5 era. | Compatible cluster |
| KSP | 2.3.11 | KSP 2.3.11 released 2026-08-04; version numbering separated from Kotlin in 2.3.x line. | Compatible cluster |
| JDK | 17 | Required minimum for AGP 9.3.0. | Compatible cluster |

**References:**
- [Android Gradle plugin compatibility notes (community)](https://chunlog.jp/android-studio-agp-update/) — AGP 9.3 → Gradle 9.5.0 minimum.
- [Maven Central KSP plugin 2.3.11 listing](https://central.sonatype.com/artifact/com.google.devtools.ksp/com.google.devtools.ksp.gradle.plugin).
- [KSP 2.3.11 release notes (community)](https://newland435.tistory.com/127) — KSP 2.3.x version split from Kotlin.

### 3.2 SDK / compile / target levels

| Module | compileSdk | minSdk | targetSdk | Notes |
|--------|-----------|--------|-----------|-------|
| `:app` | 37 | 26 | 37 | Matches AGP 9.3 max API level 37 per docs. |
| `:venice-sdk` | 37 | 26 | N/A (library) | Consistent. |
| `:core:common` | 37 | 26 | N/A | Consistent. |
| `:core:security` | 37 | 26 | N/A | Consistent. |
| `:core:designsystem` | 37 | 26 | N/A | Consistent. |
| `:core:data` | 37 | 26 | N/A | Consistent. |

All modules align on `compileSdk = 37`, `minSdk = 26`, and Java 17 source/target compatibility. No module declares `targetSdk` except `:app`, which is correct for libraries.

---

## 4. Key Findings (condensed)

| ID | Severity | Status | Title |
|----|----------|--------|-------|
| BUILD-01 | P0 | CONFIRMED | No CI/CD workflows; release validation is entirely manual |
| BUILD-02 | P1 | CONFIRMED | Release R8 enabled but no ProGuard keep rules for serialization/Room/OkHttp |
| BUILD-03 | P1 | CONFIRMED | No release signing configuration in `:app` |
| BUILD-04 | P2 | CONFIRMED | Multiple declared-but-unused dependencies in `:app` and version catalog |
| BUILD-05 | P2 | CONFIRMED | `:venice-sdk` consumer-rules.pro is empty; SDK consumers lack keep rules |
| BUILD-06 | P2 | CONFIRMED | No dependency verification metadata or lock files for reproducible builds |
| BUILD-07 | P3 | CONFIRMED | Hardcoded `versionCode`/`versionName`; no CI-driven versioning |
| BUILD-08 | P3 | CONFIRMED | `testInstrumentationRunner` declared but no `androidTest` sources exist |

Full evidence, root cause, and remediation for each finding are in `findings/build.md`.

---

## 5. Module-by-Module Dependency Usage Summary

### `:app`
- **Used:** `:venice-sdk`, `:core:*`, `core-ktx`, `activity-compose`, `lifecycle-viewmodel-compose`, Compose BOM/UI/Material3/Foundation, JUnit, coroutines, Robolectric, `test-core`, Room testing (for in-memory DB in `ChatViewModelTest`), `sqlite` (transitively via Room).
- **Declared but unused in source:** `androidx.datastore:preferences`, `androidx.work:work-runtime`, `androidx.media3:exoplayer`, `androidx.lifecycle:lifecycle-runtime-compose`, `androidx.test.ext:junit`, `androidx.sqlite` (direct usage not found; Room pulls its own sqlite artifact).

### `:venice-sdk`
- **Used:** `:core:common`, OkHttp BOM/OkHttp, kotlinx.coroutines.core, kotlinx.serialization.json, JUnit/coroutines-test for tests.
- **Declared but unused:** `okhttp-logging-interceptor` is declared in the catalog but not imported anywhere.

### `:core:data`
- **Used:** `:core:common`, Room runtime/ktx/compiler (KSP), kotlinx.coroutines.core, JUnit/Robolectric/test-core/Room testing/sqlite/coroutines-test for tests.
- **Note:** `testInstrumentationRunner` is configured but there are no `androidTest` sources.

### `:core:designsystem`
- **Used:** Compose BOM/UI/Material3/Foundation/tooling.
- **Clean.**

### `:core:common`, `:core:security`
- **Used:** JDK APIs only; JUnit in tests.
- **Clean.**

---

## 6. Reproducibility & Supply Chain

- **No `gradle/verification-metadata.xml`** or dependency locking is configured.
- **No CI wrapper validation** (e.g., `gradle/wrapper-validation-action`) because no workflows exist.
- Wrapper `distributionUrl` uses HTTPS with `validateDistributionUrl=true`, which is good.
- `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts` prevents modules from adding unvetted repositories.
- `gradle.properties` enables build cache and parallel execution but does not enable configuration cache.

---

## 7. Release Readiness Assessment

| Requirement | State | Risk |
|-------------|-------|------|
| CI pipeline | Missing | P0 — no automated validation |
| Release signing | Missing | P1 — cannot produce signed release APK/AAB |
| R8/ProGuard keep rules | Missing | P1 — release crashes likely |
| SDK consumer ProGuard rules | Missing | P1 — SDK consumers will crash/serialize incorrectly |
| Dependency verification | Missing | P2 — supply-chain reproducibility weak |
| Versioning automation | Missing | P3 — manual release bookkeeping |
| Reproducible build config | Partial | P2 — wrapper pinned, but no locks |

**Verdict:** The build system is adequate for local development and debug builds, but it is **not release-ready**. A signed, minified release build of `:app` or a consumed `:venice-sdk` AAR is expected to fail or misbehave at runtime due to missing keep rules, and there is no automation to catch regressions.

---

## 8. Recommendations (high-level)

1. Add GitHub Actions workflows for PR validation (`test`, `lint`, `:app:assembleDebug`, `:venice-sdk:assembleRelease`) and wrapper validation.
2. Add a release signing configuration that reads credentials from environment/secrets, never from committed files.
3. Add ProGuard/R8 keep rules for kotlinx.serialization, Room, and OkHttp in `:app` and `:venice-sdk`.
4. Populate `venice-sdk/consumer-rules.pro` with SDK-specific keep rules.
5. Remove or move to `debugImplementation` declared-but-unused dependencies (`media3`, `datastore`, `work-runtime`, `lifecycle-runtime-compose`, `okhttp-logging`, `media3-ui-compose`).
6. Introduce dependency locking or `gradle/verification-metadata.xml` for reproducible builds.
7. Move `versionCode`/`versionName` to a CI-driven source (e.g., `version.properties` or environment) for release builds.

---

*End of `16-CI-RELEASE-AUDIT.md`.*
