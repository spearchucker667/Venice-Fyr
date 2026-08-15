# Venice API Source Bootstrap Contract

## Purpose

The Android Venice Fyr client and `:venice-sdk` require authoritative, real-time Venice API specifications. Rather than guessing endpoint parameters, model capabilities, or error shapes, the repository uses a **read-only local source mirror** of the official [`veniceai/api-docs`](https://github.com/veniceai/api-docs) repository.

## Canonical Locations

```text
Workspace root:        /Users/super_user/Projects/Venice Fyr/
Android write target:  /Users/super_user/Projects/Venice Fyr/
Official remote:       https://github.com/veniceai/api-docs.git
Official branch:       main
Source mirror path:    /Users/super_user/Projects/Venice Fyr/.source/venice-api-docs/
Local discovery state: /Users/super_user/Projects/Venice Fyr/.local/venice-api-docs.env
```

Directory layout:

```text
/Users/super_user/Projects/Venice Fyr/   # WRITE: Android project root
├── app/, venice-sdk/, core/, docs/      # Android Kotlin modules & documentation
├── .source/
│   ├── venice-api-docs/                # READ ONLY: official Venice API documentation mirror
│   └── Venice_Forge-desktop/           # READ ONLY: desktop behavioral source mirror
└── .local/                             # Local environment files (gitignored)
```

## Mandatory Bootstrap Command

Before executing any Venice API implementation, capability updates, or test fixture modifications:

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-venice-api-docs.sh
set -a
source .local/venice-api-docs.env
set +a
```

The script is safe and idempotent:
- Clones `https://github.com/veniceai/api-docs.git` if absent.
- Verifies remote origin matches `https://github.com/veniceai/api-docs.git`.
- Fast-forwards using `git merge --ff-only` (never uses destructive `git reset --hard`).
- Refuses to update if local modifications exist in `.source/venice-api-docs/`.
- Extracts the active Swagger `info.version` from `swagger.yaml`.
- Writes `.local/venice-api-docs.env`.

## Non-Negotiable Source Precedence

When implementing or reviewing Venice API features, use this precedence hierarchy:

1. **`$VENICE_API_DOCS_SOURCE/swagger.yaml`**: Authoritative wire schema for endpoints, HTTP methods, parameters, request/response JSON schemas, enums, required fields, and authentication headers.
2. **`$VENICE_API_DOCS_SOURCE/` Guides and Reference Pages**: English documentation in `api-reference/`, `guides/`, `overview/`, `agents.md`, and `skill.md` for operational semantics (e.g. rate limits, queue lifecycles, streaming framing).
3. **Live Venice Runtime Discovery**: Dynamic capabilities from `/models`, `/models/traits`, and `/models/compatibility_mapping`.
4. **Existing Tested `:venice-sdk` Behavior**: Verified local Kotlin behavior, provided it does not contradict official upstream contracts.
5. **Venice Forge Desktop Source**: For desktop product UX and parity contracts (see [`DESKTOP_SOURCE_BOOTSTRAP.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/DESKTOP_SOURCE_BOOTSTRAP.md)).
6. **Venice Fyr Documentation**: Android platform decisions, security contracts, and architecture.

If code and the official API documentation conflict, treat it as a defect in code, not a documentation error.

## High-Value API Source Map

| Upstream Path | Description & Role |
|---|---|
| `$VENICE_API_DOCS_SOURCE/swagger.yaml` | Complete OpenAPI 3.0 specification for all REST endpoints |
| `$VENICE_API_DOCS_SOURCE/agents.md` | Agentic patterns, model traits (`text:default`, `image:fast`), discovery contracts |
| `$VENICE_API_DOCS_SOURCE/skill.md` | Venice capability matrix and skill integration guidelines |
| `$VENICE_API_DOCS_SOURCE/api-reference/` | Endpoint-specific reference guides, query params, headers, and schemas |
| `$VENICE_API_DOCS_SOURCE/guides/` | Comprehensive workflows (e.g. Web search, media queuing, rate limits) |
| `$VENICE_API_DOCS_SOURCE/overview/` | Rate limits, authentication, pricing tiers, and privacy policy |
| `$VENICE_API_DOCS_SOURCE/data/static-models.json` | Reference snapshot of model metadata (for test fixture construction only) |

## Drift Detection Workflow

When upstream `veniceai/api-docs` updates:

1. Source the environment variables:
   ```bash
   source .local/venice-api-docs.env
   ```
2. Inspect the diff between the previous baseline and current HEAD:
   ```bash
   git -C "$VENICE_API_DOCS_SOURCE" log -n 5 --oneline
   git -C "$VENICE_API_DOCS_SOURCE" diff <previous_head>..HEAD -- swagger.yaml
   ```
3. Update [`docs/reference/VENICE_API_SOURCE_MANIFEST.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/reference/VENICE_API_SOURCE_MANIFEST.md) and [`SOURCE_BASELINE.md`](file:///Users/super_user/Projects/Venice%20Fyr/SOURCE_BASELINE.md).
4. Update affected Kotlin data models, error handling, and test fixtures.
