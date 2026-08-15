# Venice Forge Android — Milestone 1 Design

**Date:** 2026-08-15
**Status:** Draft — awaiting user review
**Scope:** Immediate next implementation milestone identified by `ANDROID_PORT_HANDOFF.md`, § "Immediate next implementation milestone".
**Out of scope:** Later phases (Profile lockout, Image/Media Studio, Audio/Music/Video durable jobs, Research, Characters/RP, Documents, Workflows, Privacy/Settings/Themes/i18n/Status). Tracked separately.

---

## 1. Goals

Deliver three coupled Android-native primitives that together unlock the highest number of downstream features identified in the handoff:

1. **Typed model-capability parsing** — replace the current raw-JSON Venice `VeniceModel` with a structured capability graph derived from `/models`, `/models/traits`, and `/models/compatibility_mapping`.
2. **Chat SSE streaming** — native, OkHttp/coroutine SSE client for `/chat/completions` that reconstructs incremental content deltas and tool-call fragments across chunks and survives activity recreation.
3. **Room profile/chat schema** — durable per-profile persistence for profiles, conversations, and chat messages, with strict repository-level profile scoping.

Acceptance is the Phase 0/1/2/3 acceptance clauses narrowed to these three observables, plus tests proving profile isolation, capability-driven model gating in the chat picker, and progressive streaming rendering.

## 2. Non-goals

- New UI for any feature other than the minimum chat surface required to demonstrate parity for milestone 1.
- Tool execution / approvals flow (Phase 3 continuous work — covered in a later milestone).
- Image, audio, video, characters, research, documents, workflows, privacy backup/sync, themes, i18n, status, settings UI.
- Provider fallback adapters.
- Multi-profile UI (default profile only for milestone 1; profile picker UI is a later milestone).

## 3. Workspace layout — Option B confirmed

The handoff doc, AGENTS.md, and `scripts/bootstrap-desktop-source.sh` referred to an Android write target at `…/Venice_Forge_Android-Starter/`, but the actual starter tree lives at the project root:

```
/Users/super_user/Projects/Venice Fyr/   # WRITE: workspace root IS the Android tree
├── app/, venice-sdk/, core/, docs/      # Android modules + docs
├── .source/
│   └── Venice_Forge-desktop/           # READ ONLY: cloned Electron source
└── .local/                             # local clone state (gitignored)
```

**Decision (confirmed 2026-08-15): Option B — Update conventions to match reality.**

Applied changes:
- `AGENTS.md` path references now point at the project root.
- `ANDROID_PORT_HANDOFF.md` references updated.
- `README.md` first-run instructions updated.
- `docs/DESKTOP_SOURCE_BOOTSTRAP.md` layout diagram, instructions, and env-file paths updated.
- `scripts/bootstrap-desktop-source.sh` `ANDROID_ROOT` default now equals `$PROJECT_ROOT`.

Bootstrap is now satisfiable. Milestone 1 implementation proceeds.

## 4. Architecture

Layered delivery, SDK-first, UI-last. Each layer is independently testable.

```
                ┌──────────────────────────────┐
                │      :app  (Compose UI)     │
                │  — Chat screen replaces the │
                │     chat placeholder route  │
                │  — Model picker driven by   │
                │     ModelCapabilities       │
                └──────────────┬───────────────┘
                               │ injects
                ┌──────────────▼───────────────┐
                │  :core:data  (Room v1)       │
                │  — Profile, Conversation,    │
                │    Message entities + DAOs   │
                │  — ProfileScoped repos       │
                │  — Migration v1              │
                └──────────────┬───────────────┘
                               │ uses
                ┌──────────────▼───────────────┐
                │  :venice-sdk  (network)      │
                │  — listModels (typed)        │
                │  — getModelTraits            │
                │  — getModelCompatibilityMap  │
                │  — streamChat (SSE)          │
                │  — ChatStreamChunk delta type│
                │  — ToolCall accumulator      │
                └──────────────┬───────────────┘
                               │ uses
                ┌──────────────▼───────────────┐
                │  :core:common, :core:security│
                │  (Redactor, SecureSecretStore│
                │   unchanged)                 │
                └──────────────────────────────┘
```

