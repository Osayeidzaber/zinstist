package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.PlayerMoveEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Hooks the vanilla travel-to-move handoff, so motion modules affect only local self-movement. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {
    @ModifyArgs(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void jilaton$dispatchPlayerMove(Args args) {
        if (args.get(0) != MoverType.SELF || !((Object) this instanceof LocalPlayer player)
                || Minecraft.getInstance().player != player) {
            return;
        }

        PlayerMoveEvent event = JilatonClient.moduleManager().dispatchPlayerMove(
                (MoverType) args.get(0), (Vec3) args.get(1));
        if (event != null) {
            args.set(1, event.movement());
        }
    }
}
