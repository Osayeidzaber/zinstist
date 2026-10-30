package dev.jilaton.client.module.impl;

/** Guards inventory quick-move refill actions to deliberate, safe inventory contexts. */
final class AutoBuffRefillPolicy {
    private AutoBuffRefillPolicy() {
    }

    static boolean shouldRefill(boolean enabled, boolean ownInventoryOpen, boolean inventoryMenuActive,
                                boolean cursorEmpty, boolean emptyHotbarSlot, boolean delayElapsed) {
        return enabled && ownInventoryOpen && inventoryMenuActive && cursorEmpty
                && emptyHotbarSlot && delayElapsed;
    }
}
