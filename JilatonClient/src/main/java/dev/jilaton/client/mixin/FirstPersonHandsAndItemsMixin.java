/*
 * Injection point adapted from LiquidBounce's MixinFirstPersonHandsAndItems.java
 * Copyright (c) 2015-2026 CCBlueX
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 * Licensed under the GNU General Public License v3.0 or later.
 */
package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.render.model.FirstPersonHandStages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void jilaton$applySwingAnimation(float progress, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        ClientModule module = JilatonClient.moduleManager().get("swing_animations");
        if (module instanceof SwingAnimationsModule animations && animations.enabled()
                && animations.covers(arm == client.player.getMainArm())) {
            animations.apply(poseStack, arm, progress);
            ci.cancel();
        }
    }

    /**
     * An empty main hand is drawn by {@code renderPlayerArm}, which bakes its own swing into the arm transforms and
     * never calls {@code swingArm}, so the injection above cannot reach it. The style goes onto the arm pose here and
     * vanilla then draws the arm at rest, so the style is the whole swing and the two never stack.
     */
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V"))
    private void jilaton$emptyHandSwingStyle(ItemInHandRenderer renderer, PoseStack pose, SubmitNodeCollector collector,
                                             int light, float equip, float swing, HumanoidArm arm,
                                             Operation<Void> original) {
        ClientModule module = JilatonClient.moduleManager().get("swing_animations");
        if (!(module instanceof SwingAnimationsModule animations) || !animations.enabled()
                || !FirstPersonHandStages.swing(pose, animations, arm, swing)) {
            original.call(renderer, pose, collector, light, equip, swing, arm);
            return;
        }
        original.call(renderer, pose, collector, light, equip, 0.0f, arm);
    }
}
