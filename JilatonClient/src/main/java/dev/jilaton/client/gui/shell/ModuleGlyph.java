package dev.jilaton.client.gui.shell;

import net.minecraft.client.gui.GuiGraphics;

/** A stable geometric glyph per module id, so cards are recognisable without shipped textures. */
public final class ModuleGlyph {
    private static final int UNITS = 16;

    private ModuleGlyph() { }

    public static void draw(GuiGraphics graphics, String id, int x, int y, int size, int color) {
        if (size <= 2) return;
        float scale = size / (float) UNITS;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        switch (Math.floorMod(seed(id), 8)) {
            case 0 -> {
                graphics.fill(7, 2, 9, 14, color);
                graphics.fill(5, 4, 11, 12, color);
                graphics.fill(3, 6, 13, 10, color);
            }
            case 1 -> {
                graphics.fill(7, 2, 9, 14, color);
                graphics.fill(2, 7, 14, 9, color);
                graphics.fill(6, 6, 10, 10, color);
            }
            case 2 -> {
                graphics.fill(3, 6, 5, 14, color);
                graphics.fill(7, 3, 9, 14, color);
                graphics.fill(11, 6, 13, 14, color);
            }
            case 3 -> {
                graphics.fill(3, 3, 7, 7, color);
                graphics.fill(9, 3, 13, 7, color);
                graphics.fill(3, 9, 7, 13, color);
                graphics.fill(9, 9, 13, 13, color);
            }
            case 4 -> {
                line(graphics, 3, 12, 12, 3, color);
                graphics.fill(9, 2, 13, 4, color);
                graphics.fill(2, 10, 4, 14, color);
            }
            case 5 -> {
                graphics.fill(7, 3, 9, 5, color);
                graphics.fill(4, 5, 12, 7, color);
                graphics.fill(2, 7, 14, 9, color);
                graphics.fill(4, 9, 12, 11, color);
                graphics.fill(7, 11, 9, 13, color);
                graphics.fill(7, 6, 9, 10, color);
            }
            case 6 -> {
                line(graphics, 3, 3, 12, 12, color);
                line(graphics, 12, 3, 3, 12, color);
            }
            default -> {
                graphics.fill(2, 4, 14, 6, color);
                graphics.fill(2, 8, 14, 10, color);
                graphics.fill(2, 12, 14, 14, color);
            }
        }
        graphics.pose().popMatrix();
    }

    private static int seed(String id) {
        int hash = id == null ? 0 : id.hashCode();
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        return hash;
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) break;
            int doubled = error * 2;
            if (doubled >= dy) {
                error += dy;
                x0 += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }
}
