# Jilaton — Session Handover

Central source of truth for the next developer or agent. Written at the end of the 2026-10-31/11-01
session, after consolidating every lane into one branch. Detailed history lives in Git; the older
`JilatonClient/development/*.md` files are kept as history and are **not** the current state.

Last updated: 2026-11-01 06:55 (branch `jilaton/2026-10-31-work`, pre-handover head `6c552eb`)

---

## A. Project overview

**What it is.** Jilaton is a Fabric client mod for Minecraft that adds functional modules (combat,
movement, player, render), animated 3D cosmetics, custom HUD/ClickGUI, world-space visual effects, and a
music/lyrics presentation layer. It is a real client, not a UI mockup: every feature is expected to be
registered, configurable, cleaned up on disable, and observable in a running game.

**Technology.**
- Minecraft 1.21.11, Fabric Loader 0.20.1, Fabric API 0.141.6, Fabric Loom 1.14.10
- Java 21 (Temurin 21.0.12.1+1), Gradle 9.8.2 wrapper
- Windows 10 x64, Git Bash
- Entry point: `JilatonClient.java:36` (`ClientModInitializer`)

**Final state of the repository.**
- One repository, one working tree: `C:\Users\zaber\Desktop\zinstist`
- Active branch: `jilaton/2026-10-31-work`
- Handover head: `6c552eb` (the docs/handover commit follows it; see the final report for the hash)
- `origin/jilaton/2026-10-31-work` is 36+ commits behind and **has not been pushed** — pushing needs
  explicit user permission
- `main` (`f11bfff`) is an old snapshot and must not be merged into this branch
- All secondary agent worktrees were removed after their work was committed; their branches remain

**Verification levels used in this project.**
`SOURCE_REVIEWED` → `BUILD_VERIFIED` → `AUTOMATED_TESTED` → `RUNTIME_VERIFIED`.
A green unit test never proves a mixin applies; `BUILD SUCCESSFUL` never proves runtime behaviour.

---

## B. Completed work

### Fully verified at runtime (this session's harness, on head `bbc1f53`)

| Area | State | Evidence |
|---|---|---|
| HUD right column stacking (JA-009) | Fixed | Module list ends ≈y205, potion panel starts ≈y232 in scenes 19/20 |
| Target card above hotbar (JA-010 / P1-12) | Fixed | Whole card incl. `FALL/ARMOR/ABSORB` chip row visible above an uncovered hotbar |
| Radiance aura (P1-11) | **Visible** | `verify-23-radiance.png` shows lavender halos around both the player and the offset zombie |
| FreeLook + KillAura | Fixed (harness scope) | Scene 32 coherent third-person view; `cameraYaw=0.0` throughout; no self-disable |
| Fullbright | Fixed | `verify-26-*` off/on comparison; `options.gamma()` unchanged |
| Screenshot Copy | Fixed | Log `[System] [CHAT] Copied screenshot` (scene 31) |
| Pharaoh helmet hiding | Fixed | Scene 30 hides `minecraft:iron_helmet` under the Nemes headdress and restores it |
| Boot health | Clean | Boot log has no `InvalidMixinException`, failed injection, `BootstrapMethodError`, `NoSuchMethodError`, `ClassCastException`, or `ERROR` line |

### Implemented and covered by tests, not yet re-verified at runtime

- **Terrain shockwave legibility.** The wave *was* being submitted; at the captured age its alpha was
  0.38 and the leading edge ~1.2 px at a grazing angle, so it read as a pale band. `TerrainWave.alpha`
  is now `1-u²`, the leading edge is 2.0 px, and the block-band scan moved into pure
  `TerrainWave.emitBand(...)` with a reused buffer (budget still ≤1400 block tops/frame).
- **Radiance halo shape.** Further refined after the runtime pass: two depth-tested halos whose bright
  edges sit on the silhouette (`FxShapes.softHalo`, `RadianceModule.emitAura`), replacing the
  centre-peaked ellipses. The runtime-verified screenshot predates this refinement.
- **Velocity randomised range.** New `horizontal_random_min/max` (percent, −100..100) sampled once per
  knockback packet; equal bounds preserve the existing fixed `horizontal` setting. Arithmetic lives in
  `VelocityRange` (original code, no reference-client source copied).
- **Hold-module restart rule.** `FreeLook` and `Zoom` save `enabled=false` and ignore a hand-edited
  `true` via `ClientModule.persistEnabled()`.
- **AutoHead / AutoSoup** now share `HotbarSlotRestore.shouldRestore(...)` instead of a per-module copy.

### Verified in earlier sessions (baseline, recaptured but not re-inspected this session)

ClickGUI rendering, block outline, afterglow, CustomSky galaxy, wings/companion handedness, knife
view-model and first-person hand stages, ambience sky tint (observed, precedence untested).

### Implemented but blocked by the environment

- **Spatial / Sky Lyrics.** No lyrics resolve offline (`lines=0`); needs an authorized metadata source
  or user-provided synchronized lyrics.

---

## C. Agent work summary

