package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityStrafeMathTest {
    @Test
    void mapsDirectionalInputRelativeToYaw() {
        assertHorizontal(0.0, 2.0, VelocityStrafeMath.reorient(0.0, 1.0, 0.0F,
                true, false, false, false, 2.0));
        assertHorizontal(2.0, 0.0, VelocityStrafeMath.reorient(0.0, 1.0, 0.0F,
                false, false, false, true, 2.0));
        assertHorizontal(-2.0, 0.0, VelocityStrafeMath.reorient(0.0, 1.0, 90.0F,
                true, false, false, false, 2.0));
    }

    @Test
    void normalizesDiagonalInputAndPreservesDirectionWithoutKeys() {
        VelocityStrafeMath.Horizontal diagonal = VelocityStrafeMath.reorient(0.0, 1.0, 0.0F,
                true, false, false, true, 2.0);
        assertEquals(2.0, Math.hypot(diagonal.x(), diagonal.z()), 1.0E-9);
        assertHorizontal(1.2, 1.6, VelocityStrafeMath.reorient(3.0, 4.0, 0.0F,
                false, false, false, false, 2.0));
    }

    private static void assertHorizontal(double expectedX, double expectedZ,
                                         VelocityStrafeMath.Horizontal actual) {
        assertEquals(expectedX, actual.x(), 1.0E-9);
        assertEquals(expectedZ, actual.z(), 1.0E-9);
    }
}
