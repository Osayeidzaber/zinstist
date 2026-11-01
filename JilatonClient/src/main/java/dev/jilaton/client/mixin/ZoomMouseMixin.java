package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.ZoomModule;
import dev.jilaton.client.module.impl.AimAssistModule;
import com.llamalad7.mixinextras.sugar.Local;
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
            return;
        }
        if (window == client.getWindow().handle() && client.player != null && client.screen == null && client.isWindowActive()
                && JilatonClient.moduleManager().get("freecam") instanceof dev.jilaton.client.module.impl.FreecamModule camera
                && camera.scroll(client, vertical)) callback.cancel();
    }

    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void jilaton$zoomSensitivity(Args args, @Local(argsOnly = true) double movementTime) {
        Minecraft client = Minecraft.getInstance();
        if (JilatonClient.moduleManager().get("freecam") instanceof dev.jilaton.client.module.impl.FreecamModule camera) {
            double scale = JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom ? zoom.sensitivityFactor() : 1;
            if (camera.turn(client, (double) args.get(0) * scale, (double) args.get(1) * scale)) {
                args.set(0, 0.0); args.set(1, 0.0); return;
            }
        }
        if (JilatonClient.moduleManager().get("free_look") instanceof dev.jilaton.client.module.impl.FreeLookModule look
                && look.turn(client, (double) args.get(0), (double) args.get(1))) {
            args.set(0, 0.0);
            args.set(1, 0.0);
            return;
        }
        if (client.player != null && client.screen == null && !client.player.isScoping()
                && JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom) {
            double factor = zoom.sensitivityFactor();
            args.set(0, (double) args.get(0) * factor);
            args.set(1, (double) args.get(1) * factor);
        }
        if (client.player != null && JilatonClient.moduleManager().get("aim_assist") instanceof AimAssistModule assist) {
            double yaw = args.get(0), pitch = args.get(1);
            var nudge = assist.nudge(client.player, yaw, pitch, movementTime);
            args.set(0, yaw + nudge.yaw());
            args.set(1, pitch + nudge.pitch());
        }
    }
}
