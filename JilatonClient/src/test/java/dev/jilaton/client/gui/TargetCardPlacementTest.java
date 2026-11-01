package dev.jilaton.client.gui;

import dev.jilaton.client.config.HudStyle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetCardPlacementTest {
    @Test void cardSitsLeftOfTheBodyAndFlipsRightAtTheLeftEdge() {
        TargetCardPlacement placement = new TargetCardPlacement();
        // Anchor 400, body 30 px either side: the card's right edge clears the body by the gap.
        HudLayout left = placement.place("a", 400, 100, 30, 184, 44, 800, 450, HudStyle.DEFAULT, 0, true);
        assertEquals(400 - 30 - 8 - 184, left.x());
        assertEquals(78, left.y());
        assertTrue(left.x() + left.width() <= 400 - 30, "card must not cover the target");
        HudLayout right = placement.place("a", 100, 100, 30, 184, 44, 800, 450, HudStyle.DEFAULT, 1, true);
        assertEquals(100 + 30 + 8, right.x());
    }

    @Test void aCloseTargetPushesTheCardFurtherOut() {
        TargetCardPlacement placement = new TargetCardPlacement();
        HudLayout far = placement.place("a", 600, 200, 6, 100, 40, 800, 450, HudStyle.DEFAULT, 0, true);
        HudLayout near = placement.place("a", 600, 200, 80, 100, 40, 800, 450, HudStyle.DEFAULT, 1, true);
        assertEquals(far.x() - 74, near.x());
    }

    @Test void resizeAndScaledTinyViewportsStayInsideBoundsAndIgnoreSavedAlignment() {
        TargetCardPlacement placement = new TargetCardPlacement();
        HudStyle style = new HudStyle(2, 0.7, null, HudStyle.Alignment.RIGHT, true);
        HudLayout normal = placement.place("a", 100, -40, 0, 184, 44, 800, 450, style, 0, false);
        assertEquals(108, normal.x());
        assertEquals(0, normal.y());
        HudLayout tiny = placement.place("a", 70, 70, 0, 184, 44, 120, 80, style, 100, false);
        assertEquals(120, tiny.width());
        assertEquals(0, tiny.x());
        assertTrue(tiny.y() + tiny.height() <= 80);
    }

    @Test void targetSwitchAndReducedMotionSnapWithoutTravellingFromPreviousOpponent() {
        TargetCardPlacement placement = new TargetCardPlacement();
        placement.place("a", 700, 80, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 0, false);
        HudLayout smoothed = placement.place("a", 400, 80, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 10_000_000, false);
        assertTrue(smoothed.x() > 292 && smoothed.x() < 592);
        HudLayout switched = placement.place("b", 400, 80, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 20_000_000, false);
        assertEquals(292, switched.x());
        HudLayout immediate = placement.place("b", 500, 80, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 30_000_000, true);
        assertEquals(392, immediate.x());
    }

    @Test void sideChoiceHasHysteresisAndInvalidAnchorsAreRejected() {
        TargetCardPlacement placement = new TargetCardPlacement();
        // Left would start at 110-8-100 = 2 < 4: flip right.
        assertEquals(118, placement.place("a", 110, 100, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 0, true).x());
        // Left would start at 12: inside the hysteresis band, stays right.
        assertEquals(128, placement.place("a", 120, 100, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 1, true).x());
        // Left would start at 32: back to the preferred left side.
        assertEquals(32, placement.place("a", 140, 100, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 2, true).x());
        assertThrows(IllegalArgumentException.class, () -> placement.place("a", Double.NaN, 0, 0, 100, 40, 800, 450, HudStyle.DEFAULT, 3, true));
    }
}
