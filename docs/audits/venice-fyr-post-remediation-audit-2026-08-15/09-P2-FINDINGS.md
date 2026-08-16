# P2 Findings

## POST-P2-01

ID: `POST-P2-01`
Severity: P2
Status: CONFIRMED
Prior finding ID: `IMG-02`, `IMG-03`, `IMG-04`, `IMG-05`
Area: Image API coverage
Module: `:venice-sdk`
File: `venice-sdk/src/main/java/io/github/spearchucker667/veniceforge/sdk/image/ImageClient.kt`
Lines: current public client surface
Symbol: `ImageClient`
Evidence: The client exposes generation, edit, multi-edit, and upscale but has no background-removal, styles, compatibility-generations, or multipart request methods.
Spec evidence: `swagger.yaml` image endpoint families and request content declarations.
Expected: supported endpoints use spec-derived request/response models and the documented JSON or multipart encoding.
Actual: four image operations remain absent.
Impact: SDK consumers must bypass the SDK for those operations.
Root cause: initial image coverage implemented only the app's first workflow.
Related occurrences: no matching fixtures or examples exist.
Compatibility impact: additive API, except any future normalization of shared request models.
Remediation: add each endpoint from the authoritative Swagger content type and constraints.
Tests required: JSON and multipart wire assertions, binary/JSON success, malformed body, HTTP error, and cancellation.
Validation: absence confirmed by source and test inventory.

## POST-P2-02

ID: `POST-P2-02`
Severity: P2
Status: CONFIRMED
Prior finding ID: none
Area: Reasoning application contract
Module: `:app`
File: `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/chat/ChatScreen.kt`
Lines: streamed-delta handling and `UiMessage` rendering
Symbol: `ChatViewModel.submit`, `UiMessage`
Evidence: The SDK emits `ReasoningDelta` separately, but the app does not store reasoning in Room or expose it in the UI model.
Spec evidence: `reasoning_content`, `reasoning_details`, and reasoning request controls in `swagger.yaml`.
Expected: an explicit privacy/product policy determines whether reasoning is transient, persisted, exportable, and visible.
Actual: reasoning is consumed but discarded at the application boundary.
Impact: users cannot inspect reasoning and restarts cannot restore it.
Root cause: SDK protocol repair preceded the app data-contract decision.
Related occurrences: export/history schemas contain no reasoning field.
Compatibility impact: a persistence implementation requires a Room migration and export-policy decision.
Remediation: document the policy, then add a distinct reasoning field and opt-in UI if retention is approved.
Tests required: interleaved answer/reasoning, restart, export, privacy setting, and migration tests.
Validation: SDK separation is unit-tested; app omission confirmed statically.

## POST-P2-03

ID: `POST-P2-03`
Severity: P2
Status: CONFIRMED
Prior finding ID: `VM-08`, `ARCH-09`
Area: Image lifecycle and storage
Module: `:app`
File: `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageViewModel.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt`
Lines: image UI state and `saveBytesToCache`
Symbol: `ImageUiState`, `saveBytesToCache`
Evidence: selected input, result URI, and active operation state are memory-only; generated output is written to cache rather than a durable media record.
Spec evidence: local Android lifecycle/persistence contract in `AGENTS.md` and desktop generated-media behavior.
Expected: process-safe operation state and explicit durable save/export semantics with MIME retained.
Actual: process death loses state and cache eviction can remove results.
Impact: completed media can disappear and operation recovery is unavailable.
Root cause: the first UI milestone used transient state and cache output.
Related occurrences: export naming can imply a format rather than derive it from verified bytes.
Compatibility impact: additive saved-state and media persistence schema.
Remediation: introduce `SavedStateHandle` plus a durable media record/store and verified MIME/signature/hash metadata.
Tests required: process recreation, cache eviction, corrupt content, export/share, and MIME preservation.
Validation: confirmed by source inspection; process recreation is BLOCKED without device instrumentation.

## POST-P2-04

ID: `POST-P2-04`
Severity: P2
Status: CONFIRMED
Prior finding ID: `TEST-COVERAGE-03`, `TEST-COVERAGE-04`
Area: Android instrumentation
Module: repository
File: `app/src/androidTest` (absent), `.github/workflows/android-ci.yml`
Lines: test-source and CI configuration
Symbol: connected/managed-device test tasks
Evidence: no application instrumentation sources or managed-device configuration exist, and `adb devices -l` found no attached device.
Spec evidence: Android-native security, storage, picker, and lifecycle contracts in `AGENTS.md`.
Expected: a minimal on-device suite for behavior that JVM/Robolectric cannot prove.
Actual: only host-side tests and build/lint gates run.
Impact: Keystore, Room migrations, Photo Picker/SAF, process recreation, and minified runtime remain unproven.
Root cause: CI and test work stopped at host-side alpha gates.
Related occurrences: release APK has no install/launch smoke.
Compatibility impact: test/CI-only unless instrumentation reveals defects.
Remediation: add a managed virtual device and smallest stable security/storage/navigation smoke suite.
Tests required: Keystore round trip, Room migration, content URI persistence, process recreation, and release/minified launch.
Validation: BLOCKED; no emulator/device was attached.

