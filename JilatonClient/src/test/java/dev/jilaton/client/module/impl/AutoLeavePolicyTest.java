package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoLeavePolicyTest {
    @Test
    void disconnectRequiresLowHealthOnAnUnprotectedMultiplayerSession() {
        assertTrue(AutoLeavePolicy.shouldDisconnect(true, false, false, false, 7.5F, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, false, false, 8.0F, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(false, false, false, false, 1.0F, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, true, false, false, 1.0F, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, true, false, 1.0F, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, false, true, 1.0F, 8.0));
    }

    @Test
    void nonFiniteHealthAndInvalidThresholdFailClosed() {
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, false, false, Float.NaN, 8.0));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, false, false, 1.0F, Double.POSITIVE_INFINITY));
        assertFalse(AutoLeavePolicy.shouldDisconnect(true, false, false, false, 1.0F, -1.0));
    }

    @Test
    void delayIsBoundedToSupportedTicks() {
        assertEquals(0, AutoLeavePolicy.clampDelay(-4));
        assertEquals(16, AutoLeavePolicy.clampDelay(16));
        assertEquals(60, AutoLeavePolicy.clampDelay(140));
        assertFalse(AutoLeavePolicy.delayElapsed(1, 2));
        assertTrue(AutoLeavePolicy.delayElapsed(2, 2));
        assertTrue(AutoLeavePolicy.delayElapsed(1, 0));
    }
}
