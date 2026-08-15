# Source Baseline

- Generated: 2026-08-15
- Desktop product: Venice Forge `3.0.0-beta.2`
- Attached archive: `Venice_Forge-main.zip`
- Remote: `spearchucker667/Venice_Forge` / `main`
- Latest remote commit observed during kickoff: `bc5c17374ef4937f5837f5580d29a88bfab333ee`
- Venice API upstream commit recorded by desktop: `db3b9f4f40fe71abff2011bcaa9c23ad797c94f3`
- Venice schema version: `20260814.153445`
- Canonical Android feature IDs: 22, from desktop `src/config/tabs.ts`

## Android toolchain selected

- AGP 9.3.0
- Gradle 9.5.0
- Kotlin / Compose compiler plugin 2.3.21
- compileSdk / targetSdk 37
- minSdk 26
- Compose BOM 2026.06.00
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- DataStore 1.2.1
- WorkManager 2.11.2
- Media3 1.10.1
- OkHttp 5.3.0
- kotlinx.coroutines 1.11.0
- kotlinx.serialization JSON 1.11.0

The dependency set intentionally favors stable releases. Re-verify versions before each release branch; do not use dynamic version selectors.

## Runtime desktop source resolution

The SHA above is the kickoff baseline, not a permanent pin. Each agent session must refresh the read-only desktop mirror from `https://github.com/spearchucker667/Venice_Forge.git` `main` using `scripts/bootstrap-desktop-source.sh`, then record/use the resulting `VENICE_FORGE_DESKTOP_HEAD` from `.local/desktop-source.env`. Current desktop `origin/main` wins over this historical kickoff SHA for parity behavior.

Expected local mirror: `/Users/super_user/Projects/Venice Fyr/.source/Venice_Forge-desktop/`.
