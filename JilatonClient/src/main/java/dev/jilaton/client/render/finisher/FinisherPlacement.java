package dev.jilaton.client.render.finisher;

/** Where the finisher character stands relative to the body, as unit offsets in preference order. */
public final class FinisherPlacement {
    /** Closer than this (blocks), behind the body comes first. */
    public static final double CLOSE = 4.5;

    private FinisherPlacement() { }

    /**
     * Unit x/z pairs from the body: beside it, set a little behind it so the move stays in front of you and centred, then
     * the mirrored side, then directly behind the body. Never between you and the body.
     */
    public static double[] sides(double viewerX, double viewerZ, double bodyX, double bodyZ) {
        double dx = bodyX - viewerX, dz = bodyZ - viewerZ;
        double len = Math.hypot(dx, dz);
        if (len < 1e-4) { dx = 0; dz = 1; } else { dx /= len; dz /= len; }
        // Right of the viewer's line of sight is (-dz, dx).
        double rx = -dz, rz = dx;
        double toward = -0.2;
        double ax = rx - dx * toward, az = rz - dz * toward;
        double bx = -rx - dx * toward, bz = -rz - dz * toward;
        double al = Math.hypot(ax, az), bl = Math.hypot(bx, bz);
        // Up close a sideways spot falls out of view, so the character steps in behind the body and faces you.
        if (len < CLOSE) return new double[]{dx, dz, ax / al, az / al, bx / bl, bz / bl};
        return new double[]{ax / al, az / al, bx / bl, bz / bl, dx, dz};
    }
}
