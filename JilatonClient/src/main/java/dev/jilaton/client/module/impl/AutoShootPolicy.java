package dev.jilaton.client.module.impl;

/** Pure preconditions for an automated projectile use. */
final class AutoShootPolicy {
    private AutoShootPolicy() {
    }

    static boolean canShoot(Conditions conditions) {
        return conditions.playerReady()
                && conditions.targetValid()
                && conditions.projectileAvailable()
                && conditions.trajectoryAvailable()
                && conditions.cooldownReady()
                && conditions.aimReady();
    }

    record Conditions(boolean playerReady, boolean targetValid, boolean projectileAvailable,
                      boolean trajectoryAvailable, boolean cooldownReady, boolean aimReady) {
    }
}
