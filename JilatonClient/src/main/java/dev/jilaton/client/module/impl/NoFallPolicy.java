package dev.jilaton.client.module.impl;

/** Fall-distance gate for NoFall's movement-packet ground flag. */
final class NoFallPolicy {
    private NoFallPolicy() {
    }

    static boolean shouldSpoof(boolean onGround, boolean descending, boolean exemptState,
                               double fallDistance, double triggerDistance) {
        if (onGround || !descending || exemptState || !Double.isFinite(fallDistance)
                || !Double.isFinite(triggerDistance)) {
            return false;
        }
        return fallDistance >= Math.max(0.0, triggerDistance);
    }
}
