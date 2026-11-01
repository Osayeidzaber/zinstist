package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.impl.NoRenderModule;
import dev.jilaton.client.module.impl.NoRenderModule.Part;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoRender: underwater tint, in-wall texture and the totem pop animation drawn over the camera. */
@Mixin(ScreenEffectRenderer.class)
public abstract class NoRenderScreenEffectMixin {
    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void jilaton$water(Minecraft client, PoseStack stack, MultiBufferSource buffers, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.UNDERWATER)) ci.cancel();
    }

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void jilaton$wall(TextureAtlasSprite sprite, PoseStack stack, MultiBufferSource buffers, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.IN_WALL)) ci.cancel();
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void jilaton$totem(PoseStack stack, float partial, SubmitNodeCollector collector, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.TOTEM)) ci.cancel();
    }
}
