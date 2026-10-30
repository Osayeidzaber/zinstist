# Handoff for the next agent

**Written:** 2026-10-31, when the user moved from Windows to a Mac.
**Branch:** `jilaton/2026-10-31-work`. `main` is old: do not work there, and never merge or push to `main` unless the
user asks.
**Read first, in this order:** this file → `JilatonClient/agent.md` (working rules) →
`JilatonClient/development/GUI_REDESIGN.md` (the next big job) → `audit/JILATON_AUDIT_REPORT.md` (known issues).
`JilatonClient/development/ORCHESTRATION.md` is the long log of who did what on 2026-10-30/31. Search it; don't read
it top to bottom.

---

## 1. What this project is

**Jilaton Client** is a Fabric client mod for **Minecraft 1.21.11**:
- Java 21, Fabric Loader 0.20.1, Fabric API 0.141.6, Loom 1.14.10, Mojang mappings
- 91 modules (28 combat, 15 movement, 4 player, 40 render, 4 client), a custom ClickGUI, HUD, main menu, music
  centre with in-world lyrics, 3D characters and first-person knives

**The user's taste:** premium, mostly black, minimalist, *very* smooth animation. One violet accent, used for state
only. They test everything in game and notice jank. They like the main-menu player walk-in.

**Permissions the user has granted (recorded as R1/R4 in `ORCHESTRATION.md`):**
- **Anubis** (`Anubis Beta 0.9.9.3 (1).jar`, in the repo root) is an authorised **port source**. Exact code and assets
  may be ported with package and API changes only, credited in each file's header and in `ATTRIBUTION.md`.
- The JAR itself is reference only. Never bundle it into Jilaton's jar.
- LiquidBounce, CORZ, Purixy and Larp are layout and behaviour references only. Never copy their code or assets.
- The sky textures (`assets/jilaton/textures/sky/*`) are used with the artist's permission. Never resample, move or
  delete them.

## 2. Getting it running on the Mac

```bash
git clone -b jilaton/2026-10-31-work https://github.com/Osayeidzaber/zinstist.git
cd zinstist/JilatonClient
```

Install **Temurin 21** (e.g. `brew install --cask temurin@21`). The Windows JDK the old machine used is not in the repo.

```bash
./gradlew test build
```

```bash
./gradlew runClient
```

- `runClient` writes to `run/`, which is ignored.
- The test world "Jilaton HUD verification" was **not** copied (it was in the ignored `run/`). Create any world, or ask
  the user to copy theirs over.
- Expected state on arrival: main code compiles, and every test passes **except** the Models work in progress
  (`render/character/CustomCharactersTest` does not compile; `CustomGlbImportTest` has 3 failures). Fixing those is
  task 1 below.

**Mac differences:**
- Music Center source **"Spotify app (no setup)"** is Windows-only. It reads Windows media controls through
  `assets/jilaton/scripts/media-session.ps1`. On the Mac use **Spotify Web API** or **Local files**, or build a
  macOS bridge (an `osascript` query of the Spotify app; read-only and local, like the Windows one).
- Not in the repo, because they're ignored (the user may copy them by hand):
  - `JilatonClient/reference/ui/DIRECTION.md`: the UI rulebook. Its tokens and rules are restated in
    `GUI_REDESIGN.md` §0, so you can work without it.
  - `analysis/anubis-vineflower/`: decompiled Anubis. Regenerate it from the JAR with any Java decompiler
    (Vineflower or CFR), then map names with `JilatonClient/tools/remap-anubis.py`. The script header has usage; it
    needs Loom's `mappings.tiny`, which appears in `~/.gradle/caches/fabric-loom/1.21.11/` after the first build.
  - `analysis/models/bacon-hair.glb`, `sonic.glb`: the user's CC-BY character models (see §4, task 3).

## 3. How to work (non-negotiable; full rules in `agent.md`)

- **Never** run destructive git (`reset --hard`, `clean`, `checkout -- .`, force-push). Commit work on the branch in
  small commits; push only when the user asks.
- **Verify in game, not just in tests.** Most visual bugs today were only caught by looking at real frames. For
  first-person / visual work, use the capture harness:
  - `dev/ViewmodelShots` is armed by `-Djilaton.viewmodelShots=<mode>` (modes `grips`, `anim`, `cs`). It loads a
    world, poses the knife, saves `screenshots/vm-*.png` and quits.
  - Add a Loom run config with that `vmArg` in a **local copy** of `build.gradle`, or temporarily. Do not commit a
    personal run config.
  - `dev/VisualVerifier` (`./gradlew runVisualVerify`) is the bigger scripted capture of most screens.
- **Unit tests in this repo cannot touch Minecraft registries.** Constructing a module must not call `ItemStack`,
  `Items`, `FabricLoader` paths and so on. Make such fields lazy (see `CosmeticKnifeModule.heldItem`). Breaking this
  turns ~80 tests red at once.
