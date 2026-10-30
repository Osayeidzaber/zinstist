package dev.jilaton.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void curvesHitEndpointsAndStayMonotonic() {
        for (Motion.Easing easing : new Motion.Easing[] {Motion.SETTLE, Motion.STANDARD, Motion.LINEAR}) {
            assertEquals(0, easing.apply(0), 1e-9);
            assertEquals(1, easing.apply(1), 1e-9);
            assertEquals(0, easing.apply(-3), 1e-9);
            assertEquals(1, easing.apply(4), 1e-9);
            double previous = 0;
            for (int i = 1; i <= 200; i++) {
                double value = easing.apply(i / 200.0);
                assertTrue(value >= previous - 1e-9, "curve must not reverse");
                previous = value;
            }
        }
    }

    @Test
    void settleRespondsFasterThanStandard() {
        // The settle curve is most of the way there early: responsive, not floaty.
        assertTrue(Motion.SETTLE.apply(0.25) > 0.7);
        // Known midpoint of cubic-bezier(0.4, 0, 0.2, 1).
        assertEquals(0.7756, Motion.STANDARD.apply(0.5), 0.002);
        assertTrue(Motion.SETTLE.apply(0.2) > Motion.STANDARD.apply(0.2));
        assertEquals(Motion.bezier(0.16, 1, 0.3, 1, 0.4), Motion.SETTLE.apply(0.4), 1e-12);
    }

    @Test
    void transitionIsFrameRateIndependentAndReversible() {
        Motion.Transition coarse = new Motion.Transition(0.3, Motion.LINEAR);
        Motion.Transition fine = new Motion.Transition(0.3, Motion.LINEAR);
        coarse.update(false, 0, false);
        fine.update(false, 0, false);
        coarse.update(true, SECOND / 10, false);
        for (int i = 1; i <= 10; i++) fine.update(true, SECOND / 100 * i, false);
        assertEquals(coarse.raw(), fine.raw(), 1e-9);
        assertEquals(1 / 3.0, coarse.raw(), 1e-9);

        // Reversing mid-way continues from the current position instead of jumping.
        double before = coarse.raw();
        coarse.update(false, SECOND / 10 + SECOND / 100, false);
        assertTrue(coarse.raw() < before && coarse.raw() > before - 0.05);
        assertFalse(coarse.settled(false));
    }

    @Test
    void transitionClampsLongFramesAndHonorsImmediate() {
        Motion.Transition transition = new Motion.Transition(0.2, Motion.LINEAR);
        transition.update(false, 0, false);
        transition.update(true, 5 * SECOND, false);
        assertEquals(0.5, transition.raw(), 1e-9, "a stalled frame advances at most 100 ms");
        assertEquals(1, transition.update(true, 6 * SECOND, true), 1e-9);
        assertTrue(transition.settled(true));
        assertThrows(IllegalArgumentException.class, () -> new Motion.Transition(0, Motion.LINEAR));
    }

    @Test
    void staggeredEntranceWaitsForItsDelay() {
        assertEquals(0, Motion.staggered(0, SECOND / 10, 0.2, 0.5, Motion.LINEAR), 1e-9);
        assertEquals(0.5, Motion.staggered(0, SECOND / 2 - SECOND / 20, 0.2, 0.5, Motion.LINEAR), 1e-6);
        assertEquals(1, Motion.staggered(0, 2 * SECOND, 0.2, 0.5, Motion.LINEAR), 1e-9);
    }

    @Test
    void intensityScalesOffsetsAndReducedMotionRemovesThem() {
        assertEquals(10, Motion.amount(10, 1, false), 1e-9);
        assertEquals(15, Motion.amount(10, 9, false), 1e-9);
        assertEquals(0, Motion.amount(10, -1, false), 1e-9);
        assertEquals(0, Motion.amount(10, 1, true), 1e-9);
    }
}
