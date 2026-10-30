package dev.jilaton.client.render;

import java.util.List;

/** Original Jilaton target-marker shapes, emitted as camera-relative primitives. Pure math; no game state. */
public final class TargetEspGeometry {
    public static final String HALO = "Halo";
    public static final String MINIMAL = "Minimal";
    public static final String ORBIT = "Orbit";
    public static final String HELIX = "Helix";
    public static final String GROUND_AURA = "Ground Aura";
    public static final String BRACKETS = "Brackets";
    public static final String CYBER_TARGET = "Cyber Target";
    public static final String CINEMATIC = "Cinematic";
    public static final String CUSTOM = "Custom";

    public static final int COMP_HALO = 1;
    public static final int COMP_ORBIT = 1 << 1;
    public static final int COMP_HELIX = 1 << 2;
    public static final int COMP_GROUND = 1 << 3;
    public static final int COMP_CYBER = 1 << 4;
    public static final int COMP_RETICLE = 1 << 5;
    public static final int DEFAULT_CUSTOM_MASK = COMP_HALO | COMP_ORBIT | COMP_CYBER | COMP_RETICLE;

    public static final List<String> STYLES = List.of(
            HALO, MINIMAL, ORBIT, HELIX, GROUND_AURA, BRACKETS, CYBER_TARGET, CINEMATIC, CUSTOM
    );

    private TargetEspGeometry() { }

    /** Everything a style needs about one marker, already resolved for this frame. */
    public record Marker(double x, double y, double z, double height, double radius, double phase,
                         int rgb, double alpha, double flash, boolean walls) { }

    public static void emit(EffectBatch batch, String style, Marker m) {
        emit(batch, style, m, DEFAULT_CUSTOM_MASK);
    }

    public static void emit(EffectBatch batch, String style, Marker m, int customMask) {
        if (m.alpha() < 0.01) return;
        switch (style) {
            case MINIMAL -> minimal(batch, m);
            case ORBIT, "Orbital" -> orbit(batch, m);
            case HELIX -> helix(batch, m);
            case GROUND_AURA -> groundAura(batch, m);
            case BRACKETS -> brackets(batch, m);
            case CYBER_TARGET -> cyberTarget(batch, m);
            case CINEMATIC -> cinematic(batch, m);
            case CUSTOM -> custom(batch, m, customMask);
            default -> halo(batch, m);
        }
    }

