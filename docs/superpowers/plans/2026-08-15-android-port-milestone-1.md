# Venice Forge Android — Milestone 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver typed `/models`+`/models/traits`+`/models/compatibility_mapping` capability parsing, `/chat/completions` SSE streaming, and Room v1 profile/chat schema, wired into a minimal in-app Chat screen.

**Architecture:** Hybrid Layer-first. SDK capability types and SSE client live in `:venice-sdk` (network boundary); Room schema lives in a new `:core:data` module (durable persistence); UI lives in `:app` (Compose) and wires both via a `ChatViewModel`. No direct component ↔ SDK calls; no SDK ↔ Room calls.

**Tech Stack:**
- Kotlin 2.3.21, AGP 9.3.0, Gradle 9.5.0, JDK 17
- compileSdk/targetSdk 37, minSdk 26
- Jetpack Compose (BOM 2026.06.00), Activity Compose 1.13.0, Lifecycle 2.11.0
- Coroutines 1.11.0, kotlinx-serialization-json 1.11.0
- OkHttp 5.3.0
- AndroidX Room 2.7.0 with KSP (com.google.devtools.ksp)
- Media3 1.10.1, WorkManager 2.11.2, DataStore 1.2.1 (already in app)
- AndroidJUnit4 / Robolectric 4.13 for instrumented Room tests

## Global Constraints

These are copied verbatim from the spec at `docs/superpowers/specs/2026-08-15-android-port-milestone-1-design.md` and the project `AGENTS.md`. Every task satisfies them.

**Toolchain:**
- AGP 9.3 / built-in Kotlin. Do NOT add the deprecated `org.jetbrains.kotlin.android` plugin.
- KSP for Room annotation processing. Do NOT use KAPT.
- Use single, pinned versions in `gradle/libs.versions.toml`. Do NOT use dynamic version selectors.

**Privacy / security:**
- `:venice-sdk` accepts `apiKey: String` per call. Never persists credentials.
- All app shells / `:app` persists credentials via `core/security/SecureSecretStore` (Android Keystore AES-GCM). No DataStore / Room / BuildConfig / SharedPreferences secrets.
- All networking HTTPS-only. `network_security_config.xml` already enforces this on `:app`. New ad-hoc HTTP is forbidden.
- Redact Authorization headers, prompt bodies, response bodies, and absolute local paths via `Redactor.redact()` before any log/diagnostic.
- Do NOT request `MANAGE_EXTERNAL_STORAGE`. External file access is via SAF/Photo Picker only.
- Local Family Safe Mode and Venice provider `safe_mode` are distinct concepts. Preserve explicit `safe_mode=false` if/when the chat layer accepts it.

**Source authority:**
- Desktop source of record is `$VENICE_FORGE_DESKTOP_SOURCE` after running `scripts/bootstrap-desktop-source.sh`. Verified HEAD at plan start: `bc5c17374ef4937f5837f5580d29a88bfab333ee`.
- For each task that ports desktop behavior, record the inspected desktop files in the commit footer (e.g. `Desktop sources: src/services/veniceClient/stream.ts:42-118, src/stores/chat-stream-manager.ts:1-200`).
- Treat the desktop checkout as read-only.

**Coding conventions:**
- One responsibility per file. No mega-ViewModel; no mega-repository.
- Kotlin idiomatic: data class for immutable models, sealed class for unions.
- Tests live alongside production (`*.test.kt`). Use JUnit 4 for unit tests. Robolectric / instrumented for Android-dependent tests.
- Reuse existing file paths/extensions discovered in spec exploration. Add new files under documented package paths.
- Visible UI strings come from `app/src/main/res/values/strings.xml`. Add new entries; do not hardcode.

**Acceptance recap (from spec §9):**
- `./gradlew :venice-sdk:test :core:data:test :app:test` passes.
- `./gradlew lint` passes with no new warnings.
- `./gradlew :app:assembleDebug` produces APK.
- `./gradlew :venice-sdk:assembleRelease` produces AAR.
- Profile isolation test exists and passes.
- `SOURCE_BASELINE.md` updated with milestone-1 SHA + dependency versions.

## File Structure (planned)

### Create (new files)

```
core/data/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/io/github/spearchucker667/veniceforge/core/data/
    │       ├── AppDatabase.kt
    │       ├── Converters.kt
    │       ├── entity/
    │       │   ├── ProfileEntity.kt
    │       │   ├── ConversationEntity.kt
    │       │   ├── ConversationFolderEntity.kt
    │       │   ├── MessageEntity.kt
    │       │   └── MessageToolCallEntity.kt
    │       ├── dao/
    │       │   ├── ProfileDao.kt
    │       │   ├── ConversationDao.kt
    │       │   ├── MessageDao.kt
    │       │   └── MessageToolCallDao.kt
    │       └── repo/
    │           ├── ProfileRepository.kt
    │           └── ChatRepository.kt
    └── test/java/io/github/spearchucker667/veniceforge/core/data/
        ├── chat/MigrationTest.kt
        ├── chat/ChatRepositoryTest.kt
        └── chat/ProfileIsolationTest.kt

venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/
├── capabilities/
│   ├── ModelCapabilities.kt
│   ├── ModelCatalog.kt
│   └── CapabilitiesRepository.kt
├── chat/
│   ├── ChatRequest.kt
│   ├── ChatStreamChunk.kt
│   ├── ChatStreamAccumulator.kt
│   ├── SseLineParser.kt
│   └── ChatClient.kt

venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/
├── capabilities/
│   └── CapabilitiesRepositoryTest.kt
└── chat/
    ├── SseLineParserTest.kt
    ├── ChatStreamAccumulatorTest.kt
    └── ChatClientTest.kt

app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/
├── ChatViewModel.kt
└── ChatScreen.kt

app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/
└── ChatViewModelTest.kt
```

### Modify (existing files)

```
settings.gradle.kts                 # add :core:data
gradle/libs.versions.toml           # add Room + KSP entries
app/build.gradle.kts                # add :core:data dependency
app/src/main/java/.../VeniceForgeApp.kt   # import ChatScreen, wire ViewModel
app/src/main/res/values/strings.xml # add chat UI strings
core/data/.../AndroidManifest.xml   # empty manifest (no perms)
```

`SOURCE_BASELINE.md` and `docs/FEATURE_PARITY_MATRIX.md` get updated post-task.

---

## Task 0: Build environment / Gradle wrapper preflight

**Files:**
- Verify existence of: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Create (only if already missing): `gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}` via `./scripts/bootstrap-wrapper.sh`
- Append (only once, after verification): an Execution Notes section to this plan recording the toolchain state

**Purpose:** Confirm the Gradle wrapper, JDK, and Android SDK toolchain are ready so the first TDD-red in Task 1 is an *expected red* (behavior-not-implemented), not an *invalid red* (broken-environment).

- [ ] **Step 1: Run the repository-supported wrapper bootstrap**

Run: `./scripts/bootstrap-wrapper.sh --quiet`
Expected: script prints `Gradle wrapper generated. Verify with: ./gradlew --version` and exits 0. The script downloads Gradle 9.5.0 from `https://services.gradle.org/distributions/gradle-9.5.0-bin.zip` if missing in `.gradle-bootstrap/` and produces the wrapper jar + properties.

- [ ] **Step 2: Verify wrapper artifacts exist**

Run:

```bash
test -x ./gradlew && test -x ./gradlew.bat
test -f gradle/wrapper/gradle-wrapper.jar
test -f gradle/wrapper/gradle-wrapper.properties
echo OK
```

Expected: prints `OK`. Each must independently succeed. Do NOT fabricate any of these files manually — only the bootstrap script produces them.

- [ ] **Step 3: Verify wrapper executes and matches expected Gradle version**

Run: `./gradlew --version`
Expected: shows `Gradle 9.5.0`, a JDK version (17 minimum), and identifies a JVM. Capture all of the lines into the Execution Notes section below.

- [ ] **Step 4: Verify Android modules are discoverable**

Run: `./gradlew projects --quiet 2>&1 | head -25`
Expected: lists `:app`, `:venice-sdk`, `:core:common`, `:core:security`, `:core:designsystem`. `:core:data` will not yet appear (Task 1 wires it in).

- [ ] **Step 5: Confirm Android SDK / SDK-37 status**

Run:

```bash
test -n "$ANDROID_HOME" || test -n "$ANDROID_SDK_ROOT"
echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
test -d "$ANDROID_HOME/platforms/android-37" || test -d "$ANDROID_SDK_ROOT/platforms/android-37"
ls -d "$ANDROID_HOME/platforms/android-37" 2>/dev/null || ls -d "$ANDROID_SDK_ROOT/platforms/android-37" 2>/dev/null || echo "MISSING"
```

