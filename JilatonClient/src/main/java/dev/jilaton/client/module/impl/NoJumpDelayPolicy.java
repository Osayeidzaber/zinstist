package dev.jilaton.client.module.impl;

/** Policy for choosing the local jump cooldown when vanilla rearms it. */
final class NoJumpDelayPolicy {
    private NoJumpDelayPolicy() {
    }

    static boolean shouldRearm(int requestedTicks, int currentTicks) {
        return requestedTicks > currentTicks;
    }

    static int rearm(int vanillaTicks, int cooldownTicks, int varianceOffset) {
        int requested = Math.max(0, cooldownTicks) + Math.max(0, varianceOffset);
        return Math.min(Math.max(0, vanillaTicks), requested);
    }
}
