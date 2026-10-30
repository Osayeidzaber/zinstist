package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.NametagsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class NametagMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jilaton$replaceSelectedNames(Entity entity, EntityRenderState state, float partial, CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("nametags") instanceof NametagsModule tags
                && tags.shouldHideVanilla(entity, Minecraft.getInstance())) state.nameTag = null;
    }
}