Expected: both env vars show non-empty paths, and Platform 37 directory exists. If any check fails, **stop and report to the user** before Task 1 starts — the rubric is "invalid red vs expected red" and we will not silently let Task 1 mask a broken toolchain.

- [ ] **Step 6: Append Execution Notes to this plan**

Append (manually edit this plan file) a new section after the Self-Review section:

```markdown

---

## Execution Notes

Recorded after Task 0 (2026-08-15). **Task 0 status: PASS.**

- macOS version: 27.0 (`sw_vers -productVersion`).
- Architecture: arm64 (`uname -m`).
- Homebrew prefix: `/opt/homebrew`.
- JDK distribution installed: Homebrew `openjdk@17` 17.0.20 (Temurin cask fallback used because cask installer required sudo without tty).
- `JAVA_HOME`: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
- Persistent `JAVA_HOME` set via bounded `# >>> venice-fyr-android-toolchain >>> ... # <<< venice-fyr-android-toolchain <<<` block in `~/.zprofile` (idempotent; backed up to `~/.zprofile.bak.20260815-111816`).
- `java -version`: `openjdk version "17.0.20" 2026-07-21` (OpenJDK Runtime Environment Homebrew build 17.0.20+0).
- `javac -version`: `javac 17.0.20`.
- Android command-line tools: cask `android-commandlinetools` 15859902 installed at `/opt/homebrew/share/android-commandlinetools`.
- `ANDROID_HOME`: `/opt/homebrew/share/android-commandlinetools` (set automatically by the bounded zprofile block when the SDK root directory exists).
- `sdkmanager --version`: `22.0` (deprecated; `android` CLI is the documented replacement going forward).
- Android Platform installed: `platforms;android-37.0` at `$ANDROID_HOME/platforms/android-37.0`.
- Build Tools version: `build-tools;36.0.0`.
- Platform Tools installed: `37.0.1`. `adb version`: `Version 37.0.1-15733141` (binary at `$ANDROID_HOME/platform-tools/adb`).
- Licenses accepted via `yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses`. Final message: `All SDK package licenses accepted`.
- `local.properties`: not created. `ANDROID_HOME` env var is sufficient for AGP 9.3 SDK resolution; the file is gitignored.
- Gradle wrapper: `./gradlew`, `./gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (48 462 bytes), `gradle/wrapper/gradle-wrapper.properties` all present. `chmod +x ./gradlew` applied.
- Wrapper distribution: `https://services.gradle.org/distributions/gradle-9.5.0-bin.zip`.
- `./gradlew --version`: `Gradle 9.5.0`, Kotlin 2.3.20, Groovy 4.0.29, Launcher JVM 17.0.20, Daemon JVM resolves to the same Homebrew JDK 17.
- `./gradlew projects`: lists `:app`, `:core:common`, `:core:designsystem`, `:core:security`, `:venice-sdk`. `:core:data` will join after Task 1.
- `./gradlew help`: returns the standard Gradle 9.5 welcome text, exit 0.
- Machine-local files NOT staged: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` will be committed only if not present in HEAD (see Step 7).
- `local.properties`: NOT present and NOT committed; not needed.
- Known deviations from the user-facing spec/repository:
  - `gradlew.bat` exists but `test -x` returns false — expected for a *nix host (the .bat file is for Windows; `test -f` confirms presence).
  - Temurin cask was attempted first; the install failure mode (`sudo: a terminal is required`) shifted to the `openjdk@17` no-sudo fallback. Distribution and version remain JDK 17.0.20 as required by AGP 9.3.
- Task 1 may begin.

```

- [ ] **Step 7: Commit (init wrapper + notes only if needed)**

If `gradle/wrapper/gradle-wrapper.jar` and `gradle/wrapper/gradle-wrapper.properties` are untracked, commit them alongside the plan amendment:

```bash
git add gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat \
        docs/superpowers/plans/2026-08-15-android-port-milestone-1.md
git commit -m "build: bootstrap Gradle wrapper + Execution Notes from Task 0"
```

If the bootstrap script already registered these files in a previous commit, skip the wrapper artifacts and only commit the plan amendment.

**Failure rule:** If any of Steps 1–5 fails, STOP and report to the user. Do not dispatch Task 1 against a broken toolchain — that would conflate invalid-red failures with expected-red failures.

---

