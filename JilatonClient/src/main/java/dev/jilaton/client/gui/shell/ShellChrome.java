package dev.jilaton.client.gui.shell;

import dev.jilaton.client.gui.JilatonUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Painting primitives shared by every part of the ClickGUI shell. All art is drawn from rectangles. */
public final class ShellChrome {
    /** Unit glyph box; callers scale it up for 16 px nav icons and 32 px card tiles. */
    private static final int ICON = 10;

    private ShellChrome() { }

    public static void backdrop(GuiGraphics graphics, int width, int height, double visibility) {
        graphics.fill(0, 0, width, height, JilatonUi.alpha(ShellPalette.SCRIM, visibility));
    }

    /** Panel, sidebar column and the hairline that separates them. No accent decoration. */
    public static void panel(GuiGraphics graphics, ShellLayout layout, double visibility) {
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelWidth();
        int h = layout.panelHeight();
        JilatonUi.shadow(graphics, x, y, w, h, 11, visibility * 1.1);
        JilatonUi.fillRounded(graphics, x, y, w, h, 10, ShellPalette.PANEL);
        JilatonUi.fillRounded(graphics, x + 1, y + 1, layout.sidebarWidth() - 1, h - 2, 9, ShellPalette.SIDEBAR);
        JilatonUi.strokeRounded(graphics, x, y, w, h, 10, ShellPalette.STROKE);
        int divider = layout.sidebarRight();
        graphics.fill(divider, y + 1, divider + 1, y + h - 1, ShellPalette.STROKE);
    }

    public static void headerRule(GuiGraphics graphics, ShellLayout layout) {
        int y = layout.headerBottom();
        graphics.fill(layout.contentX(), y, layout.contentRight(), y + 1, ShellPalette.STROKE);
    }

    public static void searchFrame(GuiGraphics graphics, int x, int y, int w, int h, int accent, boolean focused) {
        JilatonUi.fillRounded(graphics, x, y, w, h, 8, focused
                ? JilatonUi.mix(ShellPalette.CONTROL, accent, 0.10) : ShellPalette.CONTROL);
        JilatonUi.strokeRounded(graphics, x, y, w, h, 8, focused ? JilatonUi.mix(ShellPalette.STROKE, accent, 0.5) : ShellPalette.STROKE);
        magnifier(graphics, x + 10, y + h / 2 - 5, focused ? ShellPalette.TEXT_2 : ShellPalette.TEXT_3);
    }

