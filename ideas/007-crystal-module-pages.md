# 007 — "What each module actually does" crystal-module pages

**Status:** idea only (recorded 2026-10-30)
**Sources:** `assets/007-crystal-module-pages-01.png` (Auto Crystal), `-02.png` (Auto Hit Crystal),
`-03.png` (Auto Totem), `-04.png` (20+ more)

## What it is

A long-form presentation of crystal modules: for each module, a category tag, a large serif title, one-line
description, a settings table, short check-bulleted claims, and a gameplay screenshot badged "In Action".
It is unclear from the images alone whether the user wants (a) such a page/docs format for Jilaton,
(b) the specific module behaviours and settings shown, or (c) both. That must be asked before work starts.

## Elements observed

Layout: alternating image/text rows, hairline-separated setting rows (label left, value pill right), and
green tick bullets under each module. Header copy: "What each module actually does. / No marketing. Here's
exactly how every crystal module works. Settings, logic, and what makes each one different."

**Auto Crystal** — tag CORE MODULE
- "Automatically places and explodes end crystals at the block you are looking at."
- Settings: Delay → Configurable Randomized; Head Bob → Silent; Damage Tick → On; Pause On Kill → On.
- Bullets: customizable randomized delay, described as "completely undetectable"; silent head bob.
- Screenshot: first-person view of a crystal on obsidian, badged AUTO CRYSTAL / In Action.

**Auto Hit Crystal** — tag OFFENSE
- "Places obsidian at your enemy's feet to instantly double tap them."
- Settings: Delay → Configurable Randomized; Crystal Mode → Double Tap; Damage Tick → On; Pause On Kill → On.
- Bullets: instant obsidian placement; works on high ping; silent head bob for double taps.
- Screenshot: crystal in the player's view, badged AUTO HIT CRYSTAL / In Action.

**Auto Totem** — tag DEFENSE
- "Never die again. Auto Totem instantly refills your totem the moment you need it, configurable smart or
  randomized delays."
- Settings: Totem Speed → Configurable Randomized; Actions → Double Hand / Open Inventory / …;
  Hover Totem → On; Legit Mouse Movements → On; Dynamic Delay → On.
- Bullets: fast re-totem; configurable actions; delays based on danger score and "legit mouse movements".
- Screenshot: inventory open with totems and lava, badged AUTO TOTEM / In Action.

**Closing section** — "And 20+ more crystal modules." with pill list: SAFE ANCHOR, SILENT AIM, AUTO ELYTRA, HITBOXES.

## Current state in Jilaton

Related modules already exist: `CrystalAuraModule` (+ policy/damage estimator), `AutoAnchorModule`,
`AutoTotemModule`, `NoBobModule`, `HitboxModule`, `ElytraTargetModule`. There is no separate
"Auto Hit Crystal". Whether settings like Damage Tick, Pause On Kill, Hover Totem, Dynamic Delay and
Actions already exist must be checked in code, not assumed.

## Likely work areas

- If the ask is documentation: a docs layout (settings table + screenshot + notes) for the existing modules,
  likely as markdown pages under `JilatonClient/development/` or a small site section.
- If the ask is behaviour: compare each listed setting with the current module and add the missing ones
  (double-tap mode, damage tick gating, pause-on-kill, hover totem, dynamic delay, action choice).
- Badge/`In Action` and the screenshot framing are presentation only.

## Open questions

- Documentation, module behaviour, or both?
- "Configurable randomized" and silent head bob imply detection avoidance; should Jilaton document this
  openly, and with what wording?
- Which crystal modules beyond the three shown should get the same treatment?

## Boundaries

- Do not reproduce the claim "completely undetectable" — it cannot be proven and should not appear in
  Jilaton docs.
- Do not copy Purixy's copy, fonts, screenshots or layout pixel-for-pixel; write Jilaton's own text.
- Showing the real state honestly matters more than matching the marketing tone.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/007-crystal-module-pages.md` and the four images in `ideas/assets/007-*`.
>
> The reference is a marketing/docs layout for crystal modules. Before writing anything, ask me whether the
> target is (a) a documentation page in this style for Jilaton's existing crystal modules, (b) the specific
> settings/behaviours shown (double-tap crystal mode, damage tick, pause on kill, hover totem, dynamic delay,
> configurable actions), or (c) both.
>
> For whichever it is: audit `CrystalAuraModule`, `AutoTotemModule`, `NoBobModule` and their settings against
> the reference list and report the real gaps with file and line references. Do not claim features exist
> because the reference lists them, and do not use "undetectable" style claims in Jilaton's own text.
