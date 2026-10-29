package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoMissCooldownPolicyTest {
    @Test
    void onlyRemovesMissTimeWhenTheModuleAndSettingAreEnabled() {
        assertEquals(10, NoMissCooldownPolicy.missTime(false, true, 10));
        assertEquals(10, NoMissCooldownPolicy.missTime(true, false, 10));
        assertEquals(0, NoMissCooldownPolicy.missTime(true, true, 10));
        assertTrue(NoMissCooldownPolicy.allowMissTimeWrite(false, true));
        assertTrue(NoMissCooldownPolicy.allowMissTimeWrite(true, false));
        assertFalse(NoMissCooldownPolicy.allowMissTimeWrite(true, true));
    }

    @Test
    void missClicksAreConsumedOnlyWhenExplicitlyConfigured() {
        assertFalse(NoMissCooldownPolicy.consumeMiss(false, true));
        assertFalse(NoMissCooldownPolicy.consumeMiss(true, false));
        assertTrue(NoMissCooldownPolicy.consumeMiss(true, true));
    }
}
