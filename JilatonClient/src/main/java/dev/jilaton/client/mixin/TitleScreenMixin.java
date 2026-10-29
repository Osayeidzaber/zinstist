package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonMainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void jilaton$openBrandedMenu(CallbackInfo callbackInfo) {
        Minecraft.getInstance().setScreen(new JilatonMainMenuScreen());
        callbackInfo.cancel();
    }
}
