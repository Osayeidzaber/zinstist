package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.WTapModule;
import dev.jilaton.client.module.impl.AutoWeaponModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class WTapAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void jilaton$selectWeapon(Player player, Entity target, CallbackInfo callbackInfo) {
        if (player != Minecraft.getInstance().player || !(target instanceof net.minecraft.world.entity.LivingEntity living)) {
            return;
        }
        ClientModule module = JilatonClient.moduleManager().get("auto_weapon");
        if (module instanceof AutoWeaponModule autoWeapon) {
            autoWeapon.beforeAttack(Minecraft.getInstance(), player, living);
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void jilaton$afterAttack(Player player, Entity target, CallbackInfo callbackInfo) {
        if (player == Minecraft.getInstance().player) {
            ClientModule module = JilatonClient.moduleManager().get("wtap");
            if (module instanceof WTapModule wTap) {
                wTap.afterAttack(player);
            }
        }
    }
}
