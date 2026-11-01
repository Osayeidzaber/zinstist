package dev.jilaton.client.gui.design;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.mixin.OverlayPipelineAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * GPU shader backgrounds for Jilaton screens ({@code assets/jilaton/shaders/core/ui_backdrop.fsh}). A custom GUI
 * pipeline runs the fragment shader over a full-screen blit; per-frame inputs (time, cursor, accent, mode,
 * quality, aspect) go through a 4x1 dynamic texture bound as Sampler0, because GUI blits have no custom
 * uniforms. The texture is created on first use and released with {@link #release}. If the shader fails to load
 * the screen simply shows the Kit base colour drawn underneath.
 */
public final class ShaderBackdrop {
    public static final List<String> MODES = List.of("Dark Veil", "Aurora", "Threads", "Beams", "Galaxy", "Liquid Chrome");
    private static final Identifier DATA = Identifier.fromNamespaceAndPath("jilaton", "ui_backdrop_data");
    private static final long EPOCH = System.nanoTime();
    private static RenderPipeline pipeline;
    private static DynamicTexture data;
    private static boolean failed;
    private static double mouseX = 0.5, mouseY = 0.5;

    private ShaderBackdrop() {
    }

    private static RenderPipeline pipeline() {
        if (pipeline == null) {
            pipeline = OverlayPipelineAccess.registerPipeline(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("jilaton", "pipeline/ui_backdrop"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("jilaton", "core/ui_backdrop"))
                    .build());
        }
        return pipeline;
    }

    public static boolean isShaderMode(String mode) {
        return MODES.contains(mode);
    }

    /**
     * Draws {@code mode} over the {@code width}x{@code height} GUI area. The cursor eases toward
     * ({@code mx}, {@code my}) so the light follows it softly.
     */
    public static boolean draw(GuiGraphics g, String mode, int width, int height, double mx, double my) {
        if (failed || width <= 0 || height <= 0) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (data == null) {
                data = new DynamicTexture(() -> "jilaton ui backdrop data", 4, 1, false);
                mc.getTextureManager().register(DATA, data);
            }
            boolean reduced = Kit.reduced();
            double ease = reduced ? 1 : 0.08;
            mouseX += (Math.max(0, Math.min(1, mx / width)) - mouseX) * ease;
            mouseY += (Math.max(0, Math.min(1, my / height)) - mouseY) * ease;
            long ms = reduced ? 20_000 : ((System.nanoTime() - EPOCH) / 1_000_000L) % 4_000_000L;
            double speed = JilatonClient.clientConfig() == null ? 1 : JilatonClient.clientConfig().transitionSpeed();
            ms = (long) (ms * Math.max(0.25, speed));
            NativeImage px = data.getPixels();
            px.setPixel(0, 0, abgr((int) (ms >>> 24) & 255, (int) (ms >>> 16) & 255, (int) (ms >>> 8) & 255, (int) ms & 255));
            int mxi = (int) Math.round(mouseX * 65535), myi = (int) Math.round(mouseY * 65535);
            px.setPixel(1, 0, abgr(mxi >>> 8, mxi & 255, myi >>> 8, myi & 255));
            int accent = Kit.accent();
            double intensity = JilatonClient.clientConfig() == null ? 1 : JilatonClient.clientConfig().shaderIntensity();
            px.setPixel(2, 0, abgr((accent >> 16) & 255, (accent >> 8) & 255, accent & 255, (int) Math.round(Math.min(1, intensity) * 255)));
            int aspect = (int) Math.min(65535, Math.round(width / (double) height * 4096));
            boolean high = JilatonClient.clientConfig() == null || JilatonClient.clientConfig().fullEffects();
            px.setPixel(3, 0, abgr(Math.max(0, MODES.indexOf(mode)), high ? 1 : 0, aspect >>> 8, aspect & 255));
            data.upload();
            g.blit(pipeline(), DATA, 0, 0, 0, 0, width, height, width, height);
            return true;
        } catch (RuntimeException failure) {
            failed = true;
            return false;
        }
    }

    /** Packs RGBA bytes as the ARGB int {@link NativeImage#setPixel} takes. */
    private static int abgr(int r, int g, int b, int a) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static void release() {
        if (data != null) {
            Minecraft.getInstance().getTextureManager().release(DATA);
            data = null;
        }
    }
}
