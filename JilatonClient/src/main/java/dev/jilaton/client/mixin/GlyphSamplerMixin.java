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
 * Jilaton's TTF faces (Inter, JetBrains Mono) are drawn scaled from a raster picked to sit at or just above the
 * on-screen size ({@link dev.jilaton.client.gui.JilatonType}). GUI text always samples its glyph atlas with NEAREST,
 * which drops whole pixel rows when a glyph shrinks even slightly and leaves small text broken and unreadable.
 * Glyphs from atlases of fonts in the {@code jilaton} namespace sample bilinearly instead; vanilla's pixel font keeps
 * its sharp NEAREST sampling.
 */
@Mixin(GlyphRenderState.class)
public abstract class GlyphSamplerMixin {
    @Shadow @Final private TextRenderable renderable;

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void jilaton$smoothOwnFonts(CallbackInfoReturnable<TextureSetup> callback) {
        GpuTextureView view = renderable.textureView();
        if (!dev.jilaton.client.gui.design.JilatonAtlases.isJilaton(view)) return;
        callback.setReturnValue(TextureSetup.singleTextureWithLightmap(view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
    }
}
