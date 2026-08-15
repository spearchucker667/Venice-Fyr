# Core Data / Persistence Audit Findings

**Auditor scope:** `core/data` production sources and tests.  
**Audit date:** 2026-08-15.  
**Methodology:** Static analysis only; no Gradle commands executed. Source-of-truth for persistence behavior is the Room schema/annotations and the checked-in `1.json` schema export.

---

## Ledger — files reviewed

| Path | Lines | Reviewed | Findings |
|---|---|---|---|
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt` | 47 | Y | 3 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt` | 21 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt` | 22 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt` | 83 | Y | 3 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt` | 27 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt` | 17 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageDao.kt` | 31 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt` | 33 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt` | 30 | Y | 1 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt` | 38 | Y | 2 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ProfileEntity.kt` | 14 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationFolderEntity.kt` | 27 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/ConversationEntity.kt` | 43 | Y | 0 |
| `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageToolCallEntity.kt` | 35 | Y | 0 |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt` | 51 | Y | 1 |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileIsolationTest.kt` | 67 | Y | 0 |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` | 59 | Y | 0 |
| `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` | 38 | Y | 0 |
| `core/data/build.gradle.kts` | 56 | Y | 0 |
| `core/data/schemas/io.github.spearchucker667.veniceforge.core.data.AppDatabase/1.json` | 478 | Y | 0 |

**Total files reviewed:** 20.  
**Total findings:** 16 (P0: 0, P1: 5, P2: 9, P3: 2).

---

## Findings

### DATA-01 | Severity: P2 | Status: CONFIRMED | Area: Schema / Referential Integrity | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt`  
**Lines:** 12–37  
**Symbol:** `parentMessageId`

**Evidence:**
```kotlin
@Entity(
    tableName = "messages",
    indices = [ ..., Index("parentMessageId"), ],
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
    ...
    val parentMessageId: String?,
    ...
)
```
`parentMessageId` is indexed but has no `ForeignKey` referencing `MessageEntity.id`. The exported schema confirms a plain nullable TEXT column with no FK.

**Expected:** A self-referencing `ForeignKey` on `parentMessageId` with `onDelete = CASCADE` or `SET_NULL`.

**Actual:** Deleting a parent message leaves child messages with a dangling `parentMessageId`.

**Impact:** Threaded UI may reference deleted messages; referential integrity is degraded.

**Root cause:** Missing `ForeignKey` annotation for the self-reference.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `ForeignKey` constraints; SQLite `ON DELETE` actions.

**Remediation:** Add a self-referencing `ForeignKey` with an appropriate `onDelete` action; bump schema version and supply a migration.

**Tests required:** Unit test that deletes a parent message and asserts children are cascaded or nulled.

**Compatibility impact:** Schema change requires migration.

---

### DATA-02 | Severity: P2 | Status: CONFIRMED | Area: Schema / Profile Isolation | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/entity/MessageEntity.kt`  
**Lines:** 12–37  
**Symbol:** `profileId`

**Evidence:** `MessageEntity.profileId` is a plain non-null TEXT column with no `ForeignKey` to `profiles(id)`. The repository (`ChatRepository.appendMessage`) validates `message.profileId` at runtime, but the database does not enforce it.

**Expected:** `profileId` should reference `ProfileEntity.id` or, at minimum, a `CHECK` constraint ensuring it matches the parent conversation's `profileId`.

**Actual:** A message can be inserted with a `profileId` that does not match its conversation's `profileId` if any caller bypasses `ChatRepository`.

**Impact:** Cross-profile data inconsistency; DAO-level isolation relies on caller discipline rather than schema guarantees.

**Root cause:** Missing FK/CHECK on `MessageEntity.profileId`.

**Related occurrences:** `ConversationEntity.profileId` and `ConversationFolderEntity.profileId` have FKs; `MessageEntity` is inconsistent.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `ForeignKey`; SQLite referential integrity.

**Remediation:** Add `ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = CASCADE)`.

**Tests required:** Migration/schema test; negative test inserting a message with invalid `profileId`.

**Compatibility impact:** Schema change requires migration.

---

### DATA-03 | Severity: P1 | Status: CONFIRMED | Area: Atomicity / Profile Management | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ProfileRepository.kt`  
**Lines:** 7–20  
**Symbol:** `ensureDefault`

**Evidence:**
```kotlin
suspend fun ensureDefault(): String {
    dao.findDefault()?.let { return it.id }
    val now = System.currentTimeMillis()
    val entity = ProfileEntity(...)
    dao.insert(entity)
    return DEFAULT_PROFILE_ID
}
```
`findDefault()` and `insert()` are not wrapped in a transaction. `ProfileDao.insert` uses `OnConflictStrategy.ABORT`.

