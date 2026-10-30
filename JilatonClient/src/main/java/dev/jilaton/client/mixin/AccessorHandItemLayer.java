package dev.jilaton.client.mixin;

import net.minecraft.client.renderer.block.model.ItemTransform;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(LayerRenderState.class)
public interface AccessorHandItemLayer {
   @Accessor("transform")
   ItemTransform anubis$handTransform();

   @Accessor("extents")
   Supplier<Vector3fc[]> anubis$handExtents();
}
