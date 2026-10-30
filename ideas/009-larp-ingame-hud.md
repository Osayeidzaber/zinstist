# 009 — Larp Client / glave in-game HUD and hit effects

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/009-larp-ingame-hud-01.png`, `-02.png`, `-03.png`
**Related:** [008](008-ingame-hud-and-combat-effects.md) is the same surface from the Purixy clips; treat 008 + 009 as the combined in-game target

## What it is

Three screenshots from a PvP client (`glave`) on `mcpvp.club`, showing a much more widget-heavy in-game
HUD than the Purixy clips: keybind list, above-head target info, mini target card, giant held-item render,
server panel, coords, module arraylist, and white/blue hit effects with floor rings.

## Elements observed

- **Top-left status strip** — small dark pills: player head + `glave`, server `mcpvp.club`, `144 fps`, `20:39`.
- **KeyBinds panel (left)** — title `KeyBinds` with a cog icon; two rows: module name and a key chip.
  `KillAura` is highlighted purple with `C`, `Fake Lag` with `G`. Sits mid-left, small footprint.
- **Above-head target info** — over the enemy: name `LImZ`, then `10 HP  0m  17ms` (health, distance, ping),
  then a second line with a number and a heart (`13 ♥` / `13`), most likely totem or heart count.
  A glowing ring/circle is drawn around the target's feet.
- **Mini target card (bottom-center)** — small rounded card: head, `LImZ`, `Distance: 0.9`, green health bar;
  a row of small glyphs top-left (`▲ ▼ ♥`) and an `X` close glyph top-right.
- **Held-item display (bottom-right)** — a very large 3D render of the held item (glowing blue netherite sword)
  floating over the hotbar area.
- **Coords pill (bottom-left)** — `XYZ 123151.0, 41.3, 1281.3`; in the third shot a `11/20 HP` text bar sits
  under it and chat sits above it.
- **Module arraylist (top-right)** — long right-aligned list of every enabled module, small coloured text:
  Ancient Predictor, Damage Marker, Damage Particles, DamagePhysics, Death Coords, Death Sounds, Effect Sounds,
  Fake Lag, Fast Place, Fight Helper, Free Look, Freecam, Item Physics, Jesus, Keep Sprint, Key Pearl, KillAura,
  Lag Notifier, Night Vision, No Render, No Slow, No Fall, No Desktop (?), No HUD, Auto HUD, Auto Play, Penis (?),
  Fast Place, Auto Effects, Auto HUD, Bridge Assist, Animations, Anti Bot, Auto Cart, FPS, HUD, TOS, ...
- **Server panel (right-middle)** — `CLUB` header, ping glyph, a score `2 - 0`, date `10/07/2026`, proxy
  `eu-duel-nc3 (34ms)`, and server `mcpvp.club`.
- **Effects** — white glowing polygonal floor rings and lattice lines across the arena floor; long white slash
  trails arcing from the player; white spark bursts on hits; blue-violet world tint; small white crosshair.

## Current state in Jilaton

`HudRenderer` / `HudLayout` / `HudEditorScreen` provide widgets (FPS/ping, coords, keystrokes, CPS, armor,
potions, module activity, target info). Overlaps to check before planning: coords pill, FPS, target card,
arraylist (`HudModule`?), and `HitEffectsModule` / `TrailsModule` for the effects. KeyBinds panel, above-head
target block, mini target card with distance and glyph row, held-item render and server panel do not appear
to exist.

## Likely work areas

- KeyBinds panel: list enabled modules that have a keybind, name + key chip, highlight on press.
- Above-head info: billboard text block anchored to the target entity, distance in blocks, ping from the
  player list entry, totem/heart count.
- Mini target card: probably the same data as [002](002-target-hud.md) in a smaller form; build both on one model.
- Held-item display: item model scaled up at a fixed screen position; needs light and a soft glow.
- Floor rings / slash trails: this is the effect layer from [008](008-ingame-hud-and-combat-effects.md), in
  a white/blue instead of violet variant.
- Server panel: latency, session score, proxy host, date — where does the score come from? Needs a decision.

## Open questions

- Above-head line: is the number after the bar a totem count, hearts, or something else?
- Is the giant held-item render wanted, and does it replace the vanilla held-item position?
- KeyBinds panel: only bound modules, or all enabled modules with empty key slots?
- Server "2 - 0" score — client-side tracked wins, or a server API?
- Purixy (008) and this client (009) show different HUD densities; which is closer to the desired look?

## Boundaries

- Reference only; no assets, fonts or copy taken from the client.
- The arraylist in the screenshot includes module names specific to that client; Jilaton's list comes from its
  own registry.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/009-larp-ingame-hud.md` with `ideas/assets/009-larp-ingame-hud-0*.png`, and
> `ideas/008-ingame-hud-and-combat-effects.md` (same surface, other client).
>
> Goal: build the in-game HUD widgets shown — keybind list, above-head target info (HP, distance, ping,
> totem count), mini target card, large held-item display, server panel, and the white floor-ring/slash
> hit effects.
>
> Audit `HudRenderer`, `HudLayout`, `HudEditorScreen`, `TargetCardPlacement`, `HitEffectsModule` and
> `TrailsModule` first and tell me which pieces already exist with file and line references. Build on the
> existing HUD layout/editor so every widget is movable and hideable. Ask which reference direction to follow
> (this or Purixy) before designing, and verify in-game before claiming it matches.
