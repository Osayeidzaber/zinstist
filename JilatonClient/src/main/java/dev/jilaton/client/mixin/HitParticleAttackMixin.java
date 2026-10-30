package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.HitParticlesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import dev.jilaton.client.render.CombatTargetTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class HitParticleAttackMixin {
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void jilaton$attackParticles(Player player, Entity target, CallbackInfo callback) {
        if (player == Minecraft.getInstance().player && target instanceof LivingEntity living)
            CombatTargetTracker.record(living, System.nanoTime());
        if (JilatonClient.moduleManager().get("hit_particles") instanceof HitParticlesModule particles)
            particles.beforeAttack(Minecraft.getInstance(), player, target);
        if (JilatonClient.moduleManager().get("hit_effects") instanceof dev.jilaton.client.module.impl.HitEffectsModule effects)
            effects.beforeAttack(Minecraft.getInstance(), player, target);
    }
}
