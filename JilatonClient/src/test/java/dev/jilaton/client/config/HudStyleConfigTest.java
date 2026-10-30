package dev.jilaton.client.config;

import dev.jilaton.client.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class HudStyleConfigTest {
    @TempDir Path directory;

    @Test
    void appearanceAndHiddenStateRoundTripThroughConfigAndProfiles() throws Exception {
        ModuleManager modules = new ModuleManager();
        ClientConfig source = new ClientConfig(directory);
        HudStyle style = new HudStyle(1.7, 0.3, 0x12ABEF, HudStyle.Alignment.RIGHT, false);
        source.setHudStyle("target", style);
        source.setHudPosition("target", 0.2, 0.9);
        source.save(modules);
        source.saveProfile("layout", modules);
        ClientConfig restored = new ClientConfig(directory);
        restored.load(modules);
        assertEquals(style, restored.hudStyle("target"));
        restored.resetHudWidget("target");
        assertEquals(HudStyle.DEFAULT, restored.hudStyle("target"));
        assertNull(restored.hudPosition("target"));
        assertTrue(restored.loadProfile("layout", modules));
        assertEquals(style, restored.hudStyle("target"));
        assertEquals(new ClientConfig.HudPosition(0.2, 0.9), restored.hudPosition("target"));
    }

    @Test
    void legacyPositionsLoadWithDefaultAppearanceAndProfileResetsStyle() throws Exception {
        Files.writeString(directory.resolve("jilaton.properties"), "hud.position.target.x=0.2\nhud.position.target.y=0.3\n");
        ClientConfig config = new ClientConfig(directory);
        config.load(new ModuleManager());
        assertEquals(HudStyle.DEFAULT, config.hudStyle("target"));
        assertEquals(new ClientConfig.HudPosition(0.2, 0.3), config.hudPosition("target"));
        config.setHudStyle("target", new HudStyle(2, 0, 0, HudStyle.Alignment.LEFT, false));
        config.load(new ModuleManager());
        assertEquals(HudStyle.DEFAULT, config.hudStyle("target"));
    }

    @Test
    void corruptStyleDoesNotDiscardOtherWidgetsOrPosition() throws Exception {
        Files.writeString(directory.resolve("jilaton.properties"), "hud.style.target.scale=NaN\n"
                + "hud.position.target.x=0.2\nhud.position.target.y=0.3\nhud.style.watermark.scale=1.5\n"
                + "hud.style.coordinates.accent=-12345\nhud.style.performance.visible=perhaps\n");
        ClientConfig config = new ClientConfig(directory);
        config.load(new ModuleManager());
        assertEquals(HudStyle.DEFAULT, config.hudStyle("target"));
        assertEquals(1.5, config.hudStyle("watermark").scale());
        assertEquals(HudStyle.DEFAULT, config.hudStyle("coordinates"));
        assertEquals(HudStyle.DEFAULT, config.hudStyle("performance"));
        assertNotNull(config.hudPosition("target"));
    }

    @Test
    void rejectsNonFiniteAndInvalidColorsAndClampsEditableRanges() {
        assertThrows(IllegalArgumentException.class, () -> new HudStyle(Double.NaN, 0.5, null, HudStyle.Alignment.FREE, true));
        assertThrows(IllegalArgumentException.class, () -> new HudStyle(1, Double.POSITIVE_INFINITY, null, HudStyle.Alignment.FREE, true));
        assertThrows(IllegalArgumentException.class, () -> new HudStyle(1, 0.5, 0x1000000, HudStyle.Alignment.FREE, true));
        HudStyle style = new HudStyle(99, -4, 0, HudStyle.Alignment.FREE, true);
        assertEquals(2, style.scale());
        assertEquals(0, style.panelOpacity());
        assertEquals(0x00101118, style.panelColor());
        assertEquals(0xFF000000, style.accentColor(0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> new ClientConfig(directory).hudStyle("unknown"));
    }
}
