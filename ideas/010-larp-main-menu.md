# 010 — Larp Client main menu (minimal monochrome)

**Status:** idea only (recorded 2026-10-30)
**Source:** `assets/010-larp-main-menu-01.png`

## What it is

A title-screen design reference: almost pure black, a small centered logo and a plain vertical text menu,
with the player model on the right and an account switcher above it. No cards, no buttons, no panels.

## Elements observed

- Background: near-black with very subtle vertical wavy bands (like slow interference patterns) and small
  outlined star glyphs scattered as a watermark texture.
- Center column, all centered text with generous spacing:
  - Rounded-square logo mark containing a six-pointed star (outline).
  - Title `Larp Client` in a clean sans, medium weight, white.
  - Menu items stacked with even gaps, plain text, no boxes: `Singleplayer`, `Multiplayer`,
    `Minecraft Settings`, `Client`, `Quit Game`. No visible selected state in the still.
- Right side: full-body player model (idle, no rotation), dark outfit, standing on a faint elliptical shadow.
  Above the model: `xln ⌄` — the account name with a chevron, i.e. a dropdown account switcher.
- No footer, no version string, no social icons, no background music indicator.

## Current state in Jilaton

`JilatonMainMenuScreen`, `JilatonBackdrop`, `JilatonLogo`, `JilatonLoadingLogo`, `MenuAvatar`, `MenuChrome`,
`JilatonWorldsScreen`, `JilatonServersScreen`. Jilaton already has a custom title screen with a dark theme,
animated backdrop and a vector J mark — this idea is a restyle toward the reference's extreme minimalism.

## Likely work areas

- Layout: single centered column, text-only items, hover/press states that stay text-based.
- Background: black base plus a low-contrast wavy/starfield watermark; must stay cheap to draw.
- Player model with account switcher above it.
- Decide whether existing worlds/servers screens keep their current layout or follow the same minimal style.

## Open questions

- Keep Jilaton's own branding (J mark, name) or adopt a similar monochrome feel?
- Should menu items animate on hover (fade, slight shift) given there are no buttons?
- Keep the account label/chevron, or skip it if account switching is not supported?

## Boundaries

- The six-pointed star mark belongs to that client; Jilaton must use its own logo.
- Reference only; no fonts, artwork or copy reused.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> then `ideas/010-larp-main-menu.md` and `ideas/assets/010-larp-main-menu-01.png`.
>
> Goal: restyle the title screen toward this minimal monochrome design — black background with a subtle
> wavy/star watermark, centered logo and text-only menu, player model on the right with an account label.
>
> Read `JilatonMainMenuScreen`, `JilatonBackdrop`, `JilatonLogo` and `MenuAvatar` first and report what the
> current screen does. Keep Jilaton's own logo and branding. Ask me whether the worlds/servers screens should
> follow the same style, then give me a short plan. Reference is inspiration only.