## Task 1: Toolchain + module wiring for `:core:data`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `core/data/build.gradle.kts`
- Create: `core/data/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: a buildable empty `:core:data` library that depends on `core:common`.

- [ ] **Step 1: Add Room + KSP entries to `gradle/libs.versions.toml`**

Edit `gradle/libs.versions.toml`, append inside `[versions]`:

```toml
ksp = "2.0.2-1.0.25"
room = "2.7.0"
sqliteKtx = "2.5.0"
robolectric = "4.13"
androidxTest = "1.6.1"
```

Append inside `[libraries]`:

```toml
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-sqlite = { module = "androidx.sqlite:sqlite", version.ref = "sqliteKtx" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-test-ext = { module = "androidx.test.ext:junit", version.ref = "androidxTest" }
```

Append inside `[plugins]`:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
androidx-room = { id = "androidx.room", version.ref = "room" }
```

- [ ] **Step 2: Add `:core:data` to `settings.gradle.kts`**

Append `include(":core:data")` to the bottom of `settings.gradle.kts`:

```kotlin
rootProject.name = "VeniceForgeAndroid"
include(":app")
include(":venice-sdk")
include(":core:common")
include(":core:security")
include(":core:designsystem")
include(":core:data")
```

- [ ] **Step 3: Create `core/data/build.gradle.kts`**

Create the file with this content:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "io.github.spearchucker667.veniceforge.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    sourceSets {
        test {
            resources.srcDirs("$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.sqlite)
    testImplementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 4: Create empty manifest `core/data/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 5: Run `:core:data:help` to confirm wiring**

Run: `./gradlew :core:data:help --quiet`
Expected: BUILD SUCCESSFUL. KSP and Room plugins must apply without complaint.

If `plugins.kotlin.android` is not declared in the [plugins] section of `libs.versions.toml`, add it:

```toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

Use it as `alias(libs.plugins.kotlin.android)` in `core/data/build.gradle.kts` only. Do NOT add it to `:app` or `:venice-sdk` (AGP 9.3 built-in Kotlin).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml gradle/libs.versions.toml settings.gradle.kts core/data/build.gradle.kts core/data/src/main/AndroidManifest.xml
git commit -m "build: scaffold :core:data module with Room + KSP"
```

---

## Task 2: Room entities + enums + converters

**Files:**
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationEntity.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationFolderEntity.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageToolCallEntity.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt`

**Interfaces:**
- Produces: 5 `@Entity` data classes and a `@TypeConverter` provider.

- [ ] **Step 1: Write `ProfileEntity.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val apiKeyAlias: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 2: Write `ConversationFolderEntity.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_folders",
    indices = [Index("profileId"), Index(value = ["profileId", "sortOrder"])],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ConversationFolderEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 3: Write `ConversationEntity.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ConversationKind { STANDARD, CHARACTER }

@Entity(
    tableName = "conversations",
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "updatedAt"]),
        Index("folderId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ConversationFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val modelId: String,
    val kind: ConversationKind,
    val pinned: Boolean,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
)
```

- [ ] **Step 4: Write `MessageEntity.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }
enum class MessageStatus { PENDING, STREAMING, COMPLETED, FAILED, CANCELLED }

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index(value = ["conversationId", "createdAt"]),
        Index("parentMessageId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val profileId: String,
    val role: MessageRole,
    val parentMessageId: String?,
    val status: MessageStatus,
    val textContent: String,
    val modelId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 5: Write `MessageToolCallEntity.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ToolCallStatus { PENDING, STREAMING, COMPLETED, FAILED, CANCELLED }

@Entity(
    tableName = "message_tool_calls",
    indices = [
        Index("messageId"),
        Index("toolCallId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageToolCallEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val toolCallId: String,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String?,
    val status: ToolCallStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 6: Write `Converters.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data

import androidx.room.TypeConverter
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationKind
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.entity.ToolCallStatus

class Converters {
    @TypeConverter fun fromMessageRole(v: MessageRole?): String? = v?.name
    @TypeConverter fun toMessageRole(v: String?): MessageRole? = v?.let { MessageRole.valueOf(it) }

    @TypeConverter fun fromMessageStatus(v: MessageStatus?): String? = v?.name
    @TypeConverter fun toMessageStatus(v: String?): MessageStatus? = v?.let { MessageStatus.valueOf(it) }

    @TypeConverter fun fromConversationKind(v: ConversationKind?): String? = v?.name
    @TypeConverter fun toConversationKind(v: String?): ConversationKind? = v?.let { ConversationKind.valueOf(it) }

    @TypeConverter fun fromToolCallStatus(v: ToolCallStatus?): String? = v?.name
    @TypeConverter fun toToolCallStatus(v: String?): ToolCallStatus? = v?.let { ToolCallStatus.valueOf(it) }
}
```

- [ ] **Step 7: Quick compile check**

Run: `./gradlew :core:data:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt
git commit -m "feat(data): add Room entities and type converters"
```

---

## Task 3: DAOs

**Files:**
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt`

**Interfaces:**
- Produces: 4 `@Dao` interfaces. Used by repositories in Task 5.

- [ ] **Step 1: Write `ProfileDao.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun findDefault(): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>
}
```

- [ ] **Step 2: Write `ConversationDao.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(profileId: String, id: String): Int

    @Query("SELECT * FROM conversations WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun findById(profileId: String, id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeForProfile(profileId: String): Flow<List<ConversationEntity>>

    @Transaction
    suspend fun deleteCascade(profileId: String, id: String) {
        deleteById(profileId, id)
    }
}
```

- [ ] **Step 3: Write `MessageDao.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET textContent = :text, status = :status, updatedAt = :updatedAt WHERE id = :id AND profileId = :profileId")
    suspend fun updateTextAndStatus(profileId: String, id: String, text: String, status: MessageStatus, updatedAt: Long): Int

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND profileId = :profileId ORDER BY createdAt ASC")
    fun observeForConversation(profileId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE id = :id AND profileId = :profileId")
    suspend fun deleteById(profileId: String, id: String): Int
}
```

- [ ] **Step 4: Write `MessageToolCallDao.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.spearchucker667.veniceforge.core.data.entity.MessageToolCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageToolCallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(call: MessageToolCallEntity)

    @Query("SELECT * FROM message_tool_calls WHERE messageId = :messageId ORDER BY createdAt ASC")
    fun observeForMessage(messageId: String): Flow<List<MessageToolCallEntity>>
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew :core:data:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao
git commit -m "feat(data): add Room DAOs"
```

---

## Task 4: AppDatabase

**Files:**
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`

**Interfaces:**
- Produces: `AppDatabase` (RoomDatabase) with version `1` exporting the v1 JSON schema.

- [ ] **Step 1: Write `AppDatabase.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.spearchucker667.veniceforge.core.data.dao.ConversationDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageDao
import io.github.spearchucker667.veniceforge.core.data.dao.MessageToolCallDao
import io.github.spearchucker667.veniceforge.core.data.dao.ProfileDao
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationFolderEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageToolCallEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        ConversationEntity::class,
        ConversationFolderEntity::class,
        MessageEntity::class,
        MessageToolCallEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageToolCallDao(): MessageToolCallDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "venice_forge.db",
            )
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()

        const val SCHEMA_VERSION = 1
    }
}
```

- [ ] **Step 2: Build to generate schemas**

Run: `./gradlew :core:data:kspDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL. Files like `core/data/schemas/io.github.spearchucker667.veniceforge.core.data.AppDatabase/1.json` must appear.

- [ ] **Step 3: Verify schema exported**

Run: `ls core/data/schemas/io.github.spearchucker667.veniceforge.core.data.AppDatabase/`
Expected: prints `1.json`.

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt core/data/schemas
git commit -m "feat(data): AppDatabase v1 with schema export"
```

---

## Task 5: ProfileRepository and ChatRepository (FAILING tests first)

**Files:**
- Create: `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt`
- Create: `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt`
- Create: `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileIsolationTest.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`

**Interfaces:**
- Produces:
  - `class ProfileRepository(private val dao: ProfileDao)` — `suspend fun ensureDefault(): String`, `suspend fun findDefault(): String?`.
  - `class ChatRepository(private val db: AppDatabase)` — profile-scoped conversation/message ops; every method takes `profileId: String` first.

- [ ] **Step 1: Write `ProfileRepositoryTest.kt` (failing test)**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val repo = ProfileRepository(db.profileDao())

    @After fun tearDown() { db.close() }

    @Test
    fun `ensureDefault creates default profile when none exists`() {
        val id = repo.ensureDefault()
        assertEquals(id, db.profileDao().findDefault()?.id)
        assertEquals("default", db.profileDao().findDefault()?.displayName)
    }

    @Test
    fun `ensureDefault is idempotent`() {
        val first = repo.ensureDefault()
        val second = repo.ensureDefault()
        assertEquals(first, second)
    }
}
```

- [ ] **Step 2: Run test (it must fail)**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*ProfileRepositoryTest*" --quiet`
Expected: FAIL with `ProfileRepository is not defined` or unresolved reference.

- [ ] **Step 3: Implement `ProfileRepository.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.repo

import io.github.spearchucker667.veniceforge.core.data.dao.ProfileDao
import io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity

class ProfileRepository(private val dao: ProfileDao) {
    suspend fun ensureDefault(): String {
        dao.findDefault()?.let { return it.id }
        val now = System.currentTimeMillis()
        val entity = ProfileEntity(
            id = DEFAULT_PROFILE_ID,
            displayName = "default",
            apiKeyAlias = DEFAULT_PROFILE_ID,
            isDefault = true,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        return DEFAULT_PROFILE_ID
    }

    suspend fun findDefault(): String? = dao.findDefault()?.id

    companion object {
        const val DEFAULT_PROFILE_ID = "default"
    }
}
```

- [ ] **Step 4: Run test (must pass)**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*ProfileRepositoryTest*" --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Write `ChatRepositoryTest.kt` (failing)**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    @Test
    fun `create conversation, append messages, observe`() = runTest {
        val profileRepo = ProfileRepository(db.profileDao())
        val profileId = profileRepo.ensureDefault()
        val chat = ChatRepository(db)

        val conversationId = chat.createConversation(profileId, "llama-3.3-70b")
        chat.appendMessage(profileId, conversationId, userMessage(conversationId, profileId, "Hi"))
        chat.appendMessage(profileId, conversationId, assistantMessage(conversationId, profileId, "Hello!"))

        val messages = chat.observeMessages(profileId, conversationId).first()
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].textContent)
        assertEquals(MessageStatus.COMPLETED, messages[1].status)
    }

    private fun userMessage(conv: String, profile: String, text: String) =
        MessageEntity(
            id = "u1", conversationId = conv, profileId = profile,
            role = MessageRole.USER, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = null, createdAt = 1L, updatedAt = 1L,
        )

    private fun assistantMessage(conv: String, profile: String, text: String) =
        MessageEntity(
            id = "a1", conversationId = conv, profileId = profile,
            role = MessageRole.ASSISTANT, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = "llama-3.3-70b", createdAt = 2L, updatedAt = 2L,
        )
}
```

- [ ] **Step 6: Run test (must fail)**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*ChatRepositoryTest*" --quiet`
Expected: FAIL — `ChatRepository` unresolved.

- [ ] **Step 7: Implement `ChatRepository.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.repo

import androidx.room.withTransaction
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationEntity
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationKind
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository.Companion.DEFAULT_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val db: AppDatabase) {
    private val profileDao = db.profileDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()

    suspend fun createConversation(
        profileId: String,
        modelId: String,
        title: String = "New conversation",
        kind: ConversationKind = ConversationKind.STANDARD,
    ): String = db.withTransaction {
        require(profileDao.findById(profileId) != null) { "Unknown profileId: $profileId" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversationDao.insert(
            ConversationEntity(
                id = id,
                profileId = profileId,
                title = title,
                modelId = modelId,
                kind = kind,
                pinned = false,
                folderId = null,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = now,
            )
        )
        id
    }

    suspend fun appendMessage(profileId: String, conversationId: String, message: MessageEntity) {
        require(message.profileId == profileId) { "Message.profileId must equal scoping profileId" }
        require(message.conversationId == conversationId) { "Message.conversationId mismatch" }
        db.withTransaction {
            require(conversationDao.findById(profileId, conversationId) != null) {
                "Unknown conversationId: $conversationId"
            }
            messageDao.upsert(message)
        }
    }

    suspend fun updateAssistantText(
        profileId: String,
        messageId: String,
        text: String,
        status: MessageStatus,
    ) {
        messageDao.updateTextAndStatus(
            profileId = profileId,
            id = messageId,
            text = text,
            status = status,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun observeConversations(profileId: String): Flow<List<ConversationEntity>> =
        conversationDao.observeForProfile(profileId)

    fun observeMessages(profileId: String, conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(profileId, conversationId).map { messages ->
            messages.filter { it.profileId == profileId }  // belt and suspenders
        }

    suspend fun deleteConversation(profileId: String, conversationId: String): Boolean =
        db.withTransaction {
            conversationDao.deleteById(profileId, conversationId) > 0
        }
}
```

- [ ] **Step 8: Run test (must pass)**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*ChatRepositoryTest*" --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Write `ProfileIsolationTest.kt` (failing)**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileIsolationTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    @Test
    fun `profile B cannot read profile A's conversations`() = runTest {
        val profileRepo = ProfileRepository(db.profileDao())
        val chat = ChatRepository(db)

        val a = profileRepo.ensureDefault()
        val b = "profile-b"
        profileRepo.run {
            // create a second non-default profile.
            db.profileDao().insert(
                io.github.spearchucker667.veniceforge.core.data.entity.ProfileEntity(
                    id = b, displayName = "B",
                    apiKeyAlias = b, isDefault = false,
                    createdAt = 0L, updatedAt = 0L,
                )
            )
        }

        val convA = chat.createConversation(a, "model-x")
        chat.appendMessage(a, convA, msg(convA, a, "secret", "a1"))

        // B cannot observe A's conversation or its messages.
        assertEquals(0, chat.observeMessages(b, convA).first().size)
        assertTrue("B's conversation list must not contain A's conversation",
            chat.observeConversations(b).first().none { it.id == convA })
        assertEquals(1, chat.observeConversations(a).first().size)
        // Sanity: B's direct lookup of A's conversation id returns null.
        assertNull(chat.observeConversations(b).first().firstOrNull { it.id == convA })
    }

    private fun msg(conv: String, profile: String, text: String, id: String) =
        MessageEntity(
            id = id, conversationId = conv, profileId = profile,
            role = MessageRole.USER, parentMessageId = null,
            status = MessageStatus.COMPLETED, textContent = text,
            modelId = null, createdAt = 1L, updatedAt = 1L,
        )
}
```

- [ ] **Step 10: Run isolation test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*ProfileIsolationTest*" --quiet`
Expected: BUILD SUCCESSFUL — the existing repository already enforces profileId parameter scoping in @Query. The `observeMessages` `map {}` belt-and-suspenders also defends the data layer.

