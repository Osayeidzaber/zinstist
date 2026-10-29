package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;

public final class TargetStrafeMath {
    private TargetStrafeMath() {
    }

    /** Produces normalized local movement input that orbits a target and corrects toward the requested radius. */
    public static Vec2 movementInput(double playerX, double playerZ, double targetX, double targetZ,
                                    float playerYawDegrees, double radius, int direction, double strength) {
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 1.0E-5 || !Double.isFinite(distance)) {
            return Vec2.ZERO;
        }

        double radialX = dx / distance;
        double radialZ = dz / distance;
        double radialCorrection = clamp((distance - Math.max(0.0, radius)) * 0.75, -0.75, 0.75);
        double worldX = -radialZ * direction + radialX * radialCorrection;
        double worldZ = radialX * direction + radialZ * radialCorrection;
        double worldLength = Math.hypot(worldX, worldZ);
        if (worldLength < 1.0E-5) {
            return Vec2.ZERO;
        }

        worldX /= worldLength;
        worldZ /= worldLength;
        double yaw = Math.toRadians(playerYawDegrees);
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        double sidewaysLeft = worldX * cosYaw + worldZ * sinYaw;
        double forward = -worldX * sinYaw + worldZ * cosYaw;
        double boundedStrength = clamp(strength, 0.0, 1.0);
        return new Vec2((float) (sidewaysLeft * boundedStrength), (float) (forward * boundedStrength));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
