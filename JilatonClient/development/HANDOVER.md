# Jilaton — Handover (device switch)

**Written:** 2026-11-01 05:37 local / 2026-10-31T23:37Z by Master session `15756b54-ca7b-4178-a6d7-ecbc10b14ae4`.
**Updated:** 2026-11-01 05:52 local — lanes 1, 2 and 3 all finished and were pushed; no gate on the integrated
tree and no runtime evidence since `d0f9e37`.
**Goal:** registered "Jilaton — Master Architect & Autonomous Development Team" objective. Last reported budget
4/20 turns used; the user paused the session and is switching devices. Resume with `/goal resume`.

## 1. Repository snapshot

- Git root: `C:\Users\zaber\Desktop\zinstist`; Gradle project: `JilatonClient` (Bash cwd is `.../JilatonClient`,
  so use absolute paths for `.qoder/...` and `git -C`).
- Branch `jilaton/2026-10-31-work`; last known HEAD `a2e9355` plus this update (`git log -1` after checkout).
  Pushed to `origin/jilaton/2026-10-31-work`.
- `main` (`f11bfff`) is old and must not be merged.
- Lane branches pushed and all **complete**:
  `origin/lane/2026-10-31-hold-key-edge` = `ec77a80`,
  `origin/lane/2026-10-31-world-fx-wip` = `d008267`,
  `origin/lane/2026-10-31-viewmodel-wip` = `61aa076`.
  None of them is merged into the work branch yet; each is green only in its own lane build.
- Resume on a new machine: clone, `git checkout jilaton/2026-10-31-work`, then
  `git worktree add <path> lane/2026-10-31-hold-key-edge` etc. `development/` docs are now tracked.
- Environment: Windows 10 x64, Temurin 21, Gradle 9.8.2 wrapper, Fabric Loader 0.20.1 / Fabric API 0.141.6 /
  Loom 1.14.10, Minecraft 1.21.11. The objective's macOS assumption does not apply.

Commits since the previous integrated head `d0f9e37`:

| SHA | Content |
|---|---|
| `c02c7ec` | VisualVerifier scene 32: clears FreeLook's hold binding before enabling it (Master) |
| `f1cd094` | Lane 4: HUD right-column stacking (Agent 4, branch `worktree-agent-general-purpose-60ae4d60`) |
| `24e9ce9` | Merge of lane 4 (no-ff) |
| `ec77a80` | Lane 1: edge-based hold-key policy + `HoldKeyPolicyTest` (Agent 1) — **committed, NOT merged** |
| `b6a9a59` | Lane 2 red-phase tests: wave lifetime, band selection, block budget, radiance aura (Agent 2) |
| `6bf22ce` | Lane 2: `TerrainWave.impactLife` keeps a wave alive for its whole 1.6 s sweep; per-wave block budget |
| `d008267` | Lane 2: radiance drawn as two feathered camera-facing layers instead of the six-face shell |
| `79c1934` | Lane 3: `FirstPersonTransformCoordinationTest` pins the mapped 1.21.11 transform order (Agent 3) |
| `b2c10a5` | Lane 3 mid-edit snapshot (superseded by `61aa076`; unreviewable, keep only for history) |
| `61aa076` | Lane 3: wing fan opens away from the spine, companion parts share one handedness, `Transform.mirrorX` |

## 2. What is integrated and why

- **HUD right column (JA-009).** Music → enabled-module list → potion effects → CPS now form one column; the
  potion panel is capped above the bottom-right chain. Radar, held item, server, armor, keystrokes are anchored to
  `guiHeight - 8` and stacked upward by `HudDesign.stackUp`. Sizes live in `HudDesign`; auto panels advance the
  cursor, dragged panels keep their saved fraction. Tests: new `HudColumnStackTest` (6) and rewritten
  `MusicHudOverlapTest`; the assertions pin `effects.top == modules.bottom + 6`.
- **Harness scene 32.** `VisualVerifier.java` calls `setting("free_look", "hold", false)` before enabling
  FreeLook, because `ModuleManager.updateKeybinds` revokes hold modules every tick the physical key is up.
