package dev.jilaton.client.config;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void animationIntensityAndMotionPreferencesRoundTripAndClamp() {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(temporaryDirectory);
        assertEquals(1.0, config.animationIntensity(), 1e-9);
        assertEquals("F2F2F2", config.customAccent());
        config.setAnimationIntensity(0.62);
        config.setReducedMotion(true);
        config.save(modules);

        ClientConfig restored = new ClientConfig(temporaryDirectory);
        restored.load(new ModuleManager());
        assertEquals(0.6, restored.animationIntensity(), 1e-9);
        assertTrue(restored.reducedMotion());

        restored.setAnimationIntensity(9);
        assertEquals(1.5, restored.animationIntensity(), 1e-9);
        restored.setAnimationIntensity(-1);
        assertEquals(0, restored.animationIntensity(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> restored.setAnimationIntensity(Double.NaN));
    }

    @Test
    void guiBackdropRoundTripsAndClamps() {
        ClientConfig config = new ClientConfig(temporaryDirectory);
        assertEquals(0.55, config.backdropOpacity(), 1e-9);
        assertTrue(config.backdropBlur());
        config.setBackdropOpacity(0.72);
        config.setBackdropBlur(false);
        config.save(new ModuleManager());

        ClientConfig restored = new ClientConfig(temporaryDirectory);
        restored.load(new ModuleManager());
        assertEquals(0.7, restored.backdropOpacity(), 1e-9);
        assertFalse(restored.backdropBlur());

        restored.setBackdropOpacity(5);
        assertEquals(0.95, restored.backdropOpacity(), 1e-9);
        restored.setBackdropOpacity(0);
        assertEquals(0.2, restored.backdropOpacity(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> restored.setBackdropOpacity(Double.NaN));
    }

    @Test
    void missingConfigStartsModulesWithTheirDeclaredDefaults() {
        ModuleManager modules = new ModuleManager();

        new ClientConfig(temporaryDirectory).load(modules);

        assertTrue(modules.get("friends").enabled());
        assertFalse(modules.get("kill_aura").enabled());
    }

    @Test
    void roundTripsModuleSettingsAndKeyBindings() throws Exception {
        ModuleManager source = new ModuleManager();
        ClientModule aimAssist = source.get("aim_assist");
        NumberSetting range = setting(aimAssist, "range", NumberSetting.class);
        range.setValue(5.1);
        aimAssist.setKeyCode(65);

        ClientModule autoClicker = source.get("auto_clicker");
        setting(autoClicker, "objective", ChoiceSetting.class).setValue("Any");
        BooleanSetting playersOnly = setting(autoClicker, "players_only", BooleanSetting.class);
        playersOnly.setValue(false);
        setting(autoClicker, "right_click_enabled", BooleanSetting.class).setValue(true);
        setting(autoClicker, "right_click_cps", NumberSetting.class).setValue(12.0);
        setting(autoClicker, "right_click_block_only", BooleanSetting.class).setValue(true);
        setting(autoClicker, "pattern", ChoiceSetting.class).setValue("Butterfly");
        setting(autoClicker, "weapon_filter", ChoiceSetting.class).setValue("Mace");
        setting(autoClicker, "delay_on_broken", BooleanSetting.class).setValue(false);
        setting(autoClicker, "delay_post_use", NumberSetting.class).setValue(6.0);
        autoClicker.setKeyCode(67);

        ClientModule autoGapple = source.get("auto_gapple");
        setting(autoGapple, "health_threshold", NumberSetting.class).setValue(7.5);
        setting(autoGapple, "enchanted_apples", BooleanSetting.class).setValue(false);
        setting(autoGapple, "consider_absorption", BooleanSetting.class).setValue(false);
        setting(source.get("auto_pot"), "speed", BooleanSetting.class).setValue(false);
        setting(source.get("auto_pot"), "water_breathing", BooleanSetting.class).setValue(true);
        setting(source.get("auto_pot"), "refresh_window", NumberSetting.class).setValue(12.0);
        setting(source.get("no_fall"), "use_safe_distance", BooleanSetting.class).setValue(false);
        setting(source.get("no_fall"), "fall_distance", NumberSetting.class).setValue(4.5);
        setting(source.get("no_fall"), "reset_fall_distance", BooleanSetting.class).setValue(false);
        setting(source.get("safe_walk"), "edge_distance", NumberSetting.class).setValue(0.45);
        setting(source.get("safe_walk"), "keep_ticks", NumberSetting.class).setValue(3.0);
        setting(source.get("no_slow"), "item_use_scale", NumberSetting.class).setValue(0.65);
        setting(source.get("no_slow"), "consume_scale", NumberSetting.class).setValue(0.4);
        setting(source.get("no_slow"), "block_scale", NumberSetting.class).setValue(1.6);
        setting(source.get("inventory_move"), "behavior", ChoiceSetting.class).setValue("Normal");
        setting(source.get("inventory_move"), "passthrough_sneak", BooleanSetting.class).setValue(true);
        setting(source.get("bow_aim"), "range", NumberSetting.class).setValue(48.0);
        setting(source.get("bow_aim"), "high_arc", BooleanSetting.class).setValue(true);
        setting(source.get("bow_aim"), "auto_release", BooleanSetting.class).setValue(true);
        setting(source.get("bow_aim"), "precharged_crossbow", BooleanSetting.class).setValue(true);
        setting(source.get("bow_aim"), "trident_release_ticks", NumberSetting.class).setValue(17.0);
        setting(source.get("strafe"), "strength_in_air", NumberSetting.class).setValue(0.65);
        setting(source.get("high_jump"), "motion", NumberSetting.class).setValue(1.15);
        setting(source.get("long_jump"), "horizontal_boost", NumberSetting.class).setValue(5.0);
        setting(source.get("anti_bot"), "ignore_duplicate_profiles", BooleanSetting.class).setValue(true);
        setting(source.get("anti_bot"), "ignore_illegal_pitch", BooleanSetting.class).setValue(false);
        setting(source.get("anti_bot"), "ignore_suspicious_entity_ids", BooleanSetting.class).setValue(false);
        setting(source.get("bow_aim"), "release_ticks", NumberSetting.class).setValue(17.0);
        setting(source.get("bow_aim"), "shot_delay", NumberSetting.class).setValue(0.5);
        setting(source.get("bow_aim"), "aim_threshold", NumberSetting.class).setValue(1.2);
        setting(source.get("auto_buff_refill"), "potions", BooleanSetting.class).setValue(false);
        setting(source.get("auto_buff_refill"), "delay_ticks", NumberSetting.class).setValue(3.0);
        setting(source.get("auto_replenish"), "mode", ChoiceSetting.class).setValue("Legit");
        setting(source.get("auto_replenish"), "threshold", NumberSetting.class).setValue(12.0);
        setting(source.get("auto_replenish"), "offhand", BooleanSetting.class).setValue(false);
        setting(source.get("auto_weapon"), "preference", ChoiceSetting.class).setValue("Axe");
        setting(source.get("auto_weapon"), "mace_smash", BooleanSetting.class).setValue(false);
        setting(source.get("auto_weapon"), "switch_back_delay", NumberSetting.class).setValue(4.0);
        setting(source.get("no_miss_cooldown"), "remove_attack_cooldown", BooleanSetting.class).setValue(false);
        setting(source.get("no_miss_cooldown"), "cancel_attack_on_miss", BooleanSetting.class).setValue(true);
        setting(source.get("hud"), "keystrokes", BooleanSetting.class).setValue(true);
        setting(source.get("swing_animations"), "mode", ChoiceSetting.class).setValue("Power");
        setting(source.get("kill_aura"), "players", BooleanSetting.class).setValue(false);
        setting(source.get("kill_aura"), "hostiles", BooleanSetting.class).setValue(true);
        setting(source.get("kill_aura"), "priority", ChoiceSetting.class).setValue("Health");
        setting(source.get("kill_aura"), "avoid_wasted_shield_attacks", BooleanSetting.class).setValue(false);
        setting(source.get("kill_aura"), "raycast_mode", ChoiceSetting.class).setValue("All");
        setting(source.get("auto_totem"), "use_hotbar_totems", BooleanSetting.class).setValue(true);
        setting(source.get("auto_totem"), "refill_delay", NumberSetting.class).setValue(7.0);
        setting(source.get("crystal_aura"), "face_place", BooleanSetting.class).setValue(false);
        setting(source.get("crystal_aura"), "face_place_health", NumberSetting.class).setValue(13.5);
        setting(source.get("crystal_aura"), "face_place_damage", NumberSetting.class).setValue(3.0);
        setting(source.get("auto_rod"), "minimum_range", NumberSetting.class).setValue(4.2);
        setting(source.get("auto_rod"), "auto_switch", BooleanSetting.class).setValue(false);
        setting(source.get("velocity"), "horizontal", NumberSetting.class).setValue(-35.0);
        setting(source.get("velocity"), "response_mode", ChoiceSetting.class).setValue("Reversal");
        setting(source.get("velocity"), "strafe_delay", NumberSetting.class).setValue(3.0);
        setting(source.get("velocity"), "jump_reset", BooleanSetting.class).setValue(true);
        setting(source.get("target_strafe"), "mode", ChoiceSetting.class).setValue("Motion");
        setting(source.get("target_strafe"), "collision_check", BooleanSetting.class).setValue(false);
        setting(source.get("target_strafe"), "edge_check", BooleanSetting.class).setValue(false);
        setting(source.get("auto_soup"), "health_percent", NumberSetting.class).setValue(35.0);
        setting(source.get("auto_soup"), "consider_absorption", BooleanSetting.class).setValue(false);
        setting(source.get("auto_soup"), "drop_bowl", BooleanSetting.class).setValue(true);
        setting(source.get("auto_soup"), "cooldown_ticks", NumberSetting.class).setValue(35.0);
        setting(source.get("auto_head"), "health_percent", NumberSetting.class).setValue(30.0);
        setting(source.get("auto_head"), "max_absorption", NumberSetting.class).setValue(2.5);
        setting(source.get("auto_head"), "cooldown_seconds", NumberSetting.class).setValue(1.5);
        setting(source.get("auto_head"), "consider_absorption", BooleanSetting.class).setValue(false);
        setting(source.get("auto_eat"), "hunger_threshold", NumberSetting.class).setValue(12.0);
        setting(source.get("auto_eat"), "health_threshold", NumberSetting.class).setValue(7.5);
        setting(source.get("auto_eat"), "use_offhand", BooleanSetting.class).setValue(false);
        setting(source.get("fast_fall"), "downward_speed", NumberSetting.class).setValue(1.35);
        setting(source.get("fast_fall"), "require_sneak", BooleanSetting.class).setValue(false);
        setting(source.get("keep_sprint"), "motion_min", NumberSetting.class).setValue(72.0);
        setting(source.get("keep_sprint"), "motion_max", NumberSetting.class).setValue(88.0);
        setting(source.get("keep_sprint"), "motion_when_hurt_min", NumberSetting.class).setValue(45.0);
        setting(source.get("keep_sprint"), "motion_when_hurt_max", NumberSetting.class).setValue(65.0);
        setting(source.get("keep_sprint"), "hurt_time_min", NumberSetting.class).setValue(3.0);
        setting(source.get("keep_sprint"), "hurt_time_max", NumberSetting.class).setValue(8.0);
        setting(source.get("keep_sprint"), "chance", NumberSetting.class).setValue(80.0);
        setting(source.get("speed"), "require_sprint", BooleanSetting.class).setValue(true);
        setting(source.get("speed"), "respect_liquids", BooleanSetting.class).setValue(false);
        setting(source.get("no_jump_delay"), "cooldown", NumberSetting.class).setValue(4.0);
        setting(source.get("no_jump_delay"), "variance", NumberSetting.class).setValue(2.0);
        setting(source.get("step"), "height", NumberSetting.class).setValue(1.8);
        setting(source.get("flight"), "speed", NumberSetting.class).setValue(2.5);
        setting(source.get("flight"), "max_velocity", NumberSetting.class).setValue(7.5);
        setting(source.get("flight"), "force_flight", BooleanSetting.class).setValue(false);
        setting(source.get("flight"), "bypass_vanilla_check", BooleanSetting.class).setValue(false);
        setting(source.get("velocity"), "jump_cooldown_mode", ChoiceSetting.class).setValue("Received hits");
        setting(source.get("velocity"), "jump_cooldown", NumberSetting.class).setValue(4.0);
        setting(source.get("velocity"), "reverse_delay", NumberSetting.class).setValue(3.0);
        setting(source.get("velocity"), "reverse_z", NumberSetting.class).setValue(75.0);
        setting(source.get("velocity"), "reverse_only_moving", BooleanSetting.class).setValue(true);
        setting(source.get("velocity"), "strafe_only_facing", BooleanSetting.class).setValue(true);
        setting(source.get("velocity"), "strafe_facing_range", NumberSetting.class).setValue(4.2);
        setting(source.get("auto_leave"), "health_threshold", NumberSetting.class).setValue(7.0);
        setting(source.get("auto_leave"), "minimum_delay", NumberSetting.class).setValue(2.0);
        setting(source.get("auto_leave"), "maximum_delay", NumberSetting.class).setValue(5.0);
        setting(source.get("elytra_target"), "range", NumberSetting.class).setValue(80.0);
        setting(source.get("elytra_target"), "predict_movement", BooleanSetting.class).setValue(false);
        setting(source.get("elytra_target"), "auto_firework", BooleanSetting.class).setValue(false);
        setting(source.get("auto_armor"), "save_armor", BooleanSetting.class).setValue(false);
        setting(source.get("auto_armor"), "durability_threshold", NumberSetting.class).setValue(32.0);
        setting(source.get("kill_aura"), "minimum_cps", NumberSetting.class).setValue(4.0);
        setting(source.get("kill_aura"), "click_pattern", ChoiceSetting.class).setValue("Steady");
        setting(source.get("auto_shoot"), "click_pattern", ChoiceSetting.class).setValue("Butterfly");
        setting(source.get("kill_aura"), "respect_attack_cooldown", BooleanSetting.class).setValue(false);
        setting(source.get("criticals"), "mode", ChoiceSetting.class).setValue("Wait");
        setting(source.get("aim_assist"), "angle_factor", NumberSetting.class).setValue(0.6);
        setting(source.get("aim_assist"), "horizontal", BooleanSetting.class).setValue(false);
        setting(source.get("teams"), "match_name_color", BooleanSetting.class).setValue(false);
        setting(source.get("teams"), "armor_head", BooleanSetting.class).setValue(false);
        setting(source.get("teams"), "armor_chest", BooleanSetting.class).setValue(true);

        ClientConfig config = new ClientConfig(temporaryDirectory);
        config.setThemeId("blue");
        config.setCustomAccent("12aBef");
        config.setHudPosition("watermark", 0.37, 0.68);
        config.setHudPosition("performance", 1.4, -0.2);
        config.save(source);
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("jilaton.properties")));

        ModuleManager restored = new ModuleManager();
        ClientConfig restoredConfig = new ClientConfig(temporaryDirectory);
        restoredConfig.load(restored);

        assertEquals(5.1, setting(restored.get("aim_assist"), "range", NumberSetting.class).value(), 0.000001);
        assertEquals(65, restored.get("aim_assist").keyCode());
        assertFalse(setting(restored.get("auto_clicker"), "players_only", BooleanSetting.class).value());
        assertTrue(setting(restored.get("auto_clicker"), "right_click_enabled", BooleanSetting.class).value());
        assertEquals(12.0, setting(restored.get("auto_clicker"), "right_click_cps", NumberSetting.class).value());
        assertTrue(setting(restored.get("auto_clicker"), "right_click_block_only", BooleanSetting.class).value());
        assertEquals("Mace", setting(restored.get("auto_clicker"), "weapon_filter", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("auto_clicker"), "delay_on_broken", BooleanSetting.class).value());
        assertEquals(6.0, setting(restored.get("auto_clicker"), "delay_post_use", NumberSetting.class).value());
        assertFalse(setting(restored.get("teams"), "match_name_color", BooleanSetting.class).value());
        assertFalse(setting(restored.get("teams"), "armor_head", BooleanSetting.class).value());
        assertTrue(setting(restored.get("teams"), "armor_chest", BooleanSetting.class).value());
        assertEquals(67, restored.get("auto_clicker").keyCode());
        assertEquals("Any", setting(restored.get("auto_clicker"), "objective", ChoiceSetting.class).value());
        assertEquals(7.5, setting(restored.get("auto_gapple"), "health_threshold", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("auto_gapple"), "enchanted_apples", BooleanSetting.class).value());
        assertFalse(setting(restored.get("auto_gapple"), "consider_absorption", BooleanSetting.class).value());
        assertFalse(setting(restored.get("auto_pot"), "speed", BooleanSetting.class).value());
        assertTrue(setting(restored.get("auto_pot"), "water_breathing", BooleanSetting.class).value());
        assertEquals(12.0, setting(restored.get("auto_pot"), "refresh_window", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("no_fall"), "use_safe_distance", BooleanSetting.class).value());
        assertEquals(4.5, setting(restored.get("no_fall"), "fall_distance", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("no_fall"), "reset_fall_distance", BooleanSetting.class).value());
        assertEquals(0.45, setting(restored.get("safe_walk"), "edge_distance", NumberSetting.class).value(), 0.000001);
        assertEquals(3.0, setting(restored.get("safe_walk"), "keep_ticks", NumberSetting.class).value(), 0.000001);
        assertEquals(0.65, setting(restored.get("no_slow"), "item_use_scale", NumberSetting.class).value(), 0.000001);
        assertEquals(0.4, setting(restored.get("no_slow"), "consume_scale", NumberSetting.class).value(), 0.000001);
        assertEquals(1.6, setting(restored.get("no_slow"), "block_scale", NumberSetting.class).value(), 0.000001);
        assertEquals("Normal", setting(restored.get("inventory_move"), "behavior", ChoiceSetting.class).value());
        assertTrue(setting(restored.get("inventory_move"), "passthrough_sneak", BooleanSetting.class).value());
        assertEquals(48.0, setting(restored.get("bow_aim"), "range", NumberSetting.class).value(), 0.000001);
        assertTrue(setting(restored.get("bow_aim"), "high_arc", BooleanSetting.class).value());
        assertTrue(setting(restored.get("bow_aim"), "auto_release", BooleanSetting.class).value());
        assertTrue(setting(restored.get("bow_aim"), "precharged_crossbow", BooleanSetting.class).value());
        assertEquals(17.0, setting(restored.get("bow_aim"), "trident_release_ticks", NumberSetting.class).value(), 0.000001);
        assertEquals(0.65, setting(restored.get("strafe"), "strength_in_air", NumberSetting.class).value(), 0.000001);
        assertEquals(1.15, setting(restored.get("high_jump"), "motion", NumberSetting.class).value(), 0.000001);
        assertEquals(5.0, setting(restored.get("long_jump"), "horizontal_boost", NumberSetting.class).value(), 0.000001);
        assertTrue(setting(restored.get("anti_bot"), "ignore_duplicate_profiles", BooleanSetting.class).value());
        assertFalse(setting(restored.get("anti_bot"), "ignore_illegal_pitch", BooleanSetting.class).value());
        assertFalse(setting(restored.get("anti_bot"), "ignore_suspicious_entity_ids", BooleanSetting.class).value());
        assertEquals(17.0, setting(restored.get("bow_aim"), "release_ticks", NumberSetting.class).value(), 0.000001);
        assertEquals(0.5, setting(restored.get("bow_aim"), "shot_delay", NumberSetting.class).value(), 0.000001);
        assertEquals(1.2, setting(restored.get("bow_aim"), "aim_threshold", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("auto_buff_refill"), "potions", BooleanSetting.class).value());
        assertEquals(3.0, setting(restored.get("auto_buff_refill"), "delay_ticks", NumberSetting.class).value(), 0.000001);
        assertEquals("Legit", setting(restored.get("auto_replenish"), "mode", ChoiceSetting.class).value());
        assertEquals(12.0, setting(restored.get("auto_replenish"), "threshold", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("auto_replenish"), "offhand", BooleanSetting.class).value());
        assertEquals("Axe", setting(restored.get("auto_weapon"), "preference", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("auto_weapon"), "mace_smash", BooleanSetting.class).value());
        assertEquals(4.0, setting(restored.get("auto_weapon"), "switch_back_delay", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("no_miss_cooldown"), "remove_attack_cooldown", BooleanSetting.class).value());
        assertTrue(setting(restored.get("no_miss_cooldown"), "cancel_attack_on_miss", BooleanSetting.class).value());
        assertTrue(setting(restored.get("hud"), "keystrokes", BooleanSetting.class).value());
        assertEquals("Power", setting(restored.get("swing_animations"), "mode", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("kill_aura"), "players", BooleanSetting.class).value());
        assertTrue(setting(restored.get("kill_aura"), "hostiles", BooleanSetting.class).value());
        assertEquals("Health", setting(restored.get("kill_aura"), "priority", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("kill_aura"), "avoid_wasted_shield_attacks", BooleanSetting.class).value());
        assertEquals("All", setting(restored.get("kill_aura"), "raycast_mode", ChoiceSetting.class).value());
        assertTrue(setting(restored.get("auto_totem"), "use_hotbar_totems", BooleanSetting.class).value());
        assertEquals(7.0, setting(restored.get("auto_totem"), "refill_delay", NumberSetting.class).value());
        assertFalse(setting(restored.get("crystal_aura"), "face_place", BooleanSetting.class).value());
        assertEquals(13.5, setting(restored.get("crystal_aura"), "face_place_health", NumberSetting.class).value());
        assertEquals(3.0, setting(restored.get("crystal_aura"), "face_place_damage", NumberSetting.class).value());
        assertEquals(4.2, setting(restored.get("auto_rod"), "minimum_range", NumberSetting.class).value());
        assertFalse(setting(restored.get("auto_rod"), "auto_switch", BooleanSetting.class).value());
        assertEquals(-35.0, setting(restored.get("velocity"), "horizontal", NumberSetting.class).value());
        assertEquals("Reversal", setting(restored.get("velocity"), "response_mode", ChoiceSetting.class).value());
        assertEquals(3.0, setting(restored.get("velocity"), "strafe_delay", NumberSetting.class).value());
        assertTrue(setting(restored.get("velocity"), "jump_reset", BooleanSetting.class).value());
        assertEquals("Motion", setting(restored.get("target_strafe"), "mode", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("target_strafe"), "collision_check", BooleanSetting.class).value());
        assertFalse(setting(restored.get("target_strafe"), "edge_check", BooleanSetting.class).value());
        assertEquals(35.0, setting(restored.get("auto_soup"), "health_percent", NumberSetting.class).value());
        assertFalse(setting(restored.get("auto_soup"), "consider_absorption", BooleanSetting.class).value());
        assertTrue(setting(restored.get("auto_soup"), "drop_bowl", BooleanSetting.class).value());
        assertEquals(35.0, setting(restored.get("auto_soup"), "cooldown_ticks", NumberSetting.class).value());
        assertEquals(30.0, setting(restored.get("auto_head"), "health_percent", NumberSetting.class).value());
        assertEquals(2.5, setting(restored.get("auto_head"), "max_absorption", NumberSetting.class).value());
        assertEquals(1.5, setting(restored.get("auto_head"), "cooldown_seconds", NumberSetting.class).value());
        assertFalse(setting(restored.get("auto_head"), "consider_absorption", BooleanSetting.class).value());
        assertEquals(12.0, setting(restored.get("auto_eat"), "hunger_threshold", NumberSetting.class).value());
        assertEquals(7.5, setting(restored.get("auto_eat"), "health_threshold", NumberSetting.class).value());
        assertFalse(setting(restored.get("auto_eat"), "use_offhand", BooleanSetting.class).value());
        assertEquals(1.35, setting(restored.get("fast_fall"), "downward_speed", NumberSetting.class).value());
        assertFalse(setting(restored.get("fast_fall"), "require_sneak", BooleanSetting.class).value());
        assertEquals(72.0, setting(restored.get("keep_sprint"), "motion_min", NumberSetting.class).value());
        assertEquals(88.0, setting(restored.get("keep_sprint"), "motion_max", NumberSetting.class).value());
        assertEquals(45.0, setting(restored.get("keep_sprint"), "motion_when_hurt_min", NumberSetting.class).value());
        assertEquals(65.0, setting(restored.get("keep_sprint"), "motion_when_hurt_max", NumberSetting.class).value());
        assertEquals(3.0, setting(restored.get("keep_sprint"), "hurt_time_min", NumberSetting.class).value());
        assertEquals(8.0, setting(restored.get("keep_sprint"), "hurt_time_max", NumberSetting.class).value());
        assertEquals(80.0, setting(restored.get("keep_sprint"), "chance", NumberSetting.class).value());
        assertTrue(setting(restored.get("speed"), "require_sprint", BooleanSetting.class).value());
        assertFalse(setting(restored.get("speed"), "respect_liquids", BooleanSetting.class).value());
        assertEquals(4.0, setting(restored.get("no_jump_delay"), "cooldown", NumberSetting.class).value());
        assertEquals(2.0, setting(restored.get("no_jump_delay"), "variance", NumberSetting.class).value());
        assertEquals(1.8, setting(restored.get("step"), "height", NumberSetting.class).value(), 0.000001);
        assertEquals(2.5, setting(restored.get("flight"), "speed", NumberSetting.class).value(), 0.000001);
        assertEquals(7.5, setting(restored.get("flight"), "max_velocity", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("flight"), "force_flight", BooleanSetting.class).value());
        assertFalse(setting(restored.get("flight"), "bypass_vanilla_check", BooleanSetting.class).value());
        assertEquals("Received hits", setting(restored.get("velocity"), "jump_cooldown_mode", ChoiceSetting.class).value());
        assertEquals(4.0, setting(restored.get("velocity"), "jump_cooldown", NumberSetting.class).value());
        assertEquals(3.0, setting(restored.get("velocity"), "reverse_delay", NumberSetting.class).value());
        assertEquals(75.0, setting(restored.get("velocity"), "reverse_z", NumberSetting.class).value());
        assertTrue(setting(restored.get("velocity"), "reverse_only_moving", BooleanSetting.class).value());
        assertTrue(setting(restored.get("velocity"), "strafe_only_facing", BooleanSetting.class).value());
        assertEquals(4.2, setting(restored.get("velocity"), "strafe_facing_range", NumberSetting.class).value());
        assertEquals(7.0, setting(restored.get("auto_leave"), "health_threshold", NumberSetting.class).value());
        assertEquals(2.0, setting(restored.get("auto_leave"), "minimum_delay", NumberSetting.class).value());
        assertEquals(5.0, setting(restored.get("auto_leave"), "maximum_delay", NumberSetting.class).value());
        assertEquals(80.0, setting(restored.get("elytra_target"), "range", NumberSetting.class).value());
        assertFalse(setting(restored.get("elytra_target"), "predict_movement", BooleanSetting.class).value());
        assertFalse(setting(restored.get("elytra_target"), "auto_firework", BooleanSetting.class).value());
        assertFalse(setting(restored.get("auto_armor"), "save_armor", BooleanSetting.class).value());
        assertEquals(32.0, setting(restored.get("auto_armor"), "durability_threshold", NumberSetting.class).value());
        assertEquals(4.0, setting(restored.get("kill_aura"), "minimum_cps", NumberSetting.class).value());
        assertEquals("Steady", setting(restored.get("kill_aura"), "click_pattern", ChoiceSetting.class).value());
        assertEquals("Butterfly", setting(restored.get("auto_shoot"), "click_pattern", ChoiceSetting.class).value());
        assertFalse(setting(restored.get("kill_aura"), "respect_attack_cooldown", BooleanSetting.class).value());
        assertEquals("Butterfly", setting(restored.get("auto_clicker"), "pattern", ChoiceSetting.class).value());
        assertEquals("Wait", setting(restored.get("criticals"), "mode", ChoiceSetting.class).value());
        assertEquals(0.6, setting(restored.get("aim_assist"), "angle_factor", NumberSetting.class).value(), 1.0e-9);
        assertFalse(setting(restored.get("aim_assist"), "horizontal", BooleanSetting.class).value());
        assertEquals("blue", restoredConfig.themeId());
        assertEquals("12ABEF", restoredConfig.customAccent());
        assertEquals(new ClientConfig.HudPosition(0.37, 0.68), restoredConfig.hudPosition("watermark"));
        assertEquals(new ClientConfig.HudPosition(1.0, 0.0), restoredConfig.hudPosition("performance"));
    }

    @Test
    void numberSettingClampsAndQuantizesValues() {
        NumberSetting setting = new NumberSetting("amount", "Amount", 0.0, 1.0, 0.25, 0.0);
        setting.setValue(0.9);
        assertEquals(1.0, setting.value(), 0.000001);
        setting.setValue(-4.0);
        assertEquals(0.0, setting.value(), 0.000001);
    }

    @Test
    void savesListsLoadsAndDeletesNamedProfiles() throws Exception {
        ClientConfig config = new ClientConfig(temporaryDirectory);
        ModuleManager source = new ModuleManager();
        config.setThemeId("rose");
        config.setCustomAccent("E070A8");
        config.setHudPosition("coordinates", 0.25, 0.8);
        setting(source.get("aim_assist"), "range", NumberSetting.class).setValue(5.4);
        source.get("auto_clicker").setKeyCode(67);
        config.saveProfile("ranked_1", source);

        assertEquals(java.util.List.of("ranked_1"), config.listProfiles());
        ModuleManager restored = new ModuleManager();
        ClientConfig restoredConfig = new ClientConfig(temporaryDirectory);
        assertTrue(restoredConfig.loadProfile("ranked_1", restored));
        assertEquals(5.4, setting(restored.get("aim_assist"), "range", NumberSetting.class).value(), 0.000001);
        assertEquals(67, restored.get("auto_clicker").keyCode());
        assertEquals("rose", restoredConfig.themeId());
        assertEquals("E070A8", restoredConfig.customAccent());
        assertEquals(new ClientConfig.HudPosition(0.25, 0.8), restoredConfig.hudPosition("coordinates"));
        assertFalse(config.loadProfile("missing", restored));
        assertTrue(config.deleteProfile("ranked_1"));
        assertFalse(config.deleteProfile("ranked_1"));
        assertTrue(config.listProfiles().isEmpty());
    }

    @Test
    void rejectsProfileNamesThatCouldEscapeTheProfileDirectory() {
        ClientConfig config = new ClientConfig(temporaryDirectory);
        assertThrows(IllegalArgumentException.class, () -> config.saveProfile("../outside", new ModuleManager()));
        assertThrows(IllegalArgumentException.class, () -> config.loadProfile("C:\\outside", new ModuleManager()));
        assertThrows(IllegalArgumentException.class, () -> config.deleteProfile(""));
        assertThrows(IllegalArgumentException.class, () -> config.setThemeId("../theme"));
        assertThrows(IllegalArgumentException.class, () -> config.setCustomAccent("GGGGGG"));
    }

    @Test
    void persistsClickGuiNavigationStateAndModuleOrderingAcrossRestarts() {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(temporaryDirectory);
        config.setGuiPage("MODULES");
        config.setGuiCategory("Visuals");
        config.setGuiSelectedModule("target_esp");
        config.setGuiExpandedModule("target_esp");
        config.setGuiEnabledOnly(true);
        config.setGuiSearchQuery("esp");
        config.setGuiSelectedProfile("ranked_1");
        config.setGuiScroll(142.5);
        config.setCategoryScroll("Visuals", 142.5);
        config.setCategoryScroll("Combat", 68.0);
        config.setVisualOrder(java.util.List.of("target_esp", "custom_sky", "player_effects"));
        config.setModuleOrder(java.util.List.of("kill_aura", "target_esp", "sprint", "custom_sky"));
        config.save(modules);

        ClientConfig restored = new ClientConfig(temporaryDirectory);
        restored.load(new ModuleManager());
        assertEquals("MODULES", restored.guiPage());
        assertEquals("Visuals", restored.guiCategory());
        assertEquals("target_esp", restored.guiSelectedModule());
        assertEquals("target_esp", restored.guiExpandedModule());
        assertTrue(restored.guiEnabledOnly());
        assertEquals("esp", restored.guiSearchQuery());
        assertEquals("ranked_1", restored.guiSelectedProfile());
        assertEquals(142.5, restored.guiScroll(), 1e-6);
        assertEquals(142.5, restored.categoryScroll("Visuals"), 1e-6);
        assertEquals(68.0, restored.categoryScroll("Combat"), 1e-6);
        assertEquals(java.util.List.of("target_esp", "custom_sky", "player_effects"), restored.visualOrder());
        assertEquals(java.util.List.of("kill_aura", "target_esp", "sprint", "custom_sky"), restored.moduleOrder());
    }

    private static <T extends Setting> T setting(ClientModule module, String id, Class<T> type) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing setting " + module.id() + "/" + id));
    }
}
