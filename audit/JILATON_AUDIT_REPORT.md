# Jilaton Client — Codebase Audit Report

**Audit window:** 2026-10-31 00:15–00:27 (local)  
**Auditor:** independent audit session (Claude Code); not one of the registered agents A–I or O  
**Scope:** whole `JilatonClient` tree, the current Git state, and all uncommitted work from the parallel agents  
**Mode:** read-only. No project file was modified. Every build ran on a private snapshot copy in the auditor's scratchpad, never in `build/` or `.isolated-build*`. No client was launched and no process was stopped.

---

## A. Executive summary

**Overall condition.** The codebase is in better structural shape than its records suggest. At 00:26:03 the live tree compiles and passes **612 tests in 140 suites with 0 failures, 0 errors and 0 skipped**. The mixin registry, module registry, settings wiring, config writes, Spotify OAuth, and asset hygiene all check out (§C, "verified sound"). The serious risks are elsewhere:

1. **Data-loss exposure (CRITICAL, process).** Everything built since the last commit is uncommitted: 316 untracked source files and 62 modified tracked files, in one checkout that 8+ agents write to at once. The newest recovery tarball is from 21:09, and **216 of the 583 source files have changed since then**. One `git clean`, `checkout --` or bad extract would lose most of a day's work.
2. **Regression: TargetStrafe is dead in normal play (HIGH).** H's 23:32–23:40 policy extraction inverted the GUI gate. The module now engages only while a screen is open. The policy test passes because it tests the policy, not the call site.
3. **Custom 3D characters can't be selected (HIGH).** The drop folder is scanned every 2 s, but the "Character" setting's choices are fixed when the module is constructed. A user `.glb` (for example the staged `bacon-hair.glb`) can never be chosen.
4. **Privacy-relevant default (MEDIUM).** Music Center is on by default with source "Spotify app". On every Windows launch, without user action, it starts `powershell.exe -ExecutionPolicy Bypass` and sends now-playing title and artist to LRCLIB and iTunes.
5. **The records are stale and contradict each other (MEDIUM).** `build-verification.json` says 383 tests and 6,065,306 B with `stale:false`. Reality is 612 tests and 8,346,709 B. `CHECKPOINT.md` holds nine different "current" test totals. The inventory documents 52 of 91 modules.
6. **Runtime evidence predates most features.** The newest in-game capture is from 22:43. Wings, halo, knife, companion, 3D characters, the M21 render fixes, the GUI word-wrap fix and the HUD rewrite have never been seen in game.

**Verified build state:** BUILD_VERIFIED at 00:26:03, with 140/612/0/0/0 and jar 8,346,709 B, SHA-256 `29ffc32bdefbb7192e83fe5a66b89fd681f4da0bd173b9c652ddd9dd8b143d75`. Nothing is RUNTIME_VERIFIED in this audit.

---

## B. Recent changes review

### B.1 Git state (observed 00:15)

