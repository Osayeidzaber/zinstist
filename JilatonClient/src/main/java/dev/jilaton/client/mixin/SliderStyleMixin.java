package dev.jilaton.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.jilaton.client.gui.JilatonWidgets;
import dev.jilaton.client.gui.Motion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slider track and handle are two sprite draws; the track is drawn first and decides the hover state. */
@Mixin(AbstractSliderButton.class)
public abstract class SliderStyleMixin {
    @Shadow protected double value;
    @Unique private final Motion.Transition jilaton$hover = new Motion.Transition(0.22, Motion.SETTLE);
    @Unique private double jilaton$hoverValue;

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void jilaton$sliderSprite(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite,
                                      int x, int y, int w, int h, int color) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        float alpha = (color >>> 24) / 255F;
        if (sprite.getPath().contains("handle")) {
            JilatonWidgets.sliderHandle(graphics, x, y, w, h, jilaton$hoverValue, self.active, alpha);
        } else {
            jilaton$hoverValue = jilaton$hover.update(self.isHoveredOrFocused() && self.active, System.nanoTime(),
                    JilatonWidgets.reducedMotion());
            JilatonWidgets.sliderTrack(graphics, x, y, w, h, value, jilaton$hoverValue, self.active, alpha);
        }
    }
}
