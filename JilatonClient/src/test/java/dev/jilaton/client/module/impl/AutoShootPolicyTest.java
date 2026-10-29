package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoShootPolicyTest {
    @Test
    void permitsThrowOnlyWhenAllAimAndActionGatesPass() {
        assertTrue(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                true, true, true, true, true, true)));
    }

    @Test
    void blocksThrowWhenAnyRequiredGateFails() {
        assertFalse(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                true, true, true, true, true, false)));
        assertFalse(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                true, true, true, true, false, true)));
        assertFalse(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                true, true, true, false, true, true)));
        assertFalse(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                true, true, false, true, true, true)));
        assertFalse(AutoShootPolicy.canShoot(new AutoShootPolicy.Conditions(
                false, true, true, true, true, true)));
    }
}
