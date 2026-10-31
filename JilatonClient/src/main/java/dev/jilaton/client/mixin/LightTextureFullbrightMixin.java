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
     *
     * <p>{@code com.mojang.blaze3d.buffers.Std140Builder} is shipped unobfuscated (it keeps this
     * exact name in the intermediary jar), so the target must not be remapped. Without
     * {@code remap = false} the mixin annotation processor emits
     * "Unable to locate method mapping for @At(INVOKE.<target>)", leaves the target out of the
     * refmap and gives no guarantee that the production remap still matches. The explicit
     * {@code require = 1} turns a future layout change into a boot-time error instead of a
     * silently missing feature.
     */
    @ModifyArg(
            method = "updateLightTexture(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 6,
                    remap = false),
            index = 0,
            require = 1)
    private float jilaton$fullbrightGamma(float gamma) {
        ClientModule module = JilatonClient.moduleManager().get("fullbright");
        if (module instanceof FullbrightModule fullbright && fullbright.enabled()) {
            return fullbright.lightmapGamma();
        }
        return gamma;
    }
}
