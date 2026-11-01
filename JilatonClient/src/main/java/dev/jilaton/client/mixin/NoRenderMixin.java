package dev.jilaton.client.mixin;

import dev.jilaton.client.module.impl.NoRenderModule;
import dev.jilaton.client.module.impl.NoRenderModule.Part;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD-side NoRender hooks: each one skips a vanilla overlay while its option is on. */
@Mixin(Gui.class)
public abstract class NoRenderMixin {
    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void jilaton$texture(GuiGraphics graphics, Identifier texture, float alpha, CallbackInfo ci) {
        String path = texture.getPath();
        if (path.contains("pumpkinblur") && NoRenderModule.hides(Part.PUMPKIN)
                || path.contains("powder_snow") && NoRenderModule.hides(Part.POWDER_SNOW)) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void jilaton$portal(GuiGraphics graphics, float alpha, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.PORTAL)) ci.cancel();
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void jilaton$nausea(GuiGraphics graphics, float strength, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.NAUSEA)) ci.cancel();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void jilaton$vignette(GuiGraphics graphics, Entity entity, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.VIGNETTE)) ci.cancel();
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void jilaton$spyglass(GuiGraphics graphics, float scale, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.SPYGLASS)) ci.cancel();
    }

    @Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true)
    private void jilaton$boss(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.BOSS_BARS)) ci.cancel();
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void jilaton$title(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (NoRenderModule.hides(Part.TITLES)) ci.cancel();
    }
}
