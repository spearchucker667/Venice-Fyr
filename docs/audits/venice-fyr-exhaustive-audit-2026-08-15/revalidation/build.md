# Build / Gradle / CI / Release Revalidation

**Scope:** P0/P1 build and baseline findings from the 2026-08-15 exhaustive audit.
**Worktree:** `/Users/super_user/Projects/Venice Fyr`
**Local main:** `ee2cd7a` (origin/main, clean except the coordinator’s one-line compile fix).
**Methodology:** Static/read-only analysis only; no Gradle commands executed per team rules. Sources inspected include the current worktree, official Android/kotlinx.serialization documentation, and the Room 2.7.0 AAR.

---

## Summary table

| ID | Original severity/status | Disposition | Corrected severity/status |
|----|--------------------------|-------------|---------------------------|
| BASELINE-01 | P1 CONFIRMED (reclassified P0 CONFIRMED in `08-P0-FINDINGS.md`) | VALID | P0 RESOLVED |
| BUILD-01 | P0 CONFIRMED | VALID | P0 OPEN |
| BUILD-02 | P1 CONFIRMED | RECLASSIFIED | P2 NEEDS_REPRODUCTION |
| BUILD-03 | P1 CONFIRMED | VALID | P1 OPEN |
| BUILD-05 | P1 CONFIRMED (downgraded to P2 in `16-CI-RELEASE-AUDIT.md`) | RECLASSIFIED | P2 NEEDS_REPRODUCTION |

---

## BASELINE-01 | Missing `ImageClient` import breaks every Gradle gate

**Original severity/status:** P1 CONFIRMED in `findings/baseline.md`; reclassified to **P0 CONFIRMED** in `08-P0-FINDINGS.md`.
**Disposition:** VALID
**Corrected severity/status:** P0 RESOLVED

### Source evidence (current worktree)

`venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt:3` now contains the missing import:

```kotlin
import io.github.spearchucker667.veniceforge.sdk.image.ImageClient
```

Line 35 uses the imported symbol:

```kotlin
fun imageClient(): ImageClient = ImageClient(this)
```

The target class exists in `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt:15`.

### Spec/contract evidence

Kotlin requires a type to be imported or in the same package. `VeniceForgeSdk` is in `io.github.spearchucker667.veniceforge.sdk`; `ImageClient` is in `io.github.spearchucker667.veniceforge.sdk.image`.

### Why the original finding was right/wrong

The original report was correct: the unqualified `ImageClient` reference could not resolve without an import or fully-qualified name, blocking compilation of `:venice-sdk` and every downstream Gradle gate. The coordinator has since applied the one-line import fix, so the issue is resolved in the current worktree.

### Correct remediation

No further source change is required. The import added by the coordinator is the correct fix.

### Tests required

GATES RUNNER should execute:

```bash
./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease
```

Existing `ImageClientTest` should be exercised once compilation is restored.

---

## BUILD-01 | No CI/CD workflows exist

**Original severity/status:** P0 CONFIRMED
**Disposition:** VALID
**Corrected severity/status:** P0 OPEN

### Source evidence (current worktree)

```text
$ find "/Users/super_user/Projects/Venice Fyr/.github" -type f
/Users/super_user/Projects/Venice Fyr/.github/CODEOWNERS
/Users/super_user/Projects/Venice Fyr/.github/PULL_REQUEST_TEMPLATE.md
/Users/super_user/Projects/Venice Fyr/.github/ISSUE_TEMPLATE/feature_request.yml
/Users/super_user/Projects/Venice Fyr/.github/ISSUE_TEMPLATE/bug_report.yml
/Users/super_user/Projects/Venice Fyr/.github/ISSUE_TEMPLATE/config.yml
/Users/super_user/Projects/Venice Fyr/.github/ISSUE_TEMPLATE/documentation.yml
```

