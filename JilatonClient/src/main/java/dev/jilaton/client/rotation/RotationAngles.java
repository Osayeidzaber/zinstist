package dev.jilaton.client.rotation;

/** A normalized yaw/pitch pair used by visible rotation requests. */
public record RotationAngles(float yaw, float pitch) {
    public RotationAngles {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Rotation angles must be finite");
        }
        yaw = RotationPolicy.wrapDegrees(yaw);
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    public static RotationAngles lookingAt(double x, double y, double z) {
        double horizontal = Math.hypot(x, z);
        if (!Double.isFinite(horizontal) || !Double.isFinite(y) || (horizontal < 1.0E-8 && Math.abs(y) < 1.0E-8)) {
            throw new IllegalArgumentException("Look direction must be finite and non-zero");
        }
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        return new RotationAngles(yaw, pitch);
    }
}
