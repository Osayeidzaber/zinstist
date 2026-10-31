/*
 * Ported from Anubis 0.9.9.3 (com.example.client.mixin.MixinSwingAnimationLivingEntity), GPL-3.0,
 * with the owners' permission recorded in development/ORCHESTRATION.md (R4).
 */
package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the module's tempo to the local player's swing duration and latches combat-only per swing. */
@Mixin(LivingEntity.class)
public abstract class SwingAnimationMixin {
    @ModifyExpressionValue(method = {"updateSwingTime", "swing(Lnet/minecraft/world/InteractionHand;Z)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"))
    private int jilaton$swingTempo(int ticks) {
        SwingAnimationsModule module = SwingAnimationsModule.active();
        if (module == null || (Object) this != Minecraft.getInstance().player) return ticks;
        return module.swingTicks(ticks);
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;swinging:Z", opcode = Opcodes.PUTFIELD))
    private void jilaton$latchSwing(InteractionHand hand, boolean broadcast, CallbackInfo ci) {
        SwingAnimationsModule module = SwingAnimationsModule.active();
        if (module != null && (Object) this == Minecraft.getInstance().player) module.latch();
    }
}
