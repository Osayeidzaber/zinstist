package dev.jilaton.client.module.impl;

/**
 * Ownership rule for the hotbar selection a consume module (AutoGapple, AutoEat) swaps in.
 *
 * <p>The server stops a main-hand item use when the carried slot changes
 * ({@code ServerGamePacketListenerImpl#handleSetCarriedItem} calls {@code stopUsingItem}), so the
 * per-tick path waits for the consumption to finish before restoring. Disabling the module is
 * different: no tick runs afterwards, so a restore deferred to the end of the use would never
 * happen and the player would keep the swapped selection. Disabling therefore restores immediately,
 * which also ends the in-flight automatic use.</p>
 */
final class HotbarSlotRestore {
    private HotbarSlotRestore() {
    }

    /** True while the selection still belongs to the module and must be put back. */
    static boolean shouldRestore(int selectedSlot, int ownedSlot, int previousSlot) {
        return previousSlot >= 0 && ownedSlot >= 0 && selectedSlot == ownedSlot;
    }
}
