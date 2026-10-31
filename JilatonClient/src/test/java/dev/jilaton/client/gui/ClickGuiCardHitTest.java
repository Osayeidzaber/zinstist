package dev.jilaton.client.gui;

import dev.jilaton.client.gui.design.Ui;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The switch is the only part of a module card that may toggle the module. */
class ClickGuiCardHitTest {
    private static final float X = 100, Y = 50, W = 300;

    @Test
    void regularCardTogglesOnlyOnTheSwitch() {
        // Switch: 40x22 at (x + w - 56, y + 20).
        float sx = X + W - 16 - Ui.TOGGLE_W, sy = Y + 20;
        assertTrue(JilatonClickGui.toggleHit(sx + 20, sy + 11, X, Y, W, false));
        assertTrue(JilatonClickGui.toggleHit(sx + 0.5, sy + 0.5, X, Y, W, false));

        // Exclusive edges: the pixel one step past each side belongs to the card, not the switch.
        assertFalse(JilatonClickGui.toggleHit(sx - 1, sy + 11, X, Y, W, false));
        assertFalse(JilatonClickGui.toggleHit(sx + Ui.TOGGLE_W, sy + 11, X, Y, W, false));
        assertFalse(JilatonClickGui.toggleHit(sx + 20, sy - 1, X, Y, W, false));
        assertFalse(JilatonClickGui.toggleHit(sx + 20, sy + Ui.TOGGLE_H, X, Y, W, false));

        // The title and description area opens settings instead of toggling.
        assertFalse(JilatonClickGui.toggleHit(X + 16, Y + 15, X, Y, W, false));
        assertFalse(JilatonClickGui.toggleHit(X + 16, Y + 35, X, Y, W, false));
    }

    @Test
    void visualCardTogglesOnlyOnTheSmallSwitch() {
        // Switch: 32x18 at (x + w - 46, y + 12).
        float sx = X + W - 14 - Ui.SMALL_TOGGLE_W, sy = Y + 12;
        assertTrue(JilatonClickGui.toggleHit(sx + 16, sy + 9, X, Y, W, true));
        assertFalse(JilatonClickGui.toggleHit(sx - 1, sy + 9, X, Y, W, true));
        assertFalse(JilatonClickGui.toggleHit(sx + Ui.SMALL_TOGGLE_W, sy + 9, X, Y, W, true));
        assertFalse(JilatonClickGui.toggleHit(X + 14, Y + 13, X, Y, W, true));

        // The two switch rectangles are not nested: the wide one is inset 16 and starts at y+20,
        // the small one is inset 14 and starts at y+12, so points exist on exactly one of them.
        assertTrue(JilatonClickGui.toggleHit(X + W - 15, Y + 15, X, Y, W, true));
        assertFalse(JilatonClickGui.toggleHit(X + W - 15, Y + 15, X, Y, W, false));
        assertTrue(JilatonClickGui.toggleHit(X + W - 55, Y + 25, X, Y, W, false));
        assertFalse(JilatonClickGui.toggleHit(X + W - 55, Y + 25, X, Y, W, true));
    }

    @Test
    void switchesStayInsideTheirCards() {
        assertTrue(Ui.TOGGLE_W > 0 && Ui.TOGGLE_H > 0);
        assertTrue(Ui.SMALL_TOGGLE_W > 0 && Ui.SMALL_TOGGLE_H > 0);
        assertTrue(X + W - 16 - Ui.TOGGLE_W >= X);
        assertTrue(Y + 20 + Ui.TOGGLE_H <= Y + 62);
        assertTrue(X + W - 14 - Ui.SMALL_TOGGLE_W >= X);
        // A visual card is 128 tall; the preview starts at y + 52 and must not cover the switch.
        assertTrue(Y + 12 + Ui.SMALL_TOGGLE_H <= Y + 52);
    }
}
