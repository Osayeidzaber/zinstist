package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.ClickGuiScreen;
import dev.jilaton.client.gui.HudEditorScreen;
import dev.jilaton.client.gui.JilatonClickGui;
import dev.jilaton.client.gui.VisualStudioScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    void everyJilatonEditorScreenCountsAsEditorInput() {
        assertTrue(InventoryMovePolicy.isJilatonEditor(ClickGuiScreen.class));
        assertTrue(InventoryMovePolicy.isJilatonEditor(JilatonClickGui.class));
        assertTrue(InventoryMovePolicy.isJilatonEditor(HudEditorScreen.class));
        assertTrue(InventoryMovePolicy.isJilatonEditor(VisualStudioScreen.class));
    }

    // Screen's constructor reads Minecraft.getInstance().font (Screen.java:91), so no screen is instantiable headless.
    @Test
    void screensOutsideTheEditorSetAreNotEditorInput() {
        assertFalse(InventoryMovePolicy.isJilatonEditor(Screen.class));
        assertFalse(InventoryMovePolicy.isJilatonEditor(InventoryScreen.class));
        assertFalse(InventoryMovePolicy.isJilatonEditor(ChatScreen.class));
        assertFalse(InventoryMovePolicy.isJilatonEditor((Screen) null));
    }

    @Test
    void menuMovementDoesNotOverrideChatOrFocusedText() {
        assertTrue(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.SAFE, true, false, false, false));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.NORMAL, true, false, false, true));
        assertFalse(InventoryMovePolicy.shouldMove(InventoryMovePolicy.Mode.NORMAL, false, false, false, false));
    }
}
