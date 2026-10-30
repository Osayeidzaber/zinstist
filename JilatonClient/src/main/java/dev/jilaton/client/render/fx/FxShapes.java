package dev.jilaton.client.render.fx;

import dev.jilaton.client.render.EffectBatch;

/**
 * Primitive geometry shared by every world effect. Input positions are absolute world coordinates; output
 * goes to the frame's batch in camera-relative space. Soft shapes are fans whose rim fades to zero alpha,
 * which reads as a glow without textures or extra shaders.
 */
public final class FxShapes {
    private FxShapes() { }

    public static int argb(int rgb, double alpha) { return EffectBatch.withAlpha(rgb, alpha); }

    /** Camera-facing soft dot: opaque-ish center, transparent rim. */
    public static void softDot(FxFrame f, double x, double y, double z, double radius, int rgb, double alpha, boolean walls) {
        if (radius <= 0 || alpha <= 0.004) return;
        int sides = f.quality.ordinal() >= 2 ? 10 : 7;
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        int center = argb(rgb, alpha), rim = argb(rgb, 0);
        for (int i = 0; i < sides; i++) {
            double a = i * Math.PI * 2 / sides, b = (i + 1) * Math.PI * 2 / sides;
            double ax = Math.cos(a) * radius, ay = Math.sin(a) * radius, bx = Math.cos(b) * radius, by = Math.sin(b) * radius;
            f.batch.quad(cx, cy, cz,
                    cx + f.rightX * ax + f.upX * ay, cy + f.rightY * ax + f.upY * ay, cz + f.rightZ * ax + f.upZ * ay,
                    cx + f.rightX * bx + f.upX * by, cy + f.rightY * bx + f.upY * by, cz + f.rightZ * bx + f.upZ * by,
                    cx, cy, cz, center, rim, rim, center, walls);
        }
    }

    /** Dot with a crisp core plus a wide faint halo, used where the user enables glow. */
    public static void glowDot(FxFrame f, double x, double y, double z, double radius, int rgb, double alpha, boolean glow, boolean walls) {
        if (glow) softDot(f, x, y, z, radius * 3.2, rgb, alpha * 0.22, walls);
        softDot(f, x, y, z, radius, rgb, alpha, walls);
    }