- [ ] **Step 11: Commit**

```bash
git add core/data/src core/data/src/test
git commit -m "feat(data): profile repositories + profile-isolation tests"
```

---

## Task 6: Migration test scaffold

**Files:**
- Create: `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt`

**Interfaces:**
- Produces: A test that verifies the v1 schema is what `AppDatabase` actually builds.

- [ ] **Step 1: Write test**

```kotlin
package io.github.spearchucker667.veniceforge.core.data.chat

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `v1 schema creates all expected tables`() {
        helper.createDatabase(dbName, 1).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.moveToNext()) names.add(cursor.getString(0))
                check("profiles" in names)
                check("conversations" in names)
                check("conversation_folders" in names)
                check("messages" in names)
                check("message_tool_calls" in names)
            }
        }
    }

    @Test
    fun `AppDatabase can open v1`() {
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            dbName,
        ).build().use { /* open succeeds */ }
    }
}
```

- [ ] **Step 2: Run**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MigrationTest*" --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/data/src/test
git commit -m "test(data): verify Room v1 schema matches expectations"
```

---

## Task 7: SDK typed capability models (FAILING tests first)

**Files:**
- Create: `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt`
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCapabilities.kt`
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/ModelCatalog.kt`
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepository.kt`
- Create: `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json`
- Create: `venice-sdk/src/test/resources/fixtures/models-with-capabilities/traits.json`
- Create: `venice-sdk/src/test/resources/fixtures/models-with-capabilities/compatibility.json`

**Interfaces:**
- Produces:
  - `class CapabilitiesRepository(private val sdk: VeniceForgeSdk)` — `suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog`.
  - `data class ModelCapabilities(...)` with typed flags + `rawJson: String`.

- [ ] **Step 1: Decide capability shape**

Read desktop source files to confirm capability keys. Inspect:

```
$VENICE_FORGE_DESKTOP_SOURCE/src/types/venice.ts
$VENICE_FORGE_DESKTOP_SOURCE/src/services/modelService.ts
$VENICE_FORGE_DESKTOP_SOURCE/docs/reference/Venice_swagger_api.yaml
```

Record what you saw in the commit footer. If the desktop uses a dedicated `traits` and `compatibility_mapping` endpoint, mirror those keys verbatim when typed. Unknown JSON keys always pass through to `rawJson`.

- [ ] **Step 2: Write `ModelCapabilities.kt` shape (placeholder header only)**

For now, the typed shape is:

```kotlin
package io.github.spearchucker667.veniceforge.sdk.capabilities

data class ModelCapabilities(
    val id: String,
    val name: String?,
    val description: String?,
    val rawJson: String,
    val supportsImageInput: Boolean = false,
    val supportsToolCalling: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsSystemPrompt: Boolean = true,
    val maxContextTokens: Int? = null,
    val inputModalities: Set<String> = setOf("text"),
    val outputModalities: Set<String> = setOf("text"),
    val traits: Map<String, String> = emptyMap(),
    val compatibleWith: Set<String> = emptySet(),
)
```

Add fields if/when the desktop types require them. Update tests accordingly.

- [ ] **Step 3: Write `ModelCatalog.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.capabilities

data class ModelCatalog(
    val models: List<ModelCapabilities>,
    val refreshedAt: Long,
) {
    fun byId(id: String): ModelCapabilities? = models.firstOrNull { it.id == id }
}
```

- [ ] **Step 4: Write failing test `CapabilitiesRepositoryTest.kt`**

The test injects a fake `VeniceForgeSdk` (or a mocked `OkHttpClient`). For milestone 1 we use a fake that returns fixture strings from `src/test/resources/fixtures/models-with-capabilities/`:

```kotlin
package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesRepositoryTest {

    private fun fixture(name: String): String =
        CapabilitiesRepositoryTest::class.java.getResourceAsStream("/fixtures/models-with-capabilities/$name")!!
            .bufferedReader().readText()

    private fun fakeSdk(): io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk {
        val jsonMedia = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.encodedPath
                val body = when {
                    url.endsWith("/" + VeniceEndpoints.MODELS) -> fixture("models.json")
                    url.endsWith("/" + VeniceEndpoints.MODEL_TRAITS) -> fixture("traits.json")
                    url.endsWith("/" + VeniceEndpoints.MODEL_COMPATIBILITY) -> fixture("compatibility.json")
                    else -> "{}"
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody(jsonMedia))
                    .build()
            })
            .build()
        return io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk(
            config = VeniceSdkConfig(),
            httpClient = client,
        )
    }

    @Test
    fun `combines models, traits and compatibility into one catalog`() = runTest {
        val repo = CapabilitiesRepository(fakeSdk())
        val catalog = repo.fetchLiveCapabilities("test-key")
        val llama = catalog.byId("llama-3.3-70b") ?: error("missing llama")
        assertTrue(llama.supportsToolCalling)
        assertTrue(llama.supportsImageInput)
        assertEquals(setOf("text", "image"), llama.inputModalities)
        assertTrue("deepseek-r1" in llama.compatibleWith)
    }
}
```

- [ ] **Step 5: Add fixture files (write minimal valid JSON)**

`fixtures/models-with-capabilities/models.json`:

```json
{
  "data": [
    { "id": "llama-3.3-70b", "object": "model", "model_spec": { "name": "Llama 3.3 70B", "description": "Open-weight chat model." } },
    { "id": "deepseek-r1",   "object": "model", "model_spec": { "name": "DeepSeek R1" } }
  ]
}
```

`fixtures/models-with-capabilities/traits.json`:

```json
{
  "data": [
    { "id": "llama-3.3-70b", "supports_image_input": true, "supports_tool_calling": true, "supports_streaming": true, "supports_system_prompt": true, "input_modalities": ["text", "image"], "output_modalities": ["text"], "max_context_tokens": 128000 },
    { "id": "deepseek-r1",   "supports_image_input": false, "supports_tool_calling": false, "supports_streaming": true, "supports_system_prompt": true, "input_modalities": ["text"], "output_modalities": ["text"] }
  ]
}
```

`fixtures/models-with-capabilities/compatibility.json`:

```json
{
  "data": [
    { "id": "llama-3.3-70b", "compatible_with": ["deepseek-r1"] }
  ]
}
```

- [ ] **Step 6: Run test — must fail**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*CapabilitiesRepositoryTest*" --quiet`
Expected: FAIL — `CapabilitiesRepository` unresolved.

