package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighJumpPolicyTest {
    @Test
    void replacesSuccessfulVanillaJumpMotionWithConfiguredValue() {
        assertEquals(0.8, HighJumpPolicy.motionAfterJump(0.42, 0.8), 1.0E-9);
        assertEquals(0.2, HighJumpPolicy.motionAfterJump(0.42, 0.2), 1.0E-9);
    }

    @Test
    void doesNotChangeUnsuccessfulJumpOrInvalidConfiguredMotion() {
        assertEquals(0.0, HighJumpPolicy.motionAfterJump(0.0, 0.8), 1.0E-9);
        assertEquals(Double.NaN, HighJumpPolicy.motionAfterJump(Double.NaN, 0.8));
        assertEquals(0.42, HighJumpPolicy.motionAfterJump(0.42, Double.NaN), 1.0E-9);
        assertEquals(0.42, HighJumpPolicy.motionAfterJump(0.42, 0.0), 1.0E-9);
    }
}