### 4.1 Module changes overview

| Module | Change | Files added / edited |
|---|---|---|
| `venice-sdk` | Add `CapabilitiesRepository`, `ChatClient`, typed `ModelCapabilities`, `ChatRequest`, SSE delta types, fixtures/test doubles | several new files under `venice-sdk/src/main/.../sdk/{chat,capabilities}`; tests under `src/test/...` |
| `core:data` *(new)* | New Gradle module `core/data/` with Room schema v1, entities, DAOs, type converters, repositories, migration test | new module: `core/data/build.gradle.kts`, src tree, single migration test |
| `app` | Replace placeholder `Chat` screen with a working chat screen bound to ChatClient + Room repos. Add a model picker. Wire `ModelCapabilitiesRepository` to populate the picker. | `app/.../chat/...` new package; replace existing accordion placeholder for `selected.id == "chat"` |
| `core:common` | Unchanged | — |
| `core:security` | Unchanged | — |
| `core:designsystem` | Optional minimal additions (chat bubble, message list) | if reused; otherwise build natively in `app` |
| Root build | Add `:core:data` to `settings.gradle.kts` | one-line edit |
| Gradle versions | No new dependencies beyond Room; `androidx.room:*` entries added to `libs.versions.toml` + AGP 9.3-compatible Room version (latest 2.7.x stable available) | two entries added |

### 4.2 SDK surface changes

`venice-sdk` adds typed methods (no behavioral change to existing `listModels`). All methods are `suspend` and accept `apiKey: String` (no SDK-side credential persistence).

```
class CapabilitiesRepository(config, http = OkHttpClient()):
    suspend fun fetchLiveCapabilities(apiKey: String): ModelCatalog
    // Combines /models + /models/traits + /models/compatibility_mapping
    // into a single typed ModelCatalog.

class ChatClient(config, http = OkHttpClient()):
    suspend fun streamChat(
        apiKey: String,
        request: ChatRequest,
    ): Flow<ChatStreamChunk>  // backs onto chat/completions stream=true
    fun cancellation: cooperative via enclosing coroutine job.

data class ModelCatalog(
    val models: List<ModelCapabilities>,
    val refreshedAt: Instant,
    val sourceRevision: String?,  // recorded for parity debugging
)
```

Capabilities are flattened into Kotlin enums / typed fields where the JSON is well-documented (e.g. `modality:enum input/output`, `supportsSystemPrompt:Boolean`, `supportsToolCalling:Boolean`, `supportsImageInput:Boolean`, `maxContextTokens:Int?`). Unknown / inconsistent JSON keys are preserved as `rawJson` and dropped from typed access — never silently coerced.

`ChatStreamChunk` is a sealed type with cases:

```
sealed class ChatStreamChunk {
    object Open : ChatStreamChunk()
    data class Delta(val index: Int, val textFragment: String?) : ChatStreamChunk()
    data class ToolCallDelta(val index: Int, val toolCallId: String?, val name: String?, val argumentsFragment: String?) : ChatStreamChunk()
    data class Finish(val reason: String, val usage: Usage?) : ChatStreamChunk()
    data class Error(val code: Int?, val message: String) : ChatStreamChunk()
}
```

### 4.3 Chat SSE streaming design

- OkHttp `Response.body.byteStream()` read line by line, parsed by a dedicated `SseLineParser`.
- Namespace each `data:` payload through `Json { ignoreUnknownKeys = true }` to align with `/chat/completions` OpenAI-compatible SSE wire shape used by Venice.
- A `ChatStreamAccumulator` consumer-side turns `Delta` + `ToolCallDelta` into `AssistantMessage` state (text buffer + tool-call argument buffer). The consumer side lives in `:core:data` or `:app` depending on layering choice.
- Cancellation: `Flow.collect { }` propagates coroutine cancellation. The HTTP call is cancelled at the OkHttp `Call` level; in-flight bytes are discarded.
- Errors: status != 200 short-circuits to a `ChatStreamChunk.Error` with body parsed for Venice error JSON.
- Timeout: separate read timeout config (`VENICE_SSE_READ_TIMEOUT_SECONDS`) defaults higher than the standard request timeout. Surface a `VeniceSdkException.Network` if exceeded.
- Composition safety: streams survive `ViewModel` scope, not Composable scope. State collection via `StateFlow<AssistantMessage>` is the only UI-facing contract.

