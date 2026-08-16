# P1 Findings

## POST-P1-01

ID: `POST-P1-01`
Severity: P1
Status: CONFIRMED
Prior finding ID: `CHAT-01`, `CHAT-02`, `CHAT-03`, `CHAT-04`
Area: Chat SDK completeness
Module: `:venice-sdk`
File: `chat/ChatRequest.kt`, `chat/ChatClient.kt`
Lines: current public request/client surface
Symbol: `ChatRequest`, `ChatMessage`, `ChatClient.streamChat`
Evidence: `streamChat` rejects `stream=false`; message content is nullable string; native search tool types, structured output, multimodal parts, stream usage, and many current request controls are absent.
Spec evidence: `swagger.yaml` `ChatCompletionRequest` and message unions.
Expected: a documented stable subset or complete typed support without silently dropping Venice features.
Actual: streaming text/function-tool subset.
Impact: external SDK consumers cannot use material current chat features.
Root cause: initial milestone scope was presented as a broader public request type.
Related occurrences: response-format, audio/image/video/file parts, parallel tools, usage and search metadata.
Compatibility impact: additive models plus a new non-streaming method; content union may be breaking.
Remediation: define a versioned content-part/tool model and typed non-streaming response before broadening app UI.
Tests required: every supported union variant, stream usage, tool fragmentation, and JSON completion errors.
Validation: current absence confirmed statically; not implemented in this work package.

## POST-P1-03

ID: `POST-P1-03`
Severity: P1
Status: CONFIRMED
Prior finding ID: `ARCH-05`, `VID-07`, `AUD-01` follow-on
Area: Billable queued-media durability
Module: `:app`
File: no WorkManager/media job implementation; `VeniceForgeApp.kt`
Lines: application routing/service graph
Symbol: absent durable queue orchestrator
Evidence: SDK queue primitives exist, but no app repository, bounded poller, WorkManager worker, persisted queue/download URL, timeout, retry classification, or duplicate-submission key exists.
Spec evidence: queue/retrieve/complete lifecycle in video/audio endpoint docs.
Expected: explicit quote/approval, one submission, durable state, bounded safe polling, verified download, then cleanup.
Actual: no app-consumed workflow.
Impact: implementing the UI directly on SDK calls would risk lost jobs or duplicate billable work.
Root cause: SDK foundation preceded app job architecture.
Related occurrences: video and music drawer destinations are scaffolds.
Compatibility impact: additive app/data schema and worker contracts.
Remediation: implement a Room-backed job state machine and WorkManager orchestration before media UI.
Tests required: process death, duplicate taps, timeout, unknown state, remote URL retention, binary completion, cleanup failure.
Validation: confirmed statically; device behavior not available.

## POST-P1-04

ID: `POST-P1-04`
Severity: P1 for a parity/release claim; P2 while explicitly alpha
Status: CONFIRMED
Prior finding ID: application completeness findings
Area: Product parity
Module: `:app`
File: `VeniceForgeApp.kt`, `FeatureCatalog.kt`
Lines: root routing
Symbol: `FeatureScreen` fallback
Evidence: only settings, chat, and image have dedicated screens; 19 other stable IDs route to the generic scaffold.
Spec evidence: desktop `src/config/tabs.ts` and current Android feature catalog.
Expected: parity claims require functional screens, domain logic, persistence, and tests.
Actual: README correctly labels the project alpha, so the current UI is honest but incomplete.
Impact: absolute blocker to a desktop-parity or production release claim.
Root cause: intentional incremental port.
Related occurrences: history, media, audio/music/video, research, characters, workflows, documents.
Compatibility impact: additive feature work.
Remediation: deliver feature milestones in dependency order; do not hide or relabel scaffolds.
Tests required: per-feature UI, persistence, lifecycle, failure, accessibility, export/import, and device tests.
Validation: routing inspected directly.
