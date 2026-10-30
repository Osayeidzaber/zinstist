package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void boundsBurstAttemptsToTwoAndNeverSchedulesNegativeThrows() {
        assertEquals(0, AutoShootPolicy.throwsThisTick(-3));
        assertEquals(0, AutoShootPolicy.throwsThisTick(0));
        assertEquals(1, AutoShootPolicy.throwsThisTick(1));
        assertEquals(2, AutoShootPolicy.throwsThisTick(2));
        assertEquals(2, AutoShootPolicy.throwsThisTick(20));
    }
}