### 4.4 Room profile/chat schema (v1)

New module `:core:data` with the following Room entities (all inherit an `id: String PK` plus `createdAt: Long` / `updatedAt: Long`):

```
@Entity Profile
    id (PK), displayName, apiKeyAlias (FK to Keystore alias SHA — never plaintext), isDefault, createdAt, updatedAt

@Entity Conversation
    id (PK), profileId (FK→Profile.id, indexed), title, modelId, pinnedConversation (Bool), folderId nullable (FK→ConversationFolder.id), createdAt, updatedAt, lastOpenedAt nullable

@Entity ConversationFolder
    id (PK), profileId (FK indexed), name, sortOrder

@Entity Message
    id (PK), conversationId (FK indexed), profileId (FK indexed — derived), role (enum: system/user/assistant/tool), parentMessageId nullable, status (enum: pending/streaming/completed/failed/cancelled), textContent, jsonToolCalls nullable (typed column → table join), modelId nullable, createdAt, updatedAt

@Entity MessageToolCall
    id (PK), messageId (FK indexed), toolCallId, toolName, argumentsJson (TEXT), resultJson nullable, status similar to Message.status
```

Repository APIs require `profileId` as the first parameter and reject mismatched IDs (asserted at runtime in Debug, throws `IllegalStateException` in production deploys):

```kotlin
class ChatRepository(private val db: AppDatabase) {
    suspend fun listConversations(profileId: String): List<Conversation>
    suspend fun createConversation(profileId: String, modelId: String): String // returns conversationId
    suspend fun appendMessage(profileId: String, conversationId: String, message: NewMessage)
    fun observeMessages(profileId: String, conversationId: String): Flow<List<Message>>
    suspend fun deleteConversation(profileId: String, conversationId: String) // transactional; cascades
}
```

Migrations:

- `v1` initial schema.
- `MigrationTest` (Room Migration Test Helper) confirms v1 with a deterministic seed.

Profile isolation test:

- Create two profiles A & B in tests, each with one conversation. Attempt to read B's conversation while supplying `profileId=A`. Assert an empty result and a logged warning (in tests) — never data leakage.

### 4.5 UI integration (minimal)

A new `ChatScreen` Composable replaces the placeholder-routed branch when `selected.id == "chat"`. Minimum viable scope:

- Conversation list (RecyclerView of conversations in current profile).
- Conversation pane: rolling message list with `AssistantMessage` bubbles and a streaming indicator while `isCollecting`.
- Composer text input + send button.
- Native model picker dropdown populated from `ModelCapabilitiesRepository.fetchLiveCapabilities()`. Disabled while streaming.
- `StateFlow`-driven state; no transient state in `Bundle`.

The model picker is the only place capabilities surface in milestone 1. It must visually distinguish models by capability class (text / vision / tool-call) drawn from `ModelCapabilities`.

### 4.6 Source precedence reaffirmation

Every desktop ported behavior must be backed by an inspect of the current desktop `origin/main` mirror at `$VENICE_FORGE_DESKTOP_SOURCE` after bootstrap. The bootstrap script must succeed before milestone 1 starts (see § 3). For each new SDK method, record:

- Desktop files inspected (e.g. `src/services/veniceClient/chat.ts`, `electron/services/chatStreamParser.ts`, `src/services/modelService.ts`).
- Desktop HEAD SHA.
- The desktop test that constrains the behavior (e.g. `chat-stream.*.test.ts`).

The first commit in milestone 1 must include these notes in the commit body.

### 4.7 Trust-boundary reaffirmation

Reaffirm, no deviation from handoff:

- `:venice-sdk` continues to receive `apiKey` per call and stores nothing.
- All chat response bodies short of the SSE wire stream are redacted by `Redactor` before logging.
- Authorization headers are never logged.
- Chat screen does not store prompt/user text outside Room and does not echo it to logs.
- Profile `apiKeyAlias` resolves to a Keystore-protected ciphertext; never plaintext.

