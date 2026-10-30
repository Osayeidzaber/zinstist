package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.CustomSkyModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Sky module steer vanilla's extracted sky state and suppress the vanilla sun and moon. */
@Mixin(SkyRenderer.class)
public abstract class CustomSkyMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jilaton$adjustSky(ClientLevel level, float partial, Camera camera, SkyRenderState state, CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("custom_sky") instanceof CustomSkyModule sky) sky.adjust(state);
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void jilaton$hideCelestial(CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("custom_sky") instanceof CustomSkyModule sky && sky.hideVanillaCelestial())
            callback.cancel();
    }
}
