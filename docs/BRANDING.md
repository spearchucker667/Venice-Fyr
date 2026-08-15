# Venice Fyr Branding & Visual Asset Policy

This document defines the branding standards, color tokens, typography policies, launcher icon rules, and animation asset contracts for **Venice Fyr**.

---

## 1. Source Authority & Official Venice Brand References

Venice Fyr adopts the visual language of the official Venice.ai brand identity while maintaining a distinct product identity.

### Canonical Brand Sources
- **Live Brand Guidelines & Assets**: [venice.ai/brand](https://venice.ai/brand)
- **Local Machine-Readable Brand Kit**: [`docs/assets/venice-brand-kit/DESIGN.md`](../docs/assets/venice-brand-kit/DESIGN.md)
- **Official Brand Guidelines Document**: `docs/assets/venice-brand-kit/venice-brand-guidelines.pdf`
- **Integrator Attribution**: [builtinvenice.ai](https://builtinvenice.ai)

---

## 2. Product Identity vs. Corporate Venice Identity

Venice Fyr is an independent native Android client and reusable SDK built for Venice AI.

### Non-Negotiable Trademark & Logo Directives
1. **Never Redraw or Modify Official Marks**: Never alter vector paths, anchor points, proportions, curves, or kerning of the official Venice wordmark, crossed keys, or lockup.
2. **Never Create Hybrid Logos**: Do not fuse a lighthouse, beacon, Android robot, or the word "Fyr" into the official Venice crossed keys or wordmark.
3. **Approved Official Mark Colors**: The official Venice marks may only appear in **Deep Blue (`#0E2942`)** on light surfaces or **Off White (`#F7F5ED`)** on dark surfaces. They are never rendered in Venetian Blue or secondary accents.
4. **Third-Party Attribution & Badging**: Third-party integration surfaces and in-app settings utilize the official **Built in Venice** badge and official Venice lockup for attribution.

---

## 3. Official Venice Palette & Design Tokens

Design tokens are centralized in `:core:designsystem` under `VeniceColors.kt`.

### Primary Accent: Venetian Blue
Used strictly as a focused interactive accent (buttons, active states, focus outlines, selected navigation items). Never used as a full-bleed background or logo ink.

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| Venetian Blue | `#3C8FDD` | `#125DA3` | Interactive CTA, selected tab, focus ring, accent highlight |

### Foundations
| Token | Hex Code | Role |
|---|---|---|
| Deep Blue | `#0E2942` | Primary dark ink, text, and official logo on light backgrounds |
| Midnight Blue | `#0A121A` | Primary dark mode background & system bar foundation |
| Off White | `#F7F5ED` | Primary light mode background & primary light ink on dark surfaces |

### Secondary Accents
| Token | Hex Code | Role |
|---|---|---|
| Sea Light | `#B3D0EB` | Quiet accent, container outline in dark mode, subtle highlights |
| Mid Sea | `#29526C` | Surface containers, secondary actions, structural elements |
| Sea Dark | `#080F16` | Deep shadow accents |
| Stucco Light | `#6E6B5F` | Subtitle text, muted labels on light surfaces |
| Stucco Mid | `#BEA989` | Warm secondary tone |
| Stucco Dark | `#1C1714` | Warm dark foundation |
| Neutral Light | `#FFFEFA` | Light mode surface cards |
| Neutral Gray | `#6E7176` | Outlines, subtle borders, inactive indicators |
| Neutral Dark | `#151F28` | Dark mode surface cards & secondary containers |

---

## 4. Typography Policy & Licensing Status

Official Venice typography references:
- **Display / Headlines**: Canela
- **Supporting Sans / Body / Interface**: Aeonik
- **Mono / Technical / Data**: Aeonik Fono

### Licensing & Implementation Gate
- Commercial font binaries for Canela, Aeonik, and Aeonik Fono are proprietary and not bundled without verified licensing.
- `:core:designsystem` provides `VeniceTypography.kt` configuring the exact scale, weights, and line-heights specified in brand guidance, backed by system font fallbacks (`Serif`, `SansSerif`, `Monospace`).
- When licensed font binaries are acquired, they may be placed into font resources and attached to `VeniceTypography` without breaking existing UI contracts.

---

## 5. Android Launcher Icon & Adaptive Icons

To prevent misleading users into believing the application is an official Venice-distributed APK while preserving brand cohesion:

1. **Venice Fyr Product Launcher Icon**: A custom beacon ("Fyr") geometric mark designed in the official Venice palette (`#0A121A`, `#151F28`, `#29526C`, `#3C8FDD`, `#F7F5ED`).
2. **Adaptive Icon Layers**:
   - Background: `app/src/main/res/drawable/ic_launcher_background.xml` (Midnight Blue `#0A121A`)
   - Foreground: `app/src/main/res/drawable/ic_launcher_foreground.xml` (Beacon & beam within 66dp safe zone)
   - Monochrome: `app/src/main/res/drawable/ic_launcher_monochrome.xml` (Android 13+ themed icon layer)
3. **Official Venice Presence**: In-app Navigation Drawer and Settings/Config screens feature the unmodified official Venice crossed keys (`app/src/main/res/drawable/ic_venice_keys_off_white.xml` / `ic_venice_keys_deep_blue.xml`) in approved ink (`#F7F5ED` / `#0E2942`) with theme-aware contrast.

---

## 6. Repository Branding & Store Assets

The repository landing page, documentation, and app store listings use brand-compliant artwork:

| Asset Path | Format / Dimensions | Description |
|---|---|---|
| `docs/assets/venice-fyr-banner.png` | PNG (1600x480) | README hero banner with Venice Fyr typography and beacon motif |
| `docs/assets/venice-fyr-banner.svg` | SVG (1600x480) | Scalable vector source (pure vector, no raster wrapper) |
| `docs/assets/venice-fyr-social-preview.png` | PNG (1280x640) | GitHub social sharing preview card |
| `docs/assets/venice-fyr-mark.png` | PNG (512x512) | Square product mark |
| `docs/assets/venice-fyr-mark.svg` | SVG (512x512) | Scalable square product mark |
| `docs/assets/store/google-play/icon-512.png` | PNG (512x512) | Google Play Store launcher asset matching Beacon v2 identity |
| `docs/assets/store/google-play/feature-graphic-1024x500.png` | PNG (1024x500) | Google Play Store promotional feature banner |
| `docs/assets/web/favicon-*.png` | PNG (16-512px) | Web and documentation favicon set |

### Product Source Vectors

The following canonical vector sources are retained for the product beacon mark and Android launcher icon layers:

| Asset Path | Description |
|---|---|
| `docs/assets/product-source/venice-fyr-beacon-v2.svg` | Full-color beacon mark source |
| `docs/assets/product-source/venice-fyr-beacon-monochrome.svg` | Monochrome beacon mark source (Android 13+ themed icon layer) |

---

## 7. Codex Pet v2 Loading & Status Animation Contract

The loading and status indicator system is powered by the supplied `ayanami-rei` Codex Pet v2 spritesheet.

### Atlas Specification
- **File**: `core/designsystem/src/main/res/drawable-nodpi/ayanami_rei_spritesheet.webp`
- **Source Package**: `docs/assets/ayanami-rei.codex-pet/`
- **Atlas Dimensions**: `1536 x 2288` px
- **Grid Layout**: `8` columns x `11` rows
- **Frame Cell Size**: `192 x 208` px (fixed-cell contract)

### Semantic State Mapping
| State | Row (0-indexed) | Frame Count | Default FPS | Semantic Application |
|---|---|---|---|---|
| `ActiveTask` | Row 7 | 6 frames | 8 fps | Active AI generation, model loading, network requests |
| `Waiting` | Row 6 | 6 frames | 6 fps | Awaiting user input, confirmation, or approval |
| `Failed` | Row 5 | 8 frames | 8 fps | Error state, request failure, blocked action |
| `Review` | Row 8 | 6 frames | 6 fps | Verification, schema checking, post-processing |
| `Waving` | Row 3 | 4 frames | 6 fps | Friendly greeting, empty state introduction |
| `Idle` | Row 0 | 6 frames | 4 fps | Default idle status |

### Renderer Architecture & Accessibility
- **Component**: `CodexPetRenderer`, `VeniceLoadingIndicator`, `VenicePetStatusIndicator` in `:core:designsystem`.
- **Memory Management**: The spritesheet bitmap is decoded once and cached; frame updates use sub-rectangle canvas drawing without per-frame allocations.
- **Reduced Motion**: When system animations are reduced/disabled or `reducedMotion = true`, the renderer displays a static frame (column 0).
- **Accessibility**: Screen readers announce status messages via semantic text rather than on every frame animation tick.

### Intellectual Property & Redistribution Notice
The supplied character spritesheet depicts `Ayanami Rei`. Redistribution rights must be verified by the repository maintainer prior to publishing binary packages to public app stores. The runtime animation abstraction is fully decoupled to allow swapping alternative sprite assets seamlessly.
