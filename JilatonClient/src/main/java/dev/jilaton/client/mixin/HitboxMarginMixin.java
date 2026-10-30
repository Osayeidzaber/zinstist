package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.HitboxMargin;
import dev.jilaton.client.module.impl.HitboxModule;
import dev.jilaton.client.module.impl.PlayerTargetFilters;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HitboxMarginMixin {
    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void jilaton$expandEligibleTargetMargin(CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        ClientModule registered = JilatonClient.moduleManager().get("hitbox");
        if (!(registered instanceof HitboxModule module) || !module.enabled()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        boolean eligible = entity != client.player && entity.isAlive() && !entity.isSpectator()
                && entity instanceof LivingEntity living
                && eligibleTarget(client, module, living);
        cir.setReturnValue(HitboxMargin.apply(cir.getReturnValue(), true, eligible, module.size().value()));
    }

    private static boolean eligibleTarget(Minecraft client, HitboxModule module, LivingEntity target) {
        if (module.playersOnly().value() && !(target instanceof Player)) {
            return false;
        }
        if (target instanceof Player candidate) {
            return PlayerTargetFilters.allowsPlayer(client, client.player, candidate);
        }
        return !client.player.isAlliedTo(target);
    }
}
