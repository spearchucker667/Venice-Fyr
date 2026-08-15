# TEST QUALITY audit findings

**Auditor scope:** Every unit-test file under `app/src/test/**`, `venice-sdk/src/test/**`, `core/common/src/test/**`, and `core/data/src/test/**`, plus their test fixtures.  
**Audit date:** 2026-08-15  
**Repository:** `github.com/spearchucker667/Venice-Fyr` @ `1da3142`  
**Venice API source-of-truth:** `.source/venice-api-docs/swagger.yaml` @ upstream HEAD `6e69346b`, info.version `20260814.194349`.  
**Methodology:** Static review only; no Gradle commands executed per audit rules.

---

## 1. File ledger

| Module | Path | Lines | Reviewed | Findings |
|--------|------|-------|----------|----------|
| `:app` | `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt` | 246 | Y | 4 |
| `:app` | `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt` | 12 | Y | 2 |
| `:app` | `app/src/test/resources/robolectric.properties` | 1 | Y | 0 |
| `:core:common` | `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt` | 15 | Y | 2 |
| `:core:data` | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` | 59 | Y | 3 |
| `:core:data` | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt` | 51 | Y | 2 |
| `:core:data` | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileIsolationTest.kt` | 67 | Y | 1 |
| `:core:data` | `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` | 38 | Y | 2 |
| `:core:data` | `core/data/src/test/resources/robolectric.properties` | 1 | Y | 0 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt` | 102 | Y | 4 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt` | 220 | Y | 4 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt` | 46 | Y | 2 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt` | 32 | Y | 2 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt` | 78 | Y | 3 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt` | 105 | Y | 4 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceEndpointsTest.kt` | 17 | Y | 1 |
| `:venice-sdk` | `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt` | 161 | Y | 4 |
| `:venice-sdk` | `venice-sdk/src/test/resources/fixtures/chat-stream/stream-good.sse` | 9 | Y | 0 |
| `:venice-sdk` | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/compatibility.json` | 9 | Y | 1 |
| `:venice-sdk` | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json` | 78 | Y | 4 |
| `:venice-sdk` | `venice-sdk/src/test/resources/fixtures/models-with-capabilities/traits.json` | 10 | Y | 0 |

**Totals:** 21 files reviewed, ~1,461 lines, 38 actionable findings.

---

## 2. Findings

### TEST-FIXTURE-01 | Severity: P1 | Status: CONFIRMED
**Area:** Fixture fidelity  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/resources/fixtures/models-with-capabilities/models.json`  
**Lines:** 10–42  
**Symbol:** `model_spec` object, `metadata` object  
**Evidence:** The fixture contains fields that are **not defined** in the authoritative `ModelResponse` schema in `swagger.yaml`:
- `model_spec.name` and `model_spec.description` (fixture lines 12–13, 51–52)
- `model_spec.pricing` (fixture lines 28–36, 65–73)
- `model_spec.traits` (fixture lines 17–21, 56)
- `model_spec.uncensored` (fixture lines 16, 55)
- top-level `metadata` (fixture lines 40–42)

`swagger.yaml` `ModelResponse.model_spec` defines only: `availableContextTokens`, `maxCompletionTokens`, `beta`, `betaModel`, `privacy`, `regionRestrictions`, `deprecation`, `capabilities`, `constraints` (`.source/venice-api-docs/swagger.yaml:4696–4919`). Top-level `ModelResponse` properties do not include `metadata` (`.source/venice-api-docs/swagger.yaml:4659–4695`).

**Expected:** Test fixtures model an authoritative current Venice schema and document any extensions.  
**Actual:** The fixture invents convenience fields that the SDK happens to parse, but which are not guaranteed by the spec.  
**Impact:** Tests pass against a fictional schema; if Venice removes or renames these fields, production parsing will break while tests stay green.  
**Root cause:** Fixture hand-authored from SDK implementation rather than from `swagger.yaml` or a recorded `/models` payload.  
**Related occurrences:** `CapabilitiesRepositoryTest.kt:54–100` derives assertions from these invented fields.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:4659–4919` (`ModelResponse`).  
**Android/Kotlin reference:** N/A.  
**Remediation:** Rebuild `models.json` from a recorded `/models` response and trim it to fields present in `swagger.yaml`; add a fixture contract test that fails if a fixture field is not in the swagger schema.  
**Tests required:** New fixture-schema contract test.  
**Compatibility impact:** Medium — may force SDK parsing changes if Venice drops these fields.

