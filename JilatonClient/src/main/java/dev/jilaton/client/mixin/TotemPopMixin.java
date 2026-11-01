package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.TotemSpiritModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forwards totem-pop entity events to the Totem Spirit visual; the packet is never altered. */
@Mixin(ClientPacketListener.class)
public abstract class TotemPopMixin {
    private static final byte TOTEM_POP_EVENT = 35;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void jilaton$totemSpirit(ClientboundEntityEventPacket packet, CallbackInfo callback) {
        if (packet.getEventId() != TOTEM_POP_EVENT) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity entity = packet.getEntity(client.level);
        if (entity instanceof LivingEntity living) {
            TotemSpiritModule.onPop(living);
            dev.jilaton.client.module.impl.NotifierModule.onPop(living);
        }
    }
}
