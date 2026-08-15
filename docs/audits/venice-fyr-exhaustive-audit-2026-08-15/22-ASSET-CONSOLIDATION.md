# 22-ASSET-CONSOLIDATION

**Scope:** WP-R10 asset consolidation execution.
**Repo state:** `main @ ee2cd7a`; coordinator compile fixes already applied.
**Authority:** `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/revalidation/assets.md` (full verified ledger with hashes).
**Method:** Plain `mv`/`rm` operations; no Gradle executed; no files touched outside ownership list.

---

## 1. Consolidation Actions Taken

| Path | Size | SHA-256 | References | Action | Reason | Legal notes |
|---|---|---|---|---|---|---|
| `docs/assets/product-source/venice-fyr-beacon-v2.svg` | 1,533 | `ed3879becc077f738dffd7f56cddcfeda88f3636c57e96cd5a3076dfc6cd22d0` | `docs/BRANDING.md` §6.1 | **Migrated** from `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/` | Unique source vector; no duplicate in active asset roots or Android resources | Product mark |
| `docs/assets/product-source/venice-fyr-beacon-monochrome.svg` | 474 | `91fc04a40712655365484d726fdae64bee52073f6389278568c9dd01cf1361f5` | `docs/BRANDING.md` §6.1 | **Migrated** from `docs/assets/Venice-Fyr-Brand-Asset-Pack/sources/product/` | Unique source vector for monochrome beacon / themed icon layer | Product mark |
| `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf269218e0bae0da1ee9d67447846f4f72c4ea91e837860f744dd454edb` | `docs/BRANDING.md` §1; `docs/assets/venice-brand-kit/DESIGN.md` | **Kept** as canonical | Official Venice brand guidelines; vendored brand-kit location | Official Venice guidelines; retain one copy |
| `docs/assets/venice-brand-guidelines.pdf` | 14,389,843 | `87abccf269218e0bae0da1ee9d67447846f4f72c4ea91e837860f744dd454edb` | `docs/BRANDING.md` (updated) | **Deleted** | Exact byte-for-byte duplicate of kit copy (SHA-256 re-verified before deletion) | Official Venice guidelines; one copy retained under brand kit |
| `docs/reviews/archive/AGENT_HANDOFF_REVIEW_FIRST.md` | 10,513 | `8fcca05aa34dc042930c3594982c41094072defa3d72f426dc17b75be6c3643d` | `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` | **Moved** from `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md` | Provenance archive of rejected package handoff metadata | Internal metadata |
| `docs/Venice-Fyr-GitHub-Docs-Pack/` | dir | n/a | Audit/review files only (see §2) | **Deleted** entire directory | Integration recorded complete in `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md`; active copies already live at repo root and `docs/` | N/A |
| `docs/assets/Venice-Fyr-Brand-Asset-Pack/` | dir | n/a | Audit ledgers only (see §2) | **Deleted** entire directory | Outputs duplicate active assets; unique vectors migrated to `docs/assets/product-source/` | See deleted `SOURCE_ATTRIBUTION.md`; official Venice marks must retain geometry/ink |
| `.DS_Store` (4 files) | ~28,000 | n/a | None | **Deleted** | Untracked macOS folder metadata; `.gitignore` already excludes them | None |
| `docs/assets/venice-fyr-mark.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` | `docs/BRANDING.md` §6 | **Kept** | Canonical 512×512 product mark | Product mark, not official Venice corporate mark |
| `docs/assets/venice-fyr-mark.svg` | 2,031 | `57079ef7506aa3559b9f7f23c153e76f32e77bd4c1fac6c64e21ba87ccadb44e` | `docs/BRANDING.md` §6 | **Kept** | Canonical vector product mark | Product mark |
| `docs/assets/web/favicon-512.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` | `docs/BRANDING.md` §6 | **Kept** | Semantic web favicon deployment copy (identical to canonical mark) | Product mark |
| `docs/assets/store/google-play/icon-512.png` | 29,746 | `9cc690de3e6c886ca055b128264d548cfd4c790419c89ecbcde035802409883a` | `docs/BRANDING.md` §6 | **Kept** | Semantic store icon deployment copy (identical to canonical mark) | Product mark |
| `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` | 2,039,996 | `0050a95a070c7ebacee8312e49ecd2e14d122c0de8e8e4960a19e257c2f71316` | `docs/BRANDING.md` §7; `pet.json` | **Kept** | Source-of-truth Codex Pet spritesheet package | Ayanami Rei likeness — redistribution rights unverified |
| `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp` | 2,039,996 | `0050a95a070c7ebacee8312e49ecd2e14d122c0de8e8e4960a19e257c2f71316` | `docs/BRANDING.md` §7; runtime `CodexPet.kt` | **Kept** | Required Android runtime resource; exact duplicate of source package | Ayanami Rei likeness — redistribution rights unverified |