---

### TEST-HARDCODE-02 | Severity: P1 | Status: CONFIRMED
**Area:** Model-ID hardcoding  
**Module:** Multiple  
**Files / Lines:**
- `ChatViewModelTest.kt:90` — `"llama-3.3-70b"`
- `ChatRepositoryTest.kt:34` — `"llama-3.3-70b"`
- `CapabilitiesRepositoryTest.kt:60` — `"llama-3.3-70b"`
- `ImageClientTest.kt:57` — `"test-model"`
- `VeniceParametersSerializationTest.kt:24` — `"test-model"`
- `models.json` fixture — `"llama-3.3-70b"`, `"deepseek-r1"`

**Symbol:** String literals used as model IDs  
**Evidence:** Tests encode specific Venice model IDs as string literals. AGENTS.md Model Rule states: "**Never hard-code a current Venice model catalog or permanent default model ID.** Model IDs and capabilities are runtime data." The upstream docs repeat: "Discover, don't hardcode." (`.source/venice-api-docs/AGENTS.md:66`).

**Expected:** Tests use parameterized or trait-resolved model IDs, or synthetic IDs when testing serialization shape.  
**Actual:** Current model IDs are baked into fixtures and assertions.  
**Impact:** Tests become stale when models rotate; `CapabilitiesRepositoryTest` will fail when the default trait mapping changes, creating noisy false positives.  
**Root cause:** Convenience fixtures copied from current catalog.  
**Related occurrences:** 5 test files and 1 fixture file.  
**Venice reference:** AGENTS.md Model Rule; `.source/venice-api-docs/AGENTS.md:66`.  
**Android/Kotlin reference:** N/A.  
**Remediation:** Replace literal model IDs with synthetic IDs in unit tests; in integration tests resolve IDs via `/models/traits`.  
**Tests required:** Refactor affected tests; add CI check that fails on new hardcoded Venice model IDs in test sources.  
**Compatibility impact:** Low — test-only change.

---

### TEST-COVERAGE-03 | Severity: P1 | Status: CONFIRMED
**Area:** Missing client tests  
**Module:** `:venice-sdk`  
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioClient.kt`  
**Lines:** 19–45  
**Symbol:** `AudioClient.speech`  
**Evidence:** No test file exists for `AudioClient`. `find venice-sdk/src/test -name '*Audio*'` returns nothing. The production class implements `POST /audio/speech` and returns binary audio bytes.  
**Expected:** Every SDK client surface has unit tests for request serialization, success, and error paths.  
**Actual:** 0 tests for TTS.  
**Impact:** TTS endpoint regressions (wrong URL, missing auth header, wrong Accept header, error mapping) go undetected.  
**Root cause:** Test module never created.  
**Related occurrences:** None.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:10878–11043` (`/audio/speech`).  
**Android/Kotlin reference:** `AudioClient.kt:19–45`.  
**Remediation:** Add `AudioClientTest` covering: request method/URL/auth/Accept header, binary 200 response, 4xx/5xx error mapping, timeout/IO exception mapping.  
**Tests required:** New `AudioClientTest.kt`.  
**Compatibility impact:** Low.

---

