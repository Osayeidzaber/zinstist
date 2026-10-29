# Jilaton implementation inventory

Checkpoint: 2026-10-30. This is a live inventory, not a completion claim. Rows marked unknown are explicit open work. Latest clean `gradlew clean build` passed 47 tests (0 failures/errors/skips); packaged client SHA-256 is `3dccdc90fbef6c0ecf00b39e82c2b5b451a7d4cd62d84562dc3489ef89e76197`.

## Jilaton modules

Jilaton targets Java 21, Fabric Loader 0.20.1, Fabric API 0.141.6, Minecraft 1.21.11. The project builds as a client-only Fabric mod.

| Category | Jilaton module | Source | Status / verification | Dependencies |
|---|---|---|---|---|
| COMBAT | `anti_bot` | `src/main/java/dev/jilaton/client/module/impl/AntiBotModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `friends` | `src/main/java/dev/jilaton/client/module/impl/FriendsModule.java` | Middle-clicking a targeted player adds them; repeating removes them. Friend list is saved, friend targeting is filtered, and an overhead blue hat is rendered. Toggle has unit coverage; hat not live/multiplayer-verified | Jilaton module/settings/config; client tick polls middle mouse and targeted entity; world-render event draws the hat |
| COMBAT | `teams` | `src/main/java/dev/jilaton/client/module/impl/TeamsModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `aim_assist` | `src/main/java/dev/jilaton/client/module/impl/AimAssistModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `kill_aura` | `src/main/java/dev/jilaton/client/module/impl/KillAuraModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `hitbox` | `src/main/java/dev/jilaton/client/module/impl/HitboxModule.java` | Configurable 0–1 entity pick margin (default 0.1), player-only filter and shared friend/team/anti-bot targeting checks; mixin mapped by Loom, helper/registry tests pass; live target acquisition not verified | `Entity.getPickRadius()` return mixin; does not modify physical collision boxes |
| PLAYER | `reach` | `src/main/java/dev/jilaton/client/module/impl/ReachModule.java` | Configurable entity-range bonus (default 1, max 5), block-range bonus (default 0.5, max 64), and item `AttackRange` maximum extension; mixins mapped, math/registry tests and full build pass; live interaction behavior not verified; server-side range checks still apply | Local player's entity/block interaction range accessors and `LivingEntity.entityAttackRange()` result; no through-wall range mode |
| COMBAT | `criticals` | `src/main/java/dev/jilaton/client/module/impl/CriticalsModule.java` | Jump-mode queue and falling-window predicate implemented; full build and unit suite pass; live attack timing not verified | Attack callback cancellation/replay, module tick, player target filters; packet/no-ground/blink/timer modes are not ported because their packet/timer hooks are absent and require separate ordering review |
| COMBAT | `crystal_aura` | `src/main/java/dev/jilaton/client/module/impl/CrystalAuraModule.java` | Build passed; damage estimator has unit tests; combat behavior not in-game verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `auto_anchor` | `src/main/java/dev/jilaton/client/module/impl/AutoAnchorModule.java` | Right-click-triggered place/charge/detonate state machine with configurable delay, hotbar switching, respawn-anchor dimension guard, estimated self-damage ceiling, friend blast protection, and optional slot restoration. Registry/safety-policy unit tests and clean build pass; Minecraft interaction and server timing remain unverified. Anubis's block-shield planner and Double mode are not ported. | Fabric `UseBlockCallback`, client tick, `CrystalDamageEstimator` using diameter 10 (Anubis anchor power 5), `FriendManager`, `RespawnAnchorBlock.CHARGE` |
| COMBAT | `triggerbot` | `src/main/java/dev/jilaton/client/module/impl/TriggerbotModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `velocity` | `src/main/java/dev/jilaton/client/module/impl/VelocityModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `wtap` | `src/main/java/dev/jilaton/client/module/impl/WTapModule.java` | W-Tap and S-Tap paths compile; S-Tap input unit tests pass; full Gradle build passes; live attack timing not verified | Attack-tail mixin, keyboard-input mixin/accessor, movement-input dispatch, sprint coordination |
| COMBAT | `auto_clicker` | `src/main/java/dev/jilaton/client/module/impl/AutoClickerModule.java` | Configurable min/max CPS, steady or bounded normal-sample timing, optional attack-key/cooldown/item-use gates, and existing target filters; timing/registry unit tests and full build pass; in-game timing not verified | Minecraft tick and crosshair entity target; does not yet include LiquidBounce's separate right-click `UseButton` or its full click-pattern set |
| COMBAT | `auto_totem` | `src/main/java/dev/jilaton/client/module/impl/AutoTotemModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `auto_pot` | `src/main/java/dev/jilaton/client/module/impl/AutoPotModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `auto_gapple` | `src/main/java/dev/jilaton/client/module/impl/AutoGappleModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| COMBAT | `auto_armor` | `src/main/java/dev/jilaton/client/module/impl/AutoArmorModule.java` | Full-kit damage score now includes armor, toughness, knockback resistance, LiquidBounce-derived enchantment weights, durability, minimum-upgrade, and Binding Curse checks; three scorer unit tests and full build pass; inventory swap not live-verified | InventoryMenu slot mapping, vanilla inventory click transaction, current-level enchantment registry |
| MOVEMENT | `sprint` | `src/main/java/dev/jilaton/client/module/impl/SprintModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| MOVEMENT | `parkour` | `src/main/java/dev/jilaton/client/module/impl/ParkourModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| MOVEMENT | `no_slow` | `src/main/java/dev/jilaton/client/module/impl/NoSlowModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| MOVEMENT | `target_strafe` | `src/main/java/dev/jilaton/client/module/impl/TargetStrafeModule.java` | Input-mode target orbit, radial correction, direction and range settings implemented; math/registry tests and full build pass; live movement not verified | MovementInputEvent vector override, KeyboardInput mixin, optional KillAura current target, friend/team/anti-bot filters; Motion mode omitted pending a cancellable player-move hook |
| RENDER | `fullbright` | `src/main/java/dev/jilaton/client/module/impl/FullbrightModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| RENDER | `swing_animations` | `src/main/java/dev/jilaton/client/module/impl/SwingAnimationsModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| RENDER | `no_bob` | `src/main/java/dev/jilaton/client/module/impl/NoBobModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |
| CLIENT | `hud` | `src/main/java/dev/jilaton/client/module/impl/HudModule.java` | Build compiled; in-game behavior not independently verified | Jilaton module/settings/config; per-module dependencies need source audit |

Supporting classes that are not modules include `FriendManager`, `FriendHatRenderer`, `ClickTiming`, `HitboxMargin`, `ReachMath`, `PlayerTargetFilters`, `CrystalDamageEstimator`, `ArmorScoring`, `MovementInputEvent`, `SprintTapInput`, `TargetStrafeMath`, `CriticalWindow`, and the client command/config/UI classes. The latest full build ran 47 unit tests with 0 failures and 0 errors, covering config/theme/registry, friend toggle, CPS sampling and timing, hitbox-margin selection, reach math, armor scoring, critical-window predicate, target-strafe vector math, W-Tap/S-Tap input conversion, and crystal damage math; they do not establish full live combat behavior.

On 2026-10-30, REA re-inventoried the installed LiquidBounce JAR at `C:\Users\zaber\AppData\Roaming\CCBlueX\LiquidLauncher\data\gameDir\nextgen\mods\LiquidBounce.jar` (SHA-256 `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898`). REA reported complete central-directory coverage: 4,642 occurrences, 4,641 artifact nodes, 4,253 files, 389 directories, and no integrity contradictions. The archive contains `net/ccbluex/liquidbounce/features/module/modules/combat/ModuleSuperKnockback.class`; this ZIP inventory alone does not establish its bytecode behavior. The current WTap/S-Tap implementation remains attributed to the Anubis lifecycle adaptation and Jilaton-authored input code in `ATTRIBUTION.md`.

## Anubis module class inventory

Source is the local CFR 0.152 decompilation of the supplied JAR. Every listed class was discovered by filename; feature behavior and transitive dependencies are not inferred from the name. Port status is not yet implemented unless separately noted in `ATTRIBUTION.md`.

The decompilation currently contains 133 `*Module.java` files across combat, movement, render, misc, client, and Donut macro packages. Donut macro entries are listed for audit completeness only; they are not candidates for Jilaton.

| Package category | Decompiled module class | Port / dependency / compatibility status |
|---|---|---|
| client | `client/HotkeysModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| client | `client/HudModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| client | `client/PerformanceModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| client | `client/RadarModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| client | `client/RegionMapModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| client | `client/SpotifyHudModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/AimAssistModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/AnchorAuraModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/SingleAnchorMacroModule.java` | Source inspected for explicit right-click trigger, place/charge/detonate sequencing, dimension checks, hotbar switching, lethal-damage guard, and optional block shielding. Jilaton `auto_anchor` adapts the single-anchor sequence and safety checks; Anubis shielding planner and Double mode remain unported. |
| combat | `combat/AutoCityModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/AutoInvTotemModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/AutoTotemModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/CrystalAuraModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/CrystalMacroModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/CrystalOptimizerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/FastXpModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/FriendGuardModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/HoverTotemModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/JumpResetModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/KeyThrowModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/KillAuraModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/MaceModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/ShieldBreakerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/SingleAnchorMacroModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/SpearLungeModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/TriggerbotModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| combat | `combat/WTapRecodeModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/AhSellModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/AhSniperModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/AutoSellModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/AutoTpaHereModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/OrderAhSellerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/OrderDropperModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/PanicPayModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| donutmacros | `donutmacros/TpMacroModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AirStuckModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AmethystBypassModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AntiTrapModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AntiVanishModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AutoClickerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AutoEatModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AutoReconnectModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AutoRelogModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/AutoReplenishModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/BlockNotifierModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/ChatMacroModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/CoordSnapperModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/DiscordPresenceModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/DonutNoFallModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/FastUseModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/FreecamModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/HomeMetaModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/LegitAutoToolModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/NameProtectModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/NetheriteFinderModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/NoInvLeakModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/PlayerBypass2Module.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/PlayerBypassModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/PlayerDetectionModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/QuantumModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/RenderDistanceToggleModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/SkinChangeModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/SlowSwingModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/SpamModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/SpawnerProtectModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/SpeedMineModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/StaffListModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/TridentUtilModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/VehicleLogModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| misc | `misc/WeatherNotifierModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/AutoJumpModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/AutoWalkModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/BoatFlyModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/BoatNoclipModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/ClickTpModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/ElytraFlyModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/ElytraSwapModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/EntityControlModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/EntitySpeedModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/FlyModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/JesusModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/NoJumpDelayModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/SneakModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/SpeedModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/SpiderModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/SprintModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| movement | `movement/TridentBoostModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/AtmosphereModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/BetterSusChunkModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/BlockEntityDebugModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/BlockEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/BlockOverlayModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/character/CharacterModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/ChunkScannerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/ColorGradingModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/CustomCrosshairModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/CustomFovModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/CustomGlintModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/EspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/FloatingRelicsModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/FreelookModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/FullbrightModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HandAuraModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HandModelModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HandsModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HeadCosmeticModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HitParticlesModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/HoleEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/ItemEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/JumpCirclesModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/LightEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/LowHealthWarningModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/MotionBlurModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/NametagsModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/NoRenderModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/PearlTrajectoryModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/PharaohHeaddressModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/PrimeChunkFinderModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/RelicParticlesModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/ShaderEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/SpawnerNametagsModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/SpawnerNotifierModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/StorageEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/SusChunkFinderModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/SwingAnimationModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/TargetEspModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/TimeChangerModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/TotemSpiritModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/TracersModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/TrajectoriesModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/WaypointsModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/WeaponInspectModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/XrayModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |
| render | `render/ZoomModule.java` | Discovered; per-class dependency, compatibility and behavior audit pending; no port claimed |

## LiquidBounce top-level module inventory

The local source checkout at commit `e033f5aaf05be802dd2ca51782f255119d7d5654` contains 94 top-level `ClientModule` declarations under the module tree. The checkout is a source reference; it is not cryptographically matched to the installed LiquidBounce JAR. Child mode/feature classes are dependencies/features rather than separate top-level modules and are not flattened into this table.

| Declared category | LiquidBounce module | Source path | Port / dependency / compatibility status |
|---|---|---|---|
| COMBAT | `AutoBow` | `combat/aimbot/ModuleAutoBow.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `DroneControl` | `combat/aimbot/ModuleDroneControl.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `ProjectileAimbot` | `combat/aimbot/ModuleProjectileAimbot.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoArmor` | `combat/autoarmor/ModuleAutoArmor.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `Backtrack` | `combat/backtrack/ModuleBacktrack.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `Criticals` | `combat/criticals/ModuleCriticals.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `combat/crystalaura/ModuleCrystalAura.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `ElytraTarget` | `combat/elytratarget/ModuleElytraTarget.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `KillAura` | `combat/killaura/ModuleKillAura.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `Aimbot` | `combat/ModuleAimbot.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoClicker` | `combat/ModuleAutoClicker.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoLeave` | `combat/ModuleAutoLeave.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoRod` | `combat/ModuleAutoRod.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoShoot` | `combat/ModuleAutoShoot.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoWeapon` | `combat/ModuleAutoWeapon.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `FakeLag` | `combat/ModuleFakeLag.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `Hitbox` | `combat/ModuleHitbox.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `KeepSprint` | `combat/ModuleKeepSprint.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `MaceKill` | `combat/ModuleMaceKill.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `NoMissCooldown` | `combat/ModuleNoMissCooldown.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `SuperKnockback` | `combat/ModuleSuperKnockback.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `SwordBlock` | `combat/ModuleSwordBlock.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `TimerRange` | `combat/ModuleTimerRange.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `SpearKill` | `combat/spearkill/ModuleSpearKill.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `TpAura` | `combat/tpaura/ModuleTpAura.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `Velocity` | `combat/velocity/ModuleVelocity.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `AntiBot` | `misc/antibot/ModuleAntiBot.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| RENDER | `BetterChat` | `misc/betterchat/ModuleBetterChat.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `DebugRecorder` | `misc/debugrecorder/ModuleDebugRecorder.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `AntiCheatDetect` | `misc/ModuleAntiCheatDetect.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `AntiStaff` | `misc/ModuleAntiStaff.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleAutoAccount.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `AutoChatGame` | `misc/ModuleAutoChatGame.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleAutoConfig.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleAutoPearl.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| RENDER | `BetterTab` | `misc/ModuleBetterTab.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleBetterTitle.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| EXPLOIT | `BookBot` | `misc/ModuleBookBot.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `EasyPearl` | `misc/ModuleEasyPearl.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleElytraSwap.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `FlagCheck` | `misc/ModuleFlagCheck.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleGUICloser.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| WORLD | `InventoryTracker` | `misc/ModuleInventoryTracker.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `ItemScroller` | `misc/ModuleItemScroller.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `Macros` | `misc/ModuleMacros.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `misc/ModuleMiddleClickAction.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `Notifier` | `misc/ModuleNotifier.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `PacketLogger` | `misc/ModulePacketLogger.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `Spammer` | `misc/ModuleSpammer.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `TargetLock` | `misc/ModuleTargetLock.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `Teams` | `misc/ModuleTeams.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `TextFieldProtect` | `misc/ModuleTextFieldProtect.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MISC | `NameProtect` | `misc/nameprotect/ModuleNameProtect.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| PLAYER | `ReportHelper` | `misc/reporthelper/ModuleReportHelper.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `AutoDodge` | `movement/autododge/ModuleAutoDodge.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `ElytraFly` | `movement/elytrafly/ModuleElytraFly.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Fly` | `movement/fly/ModuleFly.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `HighJump` | `movement/highjump/ModuleHighJump.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `InventoryMove` | `movement/inventorymove/ModuleInventoryMove.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `movement/liquidwalk/ModuleLiquidWalk.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `LongJump` | `movement/longjump/ModuleLongJump.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `AirJump` | `movement/ModuleAirJump.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `movement/ModuleAnchor.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `AntiBounce` | `movement/ModuleAntiBounce.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `AntiLevitation` | `movement/ModuleAntiLevitation.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `AvoidHazards` | `movement/ModuleAvoidHazards.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `BlockBounce` | `movement/ModuleBlockBounce.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `BlockWalk` | `movement/ModuleBlockWalk.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `ElytraRecast` | `movement/ModuleElytraRecast.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `EntityControl` | `movement/ModuleEntityControl.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Freeze` | `movement/ModuleFreeze.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoClip` | `movement/ModuleNoClip.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoJumpDelay` | `movement/ModuleNoJumpDelay.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoPose` | `movement/ModuleNoPose.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoPush` | `movement/ModuleNoPush.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Parkour` | `movement/ModuleParkour.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `SafeWalk` | `movement/ModuleSafeWalk.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `SnapTap` | `movement/ModuleSnapTap.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Sneak` | `movement/ModuleSneak.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Sprint` | `movement/ModuleSprint.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Strafe` | `movement/ModuleStrafe.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `TargetStrafe` | `movement/ModuleTargetStrafe.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| unknown | `unknown` | `movement/ModuleTeleport.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `TridentBoost` | `movement/ModuleTridentBoost.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `VehicleBoost` | `movement/ModuleVehicleBoost.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `VehicleControl` | `movement/ModuleVehicleControl.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoSlow` | `movement/noslow/ModuleNoSlow.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `NoWeb` | `movement/noweb/ModuleNoWeb.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Speed` | `movement/speed/ModuleSpeed.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Spider` | `movement/spider/ModuleSpider.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `ReverseStep` | `movement/step/ModuleReverseStep.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `Step` | `movement/step/ModuleStep.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| MOVEMENT | `TerrainSpeed` | `movement/terrainspeed/ModuleTerrainSpeed.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |
| COMBAT | `TickBase` | `combat/ModuleTickBase.kt` | Discovered; per-module dependency, compatibility, license-scope and behavior audit pending; no port claimed |

## Next work

Audit remaining reference modules by behavior and dependency graph, then implement compatible PvP features in small verified batches. Do not count discovered names as implementations. Track game-level verification separately from build and unit-test evidence.
