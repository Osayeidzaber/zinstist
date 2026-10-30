package dev.jilaton.client.mixin;

import dev.jilaton.client.render.EntityGlowSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class EntityGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void jilaton$selectedGlow(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && EntityGlowSupport.color(entity) != null) callback.setReturnValue(true);
    }
}
