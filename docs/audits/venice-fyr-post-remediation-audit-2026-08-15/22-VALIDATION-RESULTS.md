# Validation Results

Environment: Apple silicon macOS 27.0; Homebrew OpenJDK 17.0.20; Gradle 9.5.0; Android SDK at `/opt/homebrew/share/android-commandlinetools`. Commands ran from repository root on local `main`.

| Command | Status | Exit | Observed result |
|---|---|---:|---|
| `./gradlew --version` | VERIFIED | 0 | Gradle 9.5.0, Kotlin 2.3.20, JVM 17.0.20, macOS aarch64. |
| `./gradlew test` | VERIFIED | 0 | `BUILD SUCCESSFUL`; 139 actionable tasks, 4 executed and 135 up-to-date. |
| `./gradlew lint` | VERIFIED | 0 | `BUILD SUCCESSFUL`; 218 actionable tasks, 25 executed and 193 up-to-date; app HTML/SARIF reports produced. |
| `./gradlew :app:assembleDebug` | VERIFIED | 0 | `BUILD SUCCESSFUL`; debug APK produced. |
| `./gradlew :venice-sdk:assembleRelease` | VERIFIED | 0 | `BUILD SUCCESSFUL`; release AAR produced. |
| `./gradlew :app:assembleRelease` | VERIFIED | 0 | `BUILD SUCCESSFUL`; unsigned release APK produced after R8/lint-vital. |
| `./gradlew test lint :app:assembleDebug :venice-sdk:assembleRelease :app:assembleRelease` | VERIFIED | 0 | Final exact-source gate: `BUILD SUCCESSFUL` in 1m 22s; 506 actionable tasks. |
| Focused SDK test command below | VERIFIED | 0 | Image, video, audio, chat/reasoning, capability discovery/defaults, HTTP 402, and cancellation cases passed. |
| `adb devices -l` | BLOCKED | 0 | ADB ran, but no emulator or physical device was attached; instrumentation/runtime claims are not made. |
| Live authenticated/paid Venice calls | NOT RUN | — | No credential or spending authorization was used; contract validation used the bootstrapped authoritative sources and deterministic transport tests. |

Focused command:

```bash
./gradlew :venice-sdk:testDebugUnitTest \
  --tests '*ImageClientTest' \
  --tests '*VideoClientTest' \
  --tests '*AudioClientTest' \
  --tests '*ChatClientTest' \
  --tests '*ChatStreamAccumulatorTest' \
  --tests '*VeniceParametersSerializationTest' \
  --tests '*CapabilitiesRepositoryTest' \
  --tests '*VeniceForgeSdkTest'
```

Artifact evidence:

| Artifact | SHA-256 |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | `6b9a9d650709f4d140dc8ae2afbd181d03d3d669fbd12ff11bd2b1f1dbd96dcf` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | `c6b54d14bf20540bb706800b72808faeee4db3115ce85f6eccd349907c20448b` |
| `venice-sdk/build/outputs/aar/venice-sdk-release.aar` | `059d1812c2b7a91f4057e78cb9c8570a6c9ee6a5be0d4ac3f005c9ab16c18847` |

The first attempted orchestration was discarded as evidence because overlapping Gradle wrappers were launched. Only the serial commands and exit statuses above are authoritative.
