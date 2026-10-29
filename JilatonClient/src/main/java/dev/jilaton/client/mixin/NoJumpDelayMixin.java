package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.NoJumpDelayModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class NoJumpDelayMixin {
    @Shadow
    private int noJumpDelay;

    @Redirect(method = "aiStep", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I", opcode = Opcodes.PUTFIELD))
    private void jilaton$rearmJumpCooldown(LivingEntity entity, int requestedTicks) {
        Minecraft client = Minecraft.getInstance();
        ClientModule module = JilatonClient.moduleManager().get("no_jump_delay");
        if (entity == client.player && module instanceof NoJumpDelayModule noJumpDelayModule
                && noJumpDelayModule.enabled()
                && requestedTicks > noJumpDelay) {
            requestedTicks = noJumpDelayModule.rearm(requestedTicks);
        }
        noJumpDelay = requestedTicks;
    }
}