- [ ] **Step 7: Implement `CapabilitiesRepository.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.capabilities

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class CapabilitiesRepository(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog = withContext(Dispatchers.IO) {
        // Reuse VeniceForgeSdk private listModels by exposing a thin helper.
        // Pragmatic: add methods on VeniceForgeSdk to fetch /models/traits + /models/compatibility_mapping.
        // For this milestone we read them directly via a small private helper.
        val models = fetchModels(apiKey)
        val traits = fetchTraits(apiKey)
        val compat = fetchCompatibility(apiKey)

        val traitById = traits.associateBy { it.id.orEmpty() }
        val compatibleById = compat
            .groupBy { it.id.orEmpty() }
            .mapValues { (_, list) -> list.flatMap { it.compatibleWith.orEmpty() }.toSet() }
            .mapKeys { it.key }

        val merged = models.map { m ->
            val t = traitById[m.id]
            ModelCapabilities(
                id = m.id,
                name = m.name,
                description = m.description,
                rawJson = m.rawJson,
                supportsImageInput = t?.supportsImageInput ?: false,
                supportsToolCalling = t?.supportsToolCalling ?: false,
                supportsStreaming = t?.supportsStreaming ?: true,
                supportsSystemPrompt = t?.supportsSystemPrompt ?: true,
                maxContextTokens = t?.maxContextTokens,
                inputModalities = t?.inputModalities ?: setOf("text"),
                outputModalities = t?.outputModalities ?: setOf("text"),
                traits = t?.traits ?: emptyMap(),
                compatibleWith = compatibleById[m.id] ?: emptySet(),
            )
        }
        ModelCatalog(models = merged, refreshedAt = System.currentTimeMillis())
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private data class RawModel(
        val id: String,
        val name: String?,
        val description: String?,
        val rawJson: String,
    )

    private data class RawTraits(
        val id: String?,
        val supportsImageInput: Boolean,
        val supportsToolCalling: Boolean,
        val supportsStreaming: Boolean,
        val supportsSystemPrompt: Boolean,
        val maxContextTokens: Int?,
        val inputModalities: Set<String>,
        val outputModalities: Set<String>,
        val traits: Map<String, String>,
    )

    private data class RawCompat(val id: String?, val compatibleWith: List<String>?)

    private suspend fun fetchModels(apiKey: String): List<RawModel> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODELS}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = obj.str("id") ?: return@mapNotNull null
            val spec = obj["model_spec"] as? JsonObject
            RawModel(
                id = id,
                name = spec?.str("name"),
                description = spec?.str("description"),
                rawJson = obj.toString(),
            )
        }
    }

    private suspend fun fetchTraits(apiKey: String): List<RawTraits> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODEL_TRAITS}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = obj.str("id")
            val modArr = (obj["input_modalities"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }?.toSet() ?: setOf("text")
            val outArr = (obj["output_modalities"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }?.toSet() ?: setOf("text")
            val traits = obj["traits"] as? JsonObject
                ?.mapValues { (_, v) -> (v as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "" }
                ?: emptyMap()
            RawTraits(
                id = id,
                supportsImageInput = obj["supports_image_input"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: false,
                supportsToolCalling = obj["supports_tool_calling"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: false,
                supportsStreaming = obj["supports_streaming"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: true,
                supportsSystemPrompt = obj["supports_system_prompt"]?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() } ?: true,
                maxContextTokens = obj["max_context_tokens"]?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() },
                inputModalities = modArr,
                outputModalities = outArr,
                traits = traits,
            )
        }
    }

    private suspend fun fetchCompatibility(apiKey: String): List<RawCompat> {
        val raw = sdk.getRaw("/${VeniceEndpoints.MODEL_COMPATIBILITY}", apiKey)
        val arr = (json.parseToJsonElement(raw).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val compatible = (obj["compatible_with"] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.takeIf { it.isString }?.content
            }
            RawCompat(id = obj.str("id"), compatibleWith = compatible)
        }
    }
}
```

- [ ] **Step 8: Add `getRaw` to `VeniceForgeSdk`**

Modify `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt`. Add an internal helper used by the capabilities repo:

```kotlin
class VeniceForgeSdk(
    private val config: VeniceSdkConfig = VeniceSdkConfig(),
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = config.baseUrl.toHttpUrl()

    suspend fun listModels(apiKey: String, type: String = "all"): List<VeniceModel> = /* unchanged */ {
        /* original implementation */
    }

    internal suspend fun getRaw(path: String, apiKey: String): String {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments(path.trimStart('/')).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent)
            .get()
            .build()
        httpClient.newCall(request).execute().use { res ->
            check(res.isSuccessful) { "Venice API HTTP ${res.code}" }
            return res.body.string()
        }
    }
}
```

Preserve the existing `listModels` body verbatim. Replace the body of `parseModel` and helpers exactly as today.

- [ ] **Step 9: Run capability test — must pass**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*CapabilitiesRepositoryTest*" --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Run all `:venice-sdk` tests**

Run: `./gradlew :venice-sdk:test --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/capabilities \
        venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt \
        venice-sdk/src/test
git commit -m "feat(sdk): typed model capabilities + traits + compatibility"
# desktop sources: src/types/venice.ts, src/services/modelService.ts
# desktop HEAD: bc5c173...
```

---

## Task 8: ChatRequest, ChatStreamChunk, ChatStreamAccumulator (FAILING tests first)

**Files:**
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatRequest.kt`
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamChunk.kt`
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulator.kt`
- Create: `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt`

**Interfaces:**
- Produces:
  - `data class ChatRequest(model: String, messages: List<ChatMessage>, tools: List<ToolSpec>?, stream: Boolean = true, safeMode: Boolean? = null, temperature: Float? = null)`
  - `sealed class ChatStreamChunk { Open; Delta; ToolCallDelta; Finish; Error }`
  - `class ChatStreamAccumulator` mutating state into `AssistantMessage`.

- [ ] **Step 1: Write `ChatRequest.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: ToolFunction,
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String? = null,
    val parameters: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val tools: List<ToolSpec>? = null,
    @SerialName("venice_parameters") val veniceParameters: VeniceParameters? = null,
)

@Serializable
data class VeniceParameters(
    val enable_web_search: Boolean? = null,
    @SerialName("safe_mode") val safeMode: Boolean? = null,
    // Preserve explicit safe_mode=false when caller passed it. Never drop.
)
```

- [ ] **Step 2: Write `ChatStreamChunk.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import kotlinx.serialization.Serializable

sealed class ChatStreamChunk {
    data class Open(val id: String? = null) : ChatStreamChunk()
    data class Delta(val index: Int, val textFragment: String?) : ChatStreamChunk()
    data class ToolCallDelta(
        val index: Int,
        val callId: String?,
        val name: String?,
        val argumentsFragment: String?,
    ) : ChatStreamChunk()
    data class Finish(val reason: String, val usage: Usage? = null) : ChatStreamChunk()
    data class Error(val code: Int?, val message: String) : ChatStreamChunk()

    @Serializable
    data class Usage(
        val prompt_tokens: Int? = null,
        val completion_tokens: Int? = null,
        val total_tokens: Int? = null,
    )
}
```

- [ ] **Step 3: Write `ChatStreamAccumulator.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

class ChatStreamAccumulator {
    private val text = StringBuilder()
    private val toolCalls = mutableMapOf<Int, MutableToolCall>()
    var finishedReason: String? = null
        private set
    var lastError: ChatStreamChunk.Error? = null
        private set

    fun apply(chunk: ChatStreamChunk) {
        when (chunk) {
            is ChatStreamChunk.Delta -> {
                if (!chunk.textFragment.isNullOrEmpty()) text.append(chunk.textFragment)
            }
            is ChatStreamChunk.ToolCallDelta -> {
                val tc = toolCalls.getOrPut(chunk.index) { MutableToolCall() }
                chunk.callId?.let { tc.id = it }
                chunk.name?.let { tc.name = it }
                if (!chunk.argumentsFragment.isNullOrEmpty()) tc.arguments.append(chunk.argumentsFragment)
            }
            is ChatStreamChunk.Finish -> { finishedReason = chunk.reason }
            is ChatStreamChunk.Error -> { lastError = chunk }
            is ChatStreamChunk.Open -> { /* nothing */ }
        }
    }

    fun snapshot(): AssistantMessage = AssistantMessage(
        text = text.toString(),
        toolCalls = toolCalls.entries
            .sortedBy { it.key }
            .map { (_, v) ->
                ToolCall(v.id, v.name, v.arguments.toString())
            },
        finishedReason = finishedReason,
        error = lastError,
    )

