package dev.jilaton.client.mixin;

import dev.jilaton.client.gui.JilatonWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World, server and option lists: translucent panel, hairline separators and a Jilaton selection row. */
@Mixin(AbstractSelectionList.class)
public abstract class SelectionListStyleMixin {
    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void jilaton$listBackground(GuiGraphics graphics, CallbackInfo callback) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        JilatonWidgets.listBackground(graphics, self.getX(), self.getY(), self.getRight(), self.getBottom());
        callback.cancel();
    }

    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
    private void jilaton$listSeparators(GuiGraphics graphics, CallbackInfo callback) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        JilatonWidgets.listSeparators(graphics, self.getX(), self.getY(), self.getRight(), self.getBottom());
        callback.cancel();
    }

    @Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
    private void jilaton$selection(GuiGraphics graphics, @Coerce LayoutElement entry, int borderColor, CallbackInfo callback) {
        JilatonWidgets.selection(graphics, entry.getX(), entry.getY(), entry.getX() + entry.getWidth(),
                entry.getY() + entry.getHeight(), borderColor == -1);
        callback.cancel();
    }
}
