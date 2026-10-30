# Jilaton GUI redesign v1: build spec

**Source of truth for the look:** the design canvas
<https://claude.ai/artifact/4B93VkveZ3RMWprGWTifsq> (private to the user; ask them for access if you cannot open it).
Every board is 1280×720, which is a 1920×1080 screen at GUI scale 1.5. Scale the geometry with the GUI scale; do not
hard-code pixels against the window.

**Status:** design approved for build by the user on 2026-10-31. Nothing below exists in game yet unless the
"Exists today" line says so. Written by the audit session; `reference/ui/DIRECTION.md` remains the token and
anti-pattern authority, and this file does not override it.

---

## 0. Rules every screen follows

### Tokens (from `reference/ui/DIRECTION.md`; already mirrored in `gui/shell/ShellPalette.java`)

| Token | Value | Use |
| --- | --- | --- |
| ink/backdrop | `#0B0A0E` | full-screen menus (main, servers, music, accounts) |
| ink/scrim | `#08070A` at 78% | behind any panel opened over the world |
| ink/panel | `#0E0C12` | ClickGUI panel, dialogs |
| ink/sidebar | `#0A090F` | ClickGUI sidebar |
| ink/card | `#14121A` | cards, inputs, chips |
| ink/card-hover / raised | `#1A1722` | hover, selected tab, icon tiles |
| ink/drawer | `#100E16` | settings drawer, lyrics panel |
| stroke | `#262231` | every 1px hairline; never white |
| text/primary · secondary · tertiary | `#F2F0F5` · `#8E8A9A` · `#5E5A69` | tertiary = uppercase group labels |
| text/soft | `#C9C5D3` | secondary values on dark glass (HUD) |
| accent · deep · glow | `#A855F7` · `#7C3AED` · `rgba(168,85,247,0.35)` | **state only**; comes from `ClientTheme.resolveAccent`, never hard-coded |
| danger · online · warn | `#E5484D` · `#30A46C` · `#E5A13E` | destructive actions, live status, weak ping and offline accounts |

- Accent marks state: the active nav row, an "on" toggle, a selected chip, the one live number. If accent covers more
  than about 10% of a screen, it is wrong.
- Active-element glow = 1px accent ring at ~45% plus a soft halo (`0 0 24px` at ~10%). Nothing else glows.
- Radii: cards 12, panels 14, dialogs 18, chips and inputs 8–10, toggles fully rounded.
- Spacing rhythm: 4 / 6 / 8 / 10 / 12 / 16 / 24 / 40. Cards in a grid have a 10px gap.

### Type

- **Inter** (UI) and **JetBrains Mono** (numbers, keys, versions). Both are under the SIL OFL, so they may ship in
  the jar. Add the licence texts and a line to `ATTRIBUTION.md` (O writes that file).
- In game: add a TTF font provider at `assets/jilaton/font/ui.json` (`{"providers":[{"type":"ttf","file":"jilaton:inter.ttf","size":11,"oversample":4}]}`)
  plus `mono.json`, and draw text with `Component.literal(...).withStyle(s -> s.withFont(...))`.
- Keep a **Font: Inter / Minecraft** switch on the Theme page (§10). The Minecraft option must render every screen
  correctly with the vanilla font. Test both.
- Sizes used: 34 (pause title), 26 (page titles), 24 (ClickGUI page title), 17 (menu nav rows), 13–15 (card titles),
  12 (body), 11 (meta), 10 (tertiary group labels, letter-spacing 0.1em).
- All numbers that change (FPS, clock, ping, timers, percentages) use tabular figures (the mono font).

### Motion (board "Motion spec")

| Thing | Duration | Curve | Notes |
| --- | --- | --- | --- |
| Player walk-in (main menu) | 900 ms | ease-out cubic | enters from the right edge, soft stop, shadow fades in; then the head tracks the cursor; menu rows stagger 40 ms apart |
| Panel open / close | 220 / 160 ms | standard | scale 0.96→1, rise 8px, world blur+dim in parallel |
| Page change | 140 ms | cross-fade | content column only; **the sidebar never moves**; scroll resumes where it was |
| Toggle | 120 ms | ease-out | knob slide, fill fades stroke→accent, one faint ring pulse on enable |
| Hover / press | 140 ms | ease-out | text turns accent, 1px underline grows from the left; cards change their hairline only and never move |
| Knife draw / lyric rise-in | 350–550 ms | cubic out | already implemented (`KnifeMotion`, `LyricTrail`) |

