# Gemini 3.7 Flash Agent Handoff — Venice Fyr Brand Asset Integration

## Role

You are **Gemini 3.7 Flash** acting as the implementation engineer for the Venice Fyr Android repository. Integrate the supplied brand asset pack carefully. You are not a blind file copier. Repository evidence and the current official Venice brand kit are authoritative.

## Objective

Apply the reviewed assets from this package to `spearchucker667/Venice-Fyr` so the Android app has a complete, production-ready visual asset system while preserving:

- Venice Fyr's independent product identity;
- the exact official Venice corporate marks;
- the official Venice color system;
- existing Android adaptive/themed icon behavior;
- repository security, build, and documentation contracts.

The target result should feel visually native to Venice without falsely presenting Venice Fyr as an official Venice-distributed APK.

## Repository and source discovery

Primary repo:

```text
https://github.com/spearchucker667/Venice-Fyr
```

Pack-generation snapshot inspected: `02ae314cd2068f1a2a3fe7bd7d500aaf5195ea37`. Treat this only as provenance; always re-check the live/local HEAD before editing.

Before editing, determine the actual local checkout path. Then run read-only discovery first:

```bash
pwd
git status --short --branch
git rev-parse --show-toplevel
git rev-parse HEAD
find app/src/main/res -maxdepth 2 -type f | sort
find core/designsystem -maxdepth 6 -type f | sort
```

Read these files before changing anything:

```text
AGENTS.md
docs/BRANDING.md
docs/DEVELOPMENT_GUIDE.md
docs/assets/venice-brand-kit/DESIGN.md
app/src/main/AndroidManifest.xml
app/src/main/res/drawable/ic_launcher_background.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/drawable/ic_launcher_monochrome.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceColors.kt
core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceForgeTheme.kt
core/designsystem/src/main/java/io/github/spearchucker667/veniceforge/core/designsystem/VeniceTypography.kt
```

Also inspect the current UI surfaces that own About, Settings/Config, model discovery, onboarding/splash, and any existing brand attribution. Do not assume filenames; find them from the current tree.

## Brand sources of truth

Use this hierarchy:

1. Current repo `docs/BRANDING.md`.
2. Current repo `docs/assets/venice-brand-kit/DESIGN.md` and the exact official logo files beside it.
3. Current live official Venice Brand Kit at `https://venice.ai/brand` if the local kit is stale or missing an asset.
4. Existing `VeniceColors.kt` for implemented Compose color tokens.
5. This asset package for product-specific exports and placement-ready derivatives.

If these sources conflict, stop the conflicting integration step, document the conflict, and prefer the newer explicit maintainer/repository contract. Do not improvise an official logo.

## Non-negotiable logo rules

Official Venice wordmark, keys, lockup, and Built in Venice artwork are provider-owned marks.

Do not:

- redraw or trace them;
- change vector paths, anchor points, curves, proportions, kerning, or arrangement;
- add shadows, outlines, strokes, gradients, transparency, or blend effects to them;
- recolor them with Venetian Blue;
- merge the keys/wordmark with the Venice Fyr beacon, Android robot, lighthouse, or the word `Fyr`;
- create a new fake `Built in Venice` badge if the official badge is missing.

Approved official logo ink is Deep Blue `#0E2942` on light surfaces or Off White `#F7F5ED` on dark surfaces. Venetian Blue is an interactive/accent color, not logo ink.

## Product identity rule

The supplied `Venice Fyr Beacon v2` is the **product launcher/store mark**. Keep it distinct from the official Venice keys.

Do not replace the launcher with the official Venice keys unless the maintainer explicitly changes the repo's current `docs/BRANDING.md` product-identity policy. The existing policy deliberately distinguishes an independent Venice-powered client from an official Venice-distributed APK.

## Package review before integration

1. Verify package hashes:

```bash
shasum -a 256 -c SHA256SUMS.txt
```

2. Review `PREVIEW.png`, `README.md`, `SOURCE_ATTRIBUTION.md`, and `MANIFEST.json`.
3. Compare every candidate Android resource with the current target resource before replacing it.
4. Keep a short integration ledger: source asset, destination path, whether it replaced an existing file, and why.
5. Preserve unrelated user changes. Never run `git reset --hard`, `git clean -fd`, forced checkout, or destructive restoration.

## Phase 1 — Android launcher assets

Candidate package resources live under:

```text
android/res/
```

They mirror the target resource names under:

```text
app/src/main/res/
```

Review and then integrate as appropriate:

```text
android/res/drawable/ic_launcher_background.xml
android/res/drawable/ic_launcher_foreground.xml
android/res/drawable/ic_launcher_monochrome.xml
android/res/mipmap-anydpi-v26/ic_launcher.xml
android/res/mipmap-anydpi-v26/ic_launcher_round.xml
android/res/mipmap-mdpi/ic_launcher.png
android/res/mipmap-hdpi/ic_launcher.png
android/res/mipmap-xhdpi/ic_launcher.png
android/res/mipmap-xxhdpi/ic_launcher.png
android/res/mipmap-xxxhdpi/ic_launcher.png
[matching ic_launcher_round.png files]
```

