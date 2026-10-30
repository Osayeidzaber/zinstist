package dev.jilaton.client.gui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarMathTest {
    private static final double EPSILON = 1e-9;

    @Test void northUpPutsEastRightAndSouthDown() {
        RadarMath.Point east = RadarMath.project(10, 0, 0, true, 20, 100);
        assertEquals(50, east.x(), EPSILON);
        assertEquals(0, east.y(), EPSILON);
        RadarMath.Point north = RadarMath.project(0, -10, 0, true, 20, 100);
        assertEquals(0, north.x(), EPSILON);
        assertEquals(-50, north.y(), EPSILON);
        RadarMath.Point south = RadarMath.project(0, 10, 0, true, 20, 100);
        assertEquals(50, south.y(), EPSILON);
        assertTrue(south.insideRange());
    }

    @Test void followYawRotatesTheWorldAroundThePlayer() {
        // Yaw 0 faces south (+Z): a player ahead draws upward, east draws right.
        RadarMath.Point ahead = RadarMath.project(0, 10, 0, false, 20, 100);
        assertEquals(0, ahead.x(), EPSILON);
        assertEquals(-50, ahead.y(), EPSILON);
        RadarMath.Point right = RadarMath.project(10, 0, 0, false, 20, 100);
        assertEquals(50, right.x(), EPSILON);
        assertEquals(0, right.y(), EPSILON);
        // Yaw 90 faces west (-X): west is ahead and north is left.
        RadarMath.Point west = RadarMath.project(-10, 0, 90, false, 20, 100);
        assertEquals(0, west.x(), EPSILON);
        assertEquals(-50, west.y(), EPSILON);
        RadarMath.Point north = RadarMath.project(0, -10, 90, false, 20, 100);
        assertEquals(-50, north.x(), EPSILON);
        assertEquals(0, north.y(), EPSILON);
    }

    @Test void pointsScaleByRangeAndFarPointsClampToTheEdge() {
        RadarMath.Point near = RadarMath.project(6, 8, 0, true, 32, 40);
        assertEquals(10, near.distance(), EPSILON);
        assertEquals(7.5, near.x(), EPSILON);
        assertEquals(10, near.y(), EPSILON);
        assertTrue(near.insideRange());

        RadarMath.Point far = RadarMath.project(60, 80, 0, true, 32, 40);
        assertEquals(100, far.distance(), EPSILON);
        assertEquals(40, Math.hypot(far.x(), far.y()), 1e-6);
        assertFalse(far.insideRange());

        RadarMath.Point edge = RadarMath.project(32, 0, 0, true, 32, 40);
        assertTrue(edge.insideRange());
        assertEquals(40, edge.x(), EPSILON);
    }

    @Test void northUpIgnoresYaw() {
        RadarMath.Point first = RadarMath.project(3, -4, 0, true, 32, 40);
        RadarMath.Point second = RadarMath.project(3, -4, 137, true, 32, 40);
        assertEquals(first.x(), second.x(), EPSILON);
        assertEquals(first.y(), second.y(), EPSILON);
    }

    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RadarMath.project(Double.NaN, 0, 0, true, 32, 40));
        assertThrows(IllegalArgumentException.class,
                () -> RadarMath.project(0, Double.POSITIVE_INFINITY, 0, true, 32, 40));
        assertThrows(IllegalArgumentException.class,
                () -> RadarMath.project(0, 0, Float.NaN, true, 32, 40));
        assertThrows(IllegalArgumentException.class,
                () -> RadarMath.project(0, 0, 0, true, 0, 40));
        assertThrows(IllegalArgumentException.class,
                () -> RadarMath.project(0, 0, 0, false, 32, 0));
    }
}
