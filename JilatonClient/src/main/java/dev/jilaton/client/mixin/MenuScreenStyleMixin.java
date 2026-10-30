package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonBackdrop;
import dev.jilaton.client.gui.JilatonWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the panorama, blur and dirt menu texture with the Jilaton black canvas on every menu screen. */
@Mixin(Screen.class)
public abstract class MenuScreenStyleMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void jilaton$backdrop(GuiGraphics graphics, float partialTick, CallbackInfo callback) {
        JilatonBackdrop.render(graphics, width, height, JilatonBackdrop.clock(JilatonWidgets.reducedMotion()), 0, 0, 1);
        callback.cancel();
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void jilaton$noMenuBlur(GuiGraphics graphics, CallbackInfo callback) {
        if (Minecraft.getInstance().level == null) callback.cancel();
    }

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
    private void jilaton$menuBackground(GuiGraphics graphics, int x, int y, int w, int h, CallbackInfo callback) {
        if (Minecraft.getInstance().level != null) graphics.fill(x, y, x + w, y + h, 0xA8050505);
        callback.cancel();
    }
}
