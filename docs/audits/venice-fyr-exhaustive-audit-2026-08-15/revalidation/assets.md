# Asset Verification Evidence Base

**Scope:** Read-only groundwork for `22-ASSET-CONSOLIDATION.md`.
**Repo state:** `main @ ee2cd7a`, worktree clean except coordinator compile fix (`venice-sdk/.../VeniceForgeSdk.kt`).
**Method:** `sha256sum`, `git ls-files`, `find`, and `rg` over `md/kt/xml/kts` files (`.source/` and `.git/` excluded). No Gradle executed; no files moved or deleted.

---

## 1. Candidate Duplicate Verification

### 1.1 `docs/assets/venice-brand-guidelines.pdf` vs `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf`

| Path | Size | SHA-256 |
|---|---|---|
| `docs/assets/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf269218e0bae0da1ee9d67447846f4f72c4ea91e837860f744dd454edb` |
| `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf269218e0bae0da1ee9d67447846f4f72c4ea91e837860f744dd454edb` |

**Disposition:** `ASSET-DUP-01` | P3 | **VALID** | P3 | Exact byte-for-byte duplicate.

**Evidence:**
- `docs/BRANDING.md:14`: "Official Brand Guidelines Document: `docs/assets/venice-brand-guidelines.pdf`"
- `docs/assets/venice-brand-kit/DESIGN.md:195`: `└── venice-brand-guidelines.pdf`
- `docs/assets/Venice-Fyr-Brand-Asset-Pack/SOURCE_ATTRIBUTION.md:8`: "Official brand guidelines PDF: `docs/assets/venice-brand-guidelines.pdf`"

**Role / canonical source:** The `docs/assets/venice-brand-kit/` directory is the vendored official Venice brand kit (contains `DESIGN.md`, executive photos, logos, and the PDF). The root copy at `docs/assets/venice-brand-guidelines.pdf` is a convenience copy referenced by active docs.

**Recommended action:** Keep one canonical copy under `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` and redirect active references; or keep the root copy as the canonical public link and delete the kit-internal copy. Either way, one copy must remain.

**Migration required:** Yes — update `docs/BRANDING.md` and `SOURCE_ATTRIBUTION.md` references if the canonical location changes.

**Legal notes:** PDF is official Venice brand guidelines; retain at least one copy.

---

### 1.2 `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` vs `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp`

