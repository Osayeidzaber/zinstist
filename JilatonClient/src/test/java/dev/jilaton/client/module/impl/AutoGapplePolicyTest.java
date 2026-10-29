package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoGapplePolicyTest {
    @Test
    void countsAbsorptionWhenConfigured() {
        assertFalse(AutoGapplePolicy.shouldUse(9.0, 2.0, 10.0, true));
        assertTrue(AutoGapplePolicy.shouldUse(9.0, 2.0, 10.0, false));
    }

    @Test
    void neverTreatsNegativeAbsorptionAsHealth() {
        assertTrue(AutoGapplePolicy.shouldUse(9.0, -2.0, 10.0, true));
    }
}
