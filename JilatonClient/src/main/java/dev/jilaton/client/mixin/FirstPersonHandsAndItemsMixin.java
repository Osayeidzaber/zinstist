/*
 * Injection point adapted from LiquidBounce's MixinFirstPersonHandsAndItems.java
 * Copyright (c) 2015-2026 CCBlueX
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 * Licensed under the GNU General Public License v3.0 or later.
 */
package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
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
        if (client.player == null || arm != client.player.getMainArm()) {
            return;
        }

        ClientModule module = JilatonClient.moduleManager().get("swing_animations");
        if (module instanceof SwingAnimationsModule animations && animations.enabled()) {
            animations.apply(poseStack, arm, progress);
            ci.cancel();
        }
    }
}
