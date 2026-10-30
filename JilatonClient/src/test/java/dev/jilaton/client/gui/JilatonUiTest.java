package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JilatonUiTest {
    @Test
    void defaultPaletteIsMonochrome() {
        for (int color : new int[] {JilatonUi.INK, JilatonUi.SURFACE, JilatonUi.SURFACE_2, JilatonUi.RAISED,
                JilatonUi.HAIRLINE, JilatonUi.TEXT, JilatonUi.TEXT_2, JilatonUi.TEXT_3}) {
            int r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;
            assertTrue(r == g && g == b, "token must be neutral gray: " + Integer.toHexString(color));
        }
        int accent = ClientTheme.OBSIDIAN.accent();
        assertEquals(accent >> 16 & 0xFF, accent & 0xFF, "default accent must be neutral");
        assertEquals("obsidian", ClientTheme.fromId("unknown").id());
    }

    @Test
    void textOnAccentKeepsContrast() {
        assertEquals(0xFF050505, JilatonUi.onColor(ClientTheme.OBSIDIAN.accent()));
        assertEquals(JilatonUi.TEXT, JilatonUi.onColor(0xFF1A1A1A));
        assertEquals(JilatonUi.TEXT, JilatonUi.onColor(ClientTheme.VIOLET.accent()));
    }

    @Test
    void alphaAndMixInterpolateEveryChannel() {
        assertEquals(0x80FFFFFF, JilatonUi.alpha(0xFFFFFFFF, 128 / 255.0));
        assertEquals(0x00123456, JilatonUi.alpha(0xFF123456, -1));
        assertEquals(0x40123456, JilatonUi.alpha(0x80123456, 0.5));
        assertEquals(0xFF808080, JilatonUi.mix(0xFF000000, 0xFFFFFFFF, 0.5));
        assertEquals(0xFF000000, JilatonUi.mix(0xFF000000, 0xFFFFFFFF, -2));
        assertEquals(0xFFFFFFFF, JilatonUi.mix(0xFF000000, 0xFFFFFFFF, 3));
    }
}