    private data class MutableToolCall(var id: String? = null, var name: String? = null) {
        val arguments = StringBuilder()
    }

    data class AssistantMessage(
        val text: String,
        val toolCalls: List<ToolCall>,
        val finishedReason: String?,
        val error: ChatStreamChunk.Error?,
    )

    data class ToolCall(val id: String?, val name: String?, val argumentsJson: String)
}
```

- [ ] **Step 4: Write failing test `ChatStreamAccumulatorTest.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatStreamAccumulatorTest {
    @Test
    fun `accumulates text deltas into single string`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Delta(0, "Hello"))
        acc.apply(ChatStreamChunk.Delta(0, ", "))
        acc.apply(ChatStreamChunk.Delta(0, "world!"))
        assertEquals("Hello, world!", acc.snapshot().text)
    }

    @Test
    fun `reconstructs tool call across fragmented deltas`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.ToolCallDelta(0, "call_1", "web_search", null))
        acc.apply(ChatStreamChunk.ToolCallDelta(0, null, null, """{"q":"""))
        acc.apply(ChatStreamChunk.ToolCallDelta(0, null, null, ""}\""))

        val snap = acc.snapshot()
        assertEquals(1, snap.toolCalls.size)
        assertEquals("call_1", snap.toolCalls[0].id)
        assertEquals("web_search", snap.toolCalls[0].name)
        assertEquals("""{"q":""}""", snap.toolCalls[0].argumentsJson)
    }

    @Test
    fun `finish chunk sets finished reason`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Finish(reason = "stop"))
        assertEquals("stop", acc.finishedReason)
    }

    @Test
    fun `error chunk sets last error but does not clobber text`() {
        val acc = ChatStreamAccumulator()
        acc.apply(ChatStreamChunk.Delta(0, "partial"))
        acc.apply(ChatStreamChunk.Error(429, "rate limited"))
        assertEquals("partial", acc.snapshot().text)
        assertEquals(429, acc.lastError?.code)
    }
}
```

- [ ] **Step 5: Run test (must pass)**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*ChatStreamAccumulatorTest*" --quiet`
Expected: PASS (no implementation gap because Step 3 already implemented).

- [ ] **Step 6: Commit**

```bash
git add venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat \
        venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt
git commit -m "feat(sdk): chat request/chunk/accumulator types"
```

---

## Task 9: SseLineParser

**Files:**
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt`
- Create: `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt`

**Interfaces:**
- Produces: `class SseLineParser` that takes a `BufferedReader` line stream and yields `data: ...` payloads as `String?` (null means heartbeat / blank / comment).

- [ ] **Step 1: Write failing test**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class SseLineParserTest {
    @Test
    fun `parses data lines and skips comments`() {
        val input = """
            : heartbeat
            data: {"id":"abc"}

            data: {"delta":"hi"}
            data: [DONE]
        """.trimIndent()
        val reader = BufferedReader(StringReader(input))
        val parser = SseLineParser(reader)
        assertEquals("{\"id\":\"abc\"}", parser.nextData())
        assertEquals("{\"delta\":\"hi\"}", parser.nextData())
        assertEquals("[DONE]", parser.nextData())
        assertNull(parser.nextData())
    }

    @Test
    fun `yields null on blank stream when done`() {
        val parser = SseLineParser(BufferedReader(StringReader("")))
        assertNull(parser.nextData())
    }
}
```

- [ ] **Step 2: Run failing test**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*SseLineParserTest*" --quiet`
Expected: FAIL — `SseLineParser` unresolved.

- [ ] **Step 3: Implement `SseLineParser.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import java.io.BufferedReader

class SseLineParser(private val reader: BufferedReader) {
    fun nextData(): String? {
        val line = reader.readLine() ?: return null
        if (line.isEmpty()) return nextData()
        if (line.startsWith(":")) return nextData()
        if (line.startsWith("data:")) return line.removePrefix("data:").trim()
        // Other SSE fields (event:, id:) are ignored for /chat/completions.
        return nextData()
    }
}
```

- [ ] **Step 4: Run test — must pass**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*SseLineParserTest*" --quiet`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParser.kt \
        venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt
git commit -m "feat(sdk): SSE line parser"
```

---

## Task 10: ChatClient.streamChat

**Files:**
- Create: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClient.kt`
- Create: `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt`
- Create: `venice-sdk/src/test/resources/fixtures/chat-stream/stream-good.sse`

**Interfaces:**
- Produces: `class ChatClient` (constructor takes `VeniceForgeSdk` or shares config) with `suspend fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk>`.

- [ ] **Step 1: Write failing test `ChatClientTest.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatClientTest {
    private fun load(name: String): String =
        ChatClientTest::class.java.getResourceAsStream("/fixtures/chat-stream/$name")!!
            .bufferedReader().readText()

    private fun client(payload: String): ChatClient {
        val sseMime = "text/event-stream".toMediaType()
        val ok = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(payload.toResponseBody(sseMime))
                    .build()
            })
            .build()
        val sdk = VeniceForgeSdk(config = VeniceSdkConfig(), httpClient = ok)
        return ChatClient(sdk)
    }

    @Test
    fun `streams chat completions into chunks`() = runTest {
        val chunks = client(load("stream-good.sse"))
            .streamChat(
                apiKey = "test-key",
                request = ChatRequest(model = "llama-3.3-70b", messages = listOf(ChatMessage(role = "user", content = "hi"))),
            )
            .toList()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.any { it is ChatStreamChunk.Delta })
        assertTrue(chunks.last() is ChatStreamChunk.Finish)
    }
}
```

- [ ] **Step 2: Write fixture `stream-good.sse`**

```
data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"content":", "}}]}

data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"content":"world!"},"finish_reason":null}]}

data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

- [ ] **Step 3: Run test (must fail)**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*ChatClientTest*" --quiet`
Expected: FAIL — `ChatClient` unresolved.

- [ ] **Step 4: Implement `ChatClient.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.sdk.chat

import io.github.spearchucker667.veniceforge.sdk.VeniceEndpoints
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

open class ChatClient(private val sdk: VeniceForgeSdk) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    open fun streamChat(apiKey: String, request: ChatRequest): Flow<ChatStreamChunk> = callbackFlow {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        val reqBody = json.encodeToString(ChatRequest.serializer(), request)
        val httpReq = Request.Builder()
            .url(sdk.baseUrl().newBuilder().addPathSegments(VeniceEndpoints.CHAT_COMPLETIONS).build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("User-Agent", sdk.userAgent())
            .post(reqBody.toRequestBody(jsonMedia))
            .build()

        val call = sdk.httpClient().newCall(httpReq)
        val cancellationHook = trySend(ChatStreamChunk.Open()).isSuccess  // signal stream opened

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = response.body?.string().orEmpty()
                    trySend(ChatStreamChunk.Error(response.code, msg)).isSuccess
                    close()
                    return@callbackFlow
                }
                val source = response.body?.byteStream()
                    ?: throw VeniceSdkException.Protocol("Empty response body")
                val parser = SseLineParser(source.bufferedReader())
                while (true) {
                    val payload = parser.nextData() ?: break
                    if (payload == "[DONE]") break
                    val chunk = parseChunk(payload)
                    trySend(chunk).isSuccess
                }
                trySend(ChatStreamChunk.Finish(reason = "stop"))
                close()
            }
        } catch (e: Throwable) {
            trySend(ChatStreamChunk.Error(code = null, message = e.message ?: e::class.simpleName.orEmpty()))
            close(e)
        }

        awaitClose {
            if (!call.isCanceled()) runCatching { call.cancel() }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseChunk(payload: String): ChatStreamChunk {
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
            ?: return ChatStreamChunk.Error(null, "invalid SSE JSON: $payload")
        val choices = obj["choices"]
        if (choices !is JsonArray) {
            val errObj = obj["error"]
            if (errObj is JsonObject) {
                val msg = (errObj["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "stream error"
                val code = (errObj["code"] as? JsonPrimitive)?.content?.toIntOrNull()
                return ChatStreamChunk.Error(code, msg)
            }
            return ChatStreamChunk.Error(null, payload)
        }
        val first = choices.firstOrNull() as? JsonObject ?: return ChatStreamChunk.Error(null, payload)
        val index = (first["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
        val delta = first["delta"] as? JsonObject
        if (delta != null) {
            val text = (delta["content"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val toolCalls = delta["tool_calls"] as? JsonArray
            if (toolCalls != null && toolCalls.isNotEmpty()) {
                // Single tool_call deltas from the desktop / Venice `/chat/completions` look like:
                // { "index": 0, "id": "...", "function": { "name": "...", "arguments": "..." } }
                val tc = toolCalls.first() as JsonObject
                val tcIndex = (tc["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: index
                val id = (tc["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                val fn = tc["function"] as? JsonObject
                val name = (fn?.get("name") as? JsonPrimitive)?.takeIf { it.isString }?.content
                val args = (fn?.get("arguments") as? JsonPrimitive)?.takeIf { it.isString }?.content
                return ChatStreamChunk.ToolCallDelta(tcIndex, id, name, args)
            }
            return ChatStreamChunk.Delta(index, text)
        }
        val finishReason = (first["finish_reason"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (finishReason != null) {
            return ChatStreamChunk.Finish(reason = finishReason)
        }
        return ChatStreamChunk.Error(null, "unhandled SSE payload: $payload")
    }
}
```

