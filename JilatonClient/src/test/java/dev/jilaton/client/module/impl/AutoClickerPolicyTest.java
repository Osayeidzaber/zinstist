package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoClickerPolicyTest {
    @Test
    void entityObjectivesApplyEnemyAndPlayerFilters() {
        assertTrue(AutoClickerPolicy.allowsEntity("Enemy", true, true, true));
        assertFalse(AutoClickerPolicy.allowsEntity("Enemy", true, false, false));
        assertFalse(AutoClickerPolicy.allowsEntity("Enemy", false, true, false));
        assertTrue(AutoClickerPolicy.allowsEntity("Entity", false, true, false));
        assertFalse(AutoClickerPolicy.allowsEntity("Entity", false, true, true));
        assertFalse(AutoClickerPolicy.allowsEntity("Block", true, true, false));
        assertFalse(AutoClickerPolicy.allowsEntity("Any", true, false, false));
        assertTrue(AutoClickerPolicy.allowsEntity("Any", false, true, false));
    }

    @Test
    void blockObjectivesDoNotShareMeleeTargetGates() {
        assertFalse(AutoClickerPolicy.clicksBlock("Enemy"));
        assertFalse(AutoClickerPolicy.clicksBlock("Entity"));
        assertTrue(AutoClickerPolicy.clicksBlock("Block"));
        assertTrue(AutoClickerPolicy.clicksBlock("Any"));
        assertFalse(AutoClickerPolicy.clicksBlock(null));
    }

    @Test
    void blockBreakGateCoversOnlyTheConfiguredThreeHundredMillisecondWindow() {
        assertFalse(AutoClickerPolicy.delayAfterBlockBreak(10_000L, Long.MIN_VALUE, true));
        assertFalse(AutoClickerPolicy.delayAfterBlockBreak(10_000L, 9_000L, false));
        assertTrue(AutoClickerPolicy.delayAfterBlockBreak(299_999_999L, 0L, true));
        assertFalse(AutoClickerPolicy.delayAfterBlockBreak(300_000_000L, 0L, true));
        assertFalse(AutoClickerPolicy.delayAfterBlockBreak(10_000L, 11_000L, true));
    }

    @Test
    void postUseDelayResumesAtTheConfiguredTickAndClampsNegativeInput() {
        assertEquals(107L, AutoClickerPolicy.postUseResumeTick(100L, 7));
        assertEquals(100L, AutoClickerPolicy.postUseResumeTick(100L, 0));
        assertEquals(100L, AutoClickerPolicy.postUseResumeTick(100L, -1));
    }
}
