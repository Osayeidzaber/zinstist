package dev.jilaton.client.module.impl;

final class KillAuraTargeting {
    static final String DISTANCE = "Distance";
    static final String HEALTH = "Health";
    static final String ANGLE = "Angle";

    private KillAuraTargeting() {
    }

    /** Lower rank is preferred, matching Anubis's selectable target-priority modes. */
    static double rank(String priority, double distance, double effectiveHealth, double angle) {
        return switch (priority) {
            case DISTANCE -> distance;
            case HEALTH -> effectiveHealth;
            case ANGLE -> angle;
            default -> throw new IllegalArgumentException("Unknown KillAura priority: " + priority);
        };
    }

    static boolean shouldSkipShield(boolean enabled, boolean shieldBlocks, boolean axeEquipped,
                                    boolean autoWeaponCanSelectAxe) {
        return enabled && shieldBlocks && !axeEquipped && !autoWeaponCanSelectAxe;
    }
}
