# Jilaton — Project Status

Canonical status record. Detailed history lives in Git. Ownership and architecture live in
`ORCHESTRATION.md`; the module inventory lives in `IMPLEMENTATION_INVENTORY.md`.

Last updated: 2026-11-01 00:05 (Master, branch `jilaton/2026-10-31-work`)

## Repository state

- HEAD: `1923d54` — "Correct P1-9: ClickGUI persistence already exists in source".
  Source-bearing history below it: `d8aef97` (gate record — **this commit contains the harness scene
  edit that was uncommitted at gate time**), `e6c5179` (lane 3), `e9e58ec` (lane 2), `956cfa5` (lane 1),
  `24e9ce9` (lane 4).
- **`git diff --stat e6c5179..HEAD` touches only Markdown plus `dev/VisualVerifier.java`, and that Java
  diff is exactly the scene-28 edit (`world_wave_radius` 44 → 21, `shot(12)` → `shot(4)`) that the gate
  ran against.** So the gated source *content* and HEAD's source content are identical; the counts and jar
  hash below remain valid for HEAD. Any further source edit invalidates them.
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

- Automated gate on `e6c5179` (isolated build dir `o`): **200 suites / 935 tests / 0 failures /
  0 errors / 0 skipped**, recounted from `.isolated-build-o/root/test-results/test/TEST-*.xml`.
  Every XML shares mtime 2026-11-01 05:52:08, i.e. one build, postdating the lane merges.
  Log: `C:/tmp/jilaton-arch/integration-merged.log`.
- Artifact built from `e6c5179` **plus the uncommitted `VisualVerifier` scene edit**:
  `.isolated-build-o/root/libs/jilaton-client-0.1.0.jar` (9,772,837 bytes),
  SHA-256 `fcf899862bf6e2864534300ba27f1b97e385c30f3933e31fbd6579d1534266c1`, built 05:59
  (`C:/tmp/jilaton-arch/jar-merged.log`). No sources jar was produced.
  Because the harness scene edit is in this jar, the hash belongs to that state — not to bare `e6c5179`.
- Runtime harness: `runVisualVerify` on the same tree → `BUILD SUCCESSFUL in 3m 59s`, exit 0,
  32 scenes, **96 screenshots regenerated** (124 files total in `run-verify/screenshots/`),
  log `C:/tmp/jilaton-arch/harness-merged.log`, auto-quit.
- Boot log: no `InvalidMixinException`, no failed injection, no `BootstrapMethodError`,
  no `NoSuchMethodError`, no `ClassCastException`, no `ERROR` line. Fullbright mixin applied live.

## Runtime verdicts on the integrated build (this run)

