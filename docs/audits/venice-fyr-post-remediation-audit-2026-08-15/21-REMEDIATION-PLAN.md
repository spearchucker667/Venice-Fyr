# Remediation Plan

## WP-01 — Chat stable subset

Objective: add non-streaming completion and a versioned multimodal/tool/structured-output model. Addresses `POST-P1-01`. Authority: current chat Swagger/message unions. Compatibility: additive except content union migration. Tests: every supported content/tool variant and error path. Acceptance: no documented supported field is silently dropped.

## WP-02 — Capability cache

Objective: cache the now modality-aware traits/compatibility results with an explicit refresh policy. Tests: alias collision, expiry, partial auxiliary-endpoint failure, and refresh behavior. Acceptance: repeated UI collection does not refetch stable data and defaults remain runtime-derived.

## WP-03 — Durable media jobs

Objective: Room job state + WorkManager bounded poll/download/cleanup with explicit quote approval and duplicate defense. Addresses `POST-P1-03`. Tests: process death, timeout, unknown status, remote/binary completion, cleanup retry. Acceptance: no duplicate billable submission and no lost queue URL/ID.

## WP-04 — Image API completion and durability

Objective: background removal/styles/compat generation/multipart plus bounded decode, durable media metadata, SAF export. Addresses `POST-P2-01/03/07`. Tests: MIME/signature/size, content URI, denial, process death, export.

## WP-05 — Device and release evidence

Objective: smallest instrumentation suite, minified runtime smoke, binary compatibility, signing/provenance. Addresses `POST-P2-04/05`. Acceptance: device and published-artifact claims are evidence-backed.

## WP-06 onward — Product parity

Deliver functional screens in dependency order: history/media, audio/music/video, characters/RP, research, workflows/documents, then remaining studios. Each requires domain logic, persistence, lifecycle, accessibility, export/import, and tests before status changes.

## Release/legal gate

Resolve the Ayanami Rei redistribution authority or replace the asset before public binary distribution.
