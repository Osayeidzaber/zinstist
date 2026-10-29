package dev.jilaton.client.module.impl;

/** Pure horizontal reversal transform used after the configured knockback delay. */
public final class VelocityReversalPolicy {
    private VelocityReversalPolicy() {
    }

    public static boolean shouldSchedule(boolean onlyMoving, boolean moving) {
        return !onlyMoving || moving;
    }

    public static Motion apply(double x, double y, double z, double xModifier, double zModifier) {
        return new Motion(x * -bounded(xModifier), y, z * -bounded(zModifier));
    }

    private static double bounded(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record Motion(double x, double y, double z) {
    }
}
