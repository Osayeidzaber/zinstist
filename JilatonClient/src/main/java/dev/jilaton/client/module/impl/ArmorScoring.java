package dev.jilaton.client.module.impl;

/** Pure loadout ranking adapted from LiquidBounce's ArmorComparator. */
public final class ArmorScoring {
    private ArmorScoring() {
    }

    public static double score(double expectedDamage, double armor, double toughness, double knockbackResistance,
                               int protection, int projectileProtection, int fireProtection, int blastProtection,
                               int featherFalling, int thorns, int respiration, int aquaAffinity, int unbreaking,
                               double durabilityFraction) {
        double armorReduction = clamp(armor - expectedDamage / (2.0 + toughness / 4.0), armor * 0.2, 20.0);
        double damageFactor = 1.0 - armorReduction / 25.0;

        // These level weights match LiquidBounce's compiled ArmorComparator for its six-point damage model.
        double enchantmentReduction = protection * 1.2 * 0.04
                + projectileProtection * 0.4 * 0.08
                + fireProtection * 0.39 * 0.15
                + blastProtection * 0.38 * 0.08;
        enchantmentReduction = clamp(enchantmentReduction, 0.0, 0.8);
        double damageReductionPoints = (1.0 - damageFactor * (1.0 - enchantmentReduction)) * 25.0;

        // Secondary enchantments are a small tie-breaker after the estimated damage reduction.
        double utility = featherFalling * 3.0 + thorns + respiration * 0.1 + aquaAffinity * 0.05 + unbreaking * 0.01;
        return damageReductionPoints + utility * 0.05 + knockbackResistance * 2.0
                + clamp(durabilityFraction, 0.0, 1.0) * 0.5;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
