package dev.jilaton.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's full-screen post effect slot (creeper/spider vision), used by Cyberspace for its screen grade. */
@Mixin(GameRenderer.class)
public interface GameRendererPostEffectAccess {
    @Accessor("postEffectId")
    Identifier jilaton$postEffectId();

    @Invoker("setPostEffect")
    void jilaton$setPostEffect(Identifier id);
}
