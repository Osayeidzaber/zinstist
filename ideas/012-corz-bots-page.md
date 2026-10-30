# 012 — CORZ Bots page (Crystal PvP Bot, Auto Elytra Pilot, Auto Elytra Finder)

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/012-corz-bots-page-01.png`, `-02.png`, `-03.png`
**Related:** [011](011-corz-menu-system.md) for the shell this page lives in

## What it is

A dedicated "Bots" category: a roster of named bots, a preview of the selected bot, and a settings panel
for a chosen bot program. Three examples are shown: a Crystal PvP bot, an elytra route flyer, and an elytra
finder/collector.

## Elements observed

**Layout**
- Header: `Bots · 13 bots · 1 running` left, time, search right.
- Second rail: a vertical strip of small icon tiles, one per bot (sword, trident, axe, pickaxe, totem,
  boots, potion, ender pearl, …), each tile a bot.
- Center: bot identity — name (`CPX-01`, `PLT-09`, `ELY-03`) and a status word `OFFLINE`, with a 3D player
  model standing on a faint ellipse (one is a skeleton-skin crystal PvP bot; the other two show a skin and a
  pair of elytra).
- Right: detail panel — eyebrow label (`PVP BOT`, `UTILITY BOT`), name and badges
  (`Crystal PvP Bot` + `BETA`, `Auto Elytra Pilot` + star, `Auto Elytra Finder` + star + info glyph),
  one-line description, toggle at top-right, then settings.

**Crystal PvP Bot settings**
- Keybind (empty chip), Playstyle (`Terminator` dropdown with arrows), Fight Range slider `16`,
  Turn Speed (`Normal`), then toggles: Comboes, Aggro Pearls, Use Anchors, Build Obsidian, Eat Gapples,
  Escape Pearls, Learn Opponents, Protect Loot.
- Description: "Fights crystal PvP for you, start to finish."

**Auto Elytra Pilot settings**
- Description: "Flies you to your coords on an elytra."
- Keybind, Destination text field (`Type here...`), Flight Height slider `290`, Auto Mending,
  Auto Buy Rockets, Rocket Stacks slider `3`, Auto Buy XP Bottles, XP Bottle Stacks slider `2`, Price Limit (`Normal`).

**Auto Elytra Finder settings**
- Description: "Flies the End and brings home elytra."
- Keybind, Flight Altitude `110`, Firework Minimum Count `16`, Minimum Elytra Durability `30`,
  Auto Mending, Combat Shulkers, Auto Store Elytra, Fly Collected Elytra, Notifications (`Toast`),
  Status Card, Debug Rendering.

## Current state in Jilaton

Nothing in the source corresponds to player-controlled bots; this would be a new subsystem. It is also the
largest and most ambiguous idea recorded: a "bot" could mean a scripted local player, an external account,
or automations layered on the current player. The reference UI does not answer that.

## Likely work areas

- Bot registry: id, display name, skin, status, category counts.
- Bots screen: icon rail, identity/preview pane, detail pane with per-bot settings.
- Control layer: start/stop, status reporting, and how a bot actually acts (input simulation vs. pathing).
- Shared setting widgets: dropdown with arrows, sliders with value labels — overlaps [011](011-corz-menu-system.md).

## Open questions

- What is a "bot" in this design? A second local player the client drives, or automation of the current player?
- Is this meant for singleplayer/private servers only, or live multiplayer?
- What does "1 running" imply — a background task, a second game instance, or a spawned entity?
- Which bots are actually wanted? Crystal PvP is the only one with real depth in the screenshots.
- `Auto Buy …` options require a server economy — which server?

## Boundaries

- Do not implement anything that automates a second account on a public server without the user explicitly
  confirming the environment and its rules.
- Reference only; no skins, artwork or copy reused.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/012-corz-bots-page.md` and `ideas/assets/012-corz-bots-page-0*.png`.
>
> The reference shows a Bots page with a roster rail, bot preview and per-bot settings (Crystal PvP Bot,
> Auto Elytra Pilot, Auto Elytra Finder). Before any design work, ask me what a "bot" is supposed to be here —
> a locally controlled second player, automation of my own player, or something else — and which environment
> it runs in. That answer decides the entire architecture.
>
> Once answered, audit the module/setting/input systems for anything reusable and give me a plan for the
> screen first, the bot control layer second. Do not build automation against a live public server.
