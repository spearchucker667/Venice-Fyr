# Troubleshooting

## `Unable to locate a Java Runtime`

The known project baseline uses JDK 17.

Check:

```bash
java -version
javac -version
```

On macOS:

```bash
/usr/libexec/java_home -V
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```

Then:

```bash
./gradlew --version
```

The Gradle JVM should resolve to the intended JDK.

## Android SDK not found

Check:

```bash
echo "$ANDROID_HOME"
sdkmanager --version
adb version
```

If required, create a machine-local `local.properties`:

```properties
sdk.dir=/absolute/path/to/android/sdk
```

Do not commit it.

## Platform API 37 missing

Inspect installed packages:

```bash
sdkmanager --list_installed
```

Install the platform with the SDK manager if needed:

```bash
sdkmanager "platforms;android-37"
```

Use the Build Tools version required by the current Gradle/AGP configuration.

## Wrapper files missing

Use the repository-supported bootstrap rather than installing global Gradle:

```bash
./scripts/bootstrap-wrapper.sh
```

Then verify:

```bash
./gradlew --version
./gradlew projects
```

## Dependency resolution fails

Before changing versions:

1. read the failing Gradle error;
2. inspect `gradle/libs.versions.toml`;
3. inspect the affected module;
4. confirm AGP/Kotlin/KSP/Room compatibility;
5. distinguish repository incompatibility from temporary network failure.

Do not downgrade arbitrary dependencies merely to make resolution green.

## `local.properties` appears in Git status

It should remain machine-local.

Check:

```bash
git check-ignore -v local.properties
```

Fix `.gitignore` only if repository policy actually requires it.

## Desktop source bootstrap fails

Read [`DESKTOP_SOURCE_BOOTSTRAP.md`](DESKTOP_SOURCE_BOOTSTRAP.md).

Do not delete a dirty or unexpectedly configured source mirror to "fix" bootstrap. Report the mismatch first.

## A desktop feature is missing

Check [`FEATURE_PARITY_MATRIX.md`](FEATURE_PARITY_MATRIX.md).

A screen/route can exist before feature parity is complete.

## API call fails

Capture only sanitized diagnostics:

- endpoint;
- model ID;
- HTTP status;
- request ID if safe;
- timestamp;
- sanitized error body.

Never post API keys or authorization headers.

## Streaming hangs or does not cancel

For development:

- confirm coroutine cancellation reaches the HTTP call;
- confirm the response body closes;
- test `[DONE]` handling;
- inspect malformed event handling;
- verify lifecycle cancellation from ViewModel/UI.

## Room test failures

Check:

- database version;
- exported schema;
- migration path;
- foreign keys;
- profile IDs;
- in-memory database lifecycle.

Cross-profile leakage is a release-blocking failure, not an acceptable test adjustment.