    /** Streak oriented along a direction and widened toward the camera; tail fades out. */
    public static void spark(FxFrame f, double x, double y, double z, double dx, double dy, double dz,
                             double length, double width, int rgb, double alpha, boolean walls) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6 || alpha <= 0.004) return;
        dx /= len; dy /= len; dz /= len;
        double hx = x - f.camX, hy = y - f.camY, hz = z - f.camZ;
        double tx = hx - dx * length, ty = hy - dy * length, tz = hz - dz * length;
        // side = dir x toCamera
        double vx = -hx, vy = -hy, vz = -hz;
        double sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-6) { sx = f.rightX; sy = f.rightY; sz = f.rightZ; sl = 1; }
        sx = sx / sl * width; sy = sy / sl * width; sz = sz / sl * width;
        int head = argb(rgb, alpha), tail = argb(rgb, 0);
        f.batch.quad(tx, ty, tz, hx + sx, hy + sy, hz + sz, hx, hy, hz, hx - sx, hy - sy, hz - sz, tail, head, head, head, walls);
    }

    /** Camera-facing star polygon. */
    public static void star(FxFrame f, double x, double y, double z, double radius, double rotation, int points,
                            int rgb, double alpha, boolean walls) {
        if (radius <= 0 || alpha <= 0.004) return;
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        int core = argb(rgb, alpha), tip = argb(rgb, alpha * 0.55);
        double inner = radius * 0.38;
        for (int i = 0; i < points; i++) {
            double a0 = rotation + (i - 0.5) * Math.PI * 2 / points;
            double a1 = rotation + i * Math.PI * 2 / points;
            double a2 = rotation + (i + 0.5) * Math.PI * 2 / points;
            double[] p0 = basis(f, Math.cos(a0) * inner, Math.sin(a0) * inner);
            double[] p1 = basis(f, Math.cos(a1) * radius, Math.sin(a1) * radius);
            double[] p2 = basis(f, Math.cos(a2) * inner, Math.sin(a2) * inner);
            f.batch.quad(cx, cy, cz, cx + p0[0], cy + p0[1], cz + p0[2], cx + p1[0], cy + p1[1], cz + p1[2],
                    cx + p2[0], cy + p2[1], cz + p2[2], core, core, tip, core, walls);
        }
    }

    private static double[] basis(FxFrame f, double a, double b) {
        return new double[]{f.rightX * a + f.upX * b, f.rightY * a + f.upY * b, f.rightZ * a + f.upZ * b};
    }

    /** Camera-facing ring band with a feathered outer edge. */
    public static void billboardRing(FxFrame f, double x, double y, double z, double radius, double thickness,
                                     int rgb, double alpha, boolean walls) {
        if (radius <= 0 || alpha <= 0.004) return;
        int segments = f.quality.circleSegments();
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        double inner = Math.max(0, radius - thickness), outer = radius + thickness;
        int mid = argb(rgb, alpha), edge = argb(rgb, 0);
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
            double[] ai = basis(f, ca * inner, sa * inner), bi = basis(f, cb * inner, sb * inner);
            double[] am = basis(f, ca * radius, sa * radius), bm = basis(f, cb * radius, sb * radius);
            double[] ao = basis(f, ca * outer, sa * outer), bo = basis(f, cb * outer, sb * outer);
            f.batch.quad(cx + ai[0], cy + ai[1], cz + ai[2], cx + bi[0], cy + bi[1], cz + bi[2],
                    cx + bm[0], cy + bm[1], cz + bm[2], cx + am[0], cy + am[1], cz + am[2], edge, edge, mid, mid, walls);
            f.batch.quad(cx + am[0], cy + am[1], cz + am[2], cx + bm[0], cy + bm[1], cz + bm[2],
                    cx + bo[0], cy + bo[1], cz + bo[2], cx + ao[0], cy + ao[1], cz + ao[2], mid, mid, edge, edge, walls);
        }
    }

    /**
     * Ring around an arbitrary axis. {@code arc} below 1 draws a partial ring starting at {@code phase},
     * with its leading edge brightest so rotation reads clearly.
     */
    public static void tiltedRing(FxFrame f, double x, double y, double z, double nx, double ny, double nz,
                                  double radius, double phase, double arc, int rgb, double alpha, float width, boolean walls) {
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-6 || radius <= 0 || alpha <= 0.004) return;
        nx /= nl; ny /= nl; nz /= nl;
        // Orthonormal basis (u, v) perpendicular to n.
        double ux, uy, uz;
        if (Math.abs(ny) < 0.9) { ux = nz; uy = 0; uz = -nx; } else { ux = 0; uy = -nz; uz = ny; }
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        int segments = Math.max(6, (int) Math.ceil(f.quality.circleSegments() * Math.min(1, arc)));
        double span = Math.PI * 2 * Math.min(1, arc);
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments, t1 = (double) (i + 1) / segments;
            double a = phase + t0 * span, b = phase + t1 * span;
            double fade = arc >= 1 ? 1 : Math.pow(t1, 1.4);
            double ax = (Math.cos(a) * ux + Math.sin(a) * vx) * radius, ay = (Math.cos(a) * uy + Math.sin(a) * vy) * radius,
                    az = (Math.cos(a) * uz + Math.sin(a) * vz) * radius;
            double bx = (Math.cos(b) * ux + Math.sin(b) * vx) * radius, by = (Math.cos(b) * uy + Math.sin(b) * vy) * radius,
                    bz = (Math.cos(b) * uz + Math.sin(b) * vz) * radius;
            f.batch.line(cx + ax, cy + ay, cz + az, cx + bx, cy + by, cz + bz, argb(rgb, alpha * fade), width, walls);
        }
    }

    /** Small faceted octahedron with translucent faces and crisp edges, rotated around Y. */
    public static void crystal(FxFrame f, double x, double y, double z, double radius, double height, double yaw,
                               int rgb, double alpha, boolean walls) {
        if (alpha <= 0.004) return;
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        double[][] ring = new double[4][];
        for (int i = 0; i < 4; i++) {
            double a = yaw + i * Math.PI / 2;
            ring[i] = new double[]{cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius};
        }
        double topY = cy + height, bottomY = cy - height;
        int face = argb(rgb, alpha * 0.35), faceDark = argb(EffectBatch.mixRgb(rgb, 0, 0.45), alpha * 0.35);
        int edge = argb(rgb, alpha);
        for (int i = 0; i < 4; i++) {
            double[] a = ring[i], b = ring[(i + 1) % 4];
            f.batch.quad(a[0], a[1], a[2], b[0], b[1], b[2], cx, topY, cz, cx, topY, cz, face, face, face, face, walls);
            f.batch.quad(a[0], a[1], a[2], b[0], b[1], b[2], cx, bottomY, cz, cx, bottomY, cz, faceDark, faceDark, faceDark, faceDark, walls);
            f.batch.line(a[0], a[1], a[2], b[0], b[1], b[2], edge, 1.5F, walls);
            f.batch.line(a[0], a[1], a[2], cx, topY, cz, edge, 1.5F, walls);
            f.batch.line(a[0], a[1], a[2], cx, bottomY, cz, edge, 1.5F, walls);
        }
    }

    /**
     * Flat irregular splash on a horizontal surface. The outline is a deterministic function of {@code seed}
     * so a splat keeps its shape for its whole life. {@code spread} animates 0 → 1 as the ink lands.
     */
    public static void splat(FxFrame f, double x, double y, double z, double radius, long seed, double spread,
                             int rgb, double alpha, boolean walls) {
        if (radius <= 0 || alpha <= 0.004) return;
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        int sides = f.quality.ordinal() >= 2 ? 28 : 18;
        double r = radius * (0.35 + 0.65 * spread);
        int center = argb(EffectBatch.mixRgb(rgb, 0xFFFFFF, 0.35), alpha), body = argb(rgb, alpha * 0.92), rim = argb(rgb, 0);
        double prevX = 0, prevZ = 0, prevFx = 0, prevFz = 0;
        for (int i = 0; i <= sides; i++) {
            double a = i * Math.PI * 2 / sides;
            double n = shape(seed, a);
            double ex = Math.cos(a) * r * n, ez = Math.sin(a) * r * n;
            double fx = ex * 1.18, fz = ez * 1.18;
            if (i > 0) {
                f.batch.quad(cx, cy, cz, cx + prevX, cy, cz + prevZ, cx + ex, cy, cz + ez, cx, cy, cz, center, body, body, center, walls);
                f.batch.quad(cx + prevX, cy, cz + prevZ, cx + ex, cy, cz + ez, cx + fx, cy, cz + fz, cx + prevFx, cy, cz + prevFz,
                        body, body, rim, rim, walls);
            }
            prevX = ex; prevZ = ez; prevFx = fx; prevFz = fz;
        }
        // Satellite droplets thrown past the rim.
        int droplets = 4 + (int) (Math.abs(seed) % 5);
        for (int i = 0; i < droplets; i++) {
            double a = hash(seed, i) * Math.PI * 2;
            double d = r * (1.25 + hash(seed, i + 31) * 0.8);
            double s = radius * (0.06 + hash(seed, i + 63) * 0.1) * spread;
            flatDisc(f, cx + Math.cos(a) * d, cy, cz + Math.sin(a) * d, s, body, rim, walls);
        }
    }

    private static void flatDisc(FxFrame f, double rx, double ry, double rz, double radius, int core, int rim, boolean walls) {
        if (radius <= 0.001) return;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, b = (i + 1) * Math.PI / 4;
            f.batch.quad(rx, ry, rz, rx + Math.cos(a) * radius, ry, rz + Math.sin(a) * radius,
                    rx + Math.cos(b) * radius, ry, rz + Math.sin(b) * radius, rx, ry, rz, core, core, core, core, walls);
        }
    }

    /** Radius multiplier: a soft blob with a few long ink arms. */
    static double shape(long seed, double angle) {
        double n = 0.78 + 0.12 * Math.sin(angle * 3 + hash(seed, 1) * 6) + 0.08 * Math.sin(angle * 7 + hash(seed, 2) * 6);
        for (int arm = 0; arm < 4; arm++) {
            double center = hash(seed, 10 + arm) * Math.PI * 2;
            double d = Math.abs(Math.atan2(Math.sin(angle - center), Math.cos(angle - center)));
            n += 0.75 * Math.exp(-d * d * 60) * (0.5 + hash(seed, 20 + arm));
        }
        return n;
    }

    /** Deterministic 0..1 hash. */
    public static double hash(long seed, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 29;
        return (h >>> 11) / (double) (1L << 53);
    }
}
