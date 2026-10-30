package dev.jilaton.client.module.impl;

/**
 * Shield model shared by crosshair and automatic melee modules.
 *
 * <p>Blocking already excludes the axe disable window in vanilla: {@code getItemBlockingWith}
 * requires {@code isUsingItem} (LivingEntity.java:3426-3432), and {@code BlocksAttacks#disable}
 * stops using the item and adds an item cooldown (BlocksAttacks.java:68-78), so
 * {@code isBlocking()} is false for the whole window. Callers pass the live value instead of
 * tracking cooldown ticks here.
 */
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

    /**
     * True when the target's active block absorbs this attack. Vanilla reduces the hit by 90%
     * (Items.java:1564) and only then disables the block (LivingEntity.java:1436-1438,
     * Player.java:816-822), so an axe still lands a reduced first hit and is not wasted.
     */
    public static boolean shieldAbsorbsAttack(boolean blocking, boolean facesAttacker,
                                              boolean attackerDisablesBlocking) {
        return blocking && facesAttacker && !attackerDisablesBlocking;
    }
}
