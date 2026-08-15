# Venice SDK Guide

The `:venice-sdk` module is the reusable Android library boundary for Venice API operations.

## Ownership boundary

The SDK may accept credentials from a caller for an individual operation or configured client instance, but it must not silently persist those credentials.

Persistent credential storage belongs to the application/security layer.

## Build

```bash
./gradlew :venice-sdk:assembleRelease
```

Expected AAR location follows the standard Android library output layout under:

```text
venice-sdk/build/outputs/aar/
```

## Local project dependency

Inside this repository, the application uses the SDK as a Gradle project dependency:

```kotlin
implementation(project(":venice-sdk"))
```

## API evolution

Venice model and capability metadata is dynamic.

SDK behavior should:

- parse typed known fields;
- tolerate forward-compatible additions where possible;
- avoid hardcoded model allowlists as the source of truth;
- expose errors accurately;
- preserve cancellation for streaming operations;
- avoid leaking authentication material into exceptions/logs.

## Streaming

Consumers should treat streaming APIs as cancellable asynchronous operations.

Callers must be able to cancel collection when the UI/lifecycle no longer needs the stream.

SDK implementation must release HTTP/response resources on completion, failure and cancellation.

## Credential example

Do not publish examples with live keys.

Use placeholders:

```text
VENICE_API_KEY=<your-key>
```

Application examples should fetch the key from the app's secure storage and inject it only at the API boundary.

## Publishing

This repository currently documents building the local AAR. Do not claim Maven Central/GitHub Packages availability unless publishing configuration and an actual release exist.

Before publishing the SDK externally, review:

- artifact coordinates;
- semantic version policy;
- POM metadata;
- license metadata;
- consumer ProGuard/R8 rules;
- API compatibility;
- source/Javadoc artifacts;
- signing;
- release CI;
- third-party notices.
