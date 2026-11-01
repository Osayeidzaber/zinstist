# Jilaton — Project Status

> **Superseded (2026-11-01).** The current state, gate counts and open work are in
> [`../../HANDOVER.md`](../../HANDOVER.md). Everything below describes head `486c138` and predates the
> Radiance and terrain-wave fixes; read it as history, not as the current tree.

Canonical status record. Detailed history lives in Git. Ownership and architecture live in
`ORCHESTRATION.md`; the module inventory lives in `IMPLEMENTATION_INVENTORY.md`.

Last updated: 2026-11-01 06:25 (Master, branch `jilaton/2026-10-31-work`; `486c138` artifact recorded 06:23)

## Repository state

- HEAD: `486c138` — "Integrate lane 1: hotbar restore on disable and one focus-aware hold-key rule".
  Source-bearing history below it: `bd93f36` (hold-key policy), `8ca0124` (hotbar restore), `1923d54`
  (lane 3 / P1-9), `d8aef97` (gate record incl. harness scene edit), `e6c5179` (lane 3), `e9e58ec`
  (lane 2), `956cfa5` (lane 1), `24e9ce9` (lane 4).
- The gate and harness below were run **after** lane 1 was merged, so counts and evidence describe
  `486c138` exactly. Any further source edit invalidates them.
- Branch is **12 commits ahead of `origin/jilaton/2026-10-31-work`** (`5d47db9`); nothing pushed since the
  device switch. Push needs explicit user permission.
- `main` (`f11bfff`) is old and must not be merged.
- Working tree: clean apart from this file. `.qoder/` (agent worktrees, session state) is now listed in
  `.gitignore` and must never be committed — a broad `git add -A` would otherwise swallow other lanes'
  in-progress checkouts.
- Lane worktrees under `.qoder/worktrees/agent-general-purpose-*` are retained at their lane
  commits; all four previously integrated lane branches are fully merged into HEAD.
- Environment: Windows 10 x64, Temurin 21, Gradle 9.8.2 wrapper, Fabric Loader 0.20.1 /
  Fabric API 0.141.6 / Loom 1.14.10, Minecraft 1.21.11.

## Verification levels

`SOURCE_REVIEWED` → `BUILD_VERIFIED` → `AUTOMATED_TESTED` → `RUNTIME_VERIFIED`.
A green unit test never proves a mixin applies; `BUILD SUCCESSFUL` never proves runtime behaviour.

## Gates on the integrated tree

- Automated gate on **`486c138`** (isolated build dir `o`, `compileJava test`): **202 suites / 947 tests /
  0 failures / 0 errors / 0 skipped**, recounted from `.isolated-build-o/root/test-results/test/TEST-*.xml`
  (all 202 XMLs fresh after 06:16; `BUILD SUCCESSFUL in 3m 2s`). Lane-1 coverage is present as
  `HotbarSlotRestoreTest` (4 tests) and `HoldKeyPolicyTest` (7 tests).
- Previous gate on `e6c5179`: 200 suites / 935 tests / 0 failures / 0 errors / 0 skipped
  (`C:/tmp/jilaton-arch/integration-merged.log`).
- Artifact on **`486c138`** (task `jar`, build dir `o`): `.isolated-build-o/root/devlibs/jilaton-client-0.1.0-dev.jar`,
  9,751,053 bytes, mtime 06:22:10, SHA-256
  `5c221769a8598447af435e7bbe07fba500155cb30a4e32b046a3d40a60247bc9`. This is the tree the gate and the
  harness above ran against. The remapped `root/libs/…-0.1.0.jar` was **not** rebuilt for this commit and is
  still the pre-lane-1 artifact (9,772,837 bytes, SHA-256 `fcf8998…4266c1`, 05:59) — do not quote it as `486c138`.
- Runtime harness on `486c138` (`runVisualVerify`, armed by `-Djilaton.visualVerify=true`): exit 0,
  32 scenes, **96 screenshots regenerated** (freshness checked with `find -newermt`, not existence),
  auto-quit and clean world save. Log `C:/tmp/jilaton-arch/harness-486c138.log`.
- Boot log: no `InvalidMixinException`, failed injection, `BootstrapMethodError`, `NoSuchMethodError`,
  `ClassCastException`, or `ERROR` line. Fullbright mixin applied live.

## Runtime verdicts on the integrated build (this run)

