package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyricTrailTest {
    private static final long S = 1_000_000_000L;

    @Test
    void aLineIsPlacedAlongTheGazeAndStaysWhenTheCameraTurns() {
        LyricTrail trail = new LyricTrail();
        LyricTrail.Entry first = trail.place("one", 0, 0, 64, 0, 0, 0, 1, 3, 0, 5);
        assertEquals(0, first.x, 1e-9);
        assertEquals(3, first.z, 1e-9);
        // The camera turns east for the next line: the first line does not move, the new one lands east.
        LyricTrail.Entry second = trail.place("two", 1, 0, 64, 0, 1, 0, 0, 3, S, 5);
        assertEquals(3, second.x, 1e-9);
        assertEquals(0, second.z, 1e-9);
        assertEquals(3, first.z, 1e-9);
        assertEquals(2, trail.entries().size());
    }

    @Test
    void textFacesBackTowardTheCameraThatPlacedIt() {
        // Looking south (+z) places text whose face points north, back at the viewer: yaw 180 by the layout convention.
        assertEquals(180, LyricTrail.facingYaw(0, 1), 1e-4);
        assertEquals(LyricTrail.facingYaw(0, 1) + 90, LyricTrail.facingYaw(-1, 0), 1e-4);
    }

    @Test
    void aWallInTheWayPullsTheLineInFrontOfItButNeverIntoTheEye() {
        assertEquals(3.0, LyricTrail.placementDistance(3.0, -1), 1e-9);
        assertEquals(2.0 - LyricTrail.WALL_GAP, LyricTrail.placementDistance(3.0, 2.0), 1e-9);
        assertEquals(LyricTrail.MIN_DISTANCE, LyricTrail.placementDistance(3.0, 0.4), 1e-9);
    }

    @Test
    void aLineBornOnTopOfAnotherLiftsTheOlderOne() {
        LyricTrail trail = new LyricTrail();
        LyricTrail.Entry older = trail.place("one", 0, 0, 64, 0, 0, 0, 1, 3, 0, 5);
        trail.place("two", 1, 0, 64, 0, 0, 0, 1, 3, S, 5);
        for (int i = 0; i < 120; i++) trail.step(1 / 60.0, S + i * S / 60, 14, 1);
        assertEquals(64 + LyricTrail.STACK_STEP, older.drawY(), 1e-3);
    }

    @Test
    void theActiveLineHoldsWhileOldLinesFadeAfterTheirLingerTime() {
        LyricTrail trail = new LyricTrail();
        LyricTrail.Entry first = trail.place("one", 0, 0, 64, 0, 0, 0, 1, 3, 0, 5);
        LyricTrail.Entry second = trail.place("two", 1, 0, 64, 0, 1, 0, 0, 3, S, 5);
        assertSame(second, trail.active(1));
        assertNull(trail.active(0));
        long late = 30 * S;
        assertEquals(0, LyricTrail.alpha(first, late, 14, false), 1e-9);
        assertEquals(1, LyricTrail.alpha(second, late, 14, true), 1e-9);
        trail.step(0.016, late, 14, 1);
        assertEquals(1, trail.entries().size());
        assertSame(second, trail.entries().get(0));
    }

    @Test
    void linesBeyondTheLimitFadeOutQuicklyInsteadOfPopping() {
        LyricTrail trail = new LyricTrail();
        LyricTrail.Entry oldest = trail.place("a", 0, 0, 64, 0, 0, 0, 1, 3, 0, 2);
        trail.place("b", 1, 0, 64, 0, 1, 0, 0, 3, S, 2);
        trail.place("c", 2, 0, 64, 0, -1, 0, 0, 3, 2 * S, 2);
        assertTrue(oldest.evicted());
        assertTrue(LyricTrail.alpha(oldest, 2 * S + S / 10, 14, false) > 0);
        assertEquals(0, LyricTrail.alpha(oldest, 3 * S, 14, false), 1e-9);
    }
}
