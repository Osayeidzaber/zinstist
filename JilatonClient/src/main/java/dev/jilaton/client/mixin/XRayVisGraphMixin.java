package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): no section occlusion culling, so sections behind solid rock are still meshed. */
@Mixin(VisGraph.class)
public abstract class XRayVisGraphMixin {
    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void jilaton$noOcclusion(BlockPos pos, CallbackInfo ci) {
        if (XRayModule.active != null) ci.cancel();
    }
}
