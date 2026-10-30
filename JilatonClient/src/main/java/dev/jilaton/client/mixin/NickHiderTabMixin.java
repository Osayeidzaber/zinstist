package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NickHiderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class NickHiderTabMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void jilaton$hideOwnName(PlayerInfo info, CallbackInfoReturnable<Component> callback) {
        NickHiderModule module = NickHiderModule.active();
        Component name = callback.getReturnValue();
        if (module == null || name == null || info == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !info.getProfile().id().equals(client.player.getUUID())) {
            return;
        }
        callback.setReturnValue(module.displayName(name, NickHiderModule.localName(client)));
    }
}
