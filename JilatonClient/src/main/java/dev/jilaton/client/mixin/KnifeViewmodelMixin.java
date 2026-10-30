package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.impl.CosmeticKnifeModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the main-hand item pass with the Knife module's viewmodel: the player's own arm drawn by vanilla, with the
 * knife model attached at the hand. Everything else (off hand, maps, bows) keeps the vanilla path.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class KnifeViewmodelMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void jilaton$knifeViewmodel(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand,
                                        float swing, ItemStack item, float equip, PoseStack pose,
                                        SubmitNodeCollector collector, int light, CallbackInfo callback) {
        if (hand != InteractionHand.MAIN_HAND || player.isScoping()) return;
        if (!(JilatonClient.moduleManager().get("cosmetic_knife") instanceof CosmeticKnifeModule knife)
                || !knife.enabled() || !knife.replaces(item)) return;
        knife.renderInHand((ItemInHandRendererInvoker) this, player, partial, swing, pose, collector, light);
        callback.cancel();
    }
}