## POST-P2-05

ID: `POST-P2-05`
Severity: P2
Status: CONFIRMED
Prior finding ID: `BUILD-03`, `BUILD-05`
Area: Release engineering
Module: repository
File: `.github/workflows/android-ci.yml`, `app/build.gradle.kts`, `venice-sdk/build.gradle.kts`
Lines: release/signing/publication configuration
Symbol: release pipeline
Evidence: unsigned release APK and release AAR build successfully, but no signing, provenance, published-artifact smoke, or SDK binary-compatibility check is configured.
Spec evidence: repository release expectations and Android artifact trust requirements.
Expected: signed/provenanced app artifacts and consumer-tested SDK artifacts before public release.
Actual: source-checkout compilation is the strongest release evidence.
Impact: a public release could regress consumer ABI or ship without an auditable artifact chain.
Root cause: current pipeline intentionally targets alpha buildability.
Related occurrences: no release install/launch smoke due absent device.
Compatibility impact: CI/release configuration; binary checks may expose breaking SDK changes.
Remediation: configure external signing, provenance/checksums, fresh-consumer AAR smoke, and a binary compatibility validator.
Tests required: signed APK verification/install/launch and fresh-project AAR compilation/runtime smoke.
Validation: unsigned artifacts were built and hashed; publication behavior is NOT RUN.

## POST-P2-06

ID: `POST-P2-06`
Severity: P2
Status: CONFIRMED
Prior finding ID: none
Area: Credential UI lifetime
Module: `:app`
File: `app/src/main/java/io/github/spearchucker667/veniceforge/android/ui/ConfigScreen.kt`
Lines: API-key input state
Symbol: local API-key Compose state
Evidence: plaintext input is retained in Compose state for the screen lifetime before secure persistence.
Spec evidence: repository rule prohibiting plaintext persistent credentials and requiring minimized secret exposure.
Expected: transient secret state is cleared promptly on save, disposal, and backgrounding.
Actual: the state lifetime is broader than necessary, although no plaintext disk persistence was found.
Impact: increases in-process exposure to snapshots or diagnostics.
Root cause: standard text-field state was used without an explicit secret-lifetime policy.
Related occurrences: no clipboard/export path for the key was found.
Compatibility impact: internal UI behavior only.
Remediation: clear input after secure save and on disposal/background; suppress sensitive semantics/snapshots where supported.
Tests required: lifecycle clearing and secure-store persistence on device.
Validation: confirmed statically; on-device lifecycle behavior is BLOCKED.

## POST-P2-07

ID: `POST-P2-07`
Severity: P2
Status: CONFIRMED
Prior finding ID: `VM-06`, `ARCH-08`, `ARCH-13`
Area: Media memory safety
Module: `:app`
File: `app/src/main/java/io/github/spearchucker667/veniceforge/android/VeniceForgeApp.kt`, `app/src/main/java/io/github/spearchucker667/veniceforge/android/image/ImageScreen.kt`
Lines: content read/Base64 conversion and bitmap decoding
Symbol: `readBytes`, result decode
Evidence: selected media is read wholly into memory before Base64 encoding and result decoding lacks an explicit byte/pixel ceiling and signature validation.
Spec evidence: Android bounded-resource and untrusted-content boundary requirements in `AGENTS.md`.
Expected: bounded streaming reads, verified media signatures/MIME, and sampled decoding.
Actual: large or malformed inputs can create avoidable memory pressure.
Impact: denial of service or process death on adversarial/large media.
Root cause: convenience byte-array APIs in the first media implementation.
Related occurrences: durable media metadata is also absent (`POST-P2-03`).
Compatibility impact: may reject oversized inputs that were previously attempted.
Remediation: enforce documented byte and pixel limits, stream encoding where possible, and sample decode.
Tests required: oversized, truncated, MIME-mismatch, decompression-bomb, and low-memory cases.
Validation: confirmed by call-site inspection; stress behavior is NOT RUN.

## POST-P2-08

ID: `POST-P2-08`
Severity: P2
Status: CONFIRMED
Prior finding ID: `ASSET-DUP-02`
Area: Asset licensing
Module: assets
File: `docs/assets/ayanami-rei.codex-pet/spritesheet.webp` and packaged runtime copy
Lines: binary asset
Symbol: redistribution authority
Evidence: source and runtime images are byte-identical, but the repository contains no authoritative grant establishing public redistribution rights for the character artwork.
Spec evidence: repository provenance/licensing requirements; no upstream Venice API question applies.
Expected: recorded redistribution authority or an independently licensed replacement.
Actual: provenance remains unresolved.
Impact: public distribution may carry copyright/licensing risk.
Root cause: the asset was added before provenance evidence was recorded.
Related occurrences: attribution text does not establish redistribution permission.
Compatibility impact: replacing the spritesheet may change visuals but not code contracts.
Remediation: verify and record authority or replace the artwork before public distribution.
Tests required: asset reference/hash inventory and packaged-resource smoke after replacement.
Validation: duplicate identity confirmed; legal authority remains unresolved.