The current manifest already refers to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`; preserve those stable resource names unless repository evidence requires otherwise.

Validation requirements:

- no clipping across circle, squircle, rounded-square, and teardrop masks;
- foreground remains within adaptive icon safe area;
- Android 13+ themed/monochrome icon renders as one solid silhouette;
- no low-contrast edges against light/dark launchers;
- Play Store `512x512` icon matches the installed launcher identity.

## Phase 2 — Official Venice in-app attribution

The package provides exact crossed-key derivatives in:

```text
official-venice/keys/
android/res/drawable/ic_venice_keys_off_white.xml
android/res/drawable/ic_venice_keys_deep_blue.xml
```

Use these only as official Venice attribution, not as the Venice Fyr product mark.

For wordmark, full lockup, or `Built in Venice`, prefer the exact files already in:

```text
docs/assets/venice-brand-kit/logos/
```

The current repo snapshot contains approved wordmark, keys, and lockup assets. The repo branding policy references `Built in Venice`, but the checked tree may not contain that badge. If it is absent, verify the current official `https://venice.ai/brand` download and add the exact current badge asset; do not synthesize it.

Recommended in-app placement:

- About / Legal / Attribution: official Venice lockup or Built in Venice badge.
- Settings / provider configuration: official Venice wordmark or Built in Venice badge near provider identity.
- Model discovery/provider header: compact official Venice mark where space permits.
- Onboarding/splash: product identity remains primary; official Venice attribution may appear separately with generous clear space.

For all Compose `Image`/`Icon` usages:

- supply an accurate localized `contentDescription` when meaningful;
- use `contentDescription = null` only when the adjacent text already exposes identical semantics;
- do not tint official raster/logo assets through Compose;
- do not apply alpha to the official logo container or image;
- keep size/clear-space rules intact at compact breakpoints.

## Phase 3 — Store, docs, and web assets

Review and apply only where the repository actually uses them:

```text
store/google-play/icon-512.png
store/google-play/feature-graphic-1024x500.png
docs/readme/venice-fyr-banner-v2-1600x480.png
docs/github/venice-fyr-social-preview-v2-1280x640.png
web/favicon-*.png
```

If replacing README or GitHub assets:

- update only references that point to the replaced files;
- retain accessibility alt text;
- keep filenames stable when that avoids documentation churn;
- do not delete canonical official Venice brand-kit source files.

The GitHub social preview is configured outside Markdown in repository settings; document the final file intended for upload rather than claiming it changed automatically.

## Phase 4 — Color and typography alignment

Do not duplicate a second competing palette if `VeniceColors.kt` already contains the same official tokens. `tokens/` exists for reference and Android resource interoperability.

Verify these core values remain exact:

```text
Venetian Blue (light) #3C8FDD
Venetian Blue (dark)  #125DA3
Deep Blue             #0E2942
Midnight Blue         #0A121A
Off White             #F7F5ED
Sea Light             #B3D0EB
Mid Sea               #29526C
Sea Dark              #080F16
Stucco Light          #6E6B5F
Stucco Mid            #BEA989
Stucco Dark           #1C1714
Neutral Light         #FFFEFA
Neutral Gray          #6E7176
Neutral Dark          #151F28
```

Do not bundle or download proprietary Canela, Aeonik, or Aeonik Fono font binaries unless the maintainer has independently verified redistribution rights. Preserve the repository's existing fallback typography behavior otherwise.

## Phase 5 — Documentation updates

After integration, update documentation to match implemented state only. At minimum review:

```text
docs/BRANDING.md
README.md
docs/USER_GUIDE.md
```

If launcher assets changed, update the branding doc's launcher description and asset paths. If official attribution locations changed, document the actual screens/resources used. Do not describe planned placements as implemented.

## Validation

Use repository-defined validation commands from `docs/DEVELOPMENT_GUIDE.md`:

```bash
./gradlew test
./gradlew lint
./gradlew :app:assembleDebug
./gradlew :venice-sdk:assembleRelease
```

For this visual-only change, prioritize `./gradlew lint` and `./gradlew :app:assembleDebug`; run the full baseline before claiming the work complete if practical.

Also inspect the generated APK/application manually on at least:

- dark theme;
- light theme;
- Android themed icons enabled;
- one circular launcher mask;
- one rounded-square/squircle launcher mask;
- compact phone width;
- large font/display scaling for About/Settings attribution surfaces.

Do not claim any validation passed unless you actually ran it successfully.

## Required final report

Return:

1. Git commit/HEAD reviewed.
2. Pre-edit Git status and whether unrelated changes were present.
3. Exact files changed/added/removed.
4. Asset mapping table: package source -> repo destination -> purpose.
5. Brand-policy decisions, including any skipped asset and why.
6. Validation commands run with PASS/FAIL/NOT RUN.
7. Screens/surfaces where official Venice attribution is now present.
8. Any remaining trademark/licensing or asset-source uncertainty.
9. Any follow-up that requires maintainer action, such as GitHub social preview upload.

## Acceptance criteria

The task is complete only when:

- launcher resources compile and render correctly across adaptive/themed modes;
- product launcher identity remains distinct from official Venice corporate marks unless the maintainer explicitly changed that policy;
- official Venice logos used in-app are exact approved files with approved ink and no effects;
- no proprietary fonts were redistributed without verified licensing;
- the existing Venice color tokens remain authoritative and are not duplicated inconsistently;
- documentation describes the actual implemented state;
- unrelated working-tree changes were preserved;
- build/lint status is reported truthfully.
