# Changelog

All notable user-facing changes should be recorded here once the project begins producing versioned releases.

This project is currently moving rapidly through Android port milestones. Git history and the feature-parity matrix may contain more granular implementation detail than this file.

The format is inspired by Keep a Changelog and versioning should follow the project's eventual release policy.

## [Unreleased]

### Added
- Native Android port foundation.
- Reusable `:venice-sdk` module boundary.
- Core security/common/design-system modules.
- Room-backed `:core:data` foundation.
- Android/desktop parity documentation and source-bootstrap workflow.
- Image Studio foundation: `ImageScreen`, `ImageViewModel`, Photo Picker SAF integration, binary stream decoding, and local cache-backed URI loading.
- `:venice-sdk` Image client (`ImageClient`) with binary response support for `return_binary=true`.
- `:venice-sdk` Audio client (`AudioClient`) with `/audio/speech` direct binary stream support.
- `:venice-sdk` Video client (`VideoClient`) with `/video/queue`, `/video/retrieve`, `/video/complete`, and dynamic Content-Type stream/status discriminator.
- Typed queued-audio quote/queue/retrieve/complete operations, audio transcription, and voice cloning.
- Video quote and transcription operations.
- Separate chat reasoning request controls and `ReasoningDelta` stream events.

### Security
- Android-native credential-storage boundary.
- No-telemetry-by-default project contract.
- HTTPS-only networking contract.
- Redaction requirements for credentials and sensitive local data.

### Changed
- Ongoing migration of Venice Forge desktop behavior to Android-native architecture.
- Chat streaming now uses OkHttp's asynchronous call path so cancelling collection immediately cancels the underlying request, even while the response reader is blocked.
- Chat SSE parsing now honors multiline event framing, routes non-2xx responses through structured SDK exceptions, redacts provider error messages, rejects `stream=false` at the streaming boundary, and treats unexpected EOF as a protocol failure.
- Image edit, multi-edit, and upscale now return binary bytes with MIME/request metadata instead of attempting JSON deserialization.
- Video retrieval now distinguishes JSON `PROCESSING`, JSON `COMPLETED` for remote-download jobs, unknown statuses, and binary video completion.
- HTTP 402 responses now surface as `VeniceSdkException.PaymentRequired`.
- Trait and compatibility discovery now forwards discovered model modalities and namespaces non-text keys.

### Breaking SDK corrections
- `ImageClient.edit`, `multiEdit`, and `upscale` now return `BinaryMediaResult`.
- `VideoRetrieveResult.Completed` is replaced by `CompletedBinary`; JSON completion is `CompletedRemote`.
- `QueueVideoRequest.duration` is required and non-schema request fields were removed.
- Unsupported chat `safe_mode` and unimplemented `enable_e2ee` request switches were removed from `VeniceParameters`.
- Non-schema `ModelType.AUDIO` was removed; audio-producing catalogs use `TTS` and `MUSIC`, while speech recognition uses `ASR`.

### Known limitations
- Android feature parity is incomplete.
- Planned or scaffolded destinations must not be interpreted as finished features.
- Consult `docs/FEATURE_PARITY_MATRIX.md` for current status.

[Unreleased]: https://github.com/spearchucker667/Venice-Fyr/commits/main
