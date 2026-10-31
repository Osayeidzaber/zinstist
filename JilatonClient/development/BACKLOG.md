# Jilaton — Active Backlog

One line per item. Status: TODO / ACTIVE / PARTIAL / INTEGRATED / BLOCKED.
Ownership boundaries are defined in `ORCHESTRATION.md`; only the owner edits the files.
All "INTEGRATED" items below are merged into `d0f9e37` and covered by the 195-suite gate.

## P0 — Stability

| ID | Item | Owner | Status |
|---|---|---|---|
| P0-1 | Protect lane work; isolate builds, game dirs, logs | Master | INTEGRATED — lanes merged, isolated build dirs used throughout |
| P0-2 | Combined gate on the integrated tree | Master | INTEGRATED — 195 suites / 905 tests / 0 failures on `d0f9e37` |
| P0-3 | Boot-prove `LightTextureFullbrightMixin` (`remap = false`, `require = 1`) | Agent 1 | INTEGRATED — no mixin errors in the 2026-11-01 run; Fullbright runtime-proven |

## P1 — Broken functionality

| ID | Item | Owner | Status |
|---|---|---|---|
| P1-1 | FreeLook self-disables during the KillAura/FreeLook scene | Agent 1 | ACTIVE — telemetry: body/head yaw → −90°, camera yaw pinned at −12°, then self-disable at scene end |
| P1-2 | HUD right column overlaps Potion Effects (JA-009) | Agent 4 | ACTIVE — `verify-20-hud-full.png` reproduces on the integrated build |
| P1-3 | Fullbright mapping warning / silent miss | Agent 1 | INTEGRATED — runtime-verified (off/on comparison) |
| P1-4 | Custom GLB selection chain (JA-003) plus `.gltf` rejection message | Agent 3 | INTEGRATED — `CharacterCatalog.refresh`, cache stamping, regression tests |
| P1-5 | Model frame handedness: `Transform.spin` for local rotations | Agent 3 | INTEGRATED — `Transform.localRotation/spin` + `TransformMathTest` |
| P1-6 | Media bridge must be opt-in and bounded (privacy) | Agent 4 | INTEGRATED — `MediaBridgePolicy`, `desktopMediaBridge` default false |
| P1-7 | Spotify token file hardening (privileges, no value logging, delete on disconnect) | Agent 4 | INTEGRATED — `SpotifyTokenPolicy` + tests |
| P1-8 | Pharaoh helmet hiding: harness scene must equip a helmet | Agent 1 (with Master) | INTEGRATED — scene 30 already equips an iron helmet ([VisualVerifier.java:646](file:///C:/Users/zaber/Desktop/zinstist/JilatonClient/src/main/java/dev/jilaton/client/dev/VisualVerifier.java#L646)); runtime-verified by `verify-30`/`30b` (hidden on, restored off). Recorded gap was stale |
| P1-9 | ClickGUI toggle-only, persistence, status label, blur/opacity | Agent 4 | PARTIAL — blur/opacity runtime-verified; persistence still pending |
| P1-10 | Sky Lyrics / spatial lyrics runtime check (blocked offline) | Agent 4 | BLOCKED — no lyrics lookup offline |
| P1-11 | Radiance renders as a translucent rectangular column | Agent 2 (to Agent 3 if character geometry) | ACTIVE — `verify-23-radiance.png` reproduces on the integrated build |

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
| P3-9 | Terrain shockwave must actually render a wavefront | Agent 2 | ACTIVE — `verify-28b-terrain-wave-wide.png` shows no geometry |

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

1. Agent 1: FreeLook hold-key revocation root cause; fix + scene 32 harness adjustment (Master owns `VisualVerifier`).
2. Agent 4: HUD right-column layout collision (JA-009).
3. Agent 2: terrain shockwave trigger/lifetime/render-pass; Radiance geometry.
4. Agent 3: ViewModel / SwingAnimation transform coordination.
5. Combined gate + harness rerun; status and backlog refresh with new hashes.

## Known non-goals

- Do not merge `main`.
- Do not copy GPL-3.0 LiquidBounce source without licence compliance.
- Do not extract desktop tokens or scrape browser/desktop sessions.
- Do not launch or load unknown reference JARs.