| Agent | Responsibility | Outcome |
|---|---|---|
| **Master (Agent 0)** | Architecture, ownership, gates, harness, integration | Merged 5 lanes, ran every gate and harness, resolved the lane-2/Agent-2 merge conflicts, consolidated to one worktree |
| **Agent 1** | Modules, mixins, reference integration | Integrated: hotbar restore sharing, hold-key policy, FreeLook/Zoom persistence, velocity ranges. Stopped short of AutoArmor hotbar equip (already works via slots 0..35) |
| **Agent 2** | World rendering and FX | Integrated: Radiance silhouette fix (runtime-verified), shockwave legibility fix, block-top budget helper, HUD/SkyLyrics budget hardening |
| **Agent 3** | 3D models and animation | Integrated: wing fan direction, companion handedness, `Transform.mirrorX`, knife honours ViewModel/swing, first-person hand stages for knife and empty hand |
| **Agent 4** | GUI, HUD, music | Integrated: HUD right-column stacking, bottom-anchored target card. Left unverified WIP: ClickGUI persistence round-trip tests (see §D) |
| **Agent 5** | Visual reference | Idle by design; no user references were supplied |

---

## D. Remaining tasks

### High priority

1. **Verify the shockwave at runtime.** Scene 28/28b showed no wavefront at `bbc1f53`; the legibility
   fix landed afterwards. Run the harness and judge scene 28 against: a bright, high-contrast front
   sweeping outward to the configured radius across the captured frames.
   Files: `render/fx/TerrainWave.java`, `module/impl/HitEffectsModule.java`. Owner: Agent 2.
2. **Re-verify Radiance after the halo refinement.** Scene 23 acceptance: a visible lavender halo
   *around* the silhouette, no box edges, no through-wall glow. Owner: Agent 2.
3. **ClickGUI persistence — finish and verify.** Branch `worktree-agent-general-purpose-1d16dd80`
   (`5781eb6`) holds unverified tests `ClickGuiPersistenceRoundTripTest` and `ClickGuiClickTargetTest`
   plus a `ClickGuiScreen` change. Needed: run the suite, confirm card-body click opens settings while
   only the toggle flips state, and that category/module/expanded/scroll survive a restart round trip.
   Owner: Agent 4.

### Medium priority

4. **Decide AutoArmor hotbar equip.** `AutoArmorModule.java:89` already scans slots 0..35 and maps
   0..8 to menu slots 36..44, so hotbar pieces equip through the container path. A second select/use
   path would add an unverified server interaction — do not add it without live inventory testing.
5. **Spatial / Sky Lyrics.** Blocked offline; needs authorized lyrics metadata.
6. **Ambience vs CustomSky precedence** is untested.
7. **Terrain wave budget guard.** Branch `worktree-agent-general-purpose-74df511a` (`3084354`) holds
   an unverified `TerrainWaveBudgetTest` plus edits to `TerrainWave`/`HitEffectsModule`. Review before
   merging; it overlaps task 1.

### Low priority

8. **Hot-path allocations (P5).** `HitEffectsModule` per-block allocation and the 1400-cell grid vs the
   240/frame cap; `SpatialLyricsModule` per-frame allocations.
9. **Unused primitive.** `FxShapes.softRing` is no longer called by production code (the halo path
   replaced it) but is covered by `FxShapesRingTest`. Keep or remove deliberately, with the test.
10. **ClickGUI polish** items from the backlog (status labels, blur, slider/dropdown behaviour).

---

## E. Known issues and technical debt

**Confirmed**
- Scene 28/28b at `bbc1f53`: shockwave not visible. Root cause diagnosed and fixed afterwards; the fix
  is automated-tested but not runtime-verified.
- No jar was rebuilt for the consolidated head. The dev jar at
  `.isolated-build-o/root/devlibs/jilaton-client-0.1.0-dev.jar` (9,751,053 bytes, SHA-256
  `5c221769…02bc9`) is the `486c138` artifact; the remapped `root/libs` jar is even older. Do not quote
  either as the current head.
- Lyrics cannot be verified offline.
- `run-verify/` is a 163 MB copy of the user's world; `run-verify/screenshots` also retains stale files —
  always prove freshness with `find -newermt`, not existence.

**Suspected / unverified**
- Radiance quad count per entity rose (20→40 High, 14→28 Balanced) with two halo bands; well under the
  24,000-quad batch cap, but worth watching with many entities in range.
- FreeLook/Zoom no longer resume even when their `hold` setting is off (toggle mode). This follows the
  "never restore" rule but is a behaviour change for toggle users.

---

## F. Architecture and project structure

