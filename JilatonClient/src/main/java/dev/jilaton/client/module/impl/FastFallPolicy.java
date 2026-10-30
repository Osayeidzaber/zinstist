package dev.jilaton.client.module.impl;

/** Pure vertical-movement clamp for the opt-in FastFall module. */
final class FastFallPolicy {
    private FastFallPolicy() {
    }

    static double accelerateDownwardMovement(double movementY, double targetDownwardSpeed, boolean enabled) {
        if (!enabled) {
            return movementY;
        }
        if (!Double.isFinite(movementY) || !Double.isFinite(targetDownwardSpeed) || targetDownwardSpeed <= 0.0) {
            throw new IllegalArgumentException("FastFall movement and speed must be finite and speed positive");
        }
        return movementY < 0.0 ? Math.min(movementY, -targetDownwardSpeed) : movementY;
    }
}
