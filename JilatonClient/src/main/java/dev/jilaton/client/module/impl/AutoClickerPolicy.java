package dev.jilaton.client.module.impl;

/** Timing gates adapted from LiquidBounce's AutoClicker break and item-use handling. */
public final class AutoClickerPolicy {
    public static final long BLOCK_BREAK_DELAY_NANOS = 300_000_000L;

    private AutoClickerPolicy() {
    }

    public static boolean delayAfterBlockBreak(long nowNanos, long lastBreakNanos, boolean enabled) {
        if (!enabled || lastBreakNanos == Long.MIN_VALUE) {
            return false;
        }
        long elapsed = nowNanos - lastBreakNanos;
        return elapsed >= 0L && elapsed < BLOCK_BREAK_DELAY_NANOS;
    }

    public static long postUseResumeTick(long currentTick, int delayTicks) {
        return currentTick + Math.max(0, delayTicks);
    }
}
