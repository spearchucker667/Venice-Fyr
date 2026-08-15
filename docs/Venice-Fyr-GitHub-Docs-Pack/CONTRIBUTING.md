# Contributing to Venice Fyr

Thanks for contributing.

Venice Fyr is not a generic Android rewrite. It is a behaviorally constrained native port of Venice Forge with explicit privacy, security, model-capability and API-contract requirements.

## Read before coding

Required:

1. [`AGENTS.md`](AGENTS.md)
2. [`ANDROID_PORT_HANDOFF.md`](ANDROID_PORT_HANDOFF.md)
3. [`docs/DESKTOP_SOURCE_BOOTSTRAP.md`](docs/DESKTOP_SOURCE_BOOTSTRAP.md)
4. [`docs/FEATURE_PARITY_MATRIX.md`](docs/FEATURE_PARITY_MATRIX.md)
5. [`docs/SECURITY_AND_STORAGE_CONTRACT.md`](docs/SECURITY_AND_STORAGE_CONTRACT.md)

For API work, also inspect [`docs/VENICE_API_PORT_MATRIX.md`](docs/VENICE_API_PORT_MATRIX.md) and the API sources identified by the desktop source manifest.

## Development setup

See [`docs/GETTING_STARTED.md`](docs/GETTING_STARTED.md).

Basic validation:

```bash
./gradlew --version
./gradlew projects
./gradlew test
./gradlew lint
./gradlew :app:assembleDebug
./gradlew :venice-sdk:assembleRelease
```

Run the narrowest relevant tests during development, then the affected module build.

## Desktop behavior source

Before implementing behavior that already exists in Venice Forge:

```bash
./scripts/bootstrap-desktop-source.sh
```

Treat the resulting desktop checkout as read-only.

Inspect:

- implementation code;
- stores/domain types;
- Electron privilege boundaries;
- tests;
- relevant verification scripts;
- Venice API references.

Do not implement from memory when repository evidence exists.

## Scope discipline

Prefer small, reviewable changes.

Do not combine unrelated:

- feature work;
- dependency upgrades;
- formatting sweeps;
- migrations;
- architecture refactors;
- documentation rewrites.

Do not create placeholder implementations that make parity look more complete than it is.

## Kotlin / Android expectations

- Follow existing Kotlin/Gradle conventions.
- Prefer Android-native APIs and Compose architecture.
- Do not add a WebView wrapper for the Electron application.
- Keep credentials out of `:venice-sdk`.
- Preserve profile isolation.
- Keep network/model capability behavior runtime-driven.
- Avoid broad storage permissions.
- Keep exported Android components to the minimum required.
- Add migrations for durable schema changes.

## Tests

Changes should include tests appropriate to the affected layer.

Examples:

- parser/serialization fixture tests;
- SSE streaming/cancellation tests;
- Room DAO/repository tests;
- profile-isolation tests;
- migration tests;
- ViewModel tests;
- instrumentation tests for Android-specific behavior.

A failing build/configuration is not a valid expected-red TDD result.

## Privacy and logging

Never log raw:

- API keys;
- authorization headers;
- full prompts/responses by default;
- user documents/media;
- sensitive local paths.

Use repository redaction helpers where appropriate.

## Documentation

Update documentation when a change alters:

- feature parity;
- supported APIs;
- storage behavior;
- permissions;
- security boundaries;
- user-visible setup;
- SDK API surface.

Do not copy planned-state wording into current-state docs without verifying the implementation.

## Issues

Before filing:

- search existing issues;
- remove secrets/private data;
- provide environment and reproduction details;
- distinguish a reproducible bug from a feature request.

Use the repository issue templates when available.

Security vulnerabilities belong in the private security-reporting path described in [`SECURITY.md`](SECURITY.md).

## Pull requests

A good pull request:

- has one clear purpose;
- explains behavior and risk;
- names tests run;
- links relevant issue/spec;
- updates parity/docs when necessary;
- contains no unrelated generated files;
- does not include secrets or local-machine state.

Reviewers should reject parity claims unsupported by code/tests.

## Commit messages

Use concise, imperative commits. Conventional-style subjects are encouraged where they match repository history, for example:

```text
feat(data): add Room migration coverage
fix(sdk): preserve unknown model traits
docs: clarify Android source bootstrap
test(chat): cover SSE cancellation
```

## Licensing contributions

By submitting a contribution, contributors agree that their contribution may be distributed under the repository's license, currently proposed as Apache License 2.0.

Do not submit third-party code unless its license permits inclusion and required notices are preserved.
