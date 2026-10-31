package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightTexture.class)
public abstract class LightTextureFullbrightMixin {
    /**
     * The seventh float written into the lightmap uniform block is the effective gamma
     * ({@code max(0, options.gamma() - darkness)}); ordinal 6 targets exactly that one.
     */
    @ModifyArg(
            method = "updateLightTexture(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 6),
            index = 0)
    private float jilaton$fullbrightGamma(float gamma) {
        ClientModule module = JilatonClient.moduleManager().get("fullbright");
        if (module instanceof FullbrightModule fullbright && fullbright.enabled()) {
            return fullbright.lightmapGamma();
        }
        return gamma;
    }
}
