package dev.jilaton.client.module.impl;

/** Durability-first armor replacement arbitration for AutoArmor's SaveArmor option. */
public final class ArmorSelectionPolicy {
    private ArmorSelectionPolicy() {
    }

    public static boolean shouldSelect(boolean currentArmorIsDurable, boolean candidateIsDurable,
                                       boolean bestCandidateIsDurable, double candidateScore,
                                       double bestScore, double minimumUpgrade) {
        if (!Double.isFinite(candidateScore) || !Double.isFinite(bestScore)
                || !Double.isFinite(minimumUpgrade) || minimumUpgrade < 0.0) {
            return false;
        }
        if (currentArmorIsDurable && !candidateIsDurable) {
            return false;
        }
        if (candidateIsDurable != bestCandidateIsDurable) {
            return candidateIsDurable;
        }
        return candidateScore > bestScore + minimumUpgrade;
    }
}