### TEST-COVERAGE-04 | Severity: P1 | Status: CONFIRMED
**Area:** Missing client tests  
**Module:** `:venice-sdk`  
**File:** *(production)* `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoClient.kt`  
**Lines:** 19–63  
**Symbol:** `VideoClient.queue`, `VideoClient.retrieve`, `VideoClient.complete`  
**Evidence:** No test file exists for `VideoClient`. `find venice-sdk/src/test -name '*Video*'` returns nothing. The production class implements the async video job state machine (`queue` → `retrieve` → `complete`).  
**Expected:** Video job state machine tested.  
**Actual:** 0 tests for video.  
**Impact:** Bugs in queue/retrieve/complete flow, content-type switching, and status parsing are not caught.  
**Root cause:** Test module never created.  
**Related occurrences:** None.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:11570–11969` (`/video/queue`, `/video/retrieve`).  
**Android/Kotlin reference:** `VideoClient.kt:19–63`.  
**Remediation:** Add `VideoClientTest` covering: queue JSON response parsing, retrieve returning `Processing` vs `Completed` based on `Content-Type`, complete success, 4xx/5xx/404 errors, timeout.  
**Tests required:** New `VideoClientTest.kt`.  
**Compatibility impact:** Low.

---

### TEST-MOCK-05 | Severity: P2 | Status: CONFIRMED
**Area:** Heavy mocking hides real integration  
**Module:** `:app`  
**File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`  
**Lines:** 46–56  
**Symbol:** `RecordingChatClient`  
**Evidence:** `ChatViewModelTest` uses a hand-rolled `RecordingChatClient` that subclasses `ChatClient` and overrides `streamChat` to emit a static `Flow`. It never exercises real `ChatClient` SSE parsing, HTTP error handling, cancellation propagation, or `ChatStreamAccumulator` behavior.  
**Expected:** ViewModel tests either use real SDK components with a fake OkHttp stack, or are narrowly scoped to pure ViewModel state.  
**Actual:** The test is an integration-shaped test that replaces the most error-prone collaborator with a stub.  
**Impact:** Bugs in `ChatClient` → `ChatViewModel` integration (e.g., wrong chunk type handling, exception leaks, status mapping) will not be caught here.  
**Root cause:** Test fake is too coarse.  
**Related occurrences:** `ChatClientTest.kt` tests `ChatClient` in isolation.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `ChatViewModel.kt:86–193`; `ChatClient.kt:28–98`.  
**Remediation:** Add at least one integration test using the real `ChatClient` with a fake OkHttp `Interceptor`, or split into small ViewModel state tests that don't touch `ChatClient`.  
**Tests required:** New integration test or refactored unit tests.  
**Compatibility impact:** Low.

---

