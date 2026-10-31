package dev.jilaton.client.music;

/**
 * Pure start/retry policy for the desktop media bridge. Kept free of IO, processes and Minecraft types so the
 * privacy gate (default off) and the bounded restart behaviour can be tested headlessly.
 *
 * <p>The bridge is the only music path that reads another application's media session; it must never be
 * spawned unless the user opted in. Once opted in it may start at most {@link #MAX_CONSECUTIVE_STARTS} times
 * in a row without producing a line, each after an exponential back-off. A bridge that ran fine and then went
 * silent is treated as one failure and killed so the next tick can restart it.
 */
public final class MediaBridgePolicy {
    /** Consecutive starts without a single line from the bridge before Jilaton stops trying. */
    public static final int MAX_CONSECUTIVE_STARTS = 3;
    public static final long FIRST_RESTART_DELAY_NANOS = 5_000_000_000L;
    public static final long MAX_RESTART_DELAY_NANOS = 300_000_000_000L;
    /** A bridge that produces no line for this long is considered hung and is destroyed. */
    public static final long OUTPUT_STALE_NANOS = 10_000_000_000L;
    public static final String OFF_STATUS = "Desktop media bridge is off";
    public static final String UNSUPPORTED_STATUS =
            "Desktop media sessions are only available on macOS and Windows";
    public static final String GAVE_UP_STATUS = "Media bridge stopped after repeated failed starts";

    private MediaBridgePolicy() { }

    /** What the tick should do with the bridge this frame. */
    public enum Decision { OFF, UNSUPPORTED, RUNNING, WAITING, SPAWN, GAVE_UP }

    /**
     * @param enabled           the user's persisted opt-in (default off)
     * @param supported         whether this OS has a bridge implementation
     * @param alive             whether the current bridge process is still running
     * @param nowNanos          {@link System#nanoTime()} of this tick
     * @param restartAfterNanos earliest nanoTime another start is allowed
     * @param consecutiveStarts starts since the bridge last produced a line (or since opt-in)
     */
    public static Decision decide(boolean enabled, boolean supported, boolean alive, long nowNanos,
                                  long restartAfterNanos, int consecutiveStarts) {
        if (!enabled) return Decision.OFF;
        if (!supported) return Decision.UNSUPPORTED;
        if (alive) return Decision.RUNNING;
        if (consecutiveStarts >= MAX_CONSECUTIVE_STARTS) return Decision.GAVE_UP;
        if (nowNanos < restartAfterNanos) return Decision.WAITING;
        return Decision.SPAWN;
    }

    /** 5 s, doubling per failed start, capped at five minutes; negative counters behave like zero. */
    public static long restartDelayNanos(int failedStarts) {
        return Math.min(MAX_RESTART_DELAY_NANOS,
                FIRST_RESTART_DELAY_NANOS << Math.min(Math.max(failedStarts, 0), 6));
    }

    /**
     * Whether the bridge has gone silent. Before the first line the start time is the reference, so a process
     * that never speaks is recycled too instead of living forever without data.
     */
    public static boolean stale(long lastOutputNanos, long startedNanos, long nowNanos) {
        long reference = lastOutputNanos != 0 ? lastOutputNanos : startedNanos;
        return reference != 0 && nowNanos - reference > OUTPUT_STALE_NANOS;
    }

    /** Idle line for the off/unsupported states; empty when the bridge is allowed to run. */
    public static String idleStatus(boolean enabled, boolean supported) {
        if (!enabled) return OFF_STATUS;
        if (!supported) return UNSUPPORTED_STATUS;
        return "";
    }
}
