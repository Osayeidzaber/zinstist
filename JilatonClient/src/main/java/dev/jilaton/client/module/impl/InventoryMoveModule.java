/*
 * Adapted from LiquidBounce's ModuleInventoryMove screen policy.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source artifact: installed LiquidBounce.jar, SHA-256 6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898.
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Input;

import java.util.List;

public final class InventoryMoveModule extends ClientModule {
    private final ChoiceSetting behavior;
    private final BooleanSetting passthroughSneak;

    public InventoryMoveModule() {
        super("inventory_move", "InventoryMove",
                "Allows movement through supported screens without capturing text or Jilaton editor input.",
                ModuleCategory.MOVEMENT);
        behavior = addSetting(new ChoiceSetting("behavior", "Behavior",
                List.of("Normal", "Safe", "Undetectable"), "Safe"));
        passthroughSneak = addSetting(new BooleanSetting("passthrough_sneak", "Pass through sneak key", false));
    }

    @Override
    protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null || client.screen == null) {
            return;
        }

        Screen screen = client.screen;
        boolean container = screen instanceof AbstractContainerScreen<?>;
        boolean personalInventory = screen instanceof InventoryScreen;
        boolean textInput = screen instanceof ChatScreen
                || InventoryMovePolicy.isJilatonEditor(screen)
                || screen.getFocused() instanceof EditBox;
        InventoryMovePolicy.Mode mode = InventoryMovePolicy.Mode.valueOf(behavior.value().toUpperCase(java.util.Locale.ROOT));
        if (!InventoryMovePolicy.shouldMove(mode, true, container, personalInventory, textInput)) {
            return;
        }

        Input mapped = new Input(client.options.keyUp.isDown(), client.options.keyDown.isDown(),
                client.options.keyLeft.isDown(), client.options.keyRight.isDown(), client.options.keyJump.isDown(),
                passthroughSneak.value() && client.options.keyShift.isDown(), client.options.keySprint.isDown());
        Input previous = event.input();
        if (!sameInput(previous, mapped)) {
            event.setInput(mapped);
        }
    }

    private static boolean sameInput(Input first, Input second) {
        return first.forward() == second.forward()
                && first.backward() == second.backward()
                && first.left() == second.left()
                && first.right() == second.right()
                && first.jump() == second.jump()
                && first.shift() == second.shift()
                && first.sprint() == second.sprint();
    }

    public ChoiceSetting behavior() {
        return behavior;
    }

    public BooleanSetting passthroughSneak() {
        return passthroughSneak;
    }
}
