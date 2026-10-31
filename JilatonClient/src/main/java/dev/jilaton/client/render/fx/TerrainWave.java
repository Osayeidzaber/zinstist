package dev.jilaton.client.render.fx;

/**
 * Geometry and budget rules for the world-scale terrain shockwave. Radius grows fast then eases out, and
 * the number of angular samples is derived from radius but hard-capped so a 50-block wave never scans the
 * world unbounded. Behind-camera samples are culled before any block lookup happens.
 */
public final class TerrainWave {
    public static final int MAX_SAMPLES = 160;
    /** Frame-wide angular-sample budget shared by every live wave; a full wave leaves 80 for the next one. */
    public static final int MAX_SAMPLES_PER_FRAME = 240;
    public static final double MAX_LIFE_SECONDS = 1.6;
    /** Hard cap on glowing block tops drawn per frame across every live wave. */
    public static final int MAX_BLOCKS_PER_FRAME = 1400;
    /** Length of the sharp falloff ahead of the front, in blocks. */
    public static final double LEADING_EDGE = 1.2;
    /** Cells this close to the impact are always sampled, whatever the view direction. */
    public static final double CORE_RADIUS = 1.0;
    /** Angular cull margin in blocks: cells slightly behind the camera plane still count as visible. */
    public static final double FRONT_MARGIN = 0.35;

    private TerrainWave() { }

    /**
     * Seconds a hit must stay alive so every enabled style can finish. The terrain wave expands for its full
     * {@link #MAX_LIFE_SECONDS} regardless of the configured particle lifetime, so it sets the floor.
     */
    public static double impactLife(double configuredLifeSeconds) {
        return Math.max(Math.max(0, configuredLifeSeconds) * 1.2, MAX_LIFE_SECONDS);
    }

    /** Per-wave share of the frame-wide block budget, so one wave cannot starve every wave behind it. */
    public static int blockBudget(int liveWaves) {
        return Math.max(1, MAX_BLOCKS_PER_FRAME / Math.max(1, liveWaves));
    }

    /** Whether a cell at {@code distance} from the impact lies inside the visible wave band. */
    public static boolean selectsCell(double ox, double oz, double distance, double radius, double thickness,
                                      double forwardX, double forwardZ) {
        if (distance > radius + LEADING_EDGE) return false;
        if (distance < Math.max(0, radius - Math.max(0.5, thickness))) return false;
        if (distance <= CORE_RADIUS) return true;
        return inFront(ox / distance, oz / distance, forwardX, forwardZ, FRONT_MARGIN);
    }

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

    /**
     * Samples one wave may use after {@code alreadySampled} this frame: the per-wave cap, further limited by
     * what the frame-wide angular budget has left. Returns 0 once the frame budget is exhausted.
     */
    public static int samplesThisFrame(double radius, int alreadySampled) {
        return Math.max(0, Math.min(samples(radius), MAX_SAMPLES_PER_FRAME - Math.max(0, alreadySampled)));
    }

    /** Whether a radial direction is in front of the camera (or only slightly behind). */
    public static boolean inFront(double dirX, double dirZ, double forwardX, double forwardZ, double margin) {
        return dirX * forwardX + dirZ * forwardZ >= -margin;
    }

    /** Four top-face corners of the block containing the sampled point, as x,y,z triplets. */
    public static double[] blockTop(double worldX, double worldZ, double surfaceY) {
        double[] out = new double[12];
        blockTop(worldX, worldZ, surfaceY, out);
        return out;
    }

    /**
     * Writes the four top-face corners into {@code out} (12 entries) instead of allocating. A wave writes up to
     * {@link #MAX_BLOCKS_PER_FRAME} blocks per frame, so callers should reuse one buffer for the whole wave.
     */
    public static void blockTop(double worldX, double worldZ, double surfaceY, double[] out) {
        if (out == null || out.length < 12) throw new IllegalArgumentException("blockTop buffer needs 12 entries");
        double x0 = Math.floor(worldX), z0 = Math.floor(worldZ), x1 = x0 + 1, z1 = z0 + 1;
        double y = surfaceY + 0.03;
        out[0] = x0; out[1] = y; out[2] = z0;
        out[3] = x1; out[4] = y; out[5] = z0;
        out[6] = x1; out[7] = y; out[8] = z1;
        out[9] = x0; out[10] = y; out[11] = z1;
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
