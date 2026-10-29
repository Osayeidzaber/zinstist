package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMovePolicyTest {
    @Test
    void normalAllowsContainerMovementButSafeOnlyAllowsPersonalInventory() {
        assertTrue(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.NORMAL, true, true, false, false));
        assertTrue(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.SAFE, true, true, true, false));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.SAFE, true, true, false, false));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.UNDETECTABLE, true, true, true, false));
    }

    @Test
    void menuMovementDoesNotOverrideChatOrFocusedText() {
        assertTrue(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.SAFE, true, false, false, false));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.NORMAL, true, false, false, true));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.NORMAL, false, false, false, false));
    }
}
