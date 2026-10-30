package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeepSprintPolicyTest {
    @Test
    void usesConfiguredMotionAndHurtMotionRanges() {
        assertEquals(0.8, KeepSprintPolicy.motionFactor(0, 60.0, 80.0, 20.0, 40.0,
                1, 10, 100.0, 50.0, 1.0), 0.000001);
        assertEquals(0.4, KeepSprintPolicy.motionFactor(5, 60.0, 80.0, 20.0, 40.0,
                1, 10, 100.0, 50.0, 1.0), 0.000001);
    }

    @Test
    void appliesChanceFallbackAndRejectsInvalidRanges() {
        assertEquals(0.6, KeepSprintPolicy.motionFactor(0, 100.0, 100.0, 100.0, 100.0,
                1, 10, 25.0, 26.0, 0.5), 0.000001);
        assertEquals(0.6, KeepSprintPolicy.motionFactor(0, 101.0, 100.0, 100.0, 100.0,
                1, 10, 100.0, 50.0, 0.5), 0.000001);
    }
}
