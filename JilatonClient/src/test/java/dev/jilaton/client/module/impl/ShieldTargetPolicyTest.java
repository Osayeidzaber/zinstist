package dev.jilaton.client.module.impl;

import net.minecraft.world.item.component.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShieldTargetPolicyTest {
    @Test
    void detectsAttackerInsideAndOutsideTheShieldFacingHemisphere() {
        assertTrue(ShieldTargetPolicy.shieldFacesAttacker(0.0, 2.0, 0.0, 0.0, 0.0F));
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(0.0, -2.0, 0.0, 0.0, 0.0F));
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(0.0, 0.0, 0.0, 0.0, 0.0F));
    }

    @Test
    void perpendicularAttackerIsOutsideTheStrictHemisphereBoundary() {
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(2.0, 0.0, 0.0, 0.0, 0.0F));
        assertTrue(ShieldTargetPolicy.shieldFacesAttacker(0.001, 2.0, 0.0, 0.0, 0.0F));
    }

    @Test
    void rejectsNonFiniteGeometry() {
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(Double.NaN, 1.0, 0.0, 0.0, 0.0F));
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(1.0, 0.0, 0.0, 0.0, Float.NaN));
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(Double.POSITIVE_INFINITY, 1.0, 0.0, 0.0, 0.0F));
    }

    @Test
    void blockingButFacingAwayDoesNotAbsorbTheAttack() {
        assertFalse(ShieldTargetPolicy.shieldAbsorbsAttack(true, false, false));
    }

    @Test
    void blockingThatFacesTheAttackerAbsorbsTheAttack() {
        assertTrue(ShieldTargetPolicy.shieldAbsorbsAttack(true, true, false));
    }

    @Test
    void stoppedBlockingDuringTheAxeDisableWindowAbsorbsNothing() {
        int disableTicks = Math.round(Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS * 1.0F * 20.0F);
        assertEquals(100, disableTicks);
        assertFalse(ShieldTargetPolicy.shieldAbsorbsAttack(false, true, false));
        assertTrue(ShieldTargetPolicy.shieldAbsorbsAttack(true, true, false));
    }

    @Test
    void axeAttackIsNotWastedBecauseItDisablesTheShieldAfterTheReducedHit() {
        assertFalse(ShieldTargetPolicy.shieldAbsorbsAttack(true, true, true));
    }

    @Test
    void anyItemWithTheBlocksAttacksComponentBlocksNotOnlyShields() {
        assertTrue(ShieldTargetPolicy.shieldAbsorbsAttack(true, true, false));
    }
}
