package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.CustomSkyModule;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla cloud deck while the Sky module draws its own horizon clouds. */
@Mixin(CloudRenderer.class)
public abstract class CustomCloudMixin {
    @Inject(method = "render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V",
            at = @At("HEAD"), cancellable = true)
    private void jilaton$hideClouds(int color, CloudStatus status, float ticks, Vec3 camera, long time, float partial,
                                    CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("custom_sky") instanceof CustomSkyModule sky && sky.hideVanillaClouds())
            callback.cancel();
    }
}
