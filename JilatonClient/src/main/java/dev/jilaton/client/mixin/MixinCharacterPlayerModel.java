package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;

import dev.jilaton.client.render.character.CharacterLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PlayerModel.class)
public abstract class MixinCharacterPlayerModel {
   @Inject(method = "translateToHand", at = @At("HEAD"), cancellable = true)
   private void anubis$characterHand(AvatarRenderState state, HumanoidArm arm, PoseStack poseStack, CallbackInfo ci) {
      if (CharacterLayer.translateToHand((PlayerModel) (Object) this, state, arm, poseStack)) {
         ci.cancel();
      }
   }
}
