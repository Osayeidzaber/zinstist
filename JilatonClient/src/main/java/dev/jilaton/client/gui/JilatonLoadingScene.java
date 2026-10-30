package dev.jilaton.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Paints the branded startup overlay: the shared Jilaton backdrop, the wordmark with a travelling
 * shine, and the progress bar. All geometry comes from {@link #layout} so nothing can leave the window.
 */
public final class JilatonLoadingScene {
    /** Brand violet from the Jilaton mark. */
    public static final int ACCENT = 0xFF8878FF;

    private static final int TEXTURE_WIDTH = 2048;
    private static final int TEXTURE_HEIGHT = 512;
    /** Wordmark pixels that are not fully transparent, as fractions of the texture. */
    private static final float TEXT_TOP = 107.0F / TEXTURE_HEIGHT;
    private static final float TEXT_BOTTOM = 406.0F / TEXTURE_HEIGHT;
    /** One shine band per this many texture columns keeps every slice an exact 64 px strip. */
    private static final int STRIPS = 32;

    private static final int BASE_TINT = 0xFAF8FF;
    private static final double SHINE_CYCLE_SECONDS = 2.6;
    private static final double SHINE_HALF_WIDTH = 0.15;
    /** {half-width, peak alpha}: a soft halo, a mid sweep and a sharp glint on top of each other. */
    private static final double[][] SHINE_LAYERS = {{0.36, 0.22}, {0.15, 0.50}, {0.055, 1.0}};

    private JilatonLoadingScene() { }

    /** Wordmark and progress-bar rectangles for a GUI-scaled window, clamped to stay on screen. */
    public record Layout(int logoX, int logoY, int logoWidth, int logoHeight,
                         int barX, int barY, int barWidth, int barHeight) {
        public int textTop() {
            return logoY + Math.round(logoHeight * TEXT_TOP);
        }

        public int textBottom() {
            return logoY + Math.round(logoHeight * TEXT_BOTTOM);
        }
    }

    public static Layout layout(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        int margin = Math.max(6, Math.min(24, Math.round(Math.min(w, h) * 0.05F)));

        int logoHeight = Math.round(w * 0.155F);
        int widthCap = Math.max(8, (w - margin * 2) / 4);
        int heightCap = Math.max(8, Math.round((h - margin * 2) / 0.86F));
        logoHeight = Math.max(8, Math.min(logoHeight, Math.min(widthCap, heightCap)));
        int logoWidth = logoHeight * 4;
        int logoX = (w - logoWidth) / 2;

        int textHeight = Math.round(logoHeight * (TEXT_BOTTOM - TEXT_TOP));
        int barHeight = Math.max(2, Math.round(logoHeight / 40.0F));
        int gap = Math.max(8, Math.min(28, Math.round(logoHeight * 0.22F)));
        int blockHeight = textHeight + gap + barHeight;

        int textTop = Math.round(h * 0.45F - blockHeight / 2.0F);
        int maxTop = h - margin - blockHeight;
        textTop = Math.max(margin, Math.min(textTop, Math.max(margin, maxTop)));

        int logoY = textTop - Math.round(logoHeight * TEXT_TOP);
        int barWidth = Math.min(Math.max(72, Math.round(logoWidth * 0.55F)), w - margin * 2);
        int barX = (w - barWidth) / 2;
        int barY = Math.max(0, Math.min(textTop + textHeight + gap, h - barHeight));
        return new Layout(logoX, logoY, logoWidth, logoHeight, barX, barY, barWidth, barHeight);
    }

    /**
     * Brightness of the shine at a horizontal position of the wordmark (0 = left, 1 = right).
     * The band sweeps past the text once per {@link #SHINE_CYCLE_SECONDS} and rests off-screen in between.
     */
    public static double shine(double time, double fraction) {
        return shine(time, fraction, SHINE_HALF_WIDTH);
    }

    private static double shine(double time, double fraction, double halfWidth) {
        double phase = (time % SHINE_CYCLE_SECONDS) / SHINE_CYCLE_SECONDS;
        double center = -0.32 + 1.64 * phase;
        double distance = (fraction - center) / halfWidth;
        if (Math.abs(distance) >= 1.0) return 0.0;
        double falloff = 1.0 - distance * distance;
        return falloff * falloff;
    }

    public static void render(GuiGraphics graphics, int width, int height, float opacity, float progress,
                              double time, boolean showProgress) {
        if (opacity <= 0.004F) return;
        JilatonBackdrop.render(graphics, width, height, time, 0, 0, 1.0, opacity);
        int alpha = Math.round(opacity * 255.0F);
        if (alpha < 3) return;

        Layout layout = layout(width, height);
        graphics.blit(RenderPipelines.GUI_TEXTURED, JilatonLoadingLogo.ID, layout.logoX(), layout.logoY(),
                0.0F, 0.0F, layout.logoWidth(), layout.logoHeight(), TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, Math.round(alpha * 0.90F) << 24 | BASE_TINT);
        drawShine(graphics, layout, alpha, time);
        if (showProgress) drawProgress(graphics, layout, alpha, progress, time);
    }

    /** Re-draws narrow slices of the wordmark at increasing alpha so light glides across the text. */
    private static void drawShine(GuiGraphics graphics, Layout layout, int alpha, double time) {
        for (double[] layer : SHINE_LAYERS) {
            drawShineLayer(graphics, layout, alpha, time, layer[0], layer[1]);
        }
    }

    private static void drawShineLayer(GuiGraphics graphics, Layout layout, int alpha, double time,
                                       double halfWidth, double peak) {
        int stripWidth = TEXTURE_WIDTH / STRIPS;
        for (int strip = 0; strip < STRIPS; strip++) {
            double glow = shine(time, (strip + 0.5) / STRIPS, halfWidth);
            int stripAlpha = (int) Math.round(alpha * peak * glow);
            if (stripAlpha < 4) continue;
            int x0 = layout.logoX() + layout.logoWidth() * strip / STRIPS;
            int x1 = layout.logoX() + layout.logoWidth() * (strip + 1) / STRIPS;
            if (x1 <= x0) continue;
            graphics.blit(RenderPipelines.GUI_TEXTURED, JilatonLoadingLogo.ID, x0, layout.logoY(),
                    strip * stripWidth, 0.0F, x1 - x0, layout.logoHeight(), stripWidth, TEXTURE_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT, stripAlpha << 24 | 0xFFFFFF);
        }
    }

    private static void drawProgress(GuiGraphics graphics, Layout layout, int alpha, float progress, double time) {
        int x = layout.barX();
        int y = layout.barY();
        int w = layout.barWidth();
        int h = layout.barHeight();
        int cap = h / 2;
        JilatonUi.fillRounded(graphics, x, y, w, h, cap, Math.round(alpha * 0.16F) << 24 | 0xFFFFFF);

        int filled = Math.round(w * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled <= 0) return;
        int fill = JilatonUi.mix(ACCENT, 0xFFFFFFFF, 0.22) & 0xFFFFFF;
        JilatonUi.fillRounded(graphics, x, y, filled, h, cap, alpha << 24 | fill);
        graphics.fill(x, y + h, x + filled, y + h + 1, (alpha / 6) << 24 | 0xFFFFFF);

        int glintWidth = Math.max(10, w / 6);
        int glint = x + (int) Math.round((time * 0.9 % 1.0) * (w + glintWidth)) - glintWidth;
        int glintStart = Math.max(x, glint);
        int glintEnd = Math.min(x + filled, glint + glintWidth);
        if (glintEnd > glintStart) {
            graphics.fill(glintStart, y, glintEnd, y + h, Math.round(alpha * 0.32F) << 24 | 0xFFFFFF);
        }

        if (filled < w) {
            int head = x + filled;
            graphics.fill(head - 3, y - 1, head + 1, y + h + 1, Math.round(alpha * 0.45F) << 24 | 0xFFFFFF);
            graphics.fill(head - 1, y - 2, head + 1, y + h + 2, alpha << 24 | 0xFFFFFF);
        }
    }
}
