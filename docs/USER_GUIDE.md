# User Guide

> [!WARNING]
> Venice Fyr is in active development. User-visible screens or navigation entries may exist before the underlying feature reaches full parity.

## What the app is for

Venice Fyr is a native Android client designed to bring Venice Forge workflows to mobile while keeping Android-native security, storage and lifecycle behavior.

The long-term product surface includes Venice AI chat, agents, image/video/audio/media workflows and related multimodal tools. Actual availability must be determined from the current build and [`FEATURE_PARITY_MATRIX.md`](FEATURE_PARITY_MATRIX.md).

## API key

Venice-backed features require appropriate provider credentials.

Repository architecture requires persistent credentials to remain in the application security layer rather than the reusable SDK.

Never paste an API key into:

- GitHub issues;
- screenshots intended for support;
- public logs;
- shared configuration files.

## Model availability

Do not assume every model is always available.

The project treats Venice model capabilities as runtime data. Model availability, traits, compatibility and pricing can change independently of an Android release.

A user interface may therefore change which options are shown after capability refresh.

## Network use

Remote AI operations require network access and send the selected request content to the configured provider.

Large media features may consume significant bandwidth.

## Storage and documents

The project is designed to use Android-scoped access rather than broad filesystem access.

Where a document/media picker is used, grant access only to the content intended for that operation.

## Generated content

Generated content may be inaccurate or unsuitable. Review it before relying on it.

Paid or mutating operations should present the approvals required by the implementation. If an action appears to submit repeatedly, stop and report the issue rather than retrying aggressively.

## Privacy

See [`../PRIVACY.md`](../PRIVACY.md).

## Troubleshooting

See [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md).

For a missing desktop feature, check the parity matrix before filing a bug.
