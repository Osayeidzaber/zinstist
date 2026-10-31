package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.render.character.PharaohHelmetPolicy;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The headdress is world geometry drawn over the head, so a worn helmet stays visible through it.
 * Skipping the head armor piece for player avatars only — non-player humanoids keep their helmets.
 * The decision lives in {@link PharaohHelmetPolicy}: Mixin forbids non-private static mixin methods.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class PharaohHelmetMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void jilaton$hideHelmetUnderHeaddress(PoseStack pose, SubmitNodeCollector collector, ItemStack stack,
            EquipmentSlot slot, int packedLight, HumanoidRenderState state, CallbackInfo ci) {
        if (PharaohHelmetPolicy.hidesHelmet(slot, state, PharaohHelmetPolicy.playerOf(state))) {
            ci.cancel();
        }
    }
}
