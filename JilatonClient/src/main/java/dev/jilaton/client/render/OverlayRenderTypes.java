package dev.jilaton.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jilaton.client.mixin.OverlayPipelineAccess;
import dev.jilaton.client.mixin.OverlayRenderTypeAccess;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Four persistent layers, using Minecraft's native line expansion and position/color shaders. */
final class OverlayRenderTypes {
    static final RenderType FILL_DEPTH = create(false, false);
    static final RenderType FILL_VISIBLE = create(false, true);
    static final RenderType LINE_DEPTH = create(true, false);
    static final RenderType LINE_VISIBLE = create(true, true);

    /**
     * Textured skybox pipeline: vanilla's position/uv/color shader without fog, depth-tested against terrain.
     * Screen blending keeps black pixels neutral, so the pack's opaque sky layers stack without covering the
     * vanilla sun, moon or terrain.
     */
    private static final RenderPipeline SKY_SCREEN = skyPipeline();
    private static final Map<String, RenderType> SKY = new HashMap<>();

    private OverlayRenderTypes() { }

    private static RenderType create(boolean line, boolean throughWalls) {
        String name = "jilaton/" + (line ? "outline" : "surface") + (throughWalls ? "_overlay" : "_depth");
        RenderPipeline pipeline = OverlayPipelineAccess.registerPipeline(RenderPipeline.builder(
                        line ? OverlayPipelineAccess.linesSnippet() : OverlayPipelineAccess.filledSnippet())
                .withLocation(OverlayPipelineIds.of(line, throughWalls)).withCull(false).withDepthWrite(false)
                .withVertexFormat(line ? DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH : DefaultVertexFormat.POSITION_COLOR,
                        line ? VertexFormat.Mode.LINES : VertexFormat.Mode.QUADS)
                .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT).build());
        return OverlayRenderTypeAccess.createOverlay(name, RenderSetup.builder(pipeline).bufferSize(65536).createRenderSetup());
    }

    private static RenderPipeline skyPipeline() {
        return OverlayPipelineAccess.registerPipeline(RenderPipeline.builder(OverlayPipelineAccess.matricesProjectionSnippet())
                .withLocation(OverlayPipelineIds.sky())
                .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withCull(false).withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_COLOR,
                        SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).build());
    }

    static RenderType sky(Identifier texture) {
        String name = "jilaton/sky/" + texture.getPath().replace('/', '_').replace(".png", "");
        return SKY.computeIfAbsent(name, key -> OverlayRenderTypeAccess.createOverlay(key, RenderSetup.builder(SKY_SCREEN)
                .withTexture("Sampler0", texture).bufferSize(16384).createRenderSetup()));
    }

    static RenderType fill(boolean walls) { return walls ? FILL_VISIBLE : FILL_DEPTH; }
    static RenderType line(boolean walls) { return walls ? LINE_VISIBLE : LINE_DEPTH; }
}
