package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastFallPolicyTest {
    @Test
    void capsOnlyDownwardMovementAndLeavesFasterDownwardMotionUnchanged() {
        assertEquals(-0.9, FastFallPolicy.accelerateDownwardMovement(-0.2, 0.9, true), 1.0e-9);
        assertEquals(-1.2, FastFallPolicy.accelerateDownwardMovement(-1.2, 0.9, true), 1.0e-9);
        assertEquals(0.15, FastFallPolicy.accelerateDownwardMovement(0.15, 0.9, true), 1.0e-9);
        assertEquals(-0.2, FastFallPolicy.accelerateDownwardMovement(-0.2, 0.9, false), 1.0e-9);
    }

    @Test
    void rejectsNonFiniteMovementAndInvalidSpeedWhenEnabled() {
        assertThrows(IllegalArgumentException.class,
                () -> FastFallPolicy.accelerateDownwardMovement(Double.NaN, 0.9, true));
        assertThrows(IllegalArgumentException.class,
                () -> FastFallPolicy.accelerateDownwardMovement(-0.2, 0.0, true));
    }
}
