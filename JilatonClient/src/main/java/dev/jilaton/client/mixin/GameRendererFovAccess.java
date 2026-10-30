package dev.jilaton.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererFovAccess {
    @Invoker("getFov") float jilaton$worldFov(Camera camera, float partial, boolean worldProjection);
}
