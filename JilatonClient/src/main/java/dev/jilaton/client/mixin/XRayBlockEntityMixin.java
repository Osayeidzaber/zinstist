package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): block entities that XRay hides (chests, signs...) are not drawn either. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class XRayBlockEntityMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void jilaton$xraySkipHiddenBlockEntity(BlockEntity blockEntity, float partialTick,
                                                   ModelFeatureRenderer.CrumblingOverlay overlay,
                                                   CallbackInfoReturnable<BlockEntityRenderState> cir) {
        XRayModule.Snapshot snapshot = XRayModule.active;
        if (snapshot == null) return;
        Level level = blockEntity.getLevel();
        if (level != null && snapshot.alphaFor(level, blockEntity.getBlockState(), blockEntity.getBlockPos()) <= 0.0F) {
            cir.setReturnValue(null);
        }
    }
}