| Path | Size | SHA-256 |
|---|---|---|
| `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | 2,039,996 | `0050a95a070c7ebacee8312e49ecd2e14d122c0de8e8e4960a19e257c2f71316` |
| `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp` | 2,039,996 | `0050a95a070c7ebacee8312e49ecd2e14d122c0de8e8e4960a19e257c2f71316` |

**Disposition:** `ASSET-DUP-02` | P3 | **VALID** | P3 | Exact duplicate; runtime copy is required in `drawable-nodpi`.

**Evidence:**
- `docs/BRANDING.md:113`: "File: `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp`"
- `docs/BRANDING.md:114`: "Source Package: `docs/assets/ayanami-rei.codex-pet/`"
- `docs/assets/ayanami-rei.codex-pet/pet.json`: `{ "id": "ayanami-rei", "spritesheetPath": "spritesheet.webp", ... }`

**Role / canonical source:** `docs/assets/ayanami-rei.codex-pet/` is the source-of-truth package; `core/designsystem/.../ayanami_rei_spritesheet.webp` is the Android resource copy consumed at runtime.

**Recommended action:** Keep both. If the source package is removed, the runtime copy becomes the only source and loses provenance/metadata (`pet.json`). Consider documenting the duplication explicitly in `docs/BRANDING.md`.

**Migration required:** No, unless the source package is relocated.

**Legal notes:** `docs/BRANDING.md:135-136`: "The supplied character spritesheet depicts `Ayanami Rei`. Redistribution rights must be verified by the repository maintainer prior to publishing binary packages to public app stores." `pet.json` contains no explicit license/attribution field.

---

### 1.3 `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` vs `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md`

| Path | Size | SHA-256 |
|---|---|---|
| `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` | 10,513 | `8fcca05aa34dc042930c3594982c41094072defa3d72f426dc17b75be6c3643d` |
| `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md` | 10,513 | `8fcca05aa34dc042930c3594982c41094072defa3d72f426dc17b75be6c3643d` |

**Disposition:** `ASSET-DUP-03` | P3 | **VALID** | P3 | Exact duplicate; both are package handoff metadata.

**Evidence:**
- `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md:97`: "`AGENT_HANDOFF_REVIEW_FIRST.md` | **REJECT** | N/A | Package handoff instruction metadata; not part of public documentation."
- `docs/Venice-Fyr-GitHub-Docs-Pack/PACKAGE_MANIFEST.md:17`: "This package is **candidate material**. Use `AGENT_HANDOFF_REVIEW_FIRST.md` to integrate it."

**Role / canonical source:** Both copies are handoff metadata for the GitHub docs pack. The copy under `docs/Venice-Fyr-GitHub-Docs-Pack/` is the authoritative package copy; the `docs/assets/` copy is a stray duplicate.

**Recommended action:** Delete `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` and retain the copy inside `docs/Venice-Fyr-GitHub-Docs-Pack/` only while the pack exists; delete both when the pack is removed.

**Migration required:** No active production references; safe to delete after pack consolidation decision.

**Legal notes:** None; internal handoff metadata.

---

### 1.4 `docs/assets/venice-fyr-mark.png` vs `docs/assets/web/favicon-512.png` vs `docs/assets/store/google-play/icon-512.png`

| Path | Size | SHA-256 |
|---|---|---|
| `docs/assets/venice-fyr-mark.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` |
| `docs/assets/web/favicon-512.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` |
| `docs/assets/store/google-play/icon-512.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` |

**Disposition:** `ASSET-DUP-04` | P3 | **VALID** | P3 | Three byte-identical copies of the 512×512 product mark.

**Evidence:**
- `docs/BRANDING.md:100-102`: lists `venice-fyr-mark.png`, `store/google-play/icon-512.png`, and `web/favicon-*.png` as distinct brand assets.

**Role / canonical source:** `docs/assets/venice-fyr-mark.png` is the canonical square product mark. The web favicon and store icon are derived deployment copies of the same 512×512 raster.

**Recommended action:** Keep the canonical PNG and generate/store deployment copies only if the packaging pipeline cannot derive them. If retained, document that they are identical copies to avoid future divergence.

**Migration required:** No, unless store/web publishing pipelines require distinct paths.

**Legal notes:** Product-specific mark; not an official Venice corporate mark per `docs/BRANDING.md` §2 and `SOURCE_ATTRIBUTION.md:19-20`.

---

### 1.5 `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-mark-repo-baseline.svg` vs `docs/assets/venice-fyr-mark.svg`

| Path | Size | SHA-256 |
|---|---|---|
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-mark-repo-baseline.svg` | 2,031 | `57079ef7506aa3559b9f7f23c153e76f32e77bd4c1fac6c64e21ba87ccadb44e` |
| `docs/assets/venice-fyr-mark.svg` | 2,031 | `57079ef7506aa3559b9f7f23c153e76f32e77bd4c1fac6c64e21ba87ccadb44e` |

**Disposition:** `ASSET-DUP-05` | P3 | **VALID** | P3 | Exact duplicate; the brand-asset-pack copy is the retained baseline.

**Evidence:**
- `docs/assets/Venice-Fyr-Brand-Asset-Pack/MANIFEST.json:31-32`: `"sources": "Vector sources for generated outputs and retained repo baseline."`

**Role / canonical source:** `docs/assets/venice-fyr-mark.svg` is the active repo asset; the pack copy under `sources/product/` is the retained baseline.

**Recommended action:** If the brand asset pack is kept, keep the baseline copy as provenance. If the pack is removed, ensure `docs/assets/venice-fyr-mark.svg` remains.

**Migration required:** No.

**Legal notes:** Same as ASSET-DUP-04.

---

## 2. Path Reference Scan for Proposed Deletions / Migrations

Searched `md/kt/xml/kts` files for references to the four candidate removal targets. Results below exclude `.source/` and `.git/` and include line numbers.

### 2.1 `docs/Venice-Fyr-GitHub-Docs-Pack/`

