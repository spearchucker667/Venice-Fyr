# Privacy

## Summary

Venice Fyr is designed as a privacy-conscious native Android client.

The current architectural contract requires:

- no telemetry by default;
- no plaintext persistent API-key storage;
- application-owned secure credential persistence using Android Keystore-backed mechanisms;
- no credential persistence inside `:venice-sdk`;
- no raw API keys, prompts, responses, or sensitive local paths in logs;
- HTTPS-only networking;
- explicit user grants for document/workspace access rather than broad filesystem access.

These are repository requirements, not permission to assume every future feature automatically satisfies them. Implementation and release review must verify the behavior.

## Data that may leave the device

When a user invokes a remote AI/API feature, information necessary to fulfill that request may be transmitted to the selected provider.

Depending on the feature, this can include:

- prompts and conversation context;
- images, video, audio, documents or other user-selected media;
- model identifiers and generation parameters;
- API authentication material sent in the form required by the provider;
- technical request metadata necessary for normal HTTPS/API operation.

The selected provider's privacy practices and service terms apply to provider-side processing.

## API credentials

The application layer owns persistent API credentials.

Repository policy requires credentials to be protected by Android-native secure storage and prevents the reusable `:venice-sdk` library from silently persisting them.

Do not:

- commit real credentials;
- place secrets in logs or crash output;
- put API keys in screenshots, sample configs or issue templates;
- store credentials in plaintext preferences/files.

## Local application data

Features may maintain local state such as profiles, conversations, messages, tool-call state, settings, cached capability metadata or generated-media references.

Storage behavior is expected to remain profile-scoped where applicable and follow the repository's security/storage contract.

Because the app is under active development, exact schemas and retention behavior may change. Release documentation must be updated when durable user-data behavior changes.

## Telemetry and analytics

The project contract is **no telemetry by default**.

If analytics, crash reporting or diagnostics upload are ever introduced:

1. they must not be silently enabled in contradiction with repository policy;
2. collection must be documented before release;
3. sensitive prompt/response/API-key material must remain excluded;
4. user controls and provider disclosures must be evaluated.

## Android permissions

The application should request only permissions required for implemented features.

Document/workspace access should use Android's scoped mechanisms such as the Storage Access Framework rather than broad storage permission.

A future feature requiring new sensitive permissions must update this policy and the security review before release.

## Deleting local data

Until stable user-facing data-management controls are implemented and verified, do not promise a specific deletion workflow in documentation.

Contributors adding deletion/export/reset functionality must test:

- profile isolation;
- referential cleanup;
- credential deletion;
- generated-media references;
- migration behavior;
- process-death/restart behavior.

## Third-party links and services

Venice Fyr may interact with third-party APIs or open external content. Those services have their own privacy policies and terms.

## Reporting a privacy problem

If a privacy issue is also a security vulnerability, follow [`SECURITY.md`](SECURITY.md) and avoid public disclosure of exploit details or sensitive user data.

For non-sensitive documentation/privacy corrections, open a normal GitHub issue.

## Policy maintenance

Before a public release, maintainers must compare this document with the actual application, manifest, network endpoints, dependencies and storage implementation.

If code and policy differ, this file must not be treated as authoritative evidence that the implementation is compliant.
