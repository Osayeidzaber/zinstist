package dev.jilaton.client.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface OverlayRenderTypeAccess {
    @Invoker("create") static RenderType createOverlay(String name, RenderSetup setup) { throw new AssertionError(); }
}
