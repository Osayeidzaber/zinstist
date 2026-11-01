package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jilaton.client.module.impl.XRayModule;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay hook ported from Anubis Client (GPL-3.0, Horcus and 4ldenz): each block read while a section compiles gets its alpha; hidden blocks become air and faded ones move to the translucent layer. */
@Mixin(SectionCompiler.class)
public abstract class XRaySectionCompilerMixin {
    @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState jilaton$xrayBlockState(RenderSectionRegion region, BlockPos pos, Operation<BlockState> original) {
        BlockState state = original.call(region, pos);
        XRayModule.Snapshot snapshot = XRayModule.active;
        if (snapshot == null || state.isAir()) return state;
        float[] alphaSlot = XRayModule.CURRENT_ALPHA.get();
        float alpha = snapshot.alphaFor(region, state, pos);
        alphaSlot[0] = alpha;
        return alpha <= 0.0F ? Blocks.AIR.defaultBlockState() : state;
    }

    @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"))
    private ChunkSectionLayer jilaton$xrayBlockLayer(BlockState state, Operation<ChunkSectionLayer> original) {
        if (XRayModule.active == null) return original.call(state);
        float alpha = XRayModule.CURRENT_ALPHA.get()[0];
        return alpha > 0.0F && alpha < 1.0F ? ChunkSectionLayer.TRANSLUCENT : original.call(state);
    }

    @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"))
    private ChunkSectionLayer jilaton$xrayFluidLayer(FluidState fluidState, Operation<ChunkSectionLayer> original) {
        if (XRayModule.active == null) return original.call(fluidState);
        float alpha = XRayModule.CURRENT_ALPHA.get()[0];
        return alpha > 0.0F && alpha < 1.0F ? ChunkSectionLayer.TRANSLUCENT : original.call(fluidState);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void jilaton$xrayResetAlpha(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting,
                                        SectionBufferBuilderPack bufferPack, CallbackInfoReturnable<SectionCompiler.Results> cir) {
        XRayModule.CURRENT_ALPHA.get()[0] = 1.0F;
    }
}
