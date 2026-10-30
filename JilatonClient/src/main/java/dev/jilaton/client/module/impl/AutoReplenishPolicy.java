package dev.jilaton.client.module.impl;

/** Pure slot and threshold rules shared by AutoReplenish and its tests. */
final class AutoReplenishPolicy {
    private AutoReplenishPolicy() {
    }

    static boolean needsRefill(int count, int threshold) {
        if (count < 0 || threshold < 1) {
            throw new IllegalArgumentException("Count must be nonnegative and threshold must be positive");
        }
        return count == 0 || count <= threshold;
    }

    static boolean sourceImproves(int sourceCount, int targetCount) {
        return sourceCount > targetCount;
    }

    static boolean targetNeedsRefill(boolean empty, boolean matchesReference, int count, int threshold) {
        return empty || matchesReference && needsRefill(count, threshold);
    }

    static int swapButton(int trackedSlot) {
        if (trackedSlot >= 0 && trackedSlot < 9) {
            return trackedSlot;
        }
        if (trackedSlot == 9) {
            return 40;
        }
        throw new IllegalArgumentException("Tracked slot must be a hotbar slot or offhand");
    }
}
