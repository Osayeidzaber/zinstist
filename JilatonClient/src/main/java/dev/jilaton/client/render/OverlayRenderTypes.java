package dev.jilaton.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jilaton.client.mixin.OverlayPipelineAccess;
import dev.jilaton.client.mixin.OverlayRenderTypeAccess;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Four persistent layers, using Minecraft's native line expansion and position/color shaders. */
final class OverlayRenderTypes {
    static final RenderType FILL_DEPTH = create(false, false);
    static final RenderType FILL_VISIBLE = create(false, true);
    static final RenderType LINE_DEPTH = create(true, false);
    static final RenderType LINE_VISIBLE = create(true, true);

    private OverlayRenderTypes() { }

    private static RenderType create(boolean line, boolean throughWalls) {
        String name = "jilaton/" + (line ? "outline" : "surface") + (throughWalls ? "_overlay" : "_depth");
        RenderPipeline pipeline = OverlayPipelineAccess.registerPipeline(RenderPipeline.builder(
                        line ? OverlayPipelineAccess.linesSnippet() : OverlayPipelineAccess.filledSnippet())
                .withLocation("jilaton:" + name).withCull(false).withDepthWrite(false)
                .withVertexFormat(line ? DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH : DefaultVertexFormat.POSITION_COLOR,
                        line ? VertexFormat.Mode.LINES : VertexFormat.Mode.QUADS)
                .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT).build());
        return OverlayRenderTypeAccess.createOverlay(name, RenderSetup.builder(pipeline).bufferSize(65536).createRenderSetup());
    }

    static RenderType fill(boolean walls) { return walls ? FILL_VISIBLE : FILL_DEPTH; }
    static RenderType line(boolean walls) { return walls ? LINE_VISIBLE : LINE_DEPTH; }
}