**Expected:** Idempotent default-profile creation even under concurrent callers.

**Actual:** Two concurrent coroutines can both observe `findDefault() == null`, both attempt `insert`, and the second will throw `SQLiteConstraintException`.

**Impact:** First-launch crash or unhandled exception when multiple components trigger profile initialization.

**Root cause:** Read-then-write without transaction/atomic `INSERT OR IGNORE`.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; SQLite `INSERT OR IGNORE` semantics.

**Remediation:** Wrap `findDefault` + `insert` in `withTransaction`, or change DAO to `INSERT OR IGNORE` and retry.

**Tests required:** Concurrent `ensureDefault()` calls must produce exactly one profile without exceptions.

**Compatibility impact:** Behavior change only for the race window; no schema change.

---

### DATA-04 | Severity: P1 | Status: CONFIRMED | Area: Conversation Consistency | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 45–54  
**Symbol:** `appendMessage`

**Evidence:**
```kotlin
suspend fun appendMessage(profileId: String, conversationId: String, message: MessageEntity) {
    ...
    db.withTransaction {
        require(conversationDao.findById(profileId, conversationId) != null) { ... }
        messageDao.upsert(message)
    }
}
```
The transaction updates `messages` only; `conversations.updatedAt` is never touched.

**Expected:** Appending a message should atomically update the parent conversation's `updatedAt` so `observeConversations` ordering reflects recent activity.

**Actual:** `updatedAt` remains at creation time; a conversation with a newer message can appear below a conversation created later.

**Impact:** Broken "recent conversations" ordering — a core history feature.

**Root cause:** Repository omits the conversation update.

**Related occurrences:** `updateAssistantText` (DATA-05).

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; Room `@Update`.

**Remediation:** Inside the transaction, also call `conversationDao.update(copy(updatedAt = now))`.

**Tests required:** ChatRepository test asserting conversation `updatedAt` advances after `appendMessage`.

**Compatibility impact:** No schema change.

---

### DATA-05 | Severity: P1 | Status: CONFIRMED | Area: Conversation Consistency / Atomicity | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 56–69  
**Symbol:** `updateAssistantText`

**Evidence:**
```kotlin
suspend fun updateAssistantText(profileId: String, messageId: String, text: String, status: MessageStatus) {
    messageDao.updateTextAndStatus(
        profileId = profileId,
        id = messageId,
        text = text,
        status = status,
        updatedAt = System.currentTimeMillis(),
    )
}
```
There is no `db.withTransaction`, and the parent conversation's `updatedAt` is not updated.

**Expected:** Streaming updates should be atomic with the message row and should refresh the conversation's `updatedAt`.

**Actual:** Each streaming chunk updates the message only; conversation ordering is stale, and there is no transaction boundary if a future change adds side effects.

**Impact:** Stale conversation list during/after streaming; potential partial-write inconsistency.

**Root cause:** Repository updates only the message table and skips the conversation row.

**Related occurrences:** `appendMessage` (DATA-04).

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.withTransaction`; generated `@Query` updates.

**Remediation:** Wrap in `db.withTransaction` and update the parent conversation's `updatedAt`.

**Tests required:** Test that `updateAssistantText` advances conversation `updatedAt` and that message + conversation updates are atomic.

**Compatibility impact:** No schema change.

---

### DATA-06 | Severity: P2 | Status: CONFIRMED | Area: Profile/Conversation Isolation | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/repo/ChatRepository.kt`  
**Lines:** 56–69  
**Symbol:** `updateAssistantText`

**Evidence:** `updateAssistantText` accepts `profileId` and `messageId` but not `conversationId`. The DAO query is:
```kotlin
@Query("UPDATE messages SET ... WHERE id = :id AND profileId = :profileId")
```
It filters by `profileId` + `id` only.

**Expected:** The repository should verify the message belongs to the conversation the caller intends to update.

**Actual:** A message in another conversation (same profile) can be updated if its `messageId` is supplied.

**Impact:** Cross-conversation message mutation/data corruption if the UI misidentifies a message ID.

**Root cause:** Missing `conversationId` parameter and validation.

**Related occurrences:** `MessageDao.updateTextAndStatus` line 23.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Query` scoping.

**Remediation:** Add `conversationId` to the method signature and include it in the WHERE clause (or load the message and verify `conversationId`).

**Tests required:** Negative test updating a message in the wrong conversation.

**Compatibility impact:** API signature change for callers.

---

### DATA-07 | Severity: P3 | Status: CONFIRMED | Area: Clarity / Maintainability | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ConversationDao.kt`  
**Lines:** 29–32  
**Symbol:** `deleteCascade`

