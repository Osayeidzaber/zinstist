package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.impl.LowFireModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the first-person fire overlay with the Low Fire module's tint, size and lowering instead of the
 * vanilla pass: same two crossed quads, same atlas sprite, then pushed down so the flame stays out of the
 * view and the hotbar. Without the module the vanilla method runs untouched.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class LowFireMixin {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void jilaton$lowFire(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite,
                                        CallbackInfo callback) {
        if (!(JilatonClient.moduleManager().get("low_fire") instanceof LowFireModule fire) || !fire.enabled()) return;
        callback.cancel();
        ClientConfig config = JilatonClient.clientConfig();
        int rgb = fire.fireTint(ClientTheme.resolveAccent(config.themeId(), config.customAccent()));
        float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
        float alpha = (float) fire.alpha();
        float scale = (float) fire.scale();
        float drop = -0.3F - (float) fire.lowering();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        VertexConsumer buffer = buffers.getBuffer(RenderTypes.fireScreenEffect(sprite.atlasLocation()));
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.translate((1 - i * 2) * 0.24F * scale, drop, 0F);
            pose.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10F));
            Matrix4f matrix = pose.last().pose();
            vertex(buffer, matrix, -0.5F * scale, -0.5F * scale, u1, v1, r, g, b, alpha);
            vertex(buffer, matrix, 0.5F * scale, -0.5F * scale, u1, v0, r, g, b, alpha);
            vertex(buffer, matrix, 0.5F * scale, 0.5F * scale, u0, v0, r, g, b, alpha);
            vertex(buffer, matrix, -0.5F * scale, 0.5F * scale, u0, v1, r, g, b, alpha);
            pose.popPose();
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float u, float v,
                               float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, -0.5F).setUv(u, v).setColor(r, g, b, a);
    }
}
