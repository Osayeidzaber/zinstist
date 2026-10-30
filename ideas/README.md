# Ideas backlog

Notes about things the user wants to add to Jilaton. **This folder is a notebook, not a spec.**
Writing something here does not approve it for implementation, and nothing here describes
current behavior — the code is the source of truth for that.

## Layout

```
ideas/
├── README.md          # this file
├── 001-<slug>.md      # one file per idea, numbered in the order they were given
└── assets/            # screenshots and reference images, named to match the idea file
```

Each idea file has:

- **What it is** — the raw idea, plus a factual description of any attached image.
- **Elements observed** — concrete pieces seen in the reference, so nobody has to re-derive them.
- **Open questions** — what the user has not decided yet.
- **Boundaries** — reference-only material that must not be copied.
- **Handoff prompt** — a copy-paste brief for a fresh agent.

## Index

| # | Idea | Source | Already in code? |
| --- | --- | --- | --- |
| [001](001-purixy-render-menu.md) | Purixy-style Render category menu (card grid, orbit, FPS pill) | user screenshot | ClickGUI exists; this is a new layout |
| [002](002-target-hud.md) | Target HUD card (head, mace badge, segmented health, stat chips) | Purixy screenshot | target card exists — visual upgrade |
| [003](003-player-awareness-suite.md) | Staff List, Anti-Vanish, Storage ESP, Radar, Chunk Finder | Purixy screenshot | Freecam / Player ESP / Tracers exist; the rest are new |
| [004](004-sky-themes.md) | Eight sky themes + theme browser | Purixy screenshots ×2 | `CustomSkyModule` exists; presets and picker are new |
| [005](005-render-fx-pack.md) | Afterglow, Weather, Radiance, Low Fire, Block Outline | Purixy screenshot | none found by those names |
| [006](006-music-player-and-nick-hider.md) | Compact Spotify card with karaoke lyrics + Nick Hider | Purixy screenshot | music/lyrics stack exists; Nick Hider is new |
| [007](007-crystal-module-pages.md) | "What each module actually does" crystal-module pages | Purixy screenshots ×4 | crystal/totem modules exist; ask docs vs. behaviour |
| [008](008-ingame-hud-and-combat-effects.md) | In-game HUD layer (status strip, nearest players, actions, item display) + hit effects | Purixy videos ×3 | HUD/effect code exists in part; audit first |
| [009](009-larp-ingame-hud.md) | glave/Larp in-game HUD: keybinds, above-head target, held-item render, server panel, white effects | Larp screenshots ×3 | overlaps 008 and existing HUD widgets |
| [010](010-larp-main-menu.md) | Minimal monochrome title screen with player model | Larp screenshot | `JilatonMainMenuScreen` exists — restyle |
| [011](011-corz-menu-system.md) | CORZ sidebar + module card list + tabbed settings/sounds | CORZ screenshots ×6 | ClickGUI exists; **conflicts with 001 layout** |
| [012](012-corz-bots-page.md) | Bots page: Crystal PvP Bot, Elytra Pilot/Finder | CORZ screenshots ×3 | new subsystem; definition question first |

## Handoff prompt (generic)

Paste into a new session, swapping the idea file name:

> You are working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric, Minecraft 1.21.11, Java 21).
>
> First read, in this order: `JilatonClient/agent.md` (architecture and verification rules),
> `JilatonClient/development/CHECKPOINT.md` (what is actually verified), and
> `ideas/<idea-file>.md` with its screenshot in `ideas/assets/`.
>
> The idea file is the user's wish list, not an approved design. Its "Boundaries" section is
> binding: the reference client's code, assets and branding must not be copied — reproduce layout
> and behavior ideas with original code.
>
> Read the existing code before proposing anything, and tell me a short plan with the files you
> would touch before writing code. Do not claim anything is complete unless you ran the build/tests
> and distinguish automated results from behavior seen in-game.

## Status key

Keep a one-line status at the top of each idea file: `idea only` → `planned` → `in progress` → `done`.
