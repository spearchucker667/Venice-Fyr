# Venice Fyr Brand Asset Pack

Generated for `spearchucker667/Venice-Fyr` from the repository's existing product-brand policy and Venice's approved brand sources. Source HEAD inspected: `02ae314cd2068f1a2a3fe7bd7d500aaf5195ea37`. Re-check the repository HEAD before integration.

## What is in this pack

- A refined **Venice Fyr Beacon v2** product mark and a complete Android launcher export set.
- Android adaptive-icon XML, legacy density PNGs, round icon PNGs, and Android 13+ monochrome/themed icon support.
- Exact-geometry official Venice crossed-keys assets in the approved Deep Blue and Off White inks.
- Android vector resources for the official keys for in-app attribution surfaces.
- Light/dark splash candidates that keep the product mark primary and the Venice keys as a separate attribution element.
- Google Play icon + feature graphic, GitHub/README artwork, web favicons, official palette tokens, and a visual preview.
- `AGENT_HANDOFF_GEMINI_3_7_FLASH.md` with a review-first integration workflow.

## Brand architecture

The pack intentionally separates two identities:

1. **Venice Fyr product identity** — the custom beacon/lighthouse mark is used for launcher icons, store graphics, favicons, and product-level surfaces.
2. **Official Venice corporate identity** — the crossed keys are supplied unchanged for attribution. The existing repository's official wordmark, lockup, and brand-kit files remain the canonical sources for those marks.

This matches the repository policy that Venice Fyr is an independent Android client/SDK and should not present its APK as an official Venice-distributed application.

## Important constraints

- Do not recolor an official Venice mark with Venetian Blue. Venetian Blue is an accent.
- Do not alter official Venice curves, paths, proportions, opacity, kerning, or arrangement.
- Do not fuse the Venice keys or wordmark into the Venice Fyr beacon.
- Do not bundle proprietary Canela, Aeonik, or Aeonik Fono font files unless licensing is independently verified.
- The live official Venice Brand Kit remains authoritative if it changes after this pack was generated.

## Recommended integration targets

Copy from `android/res/` only after reviewing the repo's current resources. The paths mirror `app/src/main/res/` so they can be compared directly before replacement.

For official in-app wordmark/lockup/badge usage, source the exact approved files already present under `docs/assets/venice-brand-kit/` or refresh them from the official Venice Brand Kit. Do not synthesize a missing official badge.

See `AGENT_HANDOFF_GEMINI_3_7_FLASH.md` for the full integration procedure.