## 5. Data flow

End-to-end chat send:

```
ChatScreen "Send"
  → ChatViewModel.submit(text)
      → ChatRepository.createConversation(...) // if new
      → ChatClient.streamChat(apiKey, ChatRequest(...))
      → Flow<ChatStreamChunk> collected → ChatStreamAccumulator
      → AssistantMessage (StateFlow update) → ChatScreen auto-renders via collectAsState
      → ChatRepository.appendMessage(profileId, conversationId, AssistantMessage)
```

Cancel flow: composition leaves `Scope` → `Job.cancel()` → OkHttp `Call.cancel()` → next chunk `Flow` collection yields `CancellationException` → ViewModel marks `AssistantMessage.status = CANCELLED` and persists.

Refresh of capabilities: explicit "Refresh models" button on ChatScreen top bar; on click, the picker re-fetches and updates. Capability cache lives only in memory (no SQLite cache in v1 — later milestone can persist).

## 6. Error handling

- Bad API key / 401: `ChatStreamChunk.Error` with normalized `code = 401`. UI surfaces an inline non-blocking notice with a "Set API key" CTA.
- 429: `ChatStreamChunk.Error` with rate-limit message; UI surfaces retry-after hint where server provided.
- SSE network drop: detected by missing heartbeat or stream end without `[DONE]`. ViewModel marks message CANCELLED with reason "stream-interrupted", persists, and emits a recoverable error to UI ("Reply lost in transmission — your message was not charged").
- Idempotency: chat requests are not paid/queued jobs in this milestone — they are real-time. Idempotency logic for queued jobs is reserved for Phase 5.

## 7. Testing strategy

Each layer ships with tests appropriate to its nature.

**SDK (`venice-sdk` module tests, JVM):**
- `CapabilitiesRepositoryTest` — fixtures for `/models`, `/models/traits`, `/models/compatibility_mapping`. Asserts typed `ModelCatalog` is correct, and that unknown fields land in `rawJson`. Cross-reference fixtures against the OpenAPI snapshot (manual check noted in test source comments).
- `ChatClientTest` — fixture SSE responses (multi-chunk, with `[DONE]`, with tool-call fragments, with error frames). Asserts `Flow` yields the exact chunk sequence. Asserts cancellation cleans up the OkHttp call.
- `ChatStreamAccumulatorTest` — given chunks, asserts reconstructed AssistantMessage content and tool-call args.

**Data (`core:data` module tests, Robolectric / Android instrumented):**
- `ProfileIsolationTest` — proves A profile's repositories cannot read B's conversations.
- `ChatRepositoryTest` — CRUD + transactional delete + cascade.
- `MigrationTest` — Room Migration Test Helper for v1.

**UI (`app` module tests):**
- `ChatViewModelTest` — synthetic stream drives a fake ChatClient. Asserts state transitions Pending → Streaming → Completed (and Cancelled / Failed).
- `ModelCapabilitiesTest` (or `ModelPickerTest`) — verify the picker visibly groups models by capability class.

**Manual sanity (instrumentation):**
- End-to-end against a live `/models` and a chat request to a real model. Verifies SSE actually flows. Run with a real Venice API key configured outside source control.

Test commands affected:

```bash
./gradlew :venice-sdk:test
./gradlew :core:data:test
./gradlew :app:test
./gradlew lint
./gradlew :app:assembleDebug
```

For milestone 1 we accept the existing four-gate CI; instrumented tests on device are deferred.

## 8. Dependencies (additions to `libs.versions.toml`)

```toml
[versions]
room = "2.7.0"   # AGP 9.3 compatible; verify before release
sqliteKtx = "2.5.0"

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
androidx-sqlite = { module = "androidx.sqlite:sqlite", version.ref = "sqliteKtx" }

[plugins]
android-library (already present)
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }   # if Room uses KSP instead of KAPT
```

KSP vs KAPT for Room: the handoff mandates AGP 9.3 with built-in Kotlin and discourages legacy KAPT. Room 2.7.0 supports KSP — use KSP, add `com.google.devtools.ksp` Gradle plugin. (If a future Room release requires KAPT, document the deviation in commit message and update this spec.)

