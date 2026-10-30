package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NickHiderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class NickHiderNametagMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jilaton$hideOwnName(Entity entity, EntityRenderState state, float partial, CallbackInfo callback) {
        NickHiderModule module = NickHiderModule.active();
        if (module == null || state.nameTag == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || entity != client.player) {
            return;
        }
        state.nameTag = module.displayName(state.nameTag, NickHiderModule.localName(client));
    }
}
