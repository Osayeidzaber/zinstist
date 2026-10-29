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
    void roundTripsModuleSettingsAndKeyBindings() throws Exception {
        ModuleManager source = new ModuleManager();
        ClientModule aimAssist = source.get("aim_assist");
        NumberSetting range = setting(aimAssist, "range", NumberSetting.class);
        range.setValue(5.1);
        aimAssist.setKeyCode(65);

        ClientModule autoClicker = source.get("auto_clicker");
        BooleanSetting playersOnly = setting(autoClicker, "players_only", BooleanSetting.class);
        playersOnly.setValue(false);
        autoClicker.setKeyCode(67);

        ClientModule autoGapple = source.get("auto_gapple");
        setting(autoGapple, "health_threshold", NumberSetting.class).setValue(7.5);
        setting(autoGapple, "enchanted_apples", BooleanSetting.class).setValue(false);
        setting(source.get("hud"), "keystrokes", BooleanSetting.class).setValue(true);
        setting(source.get("swing_animations"), "mode", ChoiceSetting.class).setValue("Power");

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
        assertEquals(67, restored.get("auto_clicker").keyCode());
        assertEquals(7.5, setting(restored.get("auto_gapple"), "health_threshold", NumberSetting.class).value(), 0.000001);
        assertFalse(setting(restored.get("auto_gapple"), "enchanted_apples", BooleanSetting.class).value());
        assertTrue(setting(restored.get("hud"), "keystrokes", BooleanSetting.class).value());
        assertEquals("Power", setting(restored.get("swing_animations"), "mode", ChoiceSetting.class).value());
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

    private static <T extends Setting> T setting(ClientModule module, String id, Class<T> type) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing setting " + module.id() + "/" + id));
    }
}
