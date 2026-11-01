package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.SneakModule;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Sneak (Packet mode): entity interactions are sent as sneak-interactions. Ported from Anubis Client (MixinSneakServerboundInteractPacket, GPL-3.0, Horcus and 4ldenz). */
@Mixin(ServerboundInteractPacket.class)
public abstract class SneakInteractPacketMixin {
    @ModifyVariable(method = "<init>(IZLnet/minecraft/network/protocol/game/ServerboundInteractPacket$Action;)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static boolean jilaton$packetSneak(boolean usingSecondaryAction) {
        if (usingSecondaryAction) return true;
        SneakModule sneak = SneakModule.active;
        return sneak != null && sneak.doPacket();
    }
}
