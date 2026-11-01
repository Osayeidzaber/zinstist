package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): no ambient occlusion, so ores buried in stone are not shaded black. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XRayShadeMixin {
    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void jilaton$xrayNoAmbientOcclusion(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (XRayModule.active != null) cir.setReturnValue(1.0F);
    }
}
