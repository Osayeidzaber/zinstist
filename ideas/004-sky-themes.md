# 004 — Sky themes and theme picker

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/004-sky-themes-01.png` (Galaxy selected), `assets/004-sky-themes-02.png` (Amethyst selected)
**Related:** [005](005-render-fx-pack.md) shows Afterglow, which appears to use these skies

## What it is

A theme browser for custom skies: one large preview with a title and description, and a horizontal strip
of eight theme thumbnails. The user wants Jilaton's sky module to offer named, art-driven presets chosen
this way instead of a single custom sky.

## Elements observed

- Large rounded preview, roughly 16:9, filling most of the width, with margin around it.
- Title overlaid bottom-left inside the preview (`Galaxy`, then `Amethyst`), bold; a one-line description
  under it: "Nebulae, aurora, shooting stars and a ringed planet." / "Deep violet marble with fine veins of light."
- Eight thumbnails in a row below: **Galaxy, Aether, Nocturne, Amethyst, Opaline, Rain, Snow, Fire**.
  Thumbnails are landscape crops; the selected one has a purple glow border and shows its label under it.
  Labels on the others are cut off by the frame in the screenshots but the layout implies a label per tile.
- A thin scrollbar hint on the right edge under the strip.
- Visual styles differ a lot: Galaxy is illustrated (aurora curtains, ringed planet, stars); Amethyst is a
  marbled violet fluid texture; Rain/Snow/Fire look like photographic real-world skies; Opaline green haze.
- Selections animate — in the first frame Galaxy is active, in the second Amethyst, with the same layout.

## Current state in Jilaton

`module/impl/CustomSkyModule` exists with `CustomSkyMixin` and `CustomCloudMixin`, plus `AmbienceModule`
and a `VisualStudioScreen`. So a custom sky already exists; the gap is multiple named presets, real
artwork, and this browsing UI.

## Likely work areas

- Theme registry: id, display name, description, preview image, sky texture(s), optional fog/cloud colours.
- Picker screen: large preview + thumbnail strip + selection state + scroll, keyboard/mouse selection.
- Asset pipeline: eight preview images plus in-world sky textures; this is the bulk of the work and is
  art, not code.
- Persistence in the client config so the choice survives restart.
- Decide whether the picker lives inside `VisualStudioScreen` or in the ClickGUI.

## Open questions

- Where does the art come from? Original renders, licensed photography, or procedural shaders?
- Are Rain/Snow/Fire only sky textures, or do they also drive weather particles/audio?
- How much memory is acceptable for full-resolution panorama textures?
- Should the picker also set fog, cloud colour and ambient light, or only the sky box?

## Boundaries

- Do not reuse Purixy's theme artwork, names as-is, or descriptions. Names are generic enough to inspire,
  but the images must be original or properly licensed.
- Reference only; no assets extracted from the screenshots.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/004-sky-themes.md` and both images in `ideas/assets/004-sky-themes-*.png`.
>
> Goal: extend `CustomSkyModule` from one custom sky into a set of named themes with a browser screen —
> large preview with title and description, thumbnail strip, selected state, saved to config.
>
> Look at `CustomSkyModule`, `CustomSkyMixin`, `CustomCloudMixin` and `VisualStudioScreen` first and tell me
> what theme metadata they can already carry. Ask me where the theme artwork should come from (that is the
> real blocker) and whether fog/clouds are part of a theme. Then give me a short plan before coding.
> Reference is layout inspiration only — no Purixy artwork or copy.
