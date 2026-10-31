package dev.jilaton.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Jilaton's TTF faces (Inter, JetBrains Mono) are drawn from a raster snapped to their on-screen size
 * ({@link dev.jilaton.client.gui.JilatonType}). GUI text always samples its glyph atlas with NEAREST,
 * which drops whole pixel rows when a glyph shrinks even slightly and leaves small text broken and unreadable.
 * Glyphs from atlases of fonts in the {@code jilaton} namespace that are drawn scaled sample bilinearly instead;
 * 1:1 glyphs and vanilla's pixel font keep their sharp NEAREST sampling.
 */
@Mixin(GlyphRenderState.class)
public abstract class GlyphSamplerMixin {
    @Shadow @Final private TextRenderable renderable;
    @Shadow @Final private org.joml.Matrix3x2fc pose;

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void jilaton$smoothOwnFonts(CallbackInfoReturnable<TextureSetup> callback) {
        GpuTextureView view = renderable.textureView();
        float oversample = dev.jilaton.client.gui.design.JilatonAtlases.oversample(view);
        if (oversample <= 0) return;
        // A glyph drawn 1:1 with its raster stays on NEAREST, which is the sharpest; only a glyph that is really
        // scaled (a title drawn at an arbitrary size) is filtered, so it shrinks smoothly instead of dropping rows.
        double onScreen = Math.abs(pose.m00()) * net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        if (Math.abs(onScreen / oversample - 1) < 0.04) return;
        callback.setReturnValue(TextureSetup.singleTextureWithLightmap(view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
    }
}
