package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The per-line appear curve and which neighbouring lines are allowed to animate on a line change. */
class SpatialLyricsTypographyTest {
    private static final long APPEAR_NANOS = 350_000_000L;

    @Test
    void appearStartsAtZeroAndSettlesAfter350Ms() {
        assertEquals(0.0, SpatialLyricsModule.appearEase(false, 0), 1e-9);
        assertEquals(0.875, SpatialLyricsModule.appearEase(false, APPEAR_NANOS / 2), 1e-9);
        assertEquals(1.0, SpatialLyricsModule.appearEase(false, APPEAR_NANOS), 1e-9);
        assertEquals(1.0, SpatialLyricsModule.appearEase(false, APPEAR_NANOS * 10), 1e-9);
        assertEquals(0.0, SpatialLyricsModule.appearEase(false, -50_000_000L), 1e-9);
    }

    @Test
    void appearIsAnEaseOutNotLinear() {
        double previous = -1;
        for (int step = 0; step <= 14; step++) {
            double value = SpatialLyricsModule.appearEase(false, APPEAR_NANOS * step / 14);
            assertTrue(value >= previous, "monotonic at step " + step);
            previous = value;
        }
        assertTrue(SpatialLyricsModule.appearEase(false, APPEAR_NANOS / 2) > 0.5,
                "cubic ease-out is past halfway at half time");
    }

    @Test
    void reducedMotionSkipsTheRamp() {
        assertEquals(1.0, SpatialLyricsModule.appearEase(true, 0), 1e-9);
        assertEquals(1.0, SpatialLyricsModule.appearEase(true, APPEAR_NANOS / 4), 1e-9);
    }

    @Test
    void steadyNeighboursDoNotRefadeOnEveryLineChange() {
        // Three visible lines: band is -1..1. A line that was already at +1 and stays at +1 keeps full alpha.
        assertFalse(SpatialLyricsModule.fadesIn(1, 1, false));
        assertFalse(SpatialLyricsModule.fadesIn(-1, 1, false));
        // A line arriving from outside the band fades in, and the first frame of a fresh stack fades all of it.
        assertTrue(SpatialLyricsModule.fadesIn(2, 1, false));
        assertTrue(SpatialLyricsModule.fadesIn(-2, 1, false));
        assertTrue(SpatialLyricsModule.fadesIn(0, 1, true));
        // Single-line mode still animates the neighbour that slides into the only slot.
        assertTrue(SpatialLyricsModule.fadesIn(1, 0, false));
    }
}
