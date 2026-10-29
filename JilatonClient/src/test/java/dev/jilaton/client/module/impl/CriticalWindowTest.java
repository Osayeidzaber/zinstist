package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriticalWindowTest {
    @Test
    void acceptsOnlyAnAirborneFallingPlayerWithPositiveFallDistance() {
        assertTrue(CriticalWindow.isDescending(false, 0.01F, -0.08D));
        assertFalse(CriticalWindow.isDescending(true, 0.01F, -0.08D));
        assertFalse(CriticalWindow.isDescending(false, 0.0F, -0.08D));
        assertFalse(CriticalWindow.isDescending(false, 0.01F, 0.0D));
        assertFalse(CriticalWindow.isDescending(false, 0.01F, 0.08D));
    }

    @Test
    void jumpModeJumpsFromGroundAndWaitModeWaitsForAnExistingCriticalWindow() {
        assertTrue(CriticalWindow.shouldQueueAttack(true, true, false));
        assertFalse(CriticalWindow.shouldQueueAttack(true, false, false));
        assertTrue(CriticalWindow.shouldQueueAttack(false, true, false));
        assertTrue(CriticalWindow.shouldQueueAttack(false, false, false));
        assertFalse(CriticalWindow.shouldQueueAttack(true, false, true));
        assertFalse(CriticalWindow.shouldQueueAttack(false, false, true));
    }
}
