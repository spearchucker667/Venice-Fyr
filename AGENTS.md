# AGENTS.md

## Mission
Build the native Android Venice Forge client and reusable AAR without weakening the desktop application's privacy, model-capability, agent, media-integrity, persistence, or approval contracts.

## Mandatory first action: resolve the desktop source

The Android tree is the **write target**. The current Electron repository is a **read-only behavioral source** and is not vendored into this project.

Canonical paths:

```text
Workspace:      /Users/super_user/Projects/Venice Fyr/
Android target: /Users/super_user/Projects/Venice Fyr/
Desktop source: /Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/
Desktop remote: https://github.com/spearchucker667/Venice_Forge.git
Desktop branch: main
```

Before source-dependent implementation, run:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
```

Then load the discovered source location/HEAD when useful:

```bash
set -a
source .local/desktop-source.env
set +a
```

Read `docs/DESKTOP_SOURCE_BOOTSTRAP.md` before porting a feature. Never clone the Electron repo inside the Android tree, never edit it as part of normal port work, and never commit the `.source/` mirror or `.local/` state. If the source mirror is dirty or its origin is not the canonical repository, stop and report the mismatch instead of destroying local changes.

## Source authority

1. Current desktop `origin/main` checkout for product behavior, feature semantics, tests, and constraints.
2. This Android repository's `AGENTS.md`, `ANDROID_PORT_HANDOFF.md`, and `docs/*` for Android-native architecture/security adaptations.
3. Desktop `docs/reference/VENICE_API_SOURCE_MANIFEST.md` for Venice API documentation precedence.
4. Desktop `docs/reference/Venice_swagger_api.yaml` and the other reference files selected by that manifest for wire shape.
5. Live Venice `/models`, `/models/traits`, `/models/compatibility_mapping` for active model capabilities/pricing.

Do not implement from memory when the desktop repo contains the answer. For each feature, inspect the implementation, stores/domain types, Electron privilege boundary where applicable, tests, and relevant `verify:*` scripts before writing Kotlin. Record the desktop HEAD used for parity work.

## Non-negotiable boundaries
- No WebView wrapper.
- No plaintext persistent credentials.
- `:venice-sdk` never persists API keys.
- No telemetry by default.
- No broad storage permission.
- No raw prompt/response/API-key logging.
- Local Family Safe Mode and Venice provider `safe_mode` are distinct.
- Preserve explicit `safe_mode=false` when selected.
- Model capabilities are runtime data; do not replace them with hardcoded allowlists.
- Documents/workspaces use explicit SAF grants only; no shell/arbitrary filesystem access.
- Paid/mutating operations require explicit approval and duplicate-submission defenses.
- Electron IPC/services are behavioral/security specifications; map them to Android-native boundaries instead of copying architecture.

## Validation
Before marking a task complete, run the smallest relevant tests and then the affected module build. Before a release candidate, run `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease` plus instrumentation suites. Update `docs/FEATURE_PARITY_MATRIX.md` when parity status changes.