**Evidence:**
```kotlin
@Transaction
suspend fun deleteCascade(profileId: String, id: String) {
    deleteById(profileId, id)
}
```
The method name implies it performs manual cascade deletion, but it only calls `deleteById`. The actual cascade is performed by SQLite via the `ForeignKey.CASCADE` on `MessageEntity.conversationId`.

**Expected:** Either delete children manually or rename the method to reflect that it relies on DB-level cascade.

**Actual:** Misleading name; future maintainers may add duplicate deletion logic or assume it is missing.

**Impact:** Maintenance confusion; risk of double deletes or missed cascade if FKs change.

**Root cause:** Naming does not match implementation.

**Related occurrences:** `ChatRepository.deleteConversation` calls `conversationDao.deleteById` directly, not `deleteCascade`.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Transaction`; SQLite FK `ON DELETE CASCADE`.

**Remediation:** Rename to `deleteByProfileAndId` or remove the wrapper and rely on callers using `deleteById`.

**Tests required:** None beyond existing cascade tests.

**Compatibility impact:** Internal API rename only.

---

### DATA-08 | Severity: P2 | Status: CONFIRMED | Area: Profile Isolation / Tool Calls | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/MessageToolCallDao.kt`  
**Lines:** 15–16  
**Symbol:** `observeForMessage`

**Evidence:**
```kotlin
@Query("SELECT * FROM message_tool_calls WHERE messageId = :messageId ORDER BY createdAt ASC")
fun observeForMessage(messageId: String): Flow<List<MessageToolCallEntity>>
```
The query filters only by `messageId`; there is no `profileId` join to `messages`.

**Expected:** Tool-call observation should be scoped by profile (e.g., `JOIN messages ON ... WHERE messages.profileId = :profileId`).

**Actual:** Any caller with a valid `messageId` can observe tool calls for that message, regardless of profile ownership.

**Impact:** Potential cross-profile tool-call content leak if message IDs are exposed or guessed.

**Root cause:** DAO query lacks profile scoping; there is no repository wrapper enforcing it either.

**Related occurrences:** `MessageToolCallDao.upsert` also lacks profile/conversation validation.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Query` joins; SQLite foreign keys.

**Remediation:** Add a `profileId` parameter and join through `messages`, or expose tool-call operations only through a profile-scoped repository.

**Tests required:** Profile isolation test for tool calls.

**Compatibility impact:** DAO signature change.

---

### DATA-09 | Severity: P1 | Status: CONFIRMED | Area: Security / Encryption | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 36–43  
**Symbol:** `create`

**Evidence:**
```kotlin
fun create(context: Context): AppDatabase =
    Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "venice_forge.db",
    )
        .build()
```
The database is created with plain Room/SQLite. No SQLCipher, no `SupportFactory` encryption, no Keystore-backed key.

`docs/SECURITY_AND_STORAGE_CONTRACT.md` point 2 states: "App persistence uses Android Keystore-backed encryption."

**Expected:** Chat history, prompts, and responses are encrypted at rest using a Keystore-backed key.

**Actual:** All `core:data` tables are stored in plaintext SQLite in app-private storage.

**Impact:** Sensitive user prompts/responses are readable via rooted-device access or backup extraction; violates the project's storage contract.

**Root cause:** `AppDatabase.create` does not configure encryption.

**Related occurrences:** `SecureSecretStore` encrypts API keys, but no equivalent exists for the Room database.

**Venice reference:** N/A.

**Android/Kotlin reference:** `androidx.security.crypto.EncryptedFile`/SQLCipher; Android Keystore.

**Remediation:** Adopt SQLCipher with a Keystore-derived key, or store sensitive message content in encrypted blobs outside Room.

**Tests required:** Verify database file is not plaintext; key rotation/recovery tests.

**Compatibility impact:** Major change; existing DB must be migrated to encrypted format.

---

### DATA-10 | Severity: P2 | Status: CONFIRMED | Area: Corruption Handling | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 36–43  
**Symbol:** `create`

**Evidence:** `Room.databaseBuilder` is built without `fallbackToDestructiveFromCorruption()` or any corruption listener. Room's default behavior is to throw an `IllegalStateException` when the integrity check fails.

**Expected:** A corruption recovery strategy (e.g., destructive fallback with data-loss logging, or a backup restore path).

**Actual:** Corruption causes a hard crash and renders the app unusable until the user clears app data.

**Impact:** Data loss / app bricking on disk corruption.

**Root cause:** No corruption handler configured.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `RoomDatabase.Builder.fallbackToDestructiveFromCorruption()`; `DefaultDatabaseErrorHandler`.

**Remediation:** Register a corruption callback that logs a redacted event and triggers destructive fallback or backup restore.

**Tests required:** Corruption simulation test.

**Compatibility impact:** Behavior change on corruption only.

---

### DATA-11 | Severity: P2 | Status: CONFIRMED | Area: Migrations | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/AppDatabase.kt`  
**Lines:** 18–46  
**Symbol:** `AppDatabase`

