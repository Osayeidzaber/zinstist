package dev.jilaton.client.module.impl;

public final class CriticalWindow {
    private CriticalWindow() {
    }

    public static boolean isDescending(boolean onGround, double fallDistance, double verticalVelocity) {
        return !onGround && fallDistance > 0.0D && verticalVelocity < 0.0D;
    }

    public static boolean shouldQueueAttack(boolean jumpMode, boolean onGround, boolean criticalWindow) {
        if (criticalWindow) {
            return false;
        }
        return !jumpMode || onGround;
    }
}
