package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.FinisherModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forwards death entity events to the Finisher visual; the packet is never altered. */
@Mixin(ClientPacketListener.class)
public abstract class FinisherDeathMixin {
    private static final byte DEATH_EVENT = 3;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void jilaton$finisher(ClientboundEntityEventPacket packet, CallbackInfo callback) {
        if (packet.getEventId() != DEATH_EVENT) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity entity = packet.getEntity(client.level);
        if (entity instanceof LivingEntity living) FinisherModule.onDeath(living);
    }
}
