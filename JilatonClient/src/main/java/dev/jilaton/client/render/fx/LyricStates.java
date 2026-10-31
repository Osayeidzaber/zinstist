package dev.jilaton.client.render.fx;

/**
 * Explicit lifecycle shared by every lyric presentation (world stack, World Trail, Sky Lyrics). A line is born
 * {@link State#INCOMING incoming}, eases into place while {@link State#SETTLING settling}, holds as
 * {@link State#ACTIVE active} while it is sung, falls back to {@link State#TRANSITIONING transitioning} when the
 * next line takes over, then {@link State#FADING fades} and finally {@link State#EXPIRED expires} and is dropped.
 * Pure timing math, so the lifecycle is unit tested without a client.
 */
public final class LyricStates {
    public enum State { INCOMING, SETTLING, ACTIVE, TRANSITIONING, FADING, EXPIRED }

    /** Rise-in before a line starts settling into its slot. */
    public static final double INCOMING_SECONDS = 0.12;
    /** Scale/fade-in of a line into its final place. */
    public static final double SETTLE_SECONDS = 0.38;
    /** How long a replaced line keeps full brightness before it starts fading. */
    public static final double FADE_SECONDS = 1.4;
    /** Eviction is faster than the natural fade so the live-line bound is never overrun for long. */
    public static final double EVICT_SECONDS = 0.45;

    private LyricStates() { }

    /**
     * Lifecycle state of a line.
     *
     * @param age            seconds since the line was born
     * @param active         whether this line is the one currently being sung
     * @param lingerSeconds  seconds an inactive line stays readable before fading
     * @param evictedAge     seconds since eviction, or a negative value when the line is not evicted
     */
    public static State state(double age, boolean active, double lingerSeconds, double evictedAge) {
        if (evictedAge >= 0) return evictedAge >= EVICT_SECONDS ? State.EXPIRED : State.FADING;
        if (age < INCOMING_SECONDS) return State.INCOMING;
        if (age < SETTLE_SECONDS) return State.SETTLING;
        if (active) return State.ACTIVE;
        if (age < lingerSeconds) return State.TRANSITIONING;
        if (age < lingerSeconds + FADE_SECONDS) return State.FADING;
        return State.EXPIRED;
    }

    /**
     * Visibility for a line, derived from its state. Incoming and settling share one ease-in ramp: the line is
     * faint but visible the moment it is born instead of flashing empty for the first {@link #INCOMING_SECONDS}.
     */
    public static double alpha(double age, boolean active, double lingerSeconds, double evictedAge) {
        return switch (state(age, active, lingerSeconds, evictedAge)) {
            case INCOMING, SETTLING -> ease(Math.min(1, age / SETTLE_SECONDS));
            case ACTIVE, TRANSITIONING -> 1;
            case FADING -> evictedAge >= 0
                    ? Math.max(0, 1 - evictedAge / EVICT_SECONDS)
                    : Math.max(0, Math.min(1, (lingerSeconds + FADE_SECONDS - age) / FADE_SECONDS));
            case EXPIRED -> 0;
        };
    }

    /** 0..1 eased progress used for the rise/scale of a freshly born line. */
    public static double appear(double age) {
        return ease(Math.max(0, Math.min(1, age / SETTLE_SECONDS)));
    }

    private static double ease(double t) {
        return 1 - Math.pow(1 - t, 3);
    }
}
