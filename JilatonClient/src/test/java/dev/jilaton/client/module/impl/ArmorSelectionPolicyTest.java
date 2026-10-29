package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorSelectionPolicyTest {
    @Test
    void preservesSafeArmorAgainstAnUnsafeCandidate() {
        assertFalse(ArmorSelectionPolicy.shouldSelect(true, false, true, 20.0, 10.0, 0.0));
    }

    @Test
    void aSafeCandidateCanReplaceLowDurabilityArmorEvenWithSlightlyLowerScore() {
        assertTrue(ArmorSelectionPolicy.shouldSelect(false, true, false, 9.0, 10.0, 0.25));
    }

    @Test
    void onceAHighDurabilityChoiceExistsItWinsTheRemainingComparison() {
        assertFalse(ArmorSelectionPolicy.shouldSelect(false, false, true, 100.0, 10.0, 0.0));
        assertTrue(ArmorSelectionPolicy.shouldSelect(false, true, true, 12.0, 10.0, 1.0));
    }

    @Test
    void rejectsInvalidScoresAndUpgradeThresholds() {
        assertFalse(ArmorSelectionPolicy.shouldSelect(false, true, false, Double.NaN, 1.0, 0.0));
        assertFalse(ArmorSelectionPolicy.shouldSelect(false, true, false, 2.0, 1.0, -1.0));
    }
}
