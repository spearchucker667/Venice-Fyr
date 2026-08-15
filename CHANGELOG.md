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

### Security
- Android-native credential-storage boundary.
- No-telemetry-by-default project contract.
- HTTPS-only networking contract.
- Redaction requirements for credentials and sensitive local data.

### Changed
- Ongoing migration of Venice Forge desktop behavior to Android-native architecture.

### Known limitations
- Android feature parity is incomplete.
- Planned or scaffolded destinations must not be interpreted as finished features.
- Consult `docs/FEATURE_PARITY_MATRIX.md` for current status.

[Unreleased]: https://github.com/spearchucker667/Venice-Fyr/commits/main
