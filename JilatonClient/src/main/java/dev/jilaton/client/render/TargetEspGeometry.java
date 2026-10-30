package dev.jilaton.client.render;

/** Original Jilaton target-marker shapes, emitted as camera-relative primitives. Pure math; no game state. */
public final class TargetEspGeometry {
    public static final String HALO = "Halo";
    public static final String ORBIT = "Orbit";
    public static final String HELIX = "Helix";
    public static final String BRACKETS = "Brackets";

    private TargetEspGeometry() { }

    /** Everything a style needs about one marker, already resolved for this frame. */
    public record Marker(double x, double y, double z, double height, double radius, double phase,
                         int rgb, double alpha, double flash, boolean walls) { }

    public static void emit(EffectBatch batch, String style, Marker m) {
        if (m.alpha() < 0.01) return;
        switch (style) {
            case ORBIT -> orbit(batch, m);
            case HELIX -> helix(batch, m);
            case BRACKETS -> brackets(batch, m);
            default -> halo(batch, m);
        }
    }

    /** Ground ring with a travelling glint, feathered floor bands, floating cage rings and a reticle. */
    static void halo(EffectBatch batch, Marker m) {
        double ground = m.y() + 0.02;
        for (int k = 0; k < 3; k++) {
            double rr = m.radius() * (1.0 + k * 0.09);
            batch.band(m.x(), ground + k * 0.002, m.z(), rr * 0.70, rr, 64,
                    EffectBatch.withAlpha(m.rgb(), 0), EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.10 / (k + 1)), m.walls());
        }
        batch.glowRing(m.x(), ground, m.z(), m.radius(), 96, EffectBatch.withAlpha(m.rgb(), m.alpha()), 2.5F,
                m.walls(), 0.55, m.phase() * 2.4);
        double scan = 0.5 + 0.5 * Math.sin(m.phase() * 1.7);
        double scanY = m.y() + m.height() * scan;
        batch.glowRing(m.x(), scanY, m.z(), m.radius() * 0.9, 72,
                EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.55), 1.6F, m.walls(), 0.30, m.phase() * 1.8);
        // Two faint floating rings give the marker vertical presence without a cage.
        for (int k = 1; k <= 2; k++) {
            double a = m.alpha() * (0.10 + 0.05 * Math.sin(m.phase() * 1.3 + k));
            batch.glowRing(m.x(), m.y() + m.height() * (k / 3.0), m.z(), m.radius() * (0.94 - 0.10 * (k - 1)),
                    56, EffectBatch.withAlpha(m.rgb(), a), 1.2F, m.walls(), 0, 0);
        }
        reticle(batch, m, m.alpha() * 0.40);
    }

    /** Small 3D cross at chest height, the classic "locked on" cue. */
    private static void reticle(EffectBatch batch, Marker m, double alpha) {
        double cx = m.x(), cy = m.y() + m.height() * 0.55, cz = m.z();
        double s = Math.max(0.06, m.radius() * 0.16) * (1 + 0.25 * m.flash());
        int color = EffectBatch.withAlpha(EffectBatch.mixRgb(m.rgb(), 0xFFFFFF, 0.45), alpha);
        batch.glowLine(cx - s, cy, cz, cx + s, cy, cz, color, 2F, m.walls());
        batch.glowLine(cx, cy - s, cz, cx, cy + s, cz, color, 2F, m.walls());
        batch.glowLine(cx, cy, cz - s, cx, cy, cz + s, color, 2F, m.walls());
    }

    /** Three tilted orbits around the body; each carries a comet with a fading tail. */
    static void orbit(EffectBatch batch, Marker m) {
        double cy = m.y() + m.height() * 0.55;
        double r = m.radius() * 1.05;
        double tilt = Math.toRadians(24);
        for (int k = 0; k < 3; k++) {
            double yaw = k * Math.PI * 2 / 3 + m.phase() * 0.35;
            double head = m.phase() * 2.1 + k * Math.PI * 2 / 3;
            double[] prev = null;
            for (int i = 0; i <= 18; i++) {
                double t = head - i * 0.085;
                double[] p = orbitPoint(m.x(), cy, m.z(), r, tilt, yaw, t);
                if (prev != null) {
                    double fade = Math.pow(1 - i / 18.0, 1.8);
                    batch.glowLine(prev[0], prev[1], prev[2], p[0], p[1], p[2],
                            EffectBatch.withAlpha(m.rgb(), m.alpha() * fade), i < 4 ? 3.0F : 2.0F, m.walls());
                }
                prev = p;
            }
            double[] h = orbitPoint(m.x(), cy, m.z(), r, tilt, yaw, head);
            double s = 0.09 * (1 + m.flash());
            int bright = EffectBatch.withAlpha(EffectBatch.mixRgb(m.rgb(), 0xFFFFFF, 0.5), m.alpha());
            batch.glowLine(h[0] - s, h[1], h[2], h[0] + s, h[1], h[2], bright, 3.0F, m.walls());
            batch.glowLine(h[0], h[1] - s, h[2], h[0], h[1] + s, h[2], bright, 3.0F, m.walls());
            batch.glowLine(h[0], h[1], h[2] - s, h[0], h[1], h[2] + s, bright, 3.0F, m.walls());
        }
    }

    static double[] orbitPoint(double cx, double cy, double cz, double r, double tilt, double yaw, double t) {
        double px = Math.cos(t) * r;
        double pz = Math.sin(t) * r;
        double py = pz * Math.sin(tilt);
        pz *= Math.cos(tilt);
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        return new double[] {cx + px * cos - pz * sin, cy + py, cz + px * sin + pz * cos};
    }

    /** Two counter-phased strands climbing the body with faint rungs; ends fade out. */
    static void helix(EffectBatch batch, Marker m) {
        int steps = 48;
        double r = m.radius() * 0.9;
        double turns = 2.0;
        for (int strand = 0; strand < 2; strand++) {
            double offset = strand * Math.PI;
            for (int i = 0; i < steps; i++) {
                double t0 = i / (double) steps, t1 = (i + 1) / (double) steps;
                double a0 = m.phase() * 1.4 + offset + t0 * turns * Math.PI * 2;
                double a1 = m.phase() * 1.4 + offset + t1 * turns * Math.PI * 2;
                double fade = Math.sin(Math.PI * (t0 + t1) / 2);
                batch.glowLine(m.x() + Math.cos(a0) * r, m.y() + t0 * m.height(), m.z() + Math.sin(a0) * r,
                        m.x() + Math.cos(a1) * r, m.y() + t1 * m.height(), m.z() + Math.sin(a1) * r,
                        EffectBatch.withAlpha(m.rgb(), m.alpha() * (0.3 + 0.7 * fade)), 2.5F, m.walls());
                if (strand == 0 && i % 6 == 3) {
                    batch.line(m.x() + Math.cos(a0) * r, m.y() + t0 * m.height(), m.z() + Math.sin(a0) * r,
                            m.x() - Math.cos(a0) * r, m.y() + t0 * m.height(), m.z() - Math.sin(a0) * r,
                            EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.18 * fade), 1.0F, m.walls());
                }
            }
        }
    }

    /** Eight 3D corner brackets that turn slowly and snap inward on a hit. */
    static void brackets(EffectBatch batch, Marker m) {
        double half = m.radius() * 0.78 * (1 - 0.12 * m.flash());
        double length = Math.max(0.12, half * 0.42);
        double yaw = m.phase() * 0.6;
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        int color = EffectBatch.withAlpha(m.rgb(), m.alpha());
        double lift = Math.min(0.24, m.height() * 0.14);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) for (int top = 0; top <= 1; top++) {
            double lx = sx * half, lz = sz * half;
            double x = m.x() + lx * cos - lz * sin;
            double z = m.z() + lx * sin + lz * cos;
            double y = m.y() + m.height() * top;
            double ax = -sx * length * cos, az = -sx * length * sin;
            double bx = sz * length * sin, bz = -sz * length * cos;
            batch.glowLine(x, y, z, x + ax, y, z + az, color, 3.0F, m.walls());
            batch.glowLine(x, y, z, x + bx, y, z + bz, color, 3.0F, m.walls());
            batch.glowLine(x, y, z, x, y + (top == 0 ? lift : -lift), z, color, 3.0F, m.walls());
        }
    }
}
