package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.KeepSprintModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Player.class)
public abstract class KeepSprintMixin {
    @ModifyArgs(
            method = "causeExtraKnockback",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;")
    )
    private void jilaton$adjustAttackSlowdown(Args args) {
        Player player = (Player) (Object) this;
        Minecraft client = Minecraft.getInstance();
        ClientModule module = JilatonClient.moduleManager().get("keep_sprint");
        if (player != client.player || !(module instanceof KeepSprintModule keepSprint) || !keepSprint.enabled()) {
            return;
        }

        double multiplier = keepSprint.knockbackMultiplier(player);
        args.set(0, multiplier);
        args.set(2, multiplier);
    }

    @Redirect(
            method = "causeExtraKnockback",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V")
    )
    private void jilaton$preserveSprintAfterAttack(Player player, boolean sprinting) {
        ClientModule module = JilatonClient.moduleManager().get("keep_sprint");
        if (player == Minecraft.getInstance().player && !sprinting
                && module instanceof KeepSprintModule keepSprint && keepSprint.enabled()) {
            return;
        }
        player.setSprinting(sprinting);
    }
}
