package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityJumpResetPolicyTest {
    @Test
    void jumpsOnlyInTheObservedHurtWindowAfterAllSafetyGates() {
        assertTrue(VelocityJumpResetPolicy.shouldJump(9, true, true, false, 2, 2, 99, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(8, true, true, false, 2, 2, 0, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(9, false, true, false, 2, 2, 0, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(9, true, false, false, 2, 2, 0, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(9, true, true, true, 2, 2, 0, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(9, true, true, false, 1, 2, 0, 100));
        assertFalse(VelocityJumpResetPolicy.shouldJump(9, true, true, false, 2, 2, 40, 40));
    }

    @Test
    void receivedHitModeAdvancesOnlyInsideTheHurtWindow() {
        assertTrue(VelocityJumpResetPolicy.shouldAdvanceCooldown(false, 0));
        assertFalse(VelocityJumpResetPolicy.shouldAdvanceCooldown(true, 8));
        assertTrue(VelocityJumpResetPolicy.shouldAdvanceCooldown(true, 9));
    }
}
