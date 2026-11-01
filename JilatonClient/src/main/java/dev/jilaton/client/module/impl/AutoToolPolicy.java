package dev.jilaton.client.module.impl;

/** Chooses the hotbar slot that mines the targeted block fastest, without wearing tools down to nothing. */
final class AutoToolPolicy {
    private AutoToolPolicy() {
    }

    /**
     * Best slot for the block, or -1 to keep the current one. {@code speed[i]} is the destroy speed of slot i
     * (1 for bare hands), {@code correct[i]} whether it gets drops, {@code durabilityLeft[i]} remaining uses or
     * {@link Integer#MAX_VALUE} for unbreakable items. Tools at or under {@code reserve} uses are skipped.
     */
    static int best(float[] speed, boolean[] correct, int[] durabilityLeft, int current, int reserve, boolean needsTool) {
        int best = -1;
        float bestScore = score(speed, correct, durabilityLeft, current, reserve, needsTool);
        for (int i = 0; i < speed.length; i++) {
            float s = score(speed, correct, durabilityLeft, i, reserve, needsTool);
            if (s > bestScore + 1e-4F) {
                bestScore = s;
                best = i;
            }
        }
        return best;
    }

    private static float score(float[] speed, boolean[] correct, int[] durabilityLeft, int i, int reserve, boolean needsTool) {
        if (i < 0 || i >= speed.length) return Float.NEGATIVE_INFINITY;
        if (durabilityLeft[i] <= reserve) return Float.NEGATIVE_INFINITY;
        // A tool that yields the drop always beats a faster one that does not, for blocks that need one.
        return speed[i] + (needsTool && correct[i] ? 1000F : 0F);
    }
}
