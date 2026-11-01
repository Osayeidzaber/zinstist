package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTransferTest {
    @TempDir
    Path dir;

    private static NumberSetting reach(ModuleManager modules) {
        for (Setting s : modules.get("breadcrumbs").settings()) if (s instanceof NumberSetting n && s.id().equals("length")) return n;
        throw new AssertionError("breadcrumbs length setting missing");
    }

    @Test
    void exportedTextImportsAsAProfileThatRestoresSettings() throws Exception {
        ModuleManager source = new ModuleManager();
        ClientConfig config = new ClientConfig(dir.resolve("a"));
        reach(source).setValue(250);
        String text = config.exportText(source);

        ClientConfig other = new ClientConfig(dir.resolve("b"));
        assertTrue(other.importText("shared", text));
        ModuleManager target = new ModuleManager();
        assertTrue(other.loadProfile("shared", target));
        assertEquals(250, reach(target).value(), 1e-9);
    }

    @Test
    void randomClipboardTextIsRejected() throws Exception {
        ClientConfig config = new ClientConfig(dir);
        assertFalse(config.importText("x", "hello world"));
        assertFalse(config.importText("x", ""));
        assertTrue(config.listProfiles().isEmpty());
    }

    @Test
    void resetPutsEverySettingBackButKeepsTheModuleState() {
        ModuleManager modules = new ModuleManager();
        var module = modules.get("breadcrumbs");
        reach(modules).setValue(300);
        module.setEnabled(true);
        assertFalse(reach(modules).isDefault());
        module.resetSettings();
        assertTrue(reach(modules).isDefault());
        assertEquals(1000, reach(modules).value(), 1e-9);
        assertTrue(module.enabled());
    }
}
