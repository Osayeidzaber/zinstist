package dev.jilaton.client.gui;

import dev.jilaton.client.gui.shell.ShellMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-9 acceptance: a click on a module card's body opens its settings; only the dedicated switch changes the
 * module's state, and the grip only starts a reorder. Screen subclasses cannot be built headless, so the hit
 * rectangles the click handlers use are asserted directly against the rectangles the cards draw.
 */
class ClickGuiClickTargetTest {
    /** Same call the legacy screen makes: contentX + card.x(), listTop + card.y() - scroll. */
    private static final int X = 120, Y = 200, W = 280;
    private static final int H = ShellMetrics.MODULE_CARD_HEIGHT;

    /** A switch is 36x20 (ShellChrome.toggle); the visual card's sits 10 px higher. */
    private static final int SWITCH_W = 36, SWITCH_H = 20;

    @Test
    void theLegacySwitchIsTheOnlyTogglingRegionOfARegularCard() {
        int sx = X + W - 14 - SWITCH_W, sy = Y + 22;
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + 18, sy + 10, X, Y, W, false), "switch centre");
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + 0.5, sy + 0.5, X, Y, W, false), "top-left inside");
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + SWITCH_W - 0.5, sy + SWITCH_H - 0.5, X, Y, W, false),
                "bottom-right inside");
        assertFalse(ClickGuiScreen.cardSwitchHit(sx - 0.5, sy + 10, X, Y, W, false), "one pixel left of the switch");
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + SWITCH_W, sy + 10, X, Y, W, false), "one pixel right");
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + 18, sy - 0.5, X, Y, W, false), "one pixel above");
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + 18, sy + SWITCH_H, X, Y, W, false), "one pixel below");

        // Title ("Kill Aura" at y+18) and the keybind/description line (y+38) open settings instead.
        assertFalse(ClickGuiScreen.cardSwitchHit(X + 14, Y + 20, X, Y, W, false), "title");
        assertFalse(ClickGuiScreen.cardSwitchHit(X + 14, Y + 40, X, Y, W, false), "description");
        assertFalse(ClickGuiScreen.cardSwitchHit(X + W - 90, Y + 40, X, Y, W, false), "end of the description line");
    }

    @Test
    void theLegacyVisualCardSwitchSitsAboveThePreviewAndTogglesOnlyThere() {
        int sx = X + W - 14 - SWITCH_W, sy = Y + 12;
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + 18, sy + 10, X, Y, W, true));
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + 18, sy + SWITCH_H, X, Y, W, true));
        assertFalse(ClickGuiScreen.cardSwitchHit(X + 14, Y + 20, X, Y, W, true), "icon tile and name area");
        assertFalse(ClickGuiScreen.cardSwitchHit(X + W / 2, Y + 70, X, Y, W, true), "preview body");

        // Each renderer draws its switch at its own height: 12 on a visual card, 22 on a regular one.
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + 18, Y + 14, X, Y, W, true));
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + 18, Y + 14, X, Y, W, false), "visual row on a regular card");
        assertTrue(ClickGuiScreen.cardSwitchHit(sx + 18, Y + 36, X, Y, W, false));
        assertFalse(ClickGuiScreen.cardSwitchHit(sx + 18, Y + 36, X, Y, W, true), "regular row on a visual card");
    }

    @Test
    void theGripOnlyStartsAReorderAndCoversTheGripThatIsDrawn() {
        int gearX = X + W - 14 - SWITCH_W - 20;
        // ShellChrome.grip paints six 2x2 dots from (gearX - 16, y + 12): every dot must be grabbable.
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                assertTrue(ClickGuiScreen.cardGripHit(gearX - 16 + column * 4, Y + 12 + row * 4, X, Y, W, false),
                        "grip dot " + column + "," + row + " must start a drag");
            }
        }
        assertFalse(ClickGuiScreen.cardSwitchHit(gearX - 16, Y + 12, X, Y, W, false), "the grip never toggles");
        assertFalse(ClickGuiScreen.cardGearHit(gearX - 16, Y + 12, X, Y, W), "the grip is not the gear");

        // The gear (10x10 at gearX, y + 13) opens settings and never toggles the module.
        for (int dx = 0; dx <= 10; dx++) {
            assertTrue(ClickGuiScreen.cardGearHit(gearX + dx, Y + 18, X, Y, W), "gear column " + dx);
            assertFalse(ClickGuiScreen.cardSwitchHit(gearX + dx, Y + 18, X, Y, W, false));
        }
        assertFalse(ClickGuiScreen.cardGripHit(gearX + 5, Y + 18, X, Y, W, false), "the gear is not the grip");
    }

    @Test
    void theVisualGripAndSwitchStayDisjoint() {
        // Visual grip: 22x20 at (x + w - 32, y + 40). Switch: 36x20 at (x + w - 50, y + 12).
        float gx = X + W - 32, gy = Y + 40;
        assertTrue(ClickGuiScreen.cardGripHit(gx + 11, gy + 10, X, Y, W, true));
        assertFalse(ClickGuiScreen.cardSwitchHit(gx + 11, gy + 10, X, Y, W, true));
        assertFalse(ClickGuiScreen.cardGripHit(X + W - 14 - SWITCH_W + 18, Y + 22, X, Y, W, true),
                "the switch is not the grip");
    }

    @Test
    void theSwitchStaysInsideTheRightColumnOfTheCard() {
        int titleEnd = X + W - 14 - SWITCH_W - 40;
        for (float px = X; px < X + W; px += 0.5F) {
            for (float py = Y; py < Y + H; py += 0.5F) {
                if (!ClickGuiScreen.cardSwitchHit(px, py, X, Y, W, false)) continue;
                assertTrue(px >= X && px < X + W && py >= Y && py < Y + H, "switch must stay inside the card");
                assertTrue(px >= X + W - 50, "switch must sit in the card's right column");
                assertFalse(px < titleEnd && py < Y + 48, "switch must not cover the title or description text");
                assertFalse(ClickGuiScreen.cardGripHit(px, py, X, Y, W, false), "switch must not be the grip");
            }
        }
    }
}
