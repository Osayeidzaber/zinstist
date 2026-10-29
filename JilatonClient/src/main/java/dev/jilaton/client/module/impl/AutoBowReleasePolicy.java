package dev.jilaton.client.module.impl;

/** Release gate for the AutoBow shoot subset integrated into BowAim. */
final class AutoBowReleasePolicy {
    private AutoBowReleasePolicy() {
    }

    static boolean shouldRelease(boolean enabled, boolean hasTarget, int drawnTicks, int requiredTicks,
                                 double aimError, double aimThreshold, long nowNanos,
                                 long previousReleaseNanos, long delayNanos) {
        boolean delayElapsed = previousReleaseNanos == 0L
                || nowNanos - previousReleaseNanos >= Math.max(0L, delayNanos);
        return enabled && hasTarget && drawnTicks >= requiredTicks
                && Double.isFinite(aimError) && aimError <= aimThreshold && delayElapsed;
    }
}
