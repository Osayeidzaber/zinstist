package dev.jilaton.client.module.impl;

/** Pure activation gate for hotbar mushroom-stew healing. */
final class AutoSoupPolicy {
    private AutoSoupPolicy() {
    }

    static boolean shouldUse(float health, float maxHealth, float absorption, double healthPercent,
                             boolean considerAbsorption,
                             boolean hasSoup, boolean safeContext, boolean cooldownReady) {
        if (!hasSoup || !safeContext || !cooldownReady || !Float.isFinite(health)
                || !Float.isFinite(maxHealth) || !Float.isFinite(absorption)
                || !Double.isFinite(healthPercent) || health < 0.0F || maxHealth <= 0.0F
                || absorption < 0.0F || healthPercent < 0.0 || healthPercent > 100.0) {
            return false;
        }
        double effectiveHealth = health + (considerAbsorption ? absorption : 0.0F);
        return effectiveHealth <= maxHealth * healthPercent / 100.0;
    }
}
