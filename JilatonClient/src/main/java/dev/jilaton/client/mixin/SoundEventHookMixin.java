package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.AutoFishModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets modules hear positioned sounds the server plays (AutoFish bites); the packet is never altered. */
@Mixin(ClientPacketListener.class)
public abstract class SoundEventHookMixin {
    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void jilaton$sound(ClientboundSoundPacket packet, CallbackInfo callback) {
        AutoFishModule.onSound(packet.getSound().value(), packet.getX(), packet.getY(), packet.getZ());
    }
}