There is no `.github/workflows/` directory. `docs/RELEASE_CHECKLIST.md:16-19` still lists manual `./gradlew` commands as the release gate.

### Spec/contract evidence

A native Android client + reusable SDK should have automated PR and release workflows (test, lint, assembleDebug, assembleRelease, wrapper validation). See GitHub Actions documentation for Android CI.

### Why the original finding was right/wrong

The finding remains valid. No automation has been added; release validation is entirely manual.

### Correct remediation

Create `.github/workflows/pr.yml` running the standard gate on JDK 17 and include `gradle/wrapper-validation-action`. Add a release workflow that signs artifacts via GitHub secrets.

### Tests required

Verify the workflow runs green on a PR branch and that wrapper validation catches tampering.

---

## BUILD-02 | Release build enables R8 but provides no keep rules

**Original severity/status:** P1 CONFIRMED
**Disposition:** RECLASSIFIED
**Corrected severity/status:** P2 NEEDS_REPRODUCTION

### Source evidence (current worktree)

`app/build.gradle.kts:25-30`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```

There is no `proguardFiles(...)` call and no project-level `proguard-rules.pro` reference. `app/build.gradle.kts` therefore omits the default Android keep-rule file.

### Spec/contract evidence

1. Official Android docs state that a release build type should declare:

   ```kotlin
   proguardFiles(
       getDefaultProguardFile("proguard-android-optimize.txt"),
       "proguard-rules.pro"
   )
   ```

   [Shrink, obfuscate, and optimize your app](https://developer.android.com/studio/build/shrink-code)

2. `kotlinx.serialization` ships bundled ProGuard rules:

   > "By default, proguard rules are supplied with the library. These rules keep serializers for *all* serializable classes that are retained after shrinking, so you don't need additional setup. However, these rules do not affect serializable classes if they have named companion objects."

   [Kotlin/kotlinx.serialization README — Android section](https://github.com/Kotlin/kotlinx.serialization/blob/master/README.md)

3. Room 2.7.0 (`room-runtime-android-2.7.0.aar`) ships `proguard.txt`:

   ```proguard
   -keep class * extends androidx.room.RoomDatabase { void <init>(); }
   -dontwarn androidx.room.paging.**
   -dontwarn androidx.lifecycle.LiveData
   ```

4. Inspection of `:venice-sdk` `@Serializable` classes shows **no named companion objects**; only a default unnamed `companion object {}` in `ChatRequest.kt:20`. The default kotlinx.serialization keep rules therefore apply.

### Why the original finding was right/wrong

The original report correctly identified a real configuration gap: the `:app` release build type does not declare `proguardFiles`, so it misses the default Android keep-rule file. However, the original claim that this will "likely" cause runtime crashes for kotlinx.serialization and Room is unproven because both libraries ship their own consumer keep rules, and the SDK’s serializable classes do not trigger the named-companion exception. The issue should be treated as a latent misconfiguration, not a proven P1 crash, until a minified release build actually fails.

### Correct remediation

1. Add the standard ProGuard file declaration to `:app` release:

   ```kotlin
   proguardFiles(
       getDefaultProguardFile("proguard-android-optimize.txt"),
       "proguard-rules.pro"
   )
   ```

2. Author targeted keep rules only if a release build smoke test reveals actual stripping (e.g., Compose, OkHttp, or custom reflection).

### Tests required

GATES RUNNER should execute `./gradlew :app:assembleRelease` and run release smoke tests (serialization round-trips, Room queries, UI navigation). If the build succeeds and the app runs, the severity stays P2; if it fails, escalate back to P1 with the concrete R8 error.

---

## BUILD-03 | No release signing configuration

**Original severity/status:** P1 CONFIRMED
**Disposition:** VALID
**Corrected severity/status:** P1 OPEN

### Source evidence (current worktree)

`app/build.gradle.kts:25-30` contains only:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```

