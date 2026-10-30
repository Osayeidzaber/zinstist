package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class ZoomMouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void jilaton$zoomScroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && client.player != null && client.screen == null
                && client.isWindowActive() && !client.player.isScoping()
                && JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom && zoom.scroll(vertical)) {
            callback.cancel();
        }
    }

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void jilaton$zoomSensitivity(Args args) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.screen == null && !client.player.isScoping()
                && JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom) {
            double factor = zoom.sensitivityFactor();
            args.set(0, (double) args.get(0) * factor);
            args.set(1, (double) args.get(1) * factor);
        }
    }
}
