package dev.jilaton.client.render.model;

/**
 * Surface recipe for submitted geometry: a two-tone gradient from root to tip plus an emissive accent
 * that is added on top instead of being dimmed by the light term. Colours are plain 24-bit RGB; alpha
 * comes from the module's opacity so one material can be reused at any fade level.
 */
public final class Material {
    public static final double AMBIENT = 0.46;

    private final int base;
    private final int tip;
    private final int accent;
    private final double emissive;

    public Material(int base, int tip, int accent, double emissive) {
        this.base = base & 0xFFFFFF;
        this.tip = tip & 0xFFFFFF;
        this.accent = accent & 0xFFFFFF;
        this.emissive = Math.max(0, Math.min(1, emissive));
    }

    public int base() { return base; }
    public int tip() { return tip; }
    public int accent() { return accent; }
    public double emissive() { return emissive; }

    /**
     * @param tone  0 at the root, 1 at the tip
     * @param light cosine of the surface normal against the key light, clamped to 0..1
     * @param alpha final alpha 0..1
     */
    public int color(double tone, double light, double alpha) {
        double t = smooth(Math.max(0, Math.min(1, tone)));
        double lit = AMBIENT + (1 - AMBIENT) * Math.max(0, Math.min(1, light));
        int gradient = mix(base, tip, t);
        int shaded = scale(gradient, lit);
        double glow = emissive * (0.35 + 0.65 * t * t);
        int rgb = glow <= 0 ? shaded : screen(shaded, scale(accent, glow));
        return alphaByte(alpha) << 24 | rgb;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    private static int alphaByte(double alpha) {
        return (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
    }

    private static int scale(int rgb, double k) {
        int r = clamp((rgb >> 16 & 0xFF) * k);
        int g = clamp((rgb >> 8 & 0xFF) * k);
        int b = clamp((rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    /** Screen blend keeps emissive accents readable on both dark feathers and bright skies. */
    private static int screen(int a, int b) {
        int r = 255 - (255 - (a >> 16 & 0xFF)) * (255 - (b >> 16 & 0xFF)) / 255;
        int g = 255 - (255 - (a >> 8 & 0xFF)) * (255 - (b >> 8 & 0xFF)) / 255;
        int bl = 255 - (255 - (a & 0xFF)) * (255 - (b & 0xFF)) / 255;
        return r << 16 | g << 8 | bl;
    }

    private static int mix(int a, int b, double t) {
        int r = clamp((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = clamp((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = clamp((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int clamp(double value) {
        return (int) Math.round(Math.max(0, Math.min(255, value)));
    }
}
