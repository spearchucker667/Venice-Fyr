# Getting Started

This guide covers a clean developer setup for Venice Fyr.

## Requirements

- Git
- JDK 17
- Android Studio compatible with AGP 9.3+
- Android SDK Platform 37
- required Android Build Tools / Platform Tools
- an Android device or emulator for runtime testing

The repository includes its Gradle wrapper. Do not install a separate global Gradle solely for this project.

## Clone

```bash
git clone https://github.com/spearchucker667/Venice-Fyr.git
cd Venice-Fyr
```

## Verify Java

```bash
java -version
javac -version
```

Both should resolve to JDK 17 for the known project baseline.

On macOS:

```bash
/usr/libexec/java_home -V
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```

## Verify Android SDK

The project targets Android API 37.

Typical checks:

```bash
sdkmanager --version
sdkmanager --list_installed
adb version
```

The supported macOS QA setup uses one canonical SDK root at `$HOME/Library/Android/sdk`. Keep `ANDROID_HOME` and `ANDROID_SDK_ROOT` identical and put that root's command-line tools, platform tools, and emulator first on `PATH`.

A machine-local `local.properties` may contain:

```properties
sdk.dir=/absolute/path/to/android/sdk
```

Never commit machine-specific `local.properties`.

## Verify Gradle

```bash
./gradlew --version
./gradlew projects
./gradlew help
```

Confirm the expected modules are present, including:

```text
:app
:venice-sdk
:core:common
:core:security
:core:designsystem
:core:data
```

## Build

Debug application:

```bash
./gradlew :app:assembleDebug
```

SDK AAR:

```bash
./gradlew :venice-sdk:assembleRelease
```

Full baseline validation:

```bash
./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease
```

Instrumentation suites may require a connected device/emulator.

For the supported ARM64 API 37 AVD, repeatable doctor, install, launch, navigation, screenshot, logcat, and instrumented-test commands, see [`ANDROID_EMULATOR_QA.md`](ANDROID_EMULATOR_QA.md):

```bash
./scripts/android-emulator-doctor.sh
./scripts/android-emulator-qa.sh --headless
```

## Desktop reference source

The Android repo is the write target. Venice Forge desktop remains a separate read-only behavioral source.

```bash
./scripts/bootstrap-desktop-source.sh
```

Read [`DESKTOP_SOURCE_BOOTSTRAP.md`](DESKTOP_SOURCE_BOOTSTRAP.md) before parity work.

## API credentials

Do not place real credentials in:

- source code;
- Gradle files;
- `local.properties`;
- committed `.env` files;
- screenshots;
- test fixtures.

Use the application's supported secure credential flow. Venice Fyr accepts a Venice API key; normal Venice.ai account login is not supported. A stored key is never reloaded into the editable Settings field.

## Next steps

- Read [`DEVELOPMENT_GUIDE.md`](DEVELOPMENT_GUIDE.md).
- Read [`../AGENTS.md`](../AGENTS.md).
- Check [`FEATURE_PARITY_MATRIX.md`](FEATURE_PARITY_MATRIX.md).
- For SDK work, read [`SDK_GUIDE.md`](SDK_GUIDE.md).
