package dev.jilaton.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The shared black canvas behind every Jilaton menu: low-contrast wavy bands at the edges, outlined star
 * watermarks scattered through them, slow light arcs and drifting monochrome dust. Everything is a
 * function of the clock, so passing a frozen clock yields a completely static frame.
 */
public final class JilatonBackdrop {
    private static final int DUST = 48;
    private static final int ARCS = 7;
    private static final int BANDS = 6;
    private static final int STARS = 9;
    private static final long EPOCH = System.nanoTime();

    private JilatonBackdrop() { }

    /** Seconds on a clock shared by all screens, so the arcs continue smoothly across navigation. */
    public static double clock(boolean reduced) {
        return reduced ? 0 : (System.nanoTime() - EPOCH) / 1_000_000_000.0;
    }

    public static void render(GuiGraphics graphics, int width, int height, double time, double px, double py, double fade) {
        render(graphics, width, height, time, px, py, fade, 1.0);
    }

    /** As above, with an extra opacity multiplier so the backdrop can crossfade. */
    public static void render(GuiGraphics graphics, int width, int height, double time, double px, double py, double fade, double opacity) {
        if (opacity <= 0) return;
        double intensity = fade * Math.min(1, opacity);
        graphics.fill(0, 0, width, height, JilatonUi.alpha(JilatonUi.INK, opacity));
        renderBands(graphics, width, height, time, px, intensity);
        renderStars(graphics, width, height, time, intensity);
        // Concentric light arcs sweep in from the lower right; each has a travelling bright head.
        double centerX = width * 1.08 + px * 40;
        double centerY = height * 1.18 + py * 30;
        for (int arc = 0; arc < ARCS; arc++) {
            double radius = height * (0.62 + arc * 0.14) + width * 0.08;
            double span = Math.PI * 0.62;
            double start = Math.PI + 0.06 * arc;
            double head = ((time * (0.035 + arc * 0.006) + arc * 0.173) % 1.0) * 1.35 - 0.2;
            double tail = 0.28 + 0.03 * (arc % 3);
            int steps = (int) Math.max(24, radius * span * tail / 2.2);
            for (int i = 0; i <= steps; i++) {
                double local = i / (double) steps;
                double u = head - tail * local;
                if (u < 0 || u > 1) continue;
                double angle = start + span * u;
                int x = (int) Math.round(centerX + Math.cos(angle) * radius);
                int y = (int) Math.round(centerY + Math.sin(angle) * radius);
                if (x < -2 || y < -2 || x > width + 2 || y > height + 2) continue;
                double strength = Math.pow(1 - local, 2.2) * (arc % 2 == 0 ? 1.0 : 0.6) * intensity;
                int alpha = (int) Math.round(150 * strength);
                if (alpha < 3) continue;
                int size = local < 0.04 ? 2 : 1;
                graphics.fill(x, y, x + size, y + size, alpha << 24 | 0xF2F2F2);
            }
        }
        // Monochrome dust drifts upward and twinkles.
        for (int i = 0; i < DUST; i++) {
            double seedX = hash(i * 2 + 1);
            double seedY = hash(i * 2 + 7);
            double speed = 4 + hash(i + 31) * 10;
            int x = (int) Math.round(seedX * width + px * 16 * (0.5 + seedY));
            int y = (int) Math.round(((seedY * height - time * speed) % height + height) % height);
            double twinkle = 0.45 + 0.55 * Math.sin(time * (0.6 + seedX) + i);
            int alpha = (int) Math.round((10 + 38 * hash(i + 91)) * Math.max(0, twinkle) * intensity);
            if (alpha > 2) graphics.fill(x, y, x + 1, y + 1, alpha << 24 | 0xFFFFFF);
        }
        graphics.fillGradient(0, 0, width, height / 4, JilatonUi.alpha(0x99000000, opacity), 0x00000000);
        graphics.fillGradient(0, height * 3 / 4, width, height, 0x00000000, JilatonUi.alpha(0xB0000000, opacity));
    }

