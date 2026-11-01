package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.SmoothCameraModule;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class SmoothCameraMixin {
    @Shadow public abstract Vec3 position();
    @Shadow public abstract boolean isDetached();
    @Shadow protected abstract void setPosition(Vec3 position);

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
    private float jilaton$distance(float zoom) {
        return zoom * SmoothCameraModule.zoomScale();
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void jilaton$smooth(CallbackInfo ci) {
        Vec3 next = SmoothCameraModule.follow(position(), isDetached());
        if (next != null) setPosition(next);
    }
}
