package dev.jilaton.client.render.fx;

/**
 * Geometry and budget rules for the world-scale terrain shockwave. Radius grows fast then eases out, and
 * the number of angular samples is derived from radius but hard-capped so a 50-block wave never scans the
 * world unbounded. Behind-camera samples are culled before any block lookup happens.
 */
public final class TerrainWave {
    public static final int MAX_SAMPLES = 160;
    public static final double MAX_LIFE_SECONDS = 1.6;
    /** Hard cap on glowing block tops drawn per frame across every live wave. */
    public static final int MAX_BLOCKS_PER_FRAME = 1400;

    private TerrainWave() { }

    /** Fraction of the wave's life that has elapsed, clamped to [0, 1]. */
    public static double progress(double ageSeconds, double lifeSeconds) {
        if (lifeSeconds <= 0) return 1;
        return Math.max(0, Math.min(1, ageSeconds / lifeSeconds));
    }

    /** Wavefront radius: expands quickly, then settles toward {@code maxRadius}. */
    public static double radius(double progress, double maxRadius) {
        double u = Math.max(0, Math.min(1, progress));
        return maxRadius * (1 - Math.pow(1 - u, 2.4));
    }

    public static double alpha(double progress) {
        double u = Math.max(0, Math.min(1, progress));
        return Math.pow(1 - u, 1.4);
    }

    /** Angular samples: one per ~0.55 blocks of circumference at close range, capped. */
    public static int samples(double radius) {
        int wanted = (int) Math.round(Math.max(24, radius * 4.2));
        return Math.min(MAX_SAMPLES, wanted);
    }

    /** Whether a radial direction is in front of the camera (or only slightly behind). */
    public static boolean inFront(double dirX, double dirZ, double forwardX, double forwardZ, double margin) {
        return dirX * forwardX + dirZ * forwardZ >= -margin;
    }

    /** Four top-face corners of the block containing the sampled point, as x,y,z triplets. */
    public static double[] blockTop(double worldX, double worldZ, double surfaceY) {
        double x0 = Math.floor(worldX), z0 = Math.floor(worldZ), x1 = x0 + 1, z1 = z0 + 1;
        double y = surfaceY + 0.03;
        return new double[]{x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1};
    }

    /**
     * Glow of a block whose centre sits {@code distance} from the impact while the front is at {@code radius}:
     * 1 on the front, a short sharp falloff ahead of it and a longer afterglow trailing behind.
     */
    public static double frontGlow(double distance, double radius, double thickness) {
        double t = Math.max(0.5, thickness);
        double d = distance - radius;
        if (d > 0) return d > 1.2 ? 0 : Math.pow(1 - d / 1.2, 2);
        double u = -d / t;
        return u >= 1 ? 0 : Math.pow(1 - u, 1.6);
    }
}
