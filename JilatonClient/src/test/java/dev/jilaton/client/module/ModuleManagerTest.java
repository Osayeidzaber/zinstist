package dev.jilaton.client.module;

import dev.jilaton.client.module.impl.FullbrightModule;
import dev.jilaton.client.module.impl.CrystalAuraModule;
import dev.jilaton.client.module.impl.AutoClickerModule;
import dev.jilaton.client.module.impl.AutoAnchorModule;
import dev.jilaton.client.module.impl.AutoTotemModule;
import dev.jilaton.client.module.impl.AnchorAuraModule;
import dev.jilaton.client.module.impl.WTapModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.TargetStrafeModule;
import dev.jilaton.client.module.impl.HitboxModule;
import dev.jilaton.client.module.impl.KillAuraModule;
import dev.jilaton.client.module.impl.NoFallModule;
import dev.jilaton.client.module.impl.SafeWalkModule;
import dev.jilaton.client.module.impl.ReachModule;
import dev.jilaton.client.module.ModuleCategory;
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
    void noSlowHasIndependentVanillaAndSprintSettings() {
        ModuleManager manager = new ModuleManager();
        ClientModule noSlow = manager.get("no_slow");

        assertEquals(ModuleCategory.MOVEMENT, noSlow.category());
        assertFalse(noSlow.enabled());
        assertEquals(3, noSlow.settings().size());
        assertEquals(1.0, ((NumberSetting) noSlow.settings().stream()
                .filter(setting -> setting.id().equals("item_use_scale")).findFirst().orElseThrow()).value());
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
        assertTrue(module.requireAuraTarget().value());
        assertTrue(module.requireMovementInput().value());
    }

    @Test
    void autoClickerExposesBoundedCpsAndAttackSafetySettings() {
        AutoClickerModule module = new ModuleManager().get("auto_clicker") instanceof AutoClickerModule autoClicker
                ? autoClicker : null;

        assertTrue(module != null);
        assertEquals(8.0, module.minimumClicksPerSecond().value());
        assertEquals(8.0, module.clicksPerSecond().value());
        assertTrue(module.requireAttackKey().value());
        assertTrue(module.respectAttackCooldown().value());
        assertTrue(module.pauseWhileUsingItem().value());
        assertTrue(module.pattern().choices().contains("Normal distribution"));
        assertFalse(module.rightClickEnabled().value());
        assertEquals(8.0, module.rightClickCps().value());
        assertTrue(module.rightClickRequiresInput().value());
        assertFalse(module.rightClickBlockOnly().value());
        assertTrue(module.ignoreSpecialUseItems().value());
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