## 9. Acceptance criteria / completion definition

Milestone 1 is done when **all** of the following hold:

1. `./gradlew :venice-sdk:test :core:data:test :app:test` passes with green.
2. `./gradlew lint` passes with no new warnings.
3. `./gradlew :app:assembleDebug` produces `app-debug.apk`.
4. `./gradlew :venice-sdk:assembleRelease` produces `venice-sdk-release.aar`.
5. Manual demo: open `Chat` route, see model picker populated from `/models`, start a chat, observe incremental streaming text deltas, complete the message, restart the app, reopen the chat, observe the conversation persisted.
6. Profile isolation test passes (Test artifacts present in CI).
7. `SOURCE_BASELINE.md` updated with milestone-1 desktop HEAD SHA, Room version, KSP version.
8. `docs/FEATURE_PARITY_MATRIX.md` updated: `chat` moves from `Scaffolded` to `Foundation` (streaming + persistence present, but not yet full parity).
9. `docs/ELECTRON_TO_ANDROID_MAP.md` rows for chat client and chat storage reflect mapped boundaries.
10. Commit history records one source-map entry per ported behavior (commit message footer or `docs/SOURCE_MAPS/2026-08-15-milestone-1.md`).

## 10. Risks and mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| Bootstrap script failure due to workspace path mismatch (§ 3) | High | Resolve before any source-dependent work, per § 3. |
| Venice `/chat/completions` SSE wire shape drift from OpenAI conventions | Medium | Test fixtures come from recorded desktop responses; pin to desktop HEAD SHA, refresh on each upstream change. |
| Room schema v1 churn (inevitable later) | Certain | Encoded migration v1 leaves room for v2 without destructive fallback. |
| SSE read-timeout mis-tuning | Medium | Separate `VENICE_SSE_READ_TIMEOUT_SECONDS` with sensible default (90s). Surfaced as a normal user-visible state, not crash. |
| Capability cache vs. models-cache divergence | Low | v1 has no SQLite cache, only in-memory. Synchronization deferred. |
| AGP 9.3 / KSP / Room combination regressions | Medium | Lock versions in `libs.versions.toml`. Add a smoke `:app:assembleDebug` to every PR gate. |
| Hardcoded model allowlists creeping in (forbidden by handoff) | Medium | Add a `NoAllowlistGuardTest` in `:app:test` that greps the codebase for known-bad model IDs. |

## 11. Decision log (post-brainstorm)

- **Workspace layout (§3)** — **Confirmed 2026-08-15: Option B** (align conventions with reality). Applied in this spec revision.
- **KSP vs KAPT for Room** — Recommendation: KSP. Aligns with AGP 9.3 + built-in Kotlin.
- **Capability caching** — In-memory only for milestone 1; persist later.
- **Profile picker UI** — Out of scope; default profile only.
- **Provider fallback adapters** — Out of scope; deferred per handoff Phase 1 last bullet.
- **Tool execution / approvals** — Out of scope; later milestone.

## 12. Open questions for the user at review time

1. **Workspace path decision (§ 3)** — pick A, B, or C.
2. **Model picker UX** — top-bar dropdown vs. dedicated screen? (Default: dropdown, this can be revised during implementation.)
3. **Capabilities cache duration** — single fetch per session vs. 24-hour cache? (Default: per session.)
4. **Chat composer UX** — single-line text field vs. multi-line? (Default: multi-line with send button; mirrors desktop.)
5. **Multiple conversations** — single active at a time vs. tabbed? (Default: conversation list visible, single active; matches desktop.)

These can be deferred to implementation review without blocking milestone 1 start.

---

## Self-review checklist (filled inline)

- Placeholder scan: no TBD/TODO present.
- Internal consistency: § 4 module table aligns with § 4.1 file list; § 5 data flow matches § 4.3/4.5; § 9 acceptance matches § 1 goals.
- Scope check: focused on milestone 1 components and explicitly excludes later phases.
- Ambiguity check: `ModelCapabilities` typing rule stated; profile-scope API contract stated; cancellation contract stated; SSE read-timeout mentioned.
