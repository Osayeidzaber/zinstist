# Jilaton — Agent Progress

Compact per-agent records. History lives in Git; status in `PROJECT_STATUS.md`; work queue in
`BACKLOG.md`. All four lanes are merged into `d0f9e37`; no agent is running right now.

## Agent 1 — Core modules, mixins, source integration

### Active Task
P1-1 — FreeLook self-disables during the KillAura/FreeLook scene (diagnosis, not patched).

### Completed
- P0-3 — `LightTextureFullbrightMixin` corrected (`remap = false`, `require = 1`); boot-proven.
- P1-3 — Fullbright fixed and runtime-verified against the baseline defect.
- P2-2 — `ReachMath` protocol caps; AutoClicker `delayAfterBlockBreak` guard preserves the
  first-click sentinel semantics (`Long.MIN_VALUE`).
- FreeLook state-loss fix landed (state saved/restored across enable/disable) but the module
  still self-disables mid-scene, so the defect is not closed.
- Registry/persistence audit tests added (P2-3 partial).

### Changed Files
- `mixin/LightTextureFullbrightMixin.java`, `mixin/PlayerReachMixin.java`
- `module/impl/ReachMath.java`, `module/impl/FreeLookModule.java`, `module/impl/AutoClickerModule.java`
- `module/impl/AutoClickerPolicy.java`, `module/impl/ReachMathTest.java`, audit test classes

### Verification
- Automated: lane gate green before merge; merged gate 195 suites / 905 tests / 0 failures.
- Runtime: Fullbright `verify-26` vs `26b` proves off/on difference.
- Runtime: FreeLook ERROR reproduced on the integrated build with body/head yaw telemetry.

### Blockers
- FreeLook root cause unknown. Evidence: `bodyYaw`/`headYaw` follow the aura to −90° while
  `cameraYaw` stays −12°, then the module disables itself at scene end. Reproduce and diagnose
  before changing code.

### Next
P1-1 diagnosis; then P1-8 harness scene that equips a helmet; then P2-1/P2-3 module sweep.

## Agent 2 — World rendering and visual effects

### Active Task
P3-9 — terrain shockwave does not render; P1-11 Radiance rectangular column.

### Completed
- P3-1 — sky UV insets derived from real texture sizes (JA-018) + `SkyInsetVerificationTest`.
- Lyric fade/`LyricStates` and world-FX budget work (`WorldFxBoundsTest`).
- `FxParticles`/`FxShapes`/`FxFrame`/`TerrainWave` source-reviewed and gated.

### Changed Files
- `render/fx/FxFrame.java`, `FxParticles.java`, `FxShapes.java`, `LyricSky.java`,
  `LyricStates.java`, `LyricTrail.java`, `SkyPackTextures.java`, `TerrainWave.java`
- `SkyInsetVerificationTest.java`, `SkyLyricsFadeTest.java`, `WorldFxBoundsTest.java`

### Verification
- Automated: merged gate green.
- Runtime: sky scenes recaptured; `verify-28b-terrain-wave-wide.png` shows no wavefront;
  `verify-23-radiance.png` shows the translucent column.

### Blockers
- Shockwave: unknown whether the trigger, lifetime or render pass fails; must trace one attack
  frame end-to-end. `<verify-28>` was captured third-person on a flat plaza.

### Next
Diagnose shockwave then Radiance geometry; then P3-2 TargetESP link and P3-4 Ambience precedence.

## Agent 3 — 3D models and character animation

### Active Task
None running; P3-5 / P3-8 remain open.

### Completed
- P1-4 — custom GLB selection chain (JA-003), `.gltf` rejection message, cache stamping
  (`BAKE_VERSION 3`, `cacheId(name,size,mtime)`), `CharacterCatalog.refresh(Path)`.
- P1-5 — `Transform.localRotation`/`spin`; `BodyAnchor`/`KnifeThirdPerson` handedness fix.
- Five regression test classes added (selection, cache lifecycle, transform math).

### Changed Files
- `render/character/CharacterCatalog.java`, `CustomCharacters.java`, `CharacterCache.java`
- `render/model/Transform.java`, `BodyAnchor.java`, `KnifeThirdPerson.java`
- `CharacterModule.java`; `GlbSelectionRegressionTest`, `CharacterCacheLifecycleTest`,
  `TransformMathTest`

### Verification
- Automated: merged gate green.
- Runtime: cosmetic scenes recaptured; model insertion itself not re-judged in this phase.

### Blockers
- Out-of-lane `.rotate` call sites remain (`WingsModule:117`, `CompanionModule:125/130/140`, and
  `CompanionModule:131` `compose(Transform.rotation(...))`); ownership review before edits.

### Next
P3-5 ViewModel/SwingAnimation transform coordination; then P3-8 attachment review.

## Agent 4 — GUI, HUD and music

### Active Task
P1-2 — HUD right column overlaps the Potion Effects panel (JA-009).

### Completed
- P1-6 — media bridge opt-in (`MediaBridgePolicy`, `desktopMediaBridge` default false).
- P1-7 — Spotify token hardening (`SpotifyTokenPolicy`, no value logging, delete on disconnect).
- Screenshot Copy verified at runtime; `ScreenshotReveal` added for the Open path.
- `MusicCenter`/`MusicCenterScreen` lyric smoothing work; blur/opacity verified earlier.

### Changed Files
- `music/MediaBridgePolicy.java`, `MediaSessionSource.java`, `SpotifyTokenPolicy.java`,
  `SpotifyConnect.java`, `OnlineLyrics.java`, `MusicCenter.java`, `MusicCenterScreen.java`
- `config/ClientConfig.java`, `JilatonClient.java`, `screenshot/ScreenshotReveal.java`
- six privacy/screenshot test classes

### Verification
- Automated: merged gate green.
- Runtime: `[CHAT] Copied screenshot` in scene 31; `verify-20-hud-full.png` reproduces JA-009.

### Blockers
- Lyrics still unresolved offline (`lines=0`), so spatial/Sky Lyrics remain source-reviewed only.
- Follow-ups: `MusicCenterModule` `online_lyrics` should default false; `ScreenshotClickMixin`
  OPEN_ID branch needs pairing with `ScreenshotChatActions.handleOpen` + `ScreenshotReveal.reveal`;
  `attachConfig` wiring needs a note in the config owner's record.

### Next
Fix the HUD column layout, then ClickGUI persistence (P1-9) and music HUD polish (P3-7).

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
