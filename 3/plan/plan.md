<!-- AudioQualityVote#3 — implementation plan (consumes validated spec .issues/3/spec.md, validation PASS 4th round) -->

# Plan — Brand artwork: favicon SVG set + GitHub OAuth logo

Source: `.issues/3/spec.md` (zero-finding PASS).
No SC is defective against the validation standards (checked per class at planning
time); none is blocked.

## Items

### Item 1 — `favicon.svg` (SC-1, SC-2, SC-3) — depends: none

- Deliverable: `audio-quality-vote-ui/src/main/webapp/favicon.svg`, the approved
  mark per the spec's normative geometry (tile rx104; ring field centered
  (30,630), radii 175..755 step 46, stroke 22; Ꮳ 430/512 centered (256,256),
  fill+dilation then 18px white border; layer order tile→field→fill→border),
  with embedded `@media (prefers-color-scheme: dark)` overriding plate/fill
  (`#1565C0`/`#0D47A1`) and light defaults as the file's baked-in colors.
- RED: `test -f <webapp>/favicon.svg` fails (file absent — verified pre-work).
- GREEN: generate through the session's proven pipeline (fontTools glyph path from
  Noto Sans Cherokee U+13E3 + generator per spec geometry), write the file.
- Verification: (a) scripted SC-3 check (http-outside-xmlns==0,
  `<image|<script|@font-face|<text|<tspan|font-family` empty, url counts equal);
  (b) playwright renders the file at 512 on light and with
  `color-scheme: dark` forced, screenshots to the store's
  `.issues/3/designs/`; fresh-context vision-QA verdict matching SC-1's
  construction and SC-2's two states.

### Item 2 — base rasters (SC-4 core) — depends: Item 1

- Deliverables: `favicon.ico` (16/32/48), `apple-touch-icon.png` (180×180 opaque),
  `icon-192.png`, `icon-512.png` — renders of Item 1's SVG (light variant =
  baked-in colors).
- RED: all four paths fail `test -f` / absent from the webapp.
- GREEN: render from `favicon.svg` (magick; apple-touch composite on plate color
  for opacity).
- Verification: `magick identify` per file: ICO frames 16/32/48; PNG dimensions
  180/192/512; apple-touch has no alpha (opacity check), file sizes sane.

### Item 3 — circular-crop-safe derivations (SC-6, SC-7; SC-4 remainder) — depends: Item 1

- Deliverables: `oauth-logo.png` (approved geometry at 0.80 scale, light colors),
  `icon-maskable-192.png`, `icon-maskable-512.png` (approved geometry at 0.69
  scale, full-bleed plate, light colors).
- RED: paths absent.
- GREEN: generate the scaled variants from the same pipeline (generator
  scale parameter), render.
- Verification (scripted pixel checks, python): SC-6(a) fill pixels ≤ r240,
  SC-6(b) RMSE ≤ 0.02 vs reference render; SC-7(a) edge band uniform plate,
  SC-7(b) fill extent ≤ 0.4×side, SC-7(c) RMSE ≤ 0.02 vs reference render;
  identify dimensions/<1 MB.

### Item 4 — manifest + `index.html` wiring (SC-4's manifest part, SC-5) — depends: Items 2–3

- Deliverables: `site.webmanifest` (name "Audio Quality Vote", icons block with
  192/512 purpose `any` + 192/512 purpose `maskable`, theme colors in palette);
  `index.html` `<head>` gains: svg icon `sizes="any"`, ico 48, apple-touch-icon,
  manifest link.
- RED: no `.webmanifest` in webapp; `index.html` greps empty for
  `rel="icon"/apple-touch/manifest` (verified pre-work).
- GREEN: write manifest + edit `index.html` head only.
- Verification: JSON parse of the manifest; grep checks on `index.html` for the
  four rels present, `shortcut icon`/`alternate icon` absent;
  playwright loads the file:// page and asserts all four `<link>`s resolve (HTTP
  200 equivalents — file existence behind the paths).

## Dependency order

1 → 2 & 3 (parallel-safe, both consume Item 1's SVG) → 4. No cycles.

## Traceability

Item 1 → SC-1, SC-2, SC-3 · Item 2 → SC-4 · Item 3 → SC-4, SC-6, SC-7 ·
Item 4 → SC-4, SC-5. No item lacks an SC; no SC lacks an item.

## Verification evidence

All run-command outputs (screenshots, identify listings, pixel-check JSON,
grep results) are recorded under `.issues/3/` with the work report — behavioral
SCs carry the vision-QA verdict records for SC-1/SC-2.

## Not included

Everything in the spec's Out of scope; no in-app GWT branding, no OAuth
registration work, no index.html modernization beyond the icon/manifest head
wiring.

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)