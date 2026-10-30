# 002 — Target HUD (Purixy style)

**Status:** idea only (recorded 2026-10-30)
**Source:** `assets/002-target-hud-01.png`
**Related:** [001](001-purixy-render-menu.md) shows the same client's menu and crosshair

## What it is

A compact "target sheet" that appears while a player is targeted: head, name, health, held item,
distance, and three fight statistics, in one rounded card. The reference does this as a single card
rather than a list of separate HUD widgets.

## Elements observed

- One rounded, near-black translucent panel, roughly a third of the screen wide, centered slightly below
  the crosshair. Thin light border, soft drop shadow.
- Game world behind the card is heavily blurred — may be a promo-shot effect, not an in-game blur.
- Left: square player head with rounded corners and a thin lavender outline.
- Row 1: name `Lumen` in large bold white; beside it a dark-purple pill with a small tool glyph and `MACE`.
- Row 2: small pink heart + `20.0`; right-aligned `5.8 m`.
- Row 3: health bar split into about nine segments, purple-to-magenta gradient, with a faint hairline under it.
- Row 4: three chips — pink down-chevron `4.4 m up`, yellow shield `0 pops`, blue `2 charges`.
- A small pink/white crosshair floats above the card (the client's separate crosshair module, not part of the card).
- Marketing caption below the frame: "Target HUD reads the fight and picks from ten PvP modes by itself."

## Current state in Jilaton

A target card already exists (`gui/TargetCardPlacement`, rendered from `gui/HudRenderer`; README documents
"Target card style = Compact" and "Target card position = Follow target"). This idea is a visual/feature
upgrade of that card, not new plumbing.

## Likely work areas

- Health bar segmentation and gradient; the thin rule underneath.
- Head glyph at this size (the project already draws an avatar in other UI).
- Mace badge — likely reflected from the target's held item; needs item → short name mapping.
- The three stat chips need definitions (fall height, totem pops, mace charges are the likely intents).
- Extra style option in HUD settings so the current card layout is not lost.

## Open questions

- What exactly are `pops` and `charges`? (totem pops and mace charges, or something else?)
- Is the badge the target's held item, or the client picking a tactic?
- Is "ten PvP modes by itself" wanted as adaptive behavior, or was that just marketing copy?
- Should this replace the existing compact target card or be a third style?

## Boundaries

- Layout inspiration only. Do not copy Purixy's glyphs, fonts, colours or copy text.
- Do not blur the game world unless the user asks; it is probably a screenshot effect.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> `JilatonClient/development/CHECKPOINT.md`, then `ideas/002-target-hud.md` and
> `ideas/assets/002-target-hud-01.png`.
>
> Goal: upgrade the existing target card into the reference layout — head, name, held-item badge, health,
> distance, segmented health bar and three stat chips (fall height / totem pops / charges). Reuse
> `TargetCardPlacement` + `HudRenderer` and the HUD settings system; add a style option instead of replacing
> the current card. Ask me the open questions before coding, especially what `pops` and `charges` mean.
> Reference is layout inspiration only — no Purixy assets or copy.
