package dev.jilaton.client.module.impl;

/** Blast-damage guard for players explicitly listed as friends. */
public final class CrystalSafetyPolicy {
    private CrystalSafetyPolicy() {
    }

    public static boolean friendDamageIsSafe(float damage, float maximumDamage, float effectiveHealth) {
        if (!Float.isFinite(damage) || !Float.isFinite(maximumDamage) || !Float.isFinite(effectiveHealth)
                || damage < 0.0F || maximumDamage < 0.0F) {
            return false;
        }
        return damage <= 0.0F || damage <= maximumDamage && damage * 1.15F + 2.0F < effectiveHealth;
    }
}
