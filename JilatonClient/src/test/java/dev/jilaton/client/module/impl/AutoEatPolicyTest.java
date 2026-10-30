package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoEatPolicyTest {
    @Test
    void hungerAndHealthThresholdsAreIndependentAndZeroHealthDisablesThatTrigger() {
        assertTrue(AutoEatPolicy.shouldEat(16, 20.0F, 16, 10.0F));
        assertTrue(AutoEatPolicy.shouldEat(20, 10.0F, 16, 10.0F));
        assertFalse(AutoEatPolicy.shouldEat(20, 1.0F, 16, 0.0F));
        assertFalse(AutoEatPolicy.shouldEat(18, 20.0F, 16, 10.0F));
    }

    @Test
    void prefersAlwaysEdibleFoodWhenHealthIsLow() {
        assertEquals(108.0F, AutoEatPolicy.score(4, 4.0F, true, true));
        assertEquals(8.0F, AutoEatPolicy.score(4, 4.0F, true, false));
        assertEquals(6.0F, AutoEatPolicy.score(4, 2.0F, false, true));
    }
}
