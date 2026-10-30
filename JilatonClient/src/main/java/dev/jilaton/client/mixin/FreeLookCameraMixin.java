package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreeLookModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    // The first two calls initialize rotation on the minecart and ordinary paths.
    // The third call is vanilla's front-view inversion and must remain untouched.
    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void jilaton$minecartOrbit(Args args) { jilaton$orbit(args); }

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
    private void jilaton$ordinaryOrbit(Args args) { jilaton$orbit(args); }

    @org.spongepowered.asm.mixin.Unique
    private void jilaton$orbit(Args args) {
        if (JilatonClient.moduleManager().get("free_look") instanceof FreeLookModule look
                && look.active(Minecraft.getInstance())) {
            args.set(0, look.yaw());
            args.set(1, look.pitch());
        }
    }
}
