package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jilaton.client.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class MixinCharacterLivingEntityRenderer {
   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void anubis$hideModelUnderCharacter(
      LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir
   ) {
      if (CharacterModule.hidesModel(state)) {
         cir.setReturnValue(null);
      }
   }

   @WrapOperation(
      method = "submit",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"
      )
   )
   private void anubis$hideLayersUnderCharacter(
      RenderLayer<?, ?> layer, PoseStack poseStack, SubmitNodeCollector collector, int light, EntityRenderState state, float yRot, float xRot, Operation<Void> original
   ) {
      if (!CharacterModule.hidesLayer(layer, state)) {
         original.call(new Object[]{layer, poseStack, collector, light, state, yRot, xRot});
      }
   }
}
