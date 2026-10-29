package dev.jilaton.client.module.impl;

/** Health-threshold decision shared by AutoGapple execution and AutoPot arbitration. */
final class AutoGapplePolicy {
    private AutoGapplePolicy() {
    }

    static boolean shouldUse(double health, double absorption, double threshold, boolean considerAbsorption) {
        double effectiveHealth = health + (considerAbsorption ? Math.max(0.0, absorption) : 0.0);
        return effectiveHealth <= threshold;
    }
}
