# 011 — CORZ "Premium interface" menu system

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/011-corz-menu-system-01.png` … `-06.png`
**Note:** this and [001](001-purixy-render-menu.md) are **two different menu paradigms** (sidebar + two-column
list with keybinds, vs. big preview card grid with descriptions). Pick one before building.

## What it is

A full client menu system: left sidebar with categories, a header bar with greeting/time/search, a
two-column list of module cards with toggles and keybinds, and a tabbed settings area with sound options.

## Elements observed

**Shell**
- Floating rounded window on a black page, visible outer border and a soft magenta glow along the edges.
- Left sidebar (~230 px): `CORZ` wordmark + `v2.0.1`; group heading `MODULES` then
  `Combat 7`, `Movement`, `DonutSMP 5`, `Visuals 13`, `Misc 9`, `Network`, `Bots 1` — each a line icon, label
  and a count badge; group heading `GENERAL` then `Settings`, `Configs`, `Theme`, `Socials`.
  Active row is filled purple/pink with a brighter icon and text.
- Sidebar footer: account card — logo tile, `CORZ.GG`, `Lifetime`, green online dot.
- Header bar: `Hello, CORZ.GG` left, time `20:23` in accent colour, search field right with placeholder
  `Search modules...` and a `⌘ K` hint.

**Module list**
- Two equal columns of cards, ~92 px tall each, vertical scroll.
- Card contents: module name + small `(i)` circle, optional red diamond marker (premium/locked?) on some
  modules, `KeyBind: [G]` line under the name, toggle switch at the right.
- Enabled: magenta/purple glow border, lighter fill, toggle in accent colour. Disabled: dark fill, muted text.
- Modules seen across the three scroll positions: Auto Inventory Totem, Anchor Macro, Auto Crystal, Fight
  Recorder, Auto Totem, Crystal Optimizer, Aim Assist, Silent Aim, Reach, AutoTrap, AutoCart, AutoClicker,
  Trigger Bot, Hit Select, Trident Assist, Safe Anchor, Auto XP, Auto Hotbar, Auto Shield Disabler,
  Auto Hit Crystal, Hit Boxes, Obsidian Optimizer, Perfect Wind Charge, Auto Aggro Pearl, Key Pearl, Criticals,
  W Tap, Auto Jump Reset, Hit Swap, Auto Mace.
- Category counts in the sidebar (Combat 7, Visuals 13, Misc 9…) do not match the number of cards shown —
  the list includes submodules or the counts are per-subcategory; behaviour is unclear from stills.

**Settings**
- Header `Settings` + pill tabs: `All`, `Interface`, `Sounds`, `License`, `System` + a separate
  `Search settings...` field.
- Settings are cards in a two-column grid: title, one-line subtitle, control on the right — toggle, key chip
  (`RSHIFT`), dropdown (`Default`, `Soft Tick`, `Normal`, `Terminator`), or a slider with a value on the right.
- `INTERFACE`: Menu Bind (RSHIFT), 3D GUI (open the menu as a floating panel in the world), Quick Friend
  (middle-click add/remove), Toast Notifications, Module Toggle Toasts.
- `SOUNDS`: Module Sound, Hover Sound, Category Click Sound, Module Card Sound, Sound Pack (Default),
  Hover Style (Soft Tick), Volume 170%, Hover Volume 45%.
- Section labels are small caps and colour-coded; typography mixes a display font for headings with a clean
  sans for rows.

## Current state in Jilaton

`ClickGuiScreen`, `ClientTheme`, `JilatonUi`, `JilatonWidgets`, `JilatonCardScreen`, `TextSettingScreen`,
`HudEditorScreen`, `VisualStudioScreen` — an existing ClickGUI with categories, expandable settings,
keybinds and profiles. This reference implies a rework of the shell (sidebar + card list + tabbed settings)
rather than a new subsystem.

## Likely work areas

- Sidebar with counts and icon set; active-state styling.
- Module card: name, info glyph, keybind chip, toggle, enabled glow.
- Settings screen: tabs, two-column card grid, per-row control types (toggle/keybind/dropdown/slider).
  Sliders and dropdowns need to exist in Jilaton's setting widgets.
- Sound system: toggle/hover/click sounds, pack selection, separate UI volume.
- Search: filters modules and/or settings depending on context; `⌘K` shortcut (on Windows, `Ctrl`).
- "3D GUI" option links to the floating/orbit menu idea in 001.

## Open questions

- Adopt this sidebar + list layout, the Purixy card grid, or two themes?
- What do the red diamond markers mean (premium / server-limited / experimental)?
- `DonutSMP` is a server-specific category — does Jilaton need per-server categories?
- Are the sidebar counts per category computed automatically?
- Are the sounds generated or bundled assets (and is redistributing them allowed)?

## Boundaries

- Reference only: no CORZ logo, fonts, sounds or copy.
- Do not copy the reference's typo (`SOURDS`) or its module names verbatim.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/011-corz-menu-system.md` and the six images `ideas/assets/011-corz-menu-system-0*.png`.
> Also read `ideas/001-purixy-render-menu.md`, which proposes a different layout.
>
> Goal: rework the ClickGUI shell into a sidebar + category counts + header (greeting, time, search) +
> two-column module card list with keybind chips and toggles, plus a tabbed settings screen (All / Interface /
> Sounds / System) with toggle, keybind, dropdown and slider rows.
>
> Ask me first whether to follow this layout, the Purixy card grid, or build both as themes — this is the
> biggest design decision in the backlog. Then audit `ClickGuiScreen`, `JilatonUi`, `JilatonWidgets`,
> `TextSettingScreen` and `ClientTheme` and report what can be reused, with file and line references.
