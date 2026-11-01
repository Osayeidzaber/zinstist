# Jilaton — Agent Progress

Compact per-agent records. History lives in Git; status in `PROJECT_STATUS.md`; work queue in
`BACKLOG.md`. All four lanes are merged into `e6c5179`; no agent is running right now.

## Agent 1 — Core modules, mixins, source integration

### Active Task
None. P1-1 is integrated and runtime-verified within harness scope; P2-1/P2-3 remain the next queue item.

### Completed
- P0-3 — `LightTextureFullbrightMixin` corrected (`remap = false`, `require = 1`); boot-proven.
- P1-1 — hold-key revocation root cause found and fixed: `ModuleManager` revoked hold modules every
  tick the physical key was up, before FreeLook's own tick. `HoldKeyAction` is now a pure edge decision
  (`unfocused → disable`, `press → enable`, `release → disable`, `no edge → unchanged`).
  `HoldKeyPolicyTest` covers all five cases and fails 2/5 with the old rule restored.
- P1-3 — Fullbright fixed and runtime-verified against the baseline defect.
- P2-2 — `ReachMath` protocol caps; AutoClicker `delayAfterBlockBreak` guard preserves the
  first-click sentinel semantics (`Long.MIN_VALUE`).
- Registry/persistence audit tests added (P2-3 partial).

### Changed Files
- `mixin/LightTextureFullbrightMixin.java`, `mixin/PlayerReachMixin.java`
- `module/impl/HoldKeyAction.java`, `FreeLookModule.java`, `ModuleManager.java`,
  `ReachMath.java`, `AutoClickerModule.java`, `AutoClickerPolicy.java`, `HoldKeyPolicyTest.java`

### Verification
- Automated: lane gate 196/910 green; merged gate 200 suites / 935 tests / 0 failures on `e6c5179`.
- Runtime: `verify-32-freelook-killaura.png` — FreeLook stays enabled for all 24 telemetry ticks,
  `bodyYaw` driven −24 → −90 while `cameraYaw=0.0`, and **no** `module free_look disabled itself` ERROR.

### Blockers
- None for P1-1. Residual: the harness drives body yaw, so FreeLook's camera-only behaviour is still
  unasserted; a first-person capture would settle it. Intentional consequence of the fix: a hold module
  that disables itself stays off until the key is released and pressed again.

### Next
P2-1 reference inventory sweep, then P2-3 module registry/lifecycle completeness review.

## Agent 2 — World rendering and visual effects

### Active Task
P3-9 — shockwave renders no geometry at runtime; P1-11 Radiance shows no radial glow. Diagnosis only,
no lane branch open.

### Completed
- P3-8 wave lifetime: `TerrainWave.impactLife(life) = max(life*1.2, 1.6 s)` so a wave survives its whole
  sweep; one reused `double[12]` buffer; per-wave `blockBudget(liveWaves)` keeping ≤1400 quads/frame;
  front-band selection extracted to `TerrainWave.selectsCell(...)`.
- P1-11 geometry: replaced the six-face AABB shell with two feathered camera-facing elliptical layers
  (`FxShapes.softEllipse`, `auraFeather`, `auraExtents`); `softDot` now delegates to `softEllipse`.
- P3-1 — sky UV insets from real texture sizes (JA-018) + `SkyInsetVerificationTest` (earlier lane).
- Ledger tests: `WaveLifetimeTest`, `WorldFxBoundsTest`, `RadianceAuraTest`.

### Changed Files
- `render/fx/TerrainWave.java`, `FxShapes.java`, `HitEffectsModule.java`, `RadianceModule.java`
- Lane-side test classes `WaveLifetimeTest`, `RadianceAuraTest`

### Verification
- Automated: lane gate 196/912 green; merged gate 200/935 on `e6c5179`.
- Runtime: `verify-28` and `verify-28b` (radius 21, capture moved to `shot(4)`) still show an empty plaza
  with only brown impact debris — **no wavefront**. `verify-23-radiance.png` no longer shows the
  translucent rectangle, but no radial glow is discernible either.

### Blockers
- Both defects now look like "the effect does not reach the frame at all" rather than wrong parameters.
  The lifetime and budget hypotheses are already excluded by the lane work, so the next step is a
  render-pass trace (module tick → wave list → render hook → quad submission) for one attack frame.

### Next
Trace one attack frame end-to-end; then confirm whether the Radiance ellipses are submitted at all;
then P3-2 TargetESP link and P3-4 Ambience precedence.

## Agent 3 — 3D models and character animation

