# Jilaton Client — Overall Update

**Date:** 2026-10-31, ~21:15 local (15:15Z)
**Branch:** `jilaton/2026-10-31-work` (shared working tree, uncommitted)
**Client:** Minecraft 1.21.11 / Fabric Loader 0.20.1 / Java 21
**Report owner:** Orchestrator (lane O). Every claim below traces to a gate XML, a jar hash, a source read, or a harness log — nothing is inferred from a green build alone.

---

## 1. Where the project stands

All nine work streams (GUI, HUD, RenderFX, Lyrics/Music, Cosmetics, Combat, Movement, Models, plus shared infrastructure) are implemented and green on the headless gate: **180 suites / 835 tests / 0 failures / 0 errors / 0 skipped** at the last two independent lane gates. The shipping ClickGUI now supports grip-drag reordering with persistence, spells out its `SW ON` unit, and keeps per-category scroll state. Fullbright is a real lightmap change, screenshot chat actions are real clipboard/OS-file actions with macOS support, and the Pharaoh Headdress has a fully tested visibility predicate.

**The one real blocker is runtime, not code:** the client still cannot boot past resource reload because of one mixin rule (`PharaohHelmetMixin`). This was misdiagnosed twice — first as "helpers need `@Unique`", then as a stale build. It is neither: **Mixin requires every static mixin method to be `private`; `@Unique` does not exempt statics.** The fix is a small extraction (dispatched, in progress). Until it lands, no harness screenshots newer than 19:24 exist, so every visual acceptance item remains **runtime-unverified** even though it is build-verified.

A second, genuinely missing acceptance item was found today: the GUI background has **no blur and no opacity setting**. The Theme page exposes Accent / Reduced motion / Animation intensity / Font only, and `JilatonClickGui.renderBackground` never blurs. This is dispatched as C's follow-up 3.

---

## 2. Evidence legend

| Mark | Meaning |
|---|---|
| **RUNTIME_VERIFIED** | A screenshot or log line from a live client was captured and inspected |
| **BUILD_VERIFIED** | Compiles, targeted tests green in an isolated build dir, jar hash recorded |
| **IMPLEMENTED** | Code exists and was read; tests may exist; no build evidence quoted here |
| **OPEN** | Not done, or blocked |
| **PARTIAL** | Some sub-requirements met, others explicitly listed |

A full gate covers one tree state in one build dir. `BUILD SUCCESSFUL` is never treated as runtime evidence.

---

## 3. Acceptance criteria matrix