| Referencing file | Lines | Quote / context |
|---|---|---|
| `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md:13` | 1 | "The supplied **Venice Fyr GitHub Documentation Pack** (`docs/Venice-Fyr-GitHub-Docs-Pack/`) has been systematically reviewed..." |
| `.superpowers/sdd/task-6-report.md:132` | 1 | "`addProjectToGitignore: docs/Venice-Fyr-GitHub-Docs-Pack/` — unrelated to Task 6; present in this branch as untracked content but NOT staged." |
| `.superpowers/sdd/progress.md:46` | 1 | "User-supplied unrelated reference material: `docs/Venice-Fyr-GitHub-Docs-Pack/` (untracked, never staged by any Task implementer)." |
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/02-FILE-AUDIT-LEDGER.md:10,124-155` | many | Ledger entries listing every file in the pack. |
| `docs/audits/.../findings/docs.md:236,240` | 2 | DOC-07 evidence citing `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md:87`. |
| `docs/audits/.../findings/docs.md:306,311` | 2 | DOC-09 evidence citing `docs/Venice-Fyr-GitHub-Docs-Pack/CHANGELOG.md:11–16`. |
| `docs/audits/.../11-P3-FINDINGS.md:761-776,855-865` | many | Same DOC-07 / DOC-09 evidence. |
| `docs/audits/.../17-DOCUMENTATION-DRIFT.md:14,53-80,94-96` | many | Drift audit noting stale claims and listing all pack files. |

**Observation:** No active production source file (`.kt`, `.xml`, `.kts`) references this path. References are confined to review/audit/history files and the `.superpowers/` skill workspace.

### 2.2 `docs/assets/Venice-Fyr-Brand-Asset-Pack/`

| Referencing file | Lines | Quote / context |
|---|---|---|
| `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/02-FILE-AUDIT-LEDGER.md:157-235` | many | Ledger entries listing every file in the pack. |
| `docs/audits/.../17-DOCUMENTATION-DRIFT.md:55-57` | 3 | Lists `README.md`, `SOURCE_ATTRIBUTION.md`, `AGENT_HANDOFF_GEMINI_3_7_FLASH.md`. |

**Observation:** No active production source file references this path. The pack is self-contained and only referenced by audit ledgers.

### 2.3 Root PDF copy `docs/assets/venice-brand-guidelines.pdf`

| Referencing file | Lines | Quote / context |
|---|---|---|
| `docs/BRANDING.md:14` | 1 | "Official Brand Guidelines Document: `docs/assets/venice-brand-guidelines.pdf`" |
| `docs/Venice-Fyr-GitHub-Docs-Pack/docs/BRANDING.md:14` | 1 | Same text (pack mirror). |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/SOURCE_ATTRIBUTION.md:8` | 1 | "Official brand guidelines PDF: `docs/assets/venice-brand-guidelines.pdf`" |
| `docs/audits/.../02-FILE-AUDIT-LEDGER.md:240,274` | 2 | Ledger entries for both copies. |

### 2.4 Standalone handoff `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md`

| Referencing file | Lines | Quote / context |
|---|---|---|
| `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md:23,97,155` | 3 | Rejected as package metadata. |
| `docs/Venice-Fyr-GitHub-Docs-Pack/PACKAGE_MANIFEST.md:17` | 1 | "Use `AGENT_HANDOFF_REVIEW_FIRST.md` to integrate it." |
| `.superpowers/sdd/progress.md:87` | 1 | Historical note about asset bundle including `AGENT_HANDOFF_REVIEW_FIRST.md`. |
| `docs/audits/.../02-FILE-AUDIT-LEDGER.md:130,156` | 2 | Ledger entries for both copies. |
| `docs/audits/.../17-DOCUMENTATION-DRIFT.md:53,59` | 2 | Drift audit listing both copies. |

---

## 3. Unique Source Vector Verification

### 3.1 `venice-fyr-beacon-v2.svg`

| Path | SHA-256 | Match? |
|---|---|---|
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-v2.svg` | `ed3879becc077f738dffd7f56cddcfeda88f3636c57e96cd5a3076dfc6cd22d0` | — |
| All files under `docs/assets/` root | (various) | **No match** |
| All files under `app/src/main/res/` | (various) | **No match** |

**Disposition:** `ASSET-UNIQ-01` | P3 | **VALID** | P3 | Unique source vector; no duplicate found in active asset roots or Android resources.

### 3.2 `venice-fyr-beacon-monochrome.svg`

| Path | SHA-256 | Match? |
|---|---|---|
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-monochrome.svg` | `91fc04a40712655365484d726fdae64bee52073f6389278568c9dd01cf1361f5` | — |
| All files under `docs/assets/` root | (various) | **No match** |
| All files under `app/src/main/res/` | (various) | **No match** |

**Disposition:** `ASSET-UNIQ-02` | P3 | **VALID** | P3 | Unique source vector; no duplicate found.

