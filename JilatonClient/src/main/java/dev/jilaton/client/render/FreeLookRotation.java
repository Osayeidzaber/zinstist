package dev.jilaton.client.render;

/** Camera-only mouse accumulation, in the same units as Entity.turn. */
public final class FreeLookRotation {
    private float yaw;
    private float pitch;

    public void reset(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }

    public void turn(double x, double y, double sensitivity, boolean limitPitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(sensitivity)) return;
        yaw = wrap(yaw + x * 0.15 * sensitivity);
        double nextPitch = pitch + y * 0.15 * sensitivity;
        pitch = limitPitch ? (float) Math.max(-90, Math.min(90, nextPitch)) : wrap(nextPitch);
    }

    /**
     * Moves at most {@code maxDegrees} along the shortest arc toward the player's rotation.
     * Used when FreeLook releases so the camera rejoins the body instead of snapping.
     *
     * @return the remaining angular distance in degrees; zero once the target is reached
     */
    public float stepToward(float targetYaw, float targetPitch, float maxDegrees) {
        if (!Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)) {
            return Float.POSITIVE_INFINITY;
        }
        float remaining = angularError(targetYaw, targetPitch);
        if (!Float.isFinite(maxDegrees) || maxDegrees <= 0.0F) {
            return remaining;
        }
        if (remaining <= maxDegrees) {
            yaw = wrap(targetYaw);
            pitch = clampPitch(targetPitch);
            return 0.0F;
        }
        double factor = maxDegrees / remaining;
        yaw = wrap(yaw + wrap(targetYaw - yaw) * factor);
        pitch = clampPitch(pitch + (targetPitch - pitch) * factor);
        return remaining - maxDegrees;
    }

    public float angularError(float targetYaw, float targetPitch) {
        if (!Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)) {
            return Float.POSITIVE_INFINITY;
        }
        return (float) Math.hypot(wrap(targetYaw - yaw), targetPitch - pitch);
    }

    private static float clampPitch(double degrees) {
        return (float) Math.max(-90, Math.min(90, degrees));
    }

    public static float wrap(float degrees) {
        if (!Float.isFinite(degrees)) throw new IllegalArgumentException("Angle must be finite");
        float wrapped = degrees % 360;
        return wrapped >= 180 ? wrapped - 360 : wrapped < -180 ? wrapped + 360 : wrapped;
    }

    private static float wrap(double degrees) {
        return wrap((float) degrees);
    }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
