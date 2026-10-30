package dev.jilaton.client.module.impl;

/** Eligibility gate for the grounded LegitHop movement mode. */
final class SpeedPolicy {
    private SpeedPolicy() {
    }

    static boolean shouldHop(boolean grounded, boolean moving, boolean usingItem, boolean sneaking,
                             boolean fallFlying, boolean inLiquid, boolean screenOpen,
                             boolean requireSprint, boolean sprinting) {
        return grounded && moving && !usingItem && !sneaking && !fallFlying && !inLiquid
                && !screenOpen && (!requireSprint || sprinting);
    }
}
