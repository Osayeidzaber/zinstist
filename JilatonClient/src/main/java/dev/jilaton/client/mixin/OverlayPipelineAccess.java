package dev.jilaton.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface OverlayPipelineAccess {
    @Accessor("LINES_SNIPPET") static RenderPipeline.Snippet linesSnippet() { throw new AssertionError(); }
    @Accessor("DEBUG_FILLED_SNIPPET") static RenderPipeline.Snippet filledSnippet() { throw new AssertionError(); }
    @Invoker("register") static RenderPipeline registerPipeline(RenderPipeline pipeline) { throw new AssertionError(); }
}