---

## 2. Reference Re-Scan Results

Before deletion, `rg` over `md/kt/xml/kts` files (`.source/` and `.git/` excluded) confirmed:

- **`docs/Venice-Fyr-GitHub-Docs-Pack/`**: No active production source references. Mentions confined to `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md`, audit/history files under `docs/audits/...`, and `.superpowers/` skill workspace.
- **`docs/assets/Venice-Fyr-Brand-Asset-Pack/`**: No active production source references. Mentions confined to audit ledgers.
- **`docs/assets/venice-brand-guidelines.pdf`**: Active reference in `docs/BRANDING.md` updated to canonical kit path. `SOURCE_ATTRIBUTION.md` reference removed with pack deletion.
- **`docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md`**: Active reference in `docs/reviews/GITHUB_DOCS_PACK_REVIEW_2026-08-15.md` preserved as historical review evidence; file moved to `docs/reviews/archive/`.

---

## 3. Size Reduction

- Tracked files deleted: **113 files**, **15,777,175 bytes** (~15.05 MiB).
- Untracked `.DS_Store` files deleted: **4 files**, ~28 KB.
- **Approximate total reduction: ~15.8 MB**.

No runtime Android resources (`app/src/main/res/**`) were removed.

---

## 4. Unresolved Blocker: Codex Pet Licensing

**Asset:** `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` and the runtime copy `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp`.

**Status:** Retained per task instructions (runtime asset stays; source package stays).

**Issue:** `pet.json` lacks `license`, `author`, `source`, and `attribution` fields. No separate `LICENSE` or `ATTRIBUTION` file exists in `docs/assets/ayanami-rei.codex-pet/`. The spritesheet depicts `Ayanami Rei`, a third-party character likeness.

**Required before public store distribution:** Obtain and record redistribution rights, add license/attribution metadata to `pet.json` and/or a dedicated `LICENSE`/`ATTRIBUTION` file, or replace the spritesheet with a fully licensed alternative. `docs/BRANDING.md` §7 already flags this risk.

---

## 5. Files Modified

- `docs/BRANDING.md`: Official brand guidelines reference now points to `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf`; added §6.1 Product Source Vectors for the migrated beacon SVGs.

## 6. Files Created

- `docs/assets/product-source/venice-fyr-beacon-v2.svg`
- `docs/assets/product-source/venice-fyr-beacon-monochrome.svg`
- `docs/reviews/archive/AGENT_HANDOFF_REVIEW_FIRST.md`
- This ledger: `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/22-ASSET-CONSOLIDATION.md`

## 7. Files Deleted

- `docs/assets/venice-brand-guidelines.pdf`
- `docs/assets/AGENT_HANDOFF_REVIEW_FIRST.md`
- `docs/Venice-Fyr-GitHub-Docs-Pack/` (entire directory)
- `docs/assets/Venice-Fyr-Brand-Asset-Pack/` (entire directory)
- `.DS_Store`, `docs/.DS_Store`, `docs/audits/.DS_Store`, `docs/audits/venice-fyr-exhaustive-audit-2026-08-15/.DS_Store`
