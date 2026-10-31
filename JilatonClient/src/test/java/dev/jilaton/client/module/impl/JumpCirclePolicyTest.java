package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumpCirclePolicyTest {
    @Test
    void triggerSelectsTheEvent() {
        assertTrue(JumpCirclePolicy.emits(true, false, "Jump"));
        assertTrue(JumpCirclePolicy.emits(true, false, "Both"));
        assertFalse(JumpCirclePolicy.emits(true, false, "Land"));
        assertTrue(JumpCirclePolicy.emits(false, true, "Land"));
        assertTrue(JumpCirclePolicy.emits(false, true, "Both"));
        assertFalse(JumpCirclePolicy.emits(false, true, "Jump"));
        assertFalse(JumpCirclePolicy.emits(false, false, "Both"));
    }

    @Test
    void cooldownBlocksDuplicateTransitions() {
        assertTrue(JumpCirclePolicy.withinCooldown(1_000_000_000L, 950_000_000L));
        assertFalse(JumpCirclePolicy.withinCooldown(1_000_000_000L, 800_000_000L));
        assertFalse(JumpCirclePolicy.withinCooldown(1_000_000_000L, 0L));
    }

    @Test
    void landingAfterAJumpIsTheSameEffect() {
        long now = 10_000_000_000L;
        assertTrue(JumpCirclePolicy.landingCovered(now, now + 1_000));
        assertFalse(JumpCirclePolicy.landingCovered(now, now - 1));
        assertFalse(JumpCirclePolicy.landingCovered(now, 0));
    }
}
