package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.ReachMath;
import dev.jilaton.client.module.impl.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityReachMixin {
    @Inject(method = "entityAttackRange", at = @At("RETURN"), cancellable = true)
    private void jilaton$increaseAttackComponentRange(CallbackInfoReturnable<AttackRange> cir) {
        if (!((Object) this instanceof Player player) || player != Minecraft.getInstance().player) {
            return;
        }

        ClientModule registered = JilatonClient.moduleManager().get("reach");
        if (registered instanceof ReachModule module && module.enabled()) {
            cir.setReturnValue(ReachMath.extendAttackRange(cir.getReturnValue(), true,
                    module.entityBonus().value()));
        }
    }
}