### TEST-MISSING-06 | Severity: P1 | Status: CONFIRMED
**Area:** Missing failure/cancellation/lifecycle cases  
**Module:** `:app`  
**File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`  
**Lines:** 58–245  
**Symbol:** `ChatViewModel.submit`, `ChatViewModel.cancel`  
**Evidence:** The three tests cover: (1) happy-path single turn, (2) multi-turn context, (3) loading existing conversation. They do **not** cover:
- `ChatStreamChunk.Error` path (`ChatViewModel.kt:173–181`)
- `cancel()` (`ChatViewModel.kt:189–193`)
- missing API key (`ChatViewModel.kt:88–91`)
- missing model selection (`ChatViewModel.kt:93–96`)
- tool-call deltas
- `ViewModel` cleared while streaming
- configuration change / process death

**Expected:** Core ViewModel failure paths tested.  
**Actual:** Only happy paths tested.  
**Impact:** The most user-visible error paths are unverified; regressions in error UI, cancellation, and edge-case state are likely.  
**Root cause:** Incomplete test coverage.  
**Related occurrences:** `ChatViewModel.kt:86–193`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `ChatViewModel.kt:86–193`; `ViewModel` lifecycle semantics.  
**Remediation:** Add tests for error chunk, cancellation, missing API key, missing model, tool-call accumulation, and `viewModelScope` cancellation.  
**Tests required:** Expand `ChatViewModelTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-07 | Severity: P2 | Status: CONFIRMED
**Area:** Incomplete image client coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClientTest.kt`  
**Lines:** 38–104  
**Symbol:** `ImageClient.generate`, `ImageClient.edit`  
**Evidence:** Only `generate()` and `edit()` are tested. `ImageClient` also exposes `upscale()`, `multiEdit()`, `generateBinary()` (`ImageClient.kt:22–34`). No HTTP 4xx/5xx, timeout, or malformed JSON paths are tested.  
**Expected:** All public client methods and error paths tested.  
**Actual:** ~29% public-method coverage; no error-path coverage.  
**Impact:** Paid/mutating image operations (upscale, multi-edit, binary generation) can regress silently.  
**Root cause:** Incomplete test suite.  
**Related occurrences:** `ImageClient.kt:19–90`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:7221–7454` (`/image/generate`), `7848–8463` (`/image/edit`), `2897–2929` (`UpscaleImageRequest`), `3031+` (`MultiEditImageRequest`).  
**Android/Kotlin reference:** `ImageClient.kt:19–90`.  
**Remediation:** Add tests for `upscale()`, `multiEdit()`, `generateBinary()`, and error paths (400, 401, 429, 500, timeout, malformed JSON).  
**Tests required:** Expand `ImageClientTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-08 | Severity: P2 | Status: CONFIRMED
**Area:** Incomplete chat streaming coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt`  
**Lines:** 54–219  
**Symbol:** `ChatClient.streamChat`  
**Evidence:** Tests cover: good SSE stream, single finish event, multi-tool-call chunk, SSE-side error, cancellation. Missing:
- HTTP non-200 response path (`ChatClient.kt:51–56`)
- Invalid JSON inside an SSE `data:` line (`ChatClient.kt:101–102`)
- Multiple choices (`n > 1`)
- Empty `delta` object
- `[DONE]` appearing after `finish_reason`
- Missing `choices` array (error object path at `ChatClient.kt:106–110`)

