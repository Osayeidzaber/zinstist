package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.HudInputMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class HudMouseInputMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void jilaton$countPhysicalClick(long window, MouseButtonInfo button, int action, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && client.player != null && client.screen == null
                && client.isWindowActive() && action == GLFW.GLFW_PRESS) {
            HudInputMetrics.press(button.button());
        }
    }
}
