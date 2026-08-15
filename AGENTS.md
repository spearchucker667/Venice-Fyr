# AGENTS.md

## Mission
Build the native Android Venice Forge client and reusable `:venice-sdk` AAR without weakening the desktop application's privacy, model-capability, agent, media-integrity, persistence, or approval contracts, while strictly conforming to the official Venice API wire specifications.

---

## Mandatory First Actions: Source-of-Truth Bootstraps

The Android tree is the **write target**. Upstream sources are **read-only mirrors** and are never vendored or committed directly.

Canonical paths:

```text
Workspace root:     /Users/super_user/Projects/Venice Fyr/
Android target:     /Users/super_user/Projects/Venice Fyr/
Venice API docs:    /Users/super_user/Projects/Venice Fyr/.source/venice-api-docs/
Venice API remote:  https://github.com/veniceai/api-docs.git (branch: main)
Desktop source:     /Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/
Desktop remote:     https://github.com/spearchucker667/Venice_Forge.git (branch: main)
```

### 1. Mandatory Venice API Docs Bootstrap
Before any work involving Venice endpoints, models, capabilities, authentication, streaming, media, queued jobs, billing, rate limits, errors, provider parameters, privacy metadata, or request/response serialization:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-venice-api-docs.sh
set -a
source .local/venice-api-docs.env
set +a
```

Read [`docs/VENICE_API_SOURCE_BOOTSTRAP.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/VENICE_API_SOURCE_BOOTSTRAP.md).

### 2. Mandatory Desktop Parity Bootstrap
Before implementing or porting product features, UI parity, domain stores, or security boundaries from desktop:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
set -a
source .local/desktop-source.env
set +a
```

Read [`docs/DESKTOP_SOURCE_BOOTSTRAP.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/DESKTOP_SOURCE_BOOTSTRAP.md).

---

## Non-Negotiable Source Precedence

When implementing behavior or resolving discrepancies, use this strict hierarchy:

1. **`$VENICE_API_DOCS_SOURCE/swagger.yaml`**: Authoritative wire schema for endpoints, methods, requests, responses, encodings, auth headers, enums, parameter constraints, and required fields.
2. **`$VENICE_API_DOCS_SOURCE/` Guides & Markdown**: English documentation in `api-reference/`, `guides/`, `overview/`, `agents.md`, and `skill.md` for operational semantics and rate limits.
3. **Live Venice Runtime Discovery**: Dynamic endpoint responses from `/models`, `/models/traits`, and `/models/compatibility_mapping`.
4. **Existing Tested `:venice-sdk` Behavior**: Verified local Kotlin behavior where it does not conflict with authoritative upstream API sources.
5. **Current Desktop `origin/main` Checkout (`$VENICE_FORGE_DESKTOP_SOURCE`)**: Behavioral/product parity for features, stores, and tests. Desktop code cannot override the official API specification for wire contracts.
6. **Venice Fyr Android Architecture Contracts**: Local contracts in `AGENTS.md`, `ANDROID_PORT_HANDOFF.md`, and `docs/*`.
7. **Clearly Labeled Inference**: Only when the above sources genuinely do not address the question.

If code and the official API documentation conflict, treat it as a code defect to investigate.

---

## Mandatory Files to Inspect for API Work

Always inspect the authoritative subset in `$VENICE_API_DOCS_SOURCE`:

```text
$VENICE_API_DOCS_SOURCE/swagger.yaml
$VENICE_API_DOCS_SOURCE/agents.md
$VENICE_API_DOCS_SOURCE/skill.md
$VENICE_API_DOCS_SOURCE/api-reference/
$VENICE_API_DOCS_SOURCE/guides/
$VENICE_API_DOCS_SOURCE/overview/
$VENICE_API_DOCS_SOURCE/data/static-models.json
```

Consult `$VENICE_API_DOCS_SOURCE/llms.txt` and `llms-full.txt` when broad discovery is needed.

---

## Operational Rules for Agents

### 1. Model Rule
**Never hard-code a current Venice model catalog or permanent default model ID.** Model IDs and capabilities are runtime data. Use current `/models` data and the documented trait/discovery mechanisms (`/models/traits`). Static model snapshots are fixtures/reference only, never a permanent allowlist or fallback in production code.

### 2. Fixture Rule
**API test fixtures must model an authoritative current Venice schema.** Do not invent convenient fixture fields. Each nontrivial fixture must state the upstream source/schema revision it represents or be mechanically/minimally derived from an authoritative payload shape.

### 3. Drift Detection Rule
Every API-related session must record:
- Upstream `veniceai/api-docs` HEAD and Swagger `info.version`
- Local Venice Fyr starting HEAD
- Relevant upstream source paths consulted
- If upstream HEAD has changed since the baseline, inspect the diff on relevant files before implementing.

### 4. Documentation Maintenance Rule (Definition-of-Done)
**API implementation and API documentation are one change.** If Venice-facing behavior changes, the agent must update the relevant local API source manifest, examples/how-to documentation, tests, and parity/status documentation in the same task:
- `AGENTS.md`
- `SOURCE_BASELINE.md`
- `docs/reference/VENICE_API_SOURCE_MANIFEST.md`
- `docs/FEATURE_PARITY_MATRIX.md`
- `docs/API_INTEGRATION_GUIDE.md`
- `docs/SDK_EXAMPLES.md`
- `README.md`
- `CHANGELOG.md`

---

## Non-Negotiable Security and Architecture Boundaries

- **No WebView wrapper.**
- **No plaintext persistent credentials.**
- **`:venice-sdk` never persists API keys.**
- **No telemetry by default.**
- **No broad storage permissions.** Use explicit SAF grants and Photo Picker.
- **No raw prompt/response/API-key logging.**
- **Local Family Safe Mode and Venice provider `safe_mode` are distinct.**
- **Preserve explicit `safe_mode=false` when selected.**
- **Model capabilities are runtime data.**
- **Paid/mutating operations require explicit approval and duplicate-submission defenses.**
- **Electron IPC/services are behavioral specifications;** map them to Android-native boundaries (Keystore, Room, WorkManager, Media3).

---

## Validation

Before marking any task complete:
1. Run the smallest relevant unit tests.
2. Run the affected module build.
3. Before a release candidate or milestone completion, run:
   ```bash
   ./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease
   ```
4. Update `docs/FEATURE_PARITY_MATRIX.md` when parity status changes.
