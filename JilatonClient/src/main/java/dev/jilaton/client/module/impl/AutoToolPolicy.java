package dev.jilaton.client.module.impl;

/** Chooses the hotbar slot that mines the targeted block fastest, without wearing tools down to nothing. */
final class AutoToolPolicy {
    private AutoToolPolicy() {
    }

    static boolean prefer(float speed, boolean correct, float bestSpeed, boolean bestCorrect) {
        return !Float.isNaN(speed) && (correct && !bestCorrect || correct == bestCorrect && speed > bestSpeed);
    }

    /**
     * Best slot for the block, or -1 to keep the current one. {@code speed[i]} is the destroy speed of slot i
     * (1 for bare hands), {@code correct[i]} whether it gets drops, {@code durabilityLeft[i]} remaining uses or
     * {@link Integer#MAX_VALUE} for unbreakable items. Tools at or under {@code reserve} uses are skipped.
     */
    static int best(float[] speed, boolean[] correct, int[] durabilityLeft, int current, int reserve, boolean needsTool) {
        int best = -1;
        boolean currentValid = current >= 0 && current < speed.length && durabilityLeft[current] > reserve;
        float bestScore = currentValid ? speed[current] : Float.NEGATIVE_INFINITY;
        boolean bestCorrect = currentValid && (!needsTool || correct[current]);
        for (int i = 0; i < speed.length; i++) {
            if (durabilityLeft[i] <= reserve) continue;
            boolean candidateCorrect = !needsTool || correct[i];
            if (prefer(speed[i], candidateCorrect, bestScore, bestCorrect)) {
                bestScore = speed[i];
                bestCorrect = candidateCorrect;
                best = i;
            }
        }
        return best;
    }

}
