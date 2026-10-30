package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;

/** Pure target and lead rules adapted from LiquidBounce's ElytraTarget feature. */
public final class ElytraTargetPolicy {
    private ElytraTargetPolicy() {
    }

    public static boolean isEligible(boolean alive, boolean visible, double distanceSquared,
                                     double range, double angularError, double fieldOfView) {
        if (!alive || !visible || !Double.isFinite(distanceSquared) || distanceSquared < 0.0
                || !Double.isFinite(range) || range < 0.0
                || !Double.isFinite(angularError) || angularError < 0.0
                || !Double.isFinite(fieldOfView) || fieldOfView < 0.0) {
            return false;
        }
        return distanceSquared <= range * range && angularError <= fieldOfView;
    }

    public static Vec3 predictPosition(Vec3 position, Vec3 velocity, boolean enabled,
                                       double multiplier, boolean gravity) {
        if (position == null || velocity == null || !Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("Prediction requires finite, nonnegative movement lead");
        }
        if (!enabled || multiplier == 0.0) {
            return position;
        }

        Vec3 predicted = position.add(velocity.scale(multiplier));
        return gravity ? predicted.add(0.0, -0.5 * 0.05 * multiplier * multiplier, 0.0) : predicted;
    }
}
