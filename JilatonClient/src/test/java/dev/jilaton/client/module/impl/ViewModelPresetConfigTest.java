package dev.jilaton.client.module.impl;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.render.model.ViewModelPreset;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewModelPresetConfigTest {
    @TempDir
    Path directory;

    @Test
    void selectingAPresetWritesItsTransformValues() {
        ViewModelModule module = new ViewModelModule();
        NumberSetting mainY = setting(module, "main_y", NumberSetting.class);
        NumberSetting mainX = setting(module, "main_x", NumberSetting.class);

        assertTrue(module.applyPreset("Low"));
        assertEquals(-0.30, mainY.value(), 1e-9);
        assertEquals(0, mainX.value(), 1e-9);
        assertEquals("Low", setting(module, "preset", ChoiceSetting.class).value());

        assertTrue(module.applyPreset("Close"));
        assertEquals(0, mainY.value(), 1e-9);
        assertEquals(1.15, setting(module, "main_scale", NumberSetting.class).value(), 1e-9);
        assertFalse(module.applyPreset("No such preset"));
    }

    @Test
    void loadingCustomTransformsDoesNotReapplyTheSelectedPreset() {
        ModuleManager modules = new ModuleManager();
        ViewModelModule original = (ViewModelModule) modules.get("view_model");
        original.applyPreset("Low");
        setting(original, "main_y", NumberSetting.class).setValue(-0.75);
        setting(original, "off_x", NumberSetting.class).setValue(0.45);
        new ClientConfig(directory).save(modules);

        ModuleManager reloaded = new ModuleManager();
        new ClientConfig(directory).load(reloaded);
        ViewModelModule restored = (ViewModelModule) reloaded.get("view_model");
        restored.onTick(null);
        assertEquals(-0.75, setting(restored, "main_y", NumberSetting.class).value(), 1e-9);
        assertEquals(0.45, setting(restored, "off_x", NumberSetting.class).value(), 1e-9);
        assertEquals("Low", setting(restored, "preset", ChoiceSetting.class).value());

        // Explicitly choosing a different preset must still apply it.
        setting(restored, "preset", ChoiceSetting.class).setValue("Close");
        restored.onTick(null);
        assertEquals(1.15, setting(restored, "main_scale", NumberSetting.class).value(), 1e-9);
    }

    @Test
    void switchingProfilesPreservesEditsToCustomPresets() throws Exception {
        ModuleManager modules = new ModuleManager();
        ViewModelModule module = (ViewModelModule) modules.get("view_model");
        module.savePreset("Blade");
        setting(module, "main_x", NumberSetting.class).setValue(0.8);
        ClientConfig config = new ClientConfig(directory);
        config.saveProfile("blade", modules);

        module.applyPreset("Wide");
        module.onTick(null);
        assertTrue(config.loadProfile("blade", modules));
        module.onTick(null);
        assertEquals("Blade", setting(module, "preset", ChoiceSetting.class).value());
        assertEquals(0.8, setting(module, "main_x", NumberSetting.class).value(), 1e-9);
    }

    @Test
    void aSavedPresetSurvivesTheConfigRoundTrip() throws Exception {
        ModuleManager modules = new ModuleManager();
        ViewModelModule viewModel = (ViewModelModule) modules.get("view_model");
        setting(viewModel, "main_x", NumberSetting.class).setValue(0.6);
        setting(viewModel, "main_rz", NumberSetting.class).setValue(-45);
        setting(viewModel, "off_z", NumberSetting.class).setValue(0.4);
        ViewModelPreset saved = viewModel.savePreset("My Blade");

        new ClientConfig(directory).save(modules);

        ModuleManager reloaded = new ModuleManager();
        new ClientConfig(directory).load(reloaded);
        ViewModelModule restored = (ViewModelModule) reloaded.get("view_model");
        // Loading resolves the preset choices without overwriting the saved current transforms.
        restored.onTick(null);

        ViewModelPreset found = ViewModelPreset.find(restored.presets(), "My Blade");
        assertNotNull(found, "the saved preset must come back from the config file");
        assertEquals(saved, found);
        assertEquals("My Blade", setting(restored, "preset", ChoiceSetting.class).value());
        assertEquals(0.6, setting(restored, "main_x", NumberSetting.class).value(), 1e-9);

        assertTrue(restored.applyPreset("My Blade"));
        assertEquals(-45, setting(restored, "main_rz", NumberSetting.class).value(), 1e-9);
        assertEquals(0.4, setting(restored, "off_z", NumberSetting.class).value(), 1e-9);
    }

    @Test
    void deletingAUserPresetFallsBackToTheShippedOne() {
        ViewModelModule module = new ViewModelModule();
        module.savePreset("Low");
        assertEquals(ViewModelModule.SHIPPED.size(), module.presets().size(),
                "a user preset must replace, not duplicate, the shipped one of the same name");
        assertEquals(1, module.presets().stream().filter(preset -> preset.key().equals("low")).count());

        setting(module, "main_y", NumberSetting.class).setValue(-1.5);
        assertTrue(module.deletePreset("Low"));
        ViewModelPreset shipped = ViewModelPreset.find(module.presets(), "Low");
        assertNotNull(shipped);
        assertEquals(-0.30, shipped.main().y(), 1e-9);
    }

    @Test
    void saveSelectedOverwritesThePresetInPlace() {
        ViewModelModule module = new ViewModelModule();
        setting(module, "off_x", NumberSetting.class).setValue(-0.5);
        ViewModelPreset captured = module.saveSelected();
        assertEquals("Default", captured.name());
        assertEquals(-0.5, ViewModelPreset.find(module.presets(), "Default").off().x(), 1e-9);
        assertEquals(ViewModelModule.SHIPPED.size(), module.presets().size());
    }

    private static <T extends Setting> T setting(ClientModule module, String id, Class<T> type) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing setting " + module.id() + "/" + id));
    }
}
