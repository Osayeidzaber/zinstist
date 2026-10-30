package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRodPolicyTest {
    @Test
    void targetNeedsRangeHealthVisibilityAndAimEligibility() {
        assertTrue(AutoRodPolicy.targetEligible(16.0, 3.5, 5.0, 12.0F, 4.0F, true, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(4.0, 3.5, 5.0, 12.0F, 4.0F, true, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(16.0, 3.5, 5.0, 4.0F, 4.0F, true, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(16.0, 3.5, 5.0, 12.0F, 4.0F, false, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(16.0, 3.5, 5.0, 12.0F, 4.0F, true, 6.0F, 5.0F));
    }

    @Test
    void targetRangeBoundariesAreInclusiveAndNonFiniteAimIsRejected() {
        assertTrue(AutoRodPolicy.targetEligible(12.25, 3.5, 5.0, 12.0F, 4.0F, true, 5.0F, 5.0F));
        assertTrue(AutoRodPolicy.targetEligible(25.0, 3.5, 5.0, 12.0F, 4.0F, true, 5.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(12.2499, 3.5, 5.0, 12.0F, 4.0F, true, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(25.0001, 3.5, 5.0, 12.0F, 4.0F, true, 2.0F, 5.0F));
        assertFalse(AutoRodPolicy.targetEligible(16.0, 3.5, 5.0, 12.0F, 4.0F, true, Float.NaN, 5.0F));
    }

    @Test
    void initiationHonorsHealthCrowdRodAndInputRequirements() {
        assertTrue(AutoRodPolicy.mayStart(16.0F, 10.0F, 1, 1, true, true, true, false, true));
        assertTrue(AutoRodPolicy.mayStart(16.0F, 10.0F, 8, 0, true, false, false, false, true));
        assertFalse(AutoRodPolicy.mayStart(10.0F, 10.0F, 0, 1, true, false, true, false, true));
        assertFalse(AutoRodPolicy.mayStart(16.0F, 10.0F, 2, 1, true, false, true, false, true));
        assertFalse(AutoRodPolicy.mayStart(16.0F, 10.0F, 0, 1, false, false, true, false, true));
        assertFalse(AutoRodPolicy.mayStart(16.0F, 10.0F, 0, 1, true, true, false, false, true));
        assertFalse(AutoRodPolicy.mayStart(16.0F, 10.0F, 0, 1, true, false, true, true, true));
    }

    @Test
    void pullTriggersOnHookTimeoutOrConfiguredRangeExit() {
        assertTrue(AutoRodPolicy.shouldPull(true, false, false, true));
        assertTrue(AutoRodPolicy.shouldPull(false, true, false, true));
        assertTrue(AutoRodPolicy.shouldPull(false, false, true, false));
        assertFalse(AutoRodPolicy.shouldPull(false, false, false, false));
        assertFalse(AutoRodPolicy.shouldPull(false, false, true, true));
    }
}
