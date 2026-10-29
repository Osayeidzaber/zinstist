package dev.jilaton.client.module.impl;

/** Pure health and absorption gate for server-provided PvP player heads. */
final class AutoHeadPolicy {
    private AutoHeadPolicy() {
    }

    static boolean shouldUse(float health, float maxHealth, float absorption, double healthPercent,
                             double maxAbsorption, boolean considerAbsorption, boolean hasHead,
                             boolean safeContext, boolean cooldownReady) {
        if (!hasHead || !safeContext || !cooldownReady || !Float.isFinite(health)
                || !Float.isFinite(maxHealth) || !Float.isFinite(absorption)
                || !Double.isFinite(healthPercent) || !Double.isFinite(maxAbsorption)
                || health < 0.0F || maxHealth <= 0.0F || absorption < 0.0F
                || healthPercent < 0.0 || healthPercent > 100.0
                || maxAbsorption < 0.0 || maxAbsorption > 8.0 || absorption > maxAbsorption) {
            return false;
        }
        double effectiveHealth = health + (considerAbsorption ? absorption : 0.0F);
        return effectiveHealth <= maxHealth * healthPercent / 100.0;
    }
}
