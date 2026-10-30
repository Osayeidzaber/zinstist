package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonWidgets;
import dev.jilaton.client.gui.Motion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Every vanilla button (plain, cycle, toggle) paints through renderDefaultSprite; this replaces that surface. */
@Mixin(AbstractButton.class)
public abstract class ButtonStyleMixin {
    @Shadow private Supplier<Boolean> overrideRenderHighlightedSprite;
    @Unique private final Motion.Transition jilaton$hover = new Motion.Transition(0.22, Motion.SETTLE);

    @Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void jilaton$flatButton(GuiGraphics graphics, CallbackInfo callback) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        boolean highlighted = overrideRenderHighlightedSprite != null ? overrideRenderHighlightedSprite.get() : self.isHoveredOrFocused();
        double hover = jilaton$hover.update(highlighted && self.active, System.nanoTime(), JilatonWidgets.reducedMotion());
        JilatonWidgets.button(graphics, self.getX(), self.getY(), self.getWidth(), self.getHeight(), self.active, hover,
                ((AbstractWidgetAccessor) self).jilaton$alpha());
        callback.cancel();
    }
}