    public static void magnifier(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 6, y + 1, color);
        graphics.fill(x, y + 1, x + 1, y + 6, color);
        graphics.fill(x + 6, y + 1, x + 7, y + 6, color);
        graphics.fill(x + 1, y + 6, x + 6, y + 7, color);
        graphics.fill(x + 5, y + 5, x + 9, y + 7, color);
    }

    /** 36x20 switch; the knob slides 16 px and the track fades from stroke to accent. */
    public static void toggle(GuiGraphics graphics, int x, int y, double amount, int accent) {
        double t = Math.max(0, Math.min(1, amount));
        JilatonUi.fillRounded(graphics, x, y, 36, 20, 10, JilatonUi.mix(ShellPalette.STROKE, accent, t));
        int knob = x + 3 + (int) Math.round(16 * t);
        JilatonUi.fillRounded(graphics, knob, y + 3, 14, 14, 7,
                JilatonUi.mix(0xFF8E8A9A, JilatonUi.onColor(accent), t));
    }

    /** Down/up caret used by settings expansion. */
    public static void chevron(GuiGraphics graphics, int x, int y, boolean down, int color) {
        int step = down ? 1 : -1;
        int start = down ? y : y + 4;
        for (int i = 0; i < 3; i++) {
            int yy = start + step * i;
            graphics.fill(x + i, yy, x + i + 2, yy + 2, color);
            graphics.fill(x + 6 - i, yy, x + 8 - i, yy + 2, color);
        }
    }

    /** Right-pointing caret that marks the active sidebar row. */
    public static void navChevron(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 7, color);
        graphics.fill(x + 1, y + 1, x + 2, y + 6, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 5, color);
        graphics.fill(x + 3, y + 3, x + 4, y + 4, color);
    }

    public static int keycapWidth(Font font, String label) {
        return font.width(label) + 10;
    }

    public static void keycap(GuiGraphics graphics, Font font, String label, int x, int y,
                              int accent, boolean binding) {
        int width = keycapWidth(font, label);
        JilatonUi.fillRounded(graphics, x, y, width, 13, 4,
                binding ? ShellPalette.accentSurface(accent) : ShellPalette.CONTROL);
        JilatonUi.strokeRounded(graphics, x, y, width, 13, 4, binding ? accent : ShellPalette.STROKE);
        graphics.drawString(font, label, x + 5, y + 3, binding ? accent : ShellPalette.TEXT_2, false);
    }

    /** Sidebar count chip, right-aligned in the row. Accent only when the row is active. */
    public static void countPill(GuiGraphics graphics, Font font, String text, int right, int y,
                                 boolean active, int accent) {
        int width = font.width(text) + 10;
        int x = right - width;
        JilatonUi.fillRounded(graphics, x, y, width, 14, 4,
                active ? ShellPalette.accentSurface(accent) : ShellPalette.CONTROL);
        graphics.drawString(font, text, x + 5, y + 3,
                active ? accent : ShellPalette.TEXT_3, false);
    }

    /** 32x32 icon tile with an accent-tinted fill. */
    public static void iconTile(GuiGraphics graphics, int x, int y, int size, int accent) {
        JilatonUi.fillRounded(graphics, x, y, size, size, 8, ShellPalette.accentSurface(accent));
        JilatonUi.strokeRounded(graphics, x, y, size, size, 8, ShellPalette.accentBorder(accent));
    }

    /** Six-dot grip for card reordering. */
    public static void grip(GuiGraphics graphics, int x, int y, int color) {
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                graphics.fill(x + column * 4, y + row * 4, x + column * 4 + 2, y + row * 4 + 2, color);
            }
        }
    }

    /** Settings gear drawn from a ring plus four teeth. */
    public static void gear(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y, x + 7, y + 2, color);
        graphics.fill(x + 3, y + 8, x + 7, y + 10, color);
        graphics.fill(x, y + 3, x + 2, y + 7, color);
        graphics.fill(x + 8, y + 3, x + 10, y + 7, color);
        graphics.fill(x + 2, y + 2, x + 8, y + 3, color);
        graphics.fill(x + 2, y + 7, x + 8, y + 8, color);
        graphics.fill(x + 3, y + 3, x + 4, y + 7, color);
        graphics.fill(x + 6, y + 3, x + 7, y + 7, color);
    }

    /** Small blocky avatar tile: head and shoulders in the shell palette. */
    public static void avatar(GuiGraphics graphics, int x, int y, int size, int accent) {
        JilatonUi.fillRounded(graphics, x, y, size, size, 7, ShellPalette.CONTROL);
        JilatonUi.strokeRounded(graphics, x, y, size, size, 7, ShellPalette.STROKE);
        int head = Math.max(4, size / 3);
        graphics.fill(x + (size - head) / 2, y + size / 5, x + (size + head) / 2, y + size / 5 + head,
                ShellPalette.TEXT_3);
        graphics.fill(x + size / 4, y + size / 5 + head + 2, x + size - size / 4, y + size - 4,
                ShellPalette.dimmed(accent));
    }

    public static void dot(GuiGraphics graphics, int x, int y, int color) {
        JilatonUi.fillRounded(graphics, x, y, 5, 5, 2, color);
    }

    /** Original 10x10 unit glyphs for sidebar rows and module tiles. */
    public static void icon(GuiGraphics graphics, String key, int x, int y, int color) {
        switch (key) {
            case "all" -> {
                graphics.fill(x, y, x + 4, y + 4, color);
                graphics.fill(x + 6, y, x + 10, y + 4, color);
                graphics.fill(x, y + 6, x + 4, y + 10, color);
                graphics.fill(x + 6, y + 6, x + 10, y + 10, color);
            }
            case "combat" -> {
                line(graphics, x, y + 9, x + 8, y + 1, color);
                line(graphics, x + 2, y, x + 9, y + 7, color);
                graphics.fill(x, y + 8, x + 2, y + 10, color);
                graphics.fill(x + 8, y, x + 10, y + 2, color);
            }
            case "movement" -> {
                line(graphics, x, y, x + 5, y + 5, color);
                line(graphics, x, y + 9, x + 5, y + 5, color);
                graphics.fill(x + 4, y + 4, x + 10, y + 6, color);
            }
            case "player" -> {
                graphics.fill(x + 3, y, x + 7, y + 4, color);
                graphics.fill(x + 2, y + 5, x + 8, y + 6, color);
                graphics.fill(x + 1, y + 6, x + 9, y + 10, color);
                graphics.fill(x + 4, y + 4, x + 6, y + 5, color);
            }
            case "visuals" -> {
                graphics.fill(x + 4, y, x + 6, y + 2, color);
                graphics.fill(x + 2, y + 2, x + 4, y + 4, color);
                graphics.fill(x + 6, y + 2, x + 8, y + 4, color);
                graphics.fill(x, y + 4, x + 2, y + 6, color);
                graphics.fill(x + 8, y + 4, x + 10, y + 6, color);
                graphics.fill(x + 2, y + 6, x + 4, y + 8, color);
                graphics.fill(x + 6, y + 6, x + 8, y + 8, color);
                graphics.fill(x + 4, y + 8, x + 6, y + 10, color);
                graphics.fill(x + 4, y + 4, x + 6, y + 6, color);
            }
            case "client" -> {
                graphics.fill(x, y + 1, x + 10, y + 3, color);
                graphics.fill(x, y + 7, x + 10, y + 9, color);
                graphics.fill(x + 3, y + 3, x + 7, y + 7, color);
            }
            case "settings" -> gear(graphics, x, y, color);
            case "configs" -> {
                graphics.fill(x, y, x + 4, y + 2, color);
                graphics.fill(x + 3, y + 1, x + 8, y + 2, color);
                graphics.fill(x, y + 2, x + 9, y + 10, color);
                graphics.fill(x + 1, y + 4, x + 8, y + 5, ShellPalette.SIDEBAR);
            }
            case "theme" -> {
                graphics.fill(x, y, x + 4, y + 4, ShellPalette.TEXT);
                graphics.fill(x + 5, y, x + 9, y + 4, ShellPalette.TEXT_2);
                graphics.fill(x, y + 5, x + 4, y + 9, ShellPalette.TEXT_3);
                graphics.fill(x + 5, y + 5, x + 9, y + 9, color);
            }
            default -> {
            }
        }
    }

    /** Scales the 10 px unit glyph to an arbitrary box (16 px nav rows, 16 px card tiles). */
    public static void icon(GuiGraphics graphics, String key, int x, int y, int size, int color) {
        float scale = size / (float) ICON;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        icon(graphics, key, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 2, y0 + 2, color);
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

    public static int unit() {
        return ICON;
    }
}