**Note:** The active launcher icon XML files in `app/src/main/res/drawable/` (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`) are hand-coded vector drawables, not copies of the beacon SVGs. Their SHA-256 values differ from the source SVGs.

---

## 4. GitHub Docs Pack Integration Status

Reviewed `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md`.

**Integration recorded as:** Complete at review time (`02ae314`), 29 files adopted/merged (28 adopted, 1 `README.md` merged), 3 meta files rejected.

**Adopted files and existence check:**

| Adopted destination | Exists? | Notes |
|---|---|---|
| `README.md` | ✅ | Merged with existing repo foundation. |
| `LICENSE` | ✅ | Apache-2.0 text. |
| `NOTICE` | ✅ | Copyright / attribution boundary. |
| `LEGAL.md` | ✅ | Apache-2.0 adoption, trademark, disclaimers. |
| `PRIVACY.md` | ✅ | Active privacy policy. |
| `SECURITY.md` | ✅ | Vulnerability disclosure policy. |
| `CONTRIBUTING.md` | ✅ | Contribution workflow. |
| `CODE_OF_CONDUCT.md` | ✅ | Conduct guidelines. |
| `SUPPORT.md` | ✅ | Support guidance. |
| `CHANGELOG.md` | ✅ | Unreleased changelog. |
| `.github/CODEOWNERS` | ✅ | `@spearchucker667` ownership. |
| `.github/PULL_REQUEST_TEMPLATE.md` | ✅ | PR checklist. |
| `.github/ISSUE_TEMPLATE/bug_report.yml` | ✅ | Bug report form. |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | ✅ | Feature request form. |
| `.github/ISSUE_TEMPLATE/documentation.yml` | ✅ | Documentation issue form. |
| `.github/ISSUE_TEMPLATE/config.yml` | ✅ | Issue template config. |
| `docs/GETTING_STARTED.md` | ✅ | Environment setup. |
| `docs/USER_GUIDE.md` | ✅ | User guide. |
| `docs/DEVELOPMENT_GUIDE.md` | ✅ | Developer workflow. |
| `docs/SDK_GUIDE.md` | ✅ | `:venice-sdk` guide. |
| `docs/TROUBLESHOOTING.md` | ✅ | Troubleshooting. |
| `docs/RELEASE_CHECKLIST.md` | ✅ | Release checklist. |
| `docs/BRANDING.md` | ✅ | Brand policy. |
| `docs/GITHUB_DOCS_INDEX.md` | ✅ | Docs index. |
| `docs/assets/venice-fyr-banner.png` | ✅ | README banner. |
| `docs/assets/venice-fyr-banner.svg` | ✅ | Vector banner. |
| `docs/assets/venice-fyr-social-preview.png` | ✅ | Social preview card. |
| `docs/assets/venice-fyr-mark.png` | ✅ | Square product mark. |
| `docs/assets/venice-fyr-mark.svg` | ✅ | Vector square mark. |

**Rejected meta files (still present in pack):**
- `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md`
- `docs/Venice-Fyr-GitHub-Docs-Pack/PACKAGE_MANIFEST.md`
- `docs/Venice-Fyr-GitHub-Docs-Pack/package-manifest.json`

**Disposition:** `DOC-08` | P3 | **VALID** | P3 | Review report is pinned to stale HEAD `02ae314` and the candidate pack still occupies disk; integration is functionally complete but cleanup is pending.

---

## 5. Junk / macOS Metadata Scan

### 5.1 Tracked junk

`git ls-files | grep -E '(\.DS_Store|\.\_)'` returned **zero** results.

### 5.2 Untracked junk

| Path | Type | Tracked? |
|---|---|---|
| `./.DS_Store` | macOS folder metadata | No |
| `./docs/.DS_Store` | macOS folder metadata | No |
| `./docs/audits/.DS_Store` | macOS folder metadata | No |
| `./docs/audits/venice-fyr-exhaustive-audit-2026-08-15/.DS_Store` | macOS folder metadata | No |

**Disposition:** `ASSET-JUNK-01` | P3 | **VALID** | P3 | Four untracked `.DS_Store` files exist; `.gitignore` already ignores `.DS_Store` so they are not at risk of being committed, but they clutter the working tree.

**Recommended action:** Delete the four `.DS_Store` files. No migration needed. Consider adding `**/.DS_Store` enforcement in CI if not already present.

**Legal notes:** None.

### 5.3 `._*` resource-fork files

`find . -name '._*'` (excluding `.git/` and `.source/`) returned **zero** results.

---

## 6. Codex Pet Licensing & Attribution

### 6.1 Licensing note in `docs/BRANDING.md`

`docs/BRANDING.md:135-136`:

> The supplied character spritesheet depicts `Ayanami Rei`. Redistribution rights must be verified by the repository maintainer prior to publishing binary packages to public app stores. The runtime animation abstraction is fully decoupled to allow swapping alternative sprite assets seamlessly.

### 6.2 `pet.json` / attribution situation

`docs/assets/ayanami-rei.codex-pet/pet.json`:

```json
{
  "id": "ayanami-rei",
  "displayName": "Ayanami Rei",
  "description": "A compact chibi desk pet inspired by a pale blue-haired anime girl holding a colorful good-luck fan.",
  "spritesheetPath": "spritesheet.webp",
  "spriteVersionNumber": 2,
  "kind": "person"
}
```

**Disposition:** `ASSET-LIC-01` | P2 | **VALID** | P2 | Licensing/attribution is incomplete: the spritesheet is present and in use, but `pet.json` lacks a license field, author/artist attribution, and redistribution terms.

**Evidence:**
- `docs/BRANDING.md:135-136` explicitly warns about redistribution rights.
- `pet.json` contains no `license`, `author`, `source`, or `attribution` keys.
- No separate `LICENSE` or `ATTRIBUTION` file exists in `docs/assets/ayanami-rei.codex-pet/`.

**Recommended action:** Before any public store release, either (a) obtain and record redistribution rights in a dedicated `LICENSE`/`ATTRIBUTION` file and add `license`/`attribution` fields to `pet.json`, or (b) replace the spritesheet with a fully licensed alternative. Until then, keep the `docs/BRANDING.md` warning.

**Migration required:** Yes — add license/attribution metadata if the asset is retained.

**Legal notes:** Character likeness (Ayanami Rei) may involve third-party intellectual property rights; this is a legal blocker for public distribution, not merely a hygiene issue.

---

## 7. Consolidation Decision Ledger

| Path | Size | SHA-256 | References | Role | Canonical source | Recommended action | Reason | Migration required | Legal notes |
|---|---|---|---|---|---|---|---|---|---|
| `docs/assets/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf2…` | `docs/BRANDING.md:14`; `SOURCE_ATTRIBUTION.md:8` | Public brand-guidelines link | `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` (same bytes) | **Delete this root copy OR delete the kit copy** and update refs | Exact duplicate; two copies risk divergence | Yes — update `BRANDING.md` / `SOURCE_ATTRIBUTION.md` if canonical path changes | Official Venice guidelines; retain one copy |
| `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf2…` | `docs/assets/venice-brand-kit/DESIGN.md:195` | Official brand-kit member | Same as above | **Keep as canonical** if root copy deleted | Vendored brand-kit location | Yes — update refs | Same as above |
| `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | 2,039,996 | `0050a95a…` | `docs/BRANDING.md:113-114`; `pet.json` | Source-of-truth Codex Pet spritesheet | This file | **Keep** | Provenance + metadata package | No, unless relocated | Ayanami Rei likeness — redistribution rights unverified |
| `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp` | 2,039,996 | `0050a95a…` | `docs/BRANDING.md:113`; runtime `CodexPet.kt` | Runtime Android resource | `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | **Keep** | Required by Android build; exact duplicate of source | No | Same as above |
| `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` | 10,513 | `8fcca05a…` | `docs/reviews/...:97`; audit ledgers | Stray duplicate handoff | `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md` | **Delete** | Rejected meta file per review report | No | Internal metadata |
| `docs/Venice-Fyr-GitHub-Docs-Pack/AGENT_HANDOFF_REVIEW_FIRST.md` | 10,513 | `8fcca05a…` | `PACKAGE_MANIFEST.md:17`; review report | Pack handoff metadata | Same as above | **Delete when pack removed** | Rejected meta file | No | Internal metadata |
| `docs/assets/venice-fyr-mark.png` | 29,746 | `9cc690de…` | `docs/BRANDING.md:100` | Canonical 512×512 product mark | This file | **Keep as canonical** | Active repo brand asset | No | Product mark, not official Venice mark |
| `docs/assets/web/favicon-512.png` | 29,746 | `9cc690de…` | `docs/BRANDING.md:104` | Web favicon deployment copy | `docs/assets/venice-fyr-mark.png` | **Keep or derive at build time** | Identical to canonical mark | No, if path retained | Same as above |
| `docs/assets/store/google-play/icon-512.png` | 29,746 | `9cc690de…` | `docs/BRANDING.md:102` | Store icon deployment copy | `docs/assets/venice-fyr-mark.png` | **Keep or derive at build time** | Identical to canonical mark | No, if path retained | Same as above |
| `docs/assets/venice-fyr-mark.svg` | 2,031 | `57079ef7…` | `docs/BRANDING.md:101` | Canonical vector product mark | This file | **Keep as canonical** | Active repo brand asset | No | Same as above |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-mark-repo-baseline.svg` | 2,031 | `57079ef7…` | `MANIFEST.json:31-32` | Retained baseline copy | `docs/assets/venice-fyr-mark.svg` | **Keep if pack kept; migrate to canonical if pack removed** | Exact duplicate baseline | No | Same as above |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-v2.svg` | 1,533 | `ed3879be…` | `MANIFEST.json`, `SOURCE_ATTRIBUTION.md` | Unique source vector for beacon mark | This file | **Keep** | No duplicate in active roots/res | No | Product mark |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/venice-fyr-beacon-monochrome.svg` | 474 | `91fc04a4…` | `MANIFEST.json`, `SOURCE_ATTRIBUTION.md` | Unique source vector for monochrome beacon | This file | **Keep** | No duplicate in active roots/res | No | Product mark |
| `docs/Venice-Fyr-GitHub-Docs-Pack/` | dir | n/a | Audit/review files only | Candidate docs pack (stale) | Root adopted files (now active) | **Delete entire directory** after confirming no active source refs | Integration complete; contents are historical mirror | No — active copies already live at root/docs | N/A |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/` | dir | n/a | Audit ledgers only | Candidate brand asset pack | Active assets already adopted | **Evaluate retention** — keep only unique source vectors (`beacon-v2`, `beacon-monochrome`, `mark-repo-baseline`) or delete entire pack if outputs are redundant | No active source references; most outputs duplicate active assets | Yes — if unique vectors are moved to canonical locations | See SOURCE_ATTRIBUTION.md; official Venice marks in `sources/official/` must retain geometry/ink |
| `.DS_Store` (4 files) | 6,148–8,196 | n/a | None | macOS metadata | N/A | **Delete** | Untracked junk; `.gitignore` already excludes | No | None |
| `docs/assets/ayanami-rei.codex-pet/pet.json` | 268 | n/a | `docs/BRANDING.md:114` | Codex Pet manifest | This file | **Add license/attribution fields** or replace asset | Missing redistribution terms | Yes — metadata update required | Ayanami Rei likeness — legal blocker for public distribution until rights verified |

---

## 8. Disposition Summary

| ID | Original severity/status | Disposition | Corrected severity/status | Why |
|---|---|---|---|---|
| `ASSET-DUP-01` | P3 / duplicate | **VALID** | P3 | SHA-256 match confirms exact duplicate of brand-guidelines PDF. |
| `ASSET-DUP-02` | P3 / duplicate | **VALID** | P3 | Exact duplicate, but runtime copy is required; not a safe deletion. |
| `ASSET-DUP-03` | P3 / duplicate | **VALID** | P3 | Exact duplicate of rejected handoff metadata; stray copy can be deleted. |
| `ASSET-DUP-04` | P3 / duplicate | **VALID** | P3 | Three identical 512×512 raster copies exist. |
| `ASSET-DUP-05` | P3 / duplicate | **VALID** | P3 | SVG baseline is exact duplicate of active repo mark. |
| `ASSET-UNIQ-01` | P3 / uniqueness claim | **VALID** | P3 | `venice-fyr-beacon-v2.svg` has no duplicate in `docs/assets/` root or `app/src/main/res/`. |
| `ASSET-UNIQ-02` | P3 / uniqueness claim | **VALID** | P3 | `venice-fyr-beacon-monochrome.svg` has no duplicate in active roots/res. |
| `DOC-08` | P3 / stale review | **VALID** | P3 | Review report is pinned to stale HEAD; pack integration is complete but pack directory remains. |
| `ASSET-JUNK-01` | P3 / hygiene | **VALID** | P3 | Four untracked `.DS_Store` files present; `.gitignore` already covers them. |
| `ASSET-LIC-01` | P2 / legal risk | **VALID** | P2 | Codex Pet spritesheet lacks license/attribution metadata; `docs/BRANDING.md` correctly flags redistribution risk. |

---

## 9. Tests Required

None for this read-only asset verification. Follow-up consolidation work should include:

1. **Build verification** after any deletion/relocation of assets referenced by `docs/BRANDING.md` or Android resources.
2. **Visual regression check** if launcher/store/web icon paths change.
3. **Legal review** of Codex Pet redistribution rights before public store release.