There is no `signingConfigs` block and no `signingConfig = signingConfigs.getByName(...)` assignment. `docs/RELEASE_CHECKLIST.md:44-51` lists signing tasks as unchecked manual items.

### Spec/contract evidence

Android release builds must be signed with a keystore whose credentials are supplied via environment variables or a secure CI secret store, never committed. See [Sign your app](https://developer.android.com/studio/publish/app-signing).

### Why the original finding was right/wrong

The finding remains valid. The release build type is unsigned and cannot be distributed through Google Play or sideloading without ad-hoc manual signing.

### Correct remediation

Add a `release` signing config that reads `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from environment variables; configure CI to inject these secrets.

### Tests required

GATES RUNNER should run `./gradlew :app:assembleRelease` in CI and verify the resulting APK/AAB is signed with the expected certificate.

---

## BUILD-05 | `:venice-sdk` consumer ProGuard rules are empty

**Original severity/status:** P1 CONFIRMED in `findings/build.md` and `09-P1-FINDINGS.md`; downgraded to **P2 CONFIRMED** in `16-CI-RELEASE-AUDIT.md`.
**Disposition:** RECLASSIFIED
**Corrected severity/status:** P2 NEEDS_REPRODUCTION

### Source evidence (current worktree)

`venice-sdk/build.gradle.kts:12`:

```kotlin
consumerProguardFiles("consumer-rules.pro")
```

`venice-sdk/consumer-rules.pro:1`:

```proguard
# Venice Forge SDK currently requires no consumer-specific keep rules.
```

The SDK exposes many `@Serializable` data classes (e.g., `VeniceModel.kt:23`, `ChatRequest.kt`, `ImageModels.kt`, `AudioModels.kt`, `VideoModels.kt`, `ChatStreamChunk.kt`). None of these classes use **named** companion objects; `ChatRequest.kt:20` uses an unnamed default `companion object {}`.

### Spec/contract evidence

1. Android library documentation: libraries should ship consumer ProGuard rules for classes accessed via reflection or JNI. [Library consumer ProGuard rules](https://developer.android.com/studio/projects/android-library#Considerations)

2. `kotlinx.serialization` ships bundled rules that keep serializers for retained serializable classes, except when named companions are present. Since the SDK has no named companions, the bundled rules cover serialization needs.

3. Room ships consumer rules (see BUILD-02 evidence).

### Why the original finding was right/wrong

The original report correctly noted that the SDK’s `consumer-rules.pro` is empty, which is a contract gap for a reusable AAR. However, the original claim that "any consuming app with R8 enabled will likely crash" is unproven because:

- The SDK’s serialization requirements are covered by `kotlinx.serialization`’s bundled rules.
- No SDK class has a named companion object, so the known exception does not apply.
- There is no SDK reflection/JNI access that requires explicit keep rules.

Until a minimal consumer app with `isMinifyEnabled = true` demonstrates a runtime failure, this should be treated as a latent P2 gap rather than a proven P1 crash.

### Correct remediation

Populate `consumer-rules.pro` with conservative keep rules for the SDK’s public `@Serializable` surface as a defensive measure, and build a minimal R8-enabled consumer test:

```proguard
-keep @kotlinx.serialization.Serializable class io.github.spearchucker667.veniceforge.sdk.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class io.github.spearchucker667.veniceforge.sdk.** { *; }
```

If future SDK code adds named companions or reflection/JNI access, add corresponding rules.

### Tests required

Build a minimal consumer app that depends on `:venice-sdk`, enables `isMinifyEnabled = true`, and exercises serialization round-trips for chat, image, audio, and video models. If it crashes, capture the exact R8 error and escalate.

---

## Notes on other findings

- `BUILD-04`, `BUILD-06`–`BUILD-12` are P2/P3 and outside the requested P0/P1 revalidation scope.
- `ARCH-02` is noted in the coordinator’s state as a known FALSE finding, but it is an app/chat logic issue, not a build/CI finding, and is not revalidated here.
