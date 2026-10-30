package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FreecamBodyMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void jilaton$showOwnBody(Player viewer, CallbackInfoReturnable<Boolean> callback) {
        Minecraft client = Minecraft.getInstance();
        if ((Object) this == client.player && viewer == client.player
                && JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera && camera.live(client) && camera.showBody())
            callback.setReturnValue(false);
    }
}
