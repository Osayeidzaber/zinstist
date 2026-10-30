package dev.jilaton.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Original Jilaton angular J, shared by menus and HUD; normalized to a 24-unit grid. */
public final class JilatonLogo {
    private JilatonLogo() { }

    public static void draw(GuiGraphics graphics, int x, int y, int size, int accent) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(size / 24F, size / 24F);
        graphics.fill(4, 3, 21, 7, accent);
        graphics.fill(17, 7, 21, 17, accent);
        graphics.fill(14, 17, 20, 20, accent);
        graphics.fill(7, 20, 17, 23, accent);
        graphics.fill(3, 17, 7, 21, accent);
        graphics.fill(3, 13, 7, 17, accent);
        graphics.pose().popMatrix();
    }
}
