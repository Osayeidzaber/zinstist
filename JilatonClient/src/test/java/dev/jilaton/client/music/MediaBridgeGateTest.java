package dev.jilaton.client.music;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With the opt-in off the bridge is inert: it reports the opt-out, creates no script directory and never
 * touches the media session. Only the pure policy is exercised, no process is started.
 */
class MediaBridgeGateTest {
    @TempDir
    Path directory;

    @Test
    void theDefaultConstructionIsOffAndDoesNoIo() throws Exception {
        MediaSessionSource bridge = new MediaSessionSource(directory);
        try {
            assertFalse(bridge.enabled());
            assertEquals(MediaBridgePolicy.OFF_STATUS, bridge.state().status());
            bridge.tick();
            assertEquals(MediaBridgePolicy.OFF_STATUS, bridge.state().status());
            bridge.control("toggle");
            bridge.control("next");
            assertFalse(Files.exists(directory.resolve("jilaton")), "the bridge wrote into the config directory");
        } finally {
            bridge.shutdown();
        }
    }

    @Test
    void revokingTheOptInBeforeTheFirstTickPreventsTheScriptCopy() {
        MediaSessionSource bridge = new MediaSessionSource(directory, true);
        try {
            assertTrue(bridge.enabled());
            bridge.setEnabled(false);
            assertFalse(bridge.enabled());
            assertEquals(MediaBridgePolicy.OFF_STATUS, bridge.state().status());
            bridge.tick();
            assertEquals(MediaBridgePolicy.OFF_STATUS, bridge.state().status());
            assertFalse(Files.exists(directory.resolve("jilaton").resolve("media-session.ps1")));
            assertFalse(Files.exists(directory.resolve("jilaton").resolve("media-command.txt")));
        } finally {
            bridge.shutdown();
        }
    }
}
