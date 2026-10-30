package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.FlightModule;
import dev.jilaton.client.module.impl.AimAssistModule;
import dev.jilaton.client.module.impl.AntiBotModule;
import dev.jilaton.client.module.impl.CrystalAuraModule;
import dev.jilaton.client.module.impl.AutoArmorModule;
import dev.jilaton.client.module.impl.CriticalsModule;
import dev.jilaton.client.module.impl.ElytraTargetModule;
import dev.jilaton.client.module.impl.AutoClickerModule;
import dev.jilaton.client.module.impl.AutoLeaveModule;
import dev.jilaton.client.module.impl.AutoAnchorModule;
import dev.jilaton.client.module.impl.AutoTotemModule;
import dev.jilaton.client.module.impl.AutoRodModule;
import dev.jilaton.client.module.impl.AutoShootModule;
import dev.jilaton.client.module.impl.AutoWeaponModule;
import dev.jilaton.client.module.impl.AutoBuffRefillModule;
import dev.jilaton.client.module.impl.AutoReplenishModule;
import dev.jilaton.client.module.impl.BowAimModule;
import dev.jilaton.client.module.impl.AnchorAuraModule;
import dev.jilaton.client.module.impl.WTapModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.TargetStrafeModule;
import dev.jilaton.client.module.impl.AutoSoupModule;
import dev.jilaton.client.module.impl.AutoHeadModule;
import dev.jilaton.client.module.impl.KeepSprintModule;
import dev.jilaton.client.module.impl.SpeedModule;
import dev.jilaton.client.module.impl.HitboxModule;
import dev.jilaton.client.module.impl.InventoryMoveModule;
import dev.jilaton.client.module.impl.KillAuraModule;
import dev.jilaton.client.module.impl.ClickPatternDistribution;
import dev.jilaton.client.module.impl.TeamsModule;
import dev.jilaton.client.module.impl.NoFallModule;
import dev.jilaton.client.module.impl.NoJumpDelayModule;
import dev.jilaton.client.module.impl.StepModule;
import dev.jilaton.client.module.impl.NoMissCooldownModule;
import dev.jilaton.client.module.impl.SafeWalkModule;
import dev.jilaton.client.module.impl.StrafeModule;
import dev.jilaton.client.module.impl.HighJumpModule;
import dev.jilaton.client.module.impl.LongJumpModule;
import dev.jilaton.client.module.impl.ReachModule;
import dev.jilaton.client.module.impl.VelocityModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerTest {
    @Test
    void teamsExposeScoreboardAndNameColorMatching() {
        TeamsModule module = new ModuleManager().get("teams") instanceof TeamsModule teams ? teams : null;

        assertTrue(module != null);
        assertTrue(module.enabledByDefault());
        assertTrue(module.ignoreTeammates());
        assertTrue(module.matchNameColor().value());
        assertTrue(module.settings().stream().filter(setting -> setting.id().equals("armor_head"))
                .map(BooleanSetting.class::cast).findFirst().orElseThrow().value());
        assertFalse(module.settings().stream().filter(setting -> setting.id().equals("armor_chest"))
                .map(BooleanSetting.class::cast).findFirst().orElseThrow().value());
    }

    @Test
    void aimAssistExposesFactorSmoothingAndIndependentAxes() {
        AimAssistModule module = new ModuleManager().get("aim_assist") instanceof AimAssistModule aimAssist
                ? aimAssist : null;

        assertTrue(module != null);
        assertEquals(0.35, module.angleFactor().value(), 1.0e-9);
        assertTrue(module.horizontal().value());
        assertTrue(module.vertical().value());
    }

    @Test
    void criticalsOffersJumpAndNaturalWaitModes() {
        CriticalsModule module = new ModuleManager().get("criticals") instanceof CriticalsModule criticals
                ? criticals : null;

        assertTrue(module != null);
        assertEquals("Jump", module.mode().value());
        assertTrue(module.mode().choices().containsAll(java.util.List.of("Jump", "Wait")));
    }

    @Test
    void autoArmorExposesSaveArmorControls() {
        AutoArmorModule module = new ModuleManager().get("auto_armor") instanceof AutoArmorModule autoArmor
                ? autoArmor : null;

        assertTrue(module != null);
        assertEquals(6, module.settings().size());
        assertTrue(module.saveArmor().value());
        assertEquals(24.0, module.durabilityThreshold().value());
    }

    @Test
    void antiBotExposesTabProfileAndPacketSanityFilters() {
        AntiBotModule antiBot = new ModuleManager().get("anti_bot") instanceof AntiBotModule module ? module : null;

        assertTrue(antiBot != null);
        assertEquals(ModuleCategory.COMBAT, antiBot.category());
        assertEquals(4, antiBot.settings().size());
        assertTrue(antiBot.settings().stream().anyMatch(setting -> setting.id().equals("ignore_players_missing_from_tab")));
        assertTrue(antiBot.settings().stream().anyMatch(setting -> setting.id().equals("ignore_illegal_pitch")));
        assertTrue(antiBot.settings().stream().anyMatch(setting -> setting.id().equals("ignore_suspicious_entity_ids")));
        assertTrue(antiBot.settings().stream().anyMatch(setting -> setting.id().equals("ignore_duplicate_profiles")));
    }

    @Test
    void velocityExposesModifierAndStrafeControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule velocity = manager.get("velocity");

        assertTrue(velocity instanceof VelocityModule);
        assertEquals(ModuleCategory.COMBAT, velocity.category());
        assertEquals(21, velocity.settings().size());
        assertFalse(velocity.settings().stream().filter(setting -> setting.id().equals("strafe_only_facing"))
                .map(BooleanSetting.class::cast).findFirst().orElseThrow().value());
        assertEquals(3.5, velocity.settings().stream().filter(setting -> setting.id().equals("strafe_facing_range"))
                .map(NumberSetting.class::cast).findFirst().orElseThrow().value(), 1.0E-9);
    }

    @Test
    void flightRegistersRealCreativeModeControls() {
        FlightModule flight = new ModuleManager().get("flight") instanceof FlightModule module ? module : null;

        assertTrue(flight != null);
        assertEquals(ModuleCategory.MOVEMENT, flight.category());
        assertEquals(0.1, flight.speed().value(), 1.0E-9);
        assertEquals(4.0, flight.maxVelocity().value(), 1.0E-9);
        assertTrue(flight.forceFlight().value());
        assertTrue(flight.bypassVanillaCheck().value());
    }

    @Test
    void autoRodIsRegisteredWithItsCombatSettings() {
        ModuleManager manager = new ModuleManager();
        ClientModule autoRod = manager.get("auto_rod");

        assertTrue(autoRod instanceof AutoRodModule);
        assertEquals(ModuleCategory.COMBAT, autoRod.category());
        assertEquals(16, autoRod.settings().size());
    }

    @Test
    void autoShootIsRegisteredWithProjectileAndTargetControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule autoShoot = manager.get("auto_shoot");

        assertTrue(autoShoot instanceof AutoShootModule);
        assertEquals(ModuleCategory.COMBAT, autoShoot.category());
        assertEquals(18, autoShoot.settings().size());
        ChoiceSetting clickPattern = autoShoot.settings().stream()
                .filter(setting -> setting.id().equals("click_pattern"))
                .map(ChoiceSetting.class::cast)
                .findFirst().orElseThrow();
        assertTrue(clickPattern.choices().containsAll(ClickPatternDistribution.OPTIONS));
        assertFalse(autoShoot.enabled());
    }

    @Test
    void autoLeaveIsRegisteredWithHealthAndDelayControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule autoLeave = manager.get("auto_leave");

        assertTrue(autoLeave instanceof AutoLeaveModule);
        assertEquals(ModuleCategory.COMBAT, autoLeave.category());
        assertEquals(3, autoLeave.settings().size());
        assertFalse(autoLeave.enabled());
    }

    @Test
    void elytraTargetIsRegisteredWithFlightTrackingControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule elytraTarget = manager.get("elytra_target");

        assertTrue(elytraTarget instanceof ElytraTargetModule);
        assertEquals(ModuleCategory.COMBAT, elytraTarget.category());
        assertEquals(14, elytraTarget.settings().size());
        assertFalse(elytraTarget.enabled());
    }

    @Test
    void triggerbotExposesTargetTimingAndActionGates() {
        ModuleManager manager = new ModuleManager();
        ClientModule triggerbot = manager.get("triggerbot");

        assertTrue(triggerbot instanceof dev.jilaton.client.module.impl.TriggerbotModule);
        assertEquals(ModuleCategory.COMBAT, triggerbot.category());
        assertEquals(8, triggerbot.settings().size());
    }

    @Test
    void sprintExposesDirectionalAndVanillaConditionSettings() {
        ModuleManager manager = new ModuleManager();
        ClientModule sprint = manager.get("sprint");

        assertTrue(sprint instanceof dev.jilaton.client.module.impl.SprintModule);
        assertEquals(ModuleCategory.MOVEMENT, sprint.category());
        assertEquals(5, sprint.settings().size());
    }

    @Test
    void registryIsUniqueAndCannotBeModifiedThroughItsPublicView() {
        ModuleManager manager = new ModuleManager();
        Set<String> ids = new HashSet<>();
        manager.all().forEach(module -> assertTrue(ids.add(module.id()), "Duplicate module id: " + module.id()));

        int originalSize = manager.all().size();
        assertThrows(UnsupportedOperationException.class, manager.all()::clear);
        assertEquals(originalSize, manager.all().size());
        assertEquals(originalSize, ids.size());
    }

    @Test
    void noFallIsRegisteredAsAnOptInMovementModule() {
        ModuleManager manager = new ModuleManager();

        assertTrue(manager.get("no_fall") instanceof NoFallModule);
        assertEquals(ModuleCategory.MOVEMENT, manager.get("no_fall").category());
        assertFalse(manager.get("no_fall").enabled());
        assertEquals(3, manager.get("no_fall").settings().size());
    }

    @Test
    void safeWalkIsRegisteredAsAnOptInMovementModule() {
        ModuleManager manager = new ModuleManager();

        assertTrue(manager.get("safe_walk") instanceof SafeWalkModule);
        assertEquals(ModuleCategory.MOVEMENT, manager.get("safe_walk").category());
        assertFalse(manager.get("safe_walk").enabled());
        assertEquals(2, manager.get("safe_walk").settings().size());
    }

    @Test
    void strafeExposesGroundAirAndStrictMovementControls() {
        StrafeModule strafe = new ModuleManager().get("strafe") instanceof StrafeModule module ? module : null;

        assertTrue(strafe != null);
        assertEquals(ModuleCategory.MOVEMENT, strafe.category());
        assertEquals(1.0, strafe.strengthInAir().value());
        assertEquals(1.0, strafe.strengthOnGround().value());
        assertFalse(strafe.strictMovement().value());
    }

    @Test
    void highJumpIsRegisteredWithLiquidBounceVanillaMotionRange() {
        HighJumpModule highJump = new ModuleManager().get("high_jump") instanceof HighJumpModule module ? module : null;

        assertTrue(highJump != null);
        assertEquals(ModuleCategory.MOVEMENT, highJump.category());
        assertEquals(0.8, highJump.motion().value());
        assertEquals(0.2, highJump.motion().minimum());
        assertEquals(10.0, highJump.motion().maximum());
    }

    @Test
    void longJumpExposesBoostAndLifecycleControls() {
        LongJumpModule longJump = new ModuleManager().get("long_jump") instanceof LongJumpModule module ? module : null;

        assertTrue(longJump != null);
        assertEquals(ModuleCategory.MOVEMENT, longJump.category());
        assertEquals(4.25, longJump.horizontalBoost().value());
        assertEquals(1.0, longJump.horizontalBoost().minimum());
        assertEquals(10.0, longJump.horizontalBoost().maximum());
        assertFalse(longJump.autoJump().value());
        assertFalse(longJump.autoDisable().value());
    }

    @Test
    void noJumpDelayIsRegisteredWithCooldownControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule noJumpDelay = manager.get("no_jump_delay");

        assertTrue(noJumpDelay instanceof NoJumpDelayModule);
        assertEquals(ModuleCategory.MOVEMENT, noJumpDelay.category());
        assertFalse(noJumpDelay.enabled());
        assertEquals(2, noJumpDelay.settings().size());
    }

    @Test
    void stepIsRegisteredWithAConfigurableCollisionHeight() {
        ModuleManager manager = new ModuleManager();
        ClientModule step = manager.get("step");

        assertTrue(step instanceof StepModule);
        assertEquals(ModuleCategory.MOVEMENT, step.category());
        assertFalse(step.enabled());
        assertEquals(1, step.settings().size());
    }

    @Test
    void inventoryMoveExposesScreenPolicyAndSneakSettings() {
        ModuleManager manager = new ModuleManager();
        ClientModule inventoryMove = manager.get("inventory_move");

        assertTrue(inventoryMove instanceof InventoryMoveModule);
        assertEquals(ModuleCategory.MOVEMENT, inventoryMove.category());
        assertFalse(inventoryMove.enabled());
        assertEquals(2, inventoryMove.settings().size());
        assertEquals("Safe", ((ChoiceSetting) inventoryMove.settings().get(0)).value());
    }

    @Test
    void noSlowHasIndependentVanillaAndSprintSettings() {
        ModuleManager manager = new ModuleManager();
        ClientModule noSlow = manager.get("no_slow");

        assertEquals(ModuleCategory.MOVEMENT, noSlow.category());
        assertFalse(noSlow.enabled());
        assertEquals(9, noSlow.settings().size());
        assertEquals(1.0, ((NumberSetting) noSlow.settings().stream()
                .filter(setting -> setting.id().equals("item_use_scale")).findFirst().orElseThrow()).value());
    }

    @Test
    void autoWeaponIsRegisteredWithConfigurableSelectionAndRestoration() {
        ModuleManager manager = new ModuleManager();
        ClientModule autoWeapon = manager.get("auto_weapon");

        assertTrue(autoWeapon instanceof AutoWeaponModule);
        assertEquals(ModuleCategory.COMBAT, autoWeapon.category());
        assertFalse(autoWeapon.enabled());
        assertEquals(7, autoWeapon.settings().size());
    }

    @Test
    void autoBuffRefillIsRegisteredWithItemFamiliesAndDelay() {
        ModuleManager manager = new ModuleManager();
        ClientModule refill = manager.get("auto_buff_refill");

        assertTrue(refill instanceof AutoBuffRefillModule);
        assertEquals(ModuleCategory.PLAYER, refill.category());
        assertFalse(refill.enabled());
        assertEquals(5, refill.settings().size());
    }

    @Test
    void autoReplenishIsRegisteredWithThresholdModesAndOffhandControl() {
        ModuleManager manager = new ModuleManager();
        ClientModule replenish = manager.get("auto_replenish");

        assertTrue(replenish instanceof AutoReplenishModule);
        assertEquals(ModuleCategory.PLAYER, replenish.category());
        assertFalse(replenish.enabled());
        assertEquals(4, replenish.settings().size());
    }

    @Test
    void bowAimIsRegisteredWithProjectileAndTargetControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule bowAim = manager.get("bow_aim");

        assertTrue(bowAim instanceof BowAimModule);
        assertEquals(ModuleCategory.COMBAT, bowAim.category());
        assertFalse(bowAim.enabled());
        assertEquals(15, bowAim.settings().size());
    }

    @Test
    void noMissCooldownExposesTheReferenceMissTimeControls() {
        ModuleManager manager = new ModuleManager();
        ClientModule noMissCooldown = manager.get("no_miss_cooldown");

        assertTrue(noMissCooldown instanceof NoMissCooldownModule);
        assertEquals(ModuleCategory.COMBAT, noMissCooldown.category());
        assertFalse(noMissCooldown.enabled());
        assertEquals(2, noMissCooldown.settings().size());
    }

    @Test
    void fullbrightGammaSettingStaysWithinMinecraftUnitRange() {
        FullbrightModule fullbright = new FullbrightModule();
        NumberSetting brightness = fullbright.brightness();

        assertEquals(0.0, brightness.minimum());
        assertEquals(1.0, brightness.maximum());
        assertEquals(1.0, brightness.value());

        brightness.setValue(12.0);
        assertEquals(1.0, brightness.value());
    }

    @Test
    void wtapExposesBoundedReleaseAndPlayerFilterSettings() {
        WTapModule wtap = new WTapModule();

        assertEquals(1.0, wtap.releaseTicks().minimum());
        assertEquals(3.0, wtap.releaseTicks().maximum());
        assertEquals(1.0, wtap.releaseTicks().value());
        assertTrue(wtap.playersOnly().value());
        assertEquals("W Tap", wtap.mode().value());
        assertTrue(wtap.mode().choices().contains("S Tap"));
        assertTrue(wtap.onlyForward().value());

        assertTrue(new ModuleManager().get("wtap") instanceof WTapModule);
    }

    @Test
    void swingAnimationsExposeLiveStyleAndScaleSettings() {
        SwingAnimationsModule module = new SwingAnimationsModule();
        ChoiceSetting mode = (ChoiceSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("mode"))
                .findFirst().orElseThrow();
        assertEquals("Smooth", mode.value());
        mode.cycle(1);
        assertEquals("Power", mode.value());
        mode.setValue("Feast");
        assertEquals("Feast", mode.value());
        assertTrue(new ModuleManager().get("swing_animations") instanceof SwingAnimationsModule);
    }

    @Test
    void choiceSettingRejectsUnknownModesAndCyclesInBothDirections() {
        ChoiceSetting setting = new ChoiceSetting("mode", "Mode", java.util.List.of("A", "B", "C"), "A");
        setting.cycle(-1);
        assertEquals("C", setting.value());
        setting.cycle(1);
        assertEquals("A", setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.setValue("unknown"));
    }

    @Test
    void crystalAuraIsRegisteredWithRealDamageAndActionSettings() {
        CrystalAuraModule module = new ModuleManager().get("crystal_aura") instanceof CrystalAuraModule crystalAura
                ? crystalAura : null;

        assertTrue(module != null);
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("minimum_damage")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("maximum_self_damage")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("auto_switch")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("face_place_health")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("face_place_damage")));
    }

    @Test
    void autoAnchorIsRegisteredWithDamageAndSlotLifecycleSettings() {
        AutoAnchorModule module = new ModuleManager().get("auto_anchor") instanceof AutoAnchorModule autoAnchor
                ? autoAnchor : null;

        assertTrue(module != null);
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("max_self_damage")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("delay")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("switch_back")));
    }

    @Test
    void anchorAuraIsRegisteredWithPlannerAndRotationSettings() {
        AnchorAuraModule module = new ModuleManager().get("anchor_aura") instanceof AnchorAuraModule anchorAura
                ? anchorAura : null;

        assertTrue(module != null);
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("min_damage")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("max_self_damage")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("turn_speed")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("auto_switch")));
    }

    @Test
    void killAuraExposesAnubisTargetPrioritiesAndSeparateEntityTypes() {
        KillAuraModule module = new ModuleManager().get("kill_aura") instanceof KillAuraModule aura ? aura : null;

        assertTrue(module != null);
        assertEquals("Angle", module.priority().value());
        assertTrue(module.priority().choices().containsAll(java.util.List.of("Distance", "Health", "Angle")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("players")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("hostiles")));
        ChoiceSetting raycastMode = module.settings().stream()
                .filter(setting -> setting.id().equals("raycast_mode"))
                .map(ChoiceSetting.class::cast)
                .findFirst().orElseThrow();
        assertEquals(java.util.List.of("None", "Enemy", "All"), raycastMode.choices());
        assertEquals("None", raycastMode.value());
        assertEquals(6.0, module.minimumClicksPerSecond().value());
        assertEquals(8.0, module.clicksPerSecond().value());
        assertEquals("Normal distribution", module.clickPattern().value());
        assertTrue(module.clickPattern().choices().containsAll(ClickPatternDistribution.OPTIONS));
        assertTrue(module.respectAttackCooldown().value());
    }

    @Test
    void autoTotemExposesStorageSourceAndRefillTimingControls() {
        AutoTotemModule module = new ModuleManager().get("auto_totem") instanceof AutoTotemModule autoTotem
                ? autoTotem : null;

        assertTrue(module != null);
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("use_hotbar_totems")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("refill_delay")));
        assertTrue(module.settings().stream().anyMatch(setting -> setting.id().equals("replace_offhand")));
    }

    @Test
    void targetStrafeIsRegisteredWithMovementAndOrbitControls() {
        TargetStrafeModule module = new ModuleManager().get("target_strafe") instanceof TargetStrafeModule targetStrafe
                ? targetStrafe : null;

        assertTrue(module != null);
        assertEquals(2.95, module.radius().value());
        assertEquals(4.0, module.followRange().value());
        assertEquals("Input", module.mode().value());
        assertTrue(module.mode().choices().contains("Motion"));
        assertTrue(module.requireAuraTarget().value());
        assertTrue(module.requireMovementInput().value());
        assertTrue(module.collisionCheck().value());
        assertTrue(module.edgeCheck().value());
    }

    @Test
    void autoSoupIsRegisteredWithHealthAndInventoryControls() {
        AutoSoupModule module = new ModuleManager().get("auto_soup") instanceof AutoSoupModule autoSoup
                ? autoSoup : null;

        assertTrue(module != null);
        assertEquals(40.0, module.healthPercent().value());
        assertTrue(module.considerAbsorption().value());
        assertFalse(module.dropBowl().value());
        assertEquals(20.0, module.cooldownTicks().value());
    }

    @Test
    void autoHeadIsRegisteredWithPvPHealControls() {
        AutoHeadModule module = new ModuleManager().get("auto_head") instanceof AutoHeadModule autoHead
                ? autoHead : null;

        assertTrue(module != null);
        assertEquals(40.0, module.healthPercent().value());
        assertEquals(1.0, module.maxAbsorption().value());
        assertEquals(0.0, module.cooldownSeconds().value());
        assertTrue(module.considerAbsorption().value());
    }

    @Test
    void keepSprintIsRegisteredWithMotionAndChanceControls() {
        KeepSprintModule module = new ModuleManager().get("keep_sprint") instanceof KeepSprintModule keepSprint
                ? keepSprint : null;

        assertTrue(module != null);
        assertEquals(100.0, module.motionMin().value());
        assertEquals(100.0, module.motionMax().value());
        assertEquals(1.0, module.hurtTimeMin().value());
        assertEquals(10.0, module.hurtTimeMax().value());
        assertEquals(100.0, module.chance().value());
    }

    @Test
    void speedModuleIsRegisteredWithMovementSafetyControls() {
        SpeedModule module = new ModuleManager().get("speed") instanceof SpeedModule speed ? speed : null;

        assertTrue(module != null);
        assertFalse(module.requireSprint().value());
        assertTrue(module.respectItemUse().value());
        assertTrue(module.respectSneak().value());
        assertTrue(module.respectFallFlying().value());
        assertTrue(module.respectLiquids().value());
    }

    @Test
    void autoClickerExposesBoundedCpsAndAttackSafetySettings() {
        AutoClickerModule module = new ModuleManager().get("auto_clicker") instanceof AutoClickerModule autoClicker
                ? autoClicker : null;

        assertTrue(module != null);
        assertEquals(8.0, module.minimumClicksPerSecond().value());
        assertEquals(8.0, module.clicksPerSecond().value());
        assertEquals("Enemy", module.objective().value());
        assertTrue(module.objective().choices().containsAll(java.util.List.of("Enemy", "Entity", "Block", "Any")));
        assertTrue(module.requireAttackKey().value());
        assertTrue(module.respectAttackCooldown().value());
        assertTrue(module.pauseWhileUsingItem().value());
        assertTrue(module.pattern().choices().contains("Normal distribution"));
        assertFalse(module.rightClickEnabled().value());
        assertEquals(8.0, module.rightClickCps().value());
        assertTrue(module.rightClickRequiresInput().value());
        assertFalse(module.rightClickBlockOnly().value());
        assertTrue(module.ignoreSpecialUseItems().value());
        assertEquals("Any", module.weaponFilter().value());
        assertTrue(module.weaponFilter().choices().contains("Spear / Trident"));
        assertTrue(module.delayOnBlockBreak().value());
        assertEquals(0.0, module.delayPostUseTicks().value());
        assertEquals(20.0, module.delayPostUseTicks().maximum());
    }

    @Test
    void hitboxModuleIsRegisteredWithBoundedTargetMarginSettings() {
        HitboxModule module = new ModuleManager().get("hitbox") instanceof HitboxModule hitbox ? hitbox : null;

        assertTrue(module != null);
        assertEquals(0.1, module.size().value());
        assertEquals(0.0, module.size().minimum());
        assertEquals(1.0, module.size().maximum());
        assertTrue(module.playersOnly().value());
    }

    @Test
    void reachModuleIsRegisteredInThePlayerCategoryWithReferenceDefaults() {
        ReachModule module = new ModuleManager().get("reach") instanceof ReachModule reach ? reach : null;

        assertTrue(module != null);
        assertEquals(ModuleCategory.PLAYER, module.category());
        assertEquals(1.0, module.entityBonus().value());
        assertEquals(0.0, module.entityBonus().minimum());
        assertEquals(5.0, module.entityBonus().maximum());
        assertEquals(0.5, module.blockBonus().value());
        assertEquals(64.0, module.blockBonus().maximum());
    }
}
