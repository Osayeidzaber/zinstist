package dev.jilaton.client.gui;

import dev.jilaton.client.config.HudStyle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetCardPlacementTest {
    @Test void cardUsesRightSideWhenItFitsAndFlipsAtTheRightEdge() {
        TargetCardPlacement placement = new TargetCardPlacement();
        HudLayout right = placement.place("a", 100, 100, 184, 44, 800, 450, HudStyle.DEFAULT, 0, true);
        assertEquals(114, right.x());
        assertEquals(78, right.y());
        HudLayout left = placement.place("a", 750, 100, 184, 44, 800, 450, HudStyle.DEFAULT, 1, true);
        assertEquals(552, left.x());
        assertTrue(left.x() + left.width() <= 800);
    }

    @Test void resizeAndScaledTinyViewportsStayInsideBoundsAndIgnoreSavedAlignment() {
        TargetCardPlacement placement = new TargetCardPlacement();
        HudStyle style = new HudStyle(2, 0.7, null, HudStyle.Alignment.RIGHT, true);
        HudLayout normal = placement.place("a", 100, -40, 184, 44, 800, 450, style, 0, false);
        assertEquals(114, normal.x());
        assertEquals(0, normal.y());
        HudLayout tiny = placement.place("a", 70, 70, 184, 44, 120, 80, style, 100, false);
        assertEquals(120, tiny.width());
        assertEquals(0, tiny.x());
        assertTrue(tiny.y() + tiny.height() <= 80);
    }

    @Test void targetSwitchAndReducedMotionSnapWithoutTravellingFromPreviousOpponent() {
        TargetCardPlacement placement = new TargetCardPlacement();
        placement.place("a", 20, 80, 100, 40, 800, 450, HudStyle.DEFAULT, 0, false);
        HudLayout smoothed = placement.place("a", 300, 80, 100, 40, 800, 450, HudStyle.DEFAULT, 10_000_000, false);
        assertTrue(smoothed.x() > 34 && smoothed.x() < 314);
        HudLayout switched = placement.place("b", 300, 80, 100, 40, 800, 450, HudStyle.DEFAULT, 20_000_000, false);
        assertEquals(314, switched.x());
        HudLayout immediate = placement.place("b", 400, 80, 100, 40, 800, 450, HudStyle.DEFAULT, 30_000_000, true);
        assertEquals(414, immediate.x());
    }

    @Test void sideChoiceHasHysteresisAndInvalidAnchorsAreRejected() {
        TargetCardPlacement placement = new TargetCardPlacement();
        placement.place("a", 690, 100, 100, 40, 800, 450, HudStyle.DEFAULT, 0, true);
        HudLayout stillLeft = placement.place("a", 680, 100, 100, 40, 800, 450, HudStyle.DEFAULT, 1, true);
        assertEquals(566, stillLeft.x());
        HudLayout rightAgain = placement.place("a", 650, 100, 100, 40, 800, 450, HudStyle.DEFAULT, 2, true);
        assertEquals(664, rightAgain.x());
        assertThrows(IllegalArgumentException.class, () -> placement.place("a", Double.NaN, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 3, true));
    }
}
