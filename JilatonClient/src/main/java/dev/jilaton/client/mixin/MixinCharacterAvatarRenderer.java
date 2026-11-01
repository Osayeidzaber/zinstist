package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

import dev.jilaton.client.render.character.CharacterLayer;
import dev.jilaton.client.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public abstract class MixinCharacterAvatarRenderer<T extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<T, AvatarRenderState, PlayerModel> {
   protected MixinCharacterAvatarRenderer(Context context, PlayerModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void jilaton$addCharacterLayer(Context context, boolean slim, CallbackInfo ci) {
      this.addLayer(new CharacterLayer(this));
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void jilaton$extractCharacter(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
      CharacterModule.extract(entity, state);
      dev.jilaton.client.module.impl.GlassMaterialModule.applyGhost(entity, state);
   }

   @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
   private void jilaton$characterFirstPersonHand(
      PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm, boolean sleeve, CallbackInfo ci
   ) {
      if (CharacterLayer.firstPersonHand((PlayerModel)this.getModel(), poseStack, collector, light, arm)) {
         ci.cancel();
      }
   }
}
