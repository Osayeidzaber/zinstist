package dev.jilaton.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.impl.CosmeticKnifeModule;
import dev.jilaton.client.module.impl.SwingAnimationsModule;
import dev.jilaton.client.module.impl.ViewModelModule;
import dev.jilaton.client.render.model.FirstPersonHandStages;
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
 *
 * <p>Cancelling {@code renderArmWithItem} at HEAD also skips the two stages vanilla runs inside it — the view-model
 * hook after its {@code pushPose} and the single {@code swingArm} call — so this hook applies both stages to the pose
 * it owns, once, before the knife draws. The ported CS skins animate the arm from their own attack clips, so only the
 * view-model offset applies to them.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class KnifeViewmodelMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void jilaton$knifeViewmodel(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand,
                                        float swing, ItemStack item, float equip, PoseStack pose,
                                        SubmitNodeCollector collector, int light, CallbackInfo callback) {
        if (hand != InteractionHand.MAIN_HAND || player.isScoping()) return;
        ClientModule knifeModule = JilatonClient.moduleManager().get("cosmetic_knife");
        if (!(knifeModule instanceof CosmeticKnifeModule knife) || !knife.enabled() || !knife.replaces(item)) return;
        ViewModelModule viewModel = JilatonClient.moduleManager().get("view_model") instanceof ViewModelModule model
                && model.enabled() ? model : null;
        SwingAnimationsModule swingStyles = JilatonClient.moduleManager().get("swing_animations")
                instanceof SwingAnimationsModule styles && styles.enabled() ? styles : null;

        pose.pushPose();
        FirstPersonHandStages.viewmodel(pose, viewModel, InteractionHand.MAIN_HAND);
        if (!knife.csSkin()) FirstPersonHandStages.swing(pose, swingStyles, player.getMainArm(), swing);
        knife.renderInHand((ItemInHandRendererInvoker) this, player, partial, swing, pose, collector, light);
        pose.popPose();
        callback.cancel();
    }
}
