package dev.jilaton.client.render.model;

/**
 * Code-only geometry primitives. Every quad is emitted with a flat normal derived from its own corners
 * and wound to face that normal, so a primitive can never silently render inside out.
 */
public final class Meshes {
    private Meshes() { }

    /** Axis-aligned box with one tone for the whole surface. */
    public static Mesh box(double x0, double y0, double z0, double x1, double y1, double z1, float tone) {
        Mesh.Builder builder = Mesh.builder();
        face(builder, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, tone);
        face(builder, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1, 0, 0, tone);
        face(builder, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, tone);
        face(builder, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 0, -1, 0, tone);
        face(builder, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0, 0, 1, tone);
        face(builder, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, tone);
        return builder.build();
    }

    /**
     * Tapered strip along +Z from z=0 (root) to z=1 (tip). The centre line is raised by {@code fold},
     * so the two halves of a feather carry different normals; the whole strip is duplicated with flipped
     * normals, which makes it visible from either side under software culling.
     */
    public static Mesh strip(int segments, double halfWidth, double tipWidth, double fold, double bend) {
        return strip(segments, halfWidth, tipWidth, fold, bend, 1.0);
    }

    /** As {@link #strip(int, double, double, double, double)} but with an explicit length along +Z. */
    public static Mesh strip(int segments, double halfWidth, double tipWidth, double fold, double bend, double length) {
        int n = Math.max(2, segments);
        double len = Math.max(1e-4, length);
        Mesh.Builder builder = Mesh.builder();
        for (int i = 0; i < n; i++) {
            double t0 = (double) i / n, t1 = (double) (i + 1) / n;
            float tone = (float) ((t0 + t1) * 0.5);
            double w0 = width(t0, halfWidth, tipWidth), w1 = width(t1, halfWidth, tipWidth);
            double y0 = bend * t0 * t0, y1 = bend * t1 * t1;
            double r0 = fold * (w0 / halfWidth), r1 = fold * (w1 / halfWidth);
            double l0x = -w0, l0z = t0 * len, c0y = y0 + r0, r0x = w0;
            double l1x = -w1, l1z = t1 * len, c1y = y1 + r1, r1x = w1;
            face(builder, l0x, y0, l0z, l1x, y1, l1z, 0, c1y, l1z, 0, c0y, l0z, 0, 1, 0, tone);
            face(builder, 0, c0y, l0z, 0, c1y, l1z, r1x, y1, l1z, r0x, y0, l0z, 0, 1, 0, tone);
            face(builder, l0x, y0, l0z, l1x, y1, l1z, 0, c1y, l1z, 0, c0y, l0z, 0, -1, 0, tone);
            face(builder, 0, c0y, l0z, 0, c1y, l1z, r1x, y1, l1z, r0x, y0, l0z, 0, -1, 0, tone);
        }
        return builder.build();
    }

    private static double width(double t, double halfWidth, double tipWidth) {
        return halfWidth * Math.pow(1 - t, 0.85) + tipWidth;
    }

