package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.render.knife.CsKnifeEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hand-pass hooks for the ported Anubis CS knife skins (Butterfly, Karambit, Katana): the hands' base pose is
 * captured for slash trails, and the held item's draw is handed to {@link CsKnifeEngine}, which draws the knife and
 * the arm(s) holding it. Same hook points as Anubis's {@code MixinWeaponInspectItemInHandRenderer}; idle when no CS
 * skin is selected.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class CsKnifeMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void jilaton$csKnifeHands(float partial, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player,
                                      int light, CallbackInfo callback) {
        if (CsKnifeEngine.active != null) CsKnifeEngine.handsBegin(pose);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void jilaton$csKnifeItem(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack,
                                     ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light,
                                     Operation<Void> original, @Local(argsOnly = true) InteractionHand hand) {
        if (CsKnifeEngine.active == null
                || !CsKnifeEngine.renderItem(pose, entity, stack, context, collector, light, hand == InteractionHand.MAIN_HAND)) {
            original.call(renderer, entity, stack, context, pose, collector, light);
        }
    }
}
