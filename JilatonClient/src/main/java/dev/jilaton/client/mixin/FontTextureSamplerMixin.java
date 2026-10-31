package dev.jilaton.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Jilaton's TTF faces (Inter, JetBrains Mono) are drawn scaled from a raster picked to sit at or just above the
 * on-screen size ({@link dev.jilaton.client.gui.JilatonType}). Vanilla samples every glyph atlas with NEAREST, which
 * drops whole pixel rows when a glyph shrinks even slightly and makes small text look broken. Atlases for fonts in
 * the {@code jilaton} namespace sample bilinearly instead; vanilla's pixel font keeps its sharp NEAREST sampling.
 */
@Mixin(FontTexture.class)
public abstract class FontTextureSamplerMixin extends AbstractTexture {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void jilaton$smoothOwnFonts(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored,
                                         CallbackInfo callback) {
        String name = String.valueOf(label.get());
        if (name.startsWith("jilaton:")) sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
    }
}
