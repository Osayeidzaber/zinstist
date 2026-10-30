package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AimAssistPolicyTest {
    @Test
    void interpolatesYawOverTheShortestPathAcrossTheWrapBoundary() {
        AimAssistPolicy.Angles result = AimAssistPolicy.apply(179.0F, 10.0F, -179.0F, 30.0F,
                0.5, true, true);

        assertEquals(180.0F, result.yaw(), 0.001F);
        assertEquals(20.0F, result.pitch(), 0.001F);
    }

    @Test
    void interpolatesAcrossTheWrapBoundaryInTheNegativeDirection() {
        AimAssistPolicy.Angles result = AimAssistPolicy.apply(-179.0F, 0.0F, 179.0F, 0.0F,
                0.5, true, false);

        assertEquals(-180.0F, result.yaw(), 0.001F);
    }

    @Test
    void normalizesTargetYawThatLiesOutsideTheSignedRange() {
        AimAssistPolicy.Angles result = AimAssistPolicy.apply(0.0F, 0.0F, -350.0F, 0.0F,
                1.0, true, false);

        assertEquals(10.0F, result.yaw(), 0.001F);
    }

    @Test
    void axesCanBeAdjustedIndependently() {
        AimAssistPolicy.Angles result = AimAssistPolicy.apply(20.0F, 10.0F, 80.0F, 50.0F,
                0.5, true, false);

        assertEquals(50.0F, result.yaw(), 0.001F);
        assertEquals(10.0F, result.pitch(), 0.001F);
    }

    @Test
    void clampsFactorAndPitchAndIgnoresNonFiniteRequests() {
        AimAssistPolicy.Angles full = AimAssistPolicy.apply(0.0F, 0.0F, 90.0F, 120.0F,
                4.0, true, true);
        assertEquals(90.0F, full.yaw(), 0.001F);
        assertEquals(90.0F, full.pitch(), 0.001F);

        AimAssistPolicy.Angles invalid = AimAssistPolicy.apply(12.0F, -5.0F,
                Float.NaN, 30.0F, 0.5, true, true);
        assertEquals(12.0F, invalid.yaw(), 0.001F);
        assertEquals(-5.0F, invalid.pitch(), 0.001F);
    }
}
