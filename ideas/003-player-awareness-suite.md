# 003 — Player awareness suite (Staff List, Anti-Vanish, Storage ESP, Radar, Chunk Finder)

**Status:** idea only (recorded 2026-10-30)
**Source:** `assets/003-player-awareness-suite-01.png`

## What it is

Eight awareness modules presented as a feature grid: two large hero cards (Staff List, Anti Vanish) and
six smaller module cards (Freecam, Player ESP, Storage ESP, Tracers, Radar, Chunk Finder).

## Elements observed

Card layout: rounded dark card, purple icon tile, bold title, one-line greyed description, purple toggle
switch top-right. All shown as enabled.

- **Staff List** (hero, left) — screenshot of a floating list over the world: `Staff` header with a lock
  glyph and a `2 online` pill; rows `Alex — ADMIN`, `Kai — MOD` each with a small avatar tile; the third
  row is dimmed. Copy: every staff member online with their rank, the moment they join; vanished members
  stay listed, dimmed.
- **Anti Vanish** (hero, right) — alert card over a large grey spectator-like icon on a blue glow:
  head avatar, `VANISH DETECTED`, `Notch`, `Switched to spectator`. Copy: know when staff are watching
  from spectator, as a soft alert.
- **Freecam** — "Leave your body where it stands and fly the camera anywhere."
- **Player ESP** — "Every player lit through walls, in soft filled light."
- **Storage ESP** — "Chests, barrels and shulkers glow through the ground. The ones you opened glow softer."
- **Tracers** — "A thin line to every player, so nobody walks up on you."
- **Radar** — "Players further away than you choose, on a round glass radar."
- **Chunk Finder** — "Signs of bases hidden underground: lit portals, wither farms and ways down."

## Current state in Jilaton

- Already exist: `FreecamModule`, `PlayerEspModule`, `TracersModule`.
- Not found in source: Staff List, Anti-Vanish, Storage ESP, Radar, Chunk Finder.

## Likely work areas

- Staff List: needs a source of truth for rank. Options: tab-list prefix parsing, a client-maintained
  per-server staff list, or both. Vanished players usually disappear from the tab list, so the list must
  persist entries rather than rebuild from tab.
- Anti-Vanish: the server does not tell clients that someone is in spectator. Realistic signals are
  indirect — tab list presence/absence, sounds, block changes, entity spawns, ping, skin. Anything built
  here must be honest about confidence and must not claim reliable detection.
- Storage ESP: block-entity scan around the player, different tint for opened containers (block state / loot state).
- Radar: circular widget with range setting; reuses the existing ESP data.
- Chunk Finder: radius scan for lit portals, farms, drop shafts; heaviest of the set.

## Open questions

- Which server(s) is this aimed at? Rank names and vanish behaviour differ per plugin.
- Staff list: automatic detection, manual list, or both?
- Radar: circle mask over a top-down render, or actual textures?
- Chunk Finder: scan radius, what counts as a "sign", and is a background scan acceptable?
- Should these live in one "Awareness" category or spread across Player/Render?

## Boundaries

- Reference only; no Purixy assets, copy, or code.
- Staff-detection style features can breach server rules; the user decides where they run. Do not claim
  support for any named server, and do not present heuristic detection as certain.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/003-player-awareness-suite.md` and
> `ideas/assets/003-player-awareness-suite-01.png`.
>
> Goal: add Staff List, Anti-Vanish, Storage ESP, Radar and Chunk Finder as modules, reusing
> `PlayerEspModule`/`TracersModule` scanning and the shared module lifecycle. Freecam, Player ESP and Tracers
> already exist — do not rebuild them; the reference only tells you how they should look and read.
>
> Ask me which server this targets and how ranks should be detected before designing the staff list, and
> describe how Anti-Vanish would infer spectator state before implementing anything. Then give me a short
> plan with files you would touch. Reference is inspiration only — no Purixy code, assets or copy.
