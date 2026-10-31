package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PharaohHeaddressTransformTest {
    private static final double UNIT = 0.0625;
    private static final double EPS = 1.0E-9;

    @Test
    void forwardFacingHeadKeepsTheHeaddressUpright() {
        double[] p = PharaohHeaddressModule.transform(1.0, 2.0, 3.0, 10.0, 20.0, 30.0, 1.0, 0.0, 1.0, 0.0, UNIT);
        assertEquals(10.0 + UNIT, p[0], EPS, "x");
        assertEquals(20.0 + 2.0 * UNIT, p[1], EPS, "y");
        assertEquals(30.0 + 3.0 * UNIT, p[2], EPS, "z");
    }

    @Test
    void scaleFollowsTheUnitSize() {
        double small = PharaohHeaddressModule.transform(0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, UNIT)[1];
        double big = PharaohHeaddressModule.transform(0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, UNIT * 1.5)[1];
        assertEquals(4.0 * UNIT, small, EPS);
        assertEquals(1.5 * small, big, EPS);
    }

    @Test
    void yawTurnsTheFacePlaneIntoTheSidePlane() {
        double[] turned = PharaohHeaddressModule.transform(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, UNIT);
        assertEquals(-UNIT, turned[0], EPS, "a quarter yaw sends +Z to -X");
        assertEquals(0.0, turned[2], EPS);
    }

    @Test
    void pitchLiftsTheCrownBackAsTheHeadLooksUp() {
        double[] level = PharaohHeaddressModule.transform(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, UNIT);
        double[] up = PharaohHeaddressModule.transform(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, UNIT);
        assertEquals(UNIT, level[1], EPS);
        assertEquals(0.0, up[1], EPS, "a quarter pitch lays the crown height onto z");
        assertEquals(-UNIT, up[2], EPS);
        assertNotEquals(level[1], up[1]);
    }

    @Test
    void everyCornerOfTheCrownStaysFinite() {
        for (int c = 0; c < 8; c++) {
            double lx = (c & 1) == 0 ? -5.0 : 5.0;
            double ly = (c & 2) == 0 ? 6.4 : 13.0;
            double lz = (c & 4) == 0 ? -5.0 : 5.0;
            double[] p = PharaohHeaddressModule.transform(lx, ly, lz, 1.0, 2.0, 3.0, 0.8, 0.6, 0.6, 0.8, UNIT);
            for (double value : p) {
                assertTrue(Double.isFinite(value), "corner " + c + " is not finite");
            }
        }
    }
}
