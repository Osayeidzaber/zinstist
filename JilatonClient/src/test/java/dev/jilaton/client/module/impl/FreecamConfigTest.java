package dev.jilaton.client.module.impl;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FreecamConfigTest {
    @TempDir Path directory;

    @Test void freecamSettingsPersistButActivationDoesNotResumeFromConfigOrProfiles() throws Exception {
        ModuleManager source = new ModuleManager();
        FreecamModule camera = (FreecamModule) source.get("freecam");
        assertFalse(camera.persistEnabled());
        camera.setKeyCode(86);
        camera.settings().stream().filter(s -> s.id().equals("speed")).map(s -> (NumberSetting) s).findFirst().orElseThrow().setValue(3);
        camera.setEnabled(true);
        ClientConfig config = new ClientConfig(directory);
        try {
            config.save(source);
            config.saveProfile("freecam", source);
        } finally { camera.setEnabled(false); }
        assertTrue(Files.readString(directory.resolve("jilaton.properties")).contains("module.freecam.enabled=false"));
        ModuleManager restored = new ModuleManager();
        assertTrue(config.loadProfile("freecam", restored));
        FreecamModule actual = (FreecamModule) restored.get("freecam");
        assertFalse(actual.enabled());
        assertEquals(86, actual.keyCode());
        assertEquals(3, actual.settings().stream().filter(s -> s.id().equals("speed")).map(s -> (NumberSetting) s).findFirst().orElseThrow().value(), 1e-9);
        // A hand-edited or imported true value must not bypass the nonpersistent lifecycle.
        Path file = directory.resolve("jilaton.properties");
        Files.writeString(file, Files.readString(file).replace("module.freecam.enabled=false", "module.freecam.enabled=true"));
        config.load(restored);
        assertFalse(actual.enabled());
    }
}
