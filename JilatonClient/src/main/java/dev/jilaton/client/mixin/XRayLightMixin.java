package dev.jilaton.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): the lightmap is cleared to white, so blocks with no light reaching them are still fully lit. */
@Mixin(LightTexture.class)
public abstract class XRayLightMixin {
    @Shadow @Final private GpuTexture texture;

    @Inject(method = "updateLightTexture", cancellable = true, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LightTexture;updateLightTexture:Z", opcode = 181,
            shift = At.Shift.AFTER))
    private void jilaton$xrayFullbright(float partialTick, CallbackInfo ci) {
        if (XRayModule.active != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, -1);
            ci.cancel();
        }
    }
}