| # | Requirement (user's list) | Implementation | Evidence | Status |
|---|---|---|---|---|
| 1 | GUI turns modules on **only** via the dedicated toggle; card body opens settings | `JilatonClickGui.toggleHit` + press order (grip → switch → body) | Read at `JilatonClickGui.java:876-906`; earlier GUI scenes captured 19:24 | BUILD_VERIFIED + prior RUNTIME |
| 2 | Navigation state persists: category, selected module, open settings, scroll, ordering | `ClientConfig` gui* fields; per-category scroll; new `moduleOrder`/`visualOrder` writers | Read; C gate 835 tests incl. `CardReorderPersistenceTest` 3/0 | BUILD_VERIFIED; reorder not yet re-shot |
| 2b | "last Appearance Studio section" | There is no sectioned Appearance Studio in the design — `VisualStudioScreen` is one static page | Verified by reading the screen | Satisfied by construction (no state to persist) |
| 3 | Fullbright actually changes lighting | `LightTextureFullbrightMixin` modifies the lightmap gamma uniform (ordinal 6); never writes `options.gamma` | Read mixin + module; prior shots `verify-26-fullbright-off.png`, `verify-26b-fullbright-on.png` at 19:24 | RUNTIME_VERIFIED (prior capture) |
| 4 | SwingAnimation rebuilt from Anubis | 6 styles (Slash/Chop/Hook/Spin/Double Tap/Push) + legacy aliases; latch; tempo/offhand/combat-only; wired through two mixins | B report + source read; B gate 829 tests; scene 27 written | BUILD_VERIFIED; runtime pending |
| 5 | FreeLook + KillAura rotation coordination smooth | Combat lane work (G) | Accepted in earlier lane review | BUILD_VERIFIED; runtime pending |
| 6 | Real saveable ViewModel presets | `ViewModelPreset` wire codec, shipped presets, save/selected wired | Source read; tests green | BUILD_VERIFIED |
| 7 | "1/6 ON" meaning identified and clear | Ratio now `N/M SW ON` (switches on), legend `SW = switches on` on the Visuals page | Read `CardMetric` + `JilatonClickGui.java:406-410`; test gate 835 | BUILD_VERIFIED; runtime shot pending |
| 8 | Target HUD scaling proportional, presets | HUD lane (E) scaling work | Accepted earlier | BUILD_VERIFIED |
| 9 | Cleaner GUI backgrounds (black/charcoal/white/muted, minimal borders, translucent, **blur/opacity settings**) | Backgrounds + scrim exist; **blur and opacity controls missing** | Read `renderTheme` (no such controls) and `renderBackground` (no blur call); 1.21.11 API verified (`Options.menuBackgroundBlurriness()`, `Screen.renderBlurredBackground`) | **OPEN — dispatched to C** |
| 10 | Visual Studio controls connected to real modules/previews | Real preview renderers and specs (not placeholder rectangles) | Source read; prior screenshots | BUILD_VERIFIED + prior RUNTIME |
| 11 | TargetESP in KillAura, 6 variants + Custom | Combat lane (G) | Accepted earlier | BUILD_VERIFIED |
| 12 | Real Pharaoh Headdress module | `PharaohHeaddressModule.shows(Player)` single predicate; emit loop; helmet suppression mixin | 46 targeted tests green; `javap` confirms class content | **OPEN — runtime blocked by mixin rule** (fix in progress) |
| 13 | Screenshot chat Copy (clipboard image) + Open (OS viewer), macOS, no dead buttons | `ScreenshotChatActions` decorates the vanilla message; `ScreenshotClickMixin` intercepts the custom click; `ScreenshotClipboard` has Windows PowerShell / macOS osascript / Linux wl-copy or xclip backends; `[Open]` uses vanilla `ClickEvent.OpenFile` | Source read; unit tests (`ScreenshotClipboardTest`); no runtime capture | BUILD_VERIFIED; runtime pending |
| 14 | Ambience unified (Anubis + LiquidBounce) with explicit precedence vs CustomSky/Cyberspace/Glass/Fullbright | `AmbiencePrecedence` order `CustomSky > Cyberspace > Glass > Fullbright > theme`, consumed by `AmbienceModule` | B report; tests 4; scene 29 written | BUILD_VERIFIED; runtime pending |
| 15 | Jump Circles: one clean effect per jump | `JumpCirclePolicy` (120 ms cooldown, 1.5 s landing grace) + module emits one front per jump | B report; tests 3 | BUILD_VERIFIED; runtime pending |
| 16 | 3D knife/katana cosmetics without held item, first/third person, inspect key | Cosmetics lane (F) | Accepted earlier | BUILD_VERIFIED |
| 17 | Shockwaves terrain-following glow, client-side, radius ~40–50, bounded | `TerrainWave` + `WORLD_WAVE` style; radius 16–64 default **44**, ≤160 samples/wave, 240/frame, behind-camera cull | B report + source read; tests 5; scenes 28/28b | BUILD_VERIFIED; runtime pending |
| 18 | Reach within protocol limits, honest block-breaking feedback | Combat lane (G) | Accepted earlier | BUILD_VERIFIED |
| 19 | Music artwork/track info in first person (Compact HUD, Minimal, World Space, Cinematic, Custom) | `MusicDisplay` modes + placements + camera anchoring | Source read | BUILD_VERIFIED |
| 20 | Spatial lyrics with 6 lifecycle states, world-anchored, new anchor per line, bounded; Sky Lyrics | Lyrics lane (A) | Accepted earlier; Sky Lyrics translucency needs B's alpha pipeline | BUILD_VERIFIED; alpha pipeline OPEN |
| 21 | Coherent shared architecture, no lane's work overwritten | Lane ownership + file lists in `ORCHESTRATION.md`; O review of every diff | Read | Ongoing |

---

## 4. Lane status

Lanes are separate Qoder sessions (8-session ceiling). One task per idle session; never queued onto a busy one.