### Active Task
None running. P3-5 (ViewModel/SwingAnimation coordination) and P3-8 attachment review remain open.

### Completed
- P3-5 audit — no double-transform defect: the mapped 1.21.11 `ItemInHandRenderer` has exactly one
  push/pop pair in `renderArmWithItem` and the ViewModel hook is injected right after the push.
- P3-5 fixes — wing fan no longer mirrors across the spine; companion parts share one handedness
  (`CompanionMotion.bankedFrame/ringFrame/shardFrame`, `WingMotion.featherFrame`,
  `FEATHER_SWEEP_BACK = -0.16`); added `Transform.mirrorX()`, `lookAlong`, `spin`.
- P1-4 — custom GLB selection chain (JA-003), `.gltf` rejection message, cache stamping.
- P1-5 — `Transform.localRotation`/`spin`; `BodyAnchor`/`KnifeThirdPerson` handedness fix.

### Changed Files
- `render/character/CharacterCatalog.java`, `CustomCharacters.java`, `CharacterCache.java`
- `render/model/Transform.java`, `BodyAnchor.java`, `KnifeThirdPerson.java`, `CompanionMotion.java`,
  `WingMotion.java`
- `FirstPersonTransformCoordinationTest` (5), `EffectFrameOrientationTest` (6)

### Verification
- Automated: lane gate 197/917 green; merged gate 200/935 on `e6c5179`.
- Runtime: cosmetic scenes recaptured in this run; not re-judged individually.

### Blockers
- Documented product gaps, not fixed: `FirstPersonHandsAndItemsMixin` only replaces `swingArm`, so the
  empty-hand inline swing never gets a SwingAnimations style; `KnifeViewmodelMixin` HEAD-cancels
  `renderArmWithItem`, bypassing ViewModel and swing styles.

### Next
P3-5 remainder (the two bypassing mixins), then P3-8 cosmetic attachment review.

## Agent 4 — GUI, HUD and music

### Active Task
P1-12 — target card chip row (`FALL / ARMOR / ABSORB`) overlaps the vanilla hotbar.

### Completed
- P1-2 (JA-009) — HUD right column: module list → potion effects → CPS form one column, bottom-right
  chain stacked upward from `guiHeight - 8`; `HudColumnStackTest` pins `effects.top == modules.bottom + 6`.
- P1-6 — media bridge opt-in (`MediaBridgePolicy`, `desktopMediaBridge` default false).
- P1-7 — Spotify token hardening (`SpotifyTokenPolicy`, no value logging, delete on disconnect).
- Screenshot Copy runtime-verified; `ScreenshotReveal` added for the Open path.

### Changed Files
- `gui/HudRenderer.java`, `gui/HudDesign.java`, `gui/HudPreset.java`
- `music/MediaBridgePolicy.java`, `MediaSessionSource.java`, `SpotifyTokenPolicy.java`,
  `SpotifyConnect.java`, `OnlineLyrics.java`, `MusicCenter.java`, `MusicCenterScreen.java`
- `config/ClientConfig.java`, `JilatonClient.java`, `screenshot/ScreenshotReveal.java`
- `HudColumnStackTest`, rewritten `MusicHudOverlapTest`, privacy/screenshot test classes

### Verification
- Automated: merged gate 200/935 on `e6c5179`.
- Runtime: `verify-19-hud-minimal.png` and `verify-20-hud-full.png` show the module list ending ≈y205
  and `POTION EFFECTS` starting ≈y232 — P1-2 fixed. The same frames also show the chip row sitting on
  the hotbar (P1-12). Log has `[CHAT] Copied screenshot` for scene 31.

### Blockers
- Lyrics unresolved offline (`lines=0`), so spatial/Sky Lyrics remain source-reviewed only.
- P1-9 ClickGUI persistence needs a Master-owned `ClientConfig` schema addition before the lane can start.

### Next
Fix P1-12 (default Y must account for the chip-row height at `HudRenderer.java:634-635`); then P3-6 HUD
drag/scale persistence and P3-7 music HUD polish; P1-9 once the config schema lands.

## Agent 5 — Visual reference intelligence

### Active Task
None. Idle by design.

### Completed
- Earlier reference pack reviewed (`video-reference-review.md`); no new user references queued.

### Changed Files
- None; this agent never edits rendering or GUI code.

### Verification
- Not applicable — no specification produced in this phase.

### Blockers
- Needs actual attachment paths from the user; cannot see another session's attachments.

### Next
On receipt of a screenshot/video, return a six-field spec (Concept, Visual behavior, Technical
requirements, Existing implementation, Required changes, Acceptance) to the Master.
