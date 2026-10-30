# LiquidBounce compiled module inventory

This inventory is derived from REA’s complete central-directory view of the local LiquidBounce JAR. It lists top-level `Module*.class` files under `net/ccbluex/liquidbounce/features/module/modules/`, excluding `$` nested/anonymous classes and compiler-generated `*Kt.class` file facades. The archive contains 4,642 occurrences (4,641 nodes), with no ZIP integrity contradictions; that is structural coverage only, not a behavioral or security verdict. The JAR remains unexecuted and its overall static security status remains **Insufficient evidence**. SHA-256: `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`.

Jilaton mappings below indicate only that a related implementation exists. They do not claim feature parity or runtime verification. `Not integrated` means no matching Jilaton feature was identified in the current code inventory; detailed dependency/behavior review remains open.

Found **232** module entry class files across 8 categories.

## combat (26)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `combat/ModuleAimbot` | `AimAssist (partial)` | REA confirmed the 25,222-byte compiled `ModuleAimbot.class` occurrence `occ_6c12a27046556dc253d68742f3133cc58af7e3f5e44dcfdd39a1c249735cec77` (archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`, evidence `ev_217a455767180358a7294234a34cadb751034772758aa017e7b7bc1b470e46db`). Static `javap` confirms direction-priority TargetTracker, PointTracker, Requires/Axis/Ignore groups, FactorAngleSmooth, and independent horizontal/vertical angle interpolation scaled by timer speed. Jilaton now adapts the factor interpolation and axis toggles through `AimAssistPolicy` and shared `RotationManager`, retaining its Anubis-derived sticky target and range/FOV hysteresis. Policy, registry, config round-trip and build tests pass; runtime is unverified. TargetTracker requirements, PointTracker, adaptive angle-smoothing group, world TargetRenderer and mouse-rotation hook remain unported. |
| `combat/ModuleAutoClicker` | `AutoClicker (partial)` | REA verified `ModuleAutoClicker.class` (10,658 bytes), `ModuleAutoClicker$AttackButton.class` (17,112 bytes), `ModuleAutoClicker$Use.class` (3,194 bytes), and `utils/item/WeaponType.class` (5,834 bytes) in the installed archive (JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`, REA entry evidence `ev_dbfd6eb0b317ef35e0265dc86d070b2e9be4bdac240c524580cb355302cdc341`). Static `javap` shows attack objectives Enemy/Entity/Block/Any, a weapon multi-choice, Smart critical selection, a default-on delay of 300 ms after `STOP_DESTROY_BLOCK`, and item-use modes Wait/Stop/Ignore with configurable 0–20 tick post-stop delay. Jilaton adapts a persisted single-category weapon selector, the fixed 300 ms break pause, and optional 0–20 tick post-use pause; item-use Wait/Ignore correspond to Jilaton's pause toggle, while Stop remains unported. Policy, registry, registry-default and config round-trip tests pass; live clicks remain unverified. Enemy/Entity/Block/Any objective selection is now adapted; Smart critical selection, multi-select categories and special-block filters remain unported. Named click profiles are now available through the shared Jilaton scheduler. |
| `combat/ModuleAutoLeave` | `AutoLeave (partial)` | REA complete archive inventory and static CFR/`javap` plus local source review show Health default 8 HP, random Delay range default 0..0 and 0..60 maximum, and a `ModuleKick` mode defaulting to Quit. `tickConditional` confirms leave occurs only after the low-health condition persists for the sampled delay; creative/singleplayer and main/offhand death-protection abort the action. Jilaton implements a health threshold, sampled consecutive-low-health delay, multiplayer survival/death-protection gates, normal `disconnectWithProgressScreen()`, and self-disable after disconnect. Three settings; pure eligibility/delay, registry and full-build tests pass, but the live disconnect path is unverified. `ModuleKick$KickModeEnum` bytecode also contains INVALID_PACKET (NaN/infinite coordinates), illegal chat, invalid held-item slot, self-hurt and illegal-interact modes; none are exposed or copied into Jilaton. REA evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`; installed JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; local source snapshot commit `e033f5aaf05be802dd2ca51782f255119d7d5654`. |
| `combat/ModuleAutoShoot` | `AutoShoot (partial)` | REA revalidated the installed JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898` and complete archive inventory (`ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`); static `javap -p -c` and CFR 0.152 review of `ModuleAutoShoot` and `Clicker` confirmed egg/snowball versus any-held-item modes, auto/direct/projectile gravity selection, visible target/rotation tracking, default 5–8 CPS click cadence, automatic hotbar selection, optional KillAura/combat gates, and throw action. Jilaton adapts player-only filtered targeting, sticky/range/FOV selection, eggs/snowballs plus opt-in any-held-item use, auto/direct/ballistic aim, movement lead, high arc, visible slot ownership/restore, scheduled use and KillAura requirement with its existing Java helpers. It also yields during manual use, AutoRod/AnchorAura, and active food/potion hotbar actions; the KillAura-required path shares its current target and requests the corresponding rotation priority. Jilaton's item-use path and ballistic math have unit/build coverage; no Minecraft runtime test was performed. SilentHotbar, full TargetTracker/PointTracker/TargetRenderer, inventory-aware rotations, `NotDuringCombat`, `ConstantLag`, and broader target types remain unported. |
| `combat/ModuleAutoRod` | `AutoRod (partial)` | REA occurrence `occ_26d40bc8385cac8b00d29d957299e5b81f1f14efb00831a031d973728f80d162`, 31,632 bytes; CFR decompilation confirms range/scan-range selection, health and enemy-count gates, kill-aura requirements, ignore gates, fishing-bobber hit/timeout/range-exit wait, cooldown, slot reset delay and rod swing settings. Jilaton adapts the cast/reel state machine, filtered target selection, health/range/FOV gates, crowd check, aim request through the shared visible `RotationManager`, hit timeout, out-of-range pull and slot-restore ownership. The reference's projectile-derived aim solver, silent slot manager, TargetTracker/renderer and requirement groups remain unported. REA inventory evidence `ev_c6a7b1eceddeb66d5cdb1db4221ae6cfdc72ff95a25f66df2072295a308df74d`; source JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`. |
| `combat/ModuleAutoWeapon` | `AutoWeapon (partial)` | REA inventory and static `javap` show preferred hotbar weapon selection on target/attack, silent switch-back, shield-break axe selection, optional mace-smash integration, and action settings. Jilaton implements attack-time selection by active attack-damage attributes/Sharpness, axe-on-block, vanilla mace-smash eligibility, slot-owner-aware switch-back, and AutoPot/AutoGapple slot-reservation checks. LiquidBounce multi-choice preferences, silent-hotbar arbitration, on-target switching, and auto-Mace module coordination remain unported |
| `combat/ModuleFakeLag` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleHitbox` | `Hitbox (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleKeepSprint` | `KeepSprint (partial)` | REA verified class `occ_f553deb9621cbd2dfa2313f399cb6006b4f25f99aa6dce0131a10a5f02c3b0f9` and `MixinPlayer` `occ_100d6e1048cc4b64dfe53c1568959dab8aa204494580e93c0870066fe4e61a3f`; CFR shows Motion and MotionWhenHurt ranges, HurtTime range, Chance, randomized horizontal `Vec3.multiply`, sprint reset suppression, and a transient sprint-state view during travel. Jilaton ports configurable ranges/chance, scales only X/Z in `Player.causeExtraKnockback`, and suppresses that method's sprint reset. Jilaton does not reproduce LiquidBounce's transient `isSprinting` view; its WTap attack-tail path remains separately active. Policy/registry/config tests and remapped mixin-target inspection pass; in-game behavior is unverified |
| `combat/ModuleMaceKill` | — | REA/CFR-audited. On attack with a Mace, searches upward collision-free distance, calls `EntityExtensionsKt.warp`, then sends a lower-Y movement packet followed by the original position with on-ground false. The helper sends vehicle or player movement packets. This is a server-side fall-distance spoof; not ported. |
| `combat/ModuleNoMissCooldown` | `NoMissCooldown (partial)` | REA archive inventory and static `javap` show two settings: replace the `Minecraft.startAttack` `missTime` read with zero and optionally consume the action when the hit result is `MISS`. Jilaton implements both as independent settings, including suppression of the miss delay write; it affects only the local miss-time gate, not server attack-strength or validation |
| `combat/ModuleSuperKnockback` | `WTap (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleSwordBlock` | Deferred visual behavior | REA confirms the installed class (`occ_9b6c7f92be69ccf27098898c66333063afd6c74816ea5cd3096174e8911e2130`, 11,763 bytes; archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`). Static bytecode identifies `OldBlocking` and the `OnlyVisual`, `FakeOnPressing`, `HideShieldSlot`, `ApplyToThirdPersonView`, and `AlwaysHideShield` settings. Its packet handler exits for protocol 1.21.5 and newer, which includes Jilaton's 1.21.11 target; the remaining behavior is first-/third-person animation and shield-slot rendering. Deferred to the visual phase per the current feature priority; no gameplay blocking behavior is claimed. |
| `combat/ModuleTickBase` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleTimerRange` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/aimbot/ModuleAutoBow` | `BowAim` AutoShoot/Aimbot subset | REA/CFR-audited composite of AutoShoot, Aimbot, and FastCharge features. AutoShoot handles bow draw timing, charged crossbows and tridents (more than 10 use ticks), can find precharged crossbows in either hand, and gates firing on delay and aim threshold. Jilaton adapts bow/trident release and crossbow-charge aiming; optional firing accepts only precharged arrow projectiles, and skips Riptide tridents. Firework crossbows, packet FastCharge and hypothetical-hit simulation are not ported. |
| `combat/aimbot/ModuleDroneControl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/aimbot/ModuleProjectileAimbot` | `BowAim (partial)` | REA verified the compiled module and its `SituationalProjectileAngleCalculator`, `TrajectoryInfo`, and `PolynomialProjectileAngleCalculator` dependencies by path and archive hash. `javap` shows target selection, a situational angle calculation, then a rotation-manager submission. Jilaton adapts this with vanilla bow draw power, a drag/gravity intercept solver, optional target velocity lead/high arc, gradual visible rotation, and shared friend/team/anti-bot filters. This does not port LiquidBounce's generalized projectile types, target selector modes, rotation arbitration/correction group, or full hitbox verification. Math, registry, and config tests pass; in-game aiming remains unverified |
| `combat/autoarmor/ModuleAutoArmor` | `AutoArmor (partial)` | REA/CFR inspection confirms per-slot best-piece selection through `ArmorEvaluation`/`ArmorComparator`, then scheduled vanilla inventory actions with player-inventory constraints and optional Hotbar/SaveArmor groups. Jilaton has an independent full-kit damage scorer and implements a SaveArmor-style durability-first candidate policy (default threshold 24 remaining damage points), Binding-Curse filtering, and vanilla inventory-click equip; policy/config tests pass. Reference inventory management, action scheduler/constraints, hotbar equip and auto-open UI remain unported; live inventory behavior is unverified. |
| `combat/backtrack/ModuleBacktrack` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/criticals/ModuleCriticals` | `Criticals (partial)` | REA confirmed the installed JAR's `ModuleCriticals` class (12,535 bytes) and its Packet, NoGround, Jump, Blink, and Timer mode classes (archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; class-list evidence `ev_27032c81a23667f5dd1f1f498e232c6fea7f44dea982522e210ee06281d10e04`). Static `javap` confirms CriticalsSelectionMode Smart/Ignore/Always plus `canDoCriticalHit` environmental/cooldown checks. Jilaton implements a bounded vanilla jump queue and added a vanilla Wait mode that waits for a natural critical window; callback, target/world lifecycle and config tests pass. LiquidBounce's packet, no-ground, blink, timer and selection-mode control system remain unported; live behavior is unverified. Module class evidence `ev_b337ef60e2a655bf797a2b6ba160676d6fff60d55347eb53051dcb9774c843fc`. |
| `combat/crystalaura/ModuleCrystalAura` | `CrystalAura (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/elytratarget/ModuleElytraTarget` | `ElytraTarget (partial)` | REA inventory, static CFR/`javap`, and local source review confirm fall-flight gating, `TargetTracker` selecting visible targets, optional Safe upward collision nudge, an AlwaysGlide hook, an Elytra rotation processor with prediction/aim-point settings, and optional AutoFirework with normal/packet modes, cooldowns, slot delay, and optional KillAura click synchronization. Jilaton adapts player-only range/FOV/LOS tracking, sticky/nearest-angle selection, shared friend/team/anti-bot filtering, eye/center aim, configurable movement and gravity lead, visible smooth rotation, conservative predicted-collision nudge, and normal vanilla firework use with ownership-aware slot restore and near/far cooldown settings. Registered with 14 settings; policy, registry, and full-build tests pass. In-game behavior is unverified. TargetRenderer, AlwaysGlide mixin, packet firework mode, click synchronization, separate yaw/pitch rotation processing, and strict/changed-look movement correction remain unported. Source checkout snapshot: LiquidBounce GPL-3.0-or-later commit `e033f5aaf05be802dd2ca51782f255119d7d5654`; installed JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`. No reference bytecode is bundled. |
| `combat/killaura/ModuleKillAura` | `KillAura (partial)` | REA confirmed the installed JAR's `ModuleKillAura`, `KillAuraClicker`, and shared `utils/clicking/Clicker` entries (verified entry hashes; archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`). Static `javap` shows click scheduling delegates to the shared clicker, whose CPS interval and pattern feed a rolling per-tick click schedule with vanilla item/attack-cooldown gates. Jilaton now adapts the shared 20-tick `Clicker` schedule and Stabilized, Efficient, Spamming, Double click, Drag, and Butterfly distributions alongside its existing Steady and Normal distribution modes. Per-tick click counts are consumed by KillAura; the default-on attack-cooldown gate limits repeat attempts unless the user changes that setting. Item-cooldown groups, input-binding autoclick actions, critical Wait/Jump, autoblock, and fail-swing/notification features remain unported. REA confirmed ModuleKillAura (45,390 bytes), RaycastMode (3,115 bytes), and the crosshair predicate (2,343 bytes) in the installed JAR (targeted inventory evidence ev_581c015d380b316bae826a27b9820c837cb79aeec912069a7765d4e8d56bc380). Static javap shows None/Enemy/All controlling the crosshair entity predicate; Jilaton adapts them as attack-ray gates, where Enemy requires the first qualifying combat target and All the first pickable entity, with block occlusion checks. Four geometry tests pass; in-game behavior is unverified. REA archive evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5907cad4f3f94f898`; targeted occurrence evidence `ev_5b8525ef4db842dd6a215809b8bd334e139394cb7426443bff493d24b4151b3d`. |
| `combat/tpaura/ModuleTpAura` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/velocity/ModuleVelocity` | `Velocity (partial)` | Generic Modify, Strafe, Reversal and JumpReset subsets adapted. REA-confirmed `VelocityStrafe$OnlyFacing` is now represented by an opt-in ray gate with configurable 0.1–6.0 range (default 3.5); Jilaton uses player-visible camera rotation and shared friend/team/anti-bot filters. Randomized cooldown ranges, reference entity-moving gate, explosion packet handling, coroutine/move-event sequencing, NoFallBlink interaction, and server-specific modes remain unported |

## exploit (25)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `exploit/ModuleAbortBreaking` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleAntiHunger` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleAntiReducedDebugInfo` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleClickTp` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleClip` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleDamage` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleExtendedFirework` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleGhostHand` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleKick` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleMoreCarry` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleMultiActions` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleNameCollector` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleNoPitchLimit` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModulePingSpoof` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModulePlugins` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModulePortalMenu` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleResetVL` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleSleepWalker` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleTimeShift` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleVehicleOneHit` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/ModuleYggdrasilSignatureFix` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/disabler/ModuleDisabler` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/dupe/ModuleDupe` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/phase/ModulePhase` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `exploit/servercrasher/ModuleServerCrasher` | — | Not integrated; dependencies, settings, and behavior not yet audited |

## fun (7)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `fun/ModuleDankBobbing` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/ModuleDerp` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/ModuleHandDerp` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/ModuleSkinDerp` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/ModuleTwerk` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/ModuleVomit` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `fun/notebot/ModuleNotebot` | — | Not integrated; dependencies, settings, and behavior not yet audited |

## misc (28)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `misc/ModuleAntiCheatDetect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleAntiStaff` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleAutoAccount` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleAutoChatGame` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleAutoConfig` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleAutoPearl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleBetterTab` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleBetterTitle` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleBookBot` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleEasyPearl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleElytraSwap` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleFlagCheck` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleGUICloser` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleInventoryTracker` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleItemScroller` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleMacros` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleMiddleClickAction` | `Friends (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `misc/ModuleNotifier` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModulePacketLogger` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleSpammer` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleTargetLock` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/ModuleTeams` | `Teams (partial)` | REA verified `ModuleTeams.class` (12,327 bytes; entry evidence `ev_28147a99cbbb2abbf9580607909cc2062af51d5e7ff10be6c6e0e36cd9b71359`), nested `Matches` (7,491 bytes) / `ColorSource` (6,134 bytes; evidence `ev_ae9952ee290bc4bd524c713c1184258440cb2c73ecb12f43ffb9d9f6303b0708`), and `EquipmentSlotChoice` (6,546 bytes; evidence `ev_8a5c060464b1e34bb4bc82042f9a19ea0c7f7c39eca22d32725fd15e18a8cd86`) in the installed archive. Static `javap -p -c` shows default scoreboard-team and name-color matching, helmet armor color enabled by default, and Team/Armor tag color sources; armor matching reads the selected slot's `DYED_COLOR` and compares its RGB value. Jilaton adapts scoreboard alliance, configurable explicit display-name-color matching, and same-slot dyed-armor matching with selectable helmet/chest/legs/feet slots through the shared `PlayerTargetFilters`; policy, setting defaults, config round-trip and full-build tests pass. Entity-tag recoloring remains unported; live formatting/team behavior unverified. JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898` |
| `misc/ModuleTextFieldProtect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/antibot/ModuleAntiBot` | `AntiBot (Custom mode subset)` | REA inventory confirms the installed class and package; CFR 0.152 plus `javap -c -p` static review traced the default Custom mode's listed/profile presence, duplicate profile, illegal pitch (`abs(pitch) > 90`), and fake entity ID (`id < 0 || id > 1,000,000,000`) predicates. Jilaton exposes listed-player, duplicate-name profile, invalid-pitch, and suspicious-ID checks as settings, caching player-list facts once per game tick; policy, registry, config and clean-build tests pass. LiquidBounce's packet-driven invalid-ground counters, age/armor/name tests, custom conditions `NeedHit`/`IllegalHealth`/`Swung`/`Critted`/`Attributes`/`IllegalScale`, plus Matrix/IntaveHeavy/Horizon modes remain unported. Those require packet/event histories or server-specific assumptions and are not claimed. Live target classification is unverified. Archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; REA inventory evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95` |
| `misc/betterchat/ModuleBetterChat` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/debugrecorder/ModuleDebugRecorder` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/nameprotect/ModuleNameProtect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/reporthelper/ModuleReportHelper` | — | Not integrated; dependencies, settings, and behavior not yet audited |

## movement (37)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `movement/ModuleAirJump` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleAnchor` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleAntiBounce` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleAntiLevitation` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleAvoidHazards` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleBlockBounce` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleBlockWalk` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleElytraRecast` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleEntityControl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleFreeze` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleNoClip` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleNoJumpDelay` | `NoJumpDelay (adapted with configurable cooldown)` | REA/CFR inspection found the Kotlin module is metadata-only; the behavior is in `MixinLivingEntity`, which resets the private jump cooldown during movement ticks when enabled, except when AirJump owns jumping. Jilaton's `NoJumpDelayModule` exposes Anubis-style 0–9 cooldown and 0–4 variance settings, and its `NoJumpDelayMixin` intercepts only local-player `LivingEntity.aiStep` writes that increase the cooldown. Normal decrements and resets are passed through. Clean build and policy, registration, config-roundtrip tests pass; generated refmap maps the hook and field to installed 1.21.11 intermediary names. Live behavior is unverified. Source archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; REA archive inventory evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95` |
| `movement/ModuleNoPose` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleNoPush` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleParkour` | `Parkour (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `movement/ModuleSafeWalk` | `SafeWalk OnEdge (partial)` | REA inventory and `javap` inspection found two modes: direct PlayerSafeWalkEvent Safe mode and OnEdge's movement-input/edge-distance logic with keep/sneak/jump settings. Jilaton implements an edge-footprint support check that injects vanilla sneak via MovementInputEvent and coordinates with Parkour; the full SafeWalkEvent pipeline, simulated next-tick position, and jump-on-edge behavior are not ported. |
| `movement/ModuleSneak` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleSprint` | `Sprint (partial)` | REA occurrence `occ_db96abcbe6311f81467540384be4de473641a97be18f76b7dd76a41b74b21ff1`; CFR static review confirms Legit, Omnidirectional and Omnirotational modes, ignore flags for blindness/hunger/collision, and stop conditions for ground/air/sneaking/item use. Jilaton now implements forward-only/omni directional input and configurable hunger, collision, sneak and item-use gates with policy/registry tests. Omnirotational yaw correction and rotation arbitration are not ported; Jilaton has no shared rotation manager yet. |
| `movement/ModuleStrafe` | `Strafe` | Jilaton translates its input-directed horizontal velocity alignment, separate ground/air strengths, and optional strict stationary handling into the existing `MovementInputEvent`/`PlayerMoveEvent` system. LiquidBounce's shared `EntityExtensionsKt.withStrafe` helper is not copied; Jilaton adds movement-state guards and pure math/config tests. Live movement remains unverified. |
| `movement/ModuleTargetStrafe` | `TargetStrafe (partial)` | REA-verified class occurrence `occ_0648f70ad8cbdeeeb90a84107bef848fa7e0a482adb4988ae59bf126897ae5eb`; CFR review confirms Input and Motion modes, FollowRange/Range, target priority (KillAura, Aimbot, selector), control-direction handling, adaptive-range retry, collision validation, and edge/void checks. Jilaton implements both Input and Motion modes, range/direction controls, optional KillAura target, collision and ground-support checks; LiquidBounce's adaptive-range, void validation, target selector behavior, grouped requirements, and rendering state remain unported. |
| `movement/ModuleTeleport` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleVehicleBoost` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleVehicleControl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/autododge/ModuleAutoDodge` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/elytrafly/ModuleElytraFly` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/fly/ModuleFly` | `Flight (generic Creative mode)` | REA verified `ModuleFly`, generic `FlyCreative`, and its tick handler from the installed archive. Static `javap` shows the mode enables vanilla flight, applies configurable flight speed, optionally forces flying, caps velocity, and has a default-enabled `BypassVanillaCheck` that offsets movement Y from `yLast` by 0.04 and sends an additional position packet every 40 ticks when clear of nearby blocks. Jilaton adapts those generic controls with state restoration and a configurable heartbeat; its mapped packet hooks compile, but live flight behavior remains unverified. Server-specific movement modes are not ported |
| `movement/highjump/ModuleHighJump` | `HighJump (Vanilla mode)` | REA/CFR confirms Vanilla sets jump-event vertical motion to the configurable 0.2–10.0 value (default 0.8). Jilaton applies it after a successful local vanilla jump and guards nonlocal, vehicle, fluid, elytra, spectator, and flying states. Vulcan's 100-tick delayed glide behavior is omitted; no server-specific sequence is claimed. Policy, registry, config, and build tests pass; live jump behavior unverified. |
| `movement/inventorymove/ModuleInventoryMove` | `InventoryMoveModule` (screen-policy subset) | REA verified the installed class entry (18,431 bytes; occurrence `occ_8469886e7f3d284b1354d0ca8b4e63c027fb85101ddcd4acb2205c0811da1d03`) in archive inventory `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`; static CFR review of the class confirms Normal/Safe/Undetectable screen gating, text/search guards, movement-key tracking, and separate sprint/sneak/timer/blink feature groups. Jilaton adapts the three compatible screen policies through `MovementInputEvent`, reads current vanilla key mappings, and protects chat, focused text, and Jilaton editors. Policy/config/registry tests pass. Packet delay, blink, sprint/sneak controls, timer, and server-side inventory detection are omitted; live menu movement is unverified. LiquidBounce JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898` |
| `movement/liquidwalk/ModuleLiquidWalk` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/longjump/ModuleLongJump` | `LongJump (NCPBoost subset)` | REA/CFR confirms the module's jump tracking, optional auto-jump and disable-after-landing controls. `NoCheatPlusBoost` applies a one-shot X/Z multiplier (default 4.25, range 1–10) and zeros horizontal movement when jump is active without directional input. Jilaton adapts this through the shared jump mixin, movement and tick events with state reset on landing/disable. Bow, Vulcan and Matrix modes remain unported because they require separate bow-use, timed glide and flag state machines; no behavior from those modes is claimed. Math/registry/config/build tests pass; in-game movement unverified. |
| `movement/noslow/ModuleNoSlow` | `NoSlow (partial)` | REA archive inventory verified `ModuleNoSlow`, `PlayerUseMultiplier`, and `WhenMappings` entries by archive hash. Static `javap` confirms the handler reads the player's active item-use animation and dispatches EAT/DRINK to consume behavior, BLOCK/BOW/TRIDENT/CROSSBOW to the blocking group, and remaining animations to additional groups. Jilaton now selects six real per-action scales in its vanilla `LocalPlayer.modifyInput` hook, composed with the global scale and clamped, plus the independent sprint eligibility hook. The reference's protocol/server-specific packet modes and axis-specific handling remain unported; behavior still needs in-game verification |
| `movement/noweb/ModuleNoWeb` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/speed/ModuleSpeed` | `Speed (LegitHop only; partial)` | REA verified `ModuleSpeed` and its generic mode hierarchy. The module registers 21 modes; `SpeedLegitHop` delegates to `SpeedBHopBase`, whose movement-input handler sets jump when grounded and moving and consults jump optimizations for Criticals/anti-corner delay. Jilaton adapts that grounded input hop with configurable item/sneak/fall-flying/liquid guards and optional sprint requirement. It does not port the anti-corner simulated-player prediction, Scaffold/combat/potion requirement groups, or the other 20 server-specific modes. REA also found `SpeedSpartanV4043FastFall`, a mode nested under Spartan speed rather than a standalone FastFall module; static javap shows it depends on jump bursts, timer manipulation, and an on-ground movement packet. Jilaton's separate generic `fast_fall` uses the local movement event and does not claim that server-specific behavior. REA occurrences: `ModuleSpeed` `occ_adacbc27ef7033b6ffccb77f1285fa57992c5e0232278a2d0a21dddada591e5e`; `SpeedLegitHop` `occ_78d1310cee4e83a9e2e63ab73562995637834cd6d2d5cfc452e51cb126c8bf15`; `SpeedBHopBase` `occ_13612d96ecc9389c747790cc6f3c6e988faeb27863857292fb358efd263fc52b`; `SpeedAntiCornerBump` `occ_521157640e2ee946366af138033c6e7fe1910afdfb4ff057ca66811a5e1a6a00`; `SpeedSpartanV4043FastFall` `occ_f359270cf24caca1d4bfa68947d2684087c0be64d2565a67f465297d6fd4107f`. Policy/registry/config tests and build pass; live movement is unverified |
| `movement/spider/ModuleSpider` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/step/ModuleReverseStep` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/step/ModuleStep` | `Step (collision height subset)` | REA/CFR inspection found five modes: Instant (the default), Legit, Vulcan286, BlocksMC, and Hypixel. `MixinEntity` adds the `PlayerStepEvent` height override to movement collision and publishes step-success data; the Instant path can synthesize a sequence of movement packets, trim/shape the simulated jump, vary the wait, and select packet types. Legit allows vanilla auto-jump. Jilaton adapts only the collision-height seam using a 0.6–2.5 setting and `Entity.move`'s `maxUpStep` call for the local player. It does not port packet sequences, Timer, Blink, spoofed ground, alternate bypasses, or server-specific mode state because those require packet sequencing and timing infrastructure absent from Jilaton. REA archive SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; archive inventory evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`. Policy, registry/config tests, clean build, and generated refmap mapping pass; live collision behavior is unverified |
| `movement/terrainspeed/ModuleTerrainSpeed` | — | Not integrated; dependencies, settings, and behavior not yet audited |

## player (28)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `player/ModuleAntiAFK` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAntiExploit` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAutoBreak` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAutoFish` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAutoRespawn` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAutoWalk` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleAutoWindCharge` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleBlink` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleChestCleaner` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleEagle` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleFastExp` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleFastUse` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleNoBlockInteract` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleNoEntityInteract` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleNoRotateSet` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleNoSlotSet` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModulePotionSpoof` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleReach` | `Reach (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `player/ModuleReplenish` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/ModuleSmartEat` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/antivoid/ModuleAntiVoid` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/autobuff/ModuleAutoBuff` | `AutoPot / AutoGapple / AutoSoup / AutoHead / AutoBuffRefill (partial)` | AutoPot adapts the Health/Regen/Strength/Speed/Fire Resistance/Jump Boost/Water Breathing potion groups and adds a configurable refresh window; AutoGapple uses the audited `HealthBasedBuff` absorption-aware threshold; AutoSoup adapts verified `Soup.class` hotbar stew selection, health gate, item use, and optional bowl throw; AutoHead adapts verified `Head.class` player-head selection, the same health gate, maximum absorption limit, and cooldown. AutoBuffRefill quick-moves eligible items from main inventory only while the player's own inventory screen is open, gated by active consumer modules and empty hotbar space. Drink feature family, silent slot strategy, rotation modes, and combat-pause coordination remain unported. REA inventory evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`; installed JAR SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`; Soup `occ_f8889cc3641218fed2cee19dc488c26038426383b27d32ab12cfa9759f8d4ca2`; Head `occ_22a11e3aea0ff85ceb72ebb0f77afacb77cea9e99c8a996a3eccfaa97ffab77e`; HealthBasedBuff `occ_bc8c1e222c742ff2dde67c1ac5b79f4c90145e3829d7859679b1a4137a2cde04`. |
| `player/autoqueue/ModuleAutoQueue` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/autoshop/ModuleAutoShop` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/cheststealer/ModuleChestStealer` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/invcleaner/ModuleInventoryCleaner` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `player/nofall/ModuleNoFall` | `NoFall SpoofGround (partial)` | REA archive inventory plus `javap` inspection audited all 19 available NoFall mode class names and traced SpoofGround: movement-packet onGround mutation at Smart safe-fall distance, optional local fall-distance reset, and Constant threshold option. Jilaton ports that mode with safety exemptions; remaining 18 server-specific modes are not integrated and are not claimed as compatible. |
| `player/offhand/ModuleOffhand` | `AutoTotem (partial)` | Partial adaptation; source behavior and dependencies need further audit |

## render (56)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `render/ModuleAnimations` | `SwingAnimations (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `render/ModuleAntiBlind` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleAspect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleAutoF5` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleBedPlates` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleBetterInventory` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleBlockESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleBlockOutline` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleBreadcrumbs` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleChams` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleClickGui` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleCombineMobs` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleCrystalView` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleCustomAmbience` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleDamageParticles` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleDebug` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleFreeCam` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleFreeLook` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleFullBright` | `Fullbright (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `render/ModuleHoleESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleHud` | `HUD (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `render/ModuleItemChams` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleItemESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleItemTags` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleJumpEffect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleLogoffSpot` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleMobOwners` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleNewChunks` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleNoBob` | `NoBob (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `render/ModuleNoFov` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleNoHurtCam` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleNoSwing` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleParticles` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleProphuntESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleProtectionZones` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleQuickPerspectiveSwap` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleRadar` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleRotations` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleSilentHotbar` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleSkinChanger` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleSmoothCamera` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleStorageESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleTNTTimer` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleTracers` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleTrueSight` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleVoidESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleXRay` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/ModuleZoom` | `Zoom (partial)` | Public-source behavior reviewed and adapted to Java: hold/toggle bind, configurable FOV, scroll adjustment, float exponential interpolation, scaled mouse sensitivity. Settings/profile and ratio tests pass; Mixins load in the dev client. Full live hold/scroll/sensitivity behavior remains unverified. LiquidBounce easing choices/event infrastructure are not imported. |
| `render/cameraclip/ModuleCameraClip` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/crosshair/ModuleCrosshair` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/esp/ModuleESP` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/hats/ModuleHats` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/hitfx/ModuleHitFX` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/murdermystery/ModuleMurderMystery` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/nametags/ModuleNametags` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `render/trajectories/ModuleTrajectories` | — | Not integrated; dependencies, settings, and behavior not yet audited |

## world (25)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `world/ModuleAirPlace` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleAutoDisable` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleAutoGolemRepair` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleAutoTool` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleBedDefender` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleBlockIn` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleBlockTrap` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleExtinguish` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleFastBreak` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleFastPlace` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleHoleFiller` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleLiquidPlace` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleNoInterpolation` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleNoSlowBreak` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleProjectilePuncher` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleStrongholdFinder` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleSurround` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/ModuleTimer` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/autobuild/ModuleAutoBuild` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/autofarm/ModuleAutoFarm` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/fucker/ModuleFucker` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/nuker/ModuleNuker` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/packetmine/ModulePacketMine` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/scaffold/ModuleScaffold` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `world/traps/ModuleAutoTrap` | — | Not integrated; dependencies, settings, and behavior not yet audited |


