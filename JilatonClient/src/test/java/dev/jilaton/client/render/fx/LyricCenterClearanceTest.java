package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Music text must never sit on the crosshair, and must give a fight more room. */
class LyricCenterClearanceTest {
    private static double at(double degrees, boolean combat) {
        double r = Math.toRadians(degrees);
        return LyricLayout.centerClearance(Math.sin(r), 0, Math.cos(r), 0, 0, 1, combat);
    }

    @Test
    void textOnTheCrosshairIsHiddenAndTextBesideItIsNot() {
        assertEquals(0, at(0, false), 1e-9);
        assertEquals(0, at(10, false), 1e-9);
        assertEquals(1, at(26, false), 1e-9);
    }

    @Test
    void combatWidensTheClearZone() {
        assertTrue(at(20, false) > 0.9);
        assertEquals(0, at(20, true), 1e-9);
        assertEquals(1, at(40, true), 1e-9);
    }

    @Test
    void trailDirectionTurnsByTheSideAngleAndKeepsPitch() {
        double[] dir = LyricLayout.turned(0, -0.3, 1, 26);
        double clearance = LyricLayout.centerClearance(dir[0], dir[1], dir[2], 0, -0.3, 1, false);
        assertEquals(-0.3, dir[1], 1e-9);
        assertTrue(clearance > 0.99, "26 degree side angle clears the crosshair: " + clearance);
    }
}
