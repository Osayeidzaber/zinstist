package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.CustomSkyModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tints the horizon fog color toward the active sky theme while the camera is in open air. */
@Mixin(FogRenderer.class)
public abstract class CustomFogColorMixin {
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void jilaton$tintFog(Camera camera, float partial, ClientLevel level, int renderDistance, float darken,
                                 CallbackInfoReturnable<Vector4f> callback) {
        if (JilatonClient.moduleManager().get("custom_sky") instanceof CustomSkyModule sky && callback.getReturnValue() != null)
            sky.adjustFog(callback.getReturnValue(), camera);
    }
}