- No bounce or spring overshoot anywhere.
- Use the existing `gui/Motion` and `AnimatedValue` helpers.
- `ClientConfig.reducedMotion()` makes every one of these instant, and `animationIntensity()` scales spatial movement.

### Data honesty

- Every value on screen is live: the real keybind ("Unbound" when none), the real toggle state, counts from
  `ModuleManager`, real ping and MOTD.
- A card with placeholder text fails review.
- Where the design shows `[placeholder]`, the build shows the real value.

### Accessibility and input

- Every action is reachable by keyboard: arrows and Tab move, Enter activates, Esc goes back.
- Hit targets ≥ 20 GUI px.
- Text contrast ≥ 4.5:1 (tertiary `#5E5A69` only for 10–11px labels on ink/sidebar, which passes 3:1 at that size;
  never for body text).

---

## 1. Main menu: board "Main menu"

**Exists today:** `gui/JilatonMainMenuScreen` (redirected from `TitleScreen` by `mixin/TitleScreenMixin`),
`gui/MenuAvatar` (player render + walk-in + head tracking), `gui/JilatonBackdrop`, `gui/MenuChrome`.
**Owner:** unassigned. O to assign.

Layout (1280×720 board coordinates):

- **Backdrop:** `#0B0A0E` with two soft radial lights:
  - violet: centre at (1010, 230), radius 450, 16% → 0
  - white: centre at (120, 640), radius 380, 5%
- **Backdrop details:** sparse 1–2px star dots at 25–50% alpha, and a bottom 180px fade to `#08070A`. No images and no
  logo wall.
- **Left column** at x=112, y=132, width 360:
  - Brand row: 52px rounded-14 mark tile (card fill, stroke, accent ring + 28px halo) holding the "J" glyph.
    `JILATON` 22px bold, letter-spacing 0.32em. Under it, "Client · Minecraft 1.21.11" 12px secondary.
  - 40px gap, then the nav (17px, rows 10px padding, 4px apart):
    - Singleplayer: right-aligned hint = the last played world name (real `LevelSummary`)
    - Multiplayer: hint "Servers · LAN"
    - Client: hint "Modules · Visuals · Music"
    - Minecraft settings
    - Quit, in secondary colour
  - **Hover / keyboard focus row:** accent text, weight 600, plus a 120px 1px accent underline with a glow that grows
    from the left. No filled buttons.
- **Right group** centred at x≈990:
  - Account chip (36px, online dot + name + chevron). It opens the Account manager (§11).
  - The player model, 400px tall, standing on an elliptical ground shadow, rim-lit from the right with a faint accent
    line. **Keep the existing walk-in and head tracking. This is the part the user likes; make it smoother:** 900 ms
    ease-out arrival, no pop at the end, head tracking eased at about 12 Hz.
  - Under it, three 28px chips: Skin · Cape · 3D character. They open the Account skin page, the Cape module and the
    character-3d module respectively.
- **Bottom-left:** music status pill (40px). "Music is off / Press M to pick a source" when `MusicCenter.source()==OFF`;
  otherwise the now-playing title and source. Clicking it opens the Music Center.
- **Bottom-right:** "Jilaton 0.1.0 / Fabric 0.20.1" in mono 11px tertiary, read from the mod metadata, not literals.

Acceptance: a harness capture at 1920×1080 and 1280×720 matches the board; the walk-in plays once per menu entry;
Reduced Motion shows the player already standing.

## 2. ClickGUI · Combat page: board "ClickGUI · Combat"

**Exists today:** `gui/ClickGuiScreen` + `gui/shell/*` (ShellLayout, ShellMetrics, ShellPalette, ModuleFlow, TextWrap).
**Owner:** C.

The skeleton is already close. Deltas to build:

