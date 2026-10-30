package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonServersScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every path into the vanilla server list (safety notice, "Back to server list" after a disconnect) opens the Jilaton screen. */
@Mixin(JoinMultiplayerScreen.class)
public abstract class ServerListRedirectMixin {
    @Shadow @Final private Screen lastScreen;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void jilaton$redirect(CallbackInfo callback) {
        Minecraft.getInstance().setScreen(new JilatonServersScreen(lastScreen));
        callback.cancel();
    }
}
