package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.module.impl.NoRenderModule;
import dev.jilaton.client.module.impl.NoRenderModule.Part;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoRender: skips the camera tilt when you take damage. */
@Mixin(GameRenderer.class)
public abstract class NoRenderHurtCamMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void jilaton$hurt(PoseStack stack, float partial, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.HURT_CAM)) ci.cancel();
    }
}