    /** Clean thin ring with a subtle pulse and cardinal ticks. */
    static void minimal(EffectBatch batch, Marker m) {
        double pulse = 1.0 + 0.06 * Math.sin(m.phase() * 2.2) + 0.08 * m.flash();
        double r = m.radius() * 0.92 * pulse;
        double ground = m.y() + 0.03;
        batch.glowRing(m.x(), ground, m.z(), r, 72,
                EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.92), 1.8F, m.walls(), 0.35, m.phase() * 1.6);
        batch.ring(m.x(), ground, m.z(), r * 0.78, 48,
                EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.32), 1.1F, m.walls(), 0, 0);
        for (int i = 0; i < 4; i++) {
            double ang = m.phase() * 0.5 + i * Math.PI * 0.5;
            double c = Math.cos(ang), s = Math.sin(ang);
            batch.glowLine(
                    m.x() + c * r * 0.82, ground, m.z() + s * r * 0.82,
                    m.x() + c * r * 1.06, ground, m.z() + s * r * 1.06,
                    EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.75), 1.6F, m.walls());
        }
    }

    /** Concentric ground aura with radial ticks and rising energy pillars around the target. */
    static void groundAura(EffectBatch batch, Marker m) {
        double ground = m.y() + 0.025;
        double r = m.radius();
        for (int k = 0; k < 3; k++) {
            double rk = r * (0.62 + k * 0.22);
            batch.band(m.x(), ground + k * 0.002, m.z(), rk * 0.78, rk, 56,
                    EffectBatch.withAlpha(m.rgb(), 0),
                    EffectBatch.withAlpha(m.rgb(), m.alpha() * (0.14 - k * 0.03)), m.walls());
            batch.glowRing(m.x(), ground + k * 0.004, m.z(), rk, 72,
                    EffectBatch.withAlpha(m.rgb(), m.alpha() * (0.90 - k * 0.22)),
                    k == 1 ? 2.4F : 1.5F, m.walls(), 0.45, m.phase() * (k % 2 == 0 ? 1.8 : -1.5));
        }
        int pillars = 8;
        for (int i = 0; i < pillars; i++) {
            double ang = m.phase() * 0.8 + i * Math.PI * 2 / pillars;
            double pr = r * 0.88;
            double px = m.x() + Math.cos(ang) * pr;
            double pz = m.z() + Math.sin(ang) * pr;
            double wave = 0.25 + 0.45 * (0.5 + 0.5 * Math.sin(m.phase() * 2.6 + i * 1.3));
            double topY = m.y() + Math.min(m.height(), m.height() * wave);
            batch.glowLine(px, ground, pz, px, topY, pz,
                    EffectBatch.withAlpha(m.rgb(), m.alpha() * (0.35 + 0.35 * wave)), 1.8F, m.walls());
        }
    }

    /** Futuristic cyber target: 3D corner brackets, horizontal scanning plane, and skeletal lock nodes. */
    static void cyberTarget(EffectBatch batch, Marker m) {
        brackets(batch, m);
        double scan = 0.15 + 0.70 * (0.5 + 0.5 * Math.sin(m.phase() * 2.0));
        double scanY = m.y() + m.height() * scan;
        double half = m.radius() * 0.74;
        double yaw = m.phase() * 0.6;
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        int scanLine = EffectBatch.withAlpha(EffectBatch.mixRgb(m.rgb(), 0xFFFFFF, 0.35), m.alpha() * 0.78);
        int scanFill = EffectBatch.withAlpha(m.rgb(), m.alpha() * 0.12);
        double[] c0 = {m.x() + (-half * cos - (-half) * sin), scanY, m.z() + (-half * sin + (-half) * cos)};
        double[] c1 = {m.x() + (half * cos - (-half) * sin), scanY, m.z() + (half * sin + (-half) * cos)};
        double[] c2 = {m.x() + (half * cos - half * sin), scanY, m.z() + (half * sin + half * cos)};
        double[] c3 = {m.x() + (-half * cos - half * sin), scanY, m.z() + (-half * sin + half * cos)};
        batch.quad(c0[0], c0[1], c0[2], c1[0], c1[1], c1[2], c2[0], c2[1], c2[2], c3[0], c3[1], c3[2],
                scanFill, scanFill, scanFill, scanFill, m.walls());
        batch.glowLine(c0[0], c0[1], c0[2], c1[0], c1[1], c1[2], scanLine, 2.0F, m.walls());
        batch.glowLine(c1[0], c1[1], c1[2], c2[0], c2[1], c2[2], scanLine, 2.0F, m.walls());
        batch.glowLine(c2[0], c2[1], c2[2], c3[0], c3[1], c3[2], scanLine, 2.0F, m.walls());
        batch.glowLine(c3[0], c3[1], c3[2], c0[0], c0[1], c0[2], scanLine, 2.0F, m.walls());
        reticle(batch, m, m.alpha() * 0.85);
    }

    /** Multi-layered 3D cinematic marker combining ground aura, tilted orbits, and double-helix ribbons. */
    static void cinematic(EffectBatch batch, Marker m) {
        Marker toned = new Marker(m.x(), m.y(), m.z(), m.height(), m.radius(), m.phase(),
                m.rgb(), m.alpha() * 0.78, m.flash(), m.walls());
        groundAura(batch, toned);
        orbit(batch, m);
        helix(batch, toned);
        reticle(batch, m, m.alpha() * 0.85);
    }

    /** User-customizable combination of TargetESP layers. */
    static void custom(EffectBatch batch, Marker m, int mask) {
        int active = mask == 0 ? COMP_HALO : mask;
        if ((active & COMP_HALO) != 0) halo(batch, m);
        if ((active & COMP_ORBIT) != 0) orbit(batch, m);
        if ((active & COMP_HELIX) != 0) helix(batch, m);
        if ((active & COMP_GROUND) != 0) groundAura(batch, m);
        if ((active & COMP_CYBER) != 0) cyberTarget(batch, m);
        if ((active & COMP_RETICLE) != 0 && (active & (COMP_HALO | COMP_CYBER)) == 0) {
            reticle(batch, m, m.alpha() * 0.75);
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
