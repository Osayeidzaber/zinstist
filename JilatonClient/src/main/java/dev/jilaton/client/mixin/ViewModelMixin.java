package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.ViewModelModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ViewModelMixin {
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void jilaton$transformHand(AbstractClientPlayer player, float partial, float pitch,
            InteractionHand hand, float swing, ItemStack item, float equip, PoseStack pose,
            SubmitNodeCollector collector, int light, CallbackInfo callback) {
        if (JilatonClient.moduleManager().get("view_model") instanceof ViewModelModule model && model.renderActive())
            model.applyRendered(pose, hand, hand == InteractionHand.MAIN_HAND && item.has(DataComponents.MAP_ID)
                    && player.getOffhandItem().isEmpty(), player.getMainArm());
    }
}
