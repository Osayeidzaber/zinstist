package dev.jilaton.client.mixin.screenshot;

import dev.jilaton.client.screenshot.ScreenshotChatActions;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * Decorates the message vanilla delivers for every screenshot (both grab overloads funnel into the
 * five-argument one) so the chat line carries the [Copy] and [Open] buttons.
 */
@Mixin(Screenshot.class)
public abstract class ScreenshotGrabMixin {
    @ModifyVariable(
            method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, index = 4)
    private static Consumer<Component> jilaton$decorateScreenshotMessage(Consumer<Component> consumer) {
        return ScreenshotChatActions.decorating(consumer);
    }
}
