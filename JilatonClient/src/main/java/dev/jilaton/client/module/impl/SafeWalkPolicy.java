package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec2;

/** Pure input gate and travel direction for SafeWalk's edge-triggered vanilla sneak behavior. */
final class SafeWalkPolicy {
    private SafeWalkPolicy() {
    }

    static boolean shouldSneak(boolean grounded, boolean manualSneak, boolean moving,
                               boolean exemptState, boolean unsupportedAhead) {
        return grounded && !manualSneak && moving && !exemptState && unsupportedAhead;
    }

    /** Vanilla convention: left is positive sideways, forward is positive forward. */
    static Vec2 aheadDirection(float yawDegrees, double forward, double sideways) {
        if (!Float.isFinite(yawDegrees) || !Double.isFinite(forward) || !Double.isFinite(sideways)) {
            return Vec2.ZERO;
        }
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = -sin * forward + cos * sideways;
        double z = cos * forward + sin * sideways;
        if (!(x * x + z * z > 1.0E-12)) {
            return Vec2.ZERO;
        }
        return new Vec2((float) x, (float) z).normalized();
    }
}
