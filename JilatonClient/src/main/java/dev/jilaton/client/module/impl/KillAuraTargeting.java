package dev.jilaton.client.module.impl;

import net.minecraft.util.Mth;

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

    /** Signed shortest-path yaw difference from the view to the target, in [-180, 180]. */
    static float yawDelta(float viewYaw, float targetYaw) {
        return Mth.wrapDegrees(targetYaw - viewYaw);
    }

    /** Selection order: best rank, then least hurt time, then closest; negative means left wins. */
    static int compare(double leftRank, int leftHurtTime, double leftDistance,
                       double rightRank, int rightHurtTime, double rightDistance) {
        int order = Double.compare(leftRank, rightRank);
        if (order == 0) {
            order = Integer.compare(leftHurtTime, rightHurtTime);
        }
        if (order == 0) {
            order = Double.compare(leftDistance, rightDistance);
        }
        return order;
    }
}
