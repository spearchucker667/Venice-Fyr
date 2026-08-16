# Android Emulator QA

Venice Fyr uses the official Android Studio and Android Emulator toolchain for local development and device QA. The repository does not require a paid emulator or cloud-device service.

## Supported local configuration

- Apple silicon macOS
- Android Studio stable
- JDK 17 for Gradle
- Canonical SDK root: `$HOME/Library/Android/sdk`
- Primary AVD: `Venice_Fyr_Agent_API_37`
- System image: Android API 37, Google APIs, `arm64-v8a`
- Device profile: Pixel 8

The repository currently compiles against Android API 37. Check `app/build.gradle.kts` before provisioning a new machine because that requirement may change.

## Shell environment

Keep one canonical SDK root and place its tools before any Homebrew compatibility installation:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

Put the block in `~/.zprofile` when login shells and GUI-launched developer tools must inherit it. Verify from a fresh shell:

```bash
zsh -lic 'command -v adb emulator sdkmanager avdmanager; printf "%s\n" "$ANDROID_HOME" "$ANDROID_SDK_ROOT"'
```

## Provision the AVD

Use `sdkmanager` to install the platform, build tools, emulator, platform tools, and ARM64 image required by the project. Review and accept Android SDK licenses interactively; do not fabricate license hashes.

```bash
sdkmanager --install \
  'platform-tools' \
  'emulator' \
  'platforms;android-37.0' \
  'build-tools;36.0.0' \
  'system-images;android-37.0;google_apis;arm64-v8a'

avdmanager create avd \
  --name Venice_Fyr_Agent_API_37 \
  --package 'system-images;android-37.0;google_apis;arm64-v8a' \
  --device pixel_8
```

The AVD is persistent under `~/.android/avd/`. The QA scripts never delete AVDs or stop unrelated emulators.

## Doctor and repeatable QA

```bash
./scripts/android-emulator-doctor.sh
./scripts/android-emulator-qa.sh --headless
```

The doctor validates host architecture, SDK selection, command-line tools, JDK, Gradle, required platform/build tools, AVD ABI, disk capacity, and connected devices. The QA script targets only the named emulator, starts it when necessary, waits for complete boot, builds and installs the debug app, resolves and launches its actual activity, verifies activity state, exercises the Menu control, and captures UI, screenshot, logcat, crash, StrictMode, and test evidence.

Each run writes a timestamped, gitignored directory under `artifacts/android-qa/`. Keep useful summaries outside Git or attach them to the release/audit record; do not commit SDKs, emulator disks, APKs, or large logs.

Override the AVD without editing the scripts:

```bash
VENICE_FYR_AVD=Another_ARM64_AVD ./scripts/android-emulator-qa.sh
```

## Manual launch and evidence

Always target the exact emulator serial and never rely on adb's implicit device selection:

```bash
export ANDROID_SERIAL=emulator-5554
./gradlew :app:installDebug

PACKAGE=io.github.spearchucker667.veniceforge.android
ACTIVITY="$(adb -s "$ANDROID_SERIAL" shell cmd package resolve-activity --brief "$PACKAGE" | tr -d '\r' | tail -n 1)"
adb -s "$ANDROID_SERIAL" shell am start -W -n "$ACTIVITY"
adb -s "$ANDROID_SERIAL" shell uiautomator dump /sdcard/venice-fyr.xml
adb -s "$ANDROID_SERIAL" pull /sdcard/venice-fyr.xml .
adb -s "$ANDROID_SERIAL" exec-out screencap -p > screen.png
adb -s "$ANDROID_SERIAL" logcat -d -v threadtime > logcat.txt
adb -s "$ANDROID_SERIAL" logcat -d -b crash -v threadtime > crashes.txt
```

## Instrumented tests

Discover the repository's actual device-test tasks before invoking them:

```bash
find app -path '*androidTest*' -type f -print
./gradlew tasks --all | rg -i 'androidTest|connected'
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
```

The QA harness runs `:app:connectedDebugAndroidTest` only when `app/src/**/androidTest/` contains test sources. Without such sources it records the device-test result as `NOT RUN` rather than reporting a fabricated pass.

## Troubleshooting

- If `adb`, `emulator`, and `sdkmanager` resolve to different roots, fix `PATH` and make `ANDROID_HOME` and `ANDROID_SDK_ROOT` identical.
- If the emulator does not boot, inspect the run's `emulator.log`, confirm an ARM64 image on Apple silicon, and check free disk and memory.
- On an 8 GiB host, close memory-heavy applications and use `--headless`; Android Emulator may warn that 16 GiB is preferred.
- If installation targets the wrong device, stop and pass the intended emulator serial through `ANDROID_SERIAL` or use the QA harness.
- Treat a non-empty crash buffer, a failed foreground-state check, or failed device tests as a real failure requiring investigation.
