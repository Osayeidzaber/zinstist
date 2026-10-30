# Jilaton Client — project status and work completed

**Status date:** 2026-10-30  
**State:** active development; this is not a release-complete client.

This file summarizes work recorded in the current project and conversation. The detailed per-module evidence, source references, and known gaps are maintained in [`IMPLEMENTATION_INVENTORY.md`](IMPLEMENTATION_INVENTORY.md), [`LIQUIDBOUNCE_MODULE_INVENTORY.md`](LIQUIDBOUNCE_MODULE_INVENTORY.md), [`SECURITY_ASSESSMENT.md`](SECURITY_ASSESSMENT.md), and [`ATTRIBUTION.md`](ATTRIBUTION.md).

## Project foundation

- Continued the existing Anubis-based Fabric client instead of starting a separate client or rewriting the architecture.
- Kept Java as the implementation language. Current project target is Minecraft 1.21.11 and Java 21.
- Maintained the module registry, event/mixin hooks, settings, configuration persistence, ClickGUI/HUD systems, and existing resources while incrementally adding functionality.
- The source inventory scan recorded no Donut-specific module/command/branding references in the searched project source, README, or build files.

## Friend behavior

- Added a Friends module that is enabled by default.
- While in-world with no screen open, middle-clicking the currently targeted other player toggles their friend status: the first click adds them and another click removes them.
- The client reports whether the player was added or removed, and saves the friend list through the existing client configuration.
- Friend names are normalized case-insensitively and validated as Minecraft-style 1–16 character names. Friend state is used by player-target filtering and combat safety checks.
- Registered a world-render callback that draws a small blue hat above living friend players. The drawing and toggle logic exist in source; the hat has not been visually verified in a live Minecraft session.
- Unit coverage verifies friend toggle/add/remove behavior. It does not validate GLFW click handling or the in-game render pipeline.

## Implemented module areas

The inventory records registered, implemented systems in these groups. “Implemented” here means code exists; it does not imply every behavior was verified in a live game.

- **Combat/targeting:** AntiBot, Friends, Teams, AimAssist, BowAim, AutoShoot, ElytraTarget, AutoLeave, KillAura, Hitbox, Criticals, CrystalAura, AnchorAura, AutoAnchor, AutoRod, Triggerbot, Velocity, WTap, KeepSprint, AutoClicker, AutoTotem, AutoWeapon, NoMissCooldown, AutoPot, AutoBuffRefill, AutoReplenish, AutoGapple, AutoSoup, AutoHead, AutoEat, AutoArmor.
- **Movement/player:** Sprint, Parkour, InventoryMove, SafeWalk, Strafe, Speed, NoJumpDelay, Step, HighJump, LongJump, Flight, NoFall, NoSlow, FastFall, TargetStrafe, Reach.
- **Visual/client UI:** Fullbright, SwingAnimations, NoBob, HUD, and the Jilaton title screen. Existing ClickGUI and HUD editor/config systems are retained and extended through the project’s module and settings infrastructure.

The detailed inventory marks individual modules as partial where applicable, lists their settings/dependencies, and separates automated verification from live-game verification. Not every feature requested in the original broad client specification has been implemented.

## Notable engineering work recorded

- Added shared visible-rotation arbitration for selected aim/combat modules, with bounded turn speed and explicit priorities.
- Built common target filters for friend/team/anti-bot checks and wired these into multiple combat systems.
- Added policy-driven combat and movement behavior, including configurable combat pacing, target/raycast checks, friend damage protection, inventory/hotbar coordination, and movement safety checks.
- Added or extended configuration persistence and registry/config tests for many settings and modules.
- Added `AutoEat` and `AutoReplenish` with policy helpers and tests. The latest recorded build includes AutoReplenish.
- Inspected selected Anubis and LiquidBounce code using REA plus static analysis tools. Adaptations and exclusions are documented; third-party JARs were not executed on the host.
- Maintained attribution and license notes for Anubis/LiquidBounce-derived work. No claim is made here that a legal permission statement was independently verified.

## Security work and limitations

- Created a static security assessment with file identity, SHA-256 values, archive/manifest observations, inspected code paths, and explicit coverage limits.
- Anubis and the installed LiquidBounce JAR remain classified **Insufficient evidence** as whole artifacts. Static inspection is not a complete safety proof.
- The unidentified `g9pfrn.jar` is classified **Suspicious** based on obfuscated classes plus process, socket, dynamic-class-loading, and JNA indicators; investigation stopped and the file was not executed.
- Krypton, Prestige, and Rise JARs are listed as unavailable for analysis after the user requested their deletion. Their safety was not assessed.
- No supplied third-party client JAR was run or loaded into Minecraft during the documented analysis. The Jilaton source scans found no matches for a selected set of direct process, dynamic/native loading, network, browser-storage, credential, cookie, or token APIs; those bounded scans are not exhaustive.

## Latest recorded build

- Command: `gradlew build --rerun-tasks --no-daemon` with Java 21.
- Result recorded in the implementation inventory: **241 automated tests passed** with no failures, errors, or skips.
- JAR: `build/libs/jilaton-client-0.1.0.jar`.
- Recorded size: **398,851 bytes**.
- Recorded SHA-256: `5EEA69802D560285AD1DBE2B2B7644AA20D6A106206A2B75878807CB78F4FECF`.
- This artifact was not launched in Minecraft. ClickGUI/title screen appearance, friend-hat rendering, combat timing, inventory synchronization, movement, and other listed live interactions remain unverified in-game.

## Current work remaining

- Continue auditing reference implementations and their dependencies before adapting more modules; do not count discovered module names as finished features.
- Add and verify remaining PvP systems incrementally, including the requested KillAura target feedback and further combat/movement features.
- Exercise the GUI, friend middle-click toggle, blue friend hat, config persistence, and module lifecycles in a controlled Minecraft session when it is available.
- Rebuild after any code changes and update artifact/test data. A verified build is not by itself a verified in-game release.
- Complete the outstanding project, dependency, license, compatibility, and runtime reviews before describing Jilaton as release-ready.
