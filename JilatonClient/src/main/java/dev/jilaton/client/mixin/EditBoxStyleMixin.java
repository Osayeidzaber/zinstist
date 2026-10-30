package dev.jilaton.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.jilaton.client.gui.JilatonWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class EditBoxStyleMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void jilaton$fieldSprite(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        EditBox self = (EditBox) (Object) this;
        JilatonWidgets.field(graphics, x, y, w, h, self.isFocused(), self.isActive());
    }
}
