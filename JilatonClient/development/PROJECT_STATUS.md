# Jilaton — Project Status

Canonical status record. Detailed history lives in Git. Ownership and architecture live in
`ORCHESTRATION.md`; the module inventory lives in `IMPLEMENTATION_INVENTORY.md`.

Last updated: 2026-11-01 (Master, branch `jilaton/2026-10-31-work`)

## Repository state

- HEAD: `d0f9e37` — "Integrate lane 4: media bridge opt-in, music privacy, screenshot reveal"
  (branch is 8 commits ahead of `origin/jilaton/2026-10-31-work`; nothing pushed).
- Integration commits: `27c8c61` (lane 1), `b7627c7` (lane 2), `0bbfe12` (lane 3),
  `d0f9e37` (lane 4). Lane commits: `2a2ca43`, `b902c7b`, `56b5700`, `ecbc1b2`.
- `main` (`f11bfff`) is old and must not be merged.
- Working tree: only this file, `BACKLOG.md` and `.qoder/` are untracked; no source edits pending.
- Lane worktrees under `.qoder/worktrees/agent-general-purpose-*` are retained at their lane
  commits; all four lane branches are fully merged into HEAD.
- Environment: Windows 10 x64 (the brief's macOS assumption does not apply), Temurin 21,
  Gradle 9.8.2 wrapper, Fabric Loader 0.20.1 / Fabric API 0.141.6 / Loom 1.14.10, Java 21.

## Verification levels

`SOURCE_REVIEWED` → `BUILD_VERIFIED` → `AUTOMATED_TESTED` → `RUNTIME_VERIFIED`.
A green unit test never proves a mixin applies; a `BUILD SUCCESSFUL` never proves runtime behaviour.

## Gates on the integrated tree (`d0f9e37`)

- Automated gate: **195 suites / 905 tests / 0 failures / 0 errors / 0 skipped**
  (isolated lane `m`, recounted from the JUnit XML on 2026-11-01).
- Artifacts: `jilaton-client-0.1.0.jar` SHA-256
  `e6e54efb2a3ea3bc9d5844801eef79024d865c00af80e5861c9ef2148b25fd9f`;
  sources jar `2440d660b7e49b2ccd6a78ca1cc3b2252c31d371f8ae34c706601904963e9d1b`.
  These hashes are the only answers that carry the counts above.
- Runtime harness: `runVisualVerify` on `d0f9e37` → `BUILD SUCCESSFUL in 3m 27s`, exit 0,
  log `C:/tmp/jilaton-arch/verify-run-20261101.log`, auto-quit after 32 scenes.
  **96 screenshots regenerated** in `run-verify/screenshots/` (120 files total incl. older run).
- Boot log: **no `InvalidMixinException`, no failed injection, no `BootstrapMethodError`,
  no `NoSuchMethodError`, no `ClassCastException`**. `LightTextureFullbrightMixin` applied live.

## Runtime verdicts on the integrated build (this run)

| Area | Verdict | Evidence |
|---|---|---|
| Fullbright | **RUNTIME_VERIFIED** | `verify-26-fullbright-off.png` near-black vs `26b-on` clearly lit night plaza; log shows `options.gamma()` unchanged. |
| Screenshot Copy | **RUNTIME_VERIFIED** | Log `[System] [CHAT] Copied screenshot` at 05:15:22 (scene 31). |
| FreeLook + KillAura | **BROKEN** | Exactly one ERROR: `module free_look disabled itself while it was being tested` (05:15:28). Telemetry: `bodyYaw`/`headYaw` driven to −90° while `cameraYaw` stayed −12°, then self-disable. |
| HUD right column (JA-009) | **BROKEN** | `verify-20-hud-full.png`: `Music Center`, `Music Visuals`, `Teams` text overlaps the `POTION EFFECTS / Resistance 256` panel. |
| Terrain shockwave | **NOT VISIBLE** | `verify-28b-terrain-wave-wide.png`: flat plaza, no wavefront in any frame of the attack sequence. |
| Radiance | **BROKEN** | `verify-23-radiance.png`: tall translucent rectangular column around the player, not a radial glow. |
| Pharaoh helmet hiding | **RUNTIME_VERIFIED** | Scene 30 equips `minecraft:iron_helmet` ([VisualVerifier.java:646](file:///C:/Users/zaber/Desktop/zinstist/JilatonClient/src/main/java/dev/jilaton/client/dev/VisualVerifier.java#L646)); `verify-30-pharaoh-helmet-on.png` hides the helmet under the Nemes headdress, `verify-30b-pharaoh-helmet-off.png` shows it restored. The earlier "never equips a helmet" record was stale. |
| Spatial / Sky Lyrics | **BLOCKED** | No lyrics resolved offline (`lines=0`, lookup failed). |
| Ambience sky tint | **OBSERVED, UNTESTED** | `verify-29-ambience-sky.png` warm sky; precedence vs CustomSky still to be regression-tested. |
| ClickGUI, block outline, afterglow, CustomSky galaxy | Baseline-verified at `69cf5df`; scenes recaptured here but not individually re-inspected | `verify-14/15/16/17/18*`, `21`, `24`, `07*`. |

## Open defects (priority order)

1. **P1 — FreeLook self-disables mid-scene.** Root cause undiagnosed; needs systematic debugging
   (state save/restore around `Player#setYRot`, camera yaw vs body yaw ownership). Owner: Agent 1.
2. **P1 — HUD right column overlaps Potion Effects (JA-009).** Layout collision in the full HUD
   preset. Owner: Agent 4.
3. **P2 — Terrain shockwave invisible at runtime.** `TerrainWave` is source-reviewed and its bounds
   test passes, but no geometry reaches the frame. Check trigger, lifetime and render pass.
   Owner: Agent 2.
4. **P3 — Radiance renders as a translucent rectangular column.** Owner: Agent 2 unless the
   geometry proves to be owned by the character renderer, then Agent 3.
5. **Environment limitation — lyrics unverified offline.** Owner: Agent 4, blocked.
6. **P5 — hot-path allocations.** `HitEffectsModule` per-block `TerrainWave.blockTop` allocation and
   the 1400-cell grid vs 240/frame cap; `SpatialLyricsModule` per-frame allocations.
   Owners: Agent 2 and Agent 4 respectively; must not regress the visual fixes.

## Lane state

| Lane | Worktree branch | State |
|---|---|---|
| Agent 1 modules/mixins | `worktree-agent-general-purpose-d8fe6c58` (`2a2ca43`) | **INTEGRATED** in `27c8c61`; Fullbright runtime-proven, FreeLook still broken. |
| Agent 2 world FX | `worktree-agent-general-purpose-122e9e8e` (`b902c7b`) | **INTEGRATED** in `b7627c7`; sky insets and budgets in, shockwave not visible. |
| Agent 3 models | `worktree-agent-general-purpose-4da0c1e7` (`56b5700`) | **INTEGRATED** in `0bbfe12`; GLB selection, cache stamping, local rotation. |
| Agent 4 GUI/HUD/music | `worktree-agent-general-purpose-5ab277c3` (`ecbc1b2`) | **INTEGRATED** in `d0f9e37`; media bridge opt-in, screenshot reveal. |
| Agent 5 visual reference | — | Idle by design; no user references queued. |

## Open risks

- Shared build cache: every gate must use the isolated init script and the shared lock
  (`C:/tmp/jilaton-arch/.build.lock`). Concurrent builds clobber the Loom cache.
- Counts and hashes above describe one tree state (`d0f9e37`) only; any later edit invalidates them.
- The `run-verify` world is a 163 MB copy of the user's save; the original is untouched.
- New or changed mixins need a live boot log every time, not just a green compile.
- Cross-owner edits: only the owning lane may touch `ClientConfig`, mixins and shared HUD code.

## Next milestone

Fix P1 (FreeLook state, HUD overlap) on fresh lane branches, rerun the combined gate and the
harness on the merged tree, then address the shockwave and Radiance rendering defects.
