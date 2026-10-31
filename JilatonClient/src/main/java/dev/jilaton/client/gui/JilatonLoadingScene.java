package dev.jilaton.client.gui;

import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.gui.design.Paint;
import dev.jilaton.client.gui.design.Ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Paints the branded startup overlay: the shared Jilaton backdrop, the wordmark with a travelling
 * shine, and the progress bar. All geometry comes from {@link #layout} so nothing can leave the window.
 */
public final class JilatonLoadingScene {
    /** Brand white from the Jilaton mark. */
    public static final int ACCENT = 0xFFF5F5F5;

    private static final int TEXTURE_HEIGHT = 512;
    /** Wordmark pixels that are not fully transparent, as fractions of the texture. */
    private static final float TEXT_TOP = 107.0F / TEXTURE_HEIGHT;
    private static final float TEXT_BOTTOM = 406.0F / TEXTURE_HEIGHT;
    /** One shine band per this many texture columns keeps every slice an exact 64 px strip. */
    private static final int STRIPS = 32;

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
        // Board "Loading screen": deep black, a faint white light, the mark in a round badge circled by the progress
        // ring, and the wordmark with its gliding shine. Everything is anti-aliased and drawn 1:1 in physical pixels
        // (Paint). No font text: fonts are not loaded yet while this screen is up.
        graphics.fill(0, 0, width, height, JilatonUi.alpha(Ui.DEEP, opacity));
        Canvas canvas = new Canvas().fit(width, height);
        graphics.pose().pushMatrix();
        float scale = canvas.scale();
        graphics.pose().translate((width - Canvas.W * scale) / 2F, (height - Canvas.H * scale) / 2F);
        graphics.pose().scale(scale, scale);
        double breathe = 0.5 + 0.5 * Math.sin(time * 1.6);
        Ui.glow(graphics, 640, 300, 360, 0xFFFFFF, (0.05 + 0.025 * breathe) * opacity);

        float cx = 640, cy = 292, r = 66;
        // Ripples: two rings leaving the badge and fading, one every 1.4 s.
        for (int i = 0; i < 2; i++) {
            double phase = ((time / 1.4) + i * 0.5) % 1.0;
            double eased = 1 - Math.pow(1 - phase, 2);
            Ui.ring(graphics, cx, cy, (float) (r + 6 + 46 * eased), 1, JilatonUi.alpha(Ui.TEXT, 0.16 * (1 - phase) * opacity));
        }
        // Progress ring: hairline track plus a 2px white arc; while waiting on the first step, a comet circles.
        Ui.ring(graphics, cx, cy, r, 1, JilatonUi.alpha(Ui.TEXT, 0.10 * opacity));
        float done = showProgress ? Math.max(0, Math.min(1, progress)) : 0;
        arc(graphics, cx, cy, r, -Math.PI / 2, done, 2.2F, JilatonUi.alpha(Ui.TEXT, 0.95 * opacity));
        double head = -Math.PI / 2 + time * 2.6;
        for (int i = 0; i < 18; i++) {
            double a = head - i * 0.05;
            float px = (float) (cx + Math.cos(a) * r), py = (float) (cy + Math.sin(a) * r);
            Ui.circle(graphics, px, py, 1.6F, JilatonUi.alpha(Ui.TEXT, (1 - i / 18.0) * 0.55 * opacity * (1 - done)));
        }
        // The mark in its badge, breathing very slightly.
        float badge = (float) (50 + 1.2 * breathe);
        Ui.circle(graphics, cx, cy, badge, JilatonUi.alpha(Ui.CARD, opacity));
        Ui.ring(graphics, cx, cy, badge, 1, JilatonUi.alpha(Ui.TEXT, 0.14 * opacity));
        Ui.mark(graphics, cx - 24, cy - 24, 48, JilatonUi.alpha(Ui.TEXT, opacity));

        // Wordmark (text only; the mark is already above), resampled to its exact pixel size.
        float wordW = 300, wordH = wordW * WORD_H / WORD_W, wordX = cx - wordW / 2, wordY = 402;
        int alpha = Math.round(opacity * 255.0F);
        Paint.image(graphics, LOGO_PATH, WORD_X, WORD_Y, WORD_W, WORD_H, wordX, wordY, wordW, wordH,
                Math.round(alpha * 0.66F) << 24 | 0xFFFFFF, 0, 1);
        for (double[] layer : SHINE_LAYERS) {
            for (int strip = 0; strip < STRIPS; strip++) {
                double glow = shine(time, (strip + 0.5) / STRIPS, layer[0]);
                int stripAlpha = (int) Math.round(alpha * layer[1] * glow * 0.6);
                if (stripAlpha < 3) continue;
                Paint.image(graphics, LOGO_PATH, WORD_X, WORD_Y, WORD_W, WORD_H, wordX, wordY, wordW, wordH,
                        stripAlpha << 24 | 0xFFFFFF, strip / (double) STRIPS, (strip + 1) / (double) STRIPS);
            }
        }
        graphics.pose().popMatrix();
    }

    private static final String LOGO_PATH = "/assets/jilaton/textures/brand/loading-logo.png";
    /** The "JILATON / CLIENT" lettering inside the 2048x512 logo texture. */
    private static final int WORD_X = 600, WORD_Y = 100, WORD_W = 1196, WORD_H = 312;

    /** Anti-aliased arc of {@code fraction} of a turn from angle {@code start}, as overlapping discs. */
    private static void arc(GuiGraphics graphics, float cx, float cy, float r, double start, float fraction,
                            float stroke, int color) {
        if (fraction <= 0.001F) return;
        int steps = Math.max(2, Math.round(r * 2 * (float) Math.PI * fraction / 0.9F));
        for (int i = 0; i <= steps; i++) {
            double a = start + Math.PI * 2 * fraction * i / steps;
            Ui.circle(graphics, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), stroke / 2, color);
        }
    }
}
