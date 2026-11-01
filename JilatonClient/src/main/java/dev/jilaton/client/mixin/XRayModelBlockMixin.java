package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): faded blocks are written with the current alpha (vanilla model renderer path). */
@Mixin(ModelBlockRenderer.class)
public abstract class XRayModelBlockMixin {
    @ModifyArg(method = "putQuadData", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"),
            index = 6)
    private float jilaton$xrayAlpha(float alpha) {
        return XRayModule.active == null ? alpha : alpha * XRayModule.CURRENT_ALPHA.get()[0];
    }
}
