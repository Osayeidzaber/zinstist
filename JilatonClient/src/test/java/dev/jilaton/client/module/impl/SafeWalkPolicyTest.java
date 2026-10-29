package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeWalkPolicyTest {
    @Test
    void sneaksOnlyForUnsupportedGroundedMovement() {
        assertTrue(SafeWalkPolicy.shouldSneak(true, false, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, true, false, false));
    }

    @Test
    void preservesManualSneakAndExemptOrStationaryStates() {
        assertFalse(SafeWalkPolicy.shouldSneak(false, false, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, true, true, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, false, false, true));
        assertFalse(SafeWalkPolicy.shouldSneak(true, false, true, true, true));
    }
}
