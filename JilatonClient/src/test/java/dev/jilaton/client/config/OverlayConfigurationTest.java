package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class OverlayConfigurationTest {
    @TempDir Path directory;

    @Test void haloAndItemFilteringSettingsUseTheExistingConfigurationAndProfileSystem() throws Exception {
        ModuleManager modules = new ModuleManager();
        ClientConfig config = new ClientConfig(directory);
        assertEquals("violet", config.themeId());
        SettingValueEditor.apply(setting(modules,"item_esp","item_ids"),"diamond, apple");
        SettingValueEditor.apply(setting(modules,"item_esp","filter"),"whitelist");
        SettingValueEditor.apply(setting(modules,"player_esp","hostiles"),"true");
        SettingValueEditor.apply(setting(modules,"kill_aura","highlight_style"),"Halo + corners");
        SettingValueEditor.apply(setting(modules,"kill_aura","highlight_opacity"),"0.6");
        config.saveProfile("visuals",modules);
        ModuleManager restored = new ModuleManager();
        assertTrue(config.loadProfile("visuals",restored));
        assertEquals("minecraft:diamond, minecraft:apple",((TextSetting)setting(restored,"item_esp","item_ids")).value());
        assertEquals("Whitelist",((ChoiceSetting)setting(restored,"item_esp","filter")).value());
        assertTrue(((BooleanSetting)setting(restored,"player_esp","hostiles")).value());
        assertEquals("Halo + corners",((ChoiceSetting)setting(restored,"kill_aura","highlight_style")).value());
        assertEquals(0.6,((NumberSetting)setting(restored,"kill_aura","highlight_opacity")).value(),0.001);
    }

    @Test void invalidCommandValuesDoNotOverwriteTheLastValidSetting() {
        ModuleManager modules = new ModuleManager();
        TextSetting text = (TextSetting)setting(modules,"item_esp","item_ids");
        text.setValue("diamond");
        assertThrows(IllegalArgumentException.class,()->text.setValue("not:a:valid:id"));
        assertEquals("minecraft:diamond",text.value());
        Setting flag = setting(modules,"player_esp","hostiles");
        assertThrows(IllegalArgumentException.class,()->SettingValueEditor.apply(flag,"maybe"));
        Setting number = setting(modules,"kill_aura","highlight_opacity");
        assertThrows(IllegalArgumentException.class,()->SettingValueEditor.apply(number,"NaN"));
        assertThrows(IllegalArgumentException.class,()->SettingValueEditor.apply(number,"Infinity"));
        assertEquals(0.85,((NumberSetting)number).value(),0.001);
    }

    private Setting setting(ModuleManager modules,String module,String id) {
        return modules.get(module).settings().stream().filter(setting->setting.id().equals(id)).findFirst().orElseThrow();
    }
}
