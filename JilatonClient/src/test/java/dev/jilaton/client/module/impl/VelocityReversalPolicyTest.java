package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityReversalPolicyTest {
    @Test
    void scalesAndReversesHorizontalComponentsWhilePreservingVerticalMotion() {
        VelocityReversalPolicy.Motion motion = VelocityReversalPolicy.apply(4.0, 1.5, -2.0, 0.5, 0.25);

        assertEquals(-2.0, motion.x(), 1.0E-9);
        assertEquals(1.5, motion.y(), 1.0E-9);
        assertEquals(0.5, motion.z(), 1.0E-9);
    }

    @Test
    void movingGateAndInvalidModifiersAreBounded() {
        assertTrue(VelocityReversalPolicy.shouldSchedule(false, false));
        assertFalse(VelocityReversalPolicy.shouldSchedule(true, false));
        assertTrue(VelocityReversalPolicy.shouldSchedule(true, true));
        VelocityReversalPolicy.Motion motion = VelocityReversalPolicy.apply(2.0, 0.0, 3.0,
                Double.NaN, 1.5);
        assertEquals(0.0, motion.x(), 1.0E-9);
        assertEquals(-3.0, motion.z(), 1.0E-9);
    }
}
