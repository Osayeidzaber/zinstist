package dev.jilaton.client.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera-relative world-effect primitives collected during extraction. Hard caps bound the per-frame
 * cost regardless of how many effects are enabled; anything past a cap is dropped, never queued.
 */
public final class EffectBatch {
    public static final int MAX_SEGMENTS = 16_000;
    public static final int MAX_QUADS = 24_000;
    private final List<Segment> segments = new ArrayList<>();
    private final List<Quad> quads = new ArrayList<>();

    public record Segment(double x1, double y1, double z1, double x2, double y2, double z2,
                          int color, float width, boolean walls) { }

    /** Four corners in winding order, each with its own color so fills can fade across the surface. */
    public record Quad(double ax, double ay, double az, double bx, double by, double bz,
                       double cx, double cy, double cz, double dx, double dy, double dz,
                       int colorA, int colorB, int colorC, int colorD, boolean walls) { }

    public List<Segment> segments() { return segments; }
    public List<Quad> quads() { return quads; }
    public boolean isEmpty() { return segments.isEmpty() && quads.isEmpty(); }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width, boolean walls) {
        if (color >>> 24 == 0 || segments.size() >= MAX_SEGMENTS) return;
        segments.add(new Segment(x1, y1, z1, x2, y2, z2, color, width, walls));
    }

    public void quad(double ax, double ay, double az, double bx, double by, double bz,
                     double cx, double cy, double cz, double dx, double dy, double dz,
                     int colorA, int colorB, int colorC, int colorD, boolean walls) {
        if ((colorA | colorB | colorC | colorD) >>> 24 == 0 || quads.size() >= MAX_QUADS) return;
        quads.add(new Quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, colorA, colorB, colorC, colorD, walls));
    }

    /**
     * A line that stays readable on bright sky and dark ground: a wide, faint halo under a bright core.
     * The halo costs one extra segment; it is skipped for nearly transparent lines.
     */
    public void glowLine(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width, boolean walls) {
        double alpha = (color >>> 24) / 255.0;
        if (alpha > 0.06) line(x1, y1, z1, x2, y2, z2, withAlpha(color, alpha * 0.22), width * 3.4F, walls);
        line(x1, y1, z1, x2, y2, z2, color, width, walls);
    }

    /** {@link #ring} drawn with {@link #glowLine}. */
    public void glowRing(double x, double y, double z, double radius, int segmentsCount, int color, float width,
                         boolean walls, double glint, double glintAngle) {
        if (radius <= 0) return;
        double alpha = (color >>> 24) / 255.0;
        for (int i = 0; i < segmentsCount; i++) {
            double a = i * Math.PI * 2 / segmentsCount;
            double b = (i + 1) * Math.PI * 2 / segmentsCount;
            double factor = glint <= 0 ? 1 : (1 - glint) + glint * Math.pow(Math.max(0, Math.cos((a + b) / 2 - glintAngle)), 6);
            glowLine(x + Math.cos(a) * radius, y, z + Math.sin(a) * radius, x + Math.cos(b) * radius, y, z + Math.sin(b) * radius,
                    withAlpha(color, alpha * factor), width, walls);
        }
    }

    /** Horizontal circle; {@code glint} brightens the sector around {@code glintAngle} when above zero. */
    public void ring(double x, double y, double z, double radius, int segmentsCount, int color, float width,
                     boolean walls, double glint, double glintAngle) {
        if (radius <= 0) return;
        double alpha = (color >>> 24) / 255.0;
        for (int i = 0; i < segmentsCount; i++) {
            double a = i * Math.PI * 2 / segmentsCount;
            double b = (i + 1) * Math.PI * 2 / segmentsCount;
            double factor = glint <= 0 ? 1 : (1 - glint) + glint * Math.pow(Math.max(0, Math.cos((a + b) / 2 - glintAngle)), 6);
            line(x + Math.cos(a) * radius, y, z + Math.sin(a) * radius, x + Math.cos(b) * radius, y, z + Math.sin(b) * radius,
                    withAlpha(color, alpha * factor), width, walls);
        }
    }

    /** Flat annulus fading from {@code innerColor} to {@code outerColor}. */
    public void band(double x, double y, double z, double inner, double outer, int segmentsCount,
                     int innerColor, int outerColor, boolean walls) {
        if (outer <= inner || outer <= 0) return;
        for (int i = 0; i < segmentsCount; i++) {
            double a = i * Math.PI * 2 / segmentsCount;
            double b = (i + 1) * Math.PI * 2 / segmentsCount;
            double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
            quad(x + ca * inner, y, z + sa * inner, x + cb * inner, y, z + sb * inner,
                    x + cb * outer, y, z + sb * outer, x + ca * outer, y, z + sa * outer,
                    innerColor, innerColor, outerColor, outerColor, walls);
        }
    }

    public int size() { return segments.size() + quads.size(); }

    /**
     * Lambert shade of {@code rgb} for a face with the given (unit) normal under a fixed key light from above-front,
     * so solid effect geometry reads as a lit volume instead of a flat silhouette.
     */
    public static int shade(int rgb, double nx, double ny, double nz) {
        double lambert = Math.max(0, nx * 0.32 + ny * 0.86 + nz * 0.40);
        double k = 0.42 + 0.58 * lambert;
        int r = (int) Math.round((rgb >> 16 & 0xFF) * k);
        int g = (int) Math.round((rgb >> 8 & 0xFF) * k);
        int b = (int) Math.round((rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    public static int withAlpha(int rgb, double opacity) {
        return (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255) << 24 | rgb & 0xFFFFFF;
    }

    public static int mixRgb(int from, int to, double t) {
        double k = Math.max(0, Math.min(1, t));
        int r = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * k);
        int g = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * k);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return r << 16 | g << 8 | b;
    }
}
