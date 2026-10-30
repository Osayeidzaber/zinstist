package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.impl.CustomCrosshairModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class CustomCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void jilaton$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.player.isSpectator()
                || client.getDebugOverlay().showDebugScreen()) return;
        if (JilatonClient.moduleManager().get("custom_crosshair") instanceof CustomCrosshairModule crosshair && crosshair.enabled()) {
            var config = JilatonClient.clientConfig();
            crosshair.draw(graphics, client, delta.getGameTimeDeltaPartialTick(false), ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
            callback.cancel();
        }
    }
}