| Item | Finding |
|---|---|
| Branch | `main` at `f11bfff`; `origin/main` identical. The remote has **only** `main` (verified with `git ls-remote`). |
| Local branches | `codex/workspace-before-cleanup` and `codex/clean-upload-before-history-restore`. Both contain `25edf9a`, a 4,589-file commit holding `analysis/anubis-decompiled`, LiquidBounce trees and similar. **Never pushed. They must never be pushed** (JA-021). |
| Worktrees / stashes | One worktree, zero stashes, no `index.lock`. |
| Modified tracked | 62 files, +5,952 / −1,495 lines. Nothing staged. No deletions or renames. |
| Untracked | 182 top-level paths and **316 untracked source files** (`--untracked-files=all`). This includes all of `render/character/**`, `render/model/**`, `render/fx/**`, `music/**`, `gui/shell/**`, `gui/hud/**`, 61 new mixins, 39 render/visual modules, the sky textures and the `.anc` assets. |
| Recovery | `.git/snapshots/worktree-20261030-2110.tar.gz` (21:09) is the newest. 216 of 583 source files are newer than it. |
| Nested repos | `analysis/liquidbounce-upstream/.git` only. It is ignored by `/analysis/`. |
| Active during audit | Gradle daemon PID 28292, started 00:11 (an agent's). Writes after 00:00: `SpatialLyricsModule.java` 00:14 and `SpatialLyricsFitTest.java` 00:15 (A); `AssetMeshTest` 00:09, `KnifePoseTest` 00:10→00:17 and `CosmeticKnifeModule` 23:58 (F); `MovementWorldSpaceInvariantsTest` and `TargetStrafePolicyTest` 00:01 (H). |

### B.2 Diff review of tracked files (read in full)

| File(s) | Change | Verdict |
|---|---|---|
| `JilatonClient.java` | DISCONNECT hook (freecam off, target tracker and FX clear), Music key `M`, `MenuChrome`, `VisualVerifier.registerIfRequested`, `WorldOverlayProjection`, damage/nametag renderers, GUI toggle through `onClose()`, music shutdown on stop | Sound. Saving before `disableAll()` on stop is the correct order. |
| `ClientModule.java` | `persistEnabled()` hook (used only by Freecam) | Sound. Snapshot and apply both respect it. |
| `ModuleManager.java` | +39 registrations, now 91 | No duplicate ids (the constructor throws on one). Every module class is registered. |
| `ClientConfig.java` | 17 HUD widget ids, HUD styles, reduced motion, animation intensity | Atomic temp+move writes are good. Read-failure handling is weak: **JA-007**. |
| `jilaton.mixins.json` | +45 entries, now 73 | 73/73 match `@Mixin` classes. No orphans, no duplicates. |
| `build.gradle` | `visualVerify` run config (`run-verify/`, `-Djilaton.visualVerify`) | Sound. Dev-only. |
| `fabric.mod.json` | Adds `icon` | Asset exists. |
| `KillAura*`, `AnchorAura*`, `AutoRod*`, `BowAim*` (G) | Yaw-wrap and compare logic extracted into testable helpers | Behaviour preserved, verified line by line. One small cost change: **JA-020**. |
| `StrafeMath`, `VelocityStrafeMath` (H) | Sideways sign now matches vanilla `moveRelative` (left = +x when facing south) | **A correction, not a regression**: the old `atan2` form mirrored strafes. Pinned by `MovementWorldSpaceInvariantsTest`. |
| `StrafeModule` (H) | Extracted to `StrafePolicy` | Arguments match positionally. Sound. |
| `TargetStrafeModule` (H) | Extracted to `TargetStrafePolicy` | **Regression, JA-002.** |
| `FlightModule`/`FlightPolicy` (H) | Restore logic extracted | Equivalent to the old code. |
| `ZoomModule`, `ZoomFovMixin` | Zoom factor now computed against the CustomFov base | Coherent. No double application (§C). |
| `ClickGuiScreen.java` (C) | +2,343/−… rewrite to the CORZ shell | Saves on release, not per drag. The reorder is not persisted: **JA-013**. |
| `HudRenderer.java` (E) | +1,091 lines (Full preset, radar, staff, above-head) | No compile issues. Collisions seen in the last capture: **JA-009**. |
| `EntityOverlayRenderer`, `OverlayRenderTypes` (B) | Sky cube pass, FX frame, immutable extraction→draw split | Sound architecture (§C). |
| `development/*.json`, `CHECKPOINT.md`, `IMPLEMENTATION_INVENTORY.md` | Rewritten by several sessions | Stale and contradictory: **JA-004**. |
| `.gitignore` | Adds `run-verify/` | `.isolated-build*` is still not ignored: **JA-014**. |

### B.3 Defects or conflicts introduced by recent edits

- **Introduced:** JA-002 (TargetStrafe, H, 23:32–23:40).
- **Transient, now resolved:** `KnifePoseTest` did not compile from 00:10 to 00:17. It referenced `DRAWN_LENGTH`, `MIN_SCALE` and `MAX_SCALE`, which are `private` in `CosmeticKnifeModule`. F fixed it at 00:17 with test-local copies (see JA-019).
- **Left behind by a superseded task:** JA-011 (M26 procedural characters are still compiled and still built at runtime).
- **Not found:** no accidentally overwritten code, no reverted fixes, no duplicate managers, no duplicate event handlers, no duplicate registrations.

---

## C. Architecture assessment

**Platform:** Minecraft **1.21.11**, Fabric Loader **0.20.1**, Fabric API **0.141.6+1.21.11**, Loom **1.14.10** (`fabric-loom-remap`), official Mojang mappings, Java **21** (Temurin 21.0.12.1+1 from `analysis/toolchains`), Gradle **9.8.2**. The wrapper distribution is pinned by SHA-256. There are no third-party runtime dependencies beyond Fabric (MixinExtras `@WrapOperation` comes from the Loader).

**Module system.** There are 91 modules: 28 Combat, 15 Movement, 4 Player, 40 Render, 4 Client. `ClientModule` is robust. Every callback (tick, movement, move, packet) is wrapped. A module that throws is logged and disabled instead of crashing the client. A failed enable rolls back. No `onEnable` dereferences `player` or `level` unguarded, so reloading enabled modules at startup is safe.

**Rendering.** The design is correct: an immutable extraction phase (`END_EXTRACTION`) feeds a draw phase (`END_MAIN`) that never touches the world. All world-space drawing is camera-relative and interpolated with `getPosition(partial)`. Depth modes are expressed as immutable `RenderPipeline`s (LEQUAL vs NO_DEPTH, no depth write), so the old GL state-leak class of bug can't occur. The sky is a textured cube at `rd·16·1.6+48` blocks, depth-tested LEQUAL without depth write. That sits beyond the terrain distance (≈`rd·22.6`) and inside the far plane (≈`rd·64`), so the math is consistent.

**Which "3D" features are real geometry:**

| Feature | Implementation | Verdict |
|---|---|---|
| Wings, Halo, Companion | `ModelBatch` + `Meshes` (lofted meshes) via `FxEmitter` | Real 3D |
| Knife | `AssetMesh` from `Knives` (bayonet, karambit, butterfly, tanto) | Real 3D, **but drawn on the world-overlay pass, not as a viewmodel** (JA-024) |
| 3D Character | Anubis port: `.anc` baked skinned meshes, glTF baker, `kit/` builders | Real 3D. `.anc` files are byte-identical to the Anubis JAR. |
| TargetESP, Trails, Jump circles, KillAura halo | `EffectBatch` world-space quads and line segments | Real world-space geometry, not screen-space |
| Spatial lyrics | `Font.drawInBatch` with a world pose | World-space text |
| Sky | Textured cube, screen blend | Real |

**Verified sound** (checked directly; no action needed):
- Mixins: 73 config entries = 73 `@Mixin` classes. FOV chain (`ZoomFovMixin` → CustomFov → Zoom factor) applies once.
- All `modules.get("…")` string lookups resolve to real ids. All 23 lang keys exist.
- All 395 field-declared settings are actually read. No dead toggles were found at the field level.
- Config writes are atomic (temp file, then `ATOMIC_MOVE`).
- Spotify: Authorization Code + PKCE (S256), `state` verified, loopback-only bind, refresh token only.
- Artwork textures: a 12-entry LRU that releases evicted textures.
- Replacement Servers/Worlds screens keep the vanilla flows (add, edit, delete, direct join, LAN). The LAN detector is interrupted in `removed()`.
- Release jar: no `net/anubis`, LiquidBounce or decompiled classes, no `.py`, no `.glb`. Sky set intact at **5,150,221 B** (2048×1365 ×4, 1024×683 ×3), matching R1.

---

## D. Confirmed issues

Severity follows the brief's scale. Status is CONFIRMED (proven from code or artifacts), LIKELY, or REQUIRES_RUNTIME_VERIFICATION.

### JA-001 — All of today's work is uncommitted in a shared checkout
- **Severity:** CRITICAL (data-loss exposure) · **Status:** CONFIRMED
- **Evidence:** `git status`: 62 modified and 316 untracked source files. The newest tarball is `.git/snapshots/worktree-20261030-2110.tar.gz` (21:09). `find src -newermt "2026-10-30 21:09"` returns 216 of 583 files.
- **Root cause:** the registry policy is "never run destructive git commands". That is correct, but nothing replaced commits as the backup.
- **Impact:** a single `git clean -fd`, `git checkout -- .`, mis-aimed tarball extract or disk fault loses `render/character/**`, `render/model/**`, `music/**`, `gui/shell/**`, the 61 new mixins and the 39 render modules. B already deleted untracked PNGs once (Incident 1).
- **Recommended fix (non-destructive):** (1) O takes a fresh tarball now, with the same exclusions as the 21:09 one. (2) After the user approves, O commits on a new branch (e.g. `wip/2026-10-30-integration`), listing paths explicitly rather than using `git add -A`, and only after JA-014 is fixed. Do not push without the user's decision.
- **Owner:** O · **Verify:** the tarball lists ≥583 `src` entries; `git status` is clean on the WIP branch.

### JA-002 — TargetStrafe engages only while a GUI is open (regression)
- **Severity:** HIGH · **Status:** CONFIRMED
- **Evidence:** `TargetStrafePolicy.java:20` declares `eligible(boolean hasLevel, boolean screenOpen, …)` and returns false when `screenOpen` is true. That is pinned by `TargetStrafePolicyTest.java:39`. Both call sites pass the opposite value: `TargetStrafeModule.java:76` and `:114` pass `client.screen == null`. The pre-refactor code required `client.screen == null`.
- **Root cause:** a boolean argument inverted during the extraction (H, 23:32/23:40).
- **Impact:** in normal play (`screen == null`) the module is never eligible: no orbit input and no motion. With a menu open, it would run.
- **Fix:** pass `client.screen != null` at both sites, or rename the parameter to `noScreen` and flip the policy. Add one module-level test that drives the real call-site argument.
- **Owner:** H (movement lane). The files have not changed since 23:40, but confirm H is idle first.
- **Verify:** unit test on the call-site expression, then in-game: KillAura target within follow range, no screen open, the strafe orbits.

### JA-003 — Custom `.glb` characters can never be selected
- **Severity:** HIGH · **Status:** CONFIRMED
- **Evidence:** `CharacterModule.java:45` builds the `character` `ChoiceSetting` once, from `CharacterCatalog.names()` at construction time (`:62-65`). That list is the nine built-ins. `ChoiceSetting.java:15` copies the choices immutably and has no mutator. `CharacterModule.onTick` calls `refreshCharacters()` every 40 ticks (`:117-119`), which updates `CharacterCatalog.designs` but never the setting. `ClientConfig` would also reject a persisted custom name ("Unsupported choice").
- **Impact:** the M29/M32 drop folder (`config/jilaton/characters/*.glb`) is scanned and baked but can't be reached from the GUI or the config. The staged `analysis/models/bacon-hair.glb` can't be worn.
- **Fix:** give `ChoiceSetting` a dynamic choice source, such as a `Supplier<List<String>>` or `setChoices(List)` that preserves the current value. `setting/**` is a shared API with no listed owner, so O defines it. I then refreshes the choices in `refreshCharacters()`. Add a test that drops a fake design, refreshes, and asserts it is selectable and survives a save/load.
- **Owner:** O (setting API), then I (`render/character/**`) · **Verify:** headless test; then in game, drop `bacon-hair.glb`, wait 2 s, select it in ClickGUI, restart, and confirm it is still selected.

### JA-004 — Verification records and docs are stale and contradict each other
- **Severity:** MEDIUM · **Status:** CONFIRMED
- **Evidence:**
  - `development/build-verification.json` claims 383 tests, 109 suites, 6,065,306 B, SHA `2bf2ad8d…`, 80 modules and `"stale": false`. **No jar with that hash exists on disk.** Actual: 612/140, 8,346,709 B, 91 modules.
  - `development/CHECKPOINT.md` records 259, 334, 335, 341, 346, 350, 359, 367 and 560 as results.
  - `README.md:122` says "259 automated tests passing". `JilatonClient/README.md:5` says "52 modules".
  - `IMPLEMENTATION_INVENTORY.md` has 55 rows (52 modules plus 3 non-module features). 39 registered modules are absent, including `custom_sky`, `wings`, `halo`, `cosmetic_knife`, `character-3d`, `nick_hider`, `freecam` and `target_esp`.
  - `integration-manifest.json` lists 66 modules.
  - The shared jars are `.isolated-build/root` 8,342,850 B (23:13, `f88023f5…`), `-m21` 8,342,055 B (22:57) and `-music` 6,262,669 B (22:16). None of them is the recorded artifact.
- **Impact:** any agent or user quoting the records is wrong. The `stale:false` flag is actively misleading.
- **Fix:** after the next quiet-tree gate, O regenerates `build-verification.json` from that gate's XML and jar hash and sets `stale` from an mtime check. Collapse CHECKPOINT totals into one dated "Current gate" line. Add the 39 missing inventory rows. Update both READMEs.
- **Owner:** O (records). Docs could go to E under RELEASE-MANIFEST-1 if O delegates.
- **Verify:** the JSON's SHA-256 matches `sha256sum` of the named jar.

### JA-005 — Music Center starts a PowerShell bridge and contacts third parties by default
- **Severity:** MEDIUM · **Status:** CONFIRMED
- **Evidence:**
  - `MusicCenterModule.java:46` is `enabledByDefault() = true`. `:22` sets the default source to "Spotify app (no setup)". `onTick` (`:56-75`) runs from the first client tick, including the title screen.
  - `MediaSessionSource.java:77-79` launches `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File <config>/jilaton/media-session.ps1`.
  - `OnlineLyrics` sends title, artist and album to `lrclib.net` when `online_lyrics` is on (default `true`, `:23`).
  - `MediaSessionSource.java:128` sends title and artist to `itunes.apple.com` (`CoverLookup`) **regardless of the online setting**.
- **Impact:** listening activity reaches two third parties without opt-in. An `-ExecutionPolicy Bypass` child process on every launch may trip AV or EDR heuristics. If PowerShell is blocked, the restart loop (JA-017) repeats forever.
- **Policy note:** `SpotifyConnect` (Web API) correctly sets `lyricsAllowed=false` under Spotify's Developer Policy. The media-session path sets `lyricsAllowed=true` for the same Spotify app. The media session is not Spotify Platform, so this is probably not a policy breach, but the code's own compliance comment is inconsistent with it. This needs a decision from the owner, not a code guess.
- **Fix:** default the source to "Off" or show a first-run choice; gate `CoverLookup` behind the same online-lookups toggle; record the policy decision.
- **Owner:** A (`music/**`, `MusicCenterModule`), after LYRIC-WIRE-1 · **Verify:** fresh config, launch, no `powershell.exe` child and no outbound request until the user picks a source.

### JA-006 — One failed lyric lookup blocks all lookups for 30 s
- **Severity:** MEDIUM · **Status:** CONFIRMED
- **Evidence:** `OnlineLyrics.java:37` uses a single-thread `worker`. On failure, `:91` submits `Thread.sleep(30_000)` to that same worker.
- **Impact:** after any network error, lookups for every following track queue behind a 30 s sleep. Lyrics appear late or not at all during a track change.
- **Fix:** record a retry deadline (`retryAfter.put(key, now+30s)`) or use a `ScheduledExecutorService`. Never sleep on the worker.
- **Owner:** A · **Verify:** unit test with a failing fetcher, then a second key resolves without delay.

### JA-007 — An unreadable config silently resets, can crash at startup, and is then overwritten
- **Severity:** MEDIUM · **Status:** CONFIRMED (code path); crash case REQUIRES_RUNTIME_VERIFICATION
- **Evidence:** `ClientConfig.java:56-57`. `read()` returns `null` on `IOException`, which leads to `apply(new Properties())` and resets everything. The next `save()` from any GUI action overwrites the user's file with defaults. `Properties.load` (`:317`) throws `IllegalArgumentException` on a malformed `\uXXXX`. That is not caught, so it propagates out of `onInitializeClient`.
- **Impact:** a transient lock or AV scan, or one bad byte, loses all module, keybind and HUD settings, or stops the client from starting.
- **Fix:** catch `IllegalArgumentException` too; on any read failure copy the file to `jilaton.properties.bad-<ts>` and set a "do not overwrite until the user saves" flag.
- **Owner:** O (`config/ClientConfig` is a contested file, additive edits only) · **Verify:** unit test with a malformed escape and with an unreadable file.

### JA-008 — Several FX emitters scan every loaded entity every frame
- **Severity:** MEDIUM · **Status:** REQUIRES_RUNTIME_VERIFICATION (unmeasured risk)
- **Evidence:** per-frame `emit()` loops over `level.entitiesForRendering()` in `AfterglowModule.java:73`, `RadianceModule.java:57`, `ExplosiveHighlightsModule.java:88` and `PearlTrajectoryModule.java:148`, plus `LivingOverlayOptions.java:56` (ESP and tracers). Per-tick scans: `HitEffectsModule.java:131`, `DamageParticlesModule.java:45`, `NametagsModule.java:93`.
- **Impact:** with several modules on and a large loaded entity count, this is k full scans per frame. ExplosiveHighlights and PearlTrajectory need only crystals and pearls, which `getEntitiesOfClass(type, AABB, …)` would find spatially. The one FPS sample (23 FPS in `verify-20-hud-full.png`, harness at 1920×1080) can't be attributed to this.
- **Fix:** measure first (Spark or a `nanoTime` probe around `fxFrame`). If confirmed, build one per-frame candidate list in `FxFrame` and switch the typed searches to AABB queries.
- **Owner:** B (`render/fx/**` and those modules) · **Verify:** frame-time delta at about 500 entities with all FX on.

### JA-009 — HUD layout collisions in the last capture
- **Severity:** MEDIUM · **Status:** REQUIRES_RUNTIME_VERIFICATION (the capture predates E's 22:47 rewrite)
- **Evidence:** `run-verify/screenshots/verify-20-hud-full.png` (22:43): the keystrokes widget (W/A/S/D/SPACE) overlaps chat lines at bottom-left; the radar's "32 m" label sits directly on the MAIN HAND "IRON INGO" label; the "IRON INGOT" truncation (registry D2).
- **Fix:** re-capture. If it reproduces, keep corner stacks clear of the chat rectangle and keep the radar label inside its own panel.
- **Owner:** E · **Verify:** new `20-hud-full` capture at 1920×1080 and 1280×720.

### JA-010 — No runtime evidence for most features built today
- **Severity:** MEDIUM · **Status:** CONFIRMED (evidence gap)
- **Evidence:** the newest file in `run-verify/screenshots/` is from 22:43. Never captured in game: wings, halo, knife, companion, 3D character, the five RENDER-FINDINGS-1 fixes, the D1 word-wrap fix (`verify-15-clickgui-visuals.png` still shows "en abled", "came ra", "ci rcle" mid-word cuts) and the HUD rewrite. `verify-07-sky-13000-galaxy.png` shows the orange dusk base sky with no galaxy layer. That is either naming drift or a real defect, and it is already with B.
- **Integration hazard to watch:** `MixinCharacterLivingEntityRenderer` nulls the vanilla body render type and filters layers when a 3D character is worn. F's wings, halo and knife anchor to vanilla body positions, so they may float off a differently shaped character model.
- **Owner:** B (one harness pass, single client, O-scheduled), then O · **Verify:** fresh captures newer than the newest source file.

### JA-011 — The superseded M26 character geometry is still compiled and built at runtime
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `render/model/asset/Characters.java` (procedural `tun_tun_sahur`, `sentryOrb`, `jackalCub`) is registered in the `ModelAssets` static initializer (`:29-31`). So the first `ModelAssets.knife(...)` call also builds three character meshes. `characterIds()` (`:49`) has no caller anywhere. A second TungTungSahur also exists in `render/character/designs/` (M29).
- **Fix:** I removes `Characters` and its registration, with O's approval because M29 superseded M26. Knives stay.
- **Owner:** I · **Verify:** `ModelAssets` loads only knives; tests stay green.

### JA-012 — The Anubis rename is incomplete
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** logger `"Anubis"` and the `[Anubis]` log prefix (`CharacterCache.java:27`, `CustomCharacters.java:31`); the resource reloader id `anubis:character_textures` (`CharacterCache.java:47`); texture ids `anubis:character/...` (`:155`); 37 `anubis$` mixin handler prefixes.
- **Impact:** logs are misattributed. A reloader-id collision is possible if the Anubis mod is ever installed alongside.
- **Fix:** rename to the `jilaton` namespace, which R4 allows ("package renames and API adaptation").
- **Owner:** I

### JA-013 — ClickGUI drag-to-reorder is lost on close
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `ClickGuiScreen.java:91` keeps `visualOrder` as an instance field. A new `ClickGuiScreen` is built every time the GUI opens (`JilatonClient.java`, `openGui` handler). There is no config key for it.
- **Fix:** persist it as a `ui.visual_order` key (an additive config edit through O), or remove the drag affordance.
- **Owner:** C (+ O for the config key)

### JA-014 — Build output directories are not ignored
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `.isolated-build/`, `.isolated-build-m21/`, `.isolated-build-music/` and `.wrapcheck-descriptions.txt` all show as `??`. Neither `.gitignore` covers them.
- **Impact:** a broad `git add` for JA-001 would sweep in test XML, refmaps and tmp data.
- **Fix:** add `.isolated-build*/` and `.wrapcheck-*.txt` to `JilatonClient/.gitignore` **before** JA-001's commit.
- **Owner:** O

### JA-015 — The dev harness ships in the release jar
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** the jar contains `dev/jilaton/client/dev/VisualVerifier*.class`. It is gated by `-Djilaton.visualVerify` (`VisualVerifier.java:61`). It toggles modules and deletes profiles named `verify-a/b/c` (`:622-628`).
- **Fix:** exclude `dev/**` from the release `jar` task. **E's RELEASE-MANIFEST-1 already covers this, so do not duplicate it.**
- **Owner:** E (existing task)

### JA-016 — Spotify token storage and callback edge case
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** the refresh token is written in plaintext and non-atomically (`SpotifyConnect.java:166-167`). A callback with `state` but no `code` reaches `Map.of(..., "code", null, ...)` (`:132`→`:148`), which throws an NPE in the login thread. `authorizing` is reset, but the status stays "Waiting for approval…".
- **Fix:** temp+move write; reject a missing `code` with a 400 and a status message.
- **Owner:** A

### JA-017 — The media bridge retries forever on the render thread
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `MediaSessionSource.tick()` (`:60-62`) restarts every 5 s with no backoff or cap. `start()` copies the script and spawns a process on the client thread (`:65-83`).
- **Fix:** exponential backoff, a give-up state shown in Music Center, and process start off-thread.
- **Owner:** A

### JA-018 — The sky UV inset is hard-coded for 1024×683
- **Severity:** LOW · **Status:** CONFIRMED (seams are cosmetic: REQUIRES_RUNTIME_VERIFICATION)
- **Evidence:** `FxFrame.java:84-85` uses `0.5/1024` and `0.5/683`. `day`, `night`, `stars` and `sun` are 2048×1365, so their inset is one texel, not a half. This contradicts the registry rule "the renderer imposes no pixel size".
- **Fix:** derive the inset from the bound texture's size.
- **Owner:** B

### JA-019 — KnifePoseTest duplicates production constants
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `KnifePoseTest.java:12-14` re-declares `0.34`, `0.70` and `1.30`, which live as `private` constants in `CosmeticKnifeModule.java:43…`. The test won't catch drift. It also failed to compile from 00:10 to 00:17.
- **Fix:** move the constants to `KnifePose` (package-visible) and reference them from both.
- **Owner:** F

### JA-020 — AutoRod raycasts before the cheap rejects
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** `AutoRodModule.java:190-192` evaluates `self.hasLineOfSight(candidate)` as an argument, so it runs for every in-range player. Before G's refactor, the health check short-circuited it.
- **Fix:** pass a `BooleanSupplier`, or check health and FOV first.
- **Owner:** G (trivial; bundle with SHIELD-MODEL-1)

### JA-021 — Local branches contain decompiled third-party trees
- **Severity:** LOW (HIGH if pushed) · **Status:** CONFIRMED
- **Evidence:** `25edf9a` (4,589 files, including `analysis/anubis-decompiled/**`) is reachable only from `codex/workspace-before-cleanup` and `codex/clean-upload-before-history-restore`. The remote has `main` only.
- **Fix:** never push these branches. Deleting them is the user's call; this audit did not touch them.
- **Owner:** user / O

### JA-022 — Gradle deprecations
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** "Deprecated Gradle features were used … incompatible with Gradle 10" on every run.
- **Owner:** O (build). Run `--warning-mode all` once to list them.

### JA-023 — The build protocol and practice have diverged
- **Severity:** LOW · **Status:** CONFIRMED
- **Evidence:** the registry names one shared `.isolated-build/`. In practice there are three directories plus private `/tmp/jilaton-a/isolated.gradle` scripts (CHECKPOINT M27). That divergence is what stopped gates corrupting each other at 22:5x.
- **Fix:** formalize per-session build dirs, e.g. `.isolated-build-<mark>/`, in the protocol.
- **Owner:** O

### JA-024 — The cosmetic knife is not a real viewmodel
- **Severity:** MEDIUM (incomplete feature) · **Status:** CONFIRMED (self-documented)
- **Evidence:** the `CosmeticKnifeModule.java:32-36` javadoc says the knife hangs off the world-overlay path. Terrain closer than the model can occlude it, the vanilla hand draws over it, and it does not follow the swing animation. A true viewmodel needs an `ItemInHandRenderer` hook. `ViewModelMixin` already targets `renderArmWithItem`, but neither it nor `ViewModelModule` appears in any ownership list.
- **Fix:** O assigns ownership of `ViewModelMixin`/`ViewModelModule`. F then submits the knife inside the hand pass. Coordinate with `MixinCharacterAvatarRenderer.renderHand`, which cancels the vanilla hand when a 3D character is worn.
- **Owner:** O (ownership), then F

---

## E. Agent conflict assessment

| Area | Observation | Risk | Resolution |
|---|---|---|---|
| Ownership vs disk | All the edits examined sit inside their owners' lanes. No cross-lane overwrite was found. `CharacterModule` is in I's `render/character/**`; only its registration line is in the contested `ModuleManager`. | Low | — |
| `ChoiceSetting` (setting API) | JA-003 needs an API change in an unowned shared package | Medium | O owns the API change, then hands it to I |
| `ViewModelMixin` / `ViewModelModule` | In no owner list. Needed by F (JA-024). `MixinCharacterAvatarRenderer.renderHand` (I) cancels the vanilla hand. | Medium | O assigns one owner for the first-person hand pass |
| Player-model pipeline | I hides vanilla body and layers; F anchors cosmetics to vanilla body; B's `PharaohHeaddress` is force-disabled by `CharacterModule.onEnable` (one-way; enabling the headdress later doesn't disable the character) | Medium (runtime) | Runtime pass (JA-010); a shared "body anchor provider" contract from F, relayed to I by O |
| Character systems | I's M26 `render/model/asset/Characters` and I's M29 `render/character/**` both provide TungTungSahur | Low | JA-011 |
| Zoom ↔ CustomFov | `ZoomModule` now reads `CustomFovModule`. Neither is in an owner list. | Low | Record an owner |
| Contested records | `CHECKPOINT.md` rewritten by several sessions; nine totals | Medium | JA-004; enforce the "write only in your `##` section" rule |
| Concurrent builds | Three build dirs and a live daemon (PID 28292) during the audit | Medium | JA-023; the six parallel dispatches at 23:26 make simultaneous gates likely |
| `VisualVerifier.java` | Contested. Its card height of 58 vs 64 is still queued for B | Low | Already queued (RENDER-FINDINGS-1) |

No duplicate managers, competing config writers, duplicate event registrations or conflicting Gradle edits were found. `ClientConfig` has one writer class, and every GUI save goes through it.

---

## F. Incomplete features

| Feature | State | Gap |
|---|---|---|
| Custom GLB characters | Scan and bake implemented | Not selectable (JA-003) |
| First-person knife | 4 meshes and pose | Not in the hand pass (JA-024) |
| ClickGUI module reorder | Drag works in-session | Not persisted (JA-013) |
| Spatial lyrics fitting | `LyricLayout.fitText` exists | Not wired yet; A's LYRIC-WIRE-1 is in progress (`SpatialLyricsModule` 00:14). Do not reassign. |
| KillAura shield model | Head-vector dot product only | G's SHIELD-MODEL-1 is in progress |
| Spotify Web API transfer and playback | Implemented | Never runtime-verified. Needs the user's own account authorization. |
| Galaxy sky theme | Theme registered | The 22:43 capture shows no galaxy layer (JA-010, B) |
| Idea 012 bots | — | Blocked on the user's definition (registry) |

No empty methods, `UnsupportedOperationException` stubs (except the intended `BakedDesign` "has no rig"), "coming soon" buttons, or empty button handlers were found in `gui/**`.

---

## G. Performance and security findings

**Confirmed (code evidence):** JA-006 (lyrics stall), JA-017 (unbounded restart loop), JA-020 (needless raycasts), JA-011 (dead mesh build at first knife use).

**Unmeasured risks (need profiling before any change):**
- JA-008: per-frame full entity scans.
- `FreecamRenderMixin` forces `isSectionCompiledAndVisible` to true and freecam disables `smartCull`. More entities render while freecam is active. This is intended, but its cost is unmeasured.
- `CharacterModule` lists the drop directory every 2 s on the client thread. Cheap, but unbounded if the folder grows.
- Integrated-graphics compatibility: every overlay uses vanilla core shaders (`position_tex_color`, line and filled snippets). There is no custom GLSL, so the shader-compatibility risk is low. Skinned 3D characters at "High" detail are the most likely iGPU cost. Not measured.

**Security:**
- No credential access, no hardcoded secrets, no native libraries beyond LWJGL/stb (which Minecraft ships), and no reflection or `Unsafe`.
- No remote code: the only executed process is the bundled `media-session.ps1`. It was read in full. It reads only the Windows media session and the command file, and exits with its parent.
- Network endpoints: `accounts.spotify.com`, `api.spotify.com`, `lrclib.net`, `itunes.apple.com`, all HTTPS. The OAuth callback is loopback-only on port 43821.
- Untrusted image decoding (album art) goes through stb_image, the same decoder Minecraft uses for textures. Downloads are HTTPS-only and capped at 4 MB.
- Findings: JA-005 (default-on third-party traffic and Bypass PowerShell), JA-016 (plaintext refresh token), JA-021 (never push the local branches).
- The Anubis JAR (`c5259f9f…85de`) was read as a ZIP only, to hash two assets. It was never executed or loaded.
- Licensing: sky art per R1 (owner-granted; the distribution-scope statement is still open and not blocking). Anubis port per R4, and `ATTRIBUTION.md` has the section. The staged CC-BY models are not in the jar.

---

## H. Build and test results

**Environment:** Windows 11 Pro 10.0.22631. JDK `analysis/toolchains/temurin-21/jdk-21.0.12.1+1`. Gradle wrapper 9.8.2 with `--no-daemon --offline`. Each run used its own snapshot copy at `%TEMP%\claude\…\scratchpad\snap` with its own `build/` and project cache. Pre-check: a single `java.exe` (PID 28292, an agent's Gradle daemon from 00:11). No Minecraft client was running. The shared build dirs were not touched.

| # | Snapshot | Command | Result |
|---|---|---|---|
| 1 | Live tree copied 00:15:22 | `gradlew --no-daemon --offline --console=plain test build --continue` | `compileJava` **OK**. `compileTestJava` **FAILED**: 7 errors, all in `KnifePoseTest.java:73-90` (`DRAWN_LENGTH`, `MIN_SCALE`, `MAX_SCALE` unresolved; F's in-flight edit). BUILD FAILED, exit 1. |
| 2 | Same snapshot with `KnifePoseTest` set aside **in the copy only** | same | BUILD SUCCESSFUL. **139 suites / 600 tests / 0 / 0 / 0** (parsed from XML). |
| 3 | Live tree re-copied 00:26:03, nothing excluded | same | BUILD SUCCESSFUL. **140 suites / 612 tests / 0 failures / 0 errors / 0 skipped**, all XML from this run. Jar **8,346,709 B**, SHA-256 `29ffc32bdefbb7192e83fe5a66b89fd681f4da0bd173b9c652ddd9dd8b143d75`; sources jar 7,462,711 B. Jar 2 and jar 3 hash identically (the remap is deterministic and main sources were unchanged). |

**What was covered:** compilation, unit tests (including the existing `*MixinTargetsTest` static target checks, config serialization tests, `ModuleManager` tests and preview-spec tests), resource processing, remapping, and jar packaging. Jar contents were inspected (677 entries).

**What was not run, and why:**
- `runClient` / `runVisualVerify`: they would launch a Minecraft instance, and `runClient` is O-only by protocol.
- Runtime mixin application: only static target tests exist, so a mixin that fails to apply in the game cannot be excluded.
- Spotify transfer and playback: these need the user's account authorization.

**Recorded vs actual:** see JA-004. No recorded artifact matches the current tree.

---

## I. Prioritized repair plan

Ordered by severity, then dependency, then regression risk. Each step leaves the tree green.

1. **JA-014, then JA-001** (O): ignore the build dirs, take a fresh tarball now, then commit to a WIP branch after the user approves. *Do this before anything else: it protects every later fix.*
2. **JA-002** (H): a two-line call-site fix plus a module-level test. Highest user-visible regression, lowest risk.
3. **JA-003** (O, then I): `ChoiceSetting` dynamic choices, then the character refresh hook. Unblocks M32 (`bacon-hair.glb`).
4. **JA-007** (O): config read hardening. A contested file, so additive edits only.
5. **JA-005 + JA-006 + JA-016 + JA-017** (A, one batch, after LYRIC-WIRE-1): music defaults and opt-in, lyric retry, token write, bridge backoff. Same lane, same files.
6. **JA-004** (O): regenerate the records from the next quiet-tree gate. It must follow steps 2–5, or it will go stale again immediately.
7. **JA-010** (B, scheduled by O, one client): a full harness capture newer than the newest source, including 3D character + wings, the knife, the galaxy sky and the HUD Full preset. That produces the evidence for JA-009 and the JA-024 hazards.
8. **JA-008** (B): profile, and change only if confirmed.
9. **JA-024** (O assigns the hand-pass owner, then F) and **JA-009** (E, after step 7).
10. **Low items:** JA-011 and JA-012 (I), JA-013 (C + O), JA-018 (B), JA-019 (F), JA-020 (G), JA-022 and JA-023 (O). JA-015 is already in E's RELEASE-MANIFEST-1.

---

## J. Agent assignments

The registry's queue policy applies: deliver only to idle sessions, and add no new agents.

| Agent | Issues | Notes |
|---|---|---|
| **O** (Orchestrator) | JA-001, JA-004, JA-007, JA-014, JA-022, JA-023; API half of JA-003; ownership decisions for JA-013 and JA-024 | JA-001 needs explicit user approval before any commit or push |
| **H** (Movement) | JA-002 | Was idle after 00:01 writes. Confirm before sending. |
| **I** (Models) | JA-003 (second half), JA-011, JA-012 | Currently on CHARACTER-EDGE-1, same files. Queue it; don't interrupt. |
| **A** (Lyrics/Music) | JA-005, JA-006, JA-016, JA-017 | **Busy** (LYRIC-WIRE-1; `SpatialLyricsModule` written 00:14). Queue. |
| **B** (RenderFX) | JA-008, JA-010 (harness), JA-018 | Already holds RENDER-FINDINGS-1. Append; don't duplicate the galaxy item. |
| **E** (HUD) | JA-009, JA-015 | JA-015 is already part of RELEASE-MANIFEST-1. Don't re-send. |
| **F** (Cosmetics) | JA-019, JA-024 (after the ownership decision) | Active at 00:17 (`KnifePoseTest`). Queue. |
| **G** (Combat) | JA-020 | Bundle with SHIELD-MODEL-1 |
| **C** (ClickGUI) | JA-013 | After GUI-SWEEP-1 |
| **User** | JA-021 (branch disposition), JA-005 policy decision, approval for JA-001's commit | — |

---

*Evidence locations: build logs `build.log`, `build2.log` and `build3.log` in the audit scratchpad; snapshot test XML at `scratchpad/snap/build/test-results/test/`. No file in `C:\Users\zaber\Desktop\zinstist` was changed by this audit apart from this report, which is in the new folder `audit/`.*
