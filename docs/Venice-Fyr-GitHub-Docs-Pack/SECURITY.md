# Security Policy

## Project status

Venice Fyr is under active development. Security expectations apply even during alpha development, but supported release/version commitments should not be invented before stable releases exist.

## Reporting a vulnerability

Do **not** publish exploitable vulnerability details, API keys, private prompts, access tokens, personal data or other sensitive evidence in a public issue.

Preferred reporting path:

1. Use GitHub's **Private vulnerability reporting / Security Advisory** interface for this repository if it is enabled.
2. If private reporting is not enabled, contact the repository maintainer through an available private GitHub contact path and request a secure disclosure channel.
3. Public issues may be used only for non-sensitive security hardening or documentation issues that do not disclose an exploitable condition.

A useful report includes:

- affected commit/version;
- affected module;
- impact;
- reproduction steps;
- proof-of-concept details sufficient to validate the issue;
- Android/device/API level where relevant;
- whether credentials, profile isolation, file access, network boundaries or provider actions are involved.

## High-priority security boundaries

Treat failures in these areas as release blockers:

- API-key exposure or plaintext persistence;
- logs containing raw credentials or sensitive prompt/response content;
- cross-profile data leakage;
- authorization/approval bypass for paid or mutating operations;
- duplicate submission of paid/mutating operations;
- unsafe file/document access beyond explicit Android grants;
- TLS/network-security regression;
- SDK behavior that silently persists credentials;
- injection across tool/agent privilege boundaries;
- database migration that exposes or corrupts private data;
- exported Android components that unintentionally expose privileged functionality.

## Secrets

Never commit:

- Venice API keys;
- provider tokens;
- signing keys;
- keystores;
- service-account credentials;
- production endpoints containing embedded secrets;
- user data captured during testing.

Use test fixtures with obviously fake values.

## Dependency and supply-chain review

Before release:

- review dependency updates;
- inspect Gradle lock/version changes where applicable;
- review bundled native code;
- verify signing configuration;
- inspect generated manifest permissions/components;
- review third-party SDK data collection;
- run the repository's relevant unit, lint and build gates.

## AI / agent security

Agent or tool features must preserve explicit privilege boundaries.

Untrusted model output, documents, webpages, metadata and generated tool arguments must not be treated as trusted instructions merely because they originated inside an AI workflow.

Paid, destructive, privacy-sensitive or mutating actions require the confirmation and duplicate-submission controls defined by repository architecture.

## Coordinated disclosure

Maintainers should acknowledge private vulnerability reports promptly, reproduce the issue, assign severity, prepare a fix and coordinate disclosure timing with the reporter where practical.

Do not promise a fixed response SLA unless the project is prepared to maintain it.
