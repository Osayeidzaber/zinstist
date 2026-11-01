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
        softEllipse(f, x, y, z, radius, radius, rgb, alpha, walls);
    }

    /** Camera-facing soft ellipse: peak alpha at the center, feathered to zero at the rim on both axes. */
    public static void softEllipse(FxFrame f, double x, double y, double z, double halfWidth, double halfHeight,
                                   int rgb, double alpha, boolean walls) {
        if (halfWidth <= 0 || halfHeight <= 0 || alpha <= 0.004) return;
        int sides = f.quality.ordinal() >= 2 ? 10 : 7;
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        int center = argb(rgb, alpha), rim = argb(rgb, 0);
        for (int i = 0; i < sides; i++) {
            double a = i * Math.PI * 2 / sides, b = (i + 1) * Math.PI * 2 / sides;
            double ax = Math.cos(a) * halfWidth, ay = Math.sin(a) * halfHeight;
            double bx = Math.cos(b) * halfWidth, by = Math.sin(b) * halfHeight;
            f.batch.quad(cx, cy, cz,
                    cx + f.rightX * ax + f.upX * ay, cy + f.rightY * ax + f.upY * ay, cz + f.rightZ * ax + f.upZ * ay,
                    cx + f.rightX * bx + f.upX * by, cy + f.rightY * bx + f.upY * by, cz + f.rightZ * bx + f.upZ * by,
                    cx, cy, cz, center, rim, rim, center, walls);
        }
    }

    /**
     * Camera-facing soft ring: zero alpha on both the inner and the outer rim, peaking on the ring itself.
     * A glow anchored on this ring keeps its bright part outside the entity silhouette, unlike a filled
     * ellipse whose centre the entity's own body always occludes.
     */
    public static void softRing(FxFrame f, double x, double y, double z, double halfWidth, double halfHeight,
                                double band, int rgb, double alpha, boolean walls) {
        if (halfWidth <= 0 || halfHeight <= 0 || band <= 0 || alpha <= 0.004) return;
        int sides = ringSides(f.quality);
        double[] radii = bandRadii(halfWidth, halfHeight, band);
        double innerW = radii[0], innerH = radii[1], outerW = radii[4], outerH = radii[5];
        double cx = x - f.camX, cy = y - f.camY, cz = z - f.camZ;
        int rim = argb(rgb, 0), peak = argb(rgb, alpha);
        for (int i = 0; i < sides; i++) {
            double a = i * Math.PI * 2 / sides, b = (i + 1) * Math.PI * 2 / sides;
            double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
            double aix = ca * innerW, aiy = sa * innerH, bix = cb * innerW, biy = sb * innerH;
            double amx = ca * halfWidth, amy = sa * halfHeight, bmx = cb * halfWidth, bmy = sb * halfHeight;
            double aox = ca * outerW, aoy = sa * outerH, box = cb * outerW, boy = sb * outerH;
            f.batch.quad(
                    cx + f.rightX * aix + f.upX * aiy, cy + f.rightY * aix + f.upY * aiy, cz + f.rightZ * aix + f.upZ * aiy,
                    cx + f.rightX * bix + f.upX * biy, cy + f.rightY * bix + f.upY * biy, cz + f.rightZ * bix + f.upZ * biy,
                    cx + f.rightX * bmx + f.upX * bmy, cy + f.rightY * bmx + f.upY * bmy, cz + f.rightZ * bmx + f.upZ * bmy,
                    cx + f.rightX * amx + f.upX * amy, cy + f.rightY * amx + f.upY * amy, cz + f.rightZ * amx + f.upZ * amy,
                    rim, rim, peak, peak, walls);
            f.batch.quad(
                    cx + f.rightX * amx + f.upX * amy, cy + f.rightY * amx + f.upY * amy, cz + f.rightZ * amx + f.upZ * amy,
                    cx + f.rightX * bmx + f.upX * bmy, cy + f.rightY * bmx + f.upY * bmy, cz + f.rightZ * bmx + f.upZ * bmy,
                    cx + f.rightX * box + f.upX * boy, cy + f.rightY * box + f.upY * boy, cz + f.rightZ * box + f.upZ * boy,
                    cx + f.rightX * aox + f.upX * aoy, cy + f.rightY * aox + f.upY * aoy, cz + f.rightZ * aox + f.upZ * aoy,
                    peak, peak, rim, rim, walls);
        }
    }

    /** Inner, peak and outer extents of a feathered band, ordered and clamped at zero. */
    static double[] bandRadii(double halfWidth, double halfHeight, double band) {
        double core = Math.max(0, band);
        return new double[]{
                Math.max(0, halfWidth - core), Math.max(0, halfHeight - core),
                halfWidth, halfHeight,
                halfWidth + core, halfHeight + core
        };
    }

    /** Segment count for a full ring; finer than the old fan so the band's edges stay round. */
    static int ringSides(VisualQuality quality) { return Math.max(12, quality.circleSegments() / 2); }

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
            double i0x = Math.cos(a0) * inner, i0y = Math.sin(a0) * inner;
            double r1x = Math.cos(a1) * radius, r1y = Math.sin(a1) * radius;
            double i2x = Math.cos(a2) * inner, i2y = Math.sin(a2) * inner;
            f.batch.quad(cx, cy, cz,
                    cx + f.rightX * i0x + f.upX * i0y, cy + f.rightY * i0x + f.upY * i0y, cz + f.rightZ * i0x + f.upZ * i0y,
                    cx + f.rightX * r1x + f.upX * r1y, cy + f.rightY * r1x + f.upY * r1y, cz + f.rightZ * r1x + f.upZ * r1y,
                    cx + f.rightX * i2x + f.upX * i2y, cy + f.rightY * i2x + f.upY * i2y, cz + f.rightZ * i2x + f.upZ * i2y,
                    core, core, tip, core, walls);
        }
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
            double aiX = ca * inner, aiY = sa * inner, biX = cb * inner, biY = sb * inner;
            double amX = ca * radius, amY = sa * radius, bmX = cb * radius, bmY = sb * radius;
            double aoX = ca * outer, aoY = sa * outer, boX = cb * outer, boY = sb * outer;
            f.batch.quad(
                    cx + f.rightX * aiX + f.upX * aiY, cy + f.rightY * aiX + f.upY * aiY, cz + f.rightZ * aiX + f.upZ * aiY,
                    cx + f.rightX * biX + f.upX * biY, cy + f.rightY * biX + f.upY * biY, cz + f.rightZ * biX + f.upZ * biY,
                    cx + f.rightX * bmX + f.upX * bmY, cy + f.rightY * bmX + f.upY * bmY, cz + f.rightZ * bmX + f.upZ * bmY,
                    cx + f.rightX * amX + f.upX * amY, cy + f.rightY * amX + f.upY * amY, cz + f.rightZ * amX + f.upZ * amY,
                    edge, edge, mid, mid, walls);
            f.batch.quad(
                    cx + f.rightX * amX + f.upX * amY, cy + f.rightY * amX + f.upY * amY, cz + f.rightZ * amX + f.upZ * amY,
                    cx + f.rightX * bmX + f.upX * bmY, cy + f.rightY * bmX + f.upY * bmY, cz + f.rightZ * bmX + f.upZ * bmY,
                    cx + f.rightX * boX + f.upX * boY, cy + f.rightY * boX + f.upY * boY, cz + f.rightZ * boX + f.upZ * boY,
                    cx + f.rightX * aoX + f.upX * aoY, cy + f.rightY * aoX + f.upY * aoY, cz + f.rightZ * aoX + f.upZ * aoY,
                    mid, mid, edge, edge, walls);
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
        double topY = cy + height, bottomY = cy - height;
        int face = argb(rgb, alpha * 0.35), faceDark = argb(EffectBatch.mixRgb(rgb, 0, 0.45), alpha * 0.35);
        int edge = argb(rgb, alpha);
        for (int i = 0; i < 4; i++) {
            double a0 = yaw + i * Math.PI / 2, a1 = yaw + (i + 1) * Math.PI / 2;
            double ax = cx + Math.cos(a0) * radius, az = cz + Math.sin(a0) * radius;
            double bx = cx + Math.cos(a1) * radius, bz = cz + Math.sin(a1) * radius;
            f.batch.quad(ax, cy, az, bx, cy, bz, cx, topY, cz, cx, topY, cz, face, face, face, face, walls);
            f.batch.quad(ax, cy, az, bx, cy, bz, cx, bottomY, cz, cx, bottomY, cz, faceDark, faceDark, faceDark, faceDark, walls);
            f.batch.line(ax, cy, az, bx, cy, bz, edge, 1.5F, walls);
            f.batch.line(ax, cy, az, cx, topY, cz, edge, 1.5F, walls);
            f.batch.line(ax, cy, az, cx, bottomY, cz, edge, 1.5F, walls);
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
