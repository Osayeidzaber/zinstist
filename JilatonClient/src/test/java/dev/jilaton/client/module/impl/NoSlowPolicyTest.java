package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;
import net.minecraft.world.item.ItemUseAnimation;

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

    @Test
    void combinesBoundedGlobalAndUseActionScales() {
        assertEquals(0.025F, NoSlowPolicy.itemUseMultiplier(true, false, 0.5, 0.25, 0.2F), 0.000001F);
        assertEquals(0.4F, NoSlowPolicy.itemUseMultiplier(true, false, 2.0, 2.0, 0.2F), 0.000001F);
        assertEquals(0.2F, NoSlowPolicy.itemUseMultiplier(true, false, Double.NaN, 1.0, 0.2F), 0.000001F);
    }

    @Test
    void selectsTheConfiguredScaleForEveryItemUseAnimation() {
        assertEquals(0.25, NoSlowPolicy.actionScale(ItemUseAnimation.EAT, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(0.25, NoSlowPolicy.actionScale(ItemUseAnimation.DRINK, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(0.5, NoSlowPolicy.actionScale(ItemUseAnimation.BLOCK, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(0.75, NoSlowPolicy.actionScale(ItemUseAnimation.BOW, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(1.0, NoSlowPolicy.actionScale(ItemUseAnimation.TRIDENT, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(1.0, NoSlowPolicy.actionScale(ItemUseAnimation.SPEAR, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(1.25, NoSlowPolicy.actionScale(ItemUseAnimation.CROSSBOW, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(1.5, NoSlowPolicy.actionScale(ItemUseAnimation.BUNDLE, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
        assertEquals(1.5, NoSlowPolicy.actionScale(null, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5));
    }
}
