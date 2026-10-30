package dev.jilaton.client.module.impl;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ZoomModuleTest {
    @TempDir Path directory;

    @Test
    void zoomRatioCannotWidenFovAndHandlesInvalidInputs() {
        assertEquals(1.0 / 3, ZoomModule.zoomFactor(30, 90), 1e-9);
        assertEquals(1, ZoomModule.zoomFactor(90, 30));
        assertEquals(1, ZoomModule.zoomFactor(30, 0));
        assertEquals(1, ZoomModule.zoomFactor(Double.NaN, 90));
        assertEquals(0.02, ZoomModule.zoomFactor(-20, 90));
    }

    @Test
    void zoomSettingsBindAndHoldModeRoundTrip() throws Exception {
        ModuleManager source = new ModuleManager();
        ZoomModule zoom = (ZoomModule) source.get("zoom");
        assertNotNull(zoom);
        assertTrue(zoom.holdKey());
        assertFalse(zoom.enabled());
        zoom.setKeyCode(86);
        zoom.settings().stream().filter(s -> s.id().equals("fov")).map(s -> (NumberSetting) s).findFirst().orElseThrow().setValue(18);
        zoom.settings().stream().filter(s -> s.id().equals("hold")).map(s -> (BooleanSetting) s).findFirst().orElseThrow().setValue(false);
        ClientConfig config = new ClientConfig(directory);
        config.setReducedMotion(true);
        config.saveProfile("zoom", source);
        ModuleManager restored = new ModuleManager();
        assertTrue(config.loadProfile("zoom", restored));
        ZoomModule loaded = (ZoomModule) restored.get("zoom");
        assertFalse(loaded.holdKey());
        assertEquals(86, loaded.keyCode());
        assertEquals(18, loaded.settings().stream().filter(s -> s.id().equals("fov")).map(s -> (NumberSetting) s).findFirst().orElseThrow().value());
        assertTrue(config.reducedMotion());
    }
}
