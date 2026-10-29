package dev.jilaton.client.module.impl;

public final class HitboxMargin {
    private HitboxMargin() {
    }

    public static float apply(float vanillaMargin, boolean enabled, boolean eligible, double configuredMargin) {
        if (!enabled || !eligible || !Double.isFinite(configuredMargin)) {
            return vanillaMargin;
        }
        return (float) Math.max(0.0, Math.min(1.0, configuredMargin));
    }
}
