package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KillAuraTargetingTest {
    @Test
    void distancePriorityChoosesClosestReachableEntity() {
        assertEquals(2.0, KillAuraTargeting.rank(KillAuraTargeting.DISTANCE, 2.0, 16.0, 40.0));
    }

    @Test
    void healthPriorityUsesHealthAndAbsorptionCombinedByCaller() {
        assertEquals(6.0, KillAuraTargeting.rank(KillAuraTargeting.HEALTH, 4.0, 6.0, 20.0));
    }

    @Test
    void anglePriorityPrefersTheTargetNearestTheCurrentView() {
        assertEquals(12.0, KillAuraTargeting.rank(KillAuraTargeting.ANGLE, 3.0, 20.0, 12.0));
    }

    @Test
    void unknownPriorityIsRejectedInsteadOfSilentlyChangingBehavior() {
        assertThrows(IllegalArgumentException.class,
                () -> KillAuraTargeting.rank("random", 1.0, 1.0, 1.0));
    }
}
