package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Paint;
import dev.jilaton.client.module.ClientModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Board HUD hotbar: while the Jilaton HUD is on, the vanilla hotbar sprites become a rounded ink glass bar with
 * soft slot tiles and a white rounded selection ring. Items, counts and durability are still drawn by vanilla.
 */
@Mixin(Gui.class)
public abstract class HotbarStyleMixin {
    @WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void jilaton$glassHotbar(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
                                     int width, int height, Operation<Void> original) {
        ClientModule hud = JilatonClient.moduleManager().get("hud");
        if (hud == null || !hud.enabled()) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
            return;
        }
        switch (sprite.getPath()) {
            case "hud/hotbar" -> {
                Paint.round(graphics, x, y, width, height, 7, 0xB80A0A0A);
                Paint.outline(graphics, x, y, width, height, 7, 0.5F, 0x1FFFFFFF);
                for (int slot = 0; slot < 9; slot++)
                    Paint.round(graphics, x + 2 + slot * 20, y + 2, 18, 18, 4, 0x10FFFFFF);
            }
            case "hud/hotbar_selection" -> {
                Paint.round(graphics, x + 1, y + 1, width - 2, width - 2, 6, 0x1CFFFFFF);
                Paint.outline(graphics, x + 1, y + 1, width - 2, width - 2, 6, 1F, 0xF0F5F5F5);
            }
            case "hud/hotbar_offhand_left" -> {
                Paint.round(graphics, x, y + 1, 22, 22, 7, 0xB80A0A0A);
                Paint.outline(graphics, x, y + 1, 22, 22, 7, 0.5F, 0x1FFFFFFF);
            }
            case "hud/hotbar_offhand_right" -> {
                Paint.round(graphics, x + width - 22, y + 1, 22, 22, 7, 0xB80A0A0A);
                Paint.outline(graphics, x + width - 22, y + 1, 22, 22, 7, 0.5F, 0x1FFFFFFF);
            }
            default -> original.call(graphics, pipeline, sprite, x, y, width, height);
        }
    }
}
