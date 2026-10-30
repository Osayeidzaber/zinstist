# 001 — Purixy-style Render category menu

**Status:** idea only (recorded 2026-10-30)
**Source:** screenshot supplied by the user — `assets/001-purixy-render-menu.png`
**Reference client:** Purixy (third-party; layout inspiration only)

## What it is

The user wants Jilaton's module menu to look and behave like the attached Purixy "Render" screen:
a full-screen, card-based module browser instead of (or as an alternative skin to) the current
category list, with per-module toggles on the card itself, an active-module counter, icon-based
category switching, a quick search, and game widgets (FPS, keystrokes/CPS) floating over the menu.

The screenshot is a single frame; motion, transitions and the "orbit" are only implied by the footer hint.

## Elements observed

**Frame and background**
- Dark violet-to-black scene with a large soft purple glow behind the panel.
- Panel floats in a 3D perspective ("Esc for the orbit" in the footer), i.e. the camera orbits the panel
  rather than the panel being a flat screen-space rectangle.
- Top-left: client logo mark + wordmark, next to a rounded pill showing `299 FPS` with a tiny graph line.
- Top-center: row of circular icon buttons for categories, plus `+` (add) and an avatar button.
  The active category (`Render`) is a purple-outlined pill with its label.
- Top-right: pill-shaped search field with a `/` key hint.

**Heading row**
- Large title `Render`, subtitle `See the world in a different light`.
- Right side: purple dot + `8 of 16 active`.

**Card grid**
- 4 columns × 4 rows of cards, ~16:9 wide, rounded corners, subtle border, slightly lighter than backdrop.
- Each card: rounded-square icon (purple gradient when enabled, dim/outline when disabled),
  module name (bold), one-line greyed description, a small list/settings icon at top-right,
  and a toggle switch at right (purple = on, grey = off).
- Some cards show a tiny keybind badge next to the title (Zoom shows `C`, Astral Garage shows `R`).
- Enabled cards seen: Sky, Ambience, Afterglow, Radiance, Custom Crosshair, Zoom, Block Outline, Name Tags
  (8 of 16 — matches the counter).
- Disabled cards seen: Cute World, Astral Garage, Fullbright, Freeblock, Motion Blur, Time, Weather, Particles.
- Descriptions are truncated with an ellipsis when long; they read like one-liners, not settings text.
- Category color-coding: each card's icon tile carries its own accent, not all uniform purple.

**Overlays on top of the menu**
- Keystroke widget bottom-right: `W` above `A S D`, with `LMB 9 CPS` and `RMB 0 CPS` beneath.
  `W` is highlighted, suggesting it is a live input display, not a static legend.
- Footer hint line centered at the bottom: `Click to toggle · Right-click for settings · Esc for the orbit`.

## Likely work areas in Jilaton

Verify these against the code before planning; this list is a starting point, not a finding.

- GUI: the module list/cards and category rail are a replacement rendering mode for the existing ClickGUI screens.
- Input: left-click on a card toggles, right-click opens that module's settings, `/` focuses search, `Esc` exits.
- Category state: the counter ("8 of 16 active") needs a count per category.
- Icons: 16+ distinct glyphs plus category icons; the project so far uses vector/text-drawn UI.
- Modules shown that may not exist yet: Sky, Ambience, Afterglow, Radiance, Cute World, Astral Garage,
  Motion Blur, Time, Weather, Particles. Check the module inventory before assuming any of these exist.
- The orbit/perspective framing and backdrop glow are the riskiest part of matching the look.

## Open questions

- Replace the current ClickGUI look, or add this as a second theme/layout option?
- Is the "orbit" mandatory, or is a static version acceptable for a first pass?
- Which of the 16 modules are wanted as real modules vs. only as layout placeholders?
- Should the FPS pill, keystrokes and CPS widgets be part of this screen only, or the general HUD?
- Naming: Jilaton's own wording for categories/subtitles, rather than copying Purixy's copy.

## Boundaries

- Reference only. Do not copy Purixy's code, icons, fonts, gradients, logo, module descriptions or
  branding into the repo; recreate the layout and interaction model with original assets.
- The screenshot stays in `assets/` as a reference image and is not a shipped game asset.
- No claim of visual parity without an in-game screenshot comparison.

## Handoff prompt

> You are working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric, Minecraft 1.21.11, Java 21).
>
> First read `JilatonClient/agent.md`, `JilatonClient/development/CHECKPOINT.md`, then
> `ideas/001-purixy-render-menu.md` and the screenshot at `ideas/assets/001-purixy-render-menu.png`.
>
> Goal: build a Purixy-style Render category screen — card grid with per-card toggles and keybind badges,
> icon category rail, `8 of 16 active` counter, `/` search, left-click toggle / right-click settings /
> `Esc` exit, FPS pill, and keystroke+CPS overlay floating over the panel.
>
> Read the existing ClickGUI, HUD and input code first; reuse the project's module registry, settings
> screens and rendering helpers rather than adding a parallel system. Ask which open questions in the
> idea file are decided before writing code, then give me a short plan with files you would touch.
> The reference is layout inspiration only — no Purixy code, assets, icons or copy.
>
> Verify with the project's tests/build, and report what was seen in-game separately from what was
> only compiled or tested.
