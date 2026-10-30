package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;

/** Movement-vector rotation and blend used by the Strafe module. */
public final class StrafeMath {
    private StrafeMath() {
    }

    public static Vec3 apply(Vec3 movement, float yawDegrees, double forward, double sideways,
                             double strength, boolean strictMovement) {
        double inputLength = Math.hypot(forward, sideways);
        if (!Double.isFinite(inputLength) || inputLength < 1.0E-6) {
            return strictMovement ? new Vec3(0.0, movement.y, 0.0) : movement;
        }
        if (!Float.isFinite(yawDegrees) || !Double.isFinite(movement.x)
                || !Double.isFinite(movement.z) || !Double.isFinite(strength)) {
            return movement;
        }

        double horizontalSpeed = Math.hypot(movement.x, movement.z);
        if (horizontalSpeed < 1.0E-8) {
            return movement;
        }

        double boundedStrength = Math.max(0.0, Math.min(1.0, strength));
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double scale = horizontalSpeed / inputLength;
        double targetX = (sideways * cos - forward * sin) * scale;
        double targetZ = (forward * cos + sideways * sin) * scale;
        double retained = 1.0 - boundedStrength;
        return new Vec3(movement.x * retained + targetX * boundedStrength,
                movement.y,
                movement.z * retained + targetZ * boundedStrength);
    }
}
