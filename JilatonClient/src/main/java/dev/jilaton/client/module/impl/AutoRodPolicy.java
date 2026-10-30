package dev.jilaton.client.module.impl;

import dev.jilaton.client.rotation.RotationAngles;
import dev.jilaton.client.rotation.RotationPolicy;

public final class AutoRodPolicy {
    private AutoRodPolicy() {
    }

    /** Shortest-path angular error from the current view to the target angles, in degrees. */
    public static float aimError(float viewYaw, float viewPitch, float targetYaw, float targetPitch) {
        return RotationPolicy.angularError(viewYaw, viewPitch, new RotationAngles(targetYaw, targetPitch));
    }

    public static boolean targetEligible(double distanceSquared, double minimumRange, double maximumRange,
                                         float targetHealth, float minimumTargetHealth, boolean visible,
                                         float aimErrorDegrees, float maximumAimErrorDegrees) {
        if (!Double.isFinite(distanceSquared) || !Double.isFinite(minimumRange)
                || !Double.isFinite(maximumRange) || !Float.isFinite(targetHealth)
                || !Float.isFinite(minimumTargetHealth) || !Float.isFinite(aimErrorDegrees)
                || !Float.isFinite(maximumAimErrorDegrees) || distanceSquared < 0.0
                || minimumRange < 0.0 || maximumRange < minimumRange || !visible) {
            return false;
        }
        return distanceSquared >= minimumRange * minimumRange
                && distanceSquared <= maximumRange * maximumRange
                && targetHealth > minimumTargetHealth
                && aimErrorDegrees <= maximumAimErrorDegrees;
    }

    public static boolean mayStart(float selfHealth, float minimumSelfHealth, int nearbyEnemies,
                                   int maximumNearbyEnemies, boolean rodAvailable, boolean attackKeyRequired,
                                   boolean attackKeyDown, boolean usingItem, boolean pauseWhileUsingItem) {
        if (!Float.isFinite(selfHealth) || !Float.isFinite(minimumSelfHealth)
                || nearbyEnemies < 0 || maximumNearbyEnemies < 0 || selfHealth <= minimumSelfHealth
                || !rodAvailable || attackKeyRequired && !attackKeyDown
                || pauseWhileUsingItem && usingItem) {
            return false;
        }
        return maximumNearbyEnemies == 0 || nearbyEnemies <= maximumNearbyEnemies;
    }

    public static boolean shouldPull(boolean hookHit, boolean timedOut, boolean pullOnOutOfRange,
                                     boolean targetInRange) {
        return hookHit || timedOut || pullOnOutOfRange && !targetInRange;
    }
}
