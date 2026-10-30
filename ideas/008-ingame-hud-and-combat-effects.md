# 008 — In-game HUD layer and combat hit effects (video reference)

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/008-video-mace-impact.jpg`, `assets/008-video-crystals.jpg`, `assets/008-video-anchors.jpg`
(2-frame-per-second contact sheets; full frames are in `analysis/video-reference-3/`, which is not committed)
**Related:** [001](001-purixy-render-menu.md) menu, [002](002-target-hud.md) target sheet, [007](007-crystal-module-pages.md) crystal modules

## What it is

Three short clips (2558×1440, 60 fps, ~5–9 s) of the reference client in a PvP hub, showing the in-game
HUD and the feel of the combat visuals: a mace hit, crystal PvP, and respawn-anchor PvP. Taken together they
define the "look and feel" target for Jilaton in-game, separate from the menu screens.

## Elements observed (shared across clips)

- **World tint.** Each clip has a strong colour grade: violet dusk for mace and anchors, deep red haze for
  crystals. Sky, fog and light appear driven by a module, not a resource pack.
- **Top-center status strip.** Small, translucent: player head + name (`YTibnGxd`, `JonzoP`), FPS, coordinates,
  server address (`pvp.hub.me`, `eu.mojang.club`), and in-game time (`23:34`, `21:15`). Reads like one row
  of tiny pills, not separate widgets.
- **"NEAREST PLAYERS" panel (left).** Narrow list of target rows: head, name, health bar, distance (`4.2m`),
  a velocity figure (`0.14b/s`), and `found in 12 ticks`. In the mace clip the same panel shows a single
  tracked player.
- **"PLAYER ACTIONS" panel (right).** Vertical list of currently active actions with icons and checkmarks:
  Auto Crystal, Auto Totem, Silent Rotations, Auto Hit Crystal, Pause On Kill, Auto Pearl, and more cut off.
  It updates as behaviours fire, so it doubles as a debug/trace view.
- **Held-item display (bottom-center).** Item name in a stylized purple label with a thin charge/durability
  bar: `Wind Charge`, `Netherite Spear`, `End Crystal`, `Glowstone`, `Respawn Anchor`, `Totem of Undying`.
- **Crosshair.** Custom small cross/plus with accent colour, plus a hit-marker flash.

## Elements observed (per clip)

**maceimpact.mp4** — wind-charge jump into a mace hit. Long violet slash arcs sweep across the target;
an impact ring plus spark burst on contact; a `Wind Charge` label with a charge bar at the bottom; the
target's health bar in the nearest-players list drains in the same tick.

**crystals.mp4** — red world; target outlined in red glow with name/health at a distance; obsidian + end crystal
placed under the target; on detonation an expanding ring and crystal shards; action list ticks through
Auto Crystal → Auto Hit Crystal; `Totem of Undying` pops and appears in the item display.

**anchors.mp4** — purple world; glowstone + respawn anchor highlighted with a soft outline; large purple
shockwave rings expand from the anchor, with slash streaks and sparks; `Glowstone` then `Respawn Anchor`
labels as items are placed; action list cycles through the anchor sequence.

## Current state in Jilaton

Existing pieces that overlap: `HudRenderer`, `HudLayout`, `HudEditorScreen`, `NowPlayingWidget`,
`TargetCardPlacement`, `PlayerEspModule`, `TargetEspModule`, `HitEffectsModule`, `HitParticlesModule`,
`DamageParticlesModule`, `ExplosiveHighlightsModule`, `TrailsModule`, `CrystalAuraModule`, `AutoAnchorModule`,
`AutoTotemModule`, `NoBobModule`, `render/fx/*`. Exact overlap must be checked in code — several of these
may already produce a version of the effect shown.

## Likely work areas

- HUD: the status strip, nearest-players panel, action list and item display are four widgets that do not
  appear to exist as such; they should be built on the existing HUD layout/editor so they can be moved and hidden.
- Action list needs a small event feed from modules (start/finish of an action) rather than polling states.
- Hit effects: slash arcs, impact rings and spark bursts as a reusable effect layer with per-context presets
  (mace / crystal / anchor).
- Colour grading: sky/fog/light presets — overlaps directly with [004](004-sky-themes.md).

## Open questions

- Which of the four HUD widgets are wanted first, and should the action list be always-on or a debug toggle?
- Is the strong world tint wanted by default, or only as a theme?
- The action list reads like a trace of what the client is doing; is that for the user's own debugging or
  just visual flavour?
- Should hit effects be one style with per-module colour, or distinct effects per hit type?

## Boundaries

- Reference only; do not extract textures, sounds or code from the clips.
- Frames extracted from the clips stay in the uncommitted `analysis/` directory; only the contact sheets are
  stored with the idea.
- Do not claim an effect matches until it has been seen in-game.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/008-ingame-hud-and-combat-effects.md` and the three contact sheets in
> `ideas/assets/008-video-*.jpg` (full frames: `analysis/video-reference-3/`).
>
> Goal: reproduce the reference's in-game layer — slim top status strip, "Nearest Players" panel, "Player
> Actions" panel, held-item display with charge bar, and the slash/ring/spark hit effects seen in the clips.
>
> Start by auditing `HudRenderer`, `HudLayout`, `TargetCardPlacement`, `HitEffectsModule`, `HitParticlesModule`,
> `ExplosiveHighlightsModule` and `TrailsModule` and report which parts already exist, with file and line
> references. Build on the existing HUD layout/editor; do not add a parallel HUD system. Ask me the open
> questions before coding, and verify visually in-game before claiming it looks like the reference.
