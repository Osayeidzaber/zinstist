/* Adapted from MixinFriendGuardMultiPlayerGameMode, Horcus / 4ldenz, GPL-3.0-only.
 * See ATTRIBUTION.md. */
package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FriendsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class FriendsGuardMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, order = 900)
    private void jilaton$friendGuard(Player player, Entity target, CallbackInfo ci) {
        if (JilatonClient.moduleManager().get("friends") instanceof FriendsModule friends && friends.intercepts(player, target)) {
            player.resetAttackStrengthTicker();
            ci.cancel();
        }
    }

    @Inject(method = "piercingAttack", at = @At("HEAD"), cancellable = true, order = 900)
    private void jilaton$friendGuardStab(PiercingWeapon weapon, CallbackInfo ci) {
        if (JilatonClient.moduleManager().get("friends") instanceof FriendsModule friends
                && friends.interceptsStab(Minecraft.getInstance().player)) ci.cancel();
    }
}