| Area | Verdict | Evidence |
|---|---|---|
| HUD right column (JA-009) | **RUNTIME_VERIFIED FIXED** | In `verify-19-hud-minimal.png` and `verify-20-hud-full.png` the module list ends ≈y205 and `POTION EFFECTS / Resistance 256` starts ≈y232 — clean gap, no overlap. |
| Target card chips (JA-010 / P1-12) | **RUNTIME_VERIFIED FIXED** | `verify-19-hud-minimal.png` shows the whole card — Zombie, 3.0 m, 20.0 and the full `FALL / ARMOR / ABSORB` chip row — entirely above a fully visible vanilla hotbar. Acceptance: whole card incl. chip row visible, hotbar uncovered, in scenes 19 and 20. |
| FreeLook + KillAura | **RUNTIME_VERIFIED FIXED** (harness scope) | `verify-32-freelook-killaura.png` is a coherent third-person view. Log: `enabled free_look -> true`, 24 `aura-freelook` ticks, `bodyYaw` −24 → −90, `cameraYaw=0.0` throughout, no self-disable ERROR. Caveat: the harness drives body yaw, so camera-only behaviour is not asserted. |
| Terrain shockwave | **STILL NOT VISIBLE** | `verify-28-terrain-wave.png` and `verify-28b-terrain-wave-wide.png` show an empty plaza with only brown impact debris; no wavefront even at radius 21. |
| Radiance | **STILL NOT VISIBLE — cause narrowed** | `verify-23-radiance.png` has no aura. Harness wiring proven good: `enabled radiance -> true`, `setting radiance.color not found` = 0, `World effect .* failed` = 0. Colour parse returns 24-bit RGB and `EffectBatch.withAlpha` correctly maps 0..1 → 0..255. Remaining suspects: the fan's bright centre sits inside the body (depth/occlusion), extents leave only ≈0.235 blocks of visible ring, and alpha 0.187/0.075 averages ≈1/3 across the fan. Owner: Agent 2. |
| Fullbright | **RUNTIME_VERIFIED** | `verify-26-fullbright-off.png` near-black vs `26b-on` clearly lit; log shows `options.gamma()` unchanged. |
| Screenshot Copy | **RUNTIME_VERIFIED** | Log `[System] [CHAT] Copied screenshot` (scene 31). |
| Pharaoh helmet hiding | **RUNTIME_VERIFIED** | Scene 30 equips `minecraft:iron_helmet` ([VisualVerifier.java:646](file:///C:/Users/zaber/Desktop/zinstist/JilatonClient/src/main/java/dev/jilaton/client/dev/VisualVerifier.java#L646)); `verify-30` hides it under the Nemes headdress, `30b` restores it. |
| Spatial / Sky Lyrics | **BLOCKED** | No lyrics resolved offline (`lines=0`). |
| Ambience sky tint | **OBSERVED, UNTESTED** | `verify-29-ambience-sky.png` warm sky; precedence vs CustomSky still untested. |
| ClickGUI, block outline, afterglow, CustomSky galaxy | Baseline-verified at `69cf5df`; recaptured here but not individually re-inspected | `verify-14/15/16/17/18*`, `21`, `24`, `07*`. |

## Open defects (priority order)

1. **P2 — Terrain shockwave still invisible at runtime.** Lifetime culling is fixed
   (`TerrainWave.impactLife` ≥ 1.6 s) and the front-band selection is tested, yet no geometry reaches
   the frame. Next step is a render-pass trace for one attack frame, not another budget change.
   Owner: **Agent 2**.
2. **P3 — Radiance draws nothing visible.** Diagnosis narrowed to occlusion/extents/amplitude
   (see table). Agent 2 lane `module`-independent fix in flight; runtime proof pending a Master harness run.
   Owner: **Agent 2**.
3. **P1 — ClickGUI persistence** (toggle-only, last category/module, expanded state, scroll). Owner:
   **Agent 4**. No schema work is needed — every field already exists in `ClientConfig` and both GUIs read
   and write them; what is missing is a restart round-trip test and a card-body click check.
4. **Environment limitation — lyrics unverified offline.** Owner: Agent 4, blocked.
5. **P5 — hot-path allocations.** `HitEffectsModule` per-block `TerrainWave.blockTop` allocation and
   the 1400-cell grid vs 240/frame cap; `SpatialLyricsModule` per-frame allocations. Owners: Agent 2
   and Agent 4; must not regress the visual fixes.

## Lane state

| Lane | Branch / commit | State |
|---|---|---|
| Agent 1 modules/mixins | `bd93f36` + `8ca0124`, merged in `486c138` | **INTEGRATED**: `HotbarSlotRestore` shared by AutoEat/AutoGapple, one focus-aware `previousKeyState` rule. Gate green (947 tests). |
| Agent 1 module-gap batch | background lane (worktree) | **RUNNING**: hold-module restart persistence, AutoHead/AutoSoup onto `HotbarSlotRestore`, randomised Velocity ranges, AutoArmor hotbar equip. Not yet reviewed. |
| Agent 2 world FX | `d008267` merged in `e9e58ec`; new Radiance lane (worktree) | **RUNNING**: Radiance amplitude/occlusion fix. Shockwave trace still outstanding. |
| Agent 3 models | `61aa076` merged in `e6c5179`, `1923d54` merged | **INTEGRATED**; wing/companion handedness fixed, `Transform.mirrorX`; knife honours ViewModel/swing. |
| Agent 4 GUI/HUD/music | `f1cd094` merged in `24e9ce9` | **INTEGRATED**; HUD column stacking and target-card chips both runtime-verified. |
| Agent 5 visual reference | — | Idle by design; no user references queued. |

Two lane agents are running in the background: Agent 1 (`module-gap-batch`) and Agent 2 (Radiance).
Neither lane may be merged until the Master has read its diff.

## Open risks

- Shared build cache: every gate must use the isolated init script and the shared lock
  (`C:/tmp/jilaton-arch/.build.lock`). Concurrent builds clobber the Loom cache.
- Counts and hashes above describe one tree state only (`486c138`); any later edit invalidates them.
- The `486c138` artifact is the **dev jar**; the remapped `root/libs` jar on disk is still the pre-lane-1 build.
- The `run-verify` world is a 163 MB copy of the user's save; the original is untouched.
- New or changed mixins need a live boot log every time, not just a green compile.
- Cross-owner edits: only the owning lane may touch `ClientConfig`, mixins and shared HUD code.
- Screenshot freshness must be proven with `find -newermt`; `run-verify/screenshots` retains stale files.

## Next milestone

Review and merge the two running lanes (Agent 1 module gaps, Agent 2 Radiance), rebuild the gate on the
new head, rerun the harness, and judge scene 23 (aura) and scene 28 (wavefront) against their written
acceptance criteria. In parallel Agent 4 takes P1-9's restart round trip and card-body click. Nothing is
pushed without explicit user permission.
