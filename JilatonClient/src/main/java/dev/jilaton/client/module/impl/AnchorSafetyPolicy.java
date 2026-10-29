package dev.jilaton.client.module.impl;

final class AnchorSafetyPolicy {
    private AnchorSafetyPolicy() {
    }

    static boolean allowsDetonation(float selfDamage, float maximumDamage, float health,
                                    float absorption, boolean hasTotem) {
        if (!Float.isFinite(selfDamage) || !Float.isFinite(maximumDamage)
                || !Float.isFinite(health) || !Float.isFinite(absorption)
                || selfDamage < 0.0F || maximumDamage < 0.0F || health < 0.0F || absorption < 0.0F
                || selfDamage > maximumDamage) {
            return false;
        }
        return hasTotem || selfDamage < health + absorption;
    }
}
