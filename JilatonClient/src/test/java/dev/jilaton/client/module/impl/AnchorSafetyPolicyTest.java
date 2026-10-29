package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorSafetyPolicyTest {
    @Test
    void rejectsDamageAboveLimitOrLethalWithoutTotem() {
        assertFalse(AnchorSafetyPolicy.allowsDetonation(7.0F, 6.0F, 20.0F, 0.0F, false));
        assertFalse(AnchorSafetyPolicy.allowsDetonation(20.0F, 20.0F, 19.0F, 0.0F, false));
    }

    @Test
    void allowsDamageWithinLimitAndLethalDamageWithTotem() {
        assertTrue(AnchorSafetyPolicy.allowsDetonation(5.0F, 6.0F, 20.0F, 0.0F, false));
        assertTrue(AnchorSafetyPolicy.allowsDetonation(20.0F, 20.0F, 19.0F, 0.0F, true));
    }

    @Test
    void rejectsNonFiniteEstimates() {
        assertFalse(AnchorSafetyPolicy.allowsDetonation(Float.NaN, 6.0F, 20.0F, 0.0F, true));
    }
}
