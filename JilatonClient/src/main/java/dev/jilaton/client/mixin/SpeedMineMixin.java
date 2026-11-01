package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jilaton.client.module.impl.SpeedMineModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SpeedMine: scaled break progress and an optional skipped delay between blocks. Ported from Anubis Client (MixinSpeedMineMultiPlayerGameMode, GPL-3.0, Horcus and 4ldenz). */
@Mixin(MultiPlayerGameMode.class)
public abstract class SpeedMineMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private int destroyDelay;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void jilaton$speedMineDelay(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = minecraft.player;
        if (destroyDelay > 0 && player != null && minecraft.level != null && !player.getAbilities().instabuild) {
            BlockState state = minecraft.level.getBlockState(pos);
            if (SpeedMineModule.dropsDelay(state, state.getDestroyProgress(player, minecraft.level, pos))) destroyDelay = 0;
        }
    }

    @WrapOperation(method = "continueDestroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float jilaton$speedMineProgress(BlockState state, Player player, BlockGetter level, BlockPos pos, Operation<Float> original) {
        return SpeedMineModule.scaleProgress(state, original.call(state, player, level, pos));
    }
}
