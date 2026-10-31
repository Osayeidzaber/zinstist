package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.ReachMath;
import dev.jilaton.client.module.impl.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerReachMixin {
    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void jilaton$increaseEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        ReachModule module = reachModule();
        if (module != null && module.enabled() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(ReachMath.extendEntityInteractionRange(cir.getReturnValue(), true,
                    module.entityBonus().value()));
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void jilaton$increaseBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        ReachModule module = reachModule();
        if (module != null && module.enabled() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(ReachMath.extendBlockInteractionRange(cir.getReturnValue(), true,
                    module.blockBonus().value()));
        }
    }

    private static ReachModule reachModule() {
        ClientModule module = JilatonClient.moduleManager().get("reach");
        return module instanceof ReachModule reach ? reach : null;
    }
}
