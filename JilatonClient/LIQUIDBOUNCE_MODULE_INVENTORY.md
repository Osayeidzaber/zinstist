# LiquidBounce compiled module inventory

This inventory is derived from REA’s complete central-directory view of the local LiquidBounce JAR. It lists top-level `Module*.class` files under `net/ccbluex/liquidbounce/features/module/modules/`, excluding `$` nested/anonymous classes and compiler-generated `*Kt.class` file facades. The archive contains 4,642 occurrences (4,641 nodes), with no ZIP integrity contradictions; that is structural coverage only, not a behavioral or security verdict. The JAR remains unexecuted and its overall static security status remains **Insufficient evidence**. SHA-256: `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`.

Jilaton mappings below indicate only that a related implementation exists. They do not claim feature parity or runtime verification. `Not integrated` means no matching Jilaton feature was identified in the current code inventory; detailed dependency/behavior review remains open.

Found **232** module entry class files across 8 categories.

## combat (26)

| Compiled module class | Jilaton coverage | Status |
|---|---|---|
| `combat/ModuleAimbot` | `AimAssist (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleAutoClicker` | `AutoClicker (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleAutoLeave` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleAutoRod` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleAutoShoot` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleAutoWeapon` | `AutoWeapon (partial)` | REA inventory and static `javap` show preferred hotbar weapon selection on target/attack, silent switch-back, shield-break axe selection, optional mace-smash integration, and action settings. Jilaton implements attack-time selection by active attack-damage attributes/Sharpness, axe-on-block, vanilla mace-smash eligibility, slot-owner-aware switch-back, and AutoPot/AutoGapple slot-reservation checks. LiquidBounce multi-choice preferences, silent-hotbar arbitration, on-target switching, and auto-Mace module coordination remain unported |
| `combat/ModuleFakeLag` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleHitbox` | `Hitbox (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleKeepSprint` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleMaceKill` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleNoMissCooldown` | `NoMissCooldown (partial)` | REA archive inventory and static `javap` show two settings: replace the `Minecraft.startAttack` `missTime` read with zero and optionally consume the action when the hit result is `MISS`. Jilaton implements both as independent settings, including suppression of the miss delay write; it affects only the local miss-time gate, not server attack-strength or validation |
| `combat/ModuleSuperKnockback` | `WTap (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/ModuleSwordBlock` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleTickBase` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/ModuleTimerRange` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/aimbot/ModuleAutoBow` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/aimbot/ModuleDroneControl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/aimbot/ModuleProjectileAimbot` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/autoarmor/ModuleAutoArmor` | `AutoArmor (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/backtrack/ModuleBacktrack` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/criticals/ModuleCriticals` | `Criticals (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/crystalaura/ModuleCrystalAura` | `CrystalAura (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/elytratarget/ModuleElytraTarget` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/killaura/ModuleKillAura` | `KillAura (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `combat/tpaura/ModuleTpAura` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `combat/velocity/ModuleVelocity` | `Velocity (partial)` | Partial adaptation; source behavior and dependencies need further audit |

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
| `misc/ModuleTeams` | `Teams (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `misc/ModuleTextFieldProtect` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `misc/antibot/ModuleAntiBot` | `AntiBot (partial)` | Partial adaptation; source behavior and dependencies need further audit |
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
| `movement/ModuleNoJumpDelay` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleNoPose` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleNoPush` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleParkour` | `Parkour (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `movement/ModuleSafeWalk` | `SafeWalk OnEdge (partial)` | REA inventory and `javap` inspection found two modes: direct PlayerSafeWalkEvent Safe mode and OnEdge's movement-input/edge-distance logic with keep/sneak/jump settings. Jilaton implements an edge-footprint support check that injects vanilla sneak via MovementInputEvent and coordinates with Parkour; the full SafeWalkEvent pipeline, simulated next-tick position, and jump-on-edge behavior are not ported. |
| `movement/ModuleSneak` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleSprint` | `Sprint (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `movement/ModuleStrafe` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleTargetStrafe` | `TargetStrafe (partial)` | Partial adaptation; source behavior and dependencies need further audit |
| `movement/ModuleTeleport` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleVehicleBoost` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/ModuleVehicleControl` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/autododge/ModuleAutoDodge` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/elytrafly/ModuleElytraFly` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/fly/ModuleFly` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/highjump/ModuleHighJump` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/inventorymove/ModuleInventoryMove` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/liquidwalk/ModuleLiquidWalk` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/longjump/ModuleLongJump` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/noslow/ModuleNoSlow` | `NoSlow (partial)` | REA archive inventory and static `javap` show a `PlayerUseMultiplier` event with item-action-specific multiplier groups; Jilaton adapts vanilla `LocalPlayer.modifyInput` slowdown scaling and `isSlowDueToUsingItem` sprint eligibility, with tested removal/scale policy. Per-action groups, axis-specific values, and server-specific packet modes are not ported |
| `movement/noweb/ModuleNoWeb` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/speed/ModuleSpeed` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/spider/ModuleSpider` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/step/ModuleReverseStep` | — | Not integrated; dependencies, settings, and behavior not yet audited |
| `movement/step/ModuleStep` | — | Not integrated; dependencies, settings, and behavior not yet audited |
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
| `player/autobuff/ModuleAutoBuff` | `AutoPot / AutoGapple (partial)` | AutoPot adapts the Health/Regen/Strength/Speed/Fire Resistance/Jump Boost/Water Breathing potion groups and adds a configurable refresh window; AutoGapple uses the audited `HealthBasedBuff` absorption-aware threshold with its own opt-out setting. Full AutoBuff feature families (drink, soup, head, refill, rotations modes, and combat-pause coordination) remain unported. |
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
| `render/ModuleZoom` | — | Not integrated; dependencies, settings, and behavior not yet audited |
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


