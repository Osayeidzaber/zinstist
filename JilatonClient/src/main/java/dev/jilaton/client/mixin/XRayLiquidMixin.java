package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): water and lava fade with the rest of the terrain. */
@Mixin(LiquidBlockRenderer.class)
public abstract class XRayLiquidMixin {
    @ModifyArg(method = "vertex", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 3)
    private float jilaton$xrayFluidAlpha(float alpha) {
        return XRayModule.active == null ? alpha : alpha * XRayModule.CURRENT_ALPHA.get()[0];
    }
}
