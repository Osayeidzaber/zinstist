package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityPolicyTest {
    @Test
    void appliesChanceGroundAndMovementFilters() {
        assertTrue(VelocityPolicy.shouldModify(true, false, 99, 100, "Any", false));
        assertFalse(VelocityPolicy.shouldModify(false, false, 4, 40, "On ground", false));
        assertTrue(VelocityPolicy.shouldModify(false, true, 4, 40, "In air", true));
        assertFalse(VelocityPolicy.shouldModify(false, false, 4, 40, "In air", true));
        assertFalse(VelocityPolicy.shouldModify(true, true, 40, 40, "Any", false));
        assertFalse(VelocityPolicy.shouldModify(true, true, 0, 0, "Any", false));
    }

    @Test
    void usesIncomingVelocityOrConfiguredCurrentMotionFallback() {
        assertEquals(1.0, VelocityPolicy.applyAxis(4.0, 2.0, 25.0, 50.0), 1.0E-9);
        assertEquals(1.0, VelocityPolicy.applyAxis(4.0, 2.0, 0.0, 50.0), 1.0E-9);
        assertEquals(-2.0, VelocityPolicy.applyAxis(4.0, 2.0, -50.0, 50.0), 1.0E-9);
    }
}
