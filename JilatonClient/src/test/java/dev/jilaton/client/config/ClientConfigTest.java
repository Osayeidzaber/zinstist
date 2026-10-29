package dev.jilaton.client.config;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
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

        ClientConfig config = new ClientConfig(temporaryDirectory);
        config.save(source);
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("jilaton.properties")));

        ModuleManager restored = new ModuleManager();
        config.load(restored);

        assertEquals(5.1, setting(restored.get("aim_assist"), "range", NumberSetting.class).value(), 0.000001);
        assertEquals(65, restored.get("aim_assist").keyCode());
        assertFalse(setting(restored.get("auto_clicker"), "players_only", BooleanSetting.class).value());
        assertEquals(67, restored.get("auto_clicker").keyCode());
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
        setting(source.get("aim_assist"), "range", NumberSetting.class).setValue(5.4);
        source.get("auto_clicker").setKeyCode(67);
        config.saveProfile("ranked_1", source);

        assertEquals(java.util.List.of("ranked_1"), config.listProfiles());
        ModuleManager restored = new ModuleManager();
        assertTrue(config.loadProfile("ranked_1", restored));
        assertEquals(5.4, setting(restored.get("aim_assist"), "range", NumberSetting.class).value(), 0.000001);
        assertEquals(67, restored.get("auto_clicker").keyCode());
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
    }

    private static <T extends Setting> T setting(ClientModule module, String id, Class<T> type) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing setting " + module.id() + "/" + id));
    }
}
