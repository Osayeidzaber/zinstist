package dev.jilaton.client.module.impl;

/** Pure face-place damage thresholds adapted from Anubis CrystalAura. */
public final class CrystalAuraPolicy {
    private CrystalAuraPolicy() {
    }

    public static boolean shouldFacePlace(boolean enabled, float effectiveHealth, float healthThreshold,
                                          boolean lowArmor, boolean inHole) {
        return enabled && (effectiveHealth <= healthThreshold || lowArmor || inHole);
    }

    public static float minimumDamage(float normalThreshold, float facePlaceThreshold, boolean facePlacing,
                                      boolean breaking) {
        float threshold = facePlacing ? facePlaceThreshold : normalThreshold;
        return breaking ? threshold * 0.6F : threshold;
    }
}
