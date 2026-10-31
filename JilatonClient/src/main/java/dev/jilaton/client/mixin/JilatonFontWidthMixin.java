package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonType;
import dev.jilaton.client.gui.design.JilatonText;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Widths of Jilaton's Inter font come from the same metrics {@link JilatonText} draws with, so layouts line up. */
@Mixin(Font.class)
public abstract class JilatonFontWidthMixin {
    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void jilaton$string(String text, CallbackInfoReturnable<Integer> callback) {
        if (!JilatonText.handles((Font) (Object) this) || text == null) return;
        callback.setReturnValue((int) Math.ceil(JilatonText.width(List.of(new JilatonText.Segment(text, JilatonType.Face.REGULAR, 0)), 9)));
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void jilaton$formatted(FormattedText text, CallbackInfoReturnable<Integer> callback) {
        if (!JilatonText.handles((Font) (Object) this) || text == null) return;
        callback.setReturnValue((int) Math.ceil(JilatonText.width(JilatonText.segments(text), 9)));
    }

    @Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), cancellable = true)
    private void jilaton$sequence(FormattedCharSequence text, CallbackInfoReturnable<Integer> callback) {
        if (!JilatonText.handles((Font) (Object) this) || text == null) return;
        callback.setReturnValue((int) Math.ceil(JilatonText.width(JilatonText.segments(text, 0), 9)));
    }
}
