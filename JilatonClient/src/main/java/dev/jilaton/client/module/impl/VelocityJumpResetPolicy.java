package dev.jilaton.client.module.impl;

/** Pure eligibility and cooldown policy for the optional jump-reset input behavior. */
public final class VelocityJumpResetPolicy {
    private static final int HURT_WINDOW_TICKS = 9;

    private VelocityJumpResetPolicy() {
    }

    public static boolean shouldJump(int hurtTime, boolean onGround, boolean sprinting,
                                     boolean fallDamageVelocity, int progress, int requiredProgress,
                                     int chanceRoll, int chancePercent) {
        return hurtTime == HURT_WINDOW_TICKS && onGround && sprinting && !fallDamageVelocity
                && progress >= requiredProgress && chancePercent > 0
                && chanceRoll >= 0 && chanceRoll < chancePercent;
    }

    public static boolean shouldAdvanceCooldown(boolean receivedHitMode, int hurtTime) {
        return !receivedHitMode || hurtTime == HURT_WINDOW_TICKS;
    }
}
