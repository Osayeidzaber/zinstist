package dev.jilaton.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Jilaton's dark design tokens and the small set of drawing primitives every screen shares. */
public final class JilatonUi {
    /** Base canvas: near-pure black. */
    public static final int INK = 0xFF000000;
    /** Layered surfaces from deepest to most raised. */
    public static final int SURFACE = 0xFF080808;
    public static final int SURFACE_2 = 0xFF0E0E0E;
    public static final int RAISED = 0xFF151515;
    public static final int RAISED_HOVER = 0xFF1C1C1C;
    /** Fine borders. */
    public static final int HAIRLINE = 0xFF1F1F1F;
    public static final int HAIRLINE_STRONG = 0xFF2E2E2E;
    /** Typography. */
    public static final int TEXT = 0xFFF2F2F2;
    public static final int TEXT_2 = 0xFFB8B8B8;
    public static final int TEXT_3 = 0xFF8C8C8C;
    public static final int TEXT_4 = 0xFF6A6A6A;
    public static final int DANGER = 0xFFE5797F;

    private JilatonUi() { }

    public static int alpha(int color, double opacity) {
        int base = color >>> 24;
        int value = (int) Math.round(base * Math.max(0, Math.min(1, opacity)));
        return value << 24 | color & 0xFFFFFF;
    }

    public static int mix(int from, int to, double t) {
        double k = Math.max(0, Math.min(1, t));
        int a = lerp(from >>> 24, to >>> 24, k);
        int r = lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, k);
        int g = lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, k);
        int b = lerp(from & 0xFF, to & 0xFF, k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    /** Relative luminance in 0..1 using Rec. 709 weights. */
    public static double luminance(int color) {
        return (0.2126 * (color >> 16 & 0xFF) + 0.7152 * (color >> 8 & 0xFF) + 0.0722 * (color & 0xFF)) / 255.0;
    }

    /** Readable foreground on top of the given fill: ink on light accents, white on dark ones. */
    public static int onColor(int fill) {
        return luminance(fill) > 0.58 ? 0xFF050505 : TEXT;
    }

    /** One-pixel border drawn inside the rectangle. */
    public static void stroke(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /** Horizontal inset of corner row {@code row} for a true circular corner of {@code radius}. */
    /** Rounded rectangle with anti-aliased circular corners, snapped to physical pixels. */
    public static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0 || color >>> 24 == 0) return;
        dev.jilaton.client.gui.design.Paint.round(graphics, x, y, width, height, radius, color);
    }

    /** Hairline outline (about half a GUI pixel, never under one physical pixel) matching {@link #fillRounded}. */
    public static void strokeRounded(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 2 || height <= 2) return;
        dev.jilaton.client.gui.design.Paint.outline(graphics, x, y, width, height, radius, 0.5F, color);
    }

    /** Soft drop shadow: a few expanding, very low-alpha layers beneath a surface. */
    public static void shadow(GuiGraphics graphics, int x, int y, int width, int height, int radius, double strength) {
        for (int layer = 4; layer >= 1; layer--) {
            int a = (int) Math.round(18 * strength / layer);
            if (a <= 0) continue;
            fillRounded(graphics, x - layer * 2, y - layer * 2 + layer, width + layer * 4, height + layer * 4,
                    radius + layer * 2, a << 24);
        }
    }
}
