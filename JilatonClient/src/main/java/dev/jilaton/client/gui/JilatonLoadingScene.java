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
        // Editorial boot sequence. No font text: fonts are not loaded while this screen is up, so the numbers are
        // drawn as segment digits and the wordmark comes from the brand texture. Progress is the real reload
        // progress; nothing here is faked or delayed.
        graphics.fill(0, 0, width, height, JilatonUi.alpha(0xFF030303, opacity));
        Canvas canvas = new Canvas().fit(width, height);
        graphics.pose().pushMatrix();
        float scale = canvas.scale();
        graphics.pose().translate((width - Canvas.W * scale) / 2F, (height - Canvas.H * scale) / 2F);
        graphics.pose().scale(scale, scale);
        float done = showProgress ? Math.max(0, Math.min(1, progress)) : 0;
        // Leaving: when the overlay starts fading, everything blooms outward slightly.
        float leave = 1 - opacity;

        // 12-column hairline grid, drawn in from the top.
        double gridIn = Math.min(1, time / 0.8);
        for (int i = 1; i < 12; i++) {
            float x = 64 + i * (1152 / 12F);
            Ui.rect(graphics, x, 0, 1, (float) (720 * gridIn), JilatonUi.alpha(0xFFFFFFFF, 0.035 * opacity));
        }
        Ui.rect(graphics, 64, 96, 1152, 1, JilatonUi.alpha(0xFFFFFFFF, 0.08 * opacity));

        // Concentric rings around the mark; each draws as an arc in proportion to progress.
        float cx = 860, cy = 330;
        float[] radii = {110, 170, 240, 320};
        for (int i = 0; i < radii.length; i++) {
            float r = radii[i] * (1 + 0.25F * leave);
            Ui.ring(graphics, cx, cy, r, 1, JilatonUi.alpha(0xFFFFFFFF, (0.05 - i * 0.008) * opacity));
            double share = Math.max(0, Math.min(1, done * radii.length - i));
            arc(graphics, cx, cy, r, -Math.PI / 2 + i * 0.6, (float) share, 1.6F,
                    JilatonUi.alpha(0xFFFFFFFF, (0.55 - i * 0.1) * opacity));
            double a = time * (0.5 + i * 0.18) + i * 1.7;
            Ui.circle(graphics, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 2.2F - i * 0.3F,
                    JilatonUi.alpha(0xFFFFFFFF, 0.6 * opacity));
        }
        Ui.glow(graphics, cx, cy, 260, 0xFFFFFF, 0.05 * opacity);

        // The J assembles from its five blocks, then breathes.
        float size = 150, mx = cx - size / 2, my = cy - size / 2, u = size / 24F;
        float[][] blocks = {{4, 3, 17, 4}, {17, 7, 4, 10}, {14, 17, 6, 3}, {7, 20, 10, 3}, {3, 13, 4, 8}};
        float[][] from = {{-40, 0}, {0, -40}, {40, 0}, {0, 40}, {-40, 20}};
        for (int i = 0; i < blocks.length; i++) {
            double k = Math.max(0, Math.min(1, (time - 0.15 - i * 0.09) / 0.5));
            double e = 1 - Math.pow(1 - k, 3);
            float ox = (float) (from[i][0] * (1 - e)), oy = (float) (from[i][1] * (1 - e));
            Ui.rect(graphics, mx + blocks[i][0] * u + ox, my + blocks[i][1] * u + oy, blocks[i][2] * u, blocks[i][3] * u,
                    JilatonUi.alpha(0xFFF5F5F5, e * opacity));
        }

        // Left column: wordmark, then the percentage in segment digits.
        float wordW = 360, wordH = wordW * WORD_H / WORD_W, wordX = 64, wordY = 150;
        int alpha = Math.round(opacity * 255.0F);
        Paint.image(graphics, LOGO_PATH, WORD_X, WORD_Y, WORD_W, WORD_H, wordX, wordY, wordW, wordH,
                Math.round(alpha * 0.9F) << 24 | 0xFFFFFF, 0, 1);
        for (int strip = 0; strip < STRIPS; strip++) {
            double glow = shine(time, (strip + 0.5) / STRIPS, 0.12);
            int stripAlpha = (int) Math.round(alpha * glow * 0.45);
            if (stripAlpha < 3) continue;
            Paint.image(graphics, LOGO_PATH, WORD_X, WORD_Y, WORD_W, WORD_H, wordX, wordY, wordW, wordH,
                    stripAlpha << 24 | 0xFFFFFF, strip / (double) STRIPS, (strip + 1) / (double) STRIPS);
        }
        int percent = Math.round(done * 100);
        digits(graphics, String.valueOf(percent), 64, 420, 54, JilatonUi.alpha(0xFFF5F5F5, opacity));
        float pw = String.valueOf(percent).length() * 44;
        // A small % sign from two dots and a slash of rects.
        Ui.circle(graphics, 64 + pw + 10, 432, 4, JilatonUi.alpha(0xFFB0B0B0, opacity));
        Ui.circle(graphics, 64 + pw + 26, 466, 4, JilatonUi.alpha(0xFFB0B0B0, opacity));
        for (int i = 0; i < 9; i++) Ui.rect(graphics, 64 + pw + 26 - i * 2, 428 + i * 5, 2, 5, JilatonUi.alpha(0xFFB0B0B0, opacity));

        // Progress rule with a travelling light head and four stage ticks that light as real progress passes them.
        float ry = 620, rx = 64, rw = 1152;
        Ui.rect(graphics, rx, ry, rw, 1, JilatonUi.alpha(0xFFFFFFFF, 0.12 * opacity));
        Ui.rect(graphics, rx, ry, rw * done, 1, JilatonUi.alpha(0xFFFFFFFF, 0.85 * opacity));
        for (int i = 0; i < 18; i++) {
            float k = 1 - i / 18F;
            Ui.rect(graphics, rx + rw * done - i * 4, ry - 1, 4, 3, JilatonUi.alpha(0xFFFFFFFF, 0.6 * k * k * opacity));
        }
        for (int i = 1; i <= 4; i++) {
            float tx = rx + rw * i / 4F;
            boolean lit = done >= i / 4F - 0.001F;
            Ui.rect(graphics, tx - 1, ry - 8, 2, 16, JilatonUi.alpha(lit ? 0xFFFFFFFF : 0xFF3A3A3A, opacity));
            Ui.rect(graphics, tx - 30, ry + 18, 30, 2, JilatonUi.alpha(lit ? 0xFF9A9A9A : 0xFF2A2A2A, opacity));
        }
        graphics.pose().popMatrix();
    }

    /** Seven-segment digits drawn from rects, {@code h} tall, for text-free screens. */
    private static void digits(GuiGraphics g, String text, float x, float y, float h, int color) {
        float w = h * 0.62F, t = h * 0.1F;
        int[] masks = {0b1110111, 0b0010010, 0b1011101, 0b1011011, 0b0111010, 0b1101011, 0b1101111, 0b1010010, 0b1111111, 0b1111011};
        for (int i = 0; i < text.length(); i++) {
            int d = text.charAt(i) - '0';
            if (d < 0 || d > 9) continue;
            int m = masks[d];
            float ox = x + i * (w + h * 0.2F);
            float half = (h - t) / 2;
            if ((m & 0b1000000) != 0) Ui.rect(g, ox + t, y, w - 2 * t, t, color);
            if ((m & 0b0100000) != 0) Ui.rect(g, ox, y + t, t, half - t / 2, color);
            if ((m & 0b0010000) != 0) Ui.rect(g, ox + w - t, y + t, t, half - t / 2, color);
            if ((m & 0b0001000) != 0) Ui.rect(g, ox + t, y + half, w - 2 * t, t, color);
            if ((m & 0b0000100) != 0) Ui.rect(g, ox, y + half + t, t, half - t / 2, color);
            if ((m & 0b0000010) != 0) Ui.rect(g, ox + w - t, y + half + t, t, half - t / 2, color);
            if ((m & 0b0000001) != 0) Ui.rect(g, ox + t, y + h - t, w - 2 * t, t, color);
        }
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
