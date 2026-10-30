package dev.jilaton.client.render.model;

/**
 * First-person knife pose, pure of rendering state. The knife lives in camera space: +X right, +Y up,
 * +Z forward. Its held position keeps the whole model to the right of the view axis so the crosshair
 * stays clear, while the idle sway and walk bob are decorative and freeze under reduced motion.
 */
public final class KnifePose {
    /** Distance from the camera eye to the knife origin, in blocks. */
    public static final double FORWARD = 0.42;
    /** The knife's inner edge stays at least this many degrees off the view axis. */
    public static final double CLEARANCE_DEGREES = 8;
    public static final double DEFAULT_SIDE = 0.24;
    public static final double DEFAULT_HALF_WIDTH = 0.035;
    /** Held angles: the blade canters toward the centre and rolls so its flat catches the light. */
    public static final double HELD_YAW = -4;
    public static final double HELD_ROLL = -14;

    /** Pose in camera space plus the extra rotation applied to the camera frame. */
    public record Pose(double right, double up, double forward, double yawDeg, double pitchDeg, double rollDeg) { }

    private KnifePose() { }

    public static double advance(double previous, double dt, double speed, boolean reducedMotion) {
        if (reducedMotion) return previous;
        return previous + Math.max(0, dt) * Math.max(0, speed);
    }

    /** Equip is a functional transition, not decoration, so reduced motion completes it at once. */
    public static double equip(double previous, double dt, double seconds, boolean reducedMotion) {
        if (reducedMotion || seconds <= 1e-6) return 1;
        return Math.min(1, Math.max(0, previous) + Math.max(0, dt) / seconds);
    }

    public static double easeOutCubic(double t) {
        double c = Math.max(0, Math.min(1, t)) - 1;
        return c * c * c + 1;
    }

    /**
     * @param swayPhase      idle sway phase in radians
     * @param walkPhase      walk-cycle phase in radians
     * @param equipProgress  0 when the knife is stowed, 1 when fully held
     * @param side           right-hand offset in blocks
     * @param height         vertical offset in blocks
     * @param sway           idle sway amount (0 disables it)
     * @param bob            walk bob amount (0 disables it)
     */
    public static Pose pose(double swayPhase, double walkPhase, double equipProgress,
                            double side, double height, double sway, double bob) {
        double held = easeOutCubic(equipProgress);
        double stow = 1 - held;
        double idleX = Math.sin(swayPhase) * 0.006 * sway;
        double idleY = Math.sin(swayPhase * 0.7 + 1.1) * 0.004 * sway;
        double walkX = Math.sin(walkPhase) * 0.009 * bob;
        double walkY = -Math.abs(Math.cos(walkPhase)) * 0.011 * bob;
        return new Pose(
                side + idleX + walkX,
                height + idleY + walkY - stow * 0.30,
                FORWARD + stow * 0.04,
                HELD_YAW - idleY * 90,
                4 + stow * 28,
                HELD_ROLL + stow * -35 + walkX * 120);
    }

    /** Signed angle from the view axis to the knife's inner edge, in degrees. */
    public static double clearanceDegrees(double side, double halfWidth, double forward) {
        double inner = Math.max(0, Math.abs(side) - Math.abs(halfWidth));
        return Math.toDegrees(Math.atan2(inner, Math.max(1e-3, forward)));
    }

    /**
     * As above, but charged for a held blade canted toward the centre: an inward yaw of the whole model
     * eats into the margin, so it is subtracted rather than ignored. This is the conservative reading —
     * if the cant is actually outward the real clearance is larger than the number returned.
     */
    public static double clearanceDegrees(double side, double halfWidth, double forward, double inwardYawDegrees) {
        return clearanceDegrees(side, halfWidth, forward) - Math.max(0, inwardYawDegrees);
    }

    /** True when the model cannot cover the crosshair, even with the camera at its widest FOV. */
    public static boolean clearsCrosshair(double side, double halfWidth, double forward) {
        return clearanceDegrees(side, halfWidth, forward) >= CLEARANCE_DEGREES;
    }

    /** As above, for a model canted inward by {@code inwardYawDegrees}. */
    public static boolean clearsCrosshair(double side, double halfWidth, double forward, double inwardYawDegrees) {
        return clearanceDegrees(side, halfWidth, forward, inwardYawDegrees) >= CLEARANCE_DEGREES;
    }
}
