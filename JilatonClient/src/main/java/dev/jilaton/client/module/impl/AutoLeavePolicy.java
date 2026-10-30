package dev.jilaton.client.module.impl;

/** Health and world gates for the graceful AutoLeave action. */
public final class AutoLeavePolicy {
    private AutoLeavePolicy() {
    }

    public static boolean shouldDisconnect(boolean connectedToMultiplayer, boolean singleplayer,
                                           boolean creativeOrSpectator, boolean hasDeathProtection,
                                           float health, double healthThreshold) {
        return connectedToMultiplayer && !singleplayer && !creativeOrSpectator && !hasDeathProtection
                && Float.isFinite(health) && Double.isFinite(healthThreshold)
                && healthThreshold >= 0.0 && health < healthThreshold;
    }

    public static int clampDelay(int delay) {
        return Math.max(0, Math.min(60, delay));
    }

    public static boolean delayElapsed(int consecutiveLowHealthTicks, int configuredDelay) {
        int requiredTicks = Math.max(1, clampDelay(configuredDelay));
        return consecutiveLowHealthTicks >= requiredTicks;
    }
}
