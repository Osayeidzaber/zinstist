package dev.jilaton.client.gui;

import dev.jilaton.client.JilatonClient;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Jilaton's replacements for vanilla widget sprites. Vanilla still lays out, labels and handles input;
 * these only paint the surfaces, so every vanilla and modded screen inherits the same flat dark style.
 */
public final class JilatonWidgets {
    private JilatonWidgets() { }

    public static boolean reducedMotion() {
        return JilatonClient.clientConfig() != null && JilatonClient.clientConfig().reducedMotion();
    }

    /** Button surface. {@code hover} is an eased 0..1 value; {@code alpha} is the widget's own opacity. */
    public static void button(GuiGraphics graphics, int x, int y, int w, int h, boolean active, double hover, float alpha) {
        if (alpha <= 0.01F || w <= 0 || h <= 0) return;
        if (!active) {
            JilatonUi.fillRounded(graphics, x, y, w, h, 2, JilatonUi.alpha(0xB0070707, alpha));
            JilatonUi.strokeRounded(graphics, x, y, w, h, 2, JilatonUi.alpha(0xFF171717, alpha));
            return;
        }
        JilatonUi.fillRounded(graphics, x, y, w, h, 2, JilatonUi.alpha(JilatonUi.mix(0xE80C0C0C, 0xF61B1B1B, hover), alpha));
        JilatonUi.strokeRounded(graphics, x, y, w, h, 2, JilatonUi.alpha(JilatonUi.mix(0xFF272727, 0xFF6E6E6E, hover), alpha));
        // A white underline grows from the center on hover.
        int line = (int) Math.round((w - 6) * hover);
        if (line > 0) {
            int cx = x + w / 2;
            graphics.fill(cx - line / 2, y + h - 2, cx + (line + 1) / 2, y + h - 1, JilatonUi.alpha(JilatonUi.TEXT, alpha * hover));
        }
    }

    public static void sliderTrack(GuiGraphics graphics, int x, int y, int w, int h, double value, double hover, boolean active, float alpha) {
        button(graphics, x, y, w, h, active, hover * 0.6, alpha);
        if (!active || alpha <= 0.01F) return;
        int filled = (int) Math.round((w - 2) * Math.max(0, Math.min(1, value)));
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, JilatonUi.alpha(0x1EFFFFFF, alpha));
    }

    public static void sliderHandle(GuiGraphics graphics, int x, int y, int w, int h, double hover, boolean active, float alpha) {
        if (alpha <= 0.01F) return;
        int cx = x + w / 2;
        int color = active ? JilatonUi.TEXT : JilatonUi.TEXT_4;
        if (hover > 0.01) graphics.fill(cx - 3, y + 2, cx + 3, y + h - 2, JilatonUi.alpha(0x30FFFFFF, alpha * hover));
        graphics.fill(cx - 1, y + 3, cx + 1, y + h - 3, JilatonUi.alpha(color, alpha));
    }

    public static void field(GuiGraphics graphics, int x, int y, int w, int h, boolean focused, boolean active) {
        JilatonUi.fillRounded(graphics, x, y, w, h, 2, 0xF2040404);
        int border = !active ? 0xFF161616 : focused ? 0xFFA6A6A6 : 0xFF2A2A2A;
        JilatonUi.strokeRounded(graphics, x, y, w, h, 2, border);
    }

    public static void checkbox(GuiGraphics graphics, int x, int y, int size, boolean selected, boolean focused, float alpha) {
        if (alpha <= 0.01F) return;
        JilatonUi.fillRounded(graphics, x, y, size, size, 2, JilatonUi.alpha(0xF0080808, alpha));
        JilatonUi.strokeRounded(graphics, x, y, size, size, 2, JilatonUi.alpha(focused ? 0xFF8A8A8A : 0xFF2E2E2E, alpha));
        if (selected) graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, JilatonUi.alpha(JilatonUi.TEXT, alpha));
    }

    /** List row highlight; vanilla passes white for the focused list and gray otherwise. */
    public static void selection(GuiGraphics graphics, int x1, int y1, int x2, int y2, boolean focused) {
        graphics.fill(x1, y1, x2, y2, 0x14FFFFFF);
        JilatonUi.stroke(graphics, x1, y1, x2 - x1, y2 - y1, focused ? 0xFF9C9C9C : 0xFF2C2C2C);
        graphics.fill(x1, y1 + 1, x1 + 2, y2 - 1, focused ? JilatonUi.TEXT : JilatonUi.TEXT_3);
    }

    public static void listBackground(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, 0x66000000);
    }

    public static void listSeparators(GuiGraphics graphics, int x1, int top, int x2, int bottom) {
        graphics.fill(x1, top - 1, x2, top, JilatonUi.HAIRLINE_STRONG);
        graphics.fill(x1, bottom, x2, bottom + 1, JilatonUi.HAIRLINE_STRONG);
    }
}
