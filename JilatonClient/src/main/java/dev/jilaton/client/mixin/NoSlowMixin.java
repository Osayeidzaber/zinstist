package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.NoSlowModule;
import dev.jilaton.client.module.impl.NoSlowPolicy;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class NoSlowMixin {
    @ModifyArg(
            method = "modifyInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;",
                    ordinal = 1
            ),
            index = 0
    )
    private float jilaton$removeItemUseSlowdown(float originalMultiplier) {
        NoSlowModule module = noSlowModule();
        return NoSlowPolicy.itemUseMultiplier(module != null && module.enabled(),
                module != null && module.removeItemSlowdown(),
                module == null ? 1.0 : module.itemUseScale(), originalMultiplier);
    }

    @Redirect(
            method = "isSlowDueToUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
            )
    )
    private boolean jilaton$allowSprintWhileUsing(LocalPlayer player) {
        NoSlowModule module = noSlowModule();
        return NoSlowPolicy.slowDueToItemUse(module != null && module.enabled(),
                module != null && module.allowSprintWhileUsing(), player.isUsingItem());
    }

    private static NoSlowModule noSlowModule() {
        ClientModule module = JilatonClient.moduleManager().get("no_slow");
        return module instanceof NoSlowModule noSlow ? noSlow : null;
    }
}
