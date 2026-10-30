<!-- AudioQualityVote#3 — Brand artwork: favicon SVG set + GitHub OAuth logo -->

# Spec — AudioQualityVote#3

## Intent

The project asked for brand artwork — "artwork for github oauth, favicon (SVG), all
based on modern standards" — and, through an in-session design exploration the
developer approved, converged on a specific mark:

- Cherokee syllabary letter **Ꮳ** (U+13E3) as the identity figure (developer: "item 3"
  concept; iterated to "waves in the background with the ja as a cutout", then the
  final construction) rendered within an audio-wave field ("waves as in expanding
  from a speaker", refined to "remove speaker … waves start from the bottom left
  'off canvas', we should only be seeing the rings heading north, north east, east").
- Material palette continuity (developer: "keep with the existing material design
  palette") — the app inherits `gwt-material-themes` `ThemeBlue`.
- Modern-browser favicon with SVG craft as the deliverable of consequence
  (developer: "assume modern everything for the favicon, SVG quality is the
  important part").
- Final approved letter treatment: "just do a ja outlined … fill the icon canvas
  space" + "fill in the ja with a color somewhat darker than the ring coloration,
  leave the white border."
- Final approval: the developer approved the delivered construction
  (this session's sheet-14 render) and authorized the pipeline ("approved. proceed.
  create specs as needed.").

## The approved mark (normative geometry, 512-space)

- **Tile**: rounded square, inner rect (6,6,500,500), rx 104.
  - Light mode: plate `#64B5F6` (Material blue-300 — the theme's `$primary-color`,
    `color("blue","lighten-2")`, verified in the gwt-material-themes source).
  - Dark mode: plate `#1565C0` (blue-800).
- **Wave field**: white `#FFFFFF` ring arcs, center off-canvas bottom-left at
  (30, 630) — only arcs expanding north / northeast / east enter the tile. Radii
  175→755 step 46, stroke 22. No origin disc, no wrap-around arcs, no stray ends
  (verified: vision-QA round 6, all-variant PASS on wave field).
- **Ꮳ**: from Noto Sans Cherokee's U+13E3 outline (verified as canonical against
  Code2000's rendering of the same glyph — two independent typefaces agree). Height
  430/512, centered (256, 256) — spans ≈ y41..471, x67..445 (developper: "fill the
  icon canvas space").
  - Band: filled with the tile's next-darker step + dilated (`fill`+`stroke` of the
    fill color, width 10 → band ≈ +5px/side; keeps the serif-bar/notch slit ≥12px).
    Light: `#1976D2` (blue-700). Dark: `#0D47A1` (blue-900). (The approved round-7
    winner: light fill `#1976D2` — QA round 6 rejected `#1E88E5` as the weakest
    figure/ground, and `#1976D2` light beat the field in both modes.)
  - **White border**: the glyph outline stroked solid white, 18px, on top
    (developer: "leave the white border"). QA round 6 rejected the 24px border
    (−24% fill area, crossbar breakup risk at launcher sizes) — 18px is normative.
- **Render mechanics**: pure paint layers + single `clipPath` — no `<mask>`, no
  `fill-rule` compositing. Verified rendering in real Chromium (playwright + vision
  QA rounds 6–7: zero fill↔field adjacency, knockout clean, border continuous,
  geometry parity across modes). Verified constraint this honors: Chromium renders
  favicons in SVG secure static mode (no scripts/external loads; masks risky).
- **Layer order** (normative): tile → ring field → letter fill+dilation → white
  border.

## Verified research constraints carried into requirements

- GitHub OAuth logo upload: **PNG/JPG/GIF only, <1 MB, ≥200×200**; rendered as a
  square image inside a circular badge — SVG is not accepted there
  (docs.github.com OAuth-app custom-badge doc, verified live 2026-10-29).
- SVG favicon support: Chrome 80+, Firefox 41+; **Safari 26.0+ (Sept 2025)**;
  ICO/PNG fallbacks still required (Safari < 26, Google Search favicons reject SVG,
  stock Android Browser) (caniuse `link-icon-svg` — 92.06% global support).
- `prefers-color-scheme` inside the SVG: honored in Chrome/Firefox/Edge; broken in
  Safari (WebKit bug #309949) → **default variant must be theme-neutral** (our
  default is the light variant, QA-verified readable on both tab backgrounds).
- apple-touch-icon: PNG, 180×180, opaque (Apple Safari Web Content Configuring guide).
- PWA maskable: separate `purpose: "maskable"` entries at 192 and 512; safe zone =
  inner 80% circle; `"any maskable"` deprecated (web.dev maskable-icon; MDN
  manifest icons; Chrome DevTools warning thread).
- Chromium favicon: `sizes="any"` SVG + 48×48 ico pairing so Chromium picks the SVG
  and dark-mode adaptation survives (masakudamatsu guide linked from caniuse notes).
- Deprecated rels to avoid: `shortcut icon`, `alternate icon` (same source).

## Success criteria

| ID | Criterion | Class | Instrument |
|----|-----------|-------|------------|
| SC-1 | `favicon.svg` renders the approved mark: ring field from off-canvas BL source (N/NE/E arcs), Ꮳ at 430/512 with 18px white border and per-mode darker fill, tile rx104 | Behavioral | Playwright screenshot of the delivered file rendered at 512; fresh-context vision-QA verdict matching the approved construction (round-7 geometry) |
| SC-2 | Default (light) variant reads on both light (#fff) and dark (#0d1117) tab backgrounds; forcing `prefers-color-scheme: dark` in Chromium switches plate to `#1565C0` and letter fill to `#0D47A1` | Behavioral | Two Chromium render states + vision-QA verdict on each |
| SC-3 | The SVG is self-contained: no external references of any kind (no `<image>`, no `<script>`, no font references; every `url(` occurrence is an in-document `url(#fragment)`; the only permitted `http` string is the mandatory XML namespace declarations, `xmlns="http://www.w3.org/2000/svg"` and `xmlns:xlink`) | Structural | Scripted check on the delivered file: occurrences of `http` outside `xmlns`/`xmlns:xlink` declarations = 0; `grep -E '<image|<script|@font-face|<text|<tspan|font-family'` empty; count(`url(`) == count(`url(#`) |
| SC-4 | Raster set from the approved artwork exists with exact specs: `favicon.ico` (16/32/48), `apple-touch-icon.png` (180×180 opaque PNG), `site.webmanifest` icon entries (192+512 purpose `any`; 192+512 purpose `maskable`, PNG), `oauth-logo.png` (512×512 PNG, <1 MB) | Structural | `magick identify` on each raster + parse `site.webmanifest` JSON + `stat` sizes |
| SC-5 | `index.html` carries the modern link set — `rel=icon` svg `sizes="any"` + `rel=icon` ico 48 + `rel=apple-touch-icon` + `rel=manifest` — and no `shortcut icon`/`alternate icon` rels | Structural | grep the delivered `index.html` |
| SC-6 | `oauth-logo.png`: the letter's ink extent (fill + its white border, the outermost ink) lies within a concentric r240 circle of its 512 canvas, so GitHub's circular badge crop never clips the Ꮳ | Structural | Scripted checks on the delivered PNG: (a) pixels matching the letter fill `#1976D2` all have radial distance from center ≤ 240; (b) RGB per-channel normalized RMSE over all pixels vs. a reference render of the approved geometry at the stated 0.80 scale, light-mode colors, ≤ 0.02 — binding the raster (including border ink) to that construction |
| SC-7 | Maskable rasters (`icon-maskable-192/512.png`): plate full-bleed and fully opaque edge-to-edge (white ring arcs may legitimately cross the edge — adaptive masks crop them; there is no transparency at the edge); the letter's ink extent lies within the inner-80% circle (r = 0.4 × canvas side at center); ring field direction preserved (off-canvas BL source, expanding N/NE/E) | Structural | Scripted pixel checks on each delivered PNG: (a) every edge-band pixel fully opaque (alpha = 1; colors = plate or white ring-crossing); (b) pixels matching the letter fill lie within r = 0.4×side; (c) RGB per-channel RMSE normalized to [0,1], computed over all pixels of the delivered PNG vs. a reference render constructed from the approved geometry at the stated maskable scale (0.69, full-bleed-plate construction, centered), ≤ 0.02 (anti-aliasing tolerance), binding the raster to that construction |

## Deliverables

In `audio-quality-vote-ui/src/main/webapp/`:

1. `favicon.svg` — the approved mark (single source of identity).
2. `favicon.ico` — 16/32/48 from the approved artwork.
3. `apple-touch-icon.png` — 180×180, opaque.
4. `icon-192.png`, `icon-512.png`, `icon-maskable-192.png`, `icon-maskable-512.png`.
5. `oauth-logo.png` — 512×512 GitHub-badge export: the full approved construction
   (tile, ring field, letter, border) at light-mode colors (`#64B5F6` plate,
   `#1976D2` fill), scaled ≈0.80 and centered so the letter's ink extent lies
   within a concentric r240 circle — GitHub's circular badge crop never clips the
   Ꮳ; the badge background color is set to `#64B5F6` at upload so any cropped
   corner blends with the plate.
6. `site.webmanifest` — name, theme colors in palette, icon set above.
7. `index.html` — wire the link set in `<head>`.

Raster derivation: direct renders of the approved SVG at each size (the QA
verified 512 and 32 renders; 16 rides the same geometry — the ICO's smallest frame
is a courtesy fallback layer under browser SVG; any legibility loss at 16 does not
block, since every supported-modern browser consumes the SVG and the ICO exists for
legacy/Google-Search coverage).

**Circular-crop-safe derivations (oauth-logo.png; maskable rasters):** the approved
mark's letter at 430/512 has bbox corner radius ≈286 from center and would be
clipped by circle crops (GitHub badge inscribed circle ≈ r256; maskable safe zone
r204.8). These variants therefore regenerate the approved geometry at a reduced
scale, centered, so the **letter's ink extent fits the relevant containment circle**
(scale for oauth: the mark's geometry scaled so the letter lies within a concentric
r240 circle of its 512 canvas — ≈0.80 scale, letter ink-extent corner radius
293 → ≈234; the badge background color is set to the plate color
at upload so any exposed badge area blends with the tile). For maskable variants
the plate paints full-bleed (no rounded corners — masks sacrifice the outer 10%),
and the field + letter are scaled by the exact factor that inscribes the letter's
ink extent in the safe circle (letter ink-extent corner radius ≈293 in the approved
512-space; 0.69 scale → ≈202 ≤ 204.8) about the canvas center. The ring
field's off-canvas-source direction (bottom-left, expanding N/NE/E) is preserved in
both.

## Out of scope

- In-app GWT UI branding/logo placement; "Sign in with GitHub" button artwork;
  OAuth client registration (IDs/secrets); PWA installability beyond manifest icon
  entries; Google OAuth artwork changes; any `index.html` modernization beyond the
  icon/manifest wiring.

## Provenance

- Design source: this session's exploration — developer statements quoted in
  Intent; approval recorded as the deck's finalization statement for the sheet-14
  construction.
- Research cards (live-verified 2026-10-29): GitHub badge spec, SVG favicon
  standards — summarized under "Verified research constraints" with sources.
- Codebase current state verified this session: `index.html` (no icon links), no
  favicon/ico files under `src/main/webapp`, `ThemeBlue` inheritance in
  `AudioQualityVote.gwt.xml`, theme palette from `gwt-material-themes` SASS source.

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)