package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test
    void scaledWidgetAtSavedBottomRightStaysInsideViewport() {
        HudLayout layout = HudLayout.resolve(100, 20, 427, 240, 8, 8, new HudPosition(1, 1),
                new HudStyle(1.5, 0.5, null, HudStyle.Alignment.FREE, true));
        assertEquals(277, layout.x());
        assertEquals(210, layout.y());
        assertEquals(150, layout.width());
        assertEquals(30, layout.height());
    }

    @Test
    void oversizedContentFitsTinyViewportWithoutClipping() {
        HudLayout layout = HudLayout.resolve(210, 900, 160, 120, -100, 500, null, HudStyle.DEFAULT);
        assertTrue(layout.width() <= 160);
        assertTrue(layout.height() <= 120);
        assertTrue(layout.x() >= 0 && layout.x() + layout.width() <= 160);
        assertTrue(layout.y() >= 0 && layout.y() + layout.height() <= 120);
    }

    @Test
    void alignmentOverridesSavedHorizontalPositionAndSurvivesResize() {
        for (int viewport : new int[]{240, 427, 1920}) {
            HudLayout right = HudLayout.resolve(100, 20, viewport, 240, 0, 8, new HudPosition(0, 0.5),
                    new HudStyle(1, 0.5, null, HudStyle.Alignment.RIGHT, true));
            assertEquals(viewport - 108, right.x());
            HudLayout center = HudLayout.resolve(100, 20, viewport, 240, 0, 8, null,
                    new HudStyle(1, 0.5, null, HudStyle.Alignment.CENTER, true));
            assertEquals((viewport - 100) / 2, center.x());
        }
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> HudLayout.resolve(0, 20, 100, 100, 0, 0, null, HudStyle.DEFAULT));
    }
}
