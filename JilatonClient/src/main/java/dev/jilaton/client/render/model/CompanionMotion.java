package dev.jilaton.client.render.model;

import java.util.List;

/**
 * Companion orbit, spring follow and banking, kept free of world state so the headless tests can pin
 * the integration and the banking limits. The orbit target is fed through a damped spring, so the body
 * lags behind the player instead of being welded to them.
 */
public final class CompanionMotion {
    public static final List<String> SHAPES = List.of("Wisp", "Sentinel", "Comet");
    private static final double MAX_STEP = 0.1;

    private CompanionMotion() { }

    public static double advanceOrbit(double previous, double dt, double speed, boolean reducedMotion) {
        if (reducedMotion) return previous;
        return previous + Math.max(0, dt) * Math.max(0, speed) * Math.PI * 2 * 0.22;
    }

    /**
     * One damped-spring step (semi-implicit Euler).
     *
     * @return {@code {x, y, z, vx, vy, vz}}
     */
    public static double[] springStep(double px, double py, double pz, double vx, double vy, double vz,
                                      double tx, double ty, double tz, double stiffness, double damping, double dt) {
        double k = Math.max(0.5, Math.min(40, stiffness));
        double c = Math.max(0, damping);
        double step = Math.max(0, Math.min(MAX_STEP, dt));
        double nvx = vx + ((tx - px) * k - vx * c) * step;
        double nvy = vy + ((ty - py) * k - vy * c) * step;
        double nvz = vz + ((tz - pz) * k - vz * c) * step;
        return new double[]{px + nvx * step, py + nvy * step, pz + nvz * step, nvx, nvy, nvz};
    }

    /** Damping coefficient for a target stiffness; ratio 1 is critical damping. */
    public static double dampingFor(double stiffness, double ratio) {
        return 2 * Math.sqrt(Math.max(0.5, Math.min(40, stiffness))) * Math.max(0.2, Math.min(1.4, ratio));
    }

    /** Orbit target around a player position. */
    public static double[] orbitTarget(double px, double py, double pz, double angle, double radius, double height) {
        return new double[]{px + Math.cos(angle) * radius, py + height, pz + Math.sin(angle) * radius};
    }

    /**
     * Banking roll: positive when the companion slides toward its own right. Bounded by {@code maxDegrees}
     * and zero when it is barely moving, so a parked companion never sits at a fixed angle.
     */
    public static double bankDegrees(double lateralSpeed, double speed, double maxDegrees) {
        if (speed < 1e-4) return 0;
        double ratio = Math.max(-1, Math.min(1, lateralSpeed / speed));
        return Math.max(0, maxDegrees) * ratio;
    }

    public static int shards(String shape) {
        return switch (shape) {
            case "Sentinel" -> 3;
            case "Comet" -> 2;
            default -> 2;
        };
    }

    public static boolean hasRing(String shape) {
        return !shape.equals("Wisp");
    }

    public static boolean hasTail(String shape) {
        return shape.equals("Comet");
    }

    /** Radius of the shard orbit around the core, in core radii. */
    public static double shardOrbit(String shape) {
        return shape.equals("Sentinel") ? 1.55 : 1.25;
    }
}