**Evidence:**
- `version = 1`.
- Builder does not call `addMigrations()` or `fallbackToDestructiveMigration()`.
- `MigrationTest` only exercises version 1.

**Expected:** A migration strategy for future schema changes (either explicit `Migration` objects or an intentional destructive fallback).

**Actual:** Any version bump without a supplied `Migration` will cause Room to throw `IllegalStateException` on app launch.

**Impact:** Future schema evolution is blocked unless migrations are added before shipping a new version.

**Root cause:** No migration fallback configured; migration test coverage is minimal.

**Related occurrences:** `docs/superpowers/plans/2026-08-15-android-port-milestone-1.md` line 821 mentions `fallbackToDestructiveMigrationOnDowngrade(true)` in plans, but production code lacks it.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room migrations; `RoomDatabase.Builder.fallbackToDestructiveMigration()`.

**Remediation:** Add `fallbackToDestructiveMigration()` (with product approval for data loss) or commit to writing explicit migrations and keep `MigrationTest` current.

**Tests required:** Round-trip migration test for every future schema version.

**Compatibility impact:** Affects future upgrades.

---

### DATA-12 | Severity: P2 | Status: CONFIRMED | Area: Schema Robustness | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/Converters.kt`  
**Lines:** 9–20  
**Symbol:** enum converters

**Evidence:**
```kotlin
@TypeConverter fun toMessageRole(v: String?): MessageRole? = v?.let { MessageRole.valueOf(it) }
```
All four enum converters use `Enum.valueOf()` directly.

**Expected:** Graceful handling of unknown enum values (e.g., default to a safe value or `null`) to avoid crashes when an enum is renamed or a stale row is read.

**Actual:** Any enum name not present in the current code will throw `IllegalArgumentException` at read time.

**Impact:** App crash on schema drift, downgrades, or corrupted enum strings.

**Root cause:** `valueOf` used without a fallback.

**Related occurrences:** `MessageRole`, `MessageStatus`, `ConversationKind`, `ToolCallStatus` converters.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `Enum.valueOf`; Room `@TypeConverter`.

**Remediation:** Replace `valueOf` with `enumValues<T>().find { it.name == v }` and define an explicit unknown/default mapping.

**Tests required:** Unit tests reading each enum column with an unrecognized string.

**Compatibility impact:** Behavior change for invalid enum data.

---

### DATA-13 | Severity: P3 | Status: CONFIRMED | Area: API Surface / Module Boundaries | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/DataServices.kt`  
**Lines:** 7–16  
**Symbol:** `DataServices`

**Evidence:**
```kotlin
/**
 * Minimal service-locator that hides Room types from `:app`. `:app` never sees
 * [AppDatabase] or any Room dependency — it only needs a single entry point...
 */
class DataServices private constructor(
    private val db: AppDatabase,
) {
    val chatRepository: ChatRepository by lazy { ChatRepository(db) }
    ...
}
```
`ChatRepository` has a public constructor that takes `AppDatabase` (`core/data/src/main/java/.../repo/ChatRepository.kt` line 14). Because `ChatRepository` is public and exposed via `DataServices`, `:app` can import it and see the Room type in the signature.

**Expected:** Room types should be internal to the module, or the comment should accurately describe the current design.

**Actual:** Room type leakage through `ChatRepository`'s public constructor.

**Impact:** `:app` can inadvertently depend on Room types; breaks the stated abstraction.

**Root cause:** `ChatRepository` constructor is public and accepts `AppDatabase`.

**Related occurrences:** `ChatRepository.kt` line 14.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin `internal` visibility; dependency inversion.

**Remediation:** Make `ChatRepository` constructor `internal` and inject DAOs instead of the database.

**Tests required:** Compile-time/API surface test ensuring `:app` cannot reference `AppDatabase`.

**Compatibility impact:** Internal API change.

---

### DATA-14 | Severity: P2 | Status: CONFIRMED | Area: Profile Integrity | Module: `core:data`

**File:** `core/data/src/main/java/io/github/spearchucker667/veniceforge/core/data/dao/ProfileDao.kt`  
**Lines:** 19–26  
**Symbol:** `findDefault` / `deleteById`

