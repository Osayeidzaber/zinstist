package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The desktop media bridge is a persisted, explicit opt-in and starts disabled. */
class MediaPrivacyConfigTest {
    @TempDir
    Path directory;

    @Test
    void aFreshConfigLeavesTheDesktopBridgeOff() {
        assertFalse(new ClientConfig(directory).desktopMediaBridgeEnabled());
        ClientConfig loaded = new ClientConfig(directory);
        loaded.load(new ModuleManager());
        assertFalse(loaded.desktopMediaBridgeEnabled(), "a missing property must never enable the bridge");
    }

    @Test
    void theOptInRoundTripsAndIsStoredExplicitly() throws Exception {
        ClientConfig config = new ClientConfig(directory);
        config.setDesktopMediaBridgeEnabled(true);
        config.save(new ModuleManager());

        String saved = Files.readString(directory.resolve("jilaton.properties"));
        assertTrue(saved.contains("music.desktop_bridge=true"), "the opt-in must be visible in the config file");

        ClientConfig restored = new ClientConfig(directory);
        restored.load(new ModuleManager());
        assertTrue(restored.desktopMediaBridgeEnabled());

        restored.setDesktopMediaBridgeEnabled(false);
        restored.save(new ModuleManager());
        assertFalse(restored.desktopMediaBridgeEnabled());
        assertTrue(Files.readString(directory.resolve("jilaton.properties")).contains("music.desktop_bridge=false"));
    }

    @Test
    void aCorruptValueFallsBackToOff() throws Exception {
        Files.writeString(directory.resolve("jilaton.properties"),
                "music.desktop_bridge=sometimes\n");
        ClientConfig config = new ClientConfig(directory);
        config.load(new ModuleManager());
        assertFalse(config.desktopMediaBridgeEnabled());
    }
}
