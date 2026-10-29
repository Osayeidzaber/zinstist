package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.HighJumpModule;
import dev.jilaton.client.module.impl.LongJumpModule;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HighJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void jilaton$applyHighJump(CallbackInfo callbackInfo) {
        if (!((Object) this instanceof Player player)) {
            return;
        }
        ClientModule module = JilatonClient.moduleManager().get("high_jump");
        if (module instanceof HighJumpModule highJump) {
            highJump.applyJump(player);
        }
        ClientModule longJumpModule = JilatonClient.moduleManager().get("long_jump");
        if (longJumpModule instanceof LongJumpModule longJump) {
            longJump.onSuccessfulLocalJump(player);
        }
    }
}
