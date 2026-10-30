package dev.jilaton.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.jilaton.client.gui.JilatonWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Checkbox.class)
public abstract class CheckboxStyleMixin {
    @Redirect(method = "renderContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void jilaton$checkboxSprite(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite,
                                        int x, int y, int w, int h, int color) {
        String path = sprite.getPath();
        JilatonWidgets.checkbox(graphics, x, y, w, path.contains("selected"), path.contains("highlighted"), (color >>> 24) / 255F);
    }
}
