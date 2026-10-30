package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonWorldsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every path into the vanilla world list opens the Jilaton worlds screen instead. */
@Mixin(SelectWorldScreen.class)
public abstract class WorldListRedirectMixin {
    @Shadow @Final protected Screen lastScreen;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void jilaton$redirect(CallbackInfo callback) {
        Minecraft.getInstance().setScreen(new JilatonWorldsScreen(lastScreen));
        callback.cancel();
    }
}
