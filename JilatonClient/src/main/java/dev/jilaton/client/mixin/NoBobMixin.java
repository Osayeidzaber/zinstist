package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.impl.NoBobModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class NoBobMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void jilaton$suppressViewBobbing(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (NoBobModule.shouldSuppress(net.minecraft.client.Minecraft.getInstance())) {
            ci.cancel();
        }
    }
}
