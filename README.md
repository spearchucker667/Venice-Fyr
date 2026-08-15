# Venice Forge Android — Starter

Native Android port foundation for Venice Forge `3.0.0-beta.2`.

This repository intentionally produces two deliverables:

1. `:app` — installable Venice Forge Android client (`APK` / `AAB`).
2. `:venice-sdk` — reusable Android library (`AAR`) containing Venice API contracts/client code that must not own persistent credentials.

The attached Electron archive and the tracked Venice API snapshot are the behavioral source of truth. This starter is **not a completed port**; it is a compilable project architecture, secure credential/network foundation, complete feature navigation registry, endpoint catalog, and implementation contract for achieving desktop parity without importing Electron assumptions into Android.

## Baseline captured for this starter

- Desktop app: Venice Forge `3.0.0-beta.2`
- Desktop canonical tabs: 22
- Desktop source archive inspected: `Venice_Forge-main.zip`
- Remote repository: `spearchucker667/Venice_Forge`, branch `main`
- Latest remote commit observed: `bc5c17374ef4937f5837f5580d29a88bfab333ee` (Media Studio capability-routing hardening)
- Venice API upstream snapshot recorded by desktop: commit `db3b9f4f40fe71abff2011bcaa9c23ad797c94f3`, retrieved `2026-08-14`
- Venice OpenAPI schema version: `20260814.153445`

## First run: download the desktop source of truth

The Android package is the implementation target. The current desktop app is intentionally kept as a separate read-only clone so an agent can inspect the real implementation and tests without contaminating the Android repository.

```bash
cd "/Users/super_user/Projects/Venice Fyr"
./scripts/bootstrap-desktop-source.sh
```

This creates/refreshes:

```text
/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/
```

from:

```text
https://github.com/spearchucker667/Venice_Forge.git
```

The exact source path, branch, remote, and commit SHA are written to the ignored `.local/desktop-source.env`. See `docs/DESKTOP_SOURCE_BOOTSTRAP.md` for the source precedence and feature-by-feature source map.

## Current Android foundation

- Kotlin + Jetpack Compose
- `compileSdk` / `targetSdk`: 37
- `minSdk`: 26
- Android Gradle Plugin: 9.3.0
- Gradle: 9.5.0
- Compose BOM: 2026.06.00
- OkHttp: 5.3.0
- HTTPS-only manifest/network security policy
- Android Keystore AES-GCM API-key persistence in app layer
- Venice SDK does not persist credentials
- Live `GET /models?type=all` probe from `:venice-sdk`
- Full 22-feature navigation catalog preserving desktop stable IDs
- No telemetry
- Central redaction helper for secrets/local paths

## Open in Android Studio

Use Android Studio Quail 2 (2026.1.2) or another version supporting AGP 9.3. Install Android SDK Platform 37 and JDK 17+.

```bash
./gradlew :app:assembleDebug
./gradlew :venice-sdk:assembleRelease
```

Expected outputs after a successful build:

```text
app/build/outputs/apk/debug/app-debug.apk
venice-sdk/build/outputs/aar/venice-sdk-release.aar
```

## Project map

```text
app/                 Android application, Compose shell, feature registry
venice-sdk/          Public AAR boundary for Venice API operations
core/common/         Redaction and cross-cutting primitives
core/security/       App-owned Android Keystore secret persistence
core/designsystem/   Compose theme/design-system seed
docs/                Porting contract, parity matrix, architecture/security guidance
```

Do not implement Android features by embedding the Electron renderer in a WebView. Port domain behavior and contracts to native Kotlin/Compose components.

See `ANDROID_PORT_HANDOFF.md` and `docs/FEATURE_PARITY_MATRIX.md` before implementing features.
