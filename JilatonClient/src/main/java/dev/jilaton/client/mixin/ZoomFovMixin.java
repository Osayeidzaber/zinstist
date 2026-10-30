package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ZoomFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void jilaton$zoom(Camera camera, float partialTick, boolean worldProjection, CallbackInfoReturnable<Float> callback) {
        if (worldProjection && JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom) {
            callback.setReturnValue(zoom.applyFov(Minecraft.getInstance(), callback.getReturnValue(),
                    JilatonClient.clientConfig().reducedMotion()));
        }
    }
}
