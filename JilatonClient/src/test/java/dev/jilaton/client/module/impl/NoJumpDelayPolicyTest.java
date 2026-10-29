package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoJumpDelayPolicyTest {
    @Test
    void onlyRearmsWhenVanillaRaisesTheCooldown() {
        assertTrue(NoJumpDelayPolicy.shouldRearm(10, 0));
        assertFalse(NoJumpDelayPolicy.shouldRearm(3, 3));
        assertFalse(NoJumpDelayPolicy.shouldRearm(0, 4));
    }

    @Test
    void clampsVanillaCooldownToConfiguredBaseAndVariance() {
        assertEquals(0, NoJumpDelayPolicy.rearm(10, 0, 0));
        assertEquals(7, NoJumpDelayPolicy.rearm(10, 5, 2));
        assertEquals(6, NoJumpDelayPolicy.rearm(6, 8, 1));
        assertEquals(0, NoJumpDelayPolicy.rearm(-1, 5, 2));
    }
}
