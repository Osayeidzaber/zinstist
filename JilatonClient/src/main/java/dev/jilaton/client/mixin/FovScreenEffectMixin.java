package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.CustomFovModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class FovScreenEffectMixin {
    @Redirect(method = "renderScreenEffect", at = @At(value = "NEW", target = "com/mojang/blaze3d/vertex/PoseStack"))
    private PoseStack jilaton$compensateOverlayProjection() {
        PoseStack pose = new PoseStack();
        if (JilatonClient.moduleManager().get("custom_fov") instanceof CustomFovModule fov) {
            float scale = fov.effectScale();
            if (scale != 1) pose.last().pose().scale(scale, scale, 1);
        }
        return pose;
    }
}
