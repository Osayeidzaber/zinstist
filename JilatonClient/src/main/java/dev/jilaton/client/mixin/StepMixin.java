package dev.jilaton.client.mixin;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.StepModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class StepMixin {
    @Redirect(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;maxUpStep()F"))
    private float jilaton$useConfiguredStepHeight(Entity entity) {
        float vanillaHeight = entity.maxUpStep();
        Minecraft client = Minecraft.getInstance();
        ClientModule module = JilatonClient.moduleManager().get("step");
        if (entity == client.player && module instanceof StepModule stepModule) {
            return stepModule.effectiveHeight(vanillaHeight);
        }
        return vanillaHeight;
    }
}
