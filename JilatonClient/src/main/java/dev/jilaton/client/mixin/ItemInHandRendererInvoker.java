package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's first-person arm: it leaves its arm transform on the pose stack, so a held model can follow it. */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {
    @Invoker("renderPlayerArm")
    void jilaton$renderPlayerArm(PoseStack pose, SubmitNodeCollector collector, int light, float equip, float swing,
                                 HumanoidArm arm);
}