- Panel 1152×640 at 64,40 over the scrim, radius 14, stroke. Sidebar 212px, `#0A090F`.
- Sidebar nav rows are 40px. The active row gets accent text plus an 8% accent wash, radius 8 (this is a deliberate
  change from DIRECTION's "no bar": a very faint wash reads better at small GUI scales).
- Count pills are mono 11px "enabled / total", from live counts.
- **Header 48px:**
  - "Hello, <name in accent>"
  - the **"Resumed where you left off" chip**, shown only when the static `Resume` state restored a page (this already
    exists in `ClickGuiScreen.Resume`). It fades out after 3 s.
  - the mono clock
  - the search field: 260×34, radius 9, with a "Ctrl F" hint
- **Page title row:** 24px title, 12px subtitle, right side "N of M active" with a 6px accent dot.
- **2-column card grid,** cards 62px, radius 12.
  - Title 13px semibold; under it the 11px line "<keybind or Unbound> · <one-line description>".
  - 40×22 toggle.
- **Expanded card** spans both columns with an accent ring. Its settings sit in a 3-column grid under a hairline:
  - Slider: 4px track, 14px white knob, accent fill, mono value on the right.
  - Choice chips: 26px; selected = 14% accent fill + 50% accent stroke.
- **KillAura "Target visual" chips** (Halo / Skeleton / Orbit / Glass) are the hook for the KillAura visual variants
  feature. Until a variant exists, show only the ones that work. Never a dead chip.
- **Footer 40px:** "Click to toggle · Right-click for settings · Drag to reorder · Esc to close".

## 3. ClickGUI · Visuals + Knife settings: board "ClickGUI · Visuals + Knife settings"

**Exists today:** Visuals category with preview cards (`VisualPreview`, `PreviewSpecs`), card order persisted
(`ui.visual_order`). **Owner:** C. The Knife settings data comes from `CosmeticKnifeModule` (F + audit session).

- **3-column grid of preview cards,** 128px tall, radius 12.
  - Title, toggle, then an 11px live summary of the key settings. Examples: "CS Katana · inspect V", "World Trail ·
    5 lines · 14 s", "Dark sunset · clouds hidden".
  - Then the miniature drawn by `VisualPreview` from the module's real settings.
  - An enabled card that is open in the drawer gets the accent ring.
- **Settings drawer** on the right: 300px, `#100E16`, left hairline. It replaces the inline expansion for Visuals, so
  the grid never reflows.
  - Header: module name 16px + close button.
  - Groups headed by tertiary labels.
  - **BLADE:** 2-column chip grid of all 7 `CosmeticKnifeModule` styles. CS skins show a small "CS" tag on the right.
    The selected one uses accent styling.
  - Rows: Replace (choice), Inspect key (key cap showing the real binding of `key.jilaton.inspect`), Animation speed
    (slider), Slash trail (toggle), Show arms (toggle).
  - Footer hint 11px tertiary: "Hold V to keep a CS knife spinning. Blade colour follows the sword you hold."
- The drawer is generic: it renders any module's `settings()` with the same row components. Knife is just the example.

## 4. In-game HUD: board "In-game HUD"

**Exists today:** `gui/HudRenderer`, `gui/hud/*`, presets Minimal/Full, HUD editor. **Owner:** E.

- **Top-left chip strip,** 26px chips, radius 7, ink at ~62%:
  - Brand chip: J glyph + "JILATON", 11px bold, 0.18em spacing
  - Server name ("Singleplayer" or the server address)
  - FPS: mono value + a 44×14 accent sparkline of the last ~30 samples
  - Clock
- **Top-right:**
  - Now-playing card: 248px, 40px art, title, source, 3px progress bar. Shown only when the Music source is not Off
    and a track is playing.
  - Below it, the enabled-module list: 12px, right-aligned, white with a soft shadow. A module may add a short
    secondary tag (e.g. "Knife CS Katana").
- **Above the target:** nametag pill (§12 nametags work).
- **Bottom-centre** above the hotbar: target card, 340px.
  - 38px head tile.
  - Name, then "3.0 m · armor 2" in mono.
  - 5px health bar, green gradient (amber below 50%, danger below 25%).
- **Bottom-right:** keystrokes, 30px keys, pressed = white key with dark text, plus a space bar. **Moved here from
  bottom-left, so it can never collide with chat** (audit JA-009).
- **Bottom-left:** coordinates only, mono 11px.
- **Hard rule:** the chat rectangle (vanilla chat bounds, bottom-left, ~380×120 at this scale) is a no-widget zone. Add
  a headless test asserting no HUD rect intersects it at the supported viewport sizes.

## 5. Motion spec: board "Motion spec"

This is the table in §0. It is a reference board, not a screen.

## 6. Multiplayer · server list: board "Multiplayer · server list"

**Exists today:** `gui/JilatonServersScreen` (extends `JilatonCardScreen`, redirected by `ServerListRedirectMixin`,
with LAN detection). **Owner:** unassigned (O).

- Full-screen backdrop (as §1), content inset 112px left and right, 64px top.
- **Header:** 36px back button, 26px title, a segmented control **Saved | LAN**, and the search field on the right.
- **Rows** 76px, radius 14:
  - 48px server icon (the real favicon; ink tile when there is none)
  - Name 15px semibold + address in mono 11px tertiary
  - MOTD 12px secondary, one line, ellipsised
  - Right side: players "online / max" in mono, a 4-bar ping meter (green ≥3 bars, amber 1–2) and the ms value
  - The selected row has the accent ring and an accent **Join** button
- **Footer actions** (36px outline buttons): Add server, Direct connect, Edit, Refresh. Hint: "Shift + ↑ ↓ to reorder ·
  Double-click to join" (both already behave this way).
- The Worlds screen (`JilatonWorldsScreen`) uses the same row component.

## 7. Music Center: board "Music Center"

**Exists today:** `gui/MusicCenterScreen`, `music/**`. **Owner:** A.

- Two columns. A soft radial glow tinted from the album art sits behind the left column.
- **Left (420px):**
  - Title "Music"
  - The **source segmented control**: Off · Spotify app · Any player · Local files · Web API. These map 1:1 to
    `MusicCenterModule.source`. Off is the default.
  - 420px album art, radius 18, with a soft drop shadow
  - Title 22px, source and device 13px
  - Progress: 4px bar + knob, mono times
  - Transport: 44px outline previous/next, 60px white play/pause
- **Right:**
  - "SYNCED LYRICS · LRCLIB" label, and on the right "Shown in your world: <mode>"
  - A lyrics panel: previous lines dim, the current line 30px white with sung words bright and unsung words secondary
    (word timing from `Lyrics`), and the next lines dim
  - Under it, 3 info tiles: Placement (spatial_lyrics mode), Lines kept (trail_lines · linger), Timing offset
- **With no track:** the art tile becomes ink with a music glyph and the lyrics panel shows one centred line: "Pick a
  source to see lyrics here".

## 8. Loading screen: board "Loading screen"

**Exists today:** `mixin/LoadingOverlayMixin`, `gui/JilatonLoadingScene`, `gui/JilatonLoadingLogo` (bar + shine).
**Owner:** unassigned (O).

- `#08070A` with a soft violet radial (r≈300) at the centre.
- 84px mark tile with an accent halo, inside a 132px **progress ring**: 2px track `#1A1722`, accent arc driven by the
  real reload progress, starting at 12 o'clock.
- `JILATON` 26px bold, 0.42em spacing, with a moving shine gradient. Reuse the existing shine sweep and move it from the
  bar to the wordmark.
- Status line: current stage + mono percent.
- Bottom-left versions in mono 11px at very low contrast (`#3A3646`).
- Keep `LoadingBrandLayoutTest`'s window-size matrix; add the ring to it.

## 9. Pause menu: board "Pause menu"

**Exists today:** vanilla `PauseScreen`, not restyled. **Owner:** unassigned (O). This needs a new
`PauseScreenRedirectMixin` (same pattern as `ServerListRedirectMixin`) and a `JilatonPauseScreen`.

- The world stays visible behind a left-to-right gradient scrim (92% → 55%).
- **Left column** at x=112, y=120:
  - World or server name (12px tertiary, 0.12em) over "Paused" 34px bold
  - Nav in the main-menu style: Back to game (focused by default), Client (shows the real ClickGUI key), Music (shows
    the real Music key), Advancements, Options, Open to LAN (singleplayer only), and "Save and quit to title" (or
    "Disconnect") in danger colour
  - Every vanilla action must still work, including Statistics and the report and feedback buttons where vanilla
    shows them: put them under an "More" row if space is short. Never drop a vanilla function.
- **Right column** (320px glass cards):
  - "ACTIVE MODULES" chips
  - The now-playing card with play/pause
  - The account card (dot + name → Account manager)

## 10. ClickGUI · Theme: board "ClickGUI · Theme" (interactive on the canvas)

**Exists today:** Theme page inside `ClickGuiScreen`, `ClientTheme`, `ClientConfig.themeId/customAccent`.
**Owner:** C.

- **ACCENT:** a 4×2 grid of 64px swatch cards, one per `ClientConfig.THEME_IDS` value (obsidian, graphite, violet,
  blue, cyan, rose, amber, custom). The swatch dot glows in its own colour; the selected card's border takes that
  colour. Custom opens the existing hex input.
- **MOTION & TYPE:**
  - Reduced motion toggle (`ui.reduced_motion`)
  - Animation intensity slider 0–1.5 (`ui.animation_intensity`)
  - Font segmented Inter | Minecraft (new key `ui.font`, additive config edit via O)
- **Live preview panel** (300px) repeats a nav row and two cards in the chosen accent, so the change is visible before
  closing. The accent applies everywhere: GUI, HUD and menus.

## 11. Account manager: boards "Account manager" and "Account manager · add account"

**Exists today: nothing.** This is a new feature. **Owner:** unassigned; O must assign it and **the user must approve
the security design below before any code is written.**

**List screen:**

- Header: back button, "Accounts", accent "Add account" button.
- **Rows** (76px, as §6):
  - 44px face tile (from the skin)
  - Name + a type badge: MICROSOFT, or OFFLINE in warn colour
  - Sub-line ("Signed in · last used …" or "Singleplayer and offline servers only")
  - Right side: "In use" (green dot) for the active account, else a "Use" outline button; then a ⋯ menu with
    Re-sign-in, Rename (offline only) and Remove.
- **Right card** (320px):
  - Skin preview (the real player model, like the main menu)
  - Name, Change skin / Cape chips
  - Facts: Type, Session ("Valid · refreshes on its own" / "Expired · sign in again"), Skin model
  - Danger outline "Sign out of this account"
- **Footer trust note:** "Microsoft sign-in happens on Microsoft's own page. Jilaton never sees your password; it keeps
  only the session token, encrypted on this PC."

**Add-account dialog** (600×500, radius 18, over a 60% scrim):

- Tabs **Microsoft | Offline name**.
- Microsoft tab = **device code flow**:
  - 8 code boxes (mono 24px) filled from Microsoft's response
  - "Open microsoft.com/link" (accent) and "Copy code"
  - A spinner line "Waiting for you to finish in the browser…"
  - Footer "No password is ever typed into Jilaton. The code expires after 15 minutes."
- Offline tab: one name field (3–16 chars, `[A-Za-z0-9_]`) and an "Add" button. It explains it works for singleplayer
  and offline-mode servers only.

**Security requirements (non-negotiable):**

- **Never** a username/password field for Microsoft accounts, and never any third-party or cracked login service.
- Microsoft OAuth **device code** flow (or an auth-code + PKCE loopback, as `SpotifyConnect` already does), then
  Xbox Live → XSTS → Minecraft Services. It needs an Azure app client ID registered by the user; ship none.
- Persist only refresh tokens:
  - encrypted at rest (Windows DPAPI via a small JNA call, or an OS keychain)
  - never logged
  - never in `jilaton.properties`
  - never in profiles exported by Configs
- Switching accounts swaps the session only from the title screen (not mid-world), through a narrow accessor on
  `Minecraft.user`.
- Remove = delete the tokens immediately.

## 12. Things the boards imply but do not draw

- **Nametags** (HUD board): a pill with name, a 40px health bar coloured by percent, and the distance in mono.
  `NametagRenderer` already exists. Restyle it to this pill.
- **KillAura target visuals** (Combat board chips): variants Halo (exists), Skeleton, Orbit, Glass. Each needs a real
  renderer before its chip appears.
- **Cape with logo:** the "Cape" chip on the main menu and account screens points at the planned logo-cape module.

---

## Build order and verification

1. Fonts + tokens (§0). This unblocks everything.
2. ClickGUI deltas (§2, §3, §10). C. Smallest change, most-seen surface.
3. HUD (§4). E. Includes the chat no-widget test.
4. Main menu (§1), then Loading (§8), Servers/Worlds (§6), Pause (§9). O assigns.
5. Music Center (§7). A.
6. Account manager (§11), only after the user approves the security design.

For each screen:

- **Headless:** layout tests at 854×480, 1280×720, 1920×1080 and GUI scales 2–4. No rect outside its panel, no
  overlapping interactive rects, no text overflow. Follow `ShellGeometryTest`'s pattern.
- **Runtime:** a harness capture (the `dev/VisualVerifier` scene pattern, or `dev/ViewmodelShots` for a quick one-off)
  compared side by side with its canvas board before the work is called done.
- Record each screen as IMPLEMENTED → BUILD_VERIFIED → RUNTIME_VERIFIED in `ORCHESTRATION.md`.
