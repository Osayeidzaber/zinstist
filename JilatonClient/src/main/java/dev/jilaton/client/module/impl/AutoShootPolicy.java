package dev.jilaton.client.module.impl;

/** Pure preconditions for an automated projectile use. */
final class AutoShootPolicy {
    private static final int MAX_THROWS_PER_TICK = 2;

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

    static int throwsThisTick(int scheduledThrows) {
        return Math.max(0, Math.min(scheduledThrows, MAX_THROWS_PER_TICK));
    }

    record Conditions(boolean playerReady, boolean targetValid, boolean projectileAvailable,
                      boolean trajectoryAvailable, boolean cooldownReady, boolean aimReady) {
    }
}
