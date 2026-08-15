# Venice API Integration & Architecture Guide

## Overview

This guide establishes standard operating procedures for integrating and maintaining official Venice.ai API endpoints, data models, error handlers, and runtime discovery within the Venice Fyr Android repository.

---

## 1. How-To: Refresh the Official Venice API Docs

The repository maintains an untracked, read-only local mirror of [`veniceai/api-docs`](https://github.com/veniceai/api-docs) under `.source/venice-api-docs/`.

### Execution

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-venice-api-docs.sh
set -a
source .local/venice-api-docs.env
set +a

printf 'API docs source : %s\n' "$VENICE_API_DOCS_SOURCE"
printf 'API docs HEAD   : %s\n' "$VENICE_API_DOCS_HEAD"
printf 'Swagger version : %s\n' "$VENICE_API_SWAGGER_VERSION"
```

### Invariants
- `.source/` and `.local/` are gitignored and must never be committed.
- The mirror is treated as read-only.
- Updates use safe fast-forward merges (`git merge --ff-only`).

---

## 2. How-To: Add or Update a Venice Endpoint

Follow this 14-step workflow whenever modifying or adding Venice API endpoints:

1. **Bootstrap / Refresh Official Docs**: Run `./scripts/bootstrap-venice-api-docs.sh` and source `.local/venice-api-docs.env`.
2. **Inspect Upstream OpenAPI**: Locate the route in `$VENICE_API_DOCS_SOURCE/swagger.yaml`.
3. **Review Guides & Reference Pages**: Read the corresponding documentation in `$VENICE_API_DOCS_SOURCE/api-reference/` and `guides/`.
4. **Check Deprecations**: Verify whether the endpoint or any parameter is deprecated (e.g. `/billing/usage` vs `/billing/usage-history`).
5. **Verify Wire Contract**: Confirm HTTP verb, path, query parameters, auth headers, and content encoding (JSON vs multipart).
6. **Define Kotlin Data Models**: Create typed `@Serializable` request and response models in `:venice-sdk`.
7. **Preserve Unknown / Additive Fields**: Use `ignoreUnknownKeys = true` and preserve raw JSON where downstream consumers need forward compatibility.
8. **Route Through Centralized SDK Transport**: Use `VeniceForgeSdk` and shared `OkHttpClient` instance; never scatter raw HTTP requests.
9. **Implement Structured Error Handling**: Route non-2xx responses through `parseHttpError` to produce typed `VeniceSdkException` variants.
10. **Implement Streaming / Queuing Lifecycle**: If streaming (SSE), enforce single terminal events and cooperative cancellation. If queued (e.g. video/music), implement polling state machines.
11. **Update Source Manifest**: Record the status in [`docs/reference/VENICE_API_SOURCE_MANIFEST.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/reference/VENICE_API_SOURCE_MANIFEST.md).
12. **Update SDK Examples**: Document public API usage in [`docs/SDK_EXAMPLES.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/SDK_EXAMPLES.md).
13. **Add Tests & Fixtures**: Write unit tests using authoritative schemas (e.g. `MockWebServer` or wire mocks).
14. **Update Parity Matrix**: Record changed status in [`docs/FEATURE_PARITY_MATRIX.md`](file:///Users/super_user/Projects/Venice%20Fyr/docs/FEATURE_PARITY_MATRIX.md).

---

## 3. How-To: Update Model Capability Support

### Core Rules
- **Never hard-code a static production model allowlist or permanent default model ID.**
- Model IDs and capabilities are runtime data that change frequently.
- Inspect the current `/models` schema (`model_spec`) and `/models/traits` semantics.

### Workflow
1. Inspect the upstream `model_spec` properties in `$VENICE_API_DOCS_SOURCE/swagger.yaml`.
2. If new capability flags exist (e.g. `supportsReasoningEffort`, `supportsXSearch`), add them to `ModelCapabilitiesSpec` and `ModelCapabilities`.
3. If new symbolic traits are defined (e.g. `text:default`, `image:fast`), ensure `ModelCatalog.defaultTextModelId` and `modelForTrait()` resolve them.
4. Update unit test fixtures to match authoritative upstream JSON shapes.
5. Verify that models missing optional metadata degrade safely without crashing.

---

## 4. How-To: Diagnose and Handle API Drift

When Venice API documentation updates upstream:

```bash
source .local/venice-api-docs.env

# Inspect recent commits in the upstream documentation mirror
git -C "$VENICE_API_DOCS_SOURCE" log -n 5 --oneline

# Inspect schema diff against previous recorded baseline
git -C "$VENICE_API_DOCS_SOURCE" diff <previous-api-docs-sha>..HEAD -- swagger.yaml api-reference/ guides/
```

- If changes affect existing endpoints: implement fixes, update tests, and update `VENICE_API_SOURCE_MANIFEST.md`.
- If changes affect planned/future endpoints: update `VENICE_API_SOURCE_MANIFEST.md` and `FEATURE_PARITY_MATRIX.md` to reflect new requirements.
