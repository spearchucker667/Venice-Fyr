# Desktop Source Bootstrap Contract

## Purpose

The Android port does not contain a vendored copy of the Electron/React source. Before porting behavior, the agent must create or refresh a **read-only local source mirror** of the canonical Venice Forge desktop repository and inspect that mirror directly.

## Canonical locations

Project workspace:

```text
/Users/super_user/Projects/Venice Fyr/
```

Android write target:

```text
/Users/super_user/Projects/Venice Fyr/
```

Desktop source remote:

```text
https://github.com/spearchucker667/Venice_Forge.git
```

Desktop source mirror:

```text
/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/
```

Expected layout:

```text
/Users/super_user/Projects/Venice Fyr/   # WRITE: workspace root IS the Android tree
├── app/, venice-sdk/, core/, docs/      # Android modules + docs
├── .source/
│   └── Venice_Forge-desktop/           # READ ONLY: cloned Electron source
└── .local/                             # local clone state (gitignored)
```

The workspace root is itself the Android write target. Never clone the desktop repository inside the Android tree and never make Android implementation commits in the desktop mirror.

## Mandatory bootstrap

From the Android project:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
```

Equivalent manual setup:

```bash
PROJECT_ROOT="/Users/super_user/Projects/Venice Fyr"
SOURCE_ROOT="$PROJECT_ROOT/.source/Venice_Forge-desktop"
REMOTE="https://github.com/spearchucker667/Venice_Forge.git"

mkdir -p "$PROJECT_ROOT/.source"
git clone --branch main --single-branch "$REMOTE" "$SOURCE_ROOT"