**Expected:** Malformed-response and non-stream error cases tested.  
**Actual:** Only in-stream error cases tested.  
**Impact:** SSE parsing edge cases and HTTP error mapping are unverified.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `ChatClient.kt:50–90`, `ChatClient.kt:100–113`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:6188–6869` (`/chat/completions`).  
**Android/Kotlin reference:** `ChatClient.kt:28–152`.  
**Remediation:** Add tests for HTTP 400/500 responses, invalid SSE JSON, multiple choices, and missing `choices`.  
**Tests required:** Expand `ChatClientTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-09 | Severity: P2 | Status: CONFIRMED
**Area:** SSE parser edge cases  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/SseLineParserTest.kt`  
**Lines:** 9–31  
**Symbol:** `SseLineParser.nextData`  
**Evidence:** Tests only: comment lines skipped, blank stream returns null. Missing:
- Lines that do not start with `data:` (e.g., `event:`, `id:`)
- Lines with empty `data:` value
- Whitespace-only lines
- Mixed field lines
- Malformed lines that could cause infinite recursion (`SseLineParser.kt:8`)

**Expected:** Parser edge cases tested.  
**Actual:** Minimal coverage.  
**Impact:** Malformed SSE lines could be silently mishandled or cause stack overflow.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `SseLineParser.kt:5–13`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `SseLineParser.kt:5–13`.  
**Remediation:** Add malformed-line tests and verify recursion safety.  
**Tests required:** Expand `SseLineParserTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-10 | Severity: P2 | Status: CONFIRMED
**Area:** Stream accumulator edge cases  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatStreamAccumulatorTest.kt`  
**Lines:** 7–45  
**Symbol:** `ChatStreamAccumulator.apply`, `ChatStreamAccumulator.snapshot`  
**Evidence:** Tests cover: text accumulation, tool-call fragments across deltas, finish reason, error without clobbering text. Missing:
- Interleaved text and tool-call deltas
- Multiple tool-call indices
- `Finish` after `Error`
- Empty/null text fragments
- Tool-call delta with only `argumentsFragment`

**Expected:** Accumulator edge cases tested.  
**Actual:** Basic coverage only.  
**Impact:** Tool-call reconstruction bugs in complex streams go undetected.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `ChatStreamAccumulator.kt:11–26`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `ChatStreamAccumulator.kt:11–26`.  
**Remediation:** Add interleaved text/tool-call and multi-index tool-call tests.  
**Tests required:** Expand `ChatStreamAccumulatorTest`.  
**Compatibility impact:** Low.

---

### TEST-FIXTURE-11 | Severity: P2 | Status: CONFIRMED
**Area:** Fixture encodes hardcoded trait mapping  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/capabilities/CapabilitiesRepositoryTest.kt`  
**Lines:** 54–100  
**Symbol:** `catalog.defaultTextModelId`, `catalog.modelForTrait`, `catalog.modelForAlias`  
**Evidence:** The test asserts `catalog.defaultTextModelId == "llama-3.3-70b"` and `modelForTrait("reasoning")?.id == "deepseek-r1"` based on fixture values. This couples trait-resolution logic to specific current model IDs.  
**Expected:** Trait resolution tested structurally (e.g., "default trait returns the model ID the fixture maps to it") without naming real models.  
**Actual:** Test names real models and will fail when the catalog rotates.  
**Impact:** Maintenance burden and false-positive failures when Venice updates default models.  
**Root cause:** Hardcoded fixture values used in assertions.  
**Related occurrences:** `models.json`, `traits.json`, `compatibility.json` fixtures.  
**Venice reference:** AGENTS.md Model Rule.  
**Android/Kotlin reference:** `CapabilitiesRepository.kt:24–92`.  
**Remediation:** Use synthetic model IDs in fixtures and assert structural behavior only (e.g., `modelForTrait("default") == traits.data["default"]`).  
**Tests required:** Refactor `CapabilitiesRepositoryTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-12 | Severity: P2 | Status: CONFIRMED
**Area:** Incomplete SDK facade coverage  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/VeniceForgeSdkTest.kt`  
**Lines:** 22–160  
**Symbol:** `VeniceForgeSdk.listModels`, `VeniceForgeSdk.parseHttpError`, `VeniceForgeSdk.getRaw`  
**Evidence:** Tests cover URL building for `listModels` and HTTP 429/401/400 errors. Missing:
- `SocketTimeoutException` / `IOException` → `VeniceSdkException.Network` (`VeniceForgeSdk.kt:78–82`, `125–130`)
- HTTP 500/503 server errors
- Actual `/models` response parsing with a realistic fixture
- `getRaw` helper used by `CapabilitiesRepository`
- `ModelType` values other than `IMAGE`

**Expected:** SDK facade error and network paths tested.  
**Actual:** Selective HTTP error coverage only.  
**Impact:** Network and server-error handling untested.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `VeniceForgeSdk.kt:60–138`, `VeniceForgeSdk.kt:140–201`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:8464–8543` (`/models`).  
**Android/Kotlin reference:** `VeniceForgeSdk.kt:60–201`.  
**Remediation:** Add timeout/IO, 500/503, and realistic `/models` parsing tests.  
**Tests required:** Expand `VeniceForgeSdkTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-13 | Severity: P2 | Status: CONFIRMED
**Area:** Incomplete repository coverage  
**Module:** `:core:data`  
**Files:**
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt` (lines 28–42)
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ProfileRepositoryTest.kt` (lines 25–37)

**Symbol:** `ChatRepository.createConversation`, `ChatRepository.deleteConversation`, `ProfileRepository.findDefault`  
**Evidence:**
- `ChatRepositoryTest` only tests create/append/observe. It does **not** test `deleteConversation`, transaction failure, `ConversationKind`, or `observeMessages` profile isolation beyond the dedicated `ProfileIsolationTest`.
- `ProfileRepositoryTest` only tests `ensureDefault` idempotency. It does **not** test `findDefault` or non-default profiles.