- **Docs correction (untracked).** Pharaoh helmet hiding is actually exercised: scene 30 already runs
  `item replace entity @p armor.head with minecraft:iron_helmet` (~line 646) and `verify-30/30b` show the helmet
  hidden then restored. The old "never equips a helmet" claim was stale; PROJECT_STATUS/BACKLOG were corrected.

## 3. Lane state (six-agent cap: Master + 4 devs + reserved Agent 5)

All worktrees are under `C:/Users/zaber/Desktop/zinstist/.qoder/worktrees/agent-general-purpose-<suffix>`.

| Lane | Suffix | Branch HEAD | Worktree state | Report |
|---|---|---|---|---|
| Agent 1 — modules/FreeLook | `389712d8` | `ec77a80` | clean | completed; 196/910 gate; **not merged**; no runtime claim |
| Agent 2 — world FX shockwave + Radiance | `15d1682f` | `d008267` | clean | completed; 196/912 lane gate; **not merged**; no runtime claim |
| Agent 3 — ViewModel/Swing coordination | `9a155ccd` | `61aa076` | clean | completed; 197/917 lane gate; **not merged**; no runtime claim |
| Agent 4 — HUD/ClickGUI | `60ae4d60` | `f1cd094` | clean | completed, task id `a0eef55b3e7cb9d5d`; merged |
| Agent 5 — visual reference | — | — | not spawned | reserved, idle; no user references queued |

Lanes 2 and 3 were wrongly reported as stalled when this file was first written; both finished afterwards. No
lane is merged except lane 4, and no lane has runtime evidence.

**Lane 2 report (received, P3-9 + P1-11).** Shockwave root cause: `HitEffectsModule.emit()` culled every impact at
`lifetime*1.2` = 1.2 s while the wave expands for 1.6 s, and scene 28's `shot(12)` lands at ~1.35 s, past the cull;
budget was not the cause. Fix: `TerrainWave.impactLife(life) = max(life*1.2, 1.6 s)`, one reused `double[12]`
buffer instead of per-cell allocation, per-wave share `MAX_BLOCKS_PER_FRAME / liveWaves` (total still ≤ 1400
quads), front-band selection extracted to `TerrainWave.selectsCell(...)`. Radiance root cause: `shell()` drew six
AABB faces as border strips at the same peak alpha with `du = min(thickness, half-extent)`, so strips overlapped
into a translucent rectangle. Fix: two camera-facing feathered ellipse layers sized from the body footprint;
`FxShapes.softDot` delegates to a new shared `softEllipse`. **Known scene limitation:** the plaza is only ~24.5
blocks wide (`VisualVerifier.java:593` sets radius 44) so the wave leaves the built ground after ~0.45 s; a
visible wide front needs a Master-owned scene edit (radius 44 → ~20–22, `shot(12)` → `shot(4)`).
**Lane 3 report (received, P3-5).** Audit verdict SOURCE_REVIEWED, **no double-transform defect**: the mapped
1.21.11 `ItemInHandRenderer` has exactly one push/pop pair in `renderArmWithItem`, the ViewModel hook is injected
right after the push (before every arm transform and the single `swingArm` call), `swingArm` applies the attack
pose once, and `renderPlayerArm` never calls `swingArm`. Two real rotation defects were found and fixed instead:
the wing fan's inserted X mirror flipped it across the spine (outward dot −0.37), and the companion mixed
handedness (core/ring/tail det −1 vs shards +1). Adds `Transform.mirrorX()` and pure frame helpers, with
`FirstPersonTransformCoordinationTest` (5) and `EffectFrameOrientationTest` (6) pinning both. Documented, not
fixed (product gaps): `FirstPersonHandsAndItemsMixin` only replaces `swingArm`, so the empty-hand inline swing
never gets a SwingAnimations style; `KnifeViewmodelMixin` HEAD-cancels `renderArmWithItem`, bypassing ViewModel and
swing styles.

