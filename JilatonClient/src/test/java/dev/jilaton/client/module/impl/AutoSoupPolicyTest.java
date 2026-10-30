package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoSoupPolicyTest {
    @Test
    void permitsLowHealthSoupUseAndCanCountAbsorption() {
        assertTrue(AutoSoupPolicy.shouldUse(6.0F, 20.0F, 0.0F, 40.0, true, true, true, true));
        assertFalse(AutoSoupPolicy.shouldUse(7.0F, 20.0F, 2.0F, 40.0, true, true, true, true));
        assertTrue(AutoSoupPolicy.shouldUse(7.0F, 20.0F, 2.0F, 40.0, false, true, true, true));
        assertTrue(AutoSoupPolicy.shouldUse(14.0F, 40.0F, 0.0F, 40.0, true, true, true, true));
    }

    @Test
    void rejectsMissingFoodUnsafeContextsAndInvalidVitals() {
        assertFalse(AutoSoupPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, true, false, true, true));
        assertFalse(AutoSoupPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, true, true, false, true));
        assertFalse(AutoSoupPolicy.shouldUse(4.0F, 20.0F, 0.0F, 40.0, true, true, true, false));
        assertFalse(AutoSoupPolicy.shouldUse(Float.NaN, 20.0F, 0.0F, 40.0, true, true, true, true));
    }
}
