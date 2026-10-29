package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedPolicyTest {
    @Test
    void hopsOnlyWhenGroundedAndMoving() {
        assertTrue(SpeedPolicy.shouldHop(true, true, false, false, false, false, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(false, true, false, false, false, false, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, false, false, false, false, false, false,
                false, false));
    }

    @Test
    void respectsInputAndStateGuards() {
        assertFalse(SpeedPolicy.shouldHop(true, true, true, false, false, false, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, true, false, true, false, false, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, true, false, false, true, false, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, true, false, false, false, true, false,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, true, false, false, false, false, true,
                false, false));
        assertFalse(SpeedPolicy.shouldHop(true, true, false, false, false, false, false,
                true, false));
    }
}