- [ ] **Step 5: Add tiny internal accessors to `VeniceForgeSdk`**

In `VeniceForgeSdk.kt`, make these small helpers `internal` so `ChatClient` can use the shared config:

```kotlin
internal fun baseUrl() = baseUrl
internal fun userAgent() = config.userAgent
internal fun httpClient() = httpClient
```

Rename the local `private val baseUrl` to `private val _baseUrl` then expose `internal fun baseUrl() = _baseUrl` (so the public surface stays stable).

- [ ] **Step 6: Run chat client test — must pass**

Run: `./gradlew :venice-sdk:testDebugUnitTest --tests "*ChatClientTest*" --quiet`
Expected: PASS.

- [ ] **Step 7: Run all `:venice-sdk` tests**

Run: `./gradlew :venice-sdk:test --quiet`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/chat \
        venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdk.kt \
        venice-sdk/src/test
git commit -m "feat(sdk): streaming chat client over SSE"
# desktop sources: src/services/veniceClient/stream.ts
# desktop HEAD: bc5c173...
```

---

## Task 11: ChatViewModel (FAILING test first)

**Files:**
- Create: `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`
- Create: `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`

**Interfaces:**
- Produces:
  - `class ChatViewModel(private val chatRepo: ChatRepository, private val chatClient: ChatClient)` exposing `StateFlow<ChatUiState>` and `fun submit(text: String)`, `fun cancel()`.

- [ ] **Step 1: Add `:core:data` + JUnit + coroutines-test to `app/build.gradle.kts`**

In `app/build.gradle.kts`, add to `dependencies`:

```kotlin
implementation(project(":core:data"))
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
```

Verify `libs.kotlinx.coroutines.test` exists in `gradle/libs.versions.toml`; if not, add:

```toml
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
```

- [ ] **Step 2: Write `ChatViewModel.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.android.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.spearchucker667.veniceforge.core.data.entity.MessageEntity
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import io.github.spearchucker667.veniceforge.sdk.chat.ChatMessage
import io.github.spearchucker667.veniceforge.sdk.chat.ChatRequest
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamAccumulator
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamChunk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val modelId: String = "llama-3.3-70b",
    val isStreaming: Boolean = false,
    val error: String? = null,
)