    /** Vertical smoke bands hugging the left and right edges, like slow interference patterns. */
    private static void renderBands(GuiGraphics graphics, int width, int height, double time, double px, double intensity) {
        if (intensity <= 0) return;
        for (int band = 0; band < BANDS; band++) {
            int depth = band >> 1;
            int baseAlpha = (int) Math.round(9 + 7 * (1 - depth / 2.0));
            for (int y = 0; y < height; y += 3) {
                double ends = Math.min(1, Math.min(y, height - y) / (height * 0.18));
                int alpha = (int) Math.round(baseAlpha * ends * intensity);
                if (alpha <= 2) continue;
                int x = (int) Math.round(bandX(band, width, time, px, y));
                graphics.fill(x, y, x + 2, y + 3, alpha << 24 | 0xE4E4E4);
            }
        }
    }

    /**
     * Horizontal centre of a band at vertical position {@code y}. Split out as a pure function so the
     * motion (and its freeze under reduced motion) can be pinned by headless tests.
     */
    static double bandX(int band, int width, double time, double px, int y) {
        int depth = band >> 1;
        boolean left = (band & 1) == 0;
        double base = left ? width * (0.03 + 0.07 * depth) : width * (0.97 - 0.07 * depth);
        double amplitude = width * (0.012 + 0.008 * depth) + px * 14;
        double phase = band * 1.3;
        return base + amplitude * (0.62 * Math.sin(y * 0.0085 + time * 0.18 + phase)
                + 0.38 * Math.sin(y * 0.021 - time * 0.11 + phase * 1.9));
    }

    /** Sparse outlined stars, kept to the side margins so the centre column stays clean. */
    private static void renderStars(GuiGraphics graphics, int width, int height, double time, double intensity) {
        if (intensity <= 0) return;
        for (int i = 0; i < STARS; i++) {
            int x = (int) Math.round(starX(i, width));
            int y = (int) Math.round(starY(i, height, time));
            int radius = starRadius(i);
            double twinkle = 0.65 + 0.35 * Math.sin(time * 0.6 + i * 1.7);
            int alpha = (int) Math.round((10 + 18 * hash(i * 13 + 29)) * twinkle * intensity);
            if (alpha > 2) star(graphics, x, y, radius, alpha << 24 | 0xF2F2F2);
        }
    }

    static double starX(int index, int width) {
        boolean left = (index & 1) == 0;
        double depth = hash(index * 5 + 3);
        return left ? width * (0.04 + 0.17 * depth) : width * (0.96 - 0.17 * depth);
    }

    static double starY(int index, int height, double time) {
        double seed = hash(index * 7 + 11);
        double drift = time * (1.5 + 2 * hash(index + 53));
        return ((seed * height - drift) % height + height) % height;
    }

    static int starRadius(int index) {
        return 3 + (int) Math.round(hash(index * 13 + 5) * 3);
    }

    /** Five-pointed outline star: generic geometry, not the reference client's six-pointed mark. */
    private static void star(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int edge = 0; edge < 10; edge++) {
            double a0 = -Math.PI / 2 + edge * Math.PI / 5;
            double a1 = -Math.PI / 2 + (edge + 1) * Math.PI / 5;
            double r0 = (edge & 1) == 0 ? radius : radius * 0.42;
            double r1 = (edge & 1) == 0 ? radius * 0.42 : radius;
            line(graphics, (int) Math.round(cx + Math.cos(a0) * r0), (int) Math.round(cy + Math.sin(a0) * r0),
                    (int) Math.round(cx + Math.cos(a1) * r1), (int) Math.round(cy + Math.sin(a1) * r1), color);
        }
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int doubled = 2 * error;
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

    static double hash(int seed) {
        long value = seed * 0x9E3779B97F4A7C15L;
        value ^= value >>> 31;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 29;
        return (value >>> 11) / (double) (1L << 53);
    }
}
