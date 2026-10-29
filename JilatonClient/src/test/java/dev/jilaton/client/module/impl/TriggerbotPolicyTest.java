package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerbotPolicyTest {
    @Test
    void reactionDeadlineIncludesTheSelectedSpread() {
        assertEquals(18L, TriggerbotPolicy.reactionReadyAt(10L, 5, 3));
        assertEquals(10L, TriggerbotPolicy.reactionReadyAt(10L, 0, 0));
    }

    @Test
    void targetTrackingGraceIncludesItsBoundaryAndRejectsClockRewind() {
        assertFalse(TriggerbotPolicy.shouldForgetTarget(14L, 10L, 4));
        assertTrue(TriggerbotPolicy.shouldForgetTarget(15L, 10L, 4));
        assertTrue(TriggerbotPolicy.shouldForgetTarget(9L, 10L, 4));
    }

    @Test
    void attackRequiresReactionChargeAndConfiguredInputAndWeaponGates() {
        assertFalse(TriggerbotPolicy.mayAttack(20L, 21L, 19L, 1.0F,
                false, false, false, false));
        assertFalse(TriggerbotPolicy.mayAttack(20L, 20L, 21L, 1.0F,
                false, false, false, false));
        assertFalse(TriggerbotPolicy.mayAttack(20L, 20L, 19L, 0.99F,
                false, false, false, false));
        assertFalse(TriggerbotPolicy.mayAttack(20L, 20L, 19L, 1.0F,
                true, false, false, false));
        assertFalse(TriggerbotPolicy.mayAttack(20L, 20L, 19L, 1.0F,
                false, false, true, false));
        assertTrue(TriggerbotPolicy.mayAttack(20L, 20L, 19L, 1.0F,
                true, true, true, true));
    }

    @Test
    void rejectsNegativeDelaysAndGrace() {
        assertThrows(IllegalArgumentException.class,
                () -> TriggerbotPolicy.reactionReadyAt(0L, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> TriggerbotPolicy.shouldForgetTarget(0L, 0L, -1));
    }
}
