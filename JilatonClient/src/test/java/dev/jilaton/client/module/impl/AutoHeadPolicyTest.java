package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoHeadPolicyTest {
    @Test
    void usesHeadAtLowHealthOnlyBelowAbsorptionCeiling() {
        assertTrue(AutoHeadPolicy.shouldUse(6.0F, 20.0F, 0.5F, 40.0, 1.0, true,
                true, true, true));
        assertFalse(AutoHeadPolicy.shouldUse(6.0F, 20.0F, 1.5F, 40.0, 1.0, true,
                true, true, true));
        assertTrue(AutoHeadPolicy.shouldUse(7.0F, 20.0F, 2.0F, 40.0, 3.0, false,
                true, true, true));
    }

    @Test
    void rejectsUnsafeMissingAndInvalidStates() {
        assertFalse(AutoHeadPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, 1.0, true,
                false, true, true));
        assertFalse(AutoHeadPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, 1.0, true,
                true, false, true));
        assertFalse(AutoHeadPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, 1.0, true,
                true, true, false));
        assertFalse(AutoHeadPolicy.shouldUse(Float.NaN, 20.0F, 0.0F, 40.0, 1.0, true,
                true, true, true));
    }
}
