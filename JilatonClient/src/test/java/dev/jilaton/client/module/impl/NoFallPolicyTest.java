package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoFallPolicyTest {
    @Test
    void spoofStartsAtTheConfiguredFallDistanceWhileDescending() {
        assertFalse(NoFallPolicy.shouldSpoof(false, true, false, 2.9, 3.0));
        assertTrue(NoFallPolicy.shouldSpoof(false, true, false, 3.0, 3.0));
        assertFalse(NoFallPolicy.shouldSpoof(false, false, false, 4.0, 3.0));
    }

    @Test
    void neverSpoofsForGroundedOrExemptMovement() {
        assertFalse(NoFallPolicy.shouldSpoof(true, true, false, 5.0, 3.0));
        assertFalse(NoFallPolicy.shouldSpoof(false, true, true, 5.0, 3.0));
        assertFalse(NoFallPolicy.shouldSpoof(false, true, false, Double.NaN, 3.0));
    }

    @Test
    void zeroTriggerSpoofsImmediatelyAndNonFiniteTriggerNeverDoes() {
        assertTrue(NoFallPolicy.shouldSpoof(false, true, false, 0.0, 0.0));
        assertFalse(NoFallPolicy.shouldSpoof(false, true, false, -0.5, 0.0));
        assertFalse(NoFallPolicy.shouldSpoof(false, true, false, 5.0, Double.NaN));
    }
}
