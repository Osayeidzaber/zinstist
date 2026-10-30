package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoReplenishPolicyTest {
    @Test
    void appliesConfiguredRefillThreshold() {
        assertTrue(AutoReplenishPolicy.needsRefill(0, 8));
        assertTrue(AutoReplenishPolicy.needsRefill(8, 8));
        assertFalse(AutoReplenishPolicy.needsRefill(9, 8));
    }

    @Test
    void onlyChoosesSourcesWithMoreItemsThanTheTarget() {
        assertTrue(AutoReplenishPolicy.sourceImproves(32, 8));
        assertFalse(AutoReplenishPolicy.sourceImproves(8, 8));
        assertFalse(AutoReplenishPolicy.sourceImproves(4, 8));
    }

    @Test
    void doesNotOverwriteAStackThatChangedDuringADeferredRefill() {
        assertTrue(AutoReplenishPolicy.targetNeedsRefill(true, false, 0, 8));
        assertTrue(AutoReplenishPolicy.targetNeedsRefill(false, true, 4, 8));
        assertFalse(AutoReplenishPolicy.targetNeedsRefill(false, false, 4, 8));
        assertFalse(AutoReplenishPolicy.targetNeedsRefill(false, true, 9, 8));
    }

    @Test
    void mapsHotbarAndOffhandSwapButtonsAndRejectsOtherSlots() {
        assertEquals(0, AutoReplenishPolicy.swapButton(0));
        assertEquals(8, AutoReplenishPolicy.swapButton(8));
        assertEquals(40, AutoReplenishPolicy.swapButton(9));
        assertThrows(IllegalArgumentException.class, () -> AutoReplenishPolicy.swapButton(10));
    }

    @Test
    void rejectsInvalidCountsAndThresholds() {
        assertThrows(IllegalArgumentException.class, () -> AutoReplenishPolicy.needsRefill(-1, 8));
        assertThrows(IllegalArgumentException.class, () -> AutoReplenishPolicy.needsRefill(2, 0));
    }
}
