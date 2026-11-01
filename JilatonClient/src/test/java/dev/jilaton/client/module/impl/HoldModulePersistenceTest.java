package dev.jilaton.client.module.impl;

import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FreeLook and Zoom are momentary hold-key modules. Restoring them as enabled after a restart would
 * leave the client with the camera detached (or the FOV zoomed) while nobody holds the key, so their
 * activation must never survive a save/load cycle. Toggle modules keep the old behaviour.
 */
class HoldModulePersistenceTest {
    @TempDir
    Path directory;

    @Test
    void holdModulesRefusePersistenceWhileToggleModulesStillPersist() {
        assertFalse(new FreeLookModule().persistEnabled());
        assertFalse(new ZoomModule().persistEnabled());
        assertTrue(new SprintModule().persistEnabled());
        assertTrue(new FullbrightModule().persistEnabled());
    }

    @Test
    void enabledHoldModulesDoNotComeBackAfterARestart() throws Exception {
        ModuleManager source = new ModuleManager();
        FreeLookModule freeLook = (FreeLookModule) source.get("free_look");
        ZoomModule zoom = (ZoomModule) source.get("zoom");

        try {
            freeLook.setEnabled(true);
            zoom.setEnabled(true);
            ClientConfig config = new ClientConfig(directory);
            config.save(source);
        } finally {
            freeLook.setEnabled(false);
            zoom.setEnabled(false);
        }

        String saved = Files.readString(directory.resolve("jilaton.properties"));
        assertTrue(saved.contains("module.free_look.enabled=false"), saved);
        assertTrue(saved.contains("module.zoom.enabled=false"), saved);

        ModuleManager restored = new ModuleManager();
        new ClientConfig(directory).load(restored);
        assertFalse(restored.get("free_look").enabled());
        assertFalse(restored.get("zoom").enabled());

        // A hand-edited or imported true value must not bypass the hold-key lifecycle either.
        Path file = directory.resolve("jilaton.properties");
        Files.writeString(file, saved
                .replace("module.free_look.enabled=false", "module.free_look.enabled=true")
                .replace("module.zoom.enabled=false", "module.zoom.enabled=true"));
        new ClientConfig(directory).load(restored);
        assertFalse(restored.get("free_look").enabled());
        assertFalse(restored.get("zoom").enabled());
    }
}