```
C:\Users\zaber\Desktop\zinstist\
  HANDOVER.md                 ← this file (central state)
  README.md, SECURITY_ASSESSMENT.md, info.md
  Anubis Beta 0.9.9.3 (1).jar ← reference artifact; NEVER execute unknown reference JARs
  analysis/                   toolchains (Temurin 21) and analysis output
  app/, audit/, ideas/        scratch/notes; not part of the build
  JilatonClient/              the Gradle subproject (all source)
    build.gradle, src/, run-verify/ (163 MB copied world, git-ignored build output)
    development/              historical reports; superseded by this file
    src/main/java/dev/jilaton/client/
      JilatonClient.java      entry point (ClientModInitializer)
      module/                 ClientModule, ModuleManager, ModuleCategory, impl/*
      mixin/                  injection points (camera, hands, velocity, hotbar, gamma…)
      render/                 EffectBatch, FxFrame, FxShapes, FX emitters, model pipeline
      gui/                    ClickGUI, HUD, Appearance Studio
      config/                 ClientConfig persistence
      music/, notification/, screenshot/, rotation/, command/, account/, setting/
      dev/VisualVerifier      scripted runtime harness (Master-owned)
    src/test/java/…           JUnit 5 suites, ~965 tests
```

- **Module contract:** `ClientModule` gives settings, enable/disable and `persistEnabled()`; modules
  register through `ModuleManager`; render modules implement `FxEmitter` and push geometry into
  `EffectBatch` via an `FxFrame`.
- **Rendering:** one shared batch and depth-test flags (`walls`). Aura-style glows must stay
  depth-tested and put their bright geometry outside the silhouette.
- **Ownership rule:** only the owning lane may change `ClientConfig`, mixins, or shared HUD code.

---

## G. Setup, build and testing

**Java.** Every Gradle call must pin the JDK or the daemon picks JVM 8:

```bash
export JAVA_HOME="C:/Users/zaber/Desktop/zinstist/analysis/toolchains/temurin-21/jdk-21.0.12.1+1"
```

**Isolated build (required).** The Gradle/Loom cache is shared between concurrent builds and will be
corrupted without a lock. Init scripts live in `C:/tmp/jilaton-arch/` (`isolated-a…r.gradle`), each
setting a private `.isolated-build-<letter>/` root and taking an exclusive `FileLock` on
`C:/tmp/jilaton-arch/.build.lock`:

```bash
cd JilatonClient
./gradlew --init-script /c/tmp/jilaton-arch/isolated-o.gradle compileJava compileTestJava test
```

**Tests.** Aggregate from XML — never the console banner:

```bash
python -c "import glob,xml.etree.ElementTree as ET
t=f=e=s=0
for p in glob.glob('.isolated-build-o/root/test-results/test/TEST-*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests',0)); f+=int(r.get('failures',0)); e+=int(r.get('errors',0)); s+=int(r.get('skipped',0))
print(len(glob.glob('.isolated-build-o/root/test-results/test/TEST-*.xml')), t, f, e, s)"
```

**Runtime harness (Master only).**

```bash
./gradlew --init-script /c/tmp/jilaton-arch/isolated-o.gradle runVisualVerify -Djilaton.visualVerify=true
```

32 scenes, ~96 screenshots in `run-verify/screenshots`, auto-quit and world save.

**Most recent results**
- Consolidated head `6c552eb`: **207 suites / 965 tests / 0 failures / 0 errors / 0 skipped**, every XML
  regenerated by that run.
- Pre-merge head `bbc1f53`: 206 / 958 / 0 / 0 / 0.
- Harness on `bbc1f53`: exit 0, `BUILD SUCCESSFUL in 3m 35s`, clean boot, screenshots at 06:33.
- **Not tested:** the runtime harness has not run on `6c552eb`; no jar was built for it.

---

## H. Next agent instructions

1. **Read first:** this file, then `git log --oneline -25`, then `JilatonClient/development/BACKLOG.md`
   for item wording. Do not re-derive state from the older `development/*.md` status files.
2. **First action:** run the gate (command in §G) and the harness on the current head to re-establish
   baseline. Judge scenes 23 and 28 before changing effect code.
3. **Priorities:** §D items 1–3. Verify before extending: the shockwave and Radiance fixes are untested
   at runtime, and the ClickGUI persistence work is an unverified branch.
4. **Do not rewrite:** `EffectBatch`/`FxFrame`/`FxShapes` API, the module contract, `ClientConfig`
   persistence format, or the `HotbarSlotRestore`/hold-key rules — all were deliberately consolidated.
5. **Preserve:** `dev/VisualVerifier` is the only runtime oracle; keep its scene numbering stable.
6. **Safety:** never execute the reference JAR in the repo root; respect GPL-3.0 obligations if porting
   LiquidBounce code; do not push without explicit user permission.
7. **Do not** merge `main`, do not reintroduce per-module copies of `HotbarSlotRestore`, and do not let
   two lanes edit `ClientConfig`, mixins or shared HUD code at the same time.

---

## Preserved unmerged branches (do not delete without reading)

| Branch | Head | Contents |
|---|---|---|
| `worktree-agent-general-purpose-1d16dd80` | `5781eb6` | ClickGUI persistence round-trip + click-target tests, `ClickGuiScreen` change (unverified) |
| `worktree-agent-general-purpose-74df511a` | `3084354` | `TerrainWaveBudgetTest`, terrain-wave/HitEffects edits (unverified, overlaps §D1) |

All other agent branches are merged into `jilaton/2026-10-31-work`.
