package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): whitelisted opaque blocks draw every face, so ores show their full shape inside hidden terrain. */
@Mixin(Block.class)
public abstract class XRayBlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void jilaton$xrayForceFace(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        XRayModule.Snapshot snapshot = XRayModule.active;
        if (snapshot != null && snapshot.isWhitelisted(state.getBlock()) && XRayModule.CURRENT_ALPHA.get()[0] >= 1.0F) {
            cir.setReturnValue(true);
        }
    }
}
