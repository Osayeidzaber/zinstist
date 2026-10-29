package dev.jilaton.client.module.impl;

/** Axis-selective factor smoothing used before the shared per-tick turn limiter. */
public final class AimAssistPolicy {
    private AimAssistPolicy() {
    }

    public static Angles apply(float currentYaw, float currentPitch, float targetYaw, float targetPitch,
                               double factor, boolean horizontal, boolean vertical) {
        if (!Float.isFinite(currentYaw) || !Float.isFinite(currentPitch)
                || !Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)
                || !Double.isFinite(factor)) {
            return new Angles(currentYaw, currentPitch);
        }

        double blend = Math.max(0.0, Math.min(1.0, factor));
        float yaw = horizontal
                ? (float) (currentYaw + wrapDegrees(targetYaw - currentYaw) * blend)
                : currentYaw;
        float pitch = vertical
                ? (float) (currentPitch + Math.max(-180.0, Math.min(180.0, targetPitch - currentPitch)) * blend)
                : currentPitch;
        return new Angles(yaw, Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public record Angles(float yaw, float pitch) {
    }
}
