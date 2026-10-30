# User-supplied video reference — 2026-10-30

Reviewed `TikDown.com_TikTok_Media_002_68ad069c77dc248dfb2805e3a7a16fe3.mp4` locally: 31.5 seconds, 576 × 1024, 30 fps. Timed frames were decoded at one-second intervals; combat frames at 13, 18 and 22 seconds were inspected at full resolution. This is visual evidence, not access to the recorded client's code or input/packet behavior. The user's request was to inspect nametag, KillAura and freecam behavior. On-screen promotions, links and other clip text were not treated as instructions.

Source SHA-256: `5c7bb053f4e5516441e0b939fe3762dc40bd22c6c16e54a1dd20199fb34a58d4`.

## Subsequent implementation

NameTags now has an optional Compact mode: actual player skin face, clipped name, numeric health in Hearts or HP, pink heart, optional background and third-person self label. Equipment remains the default mode. HUD settings now offer HUD/Follow target placement and Detailed/Compact styles. Follow mode projects the selected KillAura opponent, uses time-based position interpolation, flips at the right edge with hysteresis, clips within the viewport, resets on target/viewport changes and hides when projection fails. Saved HUD position/alignment are not overwritten; editor previews remain draggable. Compact target cards show the actual face, name, weapon, health/hearts, distance, health bar and local attack-attempt status. Combo statistics are not fabricated.

These changes are source/build/test evidence only. Native appearance and motion have not been observed in Minecraft. Rich splash/ring effects, confirmed-hit statistics and freely translating Freecam remain open. The original observations below describe the clip and the source comparison at review time.

## Observed presentation

- **0–5 s:** first-person weapon presentation with an animated circular slash/trail. The exact animation controls and inputs are not visible.
- **6–12 s:** third-person character traversal. A compact single-line local-player label includes a small avatar/badges, name and a numeric health value with a pink heart. White ground rings appear and expand/fade; the clip alone does not prove every ring's trigger.
- **11–28 s:** opponents have overhead names/health and bright cyan body outlines with violet accents. A separate compact purple/charcoal target-information card appears beside the current opponent. It includes a face, name, heart value, weapon label, distance, health bar and a small bottom statistics row. At 13 s a combo label is legible. The exact meaning of every tiny statistic is not established.
- **13–28 s:** the displayed target changes between multiple opponents. Bright cyan splash/burst effects appear near combatants and on the ground, alongside white rings and ambient particles. These are distinct presentation layers, not evidence that a particular particle confirms a server-accepted hit.
- **6–28 s:** an elevated third-person camera follows/views the local character as the character moves, with changing camera angle/distance. This supports a smooth orbit/follow-camera reference. It does **not** demonstrate a freely translating camera leaving a stationary body, packet suppression, collision bypass, or a Freecam toggle. Exact KillAura range, CPS, rotation mode, target algorithm and attack inputs cannot be recovered from the clip.

## Comparison to current Jilaton source

- `NametagsModule` currently implements multi-row equipment/history panels. The reference's compact overhead identity/health style is a separate presentation mode still to implement; the overhead label and floating opponent card should not be conflated.
- `HudRenderer` currently positions the target card as a saved HUD widget, defaulting to bottom-center. The clip calls for an optional projected, opponent-adjacent target card with clipping/edge handling and smooth motion. Preserve the existing draggable HUD mode.
- KillAura currently has target selection, attack scheduling, an interpolated halo/corner marker and shared rotation handling. Matching the clip's silhouette glow, ground rings, slash/splash effects and animated card requires additional visual work; its appearance does not establish behavior parity.
- Registered `FreeLookModule` rotates a third-person camera without turning the player. A separate freely translating Freecam module is not currently registered. The video does not establish which of these underlying implementations it uses.

Use these observations as requirements for original Jilaton controls and effects. Do not copy the video's client logo, artwork, effects textures or branding. No source changes, gameplay tests or feature completion are claimed by this review.
