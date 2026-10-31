package dev.jilaton.client.mixin.screenshot;

import dev.jilaton.client.screenshot.ScreenshotChatActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Answers the [Copy] click before vanilla's default handling runs: {@code defaultHandleGameClickEvent}
 * would otherwise send the custom click to the server as a {@code ServerboundCustomClickActionPacket}.
 */
@Mixin(Screen.class)
public abstract class ScreenshotClickMixin {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void jilaton$copyScreenshot(ClickEvent event, Minecraft minecraft, Screen screen, CallbackInfo ci) {
        if (event instanceof ClickEvent.Custom custom && ScreenshotChatActions.COPY_ID.equals(custom.id())) {
            ci.cancel();
            ScreenshotChatActions.handleCopy(minecraft, custom);
        }
    }
}