**Evidence:**
```kotlin
@Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
suspend fun findDefault(): ProfileEntity?
```
There is no unique index or constraint on `isDefault = 1`. `ProfileDao.deleteById(id)` has no guard against deleting the default profile.

**Expected:** Exactly one default profile at any time; deletion of the default profile should be prevented or should trigger default reassignment.

**Actual:** Multiple rows can have `isDefault = 1`; `findDefault` returns an arbitrary one. The default profile can be deleted without recourse.

**Impact:** Inconsistent default-profile selection; app may lose the active profile.

**Root cause:** Schema and DAO do not enforce singleton default semantics.

**Related occurrences:** `ProfileEntity.kt` lines 6–13; `ProfileRepository.kt` lines 7–20.

**Venice reference:** N/A.

**Android/Kotlin reference:** Room `@Index(unique = true)` with partial index; SQLite triggers.

**Remediation:**
- Add a migration that adds a unique partial index on `isDefault = 1` (or enforce in code).
- Guard `deleteById` to prevent deleting the last default profile, or reassign default before deletion.

**Tests required:** Test that two defaults cannot exist; test deletion of default profile.

**Compatibility impact:** Schema change requires migration.

---

### DATA-15 | Severity: P2 | Status: CONFIRMED | Area: Test Coverage / Migrations | Module: `core:data`

**File:** `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt`  
**Lines:** 26–50  
**Symbol:** `MigrationTest`

**Evidence:** The test suite contains only:
1. `v1 schema creates all expected tables` — checks `sqlite_master` table names.
2. `AppDatabase can open v1` — opens and closes the DB.

It does not validate:
- Foreign keys or `ON DELETE` actions.
- Index definitions.
- Column nullability / types.
- Destructive fallback behavior.

**Expected:** Migration tests should verify schema integrity and future migration paths.

**Actual:** Only table existence is checked.

**Impact:** Schema regressions (missing FKs, indices, columns) can slip through.

**Root cause:** Minimal test assertions.

**Related occurrences:** None.

**Venice reference:** N/A.

**Android/Kotlin reference:** `MigrationTestHelper`; `PRAGMA foreign_key_check`; `PRAGMA index_list`.

**Remediation:** Add assertions for FKs, indices, and run `PRAGMA foreign_key_check` after each migration.

**Tests required:** Expand `MigrationTest`.

**Compatibility impact:** None.

---

### DATA-16 | Severity: P2 | Status: CONFIRMED | Area: Import / Export / Backup | Module: `core:data`

**File:** `core/data` module (no relevant source file)  
**Lines:** N/A  
**Symbol:** N/A

**Evidence:**
- `core/data` contains no import, export, backup, or sync APIs.
- `app/src/main/AndroidManifest.xml` lines 5–6 sets `android:allowBackup="false"` and `android:fullBackupContent="false"`.
- `docs/FEATURE_PARITY_MATRIX.md` lists `history` target as "Room persistence, folders, lock/import/export/recovery" and `privacy` target as "Storage inventory, encrypted `.vfbackup`, sync folder, purge/maintenance".

**Expected:** Core data layer should expose redacted export/import primitives (e.g., `.vfbackup`) backed by SAF/content URIs.

**Actual:** No persistence-level support for backup/export/import exists.

**Impact:** Feature-parity claims for history/privacy are not met; users cannot back up or migrate conversations.

**Root cause:** Not implemented in current milestone.

**Related occurrences:** `docs/FEATURE_PARITY_MATRIX.md` lines 9, 27, 43.

**Venice reference:** N/A.

**Android/Kotlin reference:** Storage Access Framework (SAF); `BackupAgent`; encrypted archives.

**Remediation:** Design and implement a profile-scoped, encrypted backup/export API in `core:data`.

**Tests required:** Round-trip backup/restore tests; profile isolation tests after restore.

**Compatibility impact:** New feature; no backward-compatibility risk yet.

---

## Areas checked with no confirmed findings

- **Flow emissions on wrong dispatchers:** All `Flow` sources are Room DAO flows or a single `.map` that filters by `profileId`. Room emits on its query executor; no `Dispatchers.Main` emission or `flowOn` mismatch was identified.
- **Plaintext credentials in `core:data`:** `ProfileEntity` stores only `apiKeyAlias`; actual API keys are handled by `core:security` (`SecureSecretStore`).
- **Telemetry / logging:** No logging of prompts, responses, or DB contents was found in the reviewed files.

---

## Summary counts

| Severity | Count |
|---|---|
| P0 | 0 |
| P1 | 5 |
| P2 | 9 |
| P3 | 2 |
| **Total** | **16** |
