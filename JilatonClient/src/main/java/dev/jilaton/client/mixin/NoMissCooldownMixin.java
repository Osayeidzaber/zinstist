package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.NoMissCooldownModule;
import dev.jilaton.client.module.impl.NoMissCooldownPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class NoMissCooldownMixin {
    @Shadow
    private int missTime;

    @Shadow
    private HitResult hitResult;

    @Redirect(method = "startAttack", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int jilaton$removeMissCooldown(Minecraft client) {
        NoMissCooldownModule module = noMissCooldownModule();
        return NoMissCooldownPolicy.missTime(module != null && module.enabled(),
                module != null && module.removeAttackCooldown(), missTime);
    }

    @Redirect(method = "startAttack", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;missTime:I", opcode = Opcodes.PUTFIELD))
    private void jilaton$preventMissCooldownWrite(Minecraft client, int missTime) {
        NoMissCooldownModule module = noMissCooldownModule();
        if (NoMissCooldownPolicy.allowMissTimeWrite(module != null && module.enabled(),
                module != null && module.removeAttackCooldown())) {
            this.missTime = missTime;
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jilaton$consumeAttackOnMiss(CallbackInfoReturnable<Boolean> callback) {
        NoMissCooldownModule module = noMissCooldownModule();
        if (hitResult != null && hitResult.getType() == HitResult.Type.MISS
                && NoMissCooldownPolicy.consumeMiss(module != null && module.enabled(),
                module != null && module.cancelAttackOnMiss())) {
            callback.setReturnValue(true);
        }
    }

    private static NoMissCooldownModule noMissCooldownModule() {
        ClientModule module = JilatonClient.moduleManager().get("no_miss_cooldown");
        return module instanceof NoMissCooldownModule noMiss ? noMiss : null;
    }
}
