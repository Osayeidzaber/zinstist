package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonType;
import dev.jilaton.client.gui.design.JilatonText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Text drawn with Jilaton's Inter font goes through {@link JilatonText}, which rasterises it at its exact on-screen
 * size and places it on whole pixels, instead of Minecraft's scaled TTF glyph atlas. Every other font is untouched.
 */
@Mixin(GuiGraphics.class)
public abstract class JilatonTextMixin {
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void jilaton$string(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo callback) {
        if (!JilatonText.handles(font) || text == null) return;
        JilatonText.draw((GuiGraphics) (Object) this, List.of(new JilatonText.Segment(text, JilatonType.Face.REGULAR, color)),
                x, y, 9, shadow);
        callback.cancel();
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD"), cancellable = true)
    private void jilaton$sequence(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow,
                                  CallbackInfo callback) {
        if (!JilatonText.handles(font) || text == null) return;
        JilatonText.draw((GuiGraphics) (Object) this, JilatonText.segments(text, color), x, y, 9, shadow);
        callback.cancel();
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            at = @At("HEAD"), cancellable = true)
    private void jilaton$component(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfo callback) {
        if (!JilatonText.handles(font) || text == null) return;
        JilatonText.draw((GuiGraphics) (Object) this, JilatonText.segments(text.getVisualOrderText(), color), x, y, 9, shadow);
        callback.cancel();
    }
}
