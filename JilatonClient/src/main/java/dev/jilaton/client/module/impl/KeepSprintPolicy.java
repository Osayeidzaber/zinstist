package dev.jilaton.client.module.impl;

/** Pure knockback-scaling policy for the local player's post-attack slowdown. */
final class KeepSprintPolicy {
    private KeepSprintPolicy() {
    }

    static double motionFactor(int hurtTime, double motionMin, double motionMax,
                               double hurtMotionMin, double hurtMotionMax,
                               int hurtTimeMin, int hurtTimeMax,
                               double chance, double chanceRoll, double motionRoll) {
        if (!Double.isFinite(motionMin) || !Double.isFinite(motionMax)
                || !Double.isFinite(hurtMotionMin) || !Double.isFinite(hurtMotionMax)
                || !Double.isFinite(chance) || !Double.isFinite(chanceRoll)
                || !Double.isFinite(motionRoll) || hurtTimeMin < 0 || hurtTimeMax > 10
                || hurtTimeMin > hurtTimeMax || chance < 0.0 || chance > 100.0
                || motionMin < 0.0 || motionMax > 100.0 || motionMin > motionMax
                || hurtMotionMin < 0.0 || hurtMotionMax > 100.0 || hurtMotionMin > hurtMotionMax
                || chanceRoll < 0.0 || chanceRoll >= 100.0) {
            return 0.6;
        }
        if (chanceRoll > chance) {
            return 0.6;
        }

        boolean hurtWindow = hurtTime >= hurtTimeMin && hurtTime <= hurtTimeMax;
        double minimum = hurtWindow ? hurtMotionMin : motionMin;
        double maximum = hurtWindow ? hurtMotionMax : motionMax;
        double normalizedRoll = Math.max(0.0, Math.min(1.0, motionRoll));
        return (minimum + (maximum - minimum) * normalizedRoll) / 100.0;
    }
}
