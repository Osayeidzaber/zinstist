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
    @org.spongepowered.asm.mixin.Shadow public abstract boolean isPanoramicMode();
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void jilaton$zoom(Camera camera, float partialTick, boolean worldProjection, CallbackInfoReturnable<Float> callback) {
        float fov = callback.getReturnValue();
        if (JilatonClient.moduleManager().get("custom_fov") instanceof dev.jilaton.client.module.impl.CustomFovModule custom)
            fov = custom.apply(fov, worldProjection, isPanoramicMode());
        if (worldProjection && !isPanoramicMode() && JilatonClient.moduleManager().get("zoom") instanceof ZoomModule zoom) {
            fov = zoom.applyFov(Minecraft.getInstance(), fov,
                    JilatonClient.clientConfig().reducedMotion());
        }
        callback.setReturnValue(fov);
    }
}
