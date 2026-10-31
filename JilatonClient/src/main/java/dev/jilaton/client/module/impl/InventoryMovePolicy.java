package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.ClickGuiScreen;
import dev.jilaton.client.gui.HudEditorScreen;
import dev.jilaton.client.gui.JilatonClickGui;
import dev.jilaton.client.gui.VisualStudioScreen;
import net.minecraft.client.gui.screens.Screen;

/** Pure screen-policy logic for the InventoryMove module. */
public final class InventoryMovePolicy {
    private InventoryMovePolicy() {
    }

    /** Full-screen Jilaton editors keep the movement keys even when no text field holds focus. */
    public static boolean isJilatonEditor(Screen screen) {
        return screen != null && isJilatonEditor(screen.getClass());
    }

    /** Class form: the covered set is pinned by a headless test, and constructing these screens loads fonts. */
    static boolean isJilatonEditor(Class<?> screenType) {
        return screenType == ClickGuiScreen.class
                || screenType == JilatonClickGui.class
                || screenType == HudEditorScreen.class
                || screenType == VisualStudioScreen.class;
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