| Lane | Working name | Session (short) | State at report time | Last evidence |
|---|---|---|---|---|
| A | Lyrics / Music | `93b8be9c` | idle | Spatial lyrics + music display accepted earlier; waits for B's alpha sky pipeline |
| B | RenderFX | `4d91b750` | monitoring harness run 2 | M41 code: 829 tests, jar SHA `89e30099…`; harness runs blocked by mixin |
| C | ClickGUI | `5f8290c6` | working (follow-up 3) | Follow-up 2: 835 tests, dev jar SHA `a6f1170f…`; blur/opacity now assigned |
| E | HUD | `93b52a27` | idle | Removed dead `HudRenderer.BACKGROUND`; 835 tests green |
| F | Cosmetics | `4c5e92aa` | idle | Knife/katana + Pharaoh assets accepted earlier |
| G | Combat | `e035b901` | idle | Combat audit completed earlier |
| H | Movement | `6bc3a22e` | idle | InventoryMove policy fix accepted (4/4 tests) |
| I | Models / Pharaoh | `9c45b592` | working (follow-up 3) | `@Unique` fix: 46 tests; now extracting helpers to a policy class |
| O | Orchestrator | `56b55ea7` | this report | Records in `development/ORCHESTRATION.md` |

---

## 5. Runtime harness: what happened, and the real root cause

**Attempt 1** — 20:41 local. Client booted, reached resource reload, died:
```
Mixin apply for mod jilaton failed jilaton.mixins.json:PharaohHelmetMixin -> HumanoidArmorLayer
InvalidMixinException: Mixin ... contains non-private static method playerOf(HumanoidRenderState)Player
Caused by: BootstrapMethodError: Mixin transformation of HumanoidArmorLayer failed
```
Exited 0 in 2m28s, **zero new screenshots** (newest stayed 19:24). `BUILD SUCCESSFUL` was not evidence.

**Attempt 2** — 21:03 local, after I added `@Unique` to both helpers. Same error. `javap` on the freshly compiled class in `.isolated-build-b` confirmed `RuntimeInvisibleAnnotations: Unique` was present on both methods — so the run was **not stale**; the annotation does not fix statics.

**Proof of the rule** (read from Mixin 0.17.5 / 0.8.7 bytecode):
`MixinApplicatorStandard.checkMethodVisibility` throws `"Mixin %s contains non-private static method %s"` when a method has `ACC_STATIC`, lacks `ACC_PRIVATE`, lacks `ACC_SYNTHETIC`, and lacks `@Overwrite`. **`@Unique` is not part of that exemption.**

**Scope** — O scanned every file under `src/main/java/dev/jilaton/client/mixin/**` with the correct predicate. Exactly two methods violate it, both in `PharaohHelmetMixin`: `playerOf` and `hidesHelmet`. No other mixin is affected.

**Fix direction (I, in progress)** — move both predicates into a plain `render.character.PharaohHelmetPolicy` class; the mixin keeps only its private `@Inject` handler. Tests move to the policy; the regression test asserts the real rule (no static non-private method in the mixin).

---

## 6. Verification evidence recorded today

| Item | Evidence |
|---|---|
| Consolidated gate (GUI lane) | `.isolated-build-c/root/test-results/test/` — **180 suites / 835 tests / 0 fail / 0 err / 0 skip**, newest XML 14:48:04Z |
| Consolidated gate (HUD lane) | `.isolated-build-e/root/test-results/test/` — **180 / 835 / 0 / 0 / 0**, newest XML 14:52:15Z |
| C ship artifact | `.isolated-build-c/root/devlibs/jilaton-client-0.1.0-dev.jar` SHA-256 `a6f1170f2e1375d5b95d1feabd6acc7df1b56bf4fb35d2402d3012e357c76292`; release jar on disk `477c085a…` |
| C reorder tests | `CardReorderPersistenceTest` 3 tests: drop persists full order, Visuals writes `visualOrder` without shrinking, grip never overlaps the switch |
| I mixin tests | `PharaohHelmetMixinTest 8`, `PharaohHeaddressTransformTest 5`, `PharaohHeaddressVisibilityTest 11`, `Character* 22` = 46 / 0 / 0 / 0 |
| B M41 code gate | 179 suites / 829 tests, jar 9,750,450 B, SHA-256 `89e30099…` at 20:37:06 |
| H movement fix | `InventoryMovePolicyTest` 4 / 0 / 0 / 0 at 14:35:53Z |
| Harness logs | `C:/tmp/jilaton-m41-harness.log` (attempt 1), `C:/tmp/jilaton-m41-harness2.log` (attempt 2) |
| Last real screenshots | `run-verify/screenshots/verify-*.png`, newest `verify-26b-fullbright-on.png` at 19:24:24 |

