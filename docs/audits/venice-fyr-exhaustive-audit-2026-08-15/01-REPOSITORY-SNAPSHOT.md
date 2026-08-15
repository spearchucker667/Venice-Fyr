# 01 — Repository Snapshot

Audit date: 2026-08-15. All values captured from read-only commands before any audit output was written.

## Git state

| Item | Value |
|---|---|
| Repository root | `/Users/super_user/Projects/Venice Fyr` |
| Branch | `main` (tracking `origin/main`, no divergence) |
| HEAD | `1da3142 feat: integrate Venice API source-of-truth, Image Studio, and Audio/Video SDK clients` |
| Remote | `origin https://github.com/spearchucker667/Venice-Fyr.git` (fetch/push) |
| Dirty files | None |
| Untracked files | None (`.source/`, `.local/`, `.gradle-bootstrap/` are ignored mirrors/bootstrap caches) |
| Tracked files | 321 total; 72 Kotlin/KTS |

## Environment

| Item | Value |
|---|---|
| OS | macOS (Darwin 27.0.0), arm64 (Apple Silicon) |
| JDK | OpenJDK 17.0.20 (Homebrew `openjdk@17`, `/opt/homebrew/opt/openjdk@17`) — **not on default PATH; `java` unresolvable without `JAVA_HOME` export** |
| Gradle | 9.5.0 (wrapper, `gradle-9.5.0-bin.zip`; bootstrap copy in `.gradle-bootstrap/`) |
| Android Gradle Plugin | 9.3.0 |
| Kotlin | 2.3.21 (Gradle daemon Kotlin 2.3.20) |
| KSP | 2.3.11 |

## Android configuration (`:app`)

| Item | Value |
|---|---|
| Application ID / namespace | `io.github.spearchucker667.veniceforge.android` |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 26 |
| versionCode / versionName | 1 / `0.1.0-alpha.1` |
| Release build type | `isMinifyEnabled = true`, `isShrinkResources = true`, **no signing config, no ProGuard keep rules** |

## Gradle modules

- `:app` — Android application (Compose)
- `:venice-sdk` — Android library (Venice API client, OkHttp + kotlinx.serialization)
- `:core:common` — utilities (`Redactor`)
- `:core:security` — `SecureSecretStore` (Keystore)
- `:core:designsystem` — Compose theme/components
- `:core:data` — Room persistence (conversations, messages, profiles, tool calls)

## Dependency management

Version catalog: `gradle/libs.versions.toml`. Notable: Compose BOM 2026.06.00, OkHttp 5.3.0, kotlinx.coroutines 1.11.0, kotlinx.serialization 1.11.0, Room 2.7.0, Robolectric 4.13, DataStore 1.2.1, WorkManager 2.11.2, Media3 1.10.1 (several declared-but-unused — see `16-CI-RELEASE-AUDIT.md`).

## Venice source-of-truth

| Item | Value |
|---|---|
| Upstream | `https://github.com/veniceai/api-docs.git` (branch `main`) |
| Local mirror | `.source/venice-api-docs/` (ignored, read-only) |
| Upstream HEAD | `6e69346b13695bd53ba33a1d34e7b28841e10f98` (2026-08-15T01:49:10Z) |
| Swagger `info.version` | `20260814.194349` |
| Drift vs `SOURCE_BASELINE.md` | None (CONFIRMED — see `05-VENICE-SOURCE-OF-TRUTH.md`) |
| Desktop parity mirror | `.source/Venice_Forge-desktop/` (ignored, read-only) |