    /** Torus in the XZ plane with its axis along +Y. */
    public static Mesh torus(double majorRadius, double minorRadius, int majorSegments, int minorSegments) {
        int major = Math.max(8, majorSegments);
        int minor = Math.max(3, minorSegments);
        Mesh.Builder builder = Mesh.builder();
        double[] ring = new double[major * minor * 3];
        double[] norms = new double[major * minor * 3];
        for (int i = 0; i < major; i++) {
            double phi = Math.PI * 2 * i / major;
            double cp = Math.cos(phi), sp = Math.sin(phi);
            for (int j = 0; j < minor; j++) {
                double theta = Math.PI * 2 * j / minor;
                double ct = Math.cos(theta), st = Math.sin(theta);
                int k = (i * minor + j) * 3;
                ring[k] = cp * (majorRadius + minorRadius * ct);
                ring[k + 1] = minorRadius * st;
                ring[k + 2] = sp * (majorRadius + minorRadius * ct);
                norms[k] = cp * ct;
                norms[k + 1] = st;
                norms[k + 2] = sp * ct;
            }
        }
        for (int i = 0; i < major; i++) {
            int i2 = (i + 1) % major;
            for (int j = 0; j < minor; j++) {
                int j2 = (j + 1) % minor;
                int a = i * minor + j, b = i2 * minor + j, c = i2 * minor + j2, d = i * minor + j2;
                int v0 = builder.vertex((float) ring[a * 3], (float) ring[a * 3 + 1], (float) ring[a * 3 + 2],
                        (float) norms[a * 3], (float) norms[a * 3 + 1], (float) norms[a * 3 + 2], 0);
                int v1 = builder.vertex((float) ring[d * 3], (float) ring[d * 3 + 1], (float) ring[d * 3 + 2],
                        (float) norms[d * 3], (float) norms[d * 3 + 1], (float) norms[d * 3 + 2], 0.75F);
                int v2 = builder.vertex((float) ring[c * 3], (float) ring[c * 3 + 1], (float) ring[c * 3 + 2],
                        (float) norms[c * 3], (float) norms[c * 3 + 1], (float) norms[c * 3 + 2], 0.5F);
                int v3 = builder.vertex((float) ring[b * 3], (float) ring[b * 3 + 1], (float) ring[b * 3 + 2],
                        (float) norms[b * 3], (float) norms[b * 3 + 1], (float) norms[b * 3 + 2], 0.25F);
                builder.quad(v0, v1, v2, v3);
            }
        }
        return builder.build();
    }

    /** Four-sided bipyramid, used as the companion's faceted core. */
    public static Mesh diamond(double radius, double top, double bottom) {
        Mesh.Builder builder = Mesh.builder();
        double[][] ring = new double[4][];
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            ring[i] = new double[]{Math.cos(a) * radius, 0, Math.sin(a) * radius};
        }
        for (int i = 0; i < 4; i++) {
            double[] a = ring[i], b = ring[(i + 1) % 4];
            face(builder, a[0], a[1], a[2], b[0], b[1], b[2], 0, top, 0, 0, top, 0, 0, 1, 0, 1);
            face(builder, a[0], a[1], a[2], b[0], b[1], b[2], 0, bottom, 0, 0, bottom, 0, 0, -1, 0, 0.15F);
        }
        return builder.build();
    }

    /**
     * Adds a quad whose winding is derived from the intended normal, so a caller can describe a face by
     * its corners and an orientation hint without tracking clockwise order. The stored normal is the
     * face's own geometric normal; a zero-length normal or degenerate face is dropped rather than
     * emitted as a sliver.
     */
    public static void face(Mesh.Builder builder, double ax, double ay, double az,
                            double bx, double by, double bz, double cx, double cy, double cz,
                            double dx, double dy, double dz, double nx, double ny, double nz, float tone) {
        double e1x = bx - ax, e1y = by - ay, e1z = bz - az;
        double e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
        double gx = e1y * e2z - e1z * e2y;
        double gy = e1z * e2x - e1x * e2z;
        double gz = e1x * e2y - e1y * e2x;
        double length = Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (length < 1e-9 || (nx == 0 && ny == 0 && nz == 0)) return;
        if (gx * nx + gy * ny + gz * nz < 0) {
            double tx = bx, ty = by, tz = bz;
            bx = dx; by = dy; bz = dz;
            dx = tx; dy = ty; dz = tz;
            gx = -gx; gy = -gy; gz = -gz;
        }
        float ux = (float) (gx / length), uy = (float) (gy / length), uz = (float) (gz / length);
        int v0 = builder.vertex((float) ax, (float) ay, (float) az, ux, uy, uz, tone);
        int v1 = builder.vertex((float) bx, (float) by, (float) bz, ux, uy, uz, tone);
        int v2 = builder.vertex((float) cx, (float) cy, (float) cz, ux, uy, uz, tone);
        int v3 = builder.vertex((float) dx, (float) dy, (float) dz, ux, uy, uz, tone);
        builder.quad(v0, v1, v2, v3);
    }
}
