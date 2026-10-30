package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StepPolicyTest {
    @Test
    void onlyRaisesVanillaStepHeightForTheLocalPlayerWhenEnabled() {
        assertEquals(1.0F, StepPolicy.effectiveHeight(true, true, 0.6F, 1.0));
        assertEquals(1.5F, StepPolicy.effectiveHeight(true, true, 1.5F, 1.0));
        assertEquals(0.6F, StepPolicy.effectiveHeight(true, false, 0.6F, 2.0));
        assertEquals(0.6F, StepPolicy.effectiveHeight(false, true, 0.6F, 2.0));
    }

    @Test
    void clampsConfiguredHeightToSupportedBounds() {
        assertEquals(0.6F, StepPolicy.effectiveHeight(true, true, 0.4F, -10.0));
        assertEquals(2.5F, StepPolicy.effectiveHeight(true, true, 0.6F, 10.0));
    }
}
