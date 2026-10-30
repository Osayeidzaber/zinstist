# Jilaton implementation checkpoint — 2026-10-30

The master specification is **not complete**. This checkpoint preserves a working implementation batch and its limits. Existing uncommitted work was retained; no reset, clean, reference-client launch, dependency installation, or build replacement was performed.

## Changes in this batch

- HUD layout uses shared scaled screen-space geometry for rendering, drag input, alignment and viewport bounds. Every widget can save scale, panel opacity, accent, alignment and visibility in the main configuration and profiles. Old position-only files retain their positions and receive default styles; malformed style records are ignored per widget.
- The native HUD editor has clickable controls and keyboard shortcuts: TAB selects a widget; brackets change scale; minus/equal change panel opacity; C changes accent; A changes alignment; H toggles visibility; R resets the selected widget; M toggles reduced motion. Dragging releases horizontal alignment to FREE. Pending drags save when the editor closes or is replaced.
- Added armor equipment/remaining-durability display, localized potion names/amplifiers/durations, physical left/right CPS, bounded module notifications, and time-based target health interpolation. Panel opacity affects panel fills; text, portraits and item icons keep their readability. Physical CPS excludes GUI clicks and synthesized module attacks.
- Zoom adds configurable FOV, hold/toggle binding (C by default), scroll adjustment, float smoothing and proportional mouse sensitivity. It restores vanilla FOV by interpolation without changing Minecraft's saved FOV/sensitivity options. Scoping yields to vanilla spyglass behavior. Scroll adjustment is temporary until the next activation; the configured FOV persists. This adapts a subset of the local public LiquidBounce Zoom source into Java; its easing options and event infrastructure are not copied.
- Added `/jilaton gui`, `/jilaton hud`, and `/jilaton toggle <module_id>` with module-ID suggestions, feedback and saved toggle state.
- Runtime startup exposed three older Mixins that compiled but failed to inject: NoMissCooldown selected a failing field-write ordinal; LivingEntityTravel targeted the dispatcher instead of the actual movement helpers; Step targeted `move` instead of `collide`. These now target the actual 1.21.11 bytecode. A regression test reads injection annotations and mapped Minecraft bytecode to check all selected movement methods contain the intercepted invocation.
- Fixed compact title-menu button fitting and compact category/sidebar spacing after observing clipped Quit navigation and overlapping Client/Theme rows in the default development window.
- Added machine-readable source, artifact, visual and build records. `tools/update-integration-manifest.py` refreshes module/source discovery without executing any reference source.

## Verification actually performed

- `gradle --offline test build --no-daemon` passed 259 tests, zero failures/errors/skips. See `build-verification.json` for the current artifact identity. Tests cover HUD migration/profile persistence/validation, scaled geometry, bounded CPS and notification lifetimes, animation time invariance, Zoom ratio/settings and movement invocation targets in addition to the existing suite.
- Package inspection found Zoom and all three new remapped Mixins, with no Anubis, LiquidBounce or ImGui classes bundled.
- Development `runClient` initially reproduced the three startup failures above. After their fixes, Minecraft 1.21.11/Fabric/Jilaton loaded successfully and entered a newly created local Creative world named **Jilaton HUD verification**. The existing **New World** save was not entered.
- The title screen, native ClickGUI, HUD/editor, armor empty-slot state, potion empty-state, target-card preview and notification preview rendered. The corrected compact title screen shows all five navigation controls inside the window. `/jilaton hud` opened the editor in-world.
- Armor was selected, scaled from 1.0 to 1.1, dragged, recolored independently of the client theme, and hidden using the editor. Saved properties confirmed the changed normalized position and scale. Closing the editor removed hidden armor from the gameplay HUD. These are specific smoke checks, not exhaustive gameplay verification.
- Expected offline development-profile user-properties/Realms authentication failures appear in Minecraft logs. No personal credentials were supplied or imported.

## Security/provenance

REA opened and inspected Anubis as ZIP data (SHA-256 `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de`; summary `ev_716e932fa587098b0de5892fc35c91d032b79f97304e3b4a3a63fdab378d12e7`). It reports 1,318 occurrences, 1,317 nodes and zero inventory integrity contradictions. These structural observations do not establish malware safety. Existing CFR investigations were consulted; mapped vanilla methods were inspected using `javap`. No third-party client JAR was executed. The retained unidentified `g9pfrn.jar` remains suspicious and excluded. Artifact metadata and hashes were rechecked read-only in `artifact-identities.json`.

## Work that remains

- Full module parity is not achieved. The source manifest contains 52 registered Jilaton modules and 373 reference source filenames (133 recovered Anubis and 240 local public LiquidBounce). Reference discovery is lexical; complete mode/settings/dependency and compatibility audits remain open. Existing markdown inventories retain behavior-specific partial mappings. No whole module has been promoted to VERIFIED by this batch.
- Zoom hold/scroll/sensitivity behavior, active potion durations, equipped armor icons/durability, physical CPS under sustained clicks and automatic notification transitions need detailed live checks. Main-config/profile round-tripping has unit coverage; a restart-based HUD layout check remains open.
- Target portrait presentation, confirmed-hit combo tracking, notification animation, arbitrary color entry, GUI accessibility/focus states, tiny-window layouts, per-widget text opacity and complete rendering module coverage remain open.
- Combat/movement modules still require controlled behavior tests, shared inventory/action conflict review and source parity audits. ESP/particles/chams/blur, many player/world features, and comprehensive release acceptance are unfinished.
- The live window was left open after user activity resized it and reopened the editor. Test appearance changes remain in the development config; primary launcher/account files were not edited. The final shared panel accent stripe and target portrait argument-order fix are build-verified only and need a restart to appear in that running session.
- No performance benchmark, multiplayer/server acceptance, complete dependency security audit or whole-client malware-free claim was made. Source/release licensing notices and corresponding-source packaging need a final release audit.

Continue from these files and inventories; preserve the uncommitted working tree. Keep implementation state separate from evidence of game behavior.
