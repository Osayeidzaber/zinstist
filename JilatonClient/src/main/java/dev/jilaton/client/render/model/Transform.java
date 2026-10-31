package dev.jilaton.client.render.model;

/**
 * Rigid frame: an orthonormal basis (right, up, forward columns) and an origin. Local +X is right,
 * +Y is up and +Z is forward. Composition is parent * child, so a body anchor can carry a wing pivot,
 * a wing pivot can carry a feather pose, and the submit path only ever sees one frame.
 */
public final class Transform {
    private final double[] m;

    private Transform(double[] m) { this.m = m; }

    public static Transform identity() {
        return new Transform(new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0});
    }

    public static Transform translation(double x, double y, double z) {
        return new Transform(new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1, x, y, z});
    }

    public static Transform fromBasis(double rx, double ry, double rz, double ux, double uy, double uz,
                                      double fx, double fy, double fz, double ox, double oy, double oz) {
        return new Transform(new double[]{rx, ry, rz, ux, uy, uz, fx, fy, fz, ox, oy, oz});
    }

    /**
     * Minecraft-shaped <em>orientation frame</em>: yaw turns about world up (0 faces +Z), positive pitch looks
     * down, roll is applied about the resulting forward axis. The columns are (right, up, forward), which is a
     * left-handed triple in world coordinates, so this is suitable for starting a frame at the world origin
     * (via {@link #translation} / {@link #identity} and {@link #compose}) but not for rotating an existing
     * frame; use {@link #localRotation} or {@link #spin} for that.
     */
    public static Transform rotation(double yawDegrees, double pitchDegrees, double rollDegrees) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees), roll = Math.toRadians(rollDegrees);
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double rx = -cy, ry = 0, rz = -sy;
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        if (roll != 0) {
            double cr = Math.cos(roll), sr = Math.sin(roll);
            double nrx = rx * cr + ux * sr, nry = ry * cr + uy * sr, nrz = rz * cr + uz * sr;
            double nux = ux * cr - rx * sr, nuy = uy * cr - ry * sr, nuz = uz * cr - rz * sr;
            rx = nrx; ry = nry; rz = nrz;
            ux = nux; uy = nuy; uz = nuz;
        }
        return new Transform(new double[]{rx, ry, rz, ux, uy, uz, fx, fy, fz, 0, 0, 0});
    }

    /**
     * A proper right-handed rotation in <em>local</em> frame coordinates (X right, Y up, Z forward): yaw about
     * local up, pitch about local right, roll about local forward. Compose it with {@link #compose} when the
     * rotation is relative to an existing frame; {@link #rotation} is a Minecraft-shaped frame constructor and
     * must not be used as a relative rotation because its columns (right, up, forward) form a left-handed
     * triple.
     */
    public static Transform localRotation(double yawDegrees, double pitchDegrees, double rollDegrees) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees), roll = Math.toRadians(rollDegrees);
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double cr = Math.cos(roll), sr = Math.sin(roll);
        // Ry(yaw) * Rx(pitch) * Rz(roll), identical to the yaw/pitch/roll order of Transform.rotation.
        double rx = cy * cr + sy * sp * sr, ry = cp * sr, rz = -sy * cr + cy * sp * sr;
        double ux = -cy * sr + sy * sp * cr, uy = cp * cr, uz = sy * sr + cy * sp * cr;
        double fx = sy * cp, fy = -sp, fz = cy * cp;
        return new Transform(new double[]{rx, ry, rz, ux, uy, uz, fx, fy, fz, 0, 0, 0});
    }

    /** Composes a proper local rotation onto this frame, keeping the frame handedness intact. */
    public Transform spin(double yawDegrees, double pitchDegrees, double rollDegrees) {
        return compose(localRotation(yawDegrees, pitchDegrees, rollDegrees));
    }

    /**
     * Negates the frame's local X axis. Composed <em>after</em> an orientation it flips the frame's handedness
     * while leaving the origin, forward and up axes untouched, so a placement can keep the winding and authored
     * normals the submit path culls and lights by even when its parent attachment frame is left-handed (a
     * Minecraft-shaped right/up/forward triple from {@link #rotation} or {@code BodyAnchor}). Geometry that is
     * symmetric in X renders identically; the flip is only about winding and lighting.
     */
    public static Transform mirrorX() {
        return fromBasis(-1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0);
    }

    /** Frame whose +Z points along {@code forward} and whose +X stays level with {@code upHint}. */
    public static Transform lookAlong(double fx, double fy, double fz, double ux, double uy, double uz) {
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl < 1e-9) return identity();
        fx /= fl; fy /= fl; fz /= fl;
        double rx = uy * fz - uz * fy, ry = uz * fx - ux * fz, rz = ux * fy - uy * fx;
        double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rl < 1e-6) {
            // Forward is parallel to the hint; pick any stable perpendicular.
            if (Math.abs(fy) < 0.9) {
                rx = fz; ry = 0; rz = -fx;
            } else {
                rx = 0; ry = -fz; rz = fy;
            }
            rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
        }
        rx /= rl; ry /= rl; rz /= rl;
        double nux = fy * rz - fz * ry, nuy = fz * rx - fx * rz, nuz = fx * ry - fy * rx;
        return new Transform(new double[]{rx, ry, rz, nux, nuy, nuz, fx, fy, fz, 0, 0, 0});
    }

    public Transform compose(Transform child) {
        double[] c = child.m;
        double[] r = new double[12];
        for (int axis = 0; axis < 3; axis++) {
            double x = c[axis * 3], y = c[axis * 3 + 1], z = c[axis * 3 + 2];
            r[axis * 3] = m[0] * x + m[3] * y + m[6] * z;
            r[axis * 3 + 1] = m[1] * x + m[4] * y + m[7] * z;
            r[axis * 3 + 2] = m[2] * x + m[5] * y + m[8] * z;
        }
        r[9] = m[9] + m[0] * c[9] + m[3] * c[10] + m[6] * c[11];
        r[10] = m[10] + m[1] * c[9] + m[4] * c[10] + m[7] * c[11];
        r[11] = m[11] + m[2] * c[9] + m[5] * c[10] + m[8] * c[11];
        return new Transform(r);
    }

    public Transform translate(double x, double y, double z) {
        return compose(translation(x, y, z));
    }

    public Transform rotate(double yawDegrees, double pitchDegrees, double rollDegrees) {
        return compose(rotation(yawDegrees, pitchDegrees, rollDegrees));
    }

    public double originX() { return m[9]; }
    public double originY() { return m[10]; }
    public double originZ() { return m[11]; }

    public double rightX() { return m[0]; }
    public double rightY() { return m[1]; }
    public double rightZ() { return m[2]; }
    public double upX() { return m[3]; }
    public double upY() { return m[4]; }
    public double upZ() { return m[5]; }
    public double forwardX() { return m[6]; }
    public double forwardY() { return m[7]; }
    public double forwardZ() { return m[8]; }

    public void point(double x, double y, double z, double[] out) {
        out[0] = m[9] + m[0] * x + m[3] * y + m[6] * z;
        out[1] = m[10] + m[1] * x + m[4] * y + m[7] * z;
        out[2] = m[11] + m[2] * x + m[5] * y + m[8] * z;
    }

    public double[] point(double x, double y, double z) {
        double[] out = new double[3];
        point(x, y, z, out);
        return out;
    }

    public void direction(double x, double y, double z, double[] out) {
        out[0] = m[0] * x + m[3] * y + m[6] * z;
        out[1] = m[1] * x + m[4] * y + m[7] * z;
        out[2] = m[2] * x + m[5] * y + m[8] * z;
    }
}
