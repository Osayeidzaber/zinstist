package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalSafetyPolicyTest {
    @Test
    void allowsNoDamageAndSafeFriendDamageButRejectsExcessOrLethalBlast() {
        assertTrue(CrystalSafetyPolicy.friendDamageIsSafe(0.0F, 8.0F, 1.0F));
        assertTrue(CrystalSafetyPolicy.friendDamageIsSafe(2.0F, 8.0F, 10.0F));
        assertFalse(CrystalSafetyPolicy.friendDamageIsSafe(9.0F, 8.0F, 20.0F));
        assertFalse(CrystalSafetyPolicy.friendDamageIsSafe(4.0F, 8.0F, 6.0F));
    }

    @Test
    void rejectsInvalidDamageInputs() {
        assertFalse(CrystalSafetyPolicy.friendDamageIsSafe(Float.NaN, 8.0F, 20.0F));
        assertFalse(CrystalSafetyPolicy.friendDamageIsSafe(1.0F, -1.0F, 20.0F));
    }

    @Test
    void protectsExplicitFriendsAndScoreboardAlliesButNotSelfOrUnrelatedPlayers() {
        assertTrue(CrystalSafetyPolicy.shouldProtectExplosionVictim(false, true, false));
        assertTrue(CrystalSafetyPolicy.shouldProtectExplosionVictim(false, false, true));
        assertFalse(CrystalSafetyPolicy.shouldProtectExplosionVictim(false, false, false));
        assertFalse(CrystalSafetyPolicy.shouldProtectExplosionVictim(true, true, true));
    }
}