---

## 7. Blockers and immediate next actions

| Priority | Blocker / action | Owner | Definition of done |
|---|---|---|---|
| **P0** | Extract `playerOf`/`hidesHelmet` to `PharaohHelmetPolicy`; mixin keeps only a private injector | I | Targeted gate green; source scan shows no static non-private mixin method; mixin class `javap` shows only private/synthetic methods |
| **P0** | Relaunch `runVisualVerify`; capture scenes 27/27b, 28/28b, 29/29b + existing GUI/fullbright scenes | B (after I) | Log free of `InvalidMixinException` / `BootstrapMethodError`; fresh PNGs with timestamps; each scene's content inspected |
| **P1** | Backdrop opacity slider + real blur control on the Theme page, persisted, no `options.txt` rewrite | C | Tests for clamp/round-trip/restore; gate green; board coordinates reported for a harness scene |
| **P1** | Add harness scenes for: Pharaoh headdress with worn helmet, screenshot `[Copy]`/`[Open]` chat line, GUI grip reorder, Theme backdrop | O | Scenes compile and run in the same harness pass |
| **P2** | Alpha sky pipeline (new translucent pipeline + `skyAlpha(Identifier)` + second draw in sky pass), then A switches Sky Lyrics to it | B → A | B lands pipeline with tests; A changes one emission site; harness scene shows translucent sky lyrics |

---

## 8. Corrections to earlier records

1. **"Package-private static helpers need `@Unique`" was wrong.** `@Unique` is for non-private *instance* members. Statics must be private (or synthetic/`@Overwrite`). The first fix was therefore necessary-but-insufficient; the runtime repeat proved it.
2. **The run-2 failure was not a stale build.** The compiled class contained the annotation; the client correctly rejected it.
3. **C's follow-up 2 was correct about the dead field.** `HudRenderer.BACKGROUND` had zero references and was removed by its owner E, not by C.
4. **C's `guiScroll` change is not write-only overall** — the legacy `ClickGuiScreen` still reads and writes it; the shipping GUI now uses per-category scroll exclusively.
5. **"Appearance Studio section" persistence is satisfied by construction** — the screen has one static page, so there is no section state to save. No UI was invented to satisfy the line item.

---

## 9. Process lessons in force

- **A green unit suite is never evidence that a mixin applies.** Every new mixin needs a live client boot in a log before it is called done.
- **A gate covers one tree state in one build dir.** When a lane gate fails on a peer's test, check source mtimes against the XML timestamp before blaming either lane; re-run unchanged.
- **Every runtime claim needs a fresh PNG or a quoted log line.** `BUILD SUCCESSFUL` alone is not a runtime result.
- **One owner per file, one task per idle session.** Shared-tree edits are coordinated through the lane roster in `ORCHESTRATION.md`; no destructive git, no shared `build/`, no `clean`.

---

## 10. Where the detail lives

| File | Contents |
|---|---|
| `development/ORCHESTRATION.md` | Chronological decisions, dispatches, acceptances, corrections |
| `development/CHECKPOINT.md` | Batch history and prior live checks |
| `run-verify/screenshots/` | Harness captures (newest 19:24 until the P0 fix lands) |
| `.isolated-build-*/root/test-results/test/*.xml` | Per-lane JUnit XML with timestamps |
| `src/main/java/dev/jilaton/client/mixin/PharaohHelmetMixin.java` | The P0 offender |
| `src/main/java/dev/jilaton/client/gui/JilatonClickGui.java` | Reorder, legend, scroll, background, Theme page |
| `C:/tmp/jilaton-m41-harness2.log` | Attempt-2 runtime failure transcript |
