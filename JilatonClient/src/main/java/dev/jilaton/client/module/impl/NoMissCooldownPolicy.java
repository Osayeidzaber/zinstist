package dev.jilaton.client.module.impl;

/** Pure policy for the client miss-time gate and optional miss-click consumption. */
public final class NoMissCooldownPolicy {
    private NoMissCooldownPolicy() {
    }

    public static int missTime(boolean enabled, boolean removeCooldown, int originalMissTime) {
        return enabled && removeCooldown ? 0 : originalMissTime;
    }

    public static boolean allowMissTimeWrite(boolean enabled, boolean removeCooldown) {
        return !enabled || !removeCooldown;
    }

    public static boolean consumeMiss(boolean enabled, boolean cancelOnMiss) {
        return enabled && cancelOnMiss;
    }
}
