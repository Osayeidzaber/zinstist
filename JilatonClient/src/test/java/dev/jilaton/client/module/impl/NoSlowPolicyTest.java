package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSlowPolicyTest {
    @Test
    void preservesVanillaOrRemovesItemSlowdownAccordingToSettings() {
        assertEquals(0.2F, NoSlowPolicy.itemUseMultiplier(false, true, 0.5, 0.2F));
        assertEquals(1.0F, NoSlowPolicy.itemUseMultiplier(true, true, 0.5, 0.2F));
        assertEquals(0.1F, NoSlowPolicy.itemUseMultiplier(true, false, 0.5, 0.2F), 0.000001F);
    }

    @Test
    void boundsScaleAndKeepsSprintControlIndependent() {
        assertEquals(0.4F, NoSlowPolicy.itemUseMultiplier(true, false, 9.0, 0.2F), 0.000001F);
        assertEquals(0.0F, NoSlowPolicy.itemUseMultiplier(true, false, -1.0, 0.2F));
        assertFalse(NoSlowPolicy.slowDueToItemUse(true, true, true));
        assertTrue(NoSlowPolicy.slowDueToItemUse(true, false, true));
        assertFalse(NoSlowPolicy.slowDueToItemUse(false, true, false));
    }
}