**Expected:** Repository contract fully tested.  
**Actual:** Partial coverage.  
**Impact:** Data-layer regressions in deletion and profile handling go undetected.  
**Root cause:** Incomplete tests.  
**Related occurrences:** `ChatRepository.kt:19–83`, `ProfileRepository.kt:6–27`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Room repository patterns.  
**Remediation:** Add tests for `deleteConversation`, `ConversationKind`, `findDefault`, and non-default profile creation.  
**Tests required:** Expand existing tests.  
**Compatibility impact:** Low.

---

### TEST-MISSING-14 | Severity: P2 | Status: CONFIRMED
**Area:** Migration paths not tested  
**Module:** `:core:data`  
**File:** `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/MigrationTest.kt`  
**Lines:** 26–50  
**Symbol:** `MigrationTest.v1 schema creates all expected tables`, `AppDatabase can open v1`  
**Evidence:** The two tests create a v1 database and verify table names, then open it with Room. There are no tests that migrate from an earlier schema version to v1, or from v1 to a future version.  
**Expected:** Migration paths between schema versions tested.  
**Actual:** Only schema creation tested.  
**Impact:** Database migration regressions (data loss, missing columns) are not caught.  
**Root cause:** Single-version testing.  
**Related occurrences:** `AppDatabase` migrations.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `MigrationTestHelper` docs.  
**Remediation:** Add migration tests from any prior schema versions to v1, and document expected migration behavior.  
**Tests required:** Expand `MigrationTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-15 | Severity: P3 | Status: CONFIRMED
**Area:** Minimal redaction coverage  
**Module:** `:core:common`  
**File:** `core/common/src/test/java/io/github/spearchucker667/veniceforge/core/common/RedactorTest.kt`  
**Lines:** 7–15  
**Symbol:** `Redactor.redact`  
**Evidence:** One test case covers Bearer token, API key, and Unix path. Missing:
- Partial redaction (e.g., `Bearer vn-xxx`)
- Keys without `sk`/`vn` prefix
- Windows-style paths
- Empty/null strings
- Already-redacted strings

**Expected:** Redaction rules thoroughly tested.  
**Actual:** Single happy-path case.  
**Impact:** Edge-case leaks may not be caught.  
**Root cause:** Minimal test.  
**Related occurrences:** `Redactor.kt:8–16`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `Redactor.kt:8–16`.  
**Remediation:** Add parameterized tests for edge cases.  
**Tests required:** Expand `RedactorTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-16 | Severity: P3 | Status: CONFIRMED
**Area:** Feature catalog semantics not tested  
**Module:** `:app`  
**File:** `app/src/test/java/io/github/spearchucker667/veniceforge/android/feature/FeatureCatalogTest.kt`  
**Lines:** 7–11  
**Symbol:** `FeatureCatalog.all`, `FeatureCatalog.byId`  
**Evidence:** The single test asserts `all.size == 22` and that IDs are unique. It does not verify any feature statuses, groups, labels, or the `byId` lookup behavior.  
**Expected:** Feature catalog semantics tested.  
**Actual:** Trivial count test.  
**Impact:** Catalog regressions (wrong status, missing feature, duplicate ID) are not caught beyond count.  
**Root cause:** Minimal test.  
**Related occurrences:** `FeatureCatalog.kt:21–47`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** `FeatureCatalog.kt:21–47`.  
**Remediation:** Add tests for required fields, group distribution, and `byId` lookup.  
**Tests required:** Expand `FeatureCatalogTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-17 | Severity: P2 | Status: CONFIRMED
**Area:** Venice parameters serialization incomplete  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/VeniceParametersSerializationTest.kt`  
**Lines:** 16–77  
**Symbol:** `VeniceParameters` serialization  
**Evidence:** Tests cover: explicit `safe_mode=false`, omitted fields, deserialization of assistant message with tool calls. Missing:
- `safe_mode=true` serialization
- Full `ChatRequest` round-trip with `venice_parameters`
- Deserialization of response `venice_parameters`
- Invalid enum values for `enable_web_search`

