package dev.jilaton.client.mixin;

import dev.jilaton.client.render.EntityGlowSupport;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityGlowColorMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jilaton$glowColor(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback) {
        Integer color = EntityGlowSupport.color(entity);
        if (color != null) state.outlineColor = color;
    }
}
