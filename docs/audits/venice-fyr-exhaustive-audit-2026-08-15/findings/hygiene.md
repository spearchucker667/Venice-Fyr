# findings/hygiene.md

Repo hygiene and source-of-truth audit findings for Venice Fyr Android.

## Scope files reviewed

| Path | Lines | Reviewed | Findings |
|------|-------|----------|----------|
| `AGENTS.md` | 138 | Y | 0 |
| `SOURCE_BASELINE.md` | 57 | Y | 0 |
| `.gitignore` | 19 | Y | 0 |
| `scripts/bootstrap-venice-api-docs.sh` | 77 | Y | 0 |
| `.local/venice-api-docs.env` | 5 | Y | 0 |
| `.source/venice-api-docs/swagger.yaml` | 14595 | Y | 0 |
| `.source/venice-api-docs/agents.md` | 93 | Y | 0 |
| `.source/venice-api-docs/skill.md` | 10159 | Y | 0 |
| `.source/venice-api-docs/data/static-models.json` | 1 | Y | 0 |
| `.source/venice-api-docs/api-reference/error-codes.mdx` | 98 | Y | 0 |
| `.source/venice-api-docs/api-reference/rate-limiting.mdx` | 104 | Y | 0 |
| `.source/venice-api-docs/api-reference/api-spec.mdx` | 286 | Y | 0 |
| `.source/venice-api-docs/guides/media/video-generation.mdx` | 399 | Y | 0 |
| `.source/venice-api-docs/guides/media/music-and-sound-effects.mdx` | 208 | Y | 0 |
| `.source/venice-api-docs/guides/projects/rust-llm-gateway.mdx` | 1241 | Y | 0 |
| `.source/venice-api-docs/models/video.mdx` | 25 | Y | 0 |
| `.source/venice-api-docs/models/music.mdx` | 65 | Y | 0 |

## Findings

### HYGIENE-01 | Severity: P3 | Status: CONFIRMED | Area: Repo hygiene / test fixtures | Module: `:venice-sdk` | File: `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | Lines: 39, 64, 91, 122 | Symbol: hardcoded API key strings

**Evidence:**
- Line 39: `sdk.listModels(apiKey = "secret-key-12345", type = null)`
- Line 64: `sdk.listModels(apiKey = "secret-key-12345", type = ModelType.IMAGE)`
- Line 91: `val apiKey = "super-secret-api-key-xyz"`
- Line 122: `val apiKey = "secret-token-abcdef"`

**Expected:** Test fixtures should use obviously non-secret placeholders (e.g., `"test-api-key"`) or load values from test-only environment/configuration, and should not resemble real credential patterns.

**Actual:** Four test methods embed strings that match secret-like regexes (`secret-key-*`, `super-secret-api-key-*`, `secret-token-*`). These are not real credentials, but they trigger secret scanners and set a poor precedent.

**Impact:** Low — false positives in secret scanning; potential for copy-paste into production if not reviewed.

**Root cause:** Convenience test data without a standardized placeholder convention.

**Related occurrences:** None elsewhere in tracked files.

**Venice reference:** `AGENTS.md` Fixture Rule (`api-docs` fixtures must model authoritative schema); these are SDK unit tests, not API fixtures, but the same discipline applies.

**Android/Kotlin reference:** N/A.

**Remediation:** Replace with a single test constant such as `TEST_API_KEY = "venice-sdk-test-key"` that does not match secret regexes.

**Tests required:** None (test-only change).

**Compatibility impact:** None.

---

### HYGIENE-02 | Severity: P3 | Status: CONFIRMED | Area: Repo hygiene / .gitignore | Module: root | File: `.gitignore` | Lines: 1-19 | Symbol: `.gitignore` patterns

**Evidence:**
- `.gitignore` covers `.gradle/`, `.gradle-bootstrap/`, `.idea/`, `**/build/`, `.local/`, `.source/`, `*.apk`, `*.aab`, `*.aar`, `*.jks`, etc.
- Working tree contains `.kotlin/` and `.superpowers/` directories that are not ignored and not tracked.

**Expected:** Generated/tooling directories should be ignored to prevent accidental commits.

**Actual:** `.kotlin/` (Kotlin compiler daemon output) and `.superpowers/` (project skill/plugin workspace) are not listed in `.gitignore`.

**Impact:** Low — currently not tracked, but a future contributor could accidentally add them.

**Root cause:** `.gitignore` created before these directories were introduced.

**Related occurrences:** None tracked.

**Venice reference:** N/A.

**Android/Kotlin reference:** Kotlin/Gradle convention ignores `.kotlin/`.

**Remediation:** Add `.kotlin/` and `.superpowers/` to root `.gitignore`.

**Tests required:** None.

**Compatibility impact:** None.

---

## Build-output / ignored-file scan

No tracked files matched build-output or environment-sensitive patterns (`.gradle/`, `**/build/`, `*.apk`, `*.aab`, `*.aar`, `*.jks`, `*.keystore`, `local.properties`, `.idea/`, `*.iml`, `.DS_Store`, `.log`).


## Secret-like string scan

The following lines matched secret-like patterns. Context must be reviewed to distinguish placeholders/docs from real credentials.

| File | Line | Pattern | Evidence |
|------|------|---------|----------|
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 39 | api_key assignment | `sdk.listModels(apiKey = "secret-key-12345", type = null)` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 64 | api_key assignment | `sdk.listModels(apiKey = "secret-key-12345", type = ModelType.IMAGE)` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 91 | api_key assignment | `val apiKey = "super-secret-api-key-xyz"` |
| `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 122 | api_key assignment | `val apiKey = "secret-token-abcdef"` |

## Ignored-but-tracked files

No tracked files are also matched by `.gitignore` patterns.


## .gitignore adequacy

The root `.gitignore` (19 lines) covers: `.gradle/`, `.gradle-bootstrap/`, `.idea/`, `*.iml`, `local.properties`, `.DS_Store`, `/build/`, `**/build/`, `.externalNativeBuild/`, `.cxx/`, `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `*.apk`, `*.aab`, `*.aar`, `.local/`, `.source/`.

Notable omissions observed in working tree (not tracked): `.kotlin/` (Kotlin compiler daemon output) and `.superpowers/` (OMK managed-skill workspace) are not ignored. `.gradle-bootstrap/` is already covered. If these directories contain generated artifacts, they should be added to `.gitignore`. Currently they are not tracked, so no immediate hygiene defect.

