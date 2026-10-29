package dev.jilaton.client.module.impl;

/** Pure motion policy for the vanilla HighJump mode. */
public final class HighJumpPolicy {
    private HighJumpPolicy() {
    }

    public static double motionAfterJump(double vanillaMotion, double configuredMotion) {
        if (!Double.isFinite(vanillaMotion) || vanillaMotion <= 1.0E-5
                || !Double.isFinite(configuredMotion) || configuredMotion <= 0.0) {
            return vanillaMotion;
        }
        return configuredMotion;
    }
}
