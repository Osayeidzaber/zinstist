/* Java adaptation of LiquidBounce CrosshairCS2/CrosshairCircle cooldown geometry.
 * Copyright (c) 2015-2026 CCBlueX. GPL-3.0-or-later; see ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

public final class CustomCrosshairModule extends ClientModule {
    private final ChoiceSetting shape = addSetting(new ChoiceSetting("shape", "Shape", List.of("Cross", "Circle", "Dot"), "Cross"));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Arm length / radius", 1, 20, 1, 5));
    private final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Thickness", 1, 5, 1, 1));
    private final NumberSetting gap = addSetting(new NumberSetting("gap", "Gap", 0, 10, 1, 2));
    private final NumberSetting dynamic = addSetting(new NumberSetting("dynamic", "Cooldown expansion", 0, 10, 0.5, 3));
    private final BooleanSetting outline = addSetting(new BooleanSetting("outline", "Dark outline", true));
    private final BooleanSetting dot = addSetting(new BooleanSetting("dot", "Center dot", false));
    private final BooleanSetting thirdPerson = addSetting(new BooleanSetting("third_person", "Show in third person", true));
    private final BooleanSetting cooldownBar = addSetting(new BooleanSetting("cooldown_bar", "Attack cooldown bar", true));
    private final ChoiceSetting color = addSetting(new ChoiceSetting("color", "Color", List.of("Theme", "White", "Green"), "Theme"));

    public CustomCrosshairModule() {
        super("custom_crosshair", "CustomCrosshair", "Cooldown-reactive cross, circle or dot with outline and attack indicator.", ModuleCategory.RENDER);
    }

    public void draw(GuiGraphics graphics, Minecraft client, float partial, int themeColor) {
        if (client.screen != null || (!thirdPerson.value() && !client.options.getCameraType().isFirstPerson())) return;
        int x = graphics.guiWidth() / 2, y = graphics.guiHeight() / 2;
        float cooldown = client.player.getAttackStrengthScale(partial);
        int expansion = expansion(cooldown, dynamic.value());
        int t = (int) thickness.value(), length = (int) size.value(), g = (int) gap.value() + expansion;
        int argb = switch (color.value()) { case "White" -> 0xFFFFFFFF; case "Green" -> 0xFF69E59B; default -> themeColor; };
        if (shape.value().equals("Circle")) {
            int radius = length + expansion;
            // Bounded integer annulus; only the circumference is submitted as quads.
            if (outline.value()) ring(graphics, x, y, Math.max(0, radius - t - 1), radius + 1, 0xDD080910);
            ring(graphics, x, y, Math.max(0, radius - t), radius, argb);
        } else if (shape.value().equals("Cross")) {
            int low = t / 2;
            rect(graphics, x - low, y - g - length, t, length, argb);
            rect(graphics, x - low, y + g, t, length, argb);
            rect(graphics, x - g - length, y - low, length, t, argb);
            rect(graphics, x + g, y - low, length, t, argb);
        }
        if (dot.value() || shape.value().equals("Dot")) rect(graphics, x - t / 2, y - t / 2, t, t, argb);
        if (cooldownBar.value() && cooldown < 1) {
            int top = y + Math.max(12, length + g + 4);
            graphics.fill(x - 8, top, x + 8, top + 2, 0xDD080910);
            graphics.fill(x - 8, top, x - 8 + Math.round(16 * Math.max(0, cooldown)), top + 2, argb);
        }
    }

    public static int expansion(float cooldown, double multiplier) {
        if (!Float.isFinite(cooldown) || !Double.isFinite(multiplier)) return 0;
        return (int) Math.round(Math.max(0, Math.min(10, multiplier)) * (1 - Math.max(0, Math.min(1, cooldown))));
    }

    private void rect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (outline.value()) graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xDD080910);
        graphics.fill(x, y, x + width, y + height, color);
    }

    private static void ring(GuiGraphics graphics, int cx, int cy, int inner, int outer, int color) {
        for (int dy = -outer; dy <= outer; dy++) {
            int extent = (int) Math.floor(Math.sqrt(outer * outer - dy * dy));
            int hole = Math.abs(dy) < inner ? (int) Math.floor(Math.sqrt(inner * inner - dy * dy)) : -1;
            if (hole < 0) graphics.fill(cx - extent, cy + dy, cx + extent + 1, cy + dy + 1, color);
            else {
                graphics.fill(cx - extent, cy + dy, cx - hole, cy + dy + 1, color);
                graphics.fill(cx + hole + 1, cy + dy, cx + extent + 1, cy + dy + 1, color);
            }
        }
    }
}
