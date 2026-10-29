package dev.jilaton.client.module.impl;

/** Damage ranking adapted from Anubis's Combo.value scoring policy. */
final class AnchorAuraScoring {
    static final float POP_RANK = 500.0F;
    static final float LETHAL_RANK = 1000.0F;
    static final float SELF_DAMAGE_WEIGHT = 0.5F;

    private AnchorAuraScoring() {
    }

    static float value(float effectiveDamage, float health, float absorption, boolean hasTotem) {
        if (!Float.isFinite(effectiveDamage) || !Float.isFinite(health) || !Float.isFinite(absorption)
                || effectiveDamage < 0.0F || health < 0.0F || absorption < 0.0F) {
            return Float.NEGATIVE_INFINITY;
        }
        float effectiveHealth = health + absorption;
        float rank = effectiveDamage < effectiveHealth
                ? 0.0F : hasTotem ? POP_RANK : LETHAL_RANK;
        return Math.min(effectiveDamage, effectiveHealth) + rank;
    }

    static float blastScore(float targetValue, float selfDamage) {
        if (!Float.isFinite(targetValue) || !Float.isFinite(selfDamage) || selfDamage < 0.0F) {
            return Float.NEGATIVE_INFINITY;
        }
        return targetValue - SELF_DAMAGE_WEIGHT * selfDamage;
    }
}