**Lane 1 report (received).** Root cause, evidence-backed: the verifier's END_CLIENT_TICK listener runs before
`MODULES.tick`, and inside `ModuleManager.tick` the keybind pass runs before module `tick`; FreeLook is hold-bound
to LEFT_ALT (default `hold=true`) and ALT was never down, so `setEnabled(down && windowActive)` revoked the
programmatic enable before FreeLook's own tick. `-12.000001` was the stale raw yaw field captured after the
release blend restored FIRST_PERSON (screenshot 32 shows first-person), not an active free look. The fix makes
`holdKeyAction(down, wasDown, windowActive)` a pure edge decision: unfocused window disables; press edge enables;
release edge disables; no edge means no change. `previousKeyStates` stores `down && windowActive`, so refocus with
the key held counts as a fresh press. Intentional consequence: a hold module that disables itself (e.g. FreeLook's
F5 guard) stays off until the key is released and pressed again. Zoom, the release blend, perspective restore,
camera-type guard, freecam hand-off and screen-open release are unchanged. `HoldKeyPolicyTest` covers all five
cases; the agent re-ran the suite with the old rule restored and 2/5 tests failed, so the tests do pin the fix.

**Lane 4 review (Master).** Read the full diff: no cross-owner edits (no VisualVerifier, ClientConfig, ClickGUI or
HudPreset changes). Residuals it reported: a 20+ module list at a 540-px GUI can still reach the bottom chain
(no room left); centered transient notifications can cross the held-item card.

## 4. Verification status — be precise

- **No gate has run on the integrated tree.** Per-lane figures, each from its own worktree on baseline
  `d0f9e37` (195 suites / 905 tests): lane 1 = 196/910, lane 2 = 196/912, lane 3 = 197/917, lane 4 = 196/911,
  all 0 failures / 0 errors / 0 skipped. None of these describes the merged tree, and they cannot simply be
  added because each lane's delta was measured against the same baseline.
- Last runtime evidence is the 2026-11-01 harness run on `d0f9e37` (32 scenes, exit 0, 124 screenshots in
  `JilatonClient/run-verify/screenshots/`): Fullbright RUNTIME_VERIFIED; screenshot Copy RUNTIME_VERIFIED;
  FreeLook BROKEN; HUD overlap BROKEN; terrain shockwave NOT VISIBLE; Radiance BROKEN; pharaoh verified after
  screenshot inspection; spatial/sky lyrics BLOCKED offline.
- The new hold-key fix, the HUD stacking fix and the scene-32 harness change have **no runtime evidence**.
- Status ladder to use: `SOURCE_REVIEWED → BUILD_VERIFIED → AUTOMATED_TESTED → RUNTIME_VERIFIED`.

## 5. Next steps in order

1. Resume the goal (`/goal resume`) if the turn budget paused it.
2. Fetch and add the lane branches as worktrees: `git fetch origin`, then
   `git worktree add ../lane1 lane/2026-10-31-hold-key-edge`, `../lane2 lane/2026-10-31-world-fx-wip`,
   `../lane3 lane/2026-10-31-viewmodel-wip`.
3. Review and merge in this order, one at a time, re-reading each diff first:
   `git diff d0f9e37..ec77a80` then `git merge --no-ff lane/2026-10-31-hold-key-edge -m "Integrate lane 1: hold-key edge policy"`;
   `git diff d0f9e37..d008267` then merge lane 2 (shockwave + radiance);
   `git diff d0f9e37..61aa076` then merge lane 3 (wing/companion orientation; ignore `b2c10a5`, it is a snapshot).
   Watch for conflicts: lanes 2 and 3 both touch `render/` but in different packages.
4. Run the combined gate on the integrated tree:
   `cd C:/Users/zaber/Desktop/zinstist/JilatonClient && ./gradlew --init-script C:/tmp/jilaton-arch/isolated-o.gradle test --no-daemon --console=plain`
   Sum `tests/failures/errors/skipped` from `.isolated-build-o/root/test-results/test/TEST-*.xml` (one build at a
   time; the init script takes `C:/tmp/jilaton-arch/.build.lock`). Record the jar SHA-256 from
   `.isolated-build-o/root/libs/jilaton-client-0.1.0.jar`. Do not publish a count that belongs to another tree.
5. Run the runtime harness (Master owns runtime): `./gradlew --init-script C:/tmp/jilaton-arch/isolated-o.gradle runVisualVerify --no-daemon --console=plain`.
   It quick-plays "Jilaton HUD verification" in `run-verify`, captures 32 scenes, auto-quits. Keep the new log under
   `C:/tmp/jilaton-arch/`. Confirm the boot log has no `InvalidMixinException`, failed injection,
   `BootstrapMethodError`, `NoSuchMethodError` or `ClassCastException`.
