package dev.jilaton.client.module.impl;

/** Decision and preference rules for automatic food use. */
final class AutoEatPolicy {
    private AutoEatPolicy() {
    }

    static boolean shouldEat(int foodLevel, float health, int hungerThreshold, float healthThreshold) {
        return foodLevel <= hungerThreshold || healthThreshold > 0.0F && health <= healthThreshold;
    }

    static float score(int nutrition, float saturation, boolean canAlwaysEat, boolean lowHealth) {
        return nutrition + saturation + (lowHealth && canAlwaysEat ? 100.0F : 0.0F);
    }
}
