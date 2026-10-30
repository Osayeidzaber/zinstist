package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class FreecamLocalPlayerMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void jilaton$bodyIntentBeforeSend(CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if ((Object) this == client.player && JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera)
            camera.driveBeforeSend(client);
    }
}
