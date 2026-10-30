package dev.jilaton.client.module.impl;

/** Calculates a horizontal knockback direction from current movement input. */
public final class VelocityStrafeMath {
    private VelocityStrafeMath() {
    }

    public static Horizontal reorient(double currentX, double currentZ, float yawDegrees,
                                      boolean forward, boolean backward, boolean left, boolean right,
                                      double speed) {
        if (!Double.isFinite(speed) || speed <= 0.0) {
            return new Horizontal(0.0, 0.0);
        }

        double forwardInput = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
        double sideInput = (left ? 1.0 : 0.0) - (right ? 1.0 : 0.0);
        double inputLength = Math.hypot(forwardInput, sideInput);
        if (inputLength < 1.0E-9) {
            double currentLength = Math.hypot(currentX, currentZ);
            if (currentLength < 1.0E-9 || !Double.isFinite(currentLength)) {
                return new Horizontal(0.0, 0.0);
            }
            return new Horizontal(currentX / currentLength * speed, currentZ / currentLength * speed);
        }

        forwardInput /= inputLength;
        sideInput /= inputLength;
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = (sideInput * cos - forwardInput * sin) * speed;
        double z = (forwardInput * cos + sideInput * sin) * speed;
        return new Horizontal(x, z);
    }

    public record Horizontal(double x, double z) {
    }
}
