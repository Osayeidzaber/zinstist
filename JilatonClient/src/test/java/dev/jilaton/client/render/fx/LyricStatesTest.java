package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyricStatesTest {
    @Test
    void aFreshLineWalksThroughEveryStateOnSchedule() {
        assertEquals(LyricStates.State.INCOMING, LyricStates.state(0.05, false, 10, -1));
        assertEquals(LyricStates.State.SETTLING, LyricStates.state(0.20, false, 10, -1));
        assertEquals(LyricStates.State.SETTLING, LyricStates.state(0.20, true, 10, -1), "still settling");
        assertEquals(LyricStates.State.ACTIVE, LyricStates.state(0.40, true, 10, -1));
        assertEquals(LyricStates.State.TRANSITIONING, LyricStates.state(2.0, false, 10, -1));
        assertEquals(LyricStates.State.FADING, LyricStates.state(10.5, false, 10, -1));
        assertEquals(LyricStates.State.EXPIRED, LyricStates.state(12.0, false, 10, -1));
    }

    @Test
    void theSungLineNeverExpiresAndKeepsFullVisibility() {
        assertEquals(LyricStates.State.ACTIVE, LyricStates.state(3600, true, 14, -1));
        assertEquals(1, LyricStates.alpha(3600, true, 14, -1), 1e-9);
    }

    @Test
    void visibilityEasesInFromTheFirstFrameInsteadOfFlashingBlank() {
        double early = LyricStates.alpha(0.06, false, 10, -1);
        double later = LyricStates.alpha(0.30, false, 10, -1);
        assertTrue(early > 0, "an incoming line is visible, not blank");
        assertTrue(early < later && later < 1, "the ramp rises monotonically");
    }

    @Test
    void fadingIsMonotonicAndEndsAtZero() {
        double start = LyricStates.alpha(10.0, false, 10, -1);
        double middle = LyricStates.alpha(10.7, false, 10, -1);
        assertTrue(start > middle && middle > 0);
        assertEquals(0, LyricStates.alpha(11.4, false, 10, -1), 1e-9);
    }

    @Test
    void evictionFadesFasterThanTheNaturalLinger() {
        assertEquals(LyricStates.State.FADING, LyricStates.state(0.01, false, 10, 0.1));
        assertTrue(LyricStates.alpha(0.01, false, 10, 0.1) < 0.8);
        assertEquals(LyricStates.State.EXPIRED, LyricStates.state(0.01, false, 10, LyricStates.EVICT_SECONDS));
        assertEquals(0, LyricStates.alpha(0.01, false, 10, 1.0), 1e-9);
    }

    @Test
    void appearRisesPastHalfwayQuicklyAndHoldsAtOne() {
        assertEquals(0, LyricStates.appear(0), 1e-9);
        assertTrue(LyricStates.appear(0.19) > 0.5);
        assertEquals(1, LyricStates.appear(LyricStates.SETTLE_SECONDS), 1e-9);
        assertEquals(1, LyricStates.appear(5), 1e-9);
    }
}
