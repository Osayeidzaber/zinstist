package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

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
    void rejectsNonFiniteGeometry() {
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(Double.NaN, 1.0, 0.0, 0.0, 0.0F));
        assertFalse(ShieldTargetPolicy.shieldFacesAttacker(1.0, 0.0, 0.0, 0.0, Float.NaN));
    }
}
