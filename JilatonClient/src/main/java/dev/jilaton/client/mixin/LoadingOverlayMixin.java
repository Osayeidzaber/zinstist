package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonBackdrop;
import dev.jilaton.client.gui.JilatonLoadingLogo;
import dev.jilaton.client.gui.JilatonLoadingScene;
import dev.jilaton.client.gui.JilatonWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the Mojang startup logo and red backdrop with Jilaton branding. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeInStart;
    @Shadow private long fadeOutStart;

    @Inject(method = "registerTextures", at = @At("TAIL"))
    private static void jilaton$registerLoadingLogo(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerAndLoad(JilatonLoadingLogo.ID, new JilatonLoadingLogo());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jilaton$renderBrandedOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long now = Util.getMillis();
        if (fadeIn && fadeInStart == -1L) {
            fadeInStart = now;
        }
        float fadeOut = fadeOutStart > -1L ? (now - fadeOutStart) / 1000.0F : -1.0F;
        float fadeInTime = fadeInStart > -1L ? (now - fadeInStart) / 500.0F : -1.0F;

        boolean showScreen;
        float opacity;
        if (fadeOut >= 1.0F) {
            showScreen = true;
            opacity = 1.0F - Mth.clamp(fadeOut - 1.0F, 0.0F, 1.0F);
        } else if (fadeIn && fadeInTime < 1.0F) {
            showScreen = true;
            opacity = Mth.clamp(fadeInTime, 0.15F, 1.0F);
        } else {
            showScreen = false;
            opacity = 1.0F;
        }

        if (showScreen) {
            if (minecraft.screen != null) {
                minecraft.screen.renderWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
            } else {
                minecraft.gui.renderDeferredSubtitles();
            }
        }

        if (opacity > 0.004F) {
            graphics.nextStratum();
            currentProgress = Mth.clamp(currentProgress * 0.95F + reload.getActualProgress() * 0.05F, 0.0F, 1.0F);
            JilatonLoadingScene.render(graphics, width, height, opacity, currentProgress,
                    JilatonBackdrop.clock(JilatonWidgets.reducedMotion()), fadeOut < 1.0F);
        }

        if (fadeOut >= 2.0F) {
            minecraft.setOverlay(null);
        }
        ci.cancel();
    }
}
