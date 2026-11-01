package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.FinisherModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Finisher: postpones the client-side removal of a body while its finishing move plays (visual only). */
@Mixin(ClientLevel.class)
public abstract class FinisherBodyMixin {
    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void jilaton$keepBody(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        if (FinisherModule.holdRemoval(entityId, reason)) ci.cancel();
    }
}
