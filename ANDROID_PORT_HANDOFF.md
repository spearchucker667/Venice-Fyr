# Venice Forge Android Port — Agent Handoff

## Mission

Port Venice Forge `3.0.0-beta.2` from Electron/React/TypeScript to a fully native, downloadable Android application while also maintaining a reusable `venice-sdk` AAR. Achieve behavioral parity with the attached desktop source without using a WebView wrapper and without weakening existing privacy, capability, media-integrity, agent, or persistence boundaries.

## Mandatory workspace/source bootstrap

The agent starts from the Android package at:

```text
/Users/super_user/Projects/Venice Fyr/
```

The complete desktop source must be obtained from the canonical repository, not reconstructed from this starter:

```text
https://github.com/spearchucker667/Venice_Forge.git
```

Create/refresh a separate read-only mirror at:

```text
/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/
```

Run this before source-dependent work:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
```

The bootstrap records the exact checkout in `.local/desktop-source.env`. Read `docs/DESKTOP_SOURCE_BOOTSTRAP.md` for the complete source map, precedence rules, refresh policy, and feature-port workflow. The desktop checkout is read only for this project: do not modify, commit, or push it unless explicitly instructed.

## Authoritative inputs

1. Desktop source: the refreshed read-only `origin/main` checkout at `$VENICE_FORGE_DESKTOP_SOURCE` (normally `/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/`), especially `src/`, `electron/`, `docs/`, tests, `scripts/verify-*`, and `src/config/tabs.ts`.
2. Venice wire contract: desktop `docs/reference/Venice_swagger_api.yaml` and source precedence in `docs/reference/VENICE_API_SOURCE_MANIFEST.md`.
3. Runtime truth for active models/capabilities/pricing: `/models`, `/models/traits`, `/models/compatibility_mapping`.
4. Latest desktop behavior at the checked-out `origin/main` HEAD must win over older notes. Preserve the Aug. 14, 2026 Media Studio capability-routing hardening and re-check later desktop changes before claiming parity.

Do not invent request fields from memory. When endpoint documentation and runtime metadata disagree, follow the repository's documented source precedence and record the discrepancy.

## Product deliverables

- `app-debug.apk` for development/QA.
- signed release APK and AAB when signing configuration is supplied outside source control.
- `venice-sdk-release.aar` reusable SDK.
- release notes and checksum manifest.
- feature-parity report generated from the 22 canonical desktop tab IDs.
- Android tests and CI gates.

## Required Android architecture

- Kotlin, Jetpack Compose, single-activity application.
- `compileSdk=37`, `targetSdk=37`, `minSdk=26` unless a concrete dependency forces a justified change.
- AGP 9.3 / built-in Kotlin; do not re-add the obsolete `org.jetbrains.kotlin.android` plugin under AGP 9.x.
- App owns credential persistence; SDK never persists secrets.
- DataStore for preferences; Room for relational durable state once persistence work begins.
- OkHttp/coroutines for API calls, streaming, multipart uploads, and downloads.
- Media3 for audio/video playback.
- WorkManager for deferrable durable work. Use Android's user-initiated transfer/foreground mechanisms when a long user-started upload/download requires ongoing execution and progress notification.
- SAF/Photo Picker for external files. Persist URI grants when long-lived access is required.

## Module direction

Keep these hard boundaries even if feature code starts in the app module:

- `:venice-sdk` — public, reusable Venice HTTP/client facade; no persistent key store; no UI.
- `:core:security` — Keystore/profile credential handling.
- `:core:common` — redaction/errors/shared primitives.
- `:core:designsystem` — Compose tokens/components/themes.
- future `:core:data` — Room schema/repositories/migrations.
- future `:core:media` — content-addressed media store, validation, import/export.
- future `:core:jobs` — background generation/retrieval/download state.
- future `:core:agent` — tool registry, policy/approval engine, document/workspace tools.
- future `:core:providers` — fallback provider adapters and static/live model catalogs; keep provider transforms outside `:venice-sdk`.

Do not build one mega-ViewModel or one mega-repository.

## Phase 0 — lock the contracts

1. Copy the canonical 22 tab IDs and keep them stable in Android navigation/state.
2. Generate an endpoint/model capability contract inventory from the tracked Venice OpenAPI snapshot.
3. Translate desktop TypeScript domain models into Kotlin without dropping optional/unknown compatibility fields.
4. Build a parity test manifest mapping each desktop test/verify gate to an Android equivalent or a documented non-applicable reason.
5. Add `SOURCE_BASELINE.md` recording desktop commit/archive hash, Venice API source commit/schema version, Android dependency versions, and port date.

Acceptance: no Android feature implementation proceeds with an undocumented source mapping.

## Phase 1 — SDK/network parity

Implement typed clients in this order:

1. Models: `/models`, `/models/traits`, `/models/compatibility_mapping`.
2. Chat: `/chat/completions` plus streaming SSE and tool-call deltas; then `/responses` if the desktop app uses it.
3. Images: generation, styles, edit, multi-edit, upscale, background-remove.
4. Audio: speech, transcription, voices, quote/queue/retrieve/complete.
5. Video: quote/queue/retrieve/complete/transcriptions, including consent/error paths documented in Seedance guides.
6. Embeddings.
7. Characters.
8. Augment search/scrape/text parser.
9. Billing/rate-limit endpoints needed by Status.
10. Port the currently available fallback provider adapters behind a separate `ProviderAdapter` interface: Together (chat/image), Groq, Fireworks, Gemini Developer API, Mistral, Anthropic, and Perplexity (chat). Preserve Replicate, Bedrock, Vertex, Azure OpenAI, Hugging Face, and Cohere as deferred/unavailable until implemented; do not accept keys for them.

For each endpoint add request serialization tests, response fixture tests, non-2xx normalization, cancellation tests, timeout tests, retry rules, and redaction tests. Never log Authorization or bodies by default.

Acceptance: Android SDK fixtures are cross-checked against the tracked OpenAPI and representative desktop client tests.

## Phase 2 — durable local data/profile model

Port profiles first because almost every feature depends on isolation.

- Room entities: profiles, conversations, messages, folders, attachments, media, prompt records/versions, scenes, characters/cards, personas, lorebooks, research sessions/sources/findings, workflow definitions/runs, managed documents/revisions, background tasks, diagnostics metadata.
- Every profile-owned row includes a profile owner key and repository APIs require profile scope.
- Migrations are versioned and tested; no destructive fallback in production.
- Profile lockout/password verifier semantics must match desktop behavior where retained.
- Implement profile purge as a transactional, recoverable operation.

Acceptance: instrumentation tests prove one profile cannot query another profile's data or secret aliases.

## Phase 3 — Chat + agent runtime

Port Chat before secondary studios because it exercises model discovery, streaming, persistence, attachments, media tools, and agents.

- streaming must continue correctly across recomposition/navigation and survive activity recreation where feasible;
- message/tool-call deltas are state-machine driven, not string concatenation hacks;
- preserve immutable first-layer tool knowledge/system contract behavior;
- preserve prompt length limits and model capability gating from desktop source;
- document agent retains explicit capability constraints: no shell, Git, arbitrary network, keychain, database access, sibling traversal, or OS controls;
- workspace access is expressed only through explicit SAF grants;
- mutating/paid tool actions go through an approval coordinator.

Acceptance: multi-turn tool tests, cancellation, attachment lifecycle, character identity isolation, and generated-media rendering tests pass.

## Phase 4 — Image + Media Studio

Port Image Studio and Media Studio together.

- never hardcode an image-edit model list; derive edit support from live model capability metadata;
- preserve explicit provider `safe_mode` value, including `false`; do not omit it merely because local safe mode is off;
- local Family Safe Mode remains an independent optional guard layer;
- validate generated media MIME, file signature, dimensions/limits where applicable, hash, and persistence result before gallery insertion;
- store media bytes in app-private content-addressed files; Room stores metadata/lineage IDs, never base64 blobs;
- preserve retry-save recovery semantics with a bounded temporary cache;
- Media Studio action availability is based on the requested action/current target capabilities, not blindly inherited from the model that created the source asset;
- exports use `CreateDocument`/SAF and deterministic metadata sidecars.

Acceptance: generation/edit/multi-edit/upscale/background-remove fixtures, lineage, compare, export, recovery, and process-death scenarios pass.

## Phase 5 — Audio/Music/Video

Create a durable generation-job state machine:

`queued -> generating -> retrieving -> saving -> completed`, with terminal `failed` / `cancelled` states.

Persist every transition. WorkManager rehydrates unfinished work. Long user-initiated transfers display Android-required ongoing notification/progress. Media3 handles playback. Never keep large media payloads in SavedState/Bundle/Room blobs.

Acceptance: kill/restart app mid-job and verify recovery without duplicate paid submissions.

## Phase 6 — Research, Prompts, Scenes, Embeddings

- Research ports the currently supported Venice/Jina search/scrape/synthesis flow only. The inactive embedded Research Browser stays inactive.
- citations/sources are durable and separable from synthesized text.
- Prompt Library preserves scopes/tags/version chains.
- Scene Composer becomes a touch-first graph/canvas with stable IDs and durable geometry.
- Embeddings provides inspection without writing sensitive vectors/input into diagnostics.

## Phase 7 — Characters / ST Card / RP Studio

Port losslessly where the desktop app already preserves compatibility fields.

- Character Card V1/V2 JSON and V2 PNG codec.
- bounded PNG validation before decode/import.
- alternate greetings, embedded/linked lorebooks, personas, scenarios, prompt-traced test turn, versioning.
- preserve hosted versus local character distinctions.
- preserve character creator's currently pinned model behavior unless desktop source explicitly changes it.

Acceptance: round-trip fixture tests compare Android output to desktop fixtures with semantic equality and unknown-field preservation.

## Phase 8 — Documents / Workflows / Playground

Documents:
- CreateDocument/OpenDocument/OpenDocumentTree Activity Result contracts.
- persisted URI permissions for approved workspace directories.
- canonical URI/document ID policy to prevent grant escape.
- immutable revisions, exact diff preview, explicit apply confirmation.

Workflows/Playground:
- persisted graph model separate from Compose UI state.
- deterministic executor with typed node input/output contracts.
- paid/mutating nodes require confirmation and idempotency keys where supported.

## Phase 9 — Privacy, backup/sync, settings, themes, i18n, status

- reproduce `.vfbackup` format where cross-platform restore is a goal; otherwise version a new Android format and add explicit desktop/Android import compatibility tooling;
- SAF tree URI sync target plus conflict-safe outbox/checkpoints/retry queue;
- privacy dashboard enumerates local categories without leaking raw content;
- theme engine maps desktop theme tokens into Android semantic tokens and supports imported themes only after schema validation;
- port all supported locale catalogs and run missing-key/hardcoded-string gates;
- Status shows model connectivity, rate limits/billing where available, job state, storage health, and redacted logs.

## CI / release gates

At minimum:

```bash
./gradlew test
./gradlew lint
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease
./gradlew :venice-sdk:assembleRelease
```

Add emulator instrumentation tests for profile isolation, SAF grants, background job recovery, media import/export, and Compose critical flows. Add dependency and secret scanning. Signing material must come from CI secrets/local untracked config only.

## Do not

- Do not wrap the desktop React app in Android WebView and call that a port.
- Do not store API keys in source, BuildConfig, DataStore, Room, logs, crash text, or backups.
- Do not request broad filesystem access (`MANAGE_EXTERNAL_STORAGE`).
- Do not revive the archived embedded Research Browser.
- Do not use hardcoded model allowlists where the desktop source has moved to live capability metadata.
- Do not silently drop `safe_mode=false` or merge local Family Safe Mode with provider safe mode.
- Do not re-submit paid queue requests automatically after an ambiguous timeout without idempotency/reconciliation logic.
- Do not put large image/video/audio bytes into Room, SavedState, navigation arguments, or Compose state snapshots.
- Do not log prompt/response bodies by default.
- Do not weaken document/workspace grants compared with desktop agent policy.

## Immediate next implementation milestone

The starter already supplies secure API-key persistence, `/models` discovery, endpoint catalog, HTTPS-only networking, redaction, and the complete navigation registry. The next milestone is **typed model-capability parsing + chat SSE streaming + Room profile/chat schema**, because those three components unlock the highest number of downstream features.
