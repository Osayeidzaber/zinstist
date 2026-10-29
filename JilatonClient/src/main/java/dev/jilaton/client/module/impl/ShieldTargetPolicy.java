package dev.jilaton.client.module.impl;

/** Directional shield check shared by crosshair and automatic melee modules. */
public final class ShieldTargetPolicy {
    private ShieldTargetPolicy() {
    }

    public static boolean shieldFacesAttacker(double attackerX, double attackerZ,
                                               double targetX, double targetZ, float targetYawDegrees) {
        if (!Double.isFinite(attackerX) || !Double.isFinite(attackerZ)
                || !Double.isFinite(targetX) || !Double.isFinite(targetZ)
                || !Float.isFinite(targetYawDegrees)) {
            return false;
        }
        double towardX = attackerX - targetX;
        double towardZ = attackerZ - targetZ;
        double length = Math.hypot(towardX, towardZ);
        if (length < 1.0E-6) {
            return false;
        }
        double yaw = Math.toRadians(targetYawDegrees);
        double facingX = -Math.sin(yaw);
        double facingZ = Math.cos(yaw);
        return (facingX * towardX + facingZ * towardZ) / length > 0.0;
    }
}
