package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): the same fade for terrain meshed by the Fabric Indigo renderer. */
@Mixin(value = AbstractTerrainRenderContext.class, remap = false)
public abstract class XRayIndigoMixin {
    @Inject(method = "bufferQuad", at = @At("HEAD"))
    private void jilaton$xrayBufferQuad(MutableQuadViewImpl quad, CallbackInfo ci) {
        if (XRayModule.active == null) return;
        float alpha = XRayModule.CURRENT_ALPHA.get()[0];
        if (alpha <= 0.0F || alpha >= 1.0F) return;
        quad.renderLayer(ChunkSectionLayer.TRANSLUCENT);
        for (int vertex = 0; vertex < 4; vertex++) quad.color(vertex, ARGB.multiplyAlpha(quad.color(vertex), alpha));
    }
}
