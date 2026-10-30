package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class FreecamRenderMixin {
    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void jilaton$freecamEntityVisibility(BlockPos position, CallbackInfoReturnable<Boolean> callback) {
        if (JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera && camera.live(Minecraft.getInstance()))
            callback.setReturnValue(true);
    }
}
