package dev.jilaton.client.module.impl;

final class OffhandTotemPolicy {
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_SIZE = 36;

    private OffhandTotemPolicy() {
    }

    /** Mirrors Anubis's source policy: stored stacks by default, hotbar first when opted in. */
    static int findSource(boolean[] slots, boolean useHotbar) {
        if (slots == null || slots.length < INVENTORY_SIZE) {
            throw new IllegalArgumentException("Expected all 36 player inventory slots");
        }
        if (useHotbar) {
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                if (slots[slot]) {
                    return slot;
                }
            }
        }
        for (int slot = HOTBAR_SIZE; slot < INVENTORY_SIZE; slot++) {
            if (slots[slot]) {
                return slot;
            }
        }
        return -1;
    }
}