6. Inspect these screenshots before any success claim: `20-hud-full` (module list fully visible, POTION EFFECTS
   below it, bottom-right chain separated), `19-hud-minimal`, `23-radiance`, `28b-terrain-wave-wide`, `30/30b`
   pharaoh, `32-freelook-killaura`, plus `26/26b-fullbright` as a regression spot-check. Scene 32 acceptance with
   `c02c7ec` (hold cleared, so FreeLook is toggle-style): no `module free_look disabled itself` ERROR, FreeLook
   stays enabled for all 24 telemetry ticks while KillAura turns the body, camera yaw stays independent of the
   body turn, and the screenshot is third-person (the old run showed first-person because the release blend had
   already finished). The run must stay window-focused: FreeLook still releases when `!client.isWindowActive()`.
   For `28b-terrain-wave-wide` lane 2 recommends a Master-owned scene edit first: `world_wave_radius` 44 → ~20–22
   at `VisualVerifier.java:593` and `shot(12)` → `shot(4)` at ~line 608, otherwise the front has already left the
   ~24.5-block plaza and stays faint by design. `23-radiance` should now show two soft elliptical layers with no
   straight edges.
7. Update `PROJECT_STATUS.md`, `BACKLOG.md` and `AGENT_REPORTS.md` with the new HEAD, gate totals, jar SHA, and
   per-scene verdicts; keep them within the 100–150 / 50–80 line limits. These docs are tracked and pushed; commit
   further edits and push only when the user asks (nothing has been pushed to `main`).
8. Hand the freed lane (Agent 4) its next approved backlog item only after the gate: P1-9 ClickGUI persistence is
   still pending, but it needs ClientConfig schema additions, which are **Master-owned** — either implement the
   schema change first or split the task. P3-6 HUD scaling/drag persistence and P3-7 music HUD are lane-owned and
   can start without config changes.

## 6. Hard constraints (from the registered objective and repository rules)

- Never launch, load or execute reference JARs. Static inspection only (REA/CFR/javap) with SHA-256 evidence.
- Anubis is an authorized port source (user, 2026-10-30); LiquidBounce is layout/behaviour reference only unless
  GPL-3.0 obligations are met. Never copy GPL source without compliance.
- No credentials, tokens, browser/Microsoft/Discord profile data, or arbitrary process execution.
- One build at a time across lanes; isolated build dirs via `C:/tmp/jilaton-arch/isolated-*.gradle`.
- Never rebuild shared class outputs while Minecraft runs against them. No game instance was running at handover.
- One owner per subsystem; no concurrent edits to the same tracked file. `ClientConfig`, mixins and shared HUD
  infrastructure are Master-owned; lane agents may not change them unilaterally.
- Six agents maximum. Agent 5 only turns screenshots/videos into six-field specs; it never edits shared
  rendering/GUI code.
- Do not claim runtime success without launching; do not fabricate delegation.

## 7. Artifacts, tooling and stale material

- Init scripts: `C:/tmp/jilaton-arch/isolated-{a,b,c,d,e,f,g,h,i,m,o}.gradle`; shared lock file
  `C:/tmp/jilaton-arch/.build.lock`.
- Test XML: `.isolated-build-<lane>/root/test-results/test/TEST-*.xml`; jars under `.isolated-build-<lane>/root/libs/`.
- Runtime world `run-verify` is a 163 MB copy of the user's save; the original save is untouched.
- `development/ORCHESTRATION.md` is **stale** (describes an older eight-session team, "no worktrees", last commit
  `f11bfff`). Do not follow its queue policy or session table; current ownership is the lane table in section 3.
- Qoder agent tooling: background agents notify on completion and cannot be resumed or messaged; a new `Agent`
  call creates a new worktree and a new subagent instance. `mcp__builtin__list_agents` does not list live
  subagents — worktree SHAs are the liveness evidence. New-work rule remains: do not queue work onto a running
  lane; wait for completion, then assign.
- Known project hazards: counts/hashes are per-tree; post-gate edits invalidate them; new mixins need a live boot
  log; concurrent builds clobber the Loom cache (use the init script).
