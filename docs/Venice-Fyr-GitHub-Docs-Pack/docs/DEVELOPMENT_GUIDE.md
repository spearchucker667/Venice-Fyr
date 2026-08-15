# Development Guide

## Repository authority

For Android implementation decisions, use this order:

1. current Android repository contracts;
2. current Venice Forge desktop implementation for behavior;
3. tracked Venice API references selected by the desktop source manifest;
4. live capability endpoints for active model metadata.

Do not replace repository evidence with memory.

## Write target vs reference source

Android:

```text
Venice-Fyr/
```

Desktop reference:

```text
.source/Venice_Forge-desktop/
```

The desktop checkout is read-only during normal port work.

## Modules

### `:app`

Owns Android application behavior, Compose UI and app-level secret handling.

### `:venice-sdk`

Owns reusable Venice API client/contracts.

It must not silently persist API credentials.

### `:core:common`

Shared primitives and cross-cutting helpers.

### `:core:security`

App-facing Android security/credential facilities.

### `:core:designsystem`

Shared Compose visual system.

### `:core:data`

Room persistence and profile-scoped repositories.

## Feature implementation workflow

Before porting a desktop feature:

1. refresh/resolve the desktop source;
2. record the desktop HEAD;
3. inspect implementation and domain types;
4. inspect tests and verification scripts;
5. identify privilege/security boundaries;
6. identify Venice API contracts involved;
7. implement Android-native behavior;
8. add tests;
9. run narrow validation;
10. run module build;
11. update parity documentation.

## Validation

Typical baseline:

```bash
./gradlew test
./gradlew lint
./gradlew :app:assembleDebug
./gradlew :venice-sdk:assembleRelease
```

For Room/database changes, run repository/migration tests.

For streaming code, cover cancellation and resource cleanup.

For profile-owned data, test cross-profile isolation.

## Database changes

Do not casually change Room schemas.

For durable schema changes:

- increment the database version;
- export/update schema JSON;
- write a migration;
- add migration tests;
- test profile isolation after migration;
- verify destructive fallback is not silently enabled where data preservation is expected.

## API parsing

Venice can add models, traits and capability values without an app update.

Prefer forward-compatible parsing that preserves or tolerates unknown values where the API contract permits it.

## Streaming

SSE/chat streaming should correctly handle:

- multiple `data:` events;
- blank-line event boundaries;
- `[DONE]`;
- incremental tool-call deltas;
- cancellation;
- malformed/unknown events according to contract;
- HTTP/API errors;
- coroutine/resource cleanup.

## Documentation

Documentation should describe **implemented state**, not just planned state.

If an implementation plan says a feature will exist, that alone is not evidence that it exists.

## Review checklist

Before marking work complete:

- [ ] relevant source authority inspected
- [ ] no secrets/logging regressions
- [ ] profile isolation preserved
- [ ] API behavior covered by fixtures/tests
- [ ] cancellation/error paths tested
- [ ] module build passes
- [ ] parity docs updated if status changed
- [ ] no local-machine files staged
