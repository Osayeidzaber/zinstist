/* Adapted from LiquidBounce MixinAttackRange, Copyright (c) 2015-2026 CCBlueX.
 * GPL-3.0-or-later; see ATTRIBUTION.md. */
package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.HitboxModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttackRange.class)
public abstract class AttackRangeHitboxMixin {
    @ModifyExpressionValue(method = "isInRange(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/ToDoubleFunction;D)Z",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/component/AttackRange;hitboxMargin:F", opcode = Opcodes.GETFIELD),
            require = 1)
    private static float jilaton$componentMargin(float original, @Local(argsOnly = true) LivingEntity attacker) {
        if (attacker == Minecraft.getInstance().player
                && JilatonClient.moduleManager().get("hitbox") instanceof HitboxModule module
                && module.enabled() && module.applyToComponent()) return original + (float) module.size().value();
        return original;
    }
}