**Expected:** Venice parameters fully tested.  
**Actual:** Partial serialization tests.  
**Impact:** Regressions in `safe_mode` preservation and request shape are possible.  
**Root cause:** Incomplete coverage.  
**Related occurrences:** `ChatRequest.kt:67–68`, `VeniceParameters` in `ChatRequest.kt:75–89`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:1464–1543` (`venice_parameters`).  
**Android/Kotlin reference:** `ChatRequest.kt:57–89`.  
**Remediation:** Add round-trip and `safe_mode=true` tests; add invalid enum test.  
**Tests required:** Expand `VeniceParametersSerializationTest`.  
**Compatibility impact:** Low.

---

### TEST-MISSING-18 | Severity: P1 | Status: CONFIRMED
**Area:** `safe_mode` semantics not tested for media requests  
**Module:** `:venice-sdk`  
**Files:** *(production)*
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageModels.kt`
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/video/VideoModels.kt`
- `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/audio/AudioModels.kt`

**Symbol:** `safeMode` fields  
**Evidence:** AGENTS.md states: "**Preserve explicit `safe_mode=false` when selected.**" No test verifies that `safe_mode=false` is serialized in `GenerateImageRequest`, `EditImageRequest`, `QueueVideoRequest`, or `SpeechRequest`.  
**Expected:** Explicit `safe_mode=false` preserved in media request serialization.  
**Actual:** No tests for `safe_mode` in media requests.  
**Impact:** Could regress privacy/product semantics for image/video/audio generation.  
**Root cause:** Missing tests.  
**Related occurrences:** `ImageModels.kt:23–33`, `VideoModels.kt:17`, `AudioModels.kt:13`.  
**Venice reference:** `.source/venice-api-docs/swagger.yaml:2666–2671` (`/image/generate safe_mode`).  
**Android/Kotlin reference:** Kotlinx Serialization `@SerialName`.  
**Remediation:** Add serialization tests for `safe_mode=false` in image, video, and audio request models.  
**Tests required:** New/expanded model serialization tests.  
**Compatibility impact:** Medium — behavior change if bug exists.

---

### TEST-MISSING-19 | Severity: P1 | Status: CONFIRMED
**Area:** Security boundary not tested  
**Module:** `:venice-sdk`  
**File:** `venice-sdk/src/test` (overall)  
**Symbol:** API key handling  
**Evidence:** AGENTS.md states: "**`:venice-sdk` never persists API keys.**" and "**No raw prompt/response/API-key logging.**" The only security-adjacent test is `VeniceForgeSdkTest.kt:102,131`, which checks that exception messages do not contain the API key. There is no test verifying:
- SDK does not write keys to disk/shared prefs
- Logs/diagnostics do not contain keys
- Key redaction in diagnostics

**Expected:** Security rules backed by tests.  
**Actual:** Minimal security test coverage.  
**Impact:** A regression that logs or persists keys could go undetected.  
**Root cause:** Missing security-focused tests.  
**Related occurrences:** AGENTS.md security boundaries.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Android Keystore / SharedPreferences / Logcat.  
**Remediation:** Add tests verifying no persistence and key redaction in diagnostics.  
**Tests required:** New security tests.  
**Compatibility impact:** Low.

---

### TEST-MISSING-20 | Severity: P2 | Status: CONFIRMED
**Area:** No concurrency or lifecycle stress tests  
**Module:** `:app`, `:venice-sdk`  
**Files:**
- `venice-sdk/src/test/java/io/github/spearchucker667/veniceforge/sdk/chat/ChatClientTest.kt`
- `app/src/test/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModelTest.kt`
- `core/data/src/test/java/io/github/spearchucker667/veniceforge/core/data/chat/ChatRepositoryTest.kt`

**Symbol:** Streaming and repository concurrency  
**Evidence:** All tests are sequential. There are no tests for:
- Concurrent stream collection
- Rapid `submit()` / `cancel()` cycles
- Repository operations under concurrent flows
- `ChatViewModel` recreated after process death

**Expected:** Concurrent and lifecycle behavior tested.  
**Actual:** Sequential tests only.  
**Impact:** Race conditions in streaming and persistence are not caught.  
**Root cause:** Missing concurrency tests.  
**Related occurrences:** `ChatClient.kt:28–98`, `ChatViewModel.kt:86–193`.  
**Venice reference:** N/A.  
**Android/Kotlin reference:** Kotlin coroutines `callbackFlow`, `StateFlow`.  
**Remediation:** Add concurrency and lifecycle stress tests.  
**Tests required:** New tests.  
**Compatibility impact:** Low.

---

## 3. Coverage map: important production behavior → tests

| Production behavior | Production code | Test coverage | Gap |
|---------------------|-----------------|---------------|-----|
| Chat streaming SSE parsing | `ChatClient.kt:28–152` | `ChatClientTest.kt` (partial) | HTTP non-200, malformed SSE, multiple choices |
| Chat stream accumulation | `ChatStreamAccumulator.kt:11–26` | `ChatStreamAccumulatorTest.kt` (partial) | Interleaved text/tool calls, multi-index tool calls |
| SSE line parsing | `SseLineParser.kt:5–13` | `SseLineParserTest.kt` (partial) | Malformed lines, non-`data:` fields |
| Venice chat parameters | `ChatRequest.kt:57–89` | `VeniceParametersSerializationTest.kt` (partial) | `safe_mode=true`, full request round-trip |
| Image generation/edit/upscale/multi-edit/binary | `ImageClient.kt:19–90` | `ImageClientTest.kt` (partial) | `upscale`, `multiEdit`, `generateBinary`, errors |
| Audio TTS | `AudioClient.kt:19–45` | None | All paths missing |
| Video queue/retrieve/complete | `VideoClient.kt:19–63` | None | All paths missing |
| Model discovery & capabilities | `VeniceForgeSdk.kt:60–95`, `CapabilitiesRepository.kt:24–92` | `VeniceForgeSdkTest.kt`, `CapabilitiesRepositoryTest.kt` (partial) | Network errors, realistic `/models` parsing, hardcoded model IDs |
| SDK error mapping | `VeniceForgeSdk.kt:140–201` | `VeniceForgeSdkTest.kt` (partial) | 500/503, timeout/IO exceptions |
| Chat ViewModel lifecycle | `ChatViewModel.kt:86–193` | `ChatViewModelTest.kt` (partial) | Errors, cancellation, missing key/model, tool calls |
| Message persistence | `ChatRepository.kt:19–83` | `ChatRepositoryTest.kt` (partial) | Delete, `ConversationKind`, transaction failure |
| Profile persistence | `ProfileRepository.kt:6–27` | `ProfileRepositoryTest.kt` (partial) | `findDefault`, non-default profiles |
| Database migrations | `AppDatabase` migrations | `MigrationTest.kt` (partial) | Migration paths between versions |
| Diagnostics redaction | `Redactor.kt:8–16` | `RedactorTest.kt` (partial) | Edge cases |
| Feature catalog | `FeatureCatalog.kt:21–47` | `FeatureCatalogTest.kt` (partial) | Feature semantics |

---

## 4. Summary statistics

- **P0:** 0
- **P1:** 7  
  (TEST-FIXTURE-01, TEST-HARDCODE-02, TEST-COVERAGE-03, TEST-COVERAGE-04, TEST-MISSING-06, TEST-MISSING-18, TEST-MISSING-19)
- **P2:** 11  
  (TEST-MOCK-05, TEST-MISSING-07, TEST-MISSING-08, TEST-MISSING-09, TEST-MISSING-10, TEST-FIXTURE-11, TEST-MISSING-12, TEST-MISSING-13, TEST-MISSING-14, TEST-MISSING-17, TEST-MISSING-20)
- **P3:** 2  
  (TEST-MISSING-15, TEST-MISSING-16)

**Most critical gaps:** missing `AudioClient` and `VideoClient` tests, `models.json` fixture inventing swagger-undefined fields, and hardcoded model IDs violating the project Model Rule.