| Area | Verdict | Evidence |
|---|---|---|
| HUD right column (JA-009) | **RUNTIME_VERIFIED FIXED** | In `verify-19-hud-minimal.png` and `verify-20-hud-full.png` the module list ends ≈y205 and `POTION EFFECTS / Resistance 256` starts ≈y232 — clean gap, no overlap. |
| FreeLook + KillAura | **RUNTIME_VERIFIED FIXED** (harness scope) | `verify-32-freelook-killaura.png` is a coherent third-person view. Log: `enabled free_look -> true`, 24 `aura-freelook` ticks, `bodyYaw` −24 → −90, `cameraYaw=0.0` throughout, **no** self-disable ERROR. Caveat: the harness drives body yaw, so camera-only behaviour is not yet asserted. |
| Target card chips | **NEW DEFECT — overlaps vanilla hotbar** | `verify-19`/`verify-20`: the `FALL / ARMOR / ABSORB` chip strip sits on the hotbar. Source: `HudRenderer.java:635` fixed default `guiHeight - 76` ignores the chip-row height added by `HudDesign.baseTargetCard(chips, heldVisible)` at line 624. |
| Terrain shockwave | **STILL NOT VISIBLE** | `verify-28-terrain-wave.png` and `verify-28b-terrain-wave-wide.png` show an empty plaza with only brown impact debris; no wavefront, even with radius 21 and the earlier capture. |
| Radiance | **PARTIAL** | `verify-23-radiance.png`: the tall translucent rectangular column is gone, but no clear radial glow is discernible around the player. |
| Fullbright | **RUNTIME_VERIFIED** | `verify-26-fullbright-off.png` near-black vs `26b-on` clearly lit; log shows `options.gamma()` unchanged. |
| Screenshot Copy | **RUNTIME_VERIFIED** | Log `[System] [CHAT] Copied screenshot` (scene 31). |
| Pharaoh helmet hiding | **RUNTIME_VERIFIED** | Scene 30 equips `minecraft:iron_helmet` ([VisualVerifier.java:646](file:///C:/Users/zaber/Desktop/zinstist/JilatonClient/src/main/java/dev/jilaton/client/dev/VisualVerifier.java#L646)); `verify-30` hides it under the Nemes headdress, `30b` restores it. |
| Spatial / Sky Lyrics | **BLOCKED** | No lyrics resolved offline (`lines=0`). |
| Ambience sky tint | **OBSERVED, UNTESTED** | `verify-29-ambience-sky.png` warm sky; precedence vs CustomSky still untested. |
| ClickGUI, block outline, afterglow, CustomSky galaxy | Baseline-verified at `69cf5df`; recaptured here but not individually re-inspected | `verify-14/15/16/17/18*`, `21`, `24`, `07*`. |

## Open defects (priority order)

1. **P1 — Target card chip row overlaps the hotbar.** Default Y is fixed at `guiHeight - 76` and does
   not grow with `HudDesign.baseTargetCard`'s chip row. Owner: **Agent 4**
   (`gui/HudRenderer.java`, `gui/HudDesign.java`). Not yet a shipped-bug declaration — owner must confirm.
2. **P2 — Terrain shockwave still invisible at runtime.** Lifetime culling is fixed
   (`TerrainWave.impactLife` ≥ 1.6 s) and the front-band selection is tested, yet no geometry reaches
   the frame. Next step is a render-pass trace for one attack frame, not another budget change.
   Owner: **Agent 2**.
3. **P3 — Radiance has no visible radial glow.** The six-face shell artefact is gone; whether the two
   feathered ellipse layers draw at all is unresolved. Owner: **Agent 2**.
4. **P1 — ClickGUI persistence** (toggle-only, last category/module, expanded state, scroll). Owner:
   **Agent 4**; needs a Master-owned `ClientConfig` schema addition first.
5. **Environment limitation — lyrics unverified offline.** Owner: Agent 4, blocked.
6. **P5 — hot-path allocations.** `HitEffectsModule` per-block `TerrainWave.blockTop` allocation and
   the 1400-cell grid vs 240/frame cap; `SpatialLyricsModule` per-frame allocations. Owners: Agent 2
   and Agent 4; must not regress the visual fixes.

## Lane state

| Lane | Worktree branch | State |
|---|---|---|
| Agent 1 modules/mixins | `worktree-agent-general-purpose-389712d8` (`ec77a80`) | **INTEGRATED** in `956cfa5`; hold-key edge policy in, FreeLook no longer self-disables at runtime. |
| Agent 2 world FX | `worktree-agent-general-purpose-15d1682f` (`d008267`) | **INTEGRATED** in `e9e58ec`; lifetime + feathered aura in; shockwave and glow still unproven. |
| Agent 3 models | `worktree-agent-general-purpose-9a155ccd` (`61aa076`) | **INTEGRATED** in `e6c5179`; wing/companion handedness fixed, `Transform.mirrorX`. |
| Agent 4 GUI/HUD/music | `worktree-agent-general-purpose-60ae4d60` (`f1cd094`) | **INTEGRATED** in `24e9ce9`; HUD column stacking runtime-verified; target-card overlap found. |
| Agent 5 visual reference | — | Idle by design; no user references queued. |

No lane agent is currently running. The task list holds only the Master's open item (harness + gate).

## Open risks

- Shared build cache: every gate must use the isolated init script and the shared lock
  (`C:/tmp/jilaton-arch/.build.lock`). Concurrent builds clobber the Loom cache.
- Counts and hashes above describe one tree state only (`e6c5179` + the harness edit); any later edit
  invalidates them. The harness edit is not yet committed.
- The `run-verify` world is a 163 MB copy of the user's save; the original is untouched.
- New or changed mixins need a live boot log every time, not just a green compile.
- Cross-owner edits: only the owning lane may touch `ClientConfig`, mixins and shared HUD code.

## Next milestone

Commit the harness scene edit and these records; then hand Agent 2 the shockwave render-pass trace and
Agent 4 the target-card overlap plus ClickGUI persistence (after the Master-owned config schema), and
rerun the gate and harness on the next integrated head.