- Every value shown in UI must be live data. No placeholder cards, no invented numbers, no dead toggles.
- Respect `ClientConfig.reducedMotion()` in every animation.
- A test count is not proof that something works. Report levels honestly: IMPLEMENTED → BUILD_VERIFIED →
  RUNTIME_VERIFIED.

## 4. What to do next, in order

1. **Get the tree green.**
   - `CustomCharactersTest.java:28`: `assertDoesNotThrow(CustomCharacters::scan, …)` became ambiguous when `scan`
     gained an overload. Use a lambda: `() -> CustomCharacters.scan()`.
   - `CustomGlbImportTest`: 3 NPEs, "Cannot read the array length because <local14> is null", in the GLB bake path.
     Debug `CustomCharacters`/`CharacterBake` with the user's `bacon-hair.glb`.
2. **Build the GUI redesign** from `JilatonClient/development/GUI_REDESIGN.md`.
   - The designs are in `Jilaton GUI Redesign.html` at the repo root. Open it in a browser; it works offline.
   - Follow the spec's build order: fonts and tokens → ClickGUI (Combat, Visuals drawer, Theme) → HUD (keep chat
     clear) → main menu (keep and smooth the player walk-in) → loading → servers/worlds → pause → Music Center.
   - **Account manager:** do not start it until the user approves the security design in §11 (Microsoft
     device-code sign-in only, no passwords ever, encrypted tokens).
3. **Custom `.glb` characters:** the selection plumbing is done (`ChoiceSetting.deferUnknownValues/setChoices`,
   `CharacterModule.refreshCharacters`). After task 1, verify in game:
   - drop `bacon-hair.glb` into `<minecraft dir>/config/jilaton/characters/`
   - enable "3D Character", pick it, restart, and check it's still selected
   - before any model ships in a jar, add its CC-BY credit to `ATTRIBUTION.md`
4. **Visual features the user asked for that are not built yet:**
   - **Nametags** that look premium: pill with name, health bar and distance. See `GUI_REDESIGN.md` §12;
     `render/NametagRenderer` exists.
   - **KillAura target visuals with several variants:** Halo (exists), Skeleton, Orbit, Glass, selectable in
     KillAura's settings.
   - **Landing shockwaves:** a ring that ripples outward across the ground when the player lands.
   - **Block-place glow:** an animated outline and glow on blocks as they are placed.
   - **Glass:** players and held items rendered as glass (translucent, refractive-looking tint plus fresnel edge).
   - **Cyberspace:** a full-screen cyber colour grade and scanlines, plus glowing **skeletons** inside player models
     (reuse for the KillAura "Skeleton" variant).
   - **Logo cape:** a cape showing the Jilaton J, with cloth-like motion.
   - **Katana two-handed in Jilaton's own knife set:** the CS Katana (Anubis port) already holds two-handed. Extend
     the idea to other held weapons if the user wants.
5. **Remaining audit items** (`audit/JILATON_AUDIT_REPORT.md`):
   - JA-004: refresh `build-verification.json` and the inventory/README counts from a real build
   - JA-008: profile the per-frame entity scans before changing them
   - JA-009: re-check HUD overlaps after the redesign
   - JA-021: two local Windows branches held decompiled code. They were never pushed, so nothing to do on the Mac.
   - JA-022: the remaining Gradle deprecation is inside Loom 1.14.10; upgrading Loom is a deliberate decision

## 5. Map of the recent work (2026-10-31)

| Area | Where | Notes |
| --- | --- | --- |
| World Trail lyrics | `render/fx/LyricTrail.java`, `module/impl/SpatialLyricsModule.emitTrail` | Each lyric line is placed where you look, stays in the world, and is drawn as extruded 3D text. Default mode. |
| Knife viewmodel (Jilaton blades) | `module/impl/CosmeticKnifeModule`, `mixin/KnifeViewmodelMixin`, `mixin/ItemInHandRendererInvoker`, `render/model/KnifeMotion`, `render/model/HeldModelRenderer` | Your real arm holds the knife. Draw / slash / inspect (key V). Grip tuned from captures (`CosmeticKnifeModule.grip`, `handScale`). |
| CS knives (Anubis port) | `render/knife/*`, `mixin/CsKnifeMixin`, `textures/misc/knife_base.png` | Butterfly, Karambit, two-handed Katana. Chosen via the Knife module's "Blade" list (`CS …` entries). |
| ClickGUI memory | `gui/ClickGuiScreen` (`Resume`, `ui.visual_order`) | Reopens where you left off; card order is saved. |
| Settings safety | `config/ClientConfig` | An unreadable config is backed up, never silently overwritten. |
| Music defaults | `module/impl/MusicCenterModule` | Source defaults to **Off**, so nothing runs or goes online until the user picks a source. |
| Fixes | see the audit report's JA-002 … JA-023 and `ORCHESTRATION.md` "Audit session" blocks | TargetStrafe gate, lyric retry, Spotify callback, AutoRod raycast, sky UV inset, and more |

The older status write-up (`JilatonClient/info.md`, dated 2026-10-30) is kept for history. This file supersedes it
for "what next".
