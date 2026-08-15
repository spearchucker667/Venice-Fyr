# 17 — Documentation Drift Audit

**Auditor:** Documentation Drift auditor (read-only static review)  
**Scope:** All repository documentation under the root (`README.md`, `AGENTS.md`, `ANDROID_PORT_HANDOFF.md`, `SOURCE_BASELINE.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `SECURITY.md`, `PRIVACY.md`, `LEGAL.md`, `SUPPORT.md`, `CODE_OF_CONDUCT.md`) and all files under `docs/`.  
**Codebase reviewed:** `app/`, `venice-sdk/`, `core/` Kotlin/Gradle sources at `main @ 1da3142`.

## Executive summary

The documentation set is largely consistent with the current Android modules, toolchain, and security boundaries, but several high-priority drifts exist:

- The SDK usage example in `docs/SDK_EXAMPLES.md` would not compile against the current `ChatStreamChunk` API.
- The parity status of `chat`, `image`, `audio`, and `video` is reported differently in `docs/FEATURE_PARITY_MATRIX.md` / `CHANGELOG.md` versus `FeatureCatalog.kt`.
- Multiple API source/roadmap documents (`README.md`, `docs/VENICE_API_PORT_MATRIX.md`, `docs/reference/VENICE_API_SOURCE_MANIFEST.md`) are stale relative to the implemented SDK surface.
- The candidate GitHub docs pack under `docs/Venice-Fyr-GitHub-Docs-Pack/` contains stale claims and a review report pinned to an older HEAD.

**Severity counts:** P1: 2, P2: 4, P3: 3 (9 total findings).

## Ledger: files reviewed

| Path | Lines | Reviewed | Findings |
|---|---|---|---|
| `README.md` | 289 | Y | 1 |
| `AGENTS.md` | 138 | Y | 0 |
| `ANDROID_PORT_HANDOFF.md` | 233 | Y | 0 |
| `SOURCE_BASELINE.md` | 57 | Y | 0 |
| `CHANGELOG.md` | 36 | Y | 1 |
| `CONTRIBUTING.md` | 168 | Y | 0 |
| `SECURITY.md` | 81 | Y | 0 |
| `PRIVACY.md` | 100 | Y | 0 |
| `LEGAL.md` | 88 | Y | 0 |
| `SUPPORT.md` | 59 | Y | 0 |
| `CODE_OF_CONDUCT.md` | 33 | Y | 0 |
| `docs/API_INTEGRATION_GUIDE.md` | 86 | Y | 0 |
| `docs/BRANDING.md` | 136 | Y | 0 |
| `docs/DESKTOP_SOURCE_BOOTSTRAP.md` | 286 | Y | 0 |
| `docs/DEVELOPMENT_GUIDE.md` | 140 | Y | 0 |
| `docs/ELECTRON_TO_ANDROID_MAP.md` | 35 | Y | 0 |
| `docs/FEATURE_PARITY_MATRIX.md` | 43 | Y | 1 |
| `docs/GETTING_STARTED.md` | 130 | Y | 0 |
| `docs/GITHUB_DOCS_INDEX.md` | 54 | Y | 0 |
| `docs/PROVIDER_PARITY.md` | 34 | Y | 0 |
| `docs/SDK_EXAMPLES.md` | 109 | Y | 1 |
| `docs/SDK_GUIDE.md` | 79 | Y | 0 |
| `docs/SECURITY_AND_STORAGE_CONTRACT.md` | 15 | Y | 0 |
| `docs/TROUBLESHOOTING.md` | 148 | Y | 0 |
| `docs/USER_GUIDE.md` | 59 | Y | 0 |
| `docs/VENICE_API_PORT_MATRIX.md` | 59 | Y | 1 |
| `docs/VENICE_API_SOURCE_BOOTSTRAP.md` | 88 | Y | 0 |
| `docs/reference/VENICE_API_SOURCE_MANIFEST.md` | 57 | Y | 2 |
| `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` | 158 | Y | 1 |
| `docs/superpowers/plans/2026-08-15-android-port-milestone-1.md` | 2730 | Y | 0 |
| `docs/superpowers/specs/2026-08-15-android-port-milestone-1-design.md` | 356 | Y | 0 |
| `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` | 367 | Y | 0 |
| `docs/assets/venice-brand-kit/DESIGN.md` | 196 | Y | 0 |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/README.md` | 38 | Y | 0 |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/SOURCE_ATTRIBUTION.md` | 23 | Y | 0 |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/AGENT_HANDOFF_GEMINI_3_7_FLASH.md` | 288 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/README.md` | 184 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md` | 367 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/PACKAGE_MANIFEST.md` | 74 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md` | 32 | Y | 1 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/CODE_OF_CONDUCT.md` | 33 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/CONTRIBUTING.md` | 168 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/LEGAL.md` | 88 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/PRIVACY.md` | 100 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/SECURITY.md` | 81 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/SUPPORT.md` | 59 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md` | 133 | Y | 1 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/DEVELOPMENT_GUIDE.md` | 140 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/GETTING_STARTED.md` | 130 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/GITHUB_DOCS_INDEX.md` | 54 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/RELEASE_CHECKLIST.md` | 58 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/SDK_GUIDE.md` | 79 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/TROUBLESHOOTING.md` | 148 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/USER_GUIDE.md` | 59 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/bug_report.yml` | 85 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/config.yml` | 8 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/documentation.yml` | 27 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/.github/ISSUE_TEMPLATE/feature_request.yml` | 50 | Y | 0 |
| `docs/Venice-Fyr-GitHub-Docs-Pack/.github/PULL_REQUEST_TEMPLATE.md` | 57 | Y | 0 |

**Total files reviewed:** 58 markdown/yaml documents.

## Findings summary

| ID | Severity | Title |
|---|---|---|
| DOC-01 | P1 | `docs/SDK_EXAMPLES.md` streaming chat example uses non-existent `Delta.text` / `ToolCallDelta.arguments` fields |
| DOC-02 | P1 | Parity status for chat/image/audio/video is inconsistent between matrix/changelog and `FeatureCatalog.kt` |
| DOC-03 | P2 | `README.md` cites stale Venice OpenAPI schema version `20260814.153445` |
| DOC-04 | P2 | `docs/VENICE_API_PORT_MATRIX.md` still marks `/models/traits` and `/models/compatibility_mapping` as Planned |
| DOC-05 | P2 | `docs/reference/VENICE_API_SOURCE_MANIFEST.md` lists Image/Audio/Video SDK services as Planned |
| DOC-06 | P2 | `docs/reference/VENICE_API_SOURCE_MANIFEST.md` references missing test files |
| DOC-07 | P3 | Candidate `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md` describes non-existent About/wordmark/badge surfaces |
| DOC-08 | P3 | `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` is pinned to stale HEAD `02ae314` |
| DOC-09 | P3 | Candidate `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md` omits image/audio/video SDK clients |

See `findings/docs.md` for the full structured evidence for each finding.