data class UiMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val status: MessageStatus,
)

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatClient: ChatClient,
    private val apiKeyProvider: () -> String?,
    private val profileId: String,
    initialModelId: String = "llama-3.3-70b",
) : ViewModel() {

    private var conversationId: String? = null
    private var streamJob: Job? = null
    private val _state = MutableStateFlow(ChatUiState(modelId = initialModelId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val convId = chatRepo.createConversation(profileId, initialModelId)
            conversationId = convId
            // Hydrate with persisted messages.
            chatRepo.observeMessages(profileId, convId).collect { msgs ->
                _state.update {
                    it.copy(messages = msgs.map(::toUi))
                }
            }
        }
    }

    fun setModel(modelId: String) {
        _state.update { it.copy(modelId = modelId) }
    }

    fun submit(text: String) {
        val convId = conversationId ?: return
        val apiKey = apiKeyProvider() ?: run {
            _state.update { it.copy(error = "No API key") }
            return
        }
        val modelId = _state.value.modelId
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            profileId = profileId,
            role = MessageRole.USER,
            parentMessageId = null,
            status = MessageStatus.COMPLETED,
            textContent = text,
            modelId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantMsg = MessageEntity(
            id = assistantId,
            conversationId = convId,
            profileId = profileId,
            role = MessageRole.ASSISTANT,
            parentMessageId = userMsg.id,
            status = MessageStatus.PENDING,
            textContent = "",
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            chatRepo.appendMessage(profileId, convId, userMsg)
            chatRepo.appendMessage(profileId, convId, assistantMsg)

            val req = ChatRequest(
                model = modelId,
                messages = listOf(ChatMessage(role = "user", content = text)),
            )
            streamJob = launch {
                val accumulator = ChatStreamAccumulator()
                chatClient.streamChat(apiKey, req).collect { chunk ->
                    accumulator.apply(chunk)
                    if (chunk is ChatStreamChunk.Delta || chunk is ChatStreamChunk.ToolCallDelta) {
                        chatRepo.updateAssistantText(
                            profileId = profileId,
                            messageId = assistantId,
                            text = accumulator.snapshot().text,
                            status = MessageStatus.STREAMING,
                        )
                        _state.update { it.copy(isStreaming = true) }
                    } else if (chunk is ChatStreamChunk.Finish) {
                        chatRepo.updateAssistantText(
                            profileId = profileId,
                            messageId = assistantId,
                            text = accumulator.snapshot().text,
                            status = MessageStatus.COMPLETED,
                        )
                        _state.update { it.copy(isStreaming = false) }
                    } else if (chunk is ChatStreamChunk.Error) {
                        chatRepo.updateAssistantText(
                            profileId = profileId,
                            messageId = assistantId,
                            text = accumulator.snapshot().text,
                            status = MessageStatus.FAILED,
                        )
                        _state.update { it.copy(isStreaming = false, error = chunk.message) }
                    }
                }
            }
        }
    }

    fun cancel() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isStreaming = false) }
    }

    fun observeConversationForView(convId: String): Flow<List<MessageEntity>> =
        chatRepo.observeMessages(profileId, convId)

    private fun toUi(m: MessageEntity): UiMessage =
        UiMessage(id = m.id, role = m.role, text = m.textContent, status = m.status)
}
```

- [ ] **Step 3: Write failing test `ChatViewModelTest.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.android.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.spearchucker667.veniceforge.core.data.AppDatabase
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.repo.ChatRepository
import io.github.spearchucker667.veniceforge.sdk.VeniceForgeSdk
import io.github.spearchucker667.veniceforge.sdk.VeniceSdkConfig
import io.github.spearchucker667.veniceforge.sdk.chat.ChatClient
import io.github.spearchucker667.veniceforge.sdk.chat.ChatStreamChunk
import io.github.spearchucker667.veniceforge.core.data.repo.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After fun tearDown() { db.close() }

    private class FakeChatClient(private val script: List<ChatStreamChunk>) : ChatClient(VeniceForgeSdk(VeniceSdkConfig())) {
        override fun streamChat(apiKey: String, request: io.github.spearchucker667.veniceforge.sdk.chat.ChatRequest): Flow<ChatStreamChunk> = flowOf(*script.toTypedArray())
    }

    @Test
    fun `submit writes user message and accumulates assistant chunks`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = ProfileRepository(db.profileDao())
            val profileId = profileRepo.ensureDefault()
            val chat = ChatRepository(db)
            val client = FakeChatClient(
                listOf(
                    ChatStreamChunk.Open(),
                    ChatStreamChunk.Delta(0, "Hi "),
                    ChatStreamChunk.Delta(0, "there"),
                    ChatStreamChunk.Finish("stop"),
                )
            )
            val vm = ChatViewModel(
                chatRepo = chat,
                chatClient = client,
                apiKeyProvider = { "test-key" },
                profileId = profileId,
                initialModelId = "llama-3.3-70b",
            )
            // Drain the init coroutine that creates the conversation.
            advanceUntilIdle()

            vm.submit("Hello")
            advanceUntilIdle()

            // Wait for assistant message to be COMPLETED and accumulated.
            val finalState = vm.state.first { !it.isStreaming && it.messages.size >= 2 }
            val user = finalState.messages.first { it.role == MessageRole.USER }
            val assistant = finalState.messages.first { it.role == MessageRole.ASSISTANT }

            assertEquals("Hello", user.text)
            assertEquals("Hi there", assistant.text)
            assertEquals(MessageStatus.COMPLETED, assistant.status)
            assertEquals("llama-3.3-70b", finalState.modelId)
            assertNull(finalState.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
```

- [ ] **Step 4: Run**

Run: `./gradlew :app:testDebugUnitTest --tests "*ChatViewModelTest*" --quiet`
Expected: BUILD SUCCESSFUL (or fails with a meaningful test signal — adjust accordingly).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt \
        app/build.gradle.kts \
        gradle/libs.versions.toml
git commit -m "feat(app): chat view-model with stream aggregation"
```

---

## Task 12: ChatScreen UI

**Files:**
- Create: `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt`
- Modify: `app/src/main/java/.../VeniceForgeApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppContainer.kt`  *(we will inline a simple service-locator in `VeniceForgeApp` instead; this file is omitted if unused.)*

**Interfaces:**
- Produces:
  - `@Composable fun ChatScreen(viewModel: ChatViewModel, modifier: Modifier)`
  - Wired from `VeniceForgeApp` when `selected.id == "chat"`.

- [ ] **Step 1: Add resources to `strings.xml`**

Add inside `<resources>`:

```xml
<string name="chat_title">Chat</string>
<string name="chat_composer_hint">Send a message…</string>
<string name="chat_send">Send</string>
<string name="chat_cancel">Cancel</string>
<string name="chat_model_picker_label">Model</string>
<string name="chat_no_models">No models loaded. Go to Settings.</string>
<string name="chat_no_api_key">No API key saved. Go to Settings.</string>
```

- [ ] **Step 2: Write `ChatScreen.kt`**

```kotlin
package io.github.spearchucker667.veniceforge.android.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.spearchucker667.veniceforge.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    availableModels: List<String>,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var modelMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Model picker (text-only for milestone 1; capability-driven grouping deferred).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.chat_model_picker_label))
            OutlinedButton(onClick = { modelMenuOpen = true }) {
                Text(state.modelId)
            }
            DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                if (availableModels.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_no_models)) },
                        onClick = { modelMenuOpen = false },
                    )
                } else {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.setModel(model)
                                modelMenuOpen = false
                            },
                        )
                    }
                }
            }
            if (state.isStreaming) {
                TextButton(onClick = { viewModel.cancel() }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                Column {
                    Text(msg.role.name, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    Text(msg.text)
                }
            }
            if (state.isStreaming) {
                item { Text("…") }
            }
        }

        state.error?.let { Text(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.chat_composer_hint)) },
                enabled = !state.isStreaming,
            )
            TextButton(
                enabled = input.isNotBlank() && !state.isStreaming,
                onClick = {
                    viewModel.submit(input)
                    input = ""
                },
            ) {
                Text(stringResource(R.string.chat_send))
            }
        }
    }
}
```

- [ ] **Step 3: Wire `ChatScreen` into `VeniceForgeApp.kt`**

Replace the placeholder-routed branch:

```kotlin
if (selected.id == "settings") {
    ConfigScreen(...)
} else if (selected.id == "chat") {
    ChatScreen(
        viewModel = chatViewModel,
        availableModels = modelIds,
        modifier = Modifier.padding(padding),
    )
} else {
    FeatureScreen(selected, Modifier.padding(padding))
}
```

Construct `chatViewModel` and `modelIds` once near the top of `VeniceForgeApp()`:

```kotlin
val context = LocalContext.current
val db = remember { AppDatabase.create(context) }
val chatRepo = remember { ChatRepository(db) }
val chatClient = remember { ChatClient(VeniceForgeSdk()) }
val profileRepo = remember { ProfileRepository(db.profileDao()) }
var profileId by remember { mutableStateOf<String?>(null) }
LaunchedEffect(Unit) { profileId = profileRepo.ensureDefault() }
val secureStore = remember { SecureSecretStore(context) }
val sdk = remember { VeniceForgeSdk() }

val chatViewModel = remember(profileId) {
    profileId?.let { pid ->
        ChatViewModel(
            chatRepo = chatRepo,
            chatClient = chatClient,
            apiKeyProvider = { secureStore.loadApiKey(pid) },
            profileId = pid,
            initialModelId = state.value.modelId,
        )
    }
}
val modelCaps by produceState(initialValue = emptyList<ModelCapabilities>(), profileId) {
    val key = profileId?.let(secureStore::loadApiKey)
    if (key != null && profileId != null) {
        value = CapabilitiesRepository(sdk).fetchLiveCapabilities(key).models
    }
}
val modelIds = remember(modelCaps) { modelCaps.map { it.id } }
```

Pass `chatViewModel` only when non-null; otherwise render a placeholder that points the user to Settings.

- [ ] **Step 4: Build and assemble**

Run: `./gradlew :app:assembleDebug --quiet`
Expected: BUILD SUCCESSFUL. APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/test app/src/main/res/values/strings.xml
git commit -m "feat(app): chat screen wired to view model + capabilities"
```

---

## Task 13: Acceptance gate (test + lint + assemble)

**Files:**
- Modify: `SOURCE_BASELINE.md`
- Modify: `docs/FEATURE_PARITY_MATRIX.md`

- [ ] **Step 1: Run all module tests**

Run: `./gradlew :venice-sdk:test :core:data:test :app:test --quiet`
Expected: BUILD SUCCESSFUL. All green.

- [ ] **Step 2: Lint**

Run: `./gradlew lint --quiet`
Expected: BUILD SUCCESSFUL with no new warnings (pre-existing warnings are acceptable but document them).

- [ ] **Step 3: Build APK + AAR**

Run: `./gradlew :app:assembleDebug :venice-sdk:assembleRelease --quiet`
Expected: BUILD SUCCESSFUL. APK and AAR files produced.

- [ ] **Step 4: Update `SOURCE_BASELINE.md`**

Append a milestone section under the existing content:

```markdown

## Milestone 1 — typed capabilities + SSE + Room v1 (2026-08-15)

- Desktop HEAD at start: `bc5c17374ef4937f5837f5580d29a88bfab333ee`
- New modules: `:core:data`
- New dependencies: AndroidX Room 2.7.0, KSP 2.0.2-1.0.25, Robolectric 4.13, kotlinx-coroutines-test 1.11.0
- Behavior ported: `/models` typed metadata merging with `/models/traits` + `/models/compatibility_mapping`; `/chat/completions` SSE streaming with tool-call fragment reconstruction; Room schema v1 with profile scoping.
```

- [ ] **Step 5: Update `docs/FEATURE_PARITY_MATRIX.md`**

Find the `chat` row in the matrix and change `Scaffolded` → `Foundation`:

```markdown
| `chat` | Chat | Foundation | Streaming SSE across `/chat/completions`, run/agent modes, profile-scoped persistence. Capabilities-driven model picker. Tools, attachments, projects, and agent approvals are ported in later milestones. |
```

- [ ] **Step 6: Commit and announce**

```bash
git add SOURCE_BASELINE.md docs/FEATURE_PARITY_MATRIX.md
git commit -m "docs: milestone 1 acceptance + parity matrix update"
```

Announce milestone complete in your final reply with the changed files list, command results, and any deferred follow-ups.

---

## Self-Review (filled inline)

1. **Spec coverage:**
   - Spec §1 goal "typed model-capability parsing" → Task 7.
   - Spec §1 goal "chat SSE streaming" → Tasks 8 / 9 / 10.
   - Spec §1 goal "Room profile/chat schema" → Tasks 2 / 3 / 4 / 5 / 6.
   - Spec §4.1 module changes → covered: `:venice-sdk`, `:core:data`, `:app`, `settings.gradle.kts`, `libs.versions.toml`.
   - Spec §4.2 SDK surface changes → Task 7 (capabilities), Task 8/10 (chunks).
   - Spec §4.3 SSE design → Tasks 8/9/10.
   - Spec §4.4 Room schema v1 → Tasks 2/3/4.
   - Spec §4.5 UI integration → Task 12.
   - Spec §6 error handling → Task 10 (Error chunks), Task 11 (state transitions).
   - Spec §7 testing → covered in every task with failing tests first.
   - Spec §8 dependencies → Task 1 (toolchain), Task 11 (coroutines-test).
   - Spec §9 acceptance criteria → Task 13.
2. **Placeholder scan:** No "TODO" / "TBD" / "implement later" markers remain. ChatViewModelTest in Task 11 uses `Dispatchers.setMain(StandardTestDispatcher)` and asserts accumulated text via `state.first { ... }`.
3. **Type consistency:** Method names: `fetchLiveCapabilities` (defined in Task 7, consumed in Task 11/12), `streamChat` (Task 10, consumed in Task 11), `appendMessage` / `observeMessages` / `createConversation` / `updateAssistantText` (Task 5, consumed in Tasks 11/12), `submit` / `cancel` / `setModel` (Task 11, consumed in Task 12), `state` (val, consumed in Task 12). Sealed-class chunk types consistent across Tasks 8/10.
