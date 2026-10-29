package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBowReleasePolicyTest {
    @Test
    void requiresEnabledModuleTargetEnoughChargeAndAimAlignment() {
        assertFalse(shouldRelease(false, true, 20, 15, 0.5, 1.5, 100, 0, 0));
        assertFalse(shouldRelease(true, false, 20, 15, 0.5, 1.5, 100, 0, 0));
        assertFalse(shouldRelease(true, true, 14, 15, 0.5, 1.5, 100, 0, 0));
        assertFalse(shouldRelease(true, true, 20, 15, 1.6, 1.5, 100, 0, 0));
        assertTrue(shouldRelease(true, true, 15, 15, 1.5, 1.5, 100, 0, 0));
    }

    @Test
    void enforcesDelayAndRejectsNonFiniteAimError() {
        assertFalse(shouldRelease(true, true, 20, 15, 0.5, 1.5, 1_499, 500, 1_000));
        assertTrue(shouldRelease(true, true, 20, 15, 0.5, 1.5, 1_500, 500, 1_000));
        assertFalse(shouldRelease(true, true, 20, 15, Double.NaN, 1.5, 100, 0, 0));
        assertFalse(shouldRelease(true, true, 20, 15, Double.POSITIVE_INFINITY, 1.5, 100, 0, 0));
    }

    private static boolean shouldRelease(boolean enabled, boolean hasTarget, int drawnTicks, int requiredTicks,
                                         double aimError, double aimThreshold, long now, long previous, long delay) {
        return AutoBowReleasePolicy.shouldRelease(enabled, hasTarget, drawnTicks, requiredTicks,
                aimError, aimThreshold, now, previous, delay);
    }
}
