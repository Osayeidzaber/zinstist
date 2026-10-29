package dev.jilaton.client.module.impl;

/** Pure screen-policy logic for the InventoryMove module. */
public final class InventoryMovePolicy {
    private InventoryMovePolicy() {
    }

    public static boolean shouldMove(Mode mode, boolean screenOpen, boolean containerScreen,
                                     boolean personalInventory, boolean textInputFocused) {
        if (mode == null || !screenOpen || textInputFocused) {
            return false;
        }
        if (!containerScreen || mode == Mode.NORMAL) {
            return true;
        }
        return mode == Mode.SAFE && personalInventory;
    }

    public enum Mode {
        NORMAL,
        SAFE,
        UNDETECTABLE
    }
}
