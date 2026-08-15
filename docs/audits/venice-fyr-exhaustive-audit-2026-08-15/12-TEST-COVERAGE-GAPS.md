# 12-TEST-COVERAGE-GAPS

**Scope:** Unit-test and fixture quality for `:app`, `:venice-sdk`, `:core:common`, `:core:data`.  
**Date:** 2026-08-15  
**Source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream HEAD `6e69346b`, info.version `20260814.194349`.

---

## Executive summary

The test suite covers the happy paths of chat streaming, image generation/edit, model discovery, and local Room persistence, but it has significant gaps in:

1. **Missing client modules** — no tests for `AudioClient` or `VideoClient`.
2. **Fixture fidelity** — `models.json` contains fields not defined in the authoritative `swagger.yaml` `ModelResponse` schema.
3. **Hardcoded model IDs** — multiple tests violate the project Model Rule by embedding current model IDs.
4. **Error and lifecycle paths** — HTTP non-200, malformed SSE, cancellation, ViewModel errors, and network failures are largely untested.
5. **Security boundaries** — API-key non-persistence and redaction are only lightly tested.

---

## 1. Missing test modules (zero coverage)

| Production surface | Production file | Status |
|--------------------|-----------------|--------|
| Audio TTS | `venice-sdk/.../audio/AudioClient.kt` | **No tests** |
| Video queue/retrieve/complete | `venice-sdk/.../video/VideoClient.kt` | **No tests** |

**Impact:** Two entire Venice API surfaces (`/audio/speech`, `/video/queue`, `/video/retrieve`, `/video/complete`) have no automated regression coverage. These are paid/mutating operations and high-risk areas.

**Required:**
- `AudioClientTest.kt`: verify request URL/method/headers, binary 200 response, 4xx/5xx error mapping, timeout/IO exception mapping.
- `VideoClientTest.kt`: verify queue response parsing, retrieve `Processing` vs `Completed` content-type switching, complete success, 404/409/422 errors.

---

## 2. Partial client coverage

### Image client
`ImageClientTest.kt` exercises only `generate()` and `edit()`. The following public methods are untested:
- `upscale()`
- `multiEdit()`
- `generateBinary()`
- HTTP error paths (400, 401, 429, 500, 503)
- Network exceptions (`SocketTimeoutException`, `IOException`)

**Required:** Expand `ImageClientTest.kt` to cover all public methods and error paths.

### Chat client
`ChatClientTest.kt` covers good SSE, tool-call chunks, SSE-side errors, and cancellation. Missing:
- HTTP non-200 response handling (`ChatClient.kt:51–56`)
- Invalid JSON inside SSE `data:` lines
- Multiple choices (`n > 1`)
- Missing `choices` array / error object paths
- Empty `delta` handling

**Required:** Add malformed-response and HTTP-error tests.

### SDK facade
`VeniceForgeSdkTest.kt` covers URL building and 429/401/400 errors. Missing:
- `SocketTimeoutException` / `IOException` → `VeniceSdkException.Network`
- HTTP 500/503 server errors
- Realistic `/models` response parsing
- `getRaw` helper
- Other `ModelType` values

**Required:** Add network/server-error tests and a `/models` parsing fixture test.

---

## 3. Fixture/schema mismatches

### `models.json` invents swagger-undefined fields
The fixture used by `CapabilitiesRepositoryTest` contains:
- `model_spec.name`, `model_spec.description`
- `model_spec.pricing`
- `model_spec.traits`
- `model_spec.uncensored`
- top-level `metadata`

None of these are present in `swagger.yaml` `ModelResponse` (`swagger.yaml:4659–4919`). The SDK parses some of them, so the tests validate undocumented behavior.

**Required:** Rebuild the fixture from `swagger.yaml`-defined fields only (or from a recorded `/models` response) and add a contract test that fails on fixture fields outside the swagger schema.

---

## 4. Hardcoded model IDs

The following tests embed current Venice model IDs as string literals, violating AGENTS.md Model Rule:
- `ChatViewModelTest.kt:90`
- `ChatRepositoryTest.kt:34`
- `CapabilitiesRepositoryTest.kt:60`
- `ImageClientTest.kt:57`
- `VeniceParametersSerializationTest.kt:24`
- `models.json` fixture

**Required:** Replace with synthetic IDs or trait-resolved IDs; add a CI check to prevent new hardcoded model IDs in tests.

---

## 5. ViewModel and UI layer gaps

`ChatViewModelTest.kt` tests only happy paths. Missing:
- `ChatStreamChunk.Error` handling
- `cancel()`
- Missing API key / missing model selection
- Tool-call delta handling
- `ViewModel` cleared during streaming
- Configuration change / process death

**Required:** Add failure, cancellation, and lifecycle tests.

---

## 6. Persistence layer gaps

- `ChatRepositoryTest.kt` does not test `deleteConversation`, `ConversationKind`, or transaction failure.
- `ProfileRepositoryTest.kt` does not test `findDefault` or non-default profiles.
- `MigrationTest.kt` only verifies v1 schema creation; no migration path tests.

**Required:** Expand repository and migration tests.

---

## 7. Security and privacy gaps

- No test verifies that `:venice-sdk` does not persist API keys.
- No test verifies redaction of keys in logs/diagnostics beyond a single exception-message check.
- `safe_mode=false` preservation is tested for chat but **not** for image, video, or audio requests, despite AGENTS.md requiring it.

**Required:** Add security-focused tests and `safe_mode=false` serialization tests for media requests.

---

## 8. Parser and accumulator edge cases

- `SseLineParserTest.kt` lacks malformed-line, `event:`, `id:`, and recursion-safety tests.
- `ChatStreamAccumulatorTest.kt` lacks interleaved text/tool-call deltas, multi-index tool calls, and finish-after-error tests.
- `VeniceParametersSerializationTest.kt` lacks `safe_mode=true`, full `ChatRequest` round-trip, and invalid enum tests.

**Required:** Add edge-case tests for these small but critical components.

---

## 9. Minor gaps

- `RedactorTest.kt`: only one happy-path case.
- `FeatureCatalogTest.kt`: only counts features; no semantic checks.
- `VeniceEndpointsTest.kt`: only verifies path constants; does not verify endpoint semantics.

**Required:** Add parameterized edge-case and semantic tests.

---

## 10. Recommended priority order

1. **P1 — Create `AudioClientTest` and `VideoClientTest`.**
2. **P1 — Fix `models.json` fixture and add fixture-schema contract test.**
3. **P1 — Remove hardcoded model IDs from tests and fixtures.**
4. **P1 — Add `ChatViewModel` error/cancellation/lifecycle tests.**
5. **P1 — Add `safe_mode=false` tests for image/video/audio requests.**
6. **P2 — Expand `ImageClientTest`, `ChatClientTest`, `VeniceForgeSdkTest` error paths.**
7. **P2 — Expand repository and migration tests.**
8. **P2 — Add SSE parser and stream accumulator edge-case tests.**
9. **P2 — Add security/persistence tests for API keys.**
10. **P3 — Expand `RedactorTest`, `FeatureCatalogTest`, `VeniceEndpointsTest`.**

---

## 11. Cross-reference to detailed findings

See `findings/tests.md` for the full ledger and each finding in the required format (ID, severity, status, evidence, expected/actual, impact, root cause, remediation, tests required).
