package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBuffRefillPolicyTest {
    @Test
    void requiresAnOpenInventoryMenuAndAnEmptyCursorAndHotbarSlot() {
        assertFalse(shouldRefill(false, true, true, true, true, true));
        assertFalse(shouldRefill(true, false, true, true, true, true));
        assertFalse(shouldRefill(true, true, false, true, true, true));
        assertFalse(shouldRefill(true, true, true, false, true, true));
        assertFalse(shouldRefill(true, true, true, true, false, true));
        assertTrue(shouldRefill(true, true, true, true, true, true));
    }

    @Test
    void respectsTheConfiguredActionDelay() {
        assertFalse(shouldRefill(true, true, true, true, true, false));
        assertTrue(shouldRefill(true, true, true, true, true, true));
    }

    private static boolean shouldRefill(boolean enabled, boolean screenOpen, boolean menuActive,
                                        boolean cursorEmpty, boolean emptyHotbar, boolean delayElapsed) {
        return AutoBuffRefillPolicy.shouldRefill(enabled, screenOpen, menuActive,
                cursorEmpty, emptyHotbar, delayElapsed);
    }
}
