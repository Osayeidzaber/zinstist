package dev.jilaton.client.module.impl;

/** Pure input gate for SafeWalk's edge-triggered vanilla sneak behavior. */
final class SafeWalkPolicy {
    private SafeWalkPolicy() {
    }

    static boolean shouldSneak(boolean grounded, boolean manualSneak, boolean moving,
                               boolean exemptState, boolean unsupportedAhead) {
        return grounded && !manualSneak && moving && !exemptState && unsupportedAhead;
    }
}
