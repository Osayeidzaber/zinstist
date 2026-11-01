# Jilaton — Active Backlog

One line per item. Status: TODO / ACTIVE / PARTIAL / INTEGRATED / BLOCKED.
Ownership boundaries are defined in `ORCHESTRATION.md`; only the owner edits the files.
All "INTEGRATED" items below are merged into `e6c5179` and covered by the 200-suite / 935-test gate.

## P0 — Stability

| ID | Item | Owner | Status |
|---|---|---|---|
| P0-1 | Protect lane work; isolate builds, game dirs, logs | Master | INTEGRATED — lanes merged, isolated build dirs used throughout |
| P0-2 | Combined gate on the integrated tree | Master | INTEGRATED — 200 suites / 935 tests / 0 failures / 0 errors / 0 skipped on `e6c5179`; jar SHA-256 `fcf89986…66c1` |
| P0-3 | Boot-prove `LightTextureFullbrightMixin` (`remap = false`, `require = 1`) | Agent 1 | INTEGRATED — no mixin errors in the 2026-11-01 run; Fullbright runtime-proven |

## P1 — Broken functionality

| ID | Item | Owner | Status |
|---|---|---|---|
| P1-1 | FreeLook self-disables during the KillAura/FreeLook scene | Agent 1 | INTEGRATED — edge-based hold policy (`956cfa5`); `verify-32` has no self-disable ERROR and FreeLook stays on for all 24 ticks. Camera-only behaviour still unasserted (harness drives body yaw) |
| P1-2 | HUD right column overlaps Potion Effects (JA-009) | Agent 4 | INTEGRATED — `verify-19`/`verify-20` show a clean gap between the module list and POTION EFFECTS |
| P1-3 | Fullbright mapping warning / silent miss | Agent 1 | INTEGRATED — runtime-verified (off/on comparison) |
| P1-4 | Custom GLB selection chain (JA-003) plus `.gltf` rejection message | Agent 3 | INTEGRATED — `CharacterCatalog.refresh`, cache stamping, regression tests |
| P1-5 | Model frame handedness: `Transform.spin` for local rotations | Agent 3 | INTEGRATED — `Transform.localRotation/spin` + `TransformMathTest` |
| P1-6 | Media bridge must be opt-in and bounded (privacy) | Agent 4 | INTEGRATED — `MediaBridgePolicy`, `desktopMediaBridge` default false |
| P1-7 | Spotify token file hardening (privileges, no value logging, delete on disconnect) | Agent 4 | INTEGRATED — `SpotifyTokenPolicy` + tests |
| P1-8 | Pharaoh helmet hiding: harness scene must equip a helmet | Agent 1 (with Master) | INTEGRATED — scene 30 already equips an iron helmet ([VisualVerifier.java:646](file:///C:/Users/zaber/Desktop/zinstist/JilatonClient/src/main/java/dev/jilaton/client/dev/VisualVerifier.java#L646)); runtime-verified by `verify-30`/`30b` (hidden on, restored off). Recorded gap was stale |
| P1-9 | ClickGUI toggle-only, persistence, status label, blur/opacity | Agent 4 | PARTIAL — persistence is already wired (both GUIs read it in the constructor and write in `remember()`/`syncStateToConfig()` + `config.save(modules)`; card reorder writes `setModuleOrder`/`setVisualOrder`), but it is source-reviewed only, never restart-tested. Status label already reads `1/6 SW ON` with a "SW = switches on" legend (`CardMetric.java:36`, `JilatonClickGui.java:412`). Blur/opacity runtime-verified. Click-to-open-settings is unverified |
| P1-10 | Sky Lyrics / spatial lyrics runtime check (blocked offline) | Agent 4 | BLOCKED — no lyrics lookup offline |
| P1-11 | Radiance renders as a translucent rectangular column | Agent 2 | PARTIAL — feathered two-layer rewrite (`e9e58ec`) removed the rectangle in `verify-23-radiance.png`, but no radial glow is visible. **Root cause narrowed (Master, 2026-11-01):** "nothing is submitted" is ruled out. An enabled `FxEmitter` makes `anyFx()` true (`EntityOverlayRenderer.java:148`), the per-emitter loop has no extra gate (`:170-178`), and the gated run's `run-verify/logs/latest.log` contains **zero** `World effect ... failed` lines, so `emit()` ran every frame and was never auto-disabled; it iterates `entitiesForRendering()` and submits two `softEllipse` fans per entity. Remaining hypothesis is amplitude, not reachability: at the scene's `strength 0.34` / `thickness 0.24` / `pulse false` the inner layer peaks at ≈0.187 alpha and the outer at ≈0.075, and because `softEllipse` ramps centre→rim→centre with rim alpha 0 the mean fill is ≈6% / ≈2.5%, plausibly below perceptual threshold. **Harness defect found and fixed (Master):** `VisualVerifier.setting(...)` covered Choice/Boolean/Number but not `TextSetting`, so every `hexSetting` (including `radiance.color`) was a silent no-op — proven by log line 623 `setting radiance.color not found`; no scene had ever exercised a custom hex. Acceptance for the re-run: a visible aura around the player and mob in `verify-23-radiance.png` at the documented settings, with no `World effect` auto-disable line in the log |
| P1-12 | Target card chip row overlaps the vanilla hotbar | Agent 4 | REVIEWED (branch `47bdf41`, not yet merged) — default top is now `HudDesign.aboveHotbarTop(guiHeight, scaledHeight)` = `guiHeight - (HOTBAR_HEIGHT 22 + HUD_MARGIN 8) - height`, so the card's bottom edge holds an 8 px gap above the hotbar at every scale and the chip row grows upward; the saved `HudPosition` branch in `widgetY` is untouched. Master diff review: production change is 2 files / 22 lines, no behaviour change for the 46 px card (reproduces the historic `guiHeight - 76`), `TargetCardAnchorTest` (6 tests, sweep of 4 chip/held combos × 5 scales × 8 GUI heights) plus the lane's own gate reports 941 tests / 0 failures. **RUNTIME-CONFIRMED on the gated build** (Master, 2026-11-01 00:07): in `run-verify/screenshots/verify-19-hud-minimal.png` and `verify-20-hud-full.png` the card's `FALL/ARMOR/ABSORB` chip row is clipped by the frame edge and the vanilla hotbar is not visible at all. Cause: `HudRenderer.java:635` passes a fixed `guiHeight - 76` default, while the card's height varies with `chips`/`held` (`HudDesign.java:138`). Fix must bottom-anchor on `scaledHeight`, keep the stored-position branch, and pass both presets in a re-run harness — acceptance is the whole card, chip row included, fully visible with the hotbar uncovered in scenes 19 and 20. Audit (Master, 2026-11-01): this is the only HUD site that pairs a fixed bottom anchor with a variable height — the bottom-right chain derives every top from real heights (`HudRenderer.java:1276` `stackUp`), `effects`/`keybinds` grow inside clamped top-down limits, and `staff`'s fixed `guiHeight - 210` clears its 131 px maximum. No sibling defect |

## P2 — Reference integration

| ID | Item | Owner | Status |
|---|---|---|---|
| P2-1 | Anubis/LiquidBounce static inventory; licence-compliant reuse only | Agent 1 | ACTIVE (read-only) |
| P2-2 | Reach / interaction limits, block-break feedback | Agent 1 | INTEGRATED — `ReachMath` caps + clicker break-delay guard; runtime behaviour not yet observed |
| P2-3 | Module registry/lifecycle audit, settings persistence | Agent 1 | PARTIAL — persistence/registry audit tests added; full module sweep outstanding |

## P3 — Visual quality

| ID | Item | Owner | Status |
|---|---|---|---|
| P3-1 | Sky UV insets from real texture sizes (JA-018) + regression tests | Agent 2 | INTEGRATED — `FxFrame.recordSkyTextureSize` + `SkyInsetVerificationTest` |
| P3-2 | TargetESP linked to KillAura selected target; distinct presets | Agent 2 | TODO |
| P3-3 | One Jump Circle per jump; bounded shockwave (40–50 block radius) | Agent 2 | TODO |
| P3-4 | Ambience/CustomSky precedence and conflicts | Agent 2 | TODO — `verify-29-ambience-sky.png` shows the tint; precedence untested |
| P3-5 | ViewModel / SwingAnimation coordination, pivots, transforms | Agent 3 | PARTIAL — local-rotation/spin landed; ViewModel/Swing coordination outstanding |
| P3-6 | HUD scaling, drag persistence, Target HUD scaling | Agent 4 | TODO |
| P3-7 | Music HUD artwork/title, first-person info, lyric smoothing | Agent 4 | TODO |
| P3-8 | Cosmetic column over legs; model attachment review | Agent 3 | TODO |
| P3-9 | Terrain shockwave must actually render a wavefront | Agent 2 | ACTIVE — lifetime and front-band fixes are integrated (`e9e58ec`), yet `verify-28`/`28b` at radius 21 still show no geometry. Next step is a render-pass trace, not another budget change |

## P4/P5 — Features, performance, release

| ID | Item | Owner | Status |
|---|---|---|---|
| P4-1 | Quality presets for world FX | Agent 2 | TODO |
| P4-2 | Cosmetic inspection animation and keybind | Agent 3 | TODO |
| P5-1 | Per-frame scans / particle caps / cleanup (JA-008) | Agent 2 | TODO |
| P5-2 | Keep dev harness out of release jar (JA-015) | Master | TODO before release |
| P5-3 | Branch hygiene (JA-021), release manifest refresh | Master | TODO |
| P5-4 | Hot-path allocations: `HitEffectsModule` per-block `TerrainWave.blockTop` + 1400-cell grid vs 240/frame cap; `SpatialLyricsModule` per-frame allocs | Agent 2 / Agent 4 | TODO |

## Integration queue

1. Master: commit the harness scene 28 edit and these records (no push without user permission).
2. Agent 2: trace one attack frame end-to-end (emit → wave list → render pass → quad submission) for
   the shockwave, then determine whether the Radiance ellipses draw at all.
3. Agent 4: fix P1-12 target-card/hotbar overlap; then P1-9's remaining acceptance — restart round trip
   for page/category/module/expanded/scroll/order and a card-body click that opens settings. No config
   schema work is needed; the persistence fields and their save calls already exist.
4. Next integrated head: rerun the combined gate and the harness; refresh status/backlog with new hashes.

## Known non-goals

- Do not merge `main`.
- Do not copy GPL-3.0 LiquidBounce source without licence compliance.
- Do not extract desktop tokens or scrape browser/desktop sessions.
- Do not launch or load unknown reference JARs.
