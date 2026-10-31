package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.design.JilatonAtlases;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Records the glyph atlases of fonts in the {@code jilaton} namespace (see {@link JilatonAtlases}). */
@Mixin(FontTexture.class)
public abstract class FontTextureRegistryMixin extends AbstractTexture {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void jilaton$register(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored, CallbackInfo callback) {
        String name = String.valueOf(label.get());
        // Atlas labels are "<font id>/<page>", for example "jilaton:ui_o1h/0".
        if (name.startsWith("jilaton:")) {
            int page = name.lastIndexOf('/');
            JilatonAtlases.register(textureView, page > 0 ? name.substring(0, page) : name);
        }
    }
}