git -C "$SOURCE_ROOT" remote get-url origin
git -C "$SOURCE_ROOT" rev-parse --show-toplevel
git -C "$SOURCE_ROOT" rev-parse HEAD
git -C "$SOURCE_ROOT" status --short
```

The bootstrap script is idempotent. If the source mirror already exists and is clean, it fetches `origin/main` and fast-forwards/resets the mirror to the current remote `main`. If the mirror has local changes, the script stops instead of destroying them.

The script also writes this untracked local discovery file:

```text
/Users/super_user/Projects/Venice Fyr/.local/desktop-source.env
```

Agents may source that file to resolve the exact desktop checkout used during the session:

```bash
set -a
source "/Users/super_user/Projects/Venice Fyr/.local/desktop-source.env"
set +a
printf '%s\n' "$VENICE_FORGE_DESKTOP_SOURCE" "$VENICE_FORGE_DESKTOP_HEAD"
```

Do not commit `.local/` or a desktop source mirror.

## Source precedence

When implementing Android behavior, use this precedence:

1. **Current desktop `origin/main` checkout** at `$VENICE_FORGE_DESKTOP_SOURCE` for current product behavior, tests, constraints, and feature semantics.
2. **Android starter contracts** (`AGENTS.md`, `ANDROID_PORT_HANDOFF.md`, `docs/*`) for Android-specific architecture/security decisions and intentional platform adaptations.
3. **Desktop tracked Venice API source manifest and OpenAPI** for HTTP wire contracts:
   - `docs/reference/VENICE_API_SOURCE_MANIFEST.md`
   - `docs/reference/Venice_swagger_api.yaml`
   - other files referenced by the source manifest
4. **Live Venice runtime metadata** for active model availability/capabilities/pricing:
   - `/models`
   - `/models/traits`
   - `/models/compatibility_mapping`
5. Historical notes/README prose only when the above sources do not answer the question. Never let older prose override current code/tests or current API source precedence.

If sources conflict, document the conflict and follow the higher-precedence source. Do not silently invent a compromise.

## High-value desktop source map

Start each feature by locating its implementation, state, IPC/service boundary, tests, and verification scripts in the desktop mirror.

### Product surface and navigation

```text
src/config/tabs.ts
src/components/
src/stores/
```

`src/config/tabs.ts` is the canonical starting point for stable desktop feature/tab IDs.

### Venice HTTP/client behavior

```text
src/services/veniceClient/
electron/services/veniceClient.ts
electron/services/veniceClient.*.test.ts
electron/ipc/handlers/veniceHandlers.ts
src/services/media-request-adapter.ts
src/services/media-request-adapter.test.ts
```

Preserve serialization, SSE parsing, multipart behavior, retry/error normalization, capability handling, and explicit safety fields semantically in Kotlin rather than translating TypeScript line-by-line.

### Current Venice API documentation contract

```text
docs/reference/VENICE_API_SOURCE_MANIFEST.md
docs/reference/Venice_swagger_api.yaml
docs/reference/
```

Read `VENICE_API_SOURCE_MANIFEST.md` before relying on any individual reference file.

### Persistence and application state

```text
src/stores/
src/services/dbMigrations.ts
src/services/dbMigrations.test.ts
src/types/
```

Do not port Zustand/IndexedDB implementation mechanics directly. Port domain invariants, persistence semantics, profile isolation, migrations, and tests into Room/DataStore repositories.

### Electron privilege boundaries to reinterpret on Android

```text
electron/ipc/
electron/ipc/handlers/
electron/services/
src/services/desktopBridge.ts
```

IPC code is a behavioral/security specification, not code to copy. Map each privileged desktop operation to an Android-native boundary such as Keystore, SAF/Photo Picker, Room, WorkManager, Media3, notifications, or app-private files.

### Chat / streaming / attachments

```text
src/components/chat/
src/stores/chat-store.ts
src/stores/chat-stream-manager.ts
src/stores/chat-*.test.ts
```

### Image / media generation and inspection

```text
src/components/image/
src/components/media/
src/components/image-inspector/
src/services/media-request-adapter.ts
src/services/taskMediaCatalog.ts
src/stores/media-store.ts
src/stores/image-workspace-store.ts
src/stores/image-inspector-store.ts
```

Specifically retain the current capability-based image-edit model behavior, explicit provider `safe_mode` semantics, and Media Studio action-routing fixes present on current `main`.

### Characters / Character Cards / RP

```text
src/components/rp-studio/
src/services/characterService.ts
src/services/characterCards/
src/services/characterCardImportExport.ts
src/services/characterCreator*.ts
electron/services/characterCard*.ts
electron/ipc/characterCardFileHandlers.ts
electron/ipc/rpHandlers.ts
```

### Research

```text
src/components/research/
src/stores/research-store.ts
electron/ipc/handlers/jinaHandlers.ts
```

### Documents / document agent

```text
src/components/documents/
electron/ipc/handlers/documentAgentHandlers.ts
```

Search for document service, workspace, revision, attachment, and diff types/tests before implementing the Android equivalent.

### Workflows / background work / sync

```text
src/components/workflows/
src/stores/workflow-template-store.ts
src/stores/background-task-store.ts
electron/ipc/handlers/backgroundTaskHandlers.ts
electron/ipc/handlers/syncHandlers.ts
```

### Verification contracts

```text
package.json
scripts/verify-*.cjs
**/*.test.ts
**/*.test.tsx
```

Treat desktop tests and `verify:*` scripts as executable specifications. For every Android feature, record which desktop tests/contracts were ported, adapted, or marked non-applicable with a reason.

## Required feature-port workflow

For every feature or bugfix:

1. Refresh/verify the desktop source mirror.
2. Record the exact desktop HEAD:

   ```bash
   git -C "$VENICE_FORGE_DESKTOP_SOURCE" rev-parse HEAD
   ```

3. Search the desktop implementation, tests, related IPC/service boundary, and docs before writing Kotlin.
4. Write a short source map in the Android task notes or commit message, for example:

   ```text
   Desktop sources inspected:
   - src/components/image/image-tools.tsx
   - src/services/veniceClient/fetch.ts
   - electron/services/guardPipeline.ts
   - relevant tests
   Desktop HEAD: <sha>
   ```

5. Implement native Android behavior; do not copy renderer/Electron architecture.
6. Add Android tests reproducing the desktop behavioral contract.
7. Run the smallest relevant Gradle tests/builds.
8. Update `docs/FEATURE_PARITY_MATRIX.md` when parity status changes.

## Refresh rule

At the beginning of a new working session, and before claiming parity for a feature, refresh the mirror:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
```

If `origin/main` has advanced, inspect the changed desktop files before continuing the Android work. Do not blindly retain a previously ported contract when the source application has changed.

## Failure rules

- If GitHub cannot be reached, stop source-dependent implementation rather than guessing current behavior.
- If the desktop source mirror is dirty, stop and report it; do not reset user changes automatically.
- If `origin` is not the canonical URL, stop and report the mismatch.
- If a source path named in this document has moved, search the current repo and update this source map rather than assuming the feature was removed.
- Do not modify or push the desktop source repository as part of the Android port unless explicitly instructed.
