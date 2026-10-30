package dev.jilaton.client.module.impl;

/** Limits the Step module to raising vanilla collision step height for the local player. */
final class StepPolicy {
    private static final float MIN_STEP_HEIGHT = 0.6F;
    private static final float MAX_STEP_HEIGHT = 2.5F;

    private StepPolicy() {
    }

    static float effectiveHeight(boolean enabled, boolean localPlayer, float vanillaHeight,
                                 double configuredHeight) {
        if (!enabled || !localPlayer) {
            return vanillaHeight;
        }
        float requested = (float) Math.max(MIN_STEP_HEIGHT, Math.min(MAX_STEP_HEIGHT, configuredHeight));
        return Math.max(vanillaHeight, requested);
    }
}
