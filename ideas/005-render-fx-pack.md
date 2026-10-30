# 005 — Render FX pack (Afterglow, Weather, Radiance, Low Fire, Block Outline)

**Status:** idea only (recorded 2026-10-30)
**Source:** `assets/005-render-fx-pack-01.png`
**Related:** these modules also appear as cards in [001](001-purixy-render-menu.md); Afterglow reuses the skies in [004](004-sky-themes.md)

## What it is

A set of five visual-effect modules shown as an editorial grid: two wide cards on top (Afterglow, Weather)
and three below (Radiance, Low Fire, Block Outline), one of them mid-size.

## Elements observed

Each card is a screenshot with the title and one-line description underneath, on the same dark background.

- **Afterglow** (wide) — night scene with a purple aurora and the ringed planet from the Galaxy theme;
  mobs standing in the scene are breaking into drifting white particles, texel-like squares rising.
  Copy: "The fallen dissolve into drifting light, texel by texel."
- **Weather** (wide) — a `Rain / Fire / Snow` tab row at the top-left of the image, Fire selected; the
  shot shows lava-orange ember patches scattered across a stone platform under a bright sky with columns.
  Copy: "Glassy rain with real puddles, snow that settles, and ember showers."
- **Radiance** (small) — dark forest at night; players, mobs and dropped items have a soft, blurred glow
  outline in violet (ghost-like rather than hard ESP boxes).
  Copy: "A soft outline of light around players, mobs and items."
- **Low Fire** (small, center) — first-person shot with fire re-tinted violet and raised low on screen so
  the view and hotbar stay clear; hearts and hotbar visible.
  Copy: "Low flames in Purixy colours. Your view stays clear."
- **Block Outline** (small, right) — no in-game screenshot; a rendered illustration of a magenta glowing
  cube over a stack of blocks, with motion streaks.
  Copy: "Glides from block to block on a spring."
- A sixth card is cut off at the bottom edge of the frame.

## Current state in Jilaton

Not found as named modules: Afterglow, Weather, Low Fire, Block Outline, Radiance. Related existing code:
`AmbienceModule`, `PlayerEffectsModule`, `TrailsModule`, `JumpCirclesModule`, `ExplosiveHighlightsModule`,
`HitEffectsModule`, `PlayerEspModule` (for outlines). Verify before planning — some behavior may already be
partly covered by these.

## Likely work areas

- Afterglow: death/dissolve particle effect on mobs and players; likely needs entity-render mixins.
- Weather: particle systems plus ground decals (puddles), snow accumulation, ember look; the largest item here.
- Radiance: soft outline pass — either an expanded geometry pass or an outline shader; must not alias badly.
- Low Fire: fire block model/overlay change (mixin) and optional colour tint.
- Block Outline: spring-animated selection highlight, including the interpolation between blocks.
- All five need a module entry, settings, and an on/off cost check.

## Open questions

- Which of the five are actually wanted, and in what order? This is the most expensive idea recorded so far.
- Weather: particles only, or also sky/puddle decals and audio?
- Radiance vs. existing Player ESP — is this a replacement style or a separate module?
- Block Outline: per-block spring animation, or a global highlight color/width setting?
- Performance budget on integrated GPUs.

## Boundaries

- Reference only; no Purixy code, shaders or colours copied verbatim.
- Do not report these as done without in-game visual checks — these are exactly the features that compile
  fine and look wrong.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/005-render-fx-pack.md` and `ideas/assets/005-render-fx-pack-01.png`.
>
> Goal: implement the render effects from the reference: Afterglow (entities dissolve into drifting light),
> Weather (rain/fire/snow particles with puddles and settling snow), Radiance (soft glow outline on players,
> mobs and items), Low Fire (lower, tinted fire so the view stays clear) and Block Outline (spring-animated
> block highlight).
>
> Do not start all five at once. Inspect `AmbienceModule`, `PlayerEffectsModule`, `PlayerEspModule`,
> `TrailsModule`, `ExplosiveHighlightsModule` first, then tell me which of the five are cheapest to do on top
> of what exists, and propose an order. Ask me the open questions before coding. Reference is inspiration
> only — no Purixy assets, shaders or copy.
