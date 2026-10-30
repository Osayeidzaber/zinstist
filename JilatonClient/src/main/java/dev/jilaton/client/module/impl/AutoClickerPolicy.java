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

    public static boolean allowsEntity(String objective, boolean playerTarget,
                                       boolean playerAllowed, boolean playersOnly) {
        if ((playerTarget && !playerAllowed) || (!playerTarget && playersOnly)) {
            return false;
        }
        return switch (objective == null ? "Enemy" : objective) {
            case "Enemy" -> playerTarget && playerAllowed;
            case "Entity", "Any" -> true;
            case "Block" -> false;
            default -> false;
        };
    }

    public static boolean clicksBlock(String objective) {
        return "Block".equals(objective) || "Any".equals(objective);
    }

    public static long postUseResumeTick(long currentTick, int delayTicks) {
        return currentTick + Math.max(0, delayTicks);
    }
}
